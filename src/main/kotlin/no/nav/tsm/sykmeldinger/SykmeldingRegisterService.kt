package no.nav.tsm.sykmeldinger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.smregister.database.SmregisterDatabase
import no.nav.tsm.smregister.models.ReceivedSykmelding
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
        sykmelidngInput: MigrertReceivedSykmelding
    ) {

        val producerRecord = ProducerRecord(
            sykmeldingInputTopic,
            sykmelidngInput.sykmeldingId,
            sykmelidngInput.receivedSykmelding,
        )
        producerRecord.headers().add(RecordHeader("source", sykmelidngInput.source.toByteArray()))
        if(producerRecord.value() == null) {
            logger.info("tombstone sykmelding ${sykmelidngInput.sykmeldingId}")
        }
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

        val sykmeldingerToGetFromaDb = migrertReceivedSykmeldinger.filter {
            (it.source == "REGDUMP" || it.source == "NO_SOURCE" ) ||
                (it.receivedSykmelding != null &&
            it.receivedSykmelding.validationResult == null)
        }.map { it.sykmeldingId }
        val sykmeldingerFromDb = if(sykmeldingerToGetFromaDb.isNotEmpty()) {
            smregisterDatabase.getFullSykmeldinger(sykmeldingerToGetFromaDb).associateBy { it.sykmelding.id }
        } else {
            emptyMap()
        }
        val sykmeldingerInput = migrertReceivedSykmeldinger.map {
            val receivedSykmelding = if((it.source == "REGDUMP" || it.source == "NO_SOURCE" )  && it.receivedSykmelding == null) {
                sykmeldingerFromDb[it.sykmeldingId]
            } else if (it.receivedSykmelding != null && it.receivedSykmelding.validationResult == null) {
                val validationResult = sykmeldingerFromDb[it.sykmeldingId]?.validationResult
                if(validationResult == null) {
                    logger.warn("validationResult is null for sykmeldingId ${it.sykmeldingId} from ${it.source}, should be tombstoned")
                    null
                } else {
                    it.receivedSykmelding.copy(validationResult = validationResult)
                }
            } else {
                it.receivedSykmelding
            }
            MigrertReceivedSykmelding(
                sykmeldingId = it.sykmeldingId,
                receivedSykmelding = receivedSykmelding,
            )
        }

        sykmeldingerInput.forEach {
            sendReceivedSykmelding(it)
        }
    }
}
