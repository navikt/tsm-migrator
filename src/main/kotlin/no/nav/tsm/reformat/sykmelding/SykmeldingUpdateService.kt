package no.nav.tsm.reformat.sykmelding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.tsm.reformat.sykmelding.model.Papirsykmelding
import no.nav.tsm.reformat.sykmelding.model.SykmeldingRecord
import no.nav.tsm.reformat.sykmelding.model.Tiltak
import no.nav.tsm.reformat.sykmelding.model.XmlSykmelding
import no.nav.tsm.reformat.sykmelding.service.MappingException
import no.nav.tsm.reformat.sykmelding.service.SykmeldingMapper
import no.nav.tsm.reformat.sykmelding.util.secureLog
import no.nav.tsm.smregister.models.ReceivedSykmelding
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SykmeldingUpdateService(
    private val kafkaConsumer: KafkaConsumer<String, ReceivedSykmelding>,
    private val sykmeldingMapper: SykmeldingMapper,
    private val kafkaProducer: KafkaProducer<String, SykmeldingRecord>,
    private val outputTopic: String,
    private val inputTopic: String,
    private val cluster: String,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingReformatService::class.java)
        private val stopTimestamp = OffsetDateTime.of(2025, 6, 17, 16, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    var counter: Int = 0
    var totalRead: Int = 0

    suspend fun start() = coroutineScope {
        launch(Dispatchers.IO) {
            while(isActive) {
                log.info("Reformatted: $counter, Total read: $totalRead")
                delay(30.seconds)
            }
        }
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
        records.filter { it.timestamp() < stopTimestamp }.forEach { record ->
            try {
                val sykmeldingMedBehandlingsutfall = record.value()?.let { sykmeldingMapper.toNewSykmelding(it) }
                val shouldUpdate = shouldUpdateSykmelding(sykmeldingMedBehandlingsutfall)
                totalRead++
                if(shouldUpdate) {
                    val producerRecord = ProducerRecord(outputTopic, record.key(), sykmeldingMedBehandlingsutfall)
                    kafkaProducer.send(producerRecord).get()
                    counter++
                }
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

    private fun shouldUpdateSykmelding(sykmeldingRecord: SykmeldingRecord?): Boolean {
        return when(val sykmelding = sykmeldingRecord?.sykmelding) {
            is XmlSykmelding -> {
                isMissingAndreTiltak(sykmelding.tiltak)
            }
            is Papirsykmelding -> {
                isMissingAndreTiltak(sykmelding.tiltak)
            }
            else -> false
         }
    }

    private fun isMissingAndreTiltak(tiltak: Tiltak?): Boolean {
        return !tiltak?.andreTiltak.isNullOrBlank() && tiltak.tiltakNav == null
    }
}
