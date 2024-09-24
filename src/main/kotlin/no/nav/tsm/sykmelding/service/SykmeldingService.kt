package no.nav.tsm.sykmelding.service

import no.nav.tsm.smregister.models.AnnenFraverGrunn
import no.nav.tsm.smregister.models.AvsenderSystem
import no.nav.tsm.smregister.models.Diagnose
import no.nav.tsm.smregister.models.HarArbeidsgiver
import no.nav.tsm.smregister.models.Merknad
import no.nav.tsm.smregister.models.Periode
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.smregister.models.Status
import no.nav.tsm.smregister.models.SvarRestriksjon
import no.nav.tsm.sykmelding.Adresse
import no.nav.tsm.sykmelding.Aktivitet
import no.nav.tsm.sykmelding.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.AnnenFravarArsakType
import no.nav.tsm.sykmelding.AnnenFraverArsak
import no.nav.tsm.sykmelding.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.ArbeidsrelatertArsak
import no.nav.tsm.sykmelding.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.Avventende
import no.nav.tsm.sykmelding.Behandler
import no.nav.tsm.sykmelding.Behandlingsdager
import no.nav.tsm.sykmelding.DiagnoseInfo
import no.nav.tsm.sykmelding.DiagnoseSystem
import no.nav.tsm.sykmelding.EnArbeidsgiver
import no.nav.tsm.sykmelding.ErIArbeid
import no.nav.tsm.sykmelding.ErIkkeIArbeid
import no.nav.tsm.sykmelding.FlereArbeidsgivere
import no.nav.tsm.sykmelding.Gradert
import no.nav.tsm.sykmelding.IArbeid
import no.nav.tsm.sykmelding.IngenArbeidsgiver
import no.nav.tsm.sykmelding.Kontaktinfo
import no.nav.tsm.sykmelding.MedisinskArsak
import no.nav.tsm.sykmelding.MedisinskArsakType
import no.nav.tsm.sykmelding.MedisinskVurdering
import no.nav.tsm.sykmelding.Navn
import no.nav.tsm.sykmelding.Person
import no.nav.tsm.sykmelding.Prognose
import no.nav.tsm.sykmelding.Reisetilskudd
import no.nav.tsm.sykmelding.Sykmelding
import no.nav.tsm.sykmelding.SykmeldingKilde
import no.nav.tsm.sykmelding.SykmeldingMedBehandlingsutfall
import no.nav.tsm.sykmelding.SykmeldingMetadata
import no.nav.tsm.sykmelding.Tiltak
import no.nav.tsm.sykmelding.validation.InvalidRule
import no.nav.tsm.sykmelding.validation.OKRule
import no.nav.tsm.sykmelding.validation.PendingRule
import no.nav.tsm.sykmelding.validation.Rule
import no.nav.tsm.sykmelding.validation.RuleType
import no.nav.tsm.sykmelding.validation.ValidationResult
import java.time.ZoneOffset

