package no.nav.tsm.sykmeldinger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmelding.model.SykmeldingModule
import no.nav.tsm.sykmeldinger.kafka.MigrertReceivedSykmelding
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory

class SykmeldingRegisterService(
    private val sykmeldingInputProducer: KafkaProducer<String, ReceivedSykmelding?>,
    private val sykmeldingInputTopic: String
) {
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(SykmeldingModule())
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

    fun handleMigrertSykmeldinger(migrertSykmelding: List<MigrertSykmelding>) {
        val migrertReceivedSykmeldinger = migrertSykmelding.map {
            MigrertReceivedSykmelding(
                sykmeldingId = it.sykmeldingId,
                source = it.source,
                receivedSykmelding = it.receivedSykmelding?.let { receivedSykmelding -> objectMapper.readValue(receivedSykmelding) }
            )
        }

        migrertReceivedSykmeldinger.forEach {
            if(it.receivedSykmelding == null) {
                logger.info("Received tombstone for sykmeldingId: ${it.sykmeldingId}")
            }
            sendReceivedSykmelding(it)
        }
    }
}
