package no.nav.tsm.reformat.sykmelding

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingInputProducer
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.teamLogger
import no.nav.tsm.reformat.sykmelding.service.MappingException
import no.nav.tsm.reformat.sykmelding.service.SykmeldingMapper
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingType
import no.nav.tsm.sykmeldinger.kafka.util.SOURCE_APP
import no.nav.tsm.sykmeldinger.kafka.util.SOURCE_NAMESPACE
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val objectMapper = jacksonMapperBuilder()
    .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
    .build()


class SykmeldingReformatService(
    private val kafkaConsumer: KafkaConsumer<String, ReceivedSykmelding>,
    private val sykmeldingMapper: SykmeldingMapper,
    private val kafkaProducer: SykmeldingInputProducer,
    private val inputTopic: String,
    private val cluster: RuntimeCluster,
) {
    private val log = logger()
    private val teamLog = teamLogger()

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
                val sykmeldingRecord = record.value()?.let { sykmeldingMapper.toNewSykmelding(it) }
                val namespaceFromHeader =
                    record.headers().lastHeader(SOURCE_NAMESPACE)?.value()?.toString(Charsets.UTF_8)
                val appFromHeader = record.headers().lastHeader(SOURCE_APP)?.value()?.toString(Charsets.UTF_8)

                if (namespaceFromHeader == null || appFromHeader == null) {
                    log.warn("Missing source namespace or app header for sykmelding with id: ${record.key()}, ")
                }
                val sourceNamespace = namespaceFromHeader ?: "teamsykmelding"
                val sourceApp = appFromHeader ?: getSourceAppFromSykmelding(sykmeldingRecord)

                val additionalHeaders = record.headers().associate { it.key() to it.value().toString(Charsets.UTF_8) }
                    .filter { it.key != SOURCE_NAMESPACE && it.key != SOURCE_APP }
                val sourceIsTsm = sourceNamespace == "tsm"

                log.info(
                    "received sykmelding namespace: $sourceNamespace, app: $sourceApp, headers: ${
                        objectMapper.writeValueAsString(
                            additionalHeaders
                        )
                    }, key: ${record.key()}"
                )
                if (sourceIsTsm) {
                    log.info("skipping sykmelding from $sourceNamespace : $sourceApp: ${record.key()}")
                } else {
                    when (sykmeldingRecord) {
                        null -> kafkaProducer.tombstone(
                            record.key(),
                            sourceApp = sourceApp,
                            sourceNamespace = sourceNamespace,
                            additionalHeaders = additionalHeaders
                        )

                        else -> kafkaProducer.send(
                            sykmeldingRecord,
                            sourceApp = sourceApp,
                            sourceNamespace = sourceNamespace,
                            additionalHeaders = additionalHeaders
                        )
                    }
                }
            } catch (mappingException: MappingException) {
                log.error(
                    "error processing sykmelding ${mappingException.receivedSykmelding.sykmelding.id} for p: ${record.partition()} at offset: ${record.offset()}",
                    mappingException
                )

                if (cluster != RuntimeCluster.DEV) {
                    teamLog.error(objectMapper.writeValueAsString(mappingException.receivedSykmelding))
                    throw mappingException
                }
            } catch (ex: Exception) {
                log.error(ex.message, ex)
                if (cluster != RuntimeCluster.DEV) {
                    throw ex
                }
            }
        }
    }

    private fun getSourceAppFromSykmelding(sykmeldingRecord: SykmeldingRecord?): String {
        return when (sykmeldingRecord?.sykmelding?.type) {
            SykmeldingType.DIGITAL -> throw RuntimeException("Digital sykmelding should have source set in header")
            SykmeldingType.XML -> "syfosmmottak"
            SykmeldingType.PAPIR -> "syfosmpapirmottak"
            SykmeldingType.UTENLANDSK -> "syk-dig-backend"
            null -> "deleted"
        }
    }
}

