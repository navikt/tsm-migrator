package no.nav.tsm.sykmelding

import no.nav.tsm.sykmelding.metadata.Adresse
import no.nav.tsm.sykmelding.metadata.Helsepersonell
import no.nav.tsm.sykmelding.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.metadata.Kontaktinfo
import no.nav.tsm.sykmelding.metadata.Meldingsinformasjon
import no.nav.tsm.sykmelding.metadata.Navn
import no.nav.tsm.sykmelding.metadata.PersonId
import no.nav.tsm.sykmelding.validation.ValidationResult
import java.time.LocalDate
import java.time.OffsetDateTime


data class SykmeldingMedBehandlingsutfall(
    val meldingsInformasjon: Meldingsinformasjon,
    val sykmelding: Sykmelding,
    val validation: ValidationResult,
)

data class Pasient(
    val navn: Navn?,
    val navKontor: String?,
    val navnFastlege: String?,
    val fnr: String,
    val kontaktinfo: List<Kontaktinfo>,
)

data class Behandler(
    val navn: Navn,
    val adresse: Adresse?,
    val ids: List<PersonId>,
    val kontaktinfo: List<Kontaktinfo>,
)

data class SignerendeBehandler(
    val ids: List<PersonId>,
    val helsepersonellKategori: HelsepersonellKategori,
)

data class Sykmelding(
    val id: String,
    val metadata: SykmeldingMetadata,
    val pasient: Pasient,
    val behandler: Behandler,
    val signerendeBehandler: SignerendeBehandler,
    val arbeidsgiver: ArbeidsgiverInfo,
    val medisinskVurdering: MedisinskVurdering,
    val prognose: Prognose?,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val tilbakedatering: Tilbakedatering?,
    val aktivitet: List<Aktivitet>,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
)
data class AvsenderSystem(val navn: String, val versjon: String)
data class SykmeldingMetadata(
    val mottattDato: OffsetDateTime,
    val genDate: OffsetDateTime,
    val behandletTidspunkt: OffsetDateTime,
    val regelsettVersjon: String?,
    val avsenderSystem: AvsenderSystem,
    val strekkode: String?,
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
    val kontaktDato: LocalDate?,
    val begrunnelse: String?,
)

