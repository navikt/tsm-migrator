package no.nav.tsm.smregister.database.tables

import org.jetbrains.exposed.sql.Table

object Sykmeldingsdokument : Table("sykmeldingsdokumnet") {
    val id = text("id")
    val sykmelding = text("sykmelding")
    override val primaryKey = PrimaryKey(id)
}