enum class TilbakedatertMerknad {
    UNDER_BEHANDLING,
    UGYLDIG_TILBAKEDATERING,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    DELVIS_GODKJENT,
}
class SykmeldingService {
    fun toNewSykmelding(receivedSykmelding: ReceivedSykmelding): SykmeldingMedBehandlingsutfall {
        return SykmeldingMedBehandlingsutfall(
            kilde = SykmeldingKilde.ELEKTRONISK,
            validation = mapValidationResult(receivedSykmelding),
            sykmelding = Sykmelding(
                id = receivedSykmelding.sykmelding.id,
                metadata = SykmeldingMetadata(
                    msgId = receivedSykmelding.msgId,
                    regelsettVersjon = receivedSykmelding.rulesetVersion ?: "1",
                    partnerreferanse = receivedSykmelding.partnerreferanse,
                    avsenderSystem = AvsenderSystem(
                        navn = receivedSykmelding.sykmelding.avsenderSystem.navn,
                        versjon = receivedSykmelding.sykmelding.avsenderSystem.versjon
                    ),
                    mottattDato = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                    genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(ZoneOffset.UTC),
                    behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt.atOffset(ZoneOffset.UTC),
                ),
                pasient = Person(
                    ident = receivedSykmelding.personNrPasient,
                    navn = null,
                ),
                behandler = Behandler(
                    person = Person(
                        ident = receivedSykmelding.sykmelding.behandler.fnr,
                        navn = Navn(
                            fornavn = receivedSykmelding.sykmelding.behandler.fornavn,
                            etternavn = receivedSykmelding.sykmelding.behandler.etternavn
                        )
                    ),
                    adresse = Adresse(
                        gateAdresse = receivedSykmelding.sykmelding.behandler.adresse.gate,
                        postnummer = receivedSykmelding.sykmelding.behandler.adresse.postnummer.toString()
                            .padStart(4, '0'),
                        land = receivedSykmelding.sykmelding.behandler.adresse.land,
                        kommune = receivedSykmelding.sykmelding.behandler.adresse.kommune,
                        postbox = receivedSykmelding.sykmelding.behandler.adresse.postboks,
                    ),
                    kontaktInfo = receivedSykmelding.sykmelding.behandler.tlf?.let {
                        listOf(
                            Kontaktinfo(
                                "telefon",
                                it
                            )
                        )
                    } ?: emptyList(),
                ),
                arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
                medisinskVurdering = mapMedisinskVurdering(receivedSykmelding.sykmelding),
                prognose = mapPrognose(receivedSykmelding.sykmelding),
                tiltak = Tiltak(
                    tiltakNAV = receivedSykmelding.sykmelding.tiltakNAV,
                    andreTiltak = receivedSykmelding.sykmelding.andreTiltak,
                ),
                bistandNav = receivedSykmelding.sykmelding.meldingTilNAV?.let {
                    no.nav.tsm.sykmelding.BistandNav(
                        bistandUmiddelbart = it.bistandUmiddelbart,
                        beskrivBistand = it.beskrivBistand,
                    )
                },
                tilbakedatering = receivedSykmelding.sykmelding.kontaktMedPasient.kontaktDato?.let {
                    no.nav.tsm.sykmelding.Tilbakedatering(
                        kontaktDato = it,
                        begrunnelse = receivedSykmelding.sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                    )
                },
                aktivitet = receivedSykmelding.sykmelding.perioder.map {
                    mapAktivitet(it)
                },
                utdypendeOpplysninger = receivedSykmelding.sykmelding.utdypendeOpplysninger.mapValues { entry ->
                    entry.value.mapValues { questions ->
                        no.nav.tsm.sykmelding.SporsmalSvar(
                            sporsmal = questions.key,
                            svar = questions.value.svar,
                            restriksjoner = questions.value.restriksjoner.map {
                                when(it) {
                                    SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER -> no.nav.tsm.sykmelding.SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER
                                    SvarRestriksjon.SKJERMET_FOR_PASIENT -> no.nav.tsm.sykmelding.SvarRestriksjon.SKJERMET_FOR_PASIENT
                                    SvarRestriksjon.SKJERMET_FOR_NAV -> no.nav.tsm.sykmelding.SvarRestriksjon.SKJERMET_FOR_NAV
                                }
                            }
                        )
                    }
                },
            )
        )
    }

    private fun mapValidationResult(
        receivedSykmelding: ReceivedSykmelding,
    ): ValidationResult {
        return when (receivedSykmelding.validationResult.status) {
            Status.OK -> mapOkOrPendingValidation(receivedSykmelding)
            Status.INVALID -> mapInvalidValidation(receivedSykmelding)
            Status.MANUAL_PROCESSING -> ValidationResult(
                status = RuleType.OK,
                timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                rules = emptyList()
            )
        }
    }

    private fun mapInvalidValidation(receivedSykmelding: ReceivedSykmelding): ValidationResult {
        return ValidationResult(
            status = RuleType.INVALID,
            timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
            rules = receivedSykmelding.validationResult.ruleHits.mapNotNull {
                val rule = it
                when (rule.ruleStatus) {
                    Status.INVALID -> InvalidRule(
                        name = rule.ruleName,
                        timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                        description = rule.messageForUser,
                    )
                    else -> null
                }
            }
        )
    }

    private fun mapOkOrPendingValidation(receivedSykmelding: ReceivedSykmelding): ValidationResult {
        if(receivedSykmelding.merknader.isNullOrEmpty()) {
            return ValidationResult(
                status = RuleType.OK,
                timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                rules = emptyList()
            )
        }
        val rules: List<Rule> = mapTilbakedatertRules(receivedSykmelding.merknader, receivedSykmelding)
        return ValidationResult(
            status = rules.maxOf { it.type },
            timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
            rules = rules
        )
    }

