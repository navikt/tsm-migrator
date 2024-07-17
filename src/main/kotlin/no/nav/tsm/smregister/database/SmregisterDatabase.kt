package no.nav.tsm.smregister.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.smregister.database.tables.Behandlingsutfall
import no.nav.tsm.smregister.database.tables.Sykmeldingsdokument
import no.nav.tsm.smregister.database.tables.Sykmeldingsopplysning
import no.nav.tsm.smregister.database.tables.Sykmeldingstatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory


class SmregisterDatabase(private val database: Database) {

    companion object {
        private val logger = LoggerFactory.getLogger(SmregisterDatabase::class.java)
    }
    fun getSykmelding(id: String): String {
        return transaction(database) {
            val stm = connection.prepareStatement("select id from sykmeldingsopplysninger where id = ?", false)
            stm[1] = id
            val rs = stm.executeQuery()
            if (rs.next()) {
                val idFromDb = rs.getString(1)
                logger.info("got id from database $idFromDb")
                id
            } else {
                "not found"
            }
        }
    }

    suspend fun getFullSykmelding(sykmeldingId: String): String = withContext(Dispatchers.IO) {
        transaction(database) {
            try {
                val result = Sykmeldingsopplysning
                    .innerJoin(Sykmeldingsdokument, onColumn = { id eq Sykmeldingsdokument.id })
                    .innerJoin(Behandlingsutfall,
                        onColumn = { Sykmeldingsopplysning.id eq Behandlingsutfall.id } ).selectAll()
                    .where { Sykmeldingsopplysning.id eq sykmeldingId }
                    .andWhere {
                        notExists(
                            Sykmeldingstatus.select(Sykmeldingstatus.sykmeldingId).where {
                                Sykmeldingstatus.sykmeldingId eq sykmeldingId
                            }.andWhere { Sykmeldingstatus.event eq "SLETTET" }
                        )
                    }.map {
                        val sykmeldingId = it[Sykmeldingsopplysning.id]
                        val behandlingsutfallId = it[Behandlingsutfall.id]
                        val sykmelidngsDokumentID = it[Sykmeldingsdokument.id]
                        mapOf(
                            "sykmeldingsopplysninger" to sykmeldingId,
                            "sykmeldingsdokument" to sykmelidngsDokumentID,
                            "behandlingsutfall" to behandlingsutfallId
                        )
                    }
                val sykmeldingsInfo = result.firstOrNull()
                if (sykmeldingsInfo != null) {
                    logger.info("Got sykmelding: $sykmeldingsInfo")
                } else {
                    logger.info("Sykmelding not found: $sykmeldingId")
                }
                "Sykmelding: $sykmeldingId"
            } catch (ex: Exception) {
                logger.error("Error getting sykmelding with id $sykmeldingId")
                "Error getting sykmelding with id $sykmeldingId"
            }

        }
    }
}
