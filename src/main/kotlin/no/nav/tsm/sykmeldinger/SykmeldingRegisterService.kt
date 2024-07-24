package no.nav.tsm.sykmeldinger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.smregister.database.SmregisterDatabase
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmeldinger.kafka.CompleteMigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.MigrertReceivedSykmelding
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory

class SykmeldingRegisterService(
    private val smregisterDatabase: SmregisterDatabase,
    private val sykmeldingInputProducer: KafkaProducer<String, ReceivedSykmelding?>,
    private val sykmeldingInputTopic: String
) {
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SykmeldingRegisterService::class.java)
    }

    private fun sendReceivedSykmelding(
        completeMigrertSykmelding: CompleteMigrertSykmelding
    ) {

        val producerRecord = ProducerRecord(
            sykmeldingInputTopic,
            completeMigrertSykmelding.sykmeldingId,
            completeMigrertSykmelding.receivedSykmelding,
        )
        producerRecord.headers().add(RecordHeader("source", completeMigrertSykmelding.source.toByteArray()))

        sykmeldingInputProducer.send(
            producerRecord
        ).get()
    }

    suspend fun handleMigrertSykmeldinger(migrertSykmelding: List<MigrertSykmelding>) {

        val migrertReceivedSykmeldinger = migrertSykmelding.map {
            MigrertReceivedSykmelding(
                sykmeldingId = it.sykmeldingId,
                source = it.source,
                receivedSykmelding = it.receivedSykmelding?.let { receivedSykmelding -> objectMapper.readValue(receivedSykmelding) }
            )
        }

        val sykmeldingerToGetFromaDb = migrertReceivedSykmeldinger.filter { it.receivedSykmelding?.validationResult == null }.map { it.sykmeldingId }
        val sykmeldingerFromDb = if(sykmeldingerToGetFromaDb.isNotEmpty()) {
            smregisterDatabase.getFullSykmeldinger(sykmeldingerToGetFromaDb).associateBy { it.sykmelding.id }
        } else {
            emptyMap()
        }
        val missingSykmeldinger = mutableListOf<String>()
        val completeMigrertSykmeldinger = mutableListOf<CompleteMigrertSykmelding>()
        migrertReceivedSykmeldinger.forEach {
            val receivedSykmelding = if(it.receivedSykmelding == null) {
                sykmeldingerFromDb[it.sykmeldingId]
            } else if (it.receivedSykmelding.validationResult == null) {
                it.receivedSykmelding.copy(validationResult = sykmeldingerFromDb[it.sykmeldingId]?.validationResult)
            } else {
                it.receivedSykmelding
            }
            if(receivedSykmelding == null) {
                missingSykmeldinger.add(it.sykmeldingId)
            } else {
                completeMigrertSykmeldinger.add(CompleteMigrertSykmelding(it.sykmeldingId, receivedSykmelding, it.source))
            }
        }

        if(missingSykmeldinger.isNotEmpty()) {
            logger.warn("Missing sykmeldinger in db: $missingSykmeldinger")
        }

        completeMigrertSykmeldinger.forEach {
            sendReceivedSykmelding(it)
        }
    }
}
