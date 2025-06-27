package no.nav.tsm.digital

import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLCV
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLReceiver
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helse.msgHead.XMLSender
import no.nav.helse.sm2013.Address
import no.nav.helse.sm2013.ArsakType
import no.nav.helse.sm2013.CS
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.helse.sm2013.NavnType
import no.nav.helse.sm2013.TeleCom
import no.nav.helse.sm2013.URL
import no.nav.tsm.reformat.sykmelding.service.OldTilbakedatertMerknad
import no.nav.tsm.reformat.sykmelding.util.XmlStuff
import no.nav.tsm.smregister.models.Adresse
import no.nav.tsm.smregister.models.AktivitetIkkeMuligLegacy
import no.nav.tsm.smregister.models.AnnenFraverGrunn
import no.nav.tsm.smregister.models.AnnenFraversArsak
import no.nav.tsm.smregister.models.Arbeidsgiver
import no.nav.tsm.smregister.models.ArbeidsrelatertArsak
import no.nav.tsm.smregister.models.ArbeidsrelatertArsakTypeLegacy
import no.nav.tsm.smregister.models.AvsenderSystem
import no.nav.tsm.smregister.models.Behandler
import no.nav.tsm.smregister.models.Diagnose
import no.nav.tsm.smregister.models.GradertLegacy
import no.nav.tsm.smregister.models.HarArbeidsgiver
import no.nav.tsm.smregister.models.KontaktMedPasient
import no.nav.tsm.smregister.models.MedisinskArsak
import no.nav.tsm.smregister.models.MedisinskArsakTypeLegacy
import no.nav.tsm.smregister.models.MedisinskVurdering
import no.nav.tsm.smregister.models.Merknad
import no.nav.tsm.smregister.models.Periode
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.smregister.models.RuleInfo
import no.nav.tsm.smregister.models.Status
import no.nav.tsm.smregister.models.SykmeldingLegacy
import no.nav.tsm.smregister.models.ValidationResultLegacy
import no.nav.tsm.sykmelding.input.core.model.ARBEIDSGIVER_TYPE
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarArsakType
import no.nav.tsm.sykmelding.input.core.model.AnnenFraverArsak
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.Avventende
import no.nav.tsm.sykmelding.input.core.model.Behandlingsdager
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.EnArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.InvalidRule
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsakType
import no.nav.tsm.sykmelding.input.core.model.Papirsykmelding
import no.nav.tsm.sykmelding.input.core.model.Reisetilskudd
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmelding.input.core.model.UtenlandskSykmelding
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.EDIEmottak
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.Papir
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.sykmelding.input.core.model.metadata.Utenlandsk
import org.slf4j.LoggerFactory
import java.util.stream.Collectors

private val log = LoggerFactory.getLogger("no.nav.tsm.digital.DigitalSykmeldingMapper")
private val xmlStuff = XmlStuff()
fun SykmeldingRecord.toReceivedSykmelding() : ReceivedSykmelding {
    val fromSykmelding = sykmelding
    val sykmelding: ReceivedSykmelding = when(fromSykmelding) {
        is DigitalSykmelding -> fromDigital(fromSykmelding, this.metadata as Digital, this.validation)
        is XmlSykmelding -> fromXml(fromSykmelding, this.metadata as EDIEmottak, this.validation)
        is Papirsykmelding -> fromPapir(fromSykmelding, this.metadata as Papir, this.validation)
        is UtenlandskSykmelding -> fromUtenlandsk(fromSykmelding, this.metadata as Utenlandsk, this.validation)
    }
    return sykmelding
}
fun fromUtenlandsk(sykmelding: UtenlandskSykmelding, metadata: Utenlandsk, validation: ValidationResult): ReceivedSykmelding {
 TODO()
}

fun fromPapir(sykmelding: Papirsykmelding, metadata: Papir, validation: ValidationResult): ReceivedSykmelding {
    TODO("Not yet implemented")
}

fun fromXml(sykmelding: XmlSykmelding, metadata: EDIEmottak, validation: ValidationResult): ReceivedSykmelding {
    TODO()
}

