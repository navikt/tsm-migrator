package no.nav.tsm.reformat.sykmelding

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.reformat.sykmelding.model.SykmeldingRecord
import no.nav.tsm.reformat.sykmelding.service.MappingException
import no.nav.tsm.reformat.sykmelding.service.SykmeldingMapper
import no.nav.tsm.reformat.sykmelding.util.secureLog
import no.nav.tsm.sykmeldinger.kafka.PROCESSING_TARGET_HEADER
import no.nav.tsm.sykmeldinger.kafka.TSM_PROCESSING_TARGET
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
private val objectMapper: ObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    }
class SykmeldingReformatService(
    private val kafkaConsumer: KafkaConsumer<String, ReceivedSykmelding>,
    private val sykmeldingMapper: SykmeldingMapper,
    private val kafkaProducer: KafkaProducer<String, SykmeldingRecord>,
    private val outputTopic: String,
    private val inputTopic: String,
    private val cluster: String,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingReformatService::class.java)
    }

    suspend fun start() = coroutineScope {
        kafkaConsumer.subscribe(listOf(inputTopic))
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

    private fun processRecords(records: ConsumerRecords<String, ReceivedSykmelding>) {
        records.forEach { record ->
            try {
                val sykmeldingMedBehandlingsutfall = record.value()?.let { sykmeldingMapper.toNewSykmelding(it) }
                val processingTarget = record.headers().singleOrNull { header -> header.key() == PROCESSING_TARGET_HEADER }?.value()?.toString(Charsets.UTF_8)

                val producerRecord = ProducerRecord(outputTopic, record.key(), sykmeldingMedBehandlingsutfall)

                if(processingTarget == TSM_PROCESSING_TARGET) {
                    log.info("$TSM_PROCESSING_TARGET is $processingTarget, adding to headers")
                    producerRecord.headers().add(PROCESSING_TARGET_HEADER, TSM_PROCESSING_TARGET.toByteArray(Charsets.UTF_8))
                }

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

