package no.nav.tsm.smregister.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Sykmeldingstatus : Table("sykmeldingstatus") {
    val sykmeldingId     = text("sykmelding_id")
    val event = text("event")
    val timestamp = timestampWithTimeZone("timestamp")
    override val primaryKey = PrimaryKey(sykmeldingId, timestamp)
}
