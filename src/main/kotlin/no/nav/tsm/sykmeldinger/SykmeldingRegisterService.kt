package no.nav.tsm.sykmeldinger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.smregister.database.SmregisterDatabase
import no.nav.tsm.smregister.models.ReceivedSykmelding
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

    suspend fun handleMigrertSykmelding(migrertSykmelding: MigrertSykmelding) {
        val receivedSykmelding = if (migrertSykmelding.receivedSykmelding == null) {
            smregisterDatabase.getFullSykmelding(migrertSykmelding.sykmeldingId)
        } else {
            val receivedSykmelding = objectMapper.readValue<ReceivedSykmelding>(migrertSykmelding.receivedSykmelding)
            when (receivedSykmelding.validationResult) {
                null -> {
                    smregisterDatabase.getFullSykmelding(migrertSykmelding.sykmeldingId)?.let { sykmeldingFromDb ->
                        receivedSykmelding.copy(validationResult = sykmeldingFromDb.validationResult)
                    }
                }
                else -> receivedSykmelding
            }
        }
        if(receivedSykmelding == null) {
            logger.warn("Received sykmelding is null for sykmeldingId: ${migrertSykmelding.sykmeldingId} ${migrertSykmelding.source}")
        }

        sendReceivedSykmelding(receivedSykmelding, migrertSykmelding)
    }

    private fun sendReceivedSykmelding(
        receivedSykmelding: ReceivedSykmelding?,
        migrertSykmelding: MigrertSykmelding
    ) {

        val producerRecord = ProducerRecord(
            sykmeldingInputTopic,
            migrertSykmelding.sykmeldingId,
            receivedSykmelding,
        )
        producerRecord.headers().add(RecordHeader("source", migrertSykmelding.source.toByteArray()))

        sykmeldingInputProducer.send(
            producerRecord
        ).get()
    }
}
