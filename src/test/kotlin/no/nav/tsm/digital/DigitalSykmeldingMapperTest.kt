package no.nav.tsm.digital


import no.nav.tsm.reformat.sykmelding.service.SykmeldingMapper
import no.nav.tsm.smregister.models.SporsmalSvar
import no.nav.tsm.smregister.models.SvarRestriksjon
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.metadata.*
import org.junit.Assert
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test

class DigitalSykmeldingMapperTest {

    private val sykmeldingMapper = SykmeldingMapper()

    @Test
    fun testMapDigitalSykmelding() {
        val digitalSykmeldingRecord = getDigitalSykmeldingRecord()
        val receivedSykmelding = digitalSykmeldingRecord.toReceivedSykmelding("aktorId")
        val mappedDigitalSykmeldingRecord = sykmeldingMapper.toNewSykmelding(receivedSykmelding)
        Assert.assertEquals(digitalSykmeldingRecord, mappedDigitalSykmeldingRecord)
    }

    @Test
    fun testMapDigitalSykmeldingUtdypendeOpplysninger() {
        val spm = listOf(
            UtdypendeSporsmal("svar 1", Sporsmalstype.MEDISINSK_OPPSUMMERING),
            UtdypendeSporsmal("svar 2", Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID),
            UtdypendeSporsmal("svar 3", Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN),
        )

        val legacyUtdypendeOpplysninger = toUtdypendeOpplysninger(spm)

        val expected = mapOf<String, Map<String, SporsmalSvar>>(
            "6.3" to mapOf(
                "6.3.1" to SporsmalSvar(
                    "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, pågående/planlagt behandling)",
                    svar = "svar 1",
                    restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                ),
                "6.3.2" to SporsmalSvar(
                    "Hvilke utfordringer har pasienten med å utføre gradert arbeid?",
                    svar = "svar 2",
                    restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                ),
                "6.3.3" to SporsmalSvar(
                    "Hvilke hensyn må være på plass for at pasienten kan prøves i det nåværende arbeidet? (ikke obligatorisk)",
                    svar = "svar 3",
                    restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                ),
            )
        )

        Assert.assertEquals(expected, legacyUtdypendeOpplysninger)
        val digitalUtdypendeSporsmal = SykmeldingMapper().toDigitalUtdypendeSporsmal("1", legacyUtdypendeOpplysninger)
        Assert.assertEquals(spm, digitalUtdypendeSporsmal)

    }
}


private fun getDigitalSykmeldingRecord() : SykmeldingRecord {
    val digitalSykmelding = DigitalSykmelding(
        id = UUID.randomUUID().toString(),
        metadata = DigitalSykmeldingMetadata(
            mottattDato = OffsetDateTime.parse("2025-01-01T12:04:04.004Z"),
            genDate = OffsetDateTime.parse("2025-01-01T12:01:03.002Z"),
            avsenderSystem = no.nav.tsm.sykmelding.input.core.model.AvsenderSystem("syk-inn", "1")
        ),
        pasient = Pasient(
            navn = Navn("Fornavn", "Mellomnavn", "Etternavn"),
            navKontor = null,
            navnFastlege = "Navn Fastlege",
            fnr = "12345678901",
            kontaktinfo = listOf(
                Kontaktinfo(KontaktinfoType.TLF, "12345678"),
            )
        ),
        medisinskVurdering = DigitalMedisinskVurdering(
            hovedDiagnose = DiagnoseInfo(DiagnoseSystem.ICPC2, "R03", "diagnosetekst"),
            biDiagnoser = listOf(
                DiagnoseInfo(DiagnoseSystem.ICPC2, "R04", "diagnosetekst2"),
                DiagnoseInfo(DiagnoseSystem.ICPC2, "R05", "diagnosetekst3")
            ),
            svangerskap = true,
            yrkesskade = Yrkesskade(
                LocalDate.parse("2025-01-01"),
            ),
            skjermetForPasient = true,
            annenFravarsgrunn = AnnenFravarsgrunn.NODVENDIG_KONTROLLUNDENRSOKELSE
        ),
        aktivitet = listOf(
            AktivitetIkkeMulig(
                medisinskArsak = MedisinskArsak(
                    "medisinsk beskrivelse",
                    arsak = listOf(MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET, MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND)
                ),
                arbeidsrelatertArsak = null,
                fom = LocalDate.parse("2025-01-01"),
                tom = LocalDate.parse("2025-01-31")
            ),
            Gradert(
                grad = 50,
                reisetilskudd = false,
                fom = LocalDate.parse("2025-01-02"),
                tom = LocalDate.parse("2025-02-28")
            ),
        ),
        behandler = Behandler(
            navn = Navn("Fornavn", "Mellomnavn", "Etternavn"),
            adresse = null,
            ids = listOf(
                PersonId(
                    id = "06838098256",
                    type = PersonIdType.FNR
                ),
                PersonId(
                    id = "123456789",
                    type = PersonIdType.HPR
                ),
            ),
            kontaktinfo = listOf(
                Kontaktinfo(KontaktinfoType.TLF, "12345678"),
            )
        ),
        sykmelder = Sykmelder(
            ids = listOf(
                PersonId(
                    id = "06838098256",
                    type = PersonIdType.FNR
                ),
                PersonId(
                    id = "123456789",
                    type = PersonIdType.HPR
                ),
            ),
            HelsepersonellKategori.LEGE
        ),
        arbeidsgiver = EnArbeidsgiver(
            navn = "Arbeidsgiver AS",
            yrkesbetegnelse = "Yrke",
            stillingsprosent = 100,
            meldingTilArbeidsgiver = "melding",
            tiltakArbeidsplassen = "tiltak"
        ),
        tilbakedatering = Tilbakedatering(kontaktDato = LocalDate.parse("2025-01-01"), begrunnelse = "begrunnelse"),
        bistandNav = BistandNav(
            beskrivBistand = "beskrivelse",
            bistandUmiddelbart = true
        ),
        utdypendeSporsmal = null
    )
    val validation = ValidationResult(
        RuleType.OK,
        timestamp = OffsetDateTime.parse("2025-01-01T12:02:04.123Z"),
        rules = emptyList()
    )
    val metadata = Digital("123456789")
    return SykmeldingRecord(
        metadata = metadata,
        sykmelding = digitalSykmelding,
        validation = validation
    )
}
