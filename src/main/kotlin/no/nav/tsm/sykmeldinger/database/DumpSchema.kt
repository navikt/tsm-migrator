package no.nav.tsm.sykmeldinger.database

import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.kafka.DumpConsumer
import no.nav.tsm.sykmeldinger.kafka.model.SykmeldingInput
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

class DumpService() {

    private object Sykmelding : Table() {

        val mottak_id = text("mottak_id")
        val mottattdato = datetime("mottattdato")

        override val primaryKey = PrimaryKey(mottak_id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DumpService::class.java)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun insertDump(sykmeldingInput: SykmeldingInput): Boolean = dbQuery {
        try {
            val exists = Sykmelding.select( Sykmelding.mottak_id eq sykmeldingInput.sykmeldingId).singleOrNull() != null
            if (!exists) {
                Sykmelding.insert {
                    it[mottak_id] = sykmeldingInput.sykmeldingId
                    it[mottattdato] = sykmeldingInput.mottattDato
                }
            }
            true
        } catch (e: Exception){
            logger.info("Exception occurred while inserting batch update into Sykmelding database ${e}")
            false
        }
    }

    suspend fun batchInsert(records: List<SykmeldingInput>): Boolean = dbQuery {
        val res = Sykmelding.batchInsert(records) { (mottakId, mottattdato) ->
            this[Sykmelding.mottak_id] = mottakId
            this[Sykmelding.mottattdato] = mottattdato
        }
        if(res.size == records.size) {
            logger.info("resultrow size = ${res.size} and records size = ${records.size}")
            return@dbQuery true
        }
        return@dbQuery false
    }
}