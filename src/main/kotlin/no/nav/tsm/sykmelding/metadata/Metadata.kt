package no.nav.tsm.sykmelding.metadata

import java.time.OffsetDateTime

enum class MetadataType {
    EMOTTAK,
    EMOTTAK_ENKEL,
}

sealed interface Meldingsinformasjon {
    val type: MetadataType
    val vedlegg: List<String>?
}

data class EmottakEnkel(
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.EMOTTAK_ENKEL
}

data class EDIEmottak(
    val msgInfo: MeldingMetadata,
    val mottakenhetBlokk: MottakenhetBlokk,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val pasient: Pasient?,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.EMOTTAK
}

enum class Meldingstype {
    SYKMELDING;

    companion object {
        fun parse(v: String): Meldingstype = when (v) {
            "SYKMELD" -> SYKMELDING
            else -> throw IllegalArgumentException("Ukjent meldingstype: $v")
        }
    }
}


data class MeldingMetadata(
    val type: Meldingstype,
    val genDate: OffsetDateTime,
    val msgId: String,
    val migVersjon: String?,
)

data class MottakenhetBlokk(
    val ediLogid: String,
    val avsender: String,
    val ebXMLSamtaleId: String,
    val mottaksId: String,
    val meldingsType: String,
    val avsenderRef: String,
    val avsenderFnrFraDigSignatur: String,
    val mottattDato: OffsetDateTime,
    val orgnummer: String,
    val avsenderOrgNrFraDigSignatur: String,
    val partnerReferanse: String,
    val herIdentifikator: String,
    val ebRole: String,
    val ebService: String,
    val ebAction: String,
)
