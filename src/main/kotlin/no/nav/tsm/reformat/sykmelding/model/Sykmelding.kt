package no.nav.tsm.reformat.sykmelding.model

import no.nav.tsm.smregister.models.UtenlandskInfo
import no.nav.tsm.reformat.sykmelding.SporsmalSvar
import no.nav.tsm.reformat.sykmelding.model.metadata.Adresse
import no.nav.tsm.reformat.sykmelding.model.metadata.HelsepersonellKategori
import no.nav.tsm.reformat.sykmelding.model.metadata.Kontaktinfo
import no.nav.tsm.reformat.sykmelding.model.metadata.Meldingsinformasjon
import no.nav.tsm.reformat.sykmelding.model.metadata.Navn
import no.nav.tsm.reformat.sykmelding.model.metadata.PersonId
import no.nav.tsm.reformat.sykmelding.validation.ValidationResult
import java.time.LocalDate
import java.time.OffsetDateTime


data class SykmeldingMedBehandlingsutfall(
    val metadata: Meldingsinformasjon,
    val sykmelding: ISykmelding,
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

enum class SykmeldingType {
    SYKMELDING,
    UTENLANDSK_SYKMELDING
}

sealed interface ISykmelding {
    val type: SykmeldingType
    val id: String
    val metadata: SykmeldingMetadata
    val pasient: Pasient
    val medisinskVurdering: MedisinskVurdering
    val aktivitet: List<Aktivitet>
}

data class UtenlandskSykmelding(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: MedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    val utenlandskInfo: UtenlandskInfo
) : ISykmelding {
    override val type = SykmeldingType.UTENLANDSK_SYKMELDING
}


data class Sykmelding(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: MedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    val behandler: Behandler,
    val arbeidsgiver: ArbeidsgiverInfo,
    val signerendeBehandler: SignerendeBehandler,
    val prognose: Prognose?,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val tilbakedatering: Tilbakedatering?,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : ISykmelding {
    override val type = SykmeldingType.SYKMELDING
}

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

