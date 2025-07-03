package no.nav.tsm.digital

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.syfo.pdl.TsmPdlClient
import no.nav.tsm.reformat.sykmelding.service.MappingException
import no.nav.tsm.reformat.sykmelding.util.secureLog
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.smregister.models.ValidationResultLegacy
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmeldinger.kafka.util.SOURCE_NAMESPACE
import no.nav.tsm.sykmeldinger.kafka.util.TSM_SOURCE
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
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
                                private val tsmPdlClient: TsmPdlClient,
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

    private suspend fun processRecords(records: ConsumerRecords<String, SykmeldingRecord>) {
        records.forEach { record  ->
            try {
                val sykmeldingRecord = record.value()
                val sykmeldingId = record.key()
                val sourceNamespace = record.headers().lastHeader(SOURCE_NAMESPACE)?.value()?.toString(Charsets.UTF_8)
                val headers = record.headers()
                if (sourceNamespace == TSM_SOURCE) {
                    handleDigitalSykmelidng(sourceNamespace, sykmeldingRecord, sykmeldingId, headers)
                }
            } catch (mappingException: MappingException) {
                log.error("error processing sykmelding ${mappingException.receivedSykmelding.sykmelding.id}, for p: ${record.partition()}, o: ${record.offset()}", mappingException)
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

    private suspend fun handleDigitalSykmelidng(
        sourceNamespace: String?,
        sykmeldingRecord: SykmeldingRecord?,
        sykmeldingId: String?,
        headers: Headers
    ) {
        log.info("received sykmelding from source-namespace:$sourceNamespace, should sendt to namespace: teamsykmelding, sykmeldingId: $sykmeldingId")
        if (sykmeldingRecord == null) {
            log.info("tombstoning sykmelding with id: $sykmeldingId")
            kafkaProducer.send(ProducerRecord(okSykmeldingTopic, null, sykmeldingId, null, headers))
        } else {
            val aktorId = tsmPdlClient.getAktorId(sykmeldingRecord.sykmelding.pasient.fnr)
            val receivedSykmelding = sykmeldingRecord.toReceivedSykmelding(aktorId)
            if (isManualVurdering(sykmeldingRecord)) {
                log.info("Digital sykmelding is sendt to manuell behandling $sykmeldingId")
                val producerRecord = ProducerRecord(
                    manuellBehanldingTopic,
                    null,
                    sykmeldingRecord.sykmelding.id,
                    ManuellOppgave(
                        receivedSykmelding = receivedSykmelding,
                        validationResult = receivedSykmelding.validationResult
                    ),
                    headers
                )
                kafkaProducerManuellTIlbakedatering.send(producerRecord).get()
            } else {
                log.info("Digital sykmelding is sendt to old arc, sykmeldingId: $sykmeldingId")
                val producerRecord = ProducerRecord(
                    okSykmeldingTopic,
                    null,
                    sykmeldingRecord.sykmelding.id,
                    receivedSykmelding,
                    headers
                )
                kafkaProducer.send(producerRecord).get()
            }
        }
    }

    private fun isManualVurdering(sykmeldingRecord: SykmeldingRecord): Boolean {
        val hasPendingStatus = sykmeldingRecord.validation.status == RuleType.PENDING
        val hasOnlyOnePendingRule = sykmeldingRecord.validation.rules.size == 1
        val isTilbakedatertPending = sykmeldingRecord.validation.rules.any { it.name == TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name }

        return hasPendingStatus && hasOnlyOnePendingRule && isTilbakedatertPending
    }

}
