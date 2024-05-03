package no.nav.tsm.sykmeldinger.database

import kotlinx.coroutines.Dispatchers
import no.nav.tsm.sykmeldinger.kafka.model.GamleSykmeldingerInput
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GamleSykmeldingerService() {

    private object Sykmelding : Table() {

        val sykmelding_id = text("sykmelding_id")
        val mottattdato = datetime("mottattdato")
        val gammelSykmelding = text("gammel_sykmelding")

        override val primaryKey = PrimaryKey(sykmelding_id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GamleSykmeldingerService::class.java)
        val securelog: Logger = LoggerFactory.getLogger("securelog")
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun batchUpsert(records: List<GamleSykmeldingerInput>): Boolean = dbQuery {
        val res = Sykmelding.batchUpsert(records) { (sykmeldingId, mottattDato, sykmelding) ->
            securelog.info("GamleSykmeldingerService.batchUpsert: sykmeldingId = $sykmeldingId, mottattDato = $mottattDato, sykmelding = $sykmelding")
            this[Sykmelding.sykmelding_id] = sykmeldingId
            this[Sykmelding.mottattdato] = mottattDato
            this[Sykmelding.gammelSykmelding] = sykmelding
        }
        if(res.size == records.size) {
            logger.info("GamleSykmeldingerService.batchUpsert: resultrow size = ${res.size} and records size = ${records.size}")
            return@dbQuery true
        }
        logger.info("GamleSykmeldingerService.batchUpsert: res.size != records.size, res.size = ${res.size} and records.size = ${records.size}")
        return@dbQuery false
    }


}