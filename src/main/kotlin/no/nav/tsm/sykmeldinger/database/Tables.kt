package no.nav.tsm.sykmeldinger.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object historiske_sykmeldinger : Table() {

    val sykmeldingId = text("sykmelding_id")
    val mottattdato = datetime("mottattdato")
    val receivedSykmelding = text("receivedsykmelding").nullable()
    val sykmeldingSource = text("source")
    override val primaryKey = PrimaryKey(sykmeldingId)
}
