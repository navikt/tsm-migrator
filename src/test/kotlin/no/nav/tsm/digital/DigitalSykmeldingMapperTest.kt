package no.nav.tsm.digital


import no.nav.tsm.reformat.sykmelding.service.SykmeldingMapper
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarArsakType
import no.nav.tsm.sykmelding.input.core.model.AnnenFraverArsak
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.BistandNav
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
import no.nav.tsm.sykmelding.input.core.model.EnArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsak
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsakType
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.Yrkesskade
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.Kontaktinfo
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import org.junit.Assert
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
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
}


private fun getDigitalSykmeldingRecord() : SykmeldingRecord {
    val digitalSykmelding = DigitalSykmelding(
        id = UUID.randomUUID().toString(),
        metadata = DigitalSykmeldingMetadata(
            mottattDato = OffsetDateTime.parse("2025-01-01T12:04:04.004Z"),
            genDate = OffsetDateTime.parse("2025-01-01T12:01:03.002Z"),
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
        medisinskVurdering = MedisinskVurdering(
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
            syketilfelletStartDato = LocalDate.parse("2025-01-01"),
            annenFraversArsak = AnnenFraverArsak(
                "beskrivelse",
                arsak = listOf(
                    AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE,
                    AnnenFravarArsakType.BEHANDLING_FORHINDRER_ARBEID
                )
            ),
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
