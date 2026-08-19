package no.nav.tsm.migrator.reformat

import no.nav.tsm.ktor.kafka.consumer.RecordMeta
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingInputProducer
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.teamLogger
import no.nav.tsm.migrator.legacy.ReceivedSykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingType
import no.nav.tsm.kafka.SOURCE_APP
import no.nav.tsm.kafka.SOURCE_NAMESPACE
import no.nav.tsm.plugins.Environment
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val objectMapper: ObjectMapper = jacksonMapperBuilder()
    .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
    .build()

class SykmeldingReformatService(
    private val inputProducer: SykmeldingInputProducer,
    private val env: Environment,
) {
    private val log = logger()
    private val teamLog = teamLogger()

    fun handleRecord(record: ReceivedSykmelding?, meta: RecordMeta) {
        try {
            val sykmeldingRecord = record?.let { SykmeldingMapper.toNewSykmelding(it) }
            val namespaceFromHeader = meta.headers.lastHeader(SOURCE_NAMESPACE)?.value()?.toString(Charsets.UTF_8)
            val appFromHeader = meta.headers.lastHeader(SOURCE_APP)?.value()?.toString(Charsets.UTF_8)

            if (namespaceFromHeader == null || appFromHeader == null) {
                log.warn("Missing source namespace or app header for sykmelding with id: ${meta.key}, ")
            }
            val sourceNamespace = namespaceFromHeader ?: "teamsykmelding"
            val sourceApp = appFromHeader ?: getSourceAppFromSykmelding(sykmeldingRecord)
            val additionalHeaders = meta.headers
                .associate { it.key() to it.value().toString(Charsets.UTF_8) }
                .filter { it.key != SOURCE_NAMESPACE && it.key != SOURCE_APP }
            val sourceIsTsm = sourceNamespace == "tsm"

            log.info(
                "received sykmelding namespace: $sourceNamespace, app: $sourceApp, headers: ${
                    objectMapper.writeValueAsString(
                        additionalHeaders
                    )
                }, key: ${meta.key}"
            )
            if (sourceIsTsm) {
                log.info("skipping sykmelding from $sourceNamespace : $sourceApp: ${meta.key}")
            } else {
                when (sykmeldingRecord) {
                    null -> {
                        log.info("Tombstoning ${meta.key} from $sourceNamespace : $sourceApp on tsm.sykmeldinger-input")
                        inputProducer.tombstone(meta.key, sourceApp, sourceNamespace, additionalHeaders)
                    }
                    else -> {
                        log.info("Sending sykmelding ${sykmeldingRecord.sykmelding.id} from $sourceNamespace : $sourceApp on tsm.sykmeldinger-input")
                        inputProducer.send(sykmeldingRecord, sourceApp, sourceNamespace, additionalHeaders)
                    }
                }
            }
        } catch (mappingException: MappingException) {
            log.error(
                "error processing sykmelding ${mappingException.receivedSykmelding.sykmelding.id} for p: ${meta.partition} at offset: ${meta.offset}",
                mappingException
            )

            if (env.runtime.env != RuntimeCluster.DEV) {
                teamLog.error(objectMapper.writeValueAsString(mappingException.receivedSykmelding))
                throw mappingException
            }
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            if (env.runtime.env != RuntimeCluster.DEV) {
                throw ex
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

