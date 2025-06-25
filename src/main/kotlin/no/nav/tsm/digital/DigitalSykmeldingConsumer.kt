package no.nav.tsm.digital

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import no.nav.tsm.reformat.sykmelding.service.MappingException
import no.nav.tsm.reformat.sykmelding.util.secureLog
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DigitalSykmeldingConsumer(private val kafkaConsumer: KafkaConsumer<String, SykmeldingRecord>,
                                private val kafkaProducer: KafkaProducer<String, ReceivedSykmelding?>,
                                private val tsmSykmeldingerTopic: String,
                                private val okSykmeldingTopic: String,
                                private val cluster: String,
    ) {

    companion object {
        private val log = LoggerFactory.getLogger(DigitalSykmeldingConsumer::class.java)
    }

    private val objectMapper = jacksonObjectMapper()

    suspend fun start() = coroutineScope {
        kafkaConsumer.subscribe(listOf(tsmSykmeldingerTopic))
        try {
            while (isActive) {
                val records = kafkaConsumer.poll(10.seconds.toJavaDuration())
                processRecords(records)
            }
        } catch (ex: Exception) {
            log.error("Error processing records, stopping consuming", ex)
        }

        kafkaConsumer.unsubscribe()
    }

    private fun processRecords(records: ConsumerRecords<String, SykmeldingRecord>) {
        records.forEach { record ->
            try {
                val receivedSykmelding = record.value()?.toReceivedSykmelding()
                val producerRecord = ProducerRecord(okSykmeldingTopic, record.key(), receivedSykmelding)
                kafkaProducer.send(producerRecord).get()
            } catch (mappingException: MappingException) {
                log.error("error processing sykmelding ${mappingException.receivedSykmelding.sykmelding.id} for p: ${record.partition()} at offset: ${record.offset()}", mappingException)

                if (cluster != "dev-gcp") {
                    secureLog.error(objectMapper.writeValueAsString(mappingException.receivedSykmelding))
                    throw mappingException
                }
            } catch (ex: Exception) {
                log.error(ex.message, ex)
                if (cluster != "dev-gcp") {
                    throw ex
                }
            }
        }
    }
}
