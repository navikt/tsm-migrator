package no.nav.tsm.avro.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ReceivedSykmelding(
    val sykmelding: Sykmelding,
    val personNrPasient: String,
    val tlfPasient: String?,
    val personNrLege: String,
    val legeHelsepersonellkategori: String?,
    val legeHprNr: String?,
    val navLogId: String,
    val msgId: String?,
    val legekontorOrgNr: String?,
    val legekontorHerId: String?,
    val legekontorReshId: String?,
    val legekontorOrgName: String?,
    @Contextual
    val mottattDato: Instant,
    val rulesetVersion: String?,
    val merknader: List<Merknad>?,
    val partnerreferanse: String?,
    val vedlegg: List<String>?,
    val utenlandskSykmelding: UtenlandskInfo?,
    /**
     * Full fellesformat as a XML payload, this is only used for infotrygd compat and should be
     * removed in thefuture
     */
    val fellesformat: String?,
    /** TSS-ident, this is only used for infotrygd compat and should be removed in thefuture */
    val tssid: String?,
    val validationResult: ValidationResult,
)
