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
            UtdypendeSporsmal("svar 1", Sporsmalstype.MEDISINSK_OPPSUMMERING, sporsmal = "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, pågående/planlagt behandling)"),
            UtdypendeSporsmal("svar 2", Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID, sporsmal = "Hvilke utfordringer har pasienten med å utføre gradert arbeid?"),
            UtdypendeSporsmal("svar 3", Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN, sporsmal = "Hvilke hensyn må være på plass for at pasienten kan prøves i det nåværende arbeidet? (ikke obligatorisk)"),
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

    @Test
    fun testToUtdypendeOpplysninger_EmptyList() {
        val result = toUtdypendeOpplysninger(emptyList())
        Assert.assertEquals(emptyMap<String, Map<String, SporsmalSvar>>(), result)
    }

    @Test
    fun testToUtdypendeOpplysninger_NullList() {
        val result = toUtdypendeOpplysninger(null)
        Assert.assertEquals(emptyMap<String, Map<String, SporsmalSvar>>(), result)
    }

    @Test
    fun testToUtdypendeOpplysninger_OnlyUke7Questions() {
        val spm = listOf(
            UtdypendeSporsmal("Medisinsk oppsummering svar", Sporsmalstype.MEDISINSK_OPPSUMMERING, sporsmal = "sporsmal 1"),
            UtdypendeSporsmal("Gradert arbeid utfordringer", Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID, sporsmal = "sporsmal 2"),
            UtdypendeSporsmal("Hensyn på arbeidsplassen", Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN, sporsmal = "sporsmal 3"),
        )

        val result = toUtdypendeOpplysninger(spm)

        Assert.assertEquals(1, result.size)
        Assert.assertTrue(result.containsKey("6.3"))

        val uke7Map = result["6.3"]!!
        Assert.assertEquals(3, uke7Map.size)
        Assert.assertEquals(
            SporsmalSvar(
                "sporsmal 1",
                svar = "Medisinsk oppsummering svar",
                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
            ),
            uke7Map["6.3.1"]
        )
        Assert.assertEquals(
            SporsmalSvar(
                "sporsmal 2",
                svar = "Gradert arbeid utfordringer",
                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
            ),
            uke7Map["6.3.2"]
        )
        Assert.assertEquals(
            SporsmalSvar(
                "sporsmal 3",
                svar = "Hensyn på arbeidsplassen",
                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
            ),
            uke7Map["6.3.3"]
        )
        val digitalUtdypendeSporsmal = SykmeldingMapper().toDigitalUtdypendeSporsmal("1", result)
        Assert.assertEquals(spm, digitalUtdypendeSporsmal)
    }

    @Test
    fun testToUtdypendeOpplysninger_OnlyUke17Questions() {
        val spm = listOf(
            UtdypendeSporsmal("Medisinsk oppsummering", Sporsmalstype.MEDISINSK_OPPSUMMERING, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Utfordringer med arbeid", Sporsmalstype.UTFORDRINGER_MED_ARBEID, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Behandling og fremtidig arbeid", Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Uavklarte forhold", Sporsmalstype.UAVKLARTE_FORHOLD, sporsmal = "sporsmal"),
        )

        val result = toUtdypendeOpplysninger(spm)

        Assert.assertEquals(1, result.size)
        Assert.assertTrue(result.containsKey("6.4"))

        val uke17Map = result["6.4"]!!
        Assert.assertEquals(4, uke17Map.size)
        Assert.assertTrue(uke17Map.containsKey("6.4.1"))
        Assert.assertTrue(uke17Map.containsKey("6.4.2"))
        Assert.assertTrue(uke17Map.containsKey("6.4.3"))
        Assert.assertTrue(uke17Map.containsKey("6.4.4"))

        val digitalUtdypendeSporsmal = SykmeldingMapper().toDigitalUtdypendeSporsmal("1", result)
        Assert.assertEquals(spm, digitalUtdypendeSporsmal)
    }

    @Test
    fun testToUtdypendeOpplysninger_OnlyUke39Questions() {
        val spm = listOf(
            UtdypendeSporsmal("Medisinsk oppsummering", Sporsmalstype.MEDISINSK_OPPSUMMERING, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Utfordringer med arbeid", Sporsmalstype.UTFORDRINGER_MED_ARBEID, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Forventet helsetilstand", Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Medisinske hensyn", Sporsmalstype.MEDISINSKE_HENSYN, sporsmal = "sporsmal"),
        )

        val result = toUtdypendeOpplysninger(spm)

        Assert.assertEquals(1, result.size)
        Assert.assertTrue(result.containsKey("6.5"))

        val uke39Map = result["6.5"]!!
        Assert.assertEquals(4, uke39Map.size)
        Assert.assertTrue(uke39Map.containsKey("6.5.1"))
        Assert.assertTrue(uke39Map.containsKey("6.5.2"))
        Assert.assertTrue(uke39Map.containsKey("6.5.3"))
        Assert.assertTrue(uke39Map.containsKey("6.5.4"))

        val digitalUtdypendeSporsmal = SykmeldingMapper().toDigitalUtdypendeSporsmal("1", result)
        Assert.assertEquals(spm, digitalUtdypendeSporsmal)
    }

    @Test
    fun testToUtdypendeOpplysninger_AllQuestionsWithMultiplePrefixes() {
        val spm = listOf(
            UtdypendeSporsmal("Answer1", Sporsmalstype.MEDISINSK_OPPSUMMERING, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Answer2", Sporsmalstype.UTFORDRINGER_MED_ARBEID, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Answer3", Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Answer4", Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Answer5", Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Answer6", Sporsmalstype.UAVKLARTE_FORHOLD, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Answer7", Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING, sporsmal = "sporsmal"),
            UtdypendeSporsmal("Answer8", Sporsmalstype.MEDISINSKE_HENSYN, sporsmal = "sporsmal"),
        )

        val result = toUtdypendeOpplysninger(spm)

        Assert.assertEquals(3, result.size)
        Assert.assertTrue(result.containsKey("6.3"))
        Assert.assertTrue(result.containsKey("6.4"))
        Assert.assertTrue(result.containsKey("6.5"))

        val uke7Map = result["6.3"]!!
        Assert.assertEquals(2, uke7Map.size)
        Assert.assertTrue(uke7Map.containsKey("6.3.2"))
        Assert.assertTrue(uke7Map.containsKey("6.3.3"))

        val uke17Map = result["6.4"]!!
        Assert.assertEquals(2, uke17Map.size)
        Assert.assertTrue(uke17Map.containsKey("6.4.3"))
        Assert.assertTrue(uke17Map.containsKey("6.4.4"))

        val uke39Map = result["6.5"]!!
        Assert.assertEquals(4, uke39Map.size)
        Assert.assertTrue(uke39Map.containsKey("6.5.1"))
        Assert.assertTrue(uke39Map.containsKey("6.5.2"))
        Assert.assertTrue(uke39Map.containsKey("6.5.3"))
        Assert.assertTrue(uke39Map.containsKey("6.5.4"))
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
        utdypendeSporsmal = listOf(
            UtdypendeSporsmal(svar = "svar 1", Sporsmalstype.MEDISINSK_OPPSUMMERING, true, "sporsmal 1"),
            UtdypendeSporsmal(svar = "svar 1", Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID, true, "sporsmal 1"),
        )
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
