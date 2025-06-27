package no.nav.tsm.digital

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.tsm.reformat.sykmelding.service.MappingException
import no.nav.tsm.reformat.sykmelding.util.secureLog
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.smregister.models.ValidationResultLegacy
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingType
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

data class ManuellOppgave(
    val receivedSykmelding: ReceivedSykmelding,
    val validationResult: ValidationResultLegacy,
)

class DigitalSykmeldingConsumer(private val kafkaConsumer: KafkaConsumer<String, SykmeldingRecord>,
                                private val kafkaProducer: KafkaProducer<String, ReceivedSykmelding?>,
                                private val kafkaProducerManuellTIlbakedatering: KafkaProducer<String, ManuellOppgave>,
                                private val tsmSykmeldingerTopic: String,
                                private val okSykmeldingTopic: String,
                                private val manuellBehanldingTopic: String,
                                private val cluster: String,
    ) {

    companion object {
        private val log = LoggerFactory.getLogger(DigitalSykmeldingConsumer::class.java)
    }

    private val objectMapper = jacksonObjectMapper()

    suspend fun start() = coroutineScope {
        while (isActive) {
            try {
                consumeMessages()
            } catch (e: CancellationException) {
                log.info("Consumer cancelled")
            } catch (ex: Exception) {
                log.error("Error processing messages from kafka delaying 60 seconds to tray again", ex)
                kafkaConsumer.unsubscribe()
                delay(60_000)
            }
        }
    }

    suspend fun consumeMessages() = coroutineScope {
        kafkaConsumer.subscribe(listOf(tsmSykmeldingerTopic))
        while (isActive) {
            val records = kafkaConsumer.poll(10.seconds.toJavaDuration())
            processRecords(records)
        }
    }

    private fun processRecords(records: ConsumerRecords<String, SykmeldingRecord>) {
        records.mapNotNull { record -> record.value()?.let { it to record } }.map { (sykmeldingRecord, kafkaRecord) ->
            try {
                if (sykmeldingRecord.sykmelding.type == SykmeldingType.DIGITAL) {
                    log.info("received digital sykmelding should send to old topics ${sykmeldingRecord.sykmelding.id}")
                    val receivedSykmelding = sykmeldingRecord.toReceivedSykmelding()
                    if(sykmeldingRecord.validation.rules.any { it.validationType == ValidationType.MANUAL }) {
                        log.info("Got digital sykmelding with Manual validation ${sykmeldingRecord.sykmelding.id}, should not be sendt to old topic")
                    } else if (sykmeldingRecord.validation.status == RuleType.PENDING && sykmeldingRecord.validation.rules.singleOrNull{ it.name == TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name } != null) {
                        log.info("Got digital sykmelidng with pending result, should be sent to manual behandling ${sykmeldingRecord.sykmelding.id}")
                        val producerRecord = ProducerRecord(manuellBehanldingTopic, sykmeldingRecord.sykmelding.id, ManuellOppgave(
                            receivedSykmelding = receivedSykmelding,
                            validationResult = receivedSykmelding.validationResult
                        ))
                        kafkaProducerManuellTIlbakedatering.send(producerRecord).get()
                    } else {
                        log.info("Got digital sykmelding that should be synced over to old topics ${sykmeldingRecord.sykmelding.id}")
                        val producerRecord = ProducerRecord(okSykmeldingTopic, sykmeldingRecord.sykmelding.id, receivedSykmelding)
                        kafkaProducer.send(producerRecord).get()
                    }
                }
            } catch (mappingException: MappingException) {
                log.error("error processing sykmelding ${mappingException.receivedSykmelding.sykmelding.id}, for p: ${kafkaRecord.partition()}, o: ${kafkaRecord.offset()}", mappingException)
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