fun fromDigital(sykmelding: DigitalSykmelding, metadata: Digital, validation: ValidationResult): ReceivedSykmelding {
    val perioder = sykmelding.aktivitet.map { it.toPeriode() }
    val receivedSykmelding = ReceivedSykmelding(
        sykmelding = SykmeldingLegacy(
            id = sykmelding.id,
            msgId = sykmelding.id,
            pasientAktoerId = "",
            medisinskVurdering = MedisinskVurdering(
                hovedDiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.toDiagnose(),
                biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser?.let { bidagnoser -> bidagnoser.map { it.toDiagnose() }} ?: emptyList(),
                svangerskap = sykmelding.medisinskVurdering.svangerskap,
                yrkesskade = sykmelding.medisinskVurdering.yrkesskade != null,
                yrkesskadeDato = sykmelding.medisinskVurdering.yrkesskade?.yrkesskadeDato,
                annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak?.toAnnenFraversArsak(),
            ),
            skjermesForPasient = sykmelding.medisinskVurdering.skjermetForPasient,
            arbeidsgiver = toArbeidsgiver(sykmelding.arbeidsgiver),
            perioder = perioder,
            prognose = null,
            tiltakArbeidsplassen = null,
            tiltakNAV = null,
            andreTiltak = null,
            meldingTilNAV = null,
            meldingTilArbeidsgiver = null,
            kontaktMedPasient = KontaktMedPasient(null, null),
            behandletTidspunkt = sykmelding.metadata.genDate.toLocalDateTime(),
            behandler = Behandler(
                fornavn = sykmelding.behandler.navn.fornavn,
                mellomnavn = sykmelding.behandler.navn.mellomnavn,
                etternavn = sykmelding.behandler.navn.etternavn,
                aktoerId = "",
                fnr = sykmelding.behandler.ids.first { it.type == PersonIdType.FNR }.id,
                hpr = sykmelding.behandler.ids.firstOrNull { it.type == PersonIdType.HPR }?.id,
                her = sykmelding.behandler.ids.firstOrNull { it.type == PersonIdType.HER }?.id,
                Adresse(
                    sykmelding.behandler.adresse?.gateadresse,
                    sykmelding.behandler.adresse?.postnummer?.toInt(),
                    sykmelding.behandler.adresse?.kommune,
                    sykmelding.behandler.adresse?.postboks,
                    sykmelding.behandler.adresse?.land,
                ),
                sykmelding.behandler.kontaktinfo.firstOrNull { it.type == KontaktinfoType.TLF }?.value,
            ),
            avsenderSystem = AvsenderSystem("syk-inn", versjon = "pilot"),
            syketilfelleStartDato = null,
            signaturDato = sykmelding.metadata.genDate.toLocalDateTime(),
            navnFastlege = null,
            utdypendeOpplysninger = emptyMap() //TODO
        ),
        utenlandskSykmelding = null,
        mottattDato = sykmelding.metadata.mottattDato.toLocalDateTime(),
        msgId = sykmelding.id,
        tssid = null,
        validationResult = ValidationResultLegacy(
            status = when(validation.status) {
                RuleType.OK -> Status.OK
                RuleType.PENDING -> Status.OK
                RuleType.INVALID -> Status.INVALID
            },
            ruleHits = validation
                .rules
                .filterIsInstance<InvalidRule>()
                .filter { it.name != TilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER.name }
                .map {
                RuleInfo(
                    ruleName = it.name,
                    messageForSender = it.reason.sykmelder,
                    messageForUser = it.reason.sykmeldt,
                    ruleStatus = Status.INVALID)
            },
            timestamp = validation.timestamp
        ),
        vedlegg = null,
        fellesformat =  xmlStuff.marshal(mapToFellesformat(sykmelding, metadata, perioder)),
        merknader =  mapToMerknader(validation),
        partnerreferanse = null,
        legekontorReshId = null,
        legekontorOrgName = null,
        legekontorOrgNr = metadata.orgnummer,
        legekontorHerId = null,
        rulesetVersion = null,
        legeHelsepersonellkategori = sykmelding.sykmelder.helsepersonellKategori.name,
        personNrLege = sykmelding.sykmelder.ids.first { it.type == PersonIdType.FNR }.id,
        tlfPasient = null,
        personNrPasient = sykmelding.pasient.fnr,
        legeHprNr = sykmelding.sykmelder.ids.first { it.type == PersonIdType.HPR }.id,
        navLogId = sykmelding.id
    )

    return receivedSykmelding
}

