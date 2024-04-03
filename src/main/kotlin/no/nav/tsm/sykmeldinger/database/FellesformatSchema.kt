package no.nav.tsm.sykmeldinger.database

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.kafka.model.FellesformatInput
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import kotlin.math.log

class FellesformatService() {

    private object Sykmelding : Table() {

        val sykmelding_id = text("sykmelding_id")
        val fellesformat = text("fellesformat")

        override val primaryKey = PrimaryKey(sykmelding_id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FellesformatService::class.java)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun batchUpsert(records: List<FellesformatInput>): Boolean = dbQuery {
        val res = Sykmelding.batchUpsert(records) { (sykmelding, fellesformat) ->
            this[Sykmelding.sykmelding_id] = getSykmeldingId(sykmelding)
            this[Sykmelding.fellesformat] = fellesformat
        }
        if(res.size == records.size) {
            logger.info("FellesformatService.batchUpsert: resultrow size = ${res.size} and records size = ${records.size}")
            return@dbQuery true
        }
        logger.info("FellesformatService.batchUpsert: resultrow size = ${res.size} and records size = ${records.size}")
        return@dbQuery false
    }

    private fun getSykmeldingId(sykmelding: String): String {
        val asText = ObjectMapper().readTree(sykmelding).get("id").asText()
        logger.info("FellesformatService.getSykmeldingId: asText = $asText")
        return asText
    }


}