package no.nav.tsm.smregister

import org.jetbrains.exposed.sql.Database
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
}