fun toArbeidsgiver(it: ArbeidsgiverInfo) : Arbeidsgiver {
    return when (it) {
        is EnArbeidsgiver -> Arbeidsgiver(
            harArbeidsgiver = HarArbeidsgiver.EN_ARBEIDSGIVER,
            navn = it.navn,
            yrkesbetegnelse = it.yrkesbetegnelse,
            stillingsprosent = it.stillingsprosent,
        )

        is FlereArbeidsgivere -> Arbeidsgiver(
            harArbeidsgiver = HarArbeidsgiver.EN_ARBEIDSGIVER,
            navn = it.navn,
            yrkesbetegnelse = it.yrkesbetegnelse,
            stillingsprosent = it.stillingsprosent,
        )

        is IngenArbeidsgiver -> Arbeidsgiver(
            harArbeidsgiver = HarArbeidsgiver.INGEN_ARBEIDSGIVER,
            navn = null,
            yrkesbetegnelse = null,
            stillingsprosent = null,
        )
    }
}

fun mapToFellesformat(sykmelding: DigitalSykmelding, metadata: Digital, perioder : List<Periode>): XMLEIFellesformat {
    return XMLEIFellesformat().apply {
        any.add(
            XMLMsgHead().apply {
                msgInfo =
                    XMLMsgInfo().apply {
                        type =
                            XMLCS().apply {
                                dn = "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding"
                                v = "SYKMELD"
                            }
                        miGversion = "v1.2 2006-05-24"
                        genDate = sykmelding.metadata.genDate.toString()
                        msgId = sykmelding.id
                        ack =
                            XMLCS().apply {
                                dn = "Ja"
                                v = "J"
                            }
                        sender =
                            XMLSender().apply {
                                comMethod =
                                    XMLCS().apply {
                                        dn = "EDI"
                                        v = "EDI"
                                    }
                                organisation =
                                    XMLOrganisation().apply {
                                        healthcareProfessional =
                                            XMLHealthcareProfessional().apply {
                                                givenName = sykmelding.behandler.navn.fornavn
                                                middleName = sykmelding.behandler.navn.mellomnavn
                                                familyName = sykmelding.behandler.navn.etternavn
                                                ident.addAll(
                                                    listOf(
                                                        XMLIdent().apply {
                                                            id = sykmelding.behandler.ids.first { it.type == PersonIdType.HPR }.id
                                                            typeId =
                                                                XMLCV().apply {
                                                                    dn = "HPR-nummer"
                                                                    s = "2.16.578.1.12.4.1.1.8116"
                                                                    v = "HPR"
                                                                }
                                                        },
                                                        XMLIdent().apply {
                                                            id = sykmelding.behandler.ids.first { it.type == PersonIdType.FNR }.id
                                                            typeId =
                                                                XMLCV().apply {
                                                                    dn = "Fødselsnummer"
                                                                    s = "2.16.578.1.12.4.1.1.8116"
                                                                    v = "FNR"
                                                                }
                                                        },
                                                    ),
                                                )
                                            }
                                    }
                            }
                        receiver =
                            XMLReceiver().apply {
                                comMethod =
                                    XMLCS().apply {
                                        dn = "EDI"
                                        v = "EDI"
                                    }
                                organisation =
                                    XMLOrganisation().apply {
                                        organisationName = "NAV"
                                        ident.addAll(
                                            listOf(
                                                XMLIdent().apply {
                                                    id = "79768"
                                                    typeId =
                                                        XMLCV().apply {
                                                            dn =
                                                                "Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"
                                                            s = "2.16.578.1.12.4.1.1.9051"
                                                            v = "HER"
                                                        }
                                                },
                                                XMLIdent().apply {
                                                    id = "889640782"
                                                    typeId =
                                                        XMLCV().apply {
                                                            dn =
                                                                "Organisasjonsnummeret i Enhetsregister (Brønøysund)"
                                                            s = "2.16.578.1.12.4.1.1.9051"
                                                            v = "ENH"
                                                        }
                                                },
                                            ),
                                        )
                                    }
                            }
                    }
                document.add(
                    XMLDocument().apply {
                        refDoc =
                            XMLRefDoc().apply {
                                msgType =
                                    XMLCS().apply {
                                        dn = "XML-instans"
                                        v = "XML"
                                    }
                                content =
                                    XMLRefDoc.Content().apply {
                                        any.add(
                                            HelseOpplysningerArbeidsuforhet().apply {
                                                syketilfelleStartDato = null
                                                pasient =
                                                    HelseOpplysningerArbeidsuforhet.Pasient()
                                                        .apply {
                                                            navn =
                                                                NavnType().apply {
                                                                    fornavn = sykmelding.pasient.navn?.fornavn
                                                                    mellomnavn =
                                                                        sykmelding.pasient.navn?.mellomnavn
                                                                    etternavn =
                                                                        sykmelding.pasient.navn?.etternavn
                                                                }
                                                            fodselsnummer =
                                                                Ident().apply {
                                                                    id = sykmelding.pasient.fnr
                                                                    typeId =
                                                                        CV().apply {
                                                                            dn = "Fødselsnummer"
                                                                            s =
                                                                                "2.16.578.1.12.4.1.1.8116"
                                                                            v = "FNR"
                                                                        }
                                                                }
                                                        }
                                                arbeidsgiver =
                                                    tilArbeidsgiver(
                                                        sykmelding.arbeidsgiver,
                                                    )
                                                medisinskVurdering =
                                                    tilMedisinskVurdering(
                                                        sykmelding.medisinskVurdering,
                                                    )
                                                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet()
                                                    .apply {
                                                        periode.addAll(
                                                            tilPeriodeListe(
                                                                perioder
                                                            )
                                                        )
                                                    }
                                                prognose = null
                                                utdypendeOpplysninger = null
                                                tiltak =
                                                    HelseOpplysningerArbeidsuforhet.Tiltak().apply {
                                                        tiltakArbeidsplassen = when(val arbeidsgiver = sykmelding.arbeidsgiver) {
                                                            is IngenArbeidsgiver -> null
                                                            is EnArbeidsgiver -> arbeidsgiver.tiltakArbeidsplassen
                                                            is FlereArbeidsgivere -> arbeidsgiver.tiltakArbeidsplassen
                                                        }
                                                        tiltakNAV = null
                                                        andreTiltak = null
                                                    }
                                                meldingTilNav =
                                                    sykmelding.bistandNav
                                                        ?.let {
                                                            HelseOpplysningerArbeidsuforhet
                                                                .MeldingTilNav()
                                                                .apply {
                                                                    beskrivBistandNAV =
                                                                        it.beskrivBistand
                                                                    isBistandNAVUmiddelbart =
                                                                        it.bistandUmiddelbart
                                                                }
                                                        }
                                                meldingTilArbeidsgiver = when(val arbeidsgiver = sykmelding.arbeidsgiver) {
                                                    is IngenArbeidsgiver -> null
                                                    is EnArbeidsgiver -> arbeidsgiver.meldingTilArbeidsgiver
                                                    is FlereArbeidsgivere -> arbeidsgiver.meldingTilArbeidsgiver
                                                }
                                                kontaktMedPasient =
                                                    HelseOpplysningerArbeidsuforhet
                                                        .KontaktMedPasient()
                                                        .apply {
                                                            kontaktDato =
                                                                sykmelding.tilbakedatering
                                                                    ?.kontaktDato
                                                            begrunnIkkeKontakt =
                                                                sykmelding
                                                                    .tilbakedatering
                                                                    ?.begrunnelse
                                                            behandletDato = sykmelding.metadata.genDate.toLocalDateTime()
                                                        }
                                                behandler = tilBehandler(sykmelding.behandler)
                                                avsenderSystem =
                                                    HelseOpplysningerArbeidsuforhet.AvsenderSystem()
                                                        .apply {
                                                            systemNavn = "syk-inn"
                                                            systemVersjon = "pilot"
                                                        }
                                                strekkode = "123456789qwerty"
                                            },
                                        )
                                    }
                            }
                    },
                )
            },
        )
    }
}
fun tilBehandler(sykmelder: no.nav.tsm.sykmelding.input.core.model.Behandler): HelseOpplysningerArbeidsuforhet.Behandler =
    HelseOpplysningerArbeidsuforhet.Behandler().apply {
        navn =
            NavnType().apply {
                fornavn = sykmelder.navn.fornavn
                mellomnavn = sykmelder.navn.mellomnavn
                etternavn = sykmelder.navn.etternavn
            }
        id.addAll(
            listOf(
                Ident().apply {
                    id = sykmelder.ids.first {it.type == PersonIdType.FNR}.id
                    typeId =
                        CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                },
                Ident().apply {
                    id = sykmelder.ids.first {it.type == PersonIdType.HPR}.id
                    typeId =
                        CV().apply {
                            dn = "HPR-nummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "HPR"
                        }
                },
            ),
        )
        adresse = Address()
        kontaktInfo.add(
            TeleCom().apply {
                typeTelecom =
                    CS().apply {
                        v = "HP"
                        dn = "Hovedtelefon"
                    }
                teleAddress = URL().apply { v = "tel:55553336" }
            },
        )
    }
