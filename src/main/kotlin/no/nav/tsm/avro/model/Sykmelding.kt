package no.nav.tsm.avro.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import no.nav.tsm.smregister.models.SporsmalSvar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class Sykmelding(
    val id: String, // yes
    val msgId: String, // yes
    val pasientAktoerId: String, // fjernes
    val medisinskVurdering: MedisinskVurdering, // yes
    val skjermesForPasient: Boolean, // yes
    val arbeidsgiver: Arbeidsgiver, // yes
    val perioder: List<Periode>, // yes
    val prognose: Prognose?, // yes
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>, // yes
    val tiltakArbeidsplassen: String?, // yes
    val tiltakNAV: String?, // yes
    val andreTiltak: String?, // yes
    val meldingTilNAV: MeldingTilNAV?, // YES
    val meldingTilArbeidsgiver: String?, // YES
    val kontaktMedPasient: KontaktMedPasient, // YES
    @Contextual
    val behandletTidspunkt: Instant, // YES
    val behandler: Behandler, // YES
    val avsenderSystem: AvsenderSystem, // YES
    @Contextual
    val syketilfelleStartDato: LocalDate?,
    @Contextual
    val signaturDato: Instant,
    val navnFastlege: String?
)
@Serializable
data class MedisinskVurdering(
    val hovedDiagnose: Diagnose?,
    val biDiagnoser: List<Diagnose>,
    val svangerskap: Boolean,
    val yrkesskade: Boolean,
    @Contextual
    val yrkesskadeDato: LocalDate?,
    val annenFraversArsak: AnnenFraversArsak?
)
@Serializable
data class Diagnose(val system: String, val kode: String, val tekst: String?)
@Serializable
data class AnnenFraversArsak(val beskrivelse: String?, val grunn: List<AnnenFraverGrunn>)
@Serializable
data class Arbeidsgiver(
    val harArbeidsgiver: HarArbeidsgiver,
    val navn: String?,
    val yrkesbetegnelse: String?,
    val stillingsprosent: Int?
)

enum class HarArbeidsgiver(
    val codeValue: String,
    val text: String,
    val oid: String = "2.16.578.1.12.4.1.1.8130"
) {
    EN_ARBEIDSGIVER("1", "Én arbeidsgiver"),
    FLERE_ARBEIDSGIVERE("2", "Flere arbeidsgivere"),
    INGEN_ARBEIDSGIVER("3", "Ingen arbeidsgiver")
}
@Serializable
data class Periode(
    @Contextual
    val fom: LocalDate,
    @Contextual
    val tom: LocalDate,
    val aktivitetIkkeMulig: AktivitetIkkeMulig?,
    val avventendeInnspillTilArbeidsgiver: String?,
    val behandlingsdager: Int?,
    val gradert: Gradert?,
    val reisetilskudd: Boolean
)
@Serializable
data class AktivitetIkkeMulig(
    val medisinskArsak: MedisinskArsak?,
    val arbeidsrelatertArsak: ArbeidsrelatertArsak?
)
@Serializable
data class ArbeidsrelatertArsak(
    val beskrivelse: String?,
    val arsak: List<ArbeidsrelatertArsakType>
)
@Serializable
data class MedisinskArsak(val beskrivelse: String?, val arsak: List<MedisinskArsakType>)

enum class ArbeidsrelatertArsakType(
    val codeValue: String,
    val text: String,
    val oid: String = "2.16.578.1.12.4.1.1.8132"
) {
    MANGLENDE_TILRETTELEGGING("1", "Manglende tilrettelegging på arbeidsplassen"),
    ANNET("9", "Annet")
}

enum class MedisinskArsakType(
    val codeValue: String,
    val text: String,
    val oid: String = "2.16.578.1.12.4.1.1.8133"
) {
    TILSTAND_HINDRER_AKTIVITET("1", "Helsetilstanden hindrer pasienten i å være i aktivitet"),
    AKTIVITET_FORVERRER_TILSTAND("2", "Aktivitet vil forverre helsetilstanden"),
    AKTIVITET_FORHINDRER_BEDRING("3", "Aktivitet vil hindre/forsinke bedring av helsetilstanden"),
    ANNET("9", "Annet")
}
@Serializable
data class Gradert(val reisetilskudd: Boolean, val grad: Int)
@Serializable
data class Prognose(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
    val erIArbeid: ErIArbeid?,
    val erIkkeIArbeid: ErIkkeIArbeid?
)
@Serializable
data class ErIArbeid(
    val egetArbeidPaSikt: Boolean,
    val annetArbeidPaSikt: Boolean,
    @Contextual
    val arbeidFOM: LocalDate?,
    @Contextual
    val vurderingsdato: LocalDate?
)
@Serializable
data class ErIkkeIArbeid(
    val arbeidsforPaSikt: Boolean,
    @Contextual
    val arbeidsforFOM: LocalDate?,
    @Contextual
    val vurderingsdato: LocalDate?
)
@Serializable
data class MeldingTilNAV(val bistandUmiddelbart: Boolean, val beskrivBistand: String?)
@Serializable
data class KontaktMedPasient(@Contextual val kontaktDato: LocalDate?, val begrunnelseIkkeKontakt: String?)
@Serializable
data class Behandler(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val aktoerId: String,
    val fnr: String,
    val hpr: String?,
    val her: String?,
    val adresse: Adresse,
    val tlf: String?
)
@Serializable
data class Adresse(
    val gate: String?,
    val postnummer: Int?,
    val kommune: String?,
    val postboks: String?,
    val land: String?
)
@Serializable
data class AvsenderSystem(val navn: String, val versjon: String)

enum class AnnenFraverGrunn(
    val codeValue: String,
    val text: String,
) {
    GODKJENT_HELSEINSTITUSJON("1", "Når vedkommende er innlagt i en godkjent helseinstitusjon"),
    BEHANDLING_FORHINDRER_ARBEID(
        "2",
        "Når vedkommende er under behandling og legen erklærer at behandlingen gjør det nødvendig at vedkommende ikke arbeider"
    ),
    ARBEIDSRETTET_TILTAK("3", "Når vedkommende deltar på et arbeidsrettet tiltak"),
    MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND(
        "4",
        "Når vedkommende på grunn av sykdom, skade eller lyte får tilskott når vedkommende på grunn av sykdom, skade eller lyte får tilskott"
    ),
    NODVENDIG_KONTROLLUNDENRSOKELSE(
        "5",
        "Når vedkommende er til nødvendig kontrollundersøkelse som krever minst 24 timers fravær, reisetid medregnet"
    ),
    SMITTEFARE(
        "6",
        "Når vedkommende myndighet har nedlagt forbud mot at han eller hun arbeider på grunn av smittefare"
    ),
    ABORT("7", "Når vedkommende er arbeidsufør som følge av svangerskapsavbrudd"),
    UFOR_GRUNNET_BARNLOSHET(
        "8",
        "Når vedkommende er arbeidsufør som følge av behandling for barnløshet"
    ),
    DONOR("9", "Når vedkommende er donor eller er under vurdering som donor"),
    BEHANDLING_STERILISERING(
        "10",
        "Når vedkommende er arbeidsufør som følge av behandling i forbindelse med sterilisering"
    )
}
