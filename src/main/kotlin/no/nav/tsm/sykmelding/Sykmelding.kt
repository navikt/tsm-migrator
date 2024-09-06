package no.nav.tsm.sykmelding

import no.nav.tsm.smregister.models.AvsenderSystem
import java.time.LocalDate
import java.time.OffsetDateTime

data class SykmeldingMedBahndlingsutfall(
    val sykmelding: Sykmelding,
    val validation: ValidationResult
)

data class Sykmelding(
    val id: String,
    val metadata: SykmeldingMetadata,
    val pasient: Person,
    val arbeidsgiver: ArbeidsgiverInfo,
    val behandler: Behandler,
    val medisinskVurdering: MedisinskVurdering,
    val prognose: Prognose,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val aktivitet: List<Aktivitet>,
    val tilbakedatering: Tilbakedatering?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>,
)

data class SykmeldingMetadata(
    val msgId: String,
    val regelsettVersjon: String,
    val partnerreferanse: String,
    val avsenderSystem: AvsenderSystem,
    val mottattDato: OffsetDateTime,
    val genDate: OffsetDateTime,
    val behandletTidspunkt: OffsetDateTime,
)

data class Navn(
    val fornavn: String, val etternavn: String
)

data class Person(
    val ident: String, val navn: Navn
)


data class Adresse(
    val gateAdresse: String, val postnummer: String, val poststed: String, val land: String?
)

data class Kontaktinfo(
    val type: String, val verdi: String
)

data class Behandler(
    val person: Person,
    val adresse: Adresse,
    val kontaktInfo: List<Kontaktinfo>
)

data class BistandNav(
    val bistandUmiddelbart: Boolean,
    val beskrivBistand: String?,
)

data class Tiltak(
    val tiltakNAV: String?,
    val andreTiltak: String?,
)

data class Prognose(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
    val arbeid: IArbeid?,

    )

data class Tilbakedatering(
    val kontaktDato: LocalDate,
    val begrunnelse: String,
)

