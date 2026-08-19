package no.nav.tsm.digital

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.tsm.ktor.clients.pdl.PdlClient
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.teamLogger
import no.nav.tsm.plugins.KafkaTopics
import no.nav.tsm.reformat.sykmelding.service.MappingException
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
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
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
                                private val tsmPdlClient: PdlClient,
                                private val cluster: RuntimeCluster,
    ) {
    private val log = logger()
    private val teamLog = teamLogger()
    private val objectMapper = jacksonMapperBuilder()
        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        .build()

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
        kafkaConsumer.subscribe(listOf(KafkaTopics.tsmSykmeldingTopic))
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
                teamLog.error(objectMapper.writeValueAsString(mappingException.receivedSykmelding))
                throw mappingException
            } catch (digitalMappingException: DigitalSykmeldingMapperException) {
                log.error("Error mapping sykmelding ${digitalMappingException.sykmelding.id}, ${digitalMappingException.message}")
                teamLog.error("Error in mapping, sykmelding:  ${objectMapper.writeValueAsString(digitalMappingException.sykmelding)}")
                log.info("cluster is $cluster")
                if(cluster == RuntimeCluster.DEV) {
                    log.warn("skipping record in dev")
                } else {
                    throw digitalMappingException
                }
            }
        }
    }

    private suspend fun handleDigitalSykmelidng(
        sourceNamespace: String,
        sykmeldingRecord: SykmeldingRecord?,
        sykmeldingId: String?,
        headers: Headers
    ) {
        log.info("received sykmelding from source-namespace:$sourceNamespace, should sendt to namespace: teamsykmelding, sykmeldingId: $sykmeldingId")
        if (sykmeldingRecord == null) {
            log.info("tombstoning sykmelding with id: $sykmeldingId")
            kafkaProducer.send(ProducerRecord(KafkaTopics.okSykmeldingTopic, null, sykmeldingId, null, headers)).get()
        } else {
            val aktorId = requireNotNull(tsmPdlClient.getAktorId(sykmeldingRecord.sykmelding.pasient.fnr)) {
                "Could not find aktorId for ident in sykmelding with id: $sykmeldingId"
            }
            val receivedSykmelding = sykmeldingRecord.toReceivedSykmelding(aktorId)
            if (isManualVurdering(sykmeldingRecord)) {
                log.info("Digital sykmelding is sendt to manuell behandling $sykmeldingId")
                val producerRecord = ProducerRecord(
                    KafkaTopics.manuellTilbakedateringTopic,
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
                    KafkaTopics.okSykmeldingTopic,
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
