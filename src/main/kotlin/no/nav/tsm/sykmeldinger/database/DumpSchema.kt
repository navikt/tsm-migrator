package no.nav.tsm.sykmeldinger.database

import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.kafka.model.SykmeldingInput
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class DumpService() {

    private object Sykmelding : Table() {

        val mottak_id = text("mottak_id")
        val mottattdato = datetime("mottattdato")

        override val primaryKey = PrimaryKey(mottak_id)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun insertDump(sykmeldingInput: SykmeldingInput): String = dbQuery {
        Sykmelding.insert {
            it[mottak_id] = sykmeldingInput.sykmeldingId
            it[mottattdato] = sykmeldingInput.mottattDato
        }[Sykmelding.mottak_id]
    }
}