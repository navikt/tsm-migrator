package no.nav.tsm.smregister.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Sykmeldingsopplysning : Table("sykmeldingsopplysninger") {
    val id = text("id")
    val pasientFnr = text("pasient_fnr")
    val pasientAktoerId = text("pasient_aktoer_id")
    val legeFnr = text("lege_fnr")
    val legeAktorId = text("lege_aktoer_id")
    val mottakId = text("mottak_id")
    val legekontor_org_nr = text("legekontor_org_nr")
    val legekontorHerId = text("legekontor_her_id")
    val legekontorReshId = text("legekontor_resh_id")
    val epjSystemNavn = text("epj_system_navn")
    val epjSystemVersjon = text("epj_system_versjon")
    val mottattTidspunkt = timestampWithTimeZone("mottatt_tidspunkt")
    val tss_id = text("tss_id")
    val merknader = text("merknader").nullable()
    val partnerreferanse = text("partnerreferanse")
    val legeHpr = text("lege_hpr")
    val legeHelsepersonellKategori = text("lege_helsepersonellkategori")
    val utenlandskSykmelding = text("utenlandsk_sykmelding").nullable()
    override val primaryKey = PrimaryKey(id)
}