fun tilPeriodeListe(
    perioder: List<Periode>
): List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode> {
    return ArrayList<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>().apply {
        addAll(
            perioder.map { tilHelseOpplysningerArbeidsuforhetPeriode(it) },
        )
    }
}

fun tilHelseOpplysningerArbeidsuforhetPeriode(
    periode: Periode
): HelseOpplysningerArbeidsuforhet.Aktivitet.Periode =
    HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
        periodeFOMDato = periode.fom
        periodeTOMDato = periode.tom
        aktivitetIkkeMulig =
            if (periode.aktivitetIkkeMulig != null) {
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                    medisinskeArsaker =
                        if (periode.aktivitetIkkeMulig.medisinskArsak != null) {
                            ArsakType().apply {
                                beskriv = periode.aktivitetIkkeMulig.medisinskArsak.beskrivelse
                                arsakskode.addAll(
                                    periode.aktivitetIkkeMulig.medisinskArsak.arsak
                                        .stream()
                                        .map {
                                            CS().apply {
                                                v = it.codeValue
                                                dn = it.text
                                            }
                                        }
                                        .collect(Collectors.toList()),
                                )
                            }
                        } else {
                            null
                        }
                    arbeidsplassen =
                        if (periode.aktivitetIkkeMulig.arbeidsrelatertArsak != null) {
                            ArsakType().apply {
                                beskriv =
                                    periode.aktivitetIkkeMulig.arbeidsrelatertArsak.beskrivelse
                                arsakskode.addAll(
                                    periode.aktivitetIkkeMulig.arbeidsrelatertArsak.arsak
                                        .stream()
                                        .map {
                                            CS().apply {
                                                v = it.codeValue
                                                dn = it.text
                                            }
                                        }
                                        .collect(Collectors.toList()),
                                )
                            }
                        } else {
                            null
                        }
                }
            } else {
                null
            }
        avventendeSykmelding =
            if (periode.avventendeInnspillTilArbeidsgiver != null) {
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                    innspillTilArbeidsgiver = periode.avventendeInnspillTilArbeidsgiver
                }
            } else {
                null
            }

        gradertSykmelding =
            if (periode.gradert != null) {
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.GradertSykmelding().apply {
                    sykmeldingsgrad = periode.gradert.grad
                    isReisetilskudd = periode.gradert.reisetilskudd
                }
            } else {
                null
            }

        behandlingsdager =
            periode.behandlingsdager?.let { behandlingsdager ->
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.Behandlingsdager().apply {
                    antallBehandlingsdagerUke = behandlingsdager
                }
            }

        isReisetilskudd = periode.reisetilskudd
    }