    private fun mapTilbakedatertRules(
        merknader: List<Merknad>,
        receivedSykmelding: ReceivedSykmelding
    ) = merknader.map {
        when (TilbakedatertMerknad.valueOf(it.type)) {
            TilbakedatertMerknad.UNDER_BEHANDLING -> PendingRule(
                name = TilbakedatertMerknad.UNDER_BEHANDLING.name,
                timestamp = it.tidspunkt ?: receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                description = it.beskrivelse ?: "Tilbakedatert sykmelding til manuell behandling",
            )

            TilbakedatertMerknad.UGYLDIG_TILBAKEDATERING -> InvalidRule(
                name = TilbakedatertMerknad.UGYLDIG_TILBAKEDATERING.name,
                timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                description = it.beskrivelse ?: "Ugyldig tilbakedatering",
            )

            TilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER -> InvalidRule(
                name = TilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER.name,
                timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                description = it.beskrivelse ?: "Tilbakedatering krever flere opplysninger",
            )
            TilbakedatertMerknad.DELVIS_GODKJENT -> OKRule(
                name = TilbakedatertMerknad.DELVIS_GODKJENT.name,
                timestamp = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                description = it.beskrivelse ?: "Delvis godkjent tilbakedatering")
            }
        }
    }

    private fun mapMedisinskVurdering(sykmelding: no.nav.tsm.smregister.models.Sykmelding): MedisinskVurdering {
        return MedisinskVurdering(
            hovedDiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.let(toDiagnoseInfo()),
            biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser.map(toDiagnoseInfo()),
            annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak?.let {
                AnnenFraverArsak(it.beskrivelse, when(it.grunn.single())
                {
                    AnnenFraverGrunn.GODKJENT_HELSEINSTITUSJON -> AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON
                    AnnenFraverGrunn.BEHANDLING_FORHINDRER_ARBEID -> AnnenFravarArsakType.BEHANDLING_FORHINDRER_ARBEID
                    AnnenFraverGrunn.ARBEIDSRETTET_TILTAK -> AnnenFravarArsakType.ARBEIDSRETTET_TILTAK
                    AnnenFraverGrunn.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND -> AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
                    AnnenFraverGrunn.NODVENDIG_KONTROLLUNDENRSOKELSE -> AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE
                    AnnenFraverGrunn.SMITTEFARE -> AnnenFravarArsakType.SMITTEFARE
                    AnnenFraverGrunn.ABORT -> AnnenFravarArsakType.ABORT
                    AnnenFraverGrunn.UFOR_GRUNNET_BARNLOSHET -> AnnenFravarArsakType.UFOR_GRUNNET_BARNLOSHET
                    AnnenFraverGrunn.DONOR -> AnnenFravarArsakType.DONOR
                    AnnenFraverGrunn.BEHANDLING_STERILISERING -> AnnenFravarArsakType.BEHANDLING_STERILISERING
                })
            },
            svangerskap = sykmelding.medisinskVurdering.svangerskap,
            yrkesskade = sykmelding.medisinskVurdering.yrkesskade,
            yrkesskadeDato = sykmelding.medisinskVurdering.yrkesskadeDato,
            skjermetForPasient = sykmelding.skjermesForPasient,
            syketilfelletStartDato = sykmelding.syketilfelleStartDato,


        )
    }

    private fun toDiagnoseInfo() = { diagnose: Diagnose ->
        DiagnoseInfo(
            kode = diagnose.kode,
            system = when (diagnose.system) {
                "no.nav.tsm.smregister.models.DiagnoseSystem.ICPC2" -> DiagnoseSystem.ICPC2
                "no.nav.tsm.smregister.models.DiagnoseSystem.ICD10" -> DiagnoseSystem.ICD10
                else -> throw IllegalArgumentException("Ukjent diagnose system")
            },
            tekst = diagnose.tekst,
        )
    }

    private fun mapPrognose(sykmelding: no.nav.tsm.smregister.models.Sykmelding): Prognose? {
        return sykmelding.prognose?.let {
            Prognose(
                arbeidsforEtterPeriode = it.arbeidsforEtterPeriode,
                hensynArbeidsplassen = it.hensynArbeidsplassen,
                arbeid = mapArbeid(it.erIArbeid, it.erIkkeIArbeid),
            )
        }
    }

    private fun mapArbeid(erIArbeid: no.nav.tsm.smregister.models.ErIArbeid?, erIkkeIArbeid: no.nav.tsm.smregister.models.ErIkkeIArbeid?): IArbeid? {
        if(erIArbeid != null) {
            return ErIArbeid(
                egetArbeidPaSikt = erIArbeid.egetArbeidPaSikt,
                annetArbeidPaSikt = erIArbeid.annetArbeidPaSikt,
                arbeidFOM = erIArbeid.arbeidFOM,
                vurderingsdato = erIArbeid.vurderingsdato,
            )
        }

        if(erIkkeIArbeid != null) {
            return ErIkkeIArbeid(
                arbeidsforPaSikt = erIkkeIArbeid.arbeidsforPaSikt,
                arbeidsforFOM = erIkkeIArbeid.arbeidsforFOM,
                vurderingsdato = erIkkeIArbeid.vurderingsdato,
            )
        }

        return null
    }

    private fun mapAktivitet(periode: Periode): Aktivitet {
        if (periode.aktivitetIkkeMulig != null) {
            if(periode.aktivitetIkkeMulig.medisinskArsak == null && periode.aktivitetIkkeMulig.arbeidsrelatertArsak == null) {
                throw IllegalArgumentException("Mangler medisinsk eller arbeidsrelatert årsak")
            }
            return AktivitetIkkeMulig(
                medisinskArsak = periode.aktivitetIkkeMulig.medisinskArsak?.let { MedisinskArsak(it.beskrivelse, MedisinskArsakType.ANNET) },
                arbeidsrelatertArsak = periode.aktivitetIkkeMulig.arbeidsrelatertArsak?.let { ArbeidsrelatertArsak(it.beskrivelse, ArbeidsrelatertArsakType.ANNET) },
                fom = periode.fom,
                tom = periode.tom
            )
        }

        if(periode.reisetilskudd) {
            return Reisetilskudd(
                fom = periode.fom,
                tom = periode.tom,
            )
        }

        if(periode.gradert != null) {
            return Gradert(
                fom = periode.fom,
                tom = periode.tom,
                grad = periode.gradert.grad,
                reisetilskudd = periode.gradert.reisetilskudd,
            )
        }

        if(periode.behandlingsdager != null) {
            return Behandlingsdager(
                fom = periode.fom,
                tom = periode.tom,
                antallBehandlingsdager = periode.behandlingsdager,
            )
        }

        if(periode.avventendeInnspillTilArbeidsgiver != null) {
            return Avventende(
                fom = periode.fom,
                tom = periode.tom,
                innspillTilArbeidsgiver = periode.avventendeInnspillTilArbeidsgiver,
            )
        }

        throw IllegalArgumentException("Ukjent aktivitetstype")
    }

    private fun mapArbeidsgiver(sykmelding: no.nav.tsm.smregister.models.Sykmelding): ArbeidsgiverInfo {
        return when (sykmelding.arbeidsgiver.harArbeidsgiver) {
            HarArbeidsgiver.EN_ARBEIDSGIVER -> EnArbeidsgiver(
                meldingTilArbeidsgiver = sykmelding.meldingTilArbeidsgiver,
                tiltakArbeidsplassen = sykmelding.tiltakArbeidsplassen,
            )

            HarArbeidsgiver.FLERE_ARBEIDSGIVERE -> FlereArbeidsgivere(
                meldingTilArbeidsgiver = sykmelding.meldingTilArbeidsgiver,
                tiltakArbeidsplassen = sykmelding.tiltakArbeidsplassen,
                navn = sykmelding.arbeidsgiver.navn ?: throw IllegalArgumentException("Arbeidsgiver mangler navn"),
                yrkesbetegnelse = sykmelding.arbeidsgiver.yrkesbetegnelse
                    ?: throw IllegalArgumentException("Arbeidsgiver mangler yrkesbetegnelse"),
                stillingsprosent = sykmelding.arbeidsgiver.stillingsprosent,
            )

            HarArbeidsgiver.INGEN_ARBEIDSGIVER -> IngenArbeidsgiver()
        }
    }

