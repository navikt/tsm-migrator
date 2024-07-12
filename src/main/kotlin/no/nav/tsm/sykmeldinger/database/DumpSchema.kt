package no.nav.tsm.sykmeldinger.database

import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.kafka.model.HistoriskSykmeldingInput
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

class DumpService() {



    companion object {
        private val logger = LoggerFactory.getLogger(DumpService::class.java)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun batchInsert(records: List<HistoriskSykmeldingInput>): Boolean = dbQuery {
        val res = historiske_sykmeldinger.batchUpsert(records) { (sykmeldingId, mottattdato, receivedSykmelding, sykmeldingSource) ->
            this[historiske_sykmeldinger.sykmeldingId] = sykmeldingId
            this[historiske_sykmeldinger.mottattdato] = mottattdato
            this[historiske_sykmeldinger.receivedSykmelding] = receivedSykmelding
            this[historiske_sykmeldinger.sykmeldingSource] = sykmeldingSource
        }
        if(res.size == records.size) {
            //logger.info("resultrow size = ${res.size} and records size = ${records.size}")
            return@dbQuery true
        }
        return@dbQuery false
    }
}