fun tilMedisinskVurdering(
    medisinskVurdering: no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering,
): HelseOpplysningerArbeidsuforhet.MedisinskVurdering {
    if (
        medisinskVurdering.hovedDiagnose == null &&
        medisinskVurdering.annenFraversArsak == null
    ) {
        log.warn("Sykmelding mangler hoveddiagnose og annenFraversArsak, avbryter..")
        throw IllegalStateException("Sykmelding mangler hoveddiagnose")
    }
    val hovedDiagnose = medisinskVurdering.hovedDiagnose?.let { toDiagnose(it) }
    val biDiagnoseListe: List<CV>? =
        medisinskVurdering.biDiagnoser?.map {
            toDiagnose(it)
        }

    return HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
        if (hovedDiagnose != null) {
            this.hovedDiagnose =
                HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                    diagnosekode = hovedDiagnose
                }
        }
        if (biDiagnoseListe != null && biDiagnoseListe.isNotEmpty()) {
            biDiagnoser =
                HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser().apply {
                    diagnosekode.addAll(biDiagnoseListe)
                }
        }
        isSkjermesForPasient = medisinskVurdering.skjermetForPasient
        annenFraversArsak = medisinskVurdering.annenFraversArsak?.let {
                ArsakType().apply {
                    arsakskode.add(CS())
                    beskriv = medisinskVurdering.annenFraversArsak?.beskrivelse
                }
            }
        isSvangerskap = medisinskVurdering.svangerskap
        isYrkesskade = medisinskVurdering.yrkesskade != null
        yrkesskadeDato = medisinskVurdering.yrkesskade?.yrkesskadeDato
    }
}

