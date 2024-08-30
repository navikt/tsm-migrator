package no.nav.tsm.nysykmelding

import no.nav.tsm.smregister.models.Behandler


class SykmeldingMedBahndlingsutfall(
    val sykmelding: NySykmelding
)
class Navn(
    val fornavn: String,
    val etternavn: String
)
class Pasient (
    val ident: String,
    val navn: Navn
)

enum class ARBEIDSGIVER_TYPE {
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    INGEN_ARBEIDSGIVER,
}
sealed interface ArbeidsgiverInfo {
    val meldingTilArbeidsgiver: String?
    val tiltakArbeidsplassen: String?
    val type: ARBEIDSGIVER_TYPE
}

sealed class EnArbeidsgiver(
) : ArbeidsgiverInfo {
    override val type = ARBEIDSGIVER_TYPE.EN_ARBEIDSGIVER
}

sealed class FlereArbeidsgivere(
    val navn : String,
    val yrkesbetegnelse: String,
    val stillingsprosent: Int?
) : ArbeidsgiverInfo {
    override val type = ARBEIDSGIVER_TYPE.FLERE_ARBEIDSGIVERE
}

sealed class IngenArbeidsgiver(
) : ArbeidsgiverInfo {
    override val type = ARBEIDSGIVER_TYPE.INGEN_ARBEIDSGIVER
}
enum class DiagnoseSystem {
    ICPC2,
    ICD10,
}
class DiagnoseInfo(
    val system: DiagnoseSystem,
    val kode: String,
    val tekst: String,
)
class Diagnose(
    val hovedDiagnose: DiagnoseInfo,
    val biDiagnose: List<DiagnoseInfo>,
    val annenFravarsArsak: AnnenFraverArsak
)

class ArsakType(
    val beskrivelse: String,
    val kode: String
)


class AnnenFraverArsak(
    val beskrivelse: String?,
    val arsak: ArsakType
)
class Adresse(
    val gateAdresse: String,
    val postnummer: String,
    val poststed: String,
    val land: String?
)
class Kontaktinfo(
    val type: String,
    val verdi: String
)
class Behandler(
    val navn: Navn,
    val ident: String,
    val adresse: Adresse,
    val kontaktInfo: List<Kontaktinfo>
)

class SykmeldingMetadata (
    val regelsettVersjon: String,
    val mottattDato: String,
    val partnerreferanse: String,

)

class NySykmelding(
    val sykmeldingMetadata: SykmeldingMetadata,
    val id: String,
    val arbeidsgiver: ArbeidsgiverInfo,
    val diagnose: Diagnose,
    val pasient: Pasient,
    val behandler: Behandler,

)
