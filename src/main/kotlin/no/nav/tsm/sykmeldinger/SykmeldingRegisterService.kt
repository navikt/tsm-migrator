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
import no.nav.tsm.sykmeldinger.kafka.model.SykmeldingInput
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class SykmeldingRegisterService(private val smregisterDatabase: SmregisterDatabase,
                                private val sykmeldingInputProducer: KafkaProducer<String, SykmeldingInput>,
                                private val sykmeldingInputTopic:String) {
    private val objectMapper =     ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false) }

    companion object {
        private val logger = LoggerFactory.getLogger(SykmeldingRegisterService::class.java)
    }
    suspend fun handleMigrertSykmelding(migrertSykmelding: MigrertSykmelding) {
        if(migrertSykmelding.source == "REGDUMP" || migrertSykmelding.receivedSykmelding == null) {
            logger.info("Sykmelding with id ${migrertSykmelding.sykmeldingId} from ${migrertSykmelding.source} and ${migrertSykmelding.receivedSykmelding?.let { "not null" } ?: "null" }")
            val receivedSykmelding = smregisterDatabase.getFullSykmelding(migrertSykmelding.sykmeldingId)
            if(receivedSykmelding == null) {
                logger.warn("Sykmelding with id ${migrertSykmelding.sykmeldingId} not found in smregister")
            } else {
                sendReceivedSykmelding(receivedSykmelding, migrertSykmelding)
            }
        } else {
            val receivedSykmelding = objectMapper.readValue<ReceivedSykmelding>(migrertSykmelding.receivedSykmelding)
            if(receivedSykmelding.validationResult == null) {
                logger.info("ValidationResult is missing, getting from register for sykmelding: ${migrertSykmelding.sykmeldingId}")
                val sykmeldingFromDb = smregisterDatabase.getFullSykmelding(migrertSykmelding.sykmeldingId)
                if(sykmeldingFromDb == null) {
                    logger.warn("sykmelding with id ${migrertSykmelding.sykmeldingId} not found in smregister")
                } else {
                    val receivedSykmelding = receivedSykmelding.copy(validationResult = sykmeldingFromDb.validationResult)

                }
            }
        }
    }

    private fun sendReceivedSykmelding(
        receivedSykmelding: ReceivedSykmelding,
        migrertSykmelding: MigrertSykmelding
    ) {
        val sykmeldingInput = SykmeldingInput(
            receivedSykmelding = receivedSykmelding,
            source = migrertSykmelding.source,
            migrertSykmelding.sykmeldingId
        )

        sykmeldingInputProducer.send(
            ProducerRecord(
                sykmeldingInputTopic,
                migrertSykmelding.sykmeldingId,
                sykmeldingInput
            )
        )
    }
}