fun toDiagnose(diagnose: DiagnoseInfo): CV {
    val system = diagnose.toDiagnose().system
    val kode = diagnose.kode
    val diagnose = diagnose.tekst

    return CV().apply {
        s = system
        v = kode
        dn = diagnose
    }
}



fun tilArbeidsgiver(
    arbeidsgiver: ArbeidsgiverInfo,
): HelseOpplysningerArbeidsuforhet.Arbeidsgiver =
    HelseOpplysningerArbeidsuforhet.Arbeidsgiver().apply {
        harArbeidsgiver =
            when (arbeidsgiver.type) {
                ARBEIDSGIVER_TYPE.INGEN_ARBEIDSGIVER ->
                    CS().apply {
                        dn = "Ingen arbeidsgiver"
                        v = "3"
                    }

                ARBEIDSGIVER_TYPE.FLERE_ARBEIDSGIVERE ->
                    CS().apply {
                        dn = "Flere arbeidsgivere"
                        v = "2"
                    }

                ARBEIDSGIVER_TYPE.EN_ARBEIDSGIVER ->
                    CS().apply {
                        dn = "Én arbeidsgiver"
                        v = "1"
                    }
            }
        if (arbeidsgiver is EnArbeidsgiver) {
            navnArbeidsgiver = arbeidsgiver.navn
            yrkesbetegnelse = arbeidsgiver.yrkesbetegnelse
            stillingsprosent = arbeidsgiver.stillingsprosent
        }
        else if (arbeidsgiver is FlereArbeidsgivere) {
            navnArbeidsgiver = arbeidsgiver.navn
            yrkesbetegnelse = arbeidsgiver.yrkesbetegnelse
            stillingsprosent = arbeidsgiver.stillingsprosent
        }
    }


fun mapToMerknader(validation: ValidationResult): List<Merknad>? {
    val lastRule = validation.rules.sortedByDescending { it.timestamp }.firstOrNull() ?: return null

    val tilbakedatertMerknad = TilbakedatertMerknad.entries.firstOrNull {it.name == lastRule.name}?.toOldTilbakedateringMerknad()
        ?: return null
    val beskrivelse = when(tilbakedatertMerknad) {
        OldTilbakedatertMerknad.UNDER_BEHANDLING ->  "Sykmeldingen er til manuell vurdering for tilbakedatering"
        OldTilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER -> "Sykmeldingen er til manuell vurdering for tilbakedatering"
        else -> null
    }

    return listOf(Merknad(
        tilbakedatertMerknad.name, beskrivelse, validation.timestamp
    ))
}

private fun TilbakedatertMerknad.toOldTilbakedateringMerknad(): OldTilbakedatertMerknad {
    return when (this) {
        TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING -> OldTilbakedatertMerknad.UNDER_BEHANDLING
        TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING -> OldTilbakedatertMerknad.UGYLDIG_TILBAKEDATERING
        TilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER -> OldTilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER
        TilbakedatertMerknad.TILBAKEDATERING_DELVIS_GODKJENT -> OldTilbakedatertMerknad.DELVIS_GODKJENT
        TilbakedatertMerknad.TILBAKEDATERING_TILBAKEDATERT_PAPIRSYKMELDING -> OldTilbakedatertMerknad.TILBAKEDATERT_PAPIRSYKMELDING
    }

}

