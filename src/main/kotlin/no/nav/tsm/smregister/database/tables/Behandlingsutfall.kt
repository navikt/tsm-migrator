package no.nav.tsm.smregister.database.tables

import org.jetbrains.exposed.sql.Table

object Behandlingsutfall : Table("behandlingsutfall") {
    val id = text("id")
    val behandlingsutfall = text("behandlingsutfall")
}
