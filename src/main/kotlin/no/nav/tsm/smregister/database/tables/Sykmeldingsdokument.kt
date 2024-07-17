package no.nav.tsm.smregister.database.tables

import org.jetbrains.exposed.sql.Table

object Sykmeldingsdokument : Table("sykmeldingsdokument") {
    val id = text("id")
    val sykmelding = text("sykmelding")
    override val primaryKey = PrimaryKey(id)
}