private fun Aktivitet.toPeriode() : Periode {
    return Periode(
        fom = fom,
        tom = tom,
        aktivitetIkkeMulig = when (this) {
            is AktivitetIkkeMulig -> AktivitetIkkeMuligLegacy(
                medisinskArsak = this.medisinskArsak?.let { medisinskArsak ->
                    MedisinskArsak(
                        medisinskArsak.beskrivelse, medisinskArsak.arsak.map { it.toMedisinskArsakType() }
                    )
                },
                arbeidsrelatertArsak = this.arbeidsrelatertArsak?.let { arbeidsrelatertArsak ->
                    ArbeidsrelatertArsak(
                        arbeidsrelatertArsak.beskrivelse,
                        arbeidsrelatertArsak.arsak.map { it.toArbeidsrelatertArsak() })
                }
            )
            else -> null
        },
        avventendeInnspillTilArbeidsgiver = when(this) {
            is Avventende -> innspillTilArbeidsgiver
            else -> null
        },
        behandlingsdager = when(this) {
            is Behandlingsdager -> this.antallBehandlingsdager
            else -> null
        },
        gradert = when(this) {
            is Gradert -> GradertLegacy(this.reisetilskudd, this.grad)
            else -> null
        },
        reisetilskudd = when(this) {
            is Reisetilskudd -> true
            else -> false
        }
    )
}

private fun ArbeidsrelatertArsakType.toArbeidsrelatertArsak(): ArbeidsrelatertArsakTypeLegacy {
    return when (this) {
        ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING -> ArbeidsrelatertArsakTypeLegacy.MANGLENDE_TILRETTELEGGING
        ArbeidsrelatertArsakType.ANNET -> ArbeidsrelatertArsakTypeLegacy.ANNET
    }
}

private fun MedisinskArsakType.toMedisinskArsakType(): MedisinskArsakTypeLegacy {
    return when (this) {
        MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET -> MedisinskArsakTypeLegacy.TILSTAND_HINDRER_AKTIVITET
        MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND -> MedisinskArsakTypeLegacy.AKTIVITET_FORVERRER_TILSTAND
        MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING -> MedisinskArsakTypeLegacy.AKTIVITET_FORHINDRER_BEDRING
        MedisinskArsakType.ANNET -> MedisinskArsakTypeLegacy.ANNET
    }
}

private fun AnnenFraverArsak.toAnnenFraversArsak(): AnnenFraversArsak? {
    return arsak?.let { arsaker ->
        AnnenFraversArsak(
            beskrivelse,  arsaker.map {
                when (it) {
                    AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON -> AnnenFraverGrunn.GODKJENT_HELSEINSTITUSJON
                    AnnenFravarArsakType.BEHANDLING_FORHINDRER_ARBEID -> AnnenFraverGrunn.BEHANDLING_FORHINDRER_ARBEID
                    AnnenFravarArsakType.ARBEIDSRETTET_TILTAK -> AnnenFraverGrunn.ARBEIDSRETTET_TILTAK
                    AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND -> AnnenFraverGrunn.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
                    AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE -> AnnenFraverGrunn.NODVENDIG_KONTROLLUNDENRSOKELSE
                    AnnenFravarArsakType.SMITTEFARE -> AnnenFraverGrunn.SMITTEFARE
                    AnnenFravarArsakType.ABORT -> AnnenFraverGrunn.ABORT
                    AnnenFravarArsakType.UFOR_GRUNNET_BARNLOSHET -> AnnenFraverGrunn.UFOR_GRUNNET_BARNLOSHET
                    AnnenFravarArsakType.DONOR -> AnnenFraverGrunn.DONOR
                    AnnenFravarArsakType.BEHANDLING_STERILISERING -> AnnenFraverGrunn.BEHANDLING_STERILISERING
                }
            }
        )
    }
}


private fun DiagnoseInfo.toDiagnose() : Diagnose {
    return Diagnose(
        kode = kode,
        tekst = tekst,
        system = when (system) {
            DiagnoseSystem.ICPC2 -> "2.16.578.1.12.4.1.1.7170"
            DiagnoseSystem.ICD10 -> "2.16.578.1.12.4.1.1.7110"
            DiagnoseSystem.ICPC2B -> "2.16.578.1.12.4.1.1.7171"
            DiagnoseSystem.PHBU -> "2.16.578.1.12.4.1.1.7112"
            else -> throw IllegalArgumentException("Ukjent diagnose system $system")
        },
    )
}
