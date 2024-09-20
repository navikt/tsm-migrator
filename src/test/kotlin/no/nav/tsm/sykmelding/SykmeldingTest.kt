package no.nav.tsm.sykmelding

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.smregister.models.AvsenderSystem
import no.nav.tsm.sykmelding.validation.InvalidRule
import no.nav.tsm.sykmelding.validation.PendingRule
import no.nav.tsm.sykmelding.validation.ResolvedRule
import no.nav.tsm.sykmelding.validation.RuleOutcome
import no.nav.tsm.sykmelding.validation.RuleResult
import no.nav.tsm.sykmelding.validation.ValidationResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class SykmeldingTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(SykmeldingModule())
    @Test
    fun testSerializationAndDeserialization() {
        val sykmelding = SykmeldingMedBehandlingsutfall(
            kilde = SykmeldingKilde.ELEKTRONISK,
            validation = ValidationResult(
                status = RuleResult.INVALID,
                rules = listOf(
                    InvalidRule(
                        outcome = RuleOutcome(
                            outcome = RuleResult.INVALID,
                            timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                        ),
                        name = "INVALID_RULE",
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        description = "description"
                ),
                    PendingRule(
                        name = "PENDING_RULE",
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        description = "description"
                    ),
                    ResolvedRule(
                        name = "RESOLVED_RULE",
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        description = "description",
                        outcome = RuleOutcome(
                            outcome = RuleResult.OK,
                            timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                        )
                    )
            ),),
            sykmelding = Sykmelding(
                id = UUID.randomUUID().toString(),
                metadata = SykmeldingMetadata(
                    msgId = UUID.randomUUID().toString(),
                    regelsettVersjon = "1",
                    partnerreferanse = "partner",
                    avsenderSystem = AvsenderSystem("nav", "5"),
                    mottattDato = OffsetDateTime.now(ZoneOffset.UTC),
                    genDate = OffsetDateTime.now(ZoneOffset.UTC),
                    behandletTidspunkt = OffsetDateTime.now(ZoneOffset.UTC),
                ),
                pasient = Person("123456789", Navn("Fornavn", "Etternavn")),
                arbeidsgiver = IngenArbeidsgiver(),
                behandler = Behandler(
                    person = Person(ident = "1234567890", navn = Navn("Fornavn", "Etternanv")),
                    adresse = Adresse("adresse", "1234", "steed", "kommune", "postbox"),
                    kontaktInfo = emptyList()
                ),
                medisinskVurdering = MedisinskVurdering(
                    hovedDiagnose = DiagnoseInfo(
                        DiagnoseSystem.ICD10, "kode", "tekst"
                    ),
                    biDiagnoser = listOf(DiagnoseInfo(DiagnoseSystem.ICPC2, "kode", "tekst")),
                    svangerskap = false,
                    yrkesskade = false,
                    yrkesskadeDato = LocalDate.now(),
                    annenFraversArsak = AnnenFraverArsak("beskrivelse", AnnenFravarArsakType.DONOR),
                    skjermetForPasient = true,
                    syketilfelletStartDato = LocalDate.now(ZoneOffset.UTC),
                ),
                prognose = Prognose(
                    arbeidsforEtterPeriode = true,
                    hensynArbeidsplassen = "ingen hensyn",
                    arbeid = ErIArbeid(
                        true,
                        true,
                        LocalDate.now(),
                        LocalDate.now(),
                    )
                ),
                tilbakedatering = Tilbakedatering(
                    kontaktDato = LocalDate.now(ZoneOffset.UTC),
                    begrunnelse = "ingen god begrunnelse",
                ),
                aktivitet = listOf(
                    AktivitetIkkeMulig(
                        medisinskArsak = MedisinskArsak("beskrivelse", MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET),
                        arbeidsrelatertArsak = ArbeidsrelatertArsak("Beskrivelse", ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING),
                        fom = LocalDate.now(ZoneOffset.UTC),
                        tom = LocalDate.now(ZoneOffset.UTC),
                    )
                ),
                utdypendeOpplysninger = emptyMap(),
                tiltak = Tiltak(
                    tiltakNAV = "tiltak fra nav",
                    andreTiltak = "andre tiltak"
                ),
                bistandNav = BistandNav(
                    bistandUmiddelbart = false,
                    beskrivBistand = "beskriv bistand"
                )
            )
        )
        val serializedSykmelding = objectMapper.writeValueAsString(sykmelding)

        val deserializedSykmelding : SykmeldingMedBehandlingsutfall = objectMapper.readValue(serializedSykmelding)

        assertEquals(sykmelding, deserializedSykmelding)
    }
}

