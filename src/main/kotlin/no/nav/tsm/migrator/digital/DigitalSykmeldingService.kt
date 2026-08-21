package no.nav.tsm.migrator.digital

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.ktor.clients.pdl.PdlClient
import no.nav.tsm.ktor.kafka.consumer.RecordMeta
import no.nav.tsm.ktor.kafka.producer.KafkaRecordProducer
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.teamLogger
import no.nav.tsm.migrator.reformat.MappingException
import no.nav.tsm.migrator.legacy.ReceivedSykmelding
import no.nav.tsm.migrator.legacy.ValidationResultLegacy
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.kafka.SOURCE_NAMESPACE
import no.nav.tsm.kafka.TSM_SOURCE
import no.nav.tsm.plugins.Environment
import org.apache.kafka.common.header.Headers
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder

data class ManuellOppgave(
    val receivedSykmelding: ReceivedSykmelding,
    val validationResult: ValidationResultLegacy,
)

class DigitalSykmeldingService(
    private val okSykmeldingProducer: KafkaRecordProducer<ReceivedSykmelding>,
    private val manuellTilbakedateringProducer: KafkaRecordProducer<ManuellOppgave>,
    private val tsmPdlClient: PdlClient,
    private val env: Environment,
) {
    private val log = logger()
    private val teamLog = teamLogger()
    private val objectMapper = jacksonMapperBuilder()
        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        .build()

    suspend fun handleRecord(record: SykmeldingRecord, meta: RecordMeta) {
        try {
            val sykmeldingId = meta.key
            val sourceNamespace = meta.headers.lastHeader(SOURCE_NAMESPACE)?.value()?.toString(Charsets.UTF_8)
            val headers = meta.headers
            if (sourceNamespace == TSM_SOURCE) {
                handleDigitalSykmelding(sourceNamespace, record, sykmeldingId, headers)
            }
        } catch (mappingException: MappingException) {
            log.error(
                "error processing sykmelding ${mappingException.receivedSykmelding.sykmelding.id}, for p: ${meta.partition}, o: ${meta.offset}",
                mappingException
            )
            teamLog.error(objectMapper.writeValueAsString(mappingException.receivedSykmelding))
            throw mappingException
        } catch (digitalMappingException: DigitalSykmeldingMapperException) {
            log.error("Error mapping sykmelding ${digitalMappingException.sykmelding.id}, ${digitalMappingException.message}")
            teamLog.error("Error in mapping, sykmelding:  ${objectMapper.writeValueAsString(digitalMappingException.sykmelding)}")
            log.info("cluster is ${env.runtime.env}")
            if (env.runtime.env == RuntimeCluster.DEV) {
                log.warn("skipping record in dev")
            } else {
                throw digitalMappingException
            }
        }
    }

    suspend fun handleTombstone(meta: RecordMeta) {
        log.info("tombstoning sykmelding with id: ${meta.key}")
        withContext(Dispatchers.IO) {
            val sourceNamespace = meta.headers.lastHeader(SOURCE_NAMESPACE)?.value()?.toString(Charsets.UTF_8)
            if (sourceNamespace == TSM_SOURCE) {
                log.info("tombstoning sykmelding with id: ${meta.key} from $sourceNamespace")
                okSykmeldingProducer.tombstone(meta.key, meta.headers)
            } else {
                log.info("do not tombstone sykmelding with id: ${meta.key} source is ${sourceNamespace}")
            }
        }
    }

    private suspend fun handleDigitalSykmelding(
        sourceNamespace: String,
        sykmeldingRecord: SykmeldingRecord,
        sykmeldingId: String?,
        headers: Headers
    ) {
        log.info("received sykmelding from source-namespace:$sourceNamespace, should sendt to namespace: teamsykmelding, sykmeldingId: $sykmeldingId")
        val aktorId = requireNotNull(tsmPdlClient.getAktorId(sykmeldingRecord.sykmelding.pasient.fnr)) {
            "Could not find aktorId for ident in sykmelding with id: $sykmeldingId"
        }
        val receivedSykmelding = sykmeldingRecord.toReceivedSykmelding(aktorId)
        if (isManualVurdering(sykmeldingRecord)) {
            log.info("Digital sykmelding is sendt to manuell behandling $sykmeldingId")
            withContext(Dispatchers.IO) {
                manuellTilbakedateringProducer.send(
                    key = sykmeldingRecord.sykmelding.id, value = ManuellOppgave(
                        receivedSykmelding = receivedSykmelding,
                        validationResult = receivedSykmelding.validationResult
                    ),
                    headers = headers,
                )
            }
        } else {
            log.info("Digital sykmelding is sendt to old arc, sykmeldingId: $sykmeldingId")
            withContext(Dispatchers.IO) {
                okSykmeldingProducer.send(
                    key = sykmeldingRecord.sykmelding.id,
                    value = receivedSykmelding,
                    headers = headers,
                )
            }
        }
    }

    private fun isManualVurdering(sykmeldingRecord: SykmeldingRecord): Boolean {
        val hasPendingStatus = sykmeldingRecord.validation.status == RuleType.PENDING
        val hasOnlyOnePendingRule = sykmeldingRecord.validation.rules.size == 1
        val isTilbakedatertPending =
            sykmeldingRecord.validation.rules.any { it.name == TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name }

        return hasPendingStatus && hasOnlyOnePendingRule && isTilbakedatertPending
    }
}
