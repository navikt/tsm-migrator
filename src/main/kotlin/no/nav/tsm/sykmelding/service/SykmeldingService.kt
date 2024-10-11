package no.nav.tsm.sykmelding.service

import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.helse.msgHead.XMLAddress
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLPatient
import no.nav.helse.msgHead.XMLReceiver
import no.nav.helse.msgHead.XMLSender
import no.nav.helse.sm2013.Address
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.tsm.smregister.models.AnnenFraverGrunn
import no.nav.tsm.smregister.models.Diagnose
import no.nav.tsm.smregister.models.HarArbeidsgiver
import no.nav.tsm.smregister.models.Merknad
import no.nav.tsm.smregister.models.Periode
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.smregister.models.Status
import no.nav.tsm.smregister.models.SvarRestriksjon
import no.nav.tsm.sykmelding.Aktivitet
import no.nav.tsm.sykmelding.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.AnnenFravarArsakType
import no.nav.tsm.sykmelding.AnnenFraverArsak
import no.nav.tsm.sykmelding.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.ArbeidsrelatertArsak
import no.nav.tsm.sykmelding.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.AvsenderSystem
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
import no.nav.tsm.sykmelding.MedisinskArsak
import no.nav.tsm.sykmelding.MedisinskArsakType
import no.nav.tsm.sykmelding.MedisinskVurdering
import no.nav.tsm.sykmelding.Prognose
import no.nav.tsm.sykmelding.Reisetilskudd
import no.nav.tsm.sykmelding.SignerendeBehandler
import no.nav.tsm.sykmelding.SporsmalSvar
import no.nav.tsm.sykmelding.Sykmelding
import no.nav.tsm.sykmelding.SykmeldingMedBehandlingsutfall
import no.nav.tsm.sykmelding.SykmeldingMetadata
import no.nav.tsm.sykmelding.Tiltak
import no.nav.tsm.sykmelding.metadata.Adresse
import no.nav.tsm.sykmelding.metadata.AdresseType
import no.nav.tsm.sykmelding.metadata.EDIEmottak
import no.nav.tsm.sykmelding.metadata.EmottakEnkel
import no.nav.tsm.sykmelding.metadata.Helsepersonell
import no.nav.tsm.sykmelding.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.metadata.Kjonn
import no.nav.tsm.sykmelding.metadata.Kontaktinfo
import no.nav.tsm.sykmelding.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.metadata.MeldingMetadata
import no.nav.tsm.sykmelding.metadata.Meldingstype
import no.nav.tsm.sykmelding.metadata.MottakenhetBlokk
import no.nav.tsm.sykmelding.metadata.Navn
import no.nav.tsm.sykmelding.metadata.OrgId
import no.nav.tsm.sykmelding.metadata.OrgIdType
import no.nav.tsm.sykmelding.metadata.Organisasjon
import no.nav.tsm.sykmelding.metadata.OrganisasjonsType
import no.nav.tsm.sykmelding.metadata.Pasient
import no.nav.tsm.sykmelding.metadata.PersonId
import no.nav.tsm.sykmelding.metadata.PersonIdType
import no.nav.tsm.sykmelding.metadata.RolleTilPasient
import no.nav.tsm.sykmelding.metadata.UnderOrganisasjon
import no.nav.tsm.sykmelding.util.get
import no.nav.tsm.sykmelding.util.getIdentType
import no.nav.tsm.sykmelding.util.safeUnmarshal
import no.nav.tsm.sykmelding.validation.InvalidRule
import no.nav.tsm.sykmelding.validation.OKRule
import no.nav.tsm.sykmelding.validation.PendingRule
import no.nav.tsm.sykmelding.validation.Rule
import no.nav.tsm.sykmelding.validation.RuleType
import no.nav.tsm.sykmelding.validation.ValidationResult
import java.time.OffsetDateTime
import java.time.ZoneOffset

enum class TilbakedatertMerknad {
    UNDER_BEHANDLING,
    UGYLDIG_TILBAKEDATERING,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    DELVIS_GODKJENT,
}

class SykmeldingService {
    fun toNewSykmelding(receivedSykmelding: ReceivedSykmelding): SykmeldingMedBehandlingsutfall? {
        val sykmeldingMedBehandlingsutfall = when (receivedSykmelding.fellesformat) {
            null -> emottakEnkel(receivedSykmelding)
            else -> fromReceivedSykmeldignAndFellesformat(receivedSykmelding)
        }
        return sykmeldingMedBehandlingsutfall
    }

    private fun toSender(sender: XMLSender): Organisasjon {
        val org = toOrganisasjon(sender.organisation)
        return org
    }

    private fun toOrganisasjon(organisation: XMLOrganisation) = Organisasjon(
        navn = organisation.organisationName,
        type = organisation.typeOrganisation?.let { OrganisasjonsType.parse(it.v) }
            ?: OrganisasjonsType.IKKE_OPPGITT,
        ids = organisation.ident.map {
            OrgId(
                id = it.id,
                type = OrgIdType.valueOf(it.typeId.v),
            )
        },
        adresse = toAdresse(organisation.address),
        kontaktinfo = organisation.teleCom.map {
            Kontaktinfo(
                type = KontaktinfoType.valueOf(it.typeTelecom.v),
                value = it.teleAddress.v,
            )
        },
        underOrganisasjon = tilUnderorganisasjon(organisation.organisation),
        helsepersonell = tilHelsepersonell(organisation.healthcareProfessional),
    )

    private fun tilHelsepersonell(healthcareProfessional: XMLHealthcareProfessional?): Helsepersonell? {
        if(healthcareProfessional == null) return null

        return Helsepersonell(
            ids = healthcareProfessional.ident.map {
                PersonId(
                    id = it.id,
                    type = PersonIdType.valueOf(it.typeId.v),
                )
            },
            navn = Navn(
                fornavn = healthcareProfessional.givenName,
                mellomnavn = healthcareProfessional.middleName,
                etternavn = healthcareProfessional.familyName,
            ),
            adresse = toAdresse(healthcareProfessional.address),
            kontaktinfo = healthcareProfessional.teleCom.map {
                Kontaktinfo(
                    type = KontaktinfoType.valueOf(it.typeTelecom.v),
                    value = it.teleAddress.v,
                )
            },
            kjonn = healthcareProfessional.sex?.let { Kjonn.parse(it.v) },
            nasjonalitet = healthcareProfessional.nationality.v,
            fodselsdato = healthcareProfessional.dateOfBirth,
            helsepersonellKategori = HelsepersonellKategori.parse(healthcareProfessional.typeHealthcareProfessional.v),
            rolleTilPasient = RolleTilPasient.parse(healthcareProfessional.roleToPatient.v),

        )
    }

    private fun toAdresse(address: XMLAddress) = Adresse(
        gateadresse = address.streetAdr,
        postnummer = address.postalCode,
        poststed = address.city,
        postboks = address.postbox,
        kommune = address.county.v,
        land = address.country.v,
        type = AdresseType.parse(address.type.v),
    )

    private fun toAdresse(address: Address) = Adresse(
        gateadresse = address.streetAdr,
        postnummer = address.postalCode,
        poststed = address.city,
        postboks = address.postbox,
        kommune = address.county.v,
        land = address.country.v,
        type = AdresseType.parse(address.type.v),
    )

    private fun tilUnderorganisasjon(organisation: XMLOrganisation?): UnderOrganisasjon? {
        if(organisation == null) return null
        return UnderOrganisasjon(
            navn = organisation.organisationName,
            type = organisation.typeOrganisation?.let { OrganisasjonsType.parse(it.v ) } ?: OrganisasjonsType.IKKE_OPPGITT,
            kontaktinfo = organisation.teleCom.map {
                Kontaktinfo(
                    type = KontaktinfoType.valueOf(it.typeTelecom.v),
                    value = it.teleAddress.v,
                )
            },
            ids = organisation.ident.map {
                OrgId(
                    id = it.id,
                    type = OrgIdType.valueOf(it.typeId.v),
                )
            },
            adresse = toAdresse(organisation.address),
        )
    }

    private fun toReceiver(receiver: XMLReceiver): Organisasjon {
        return toOrganisasjon(receiver.organisation)
    }

    private fun fromReceivedSykmeldignAndFellesformat(receivedSykmelding: ReceivedSykmelding): SykmeldingMedBehandlingsutfall {
        requireNotNull(receivedSykmelding.fellesformat)
        val unmashalledSykmelding = safeUnmarshal(receivedSykmelding.fellesformat)
        val msgHead = unmashalledSykmelding.get<XMLMsgHead>()
        val mottakenhetBlokk = unmashalledSykmelding.get<XMLMottakenhetBlokk>()
        val ediEmottak = toEdiEmottak(mottakenhetBlokk, msgHead, receivedSykmelding)
        if(msgHead.document.size > 1) {
            throw IllegalArgumentException("Forventet kun en dokument for ${receivedSykmelding.sykmelding.id}")
        }
        if(msgHead.document.single().refDoc.content.any.size > 1) {
            throw IllegalArgumentException("Forventet kun en helseopplysninger for ${receivedSykmelding.sykmelding.id}")
        }

        val xmlSykmelding = msgHead.document.single().refDoc.content.any.single()
                as HelseOpplysningerArbeidsuforhet

        val sykmeldingPasient = toSykmeldingPasient(xmlSykmelding.pasient)
        val sykmelding = Sykmelding(
            id = receivedSykmelding.sykmelding.id,
            metadata = SykmeldingMetadata(
                mottattDato = ediEmottak.mottakenhetBlokk.mottattDato,
                genDate = ediEmottak.msgInfo.genDate,
                behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt.atOffset(ZoneOffset.UTC),
                regelsettVersjon = xmlSykmelding.regelSettVersjon,
                avsenderSystem = AvsenderSystem(
                    navn = receivedSykmelding.sykmelding.avsenderSystem.navn,
                    versjon = receivedSykmelding.sykmelding.avsenderSystem.versjon,
                ),
                strekkode = xmlSykmelding.strekkode,
            ),
            pasient = sykmeldingPasient,
            behandler = Behandler(
                navn = Navn(
                    fornavn = receivedSykmelding.sykmelding.behandler.fornavn,
                    mellomnavn = receivedSykmelding.sykmelding.behandler.etternavn,
                    etternavn = receivedSykmelding.sykmelding.behandler.etternavn,
                ),
                adresse = toAdresse(xmlSykmelding.behandler.adresse),
                ids = xmlSykmelding.behandler.id.map {
                    PersonId(
                        id = it.id,
                        type = PersonIdType.valueOf(it.typeId.v),
                    )
                },
                kontaktinfo = xmlSykmelding.behandler.kontaktInfo.map {
                    Kontaktinfo(
                        type = KontaktinfoType.valueOf(it.typeTelecom.v),
                        value = it.teleAddress.v,
                    )
                },
            ),
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            medisinskVurdering = mapMedisinskVurdering(receivedSykmelding.sykmelding),
            prognose = mapPrognose(receivedSykmelding.sykmelding),
            tiltak = receivedSykmelding.sykmelding.tiltakNAV?.let {
                Tiltak(
                    tiltakNAV = it,
                    andreTiltak = receivedSykmelding.sykmelding.andreTiltak,
                )
            },
            bistandNav = receivedSykmelding.sykmelding.meldingTilNAV?.let {
                no.nav.tsm.sykmelding.BistandNav(
                    bistandUmiddelbart = it.bistandUmiddelbart,
                    beskrivBistand = it.beskrivBistand,
                )
            },
            tilbakedatering = receivedSykmelding.sykmelding.kontaktMedPasient?.let {
                no.nav.tsm.sykmelding.Tilbakedatering(
                    kontaktDato = it.kontaktDato,
                    begrunnelse = it.begrunnelseIkkeKontakt,
                )
            },
            aktivitet = receivedSykmelding.sykmelding.perioder.map { periode ->
                mapAktivitet(periode)
            },
            utdypendeOpplysninger = toUtdypendeOpplysninger(receivedSykmelding),
            signerendeBehandler = toSignerendeBehandler(receivedSykmelding),
        )

        return SykmeldingMedBehandlingsutfall(
            meldingsInformasjon = ediEmottak,
            sykmelding = sykmelding,
            validation = mapValidationResult(receivedSykmelding),
        )
    }




    private fun toSykmeldingPasient(pasient: HelseOpplysningerArbeidsuforhet.Pasient): no.nav.tsm.sykmelding.Pasient {

        return no.nav.tsm.sykmelding.Pasient(
            navn = Navn(
                fornavn = pasient.navn.fornavn,
                mellomnavn = pasient.navn.mellomnavn,
                etternavn = pasient.navn.etternavn,
            ),
            fnr = pasient.fodselsnummer.id,
            navnFastlege = pasient.navnFastlege,
            navKontor = pasient.navKontor,
            kontaktinfo = pasient.kontaktInfo.map {
                Kontaktinfo(
                    type = KontaktinfoType.valueOf(it.typeTelecom.v),
                    value = it.teleAddress.v,
                )
            },
        )
    }

    private fun toEdiEmottak(
        mottakenhetBlokk: XMLMottakenhetBlokk,
        msgHead: XMLMsgHead,
        receivedSykmelding: ReceivedSykmelding
    ) = EDIEmottak(
        mottakenhetBlokk = MottakenhetBlokk(
            ediLogid = mottakenhetBlokk.ediLoggId,
            avsender = mottakenhetBlokk.avsender,
            ebXMLSamtaleId = mottakenhetBlokk.ebXMLSamtaleId,
            mottaksId = mottakenhetBlokk.mottaksId,
            meldingsType = mottakenhetBlokk.meldingsType,
            avsenderRef = mottakenhetBlokk.avsenderRef,
            avsenderFnrFraDigSignatur = mottakenhetBlokk.avsenderFnrFraDigSignatur,
            mottattDato = mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime()
                .toOffsetDateTime(),
            orgnummer = mottakenhetBlokk.orgNummer,
            avsenderOrgNrFraDigSignatur = mottakenhetBlokk.avsenderOrgNrFraDigSignatur,
            partnerReferanse = mottakenhetBlokk.partnerReferanse,
            herIdentifikator = mottakenhetBlokk.herIdentifikator,
            ebRole = mottakenhetBlokk.ebRole,
            ebService = mottakenhetBlokk.ebService,
            ebAction = mottakenhetBlokk.ebAction,
        ),
        msgInfo = MeldingMetadata(
            type = Meldingstype.parse(msgHead.msgInfo.type.v),
            genDate = OffsetDateTime.parse(msgHead.msgInfo.genDate),
            msgId = receivedSykmelding.msgId ?: throw IllegalArgumentException("Mangler msgId"),
            migVersjon = msgHead.msgInfo.msgId,
        ),
        sender = toSender(msgHead.msgInfo.sender),
        receiver = toReceiver(msgHead.msgInfo.receiver),
        pasient = toPasient(msgHead.msgInfo.patient),
        vedlegg = receivedSykmelding.vedlegg,
    )

    private fun toPasient(xmlPasient: XMLPatient?): Pasient? {
        if (xmlPasient == null) return null

        return Pasient(
            ids = xmlPasient.ident.map {
                PersonId(
                    id = it.id,
                    type = PersonIdType.valueOf(it.typeId.v),
                )
            },
            navn = Navn(
                fornavn = xmlPasient.givenName,
                mellomnavn = xmlPasient.middleName,
                etternavn = xmlPasient.familyName,
            ),
            fodselsdato = xmlPasient.dateOfBirth,
            kjonn = Kjonn.parse(xmlPasient.sex.v),
            nasjonalitet = xmlPasient.nationality.v,
            adresse = toAdresse(xmlPasient.address),
            kontaktinfo = xmlPasient.teleCom.map {
                Kontaktinfo(
                    type = KontaktinfoType.valueOf(it.typeTelecom.v),
                    value = it.teleAddress.v,
                )
            },
        )
    }


    private fun emottakEnkel(receivedSykmelding: ReceivedSykmelding): SykmeldingMedBehandlingsutfall {
        val emottakEnkel = EmottakEnkel(
            msgInfo = MeldingMetadata(
                type = Meldingstype.SYKMELDING,
                genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(ZoneOffset.UTC),
                msgId = receivedSykmelding.msgId ?: throw IllegalArgumentException("Mangler msgId"),
                migVersjon = null,
            ),
            receiver = Organisasjon(
                navn = "NAV IKT",
                type = OrganisasjonsType.IKKE_OPPGITT,
                ids = listOf(
                    OrgId(
                        id = "79768",
                        type = OrgIdType.HER,
                    ),
                    OrgId(
                        id = "990983291",
                        type = OrgIdType.ENH,
                    )
                ),
                adresse = Adresse(
                    type = AdresseType.UKJENT,
                    gateadresse = "Postboks 5 St Olavs plass",
                    postnummer = "0130",
                    poststed = "OSLO",
                    kommune = null,
                    postboks = null,
                    land = null,
                ),
                kontaktinfo = null,
                underOrganisasjon = null,
                helsepersonell = null,
            ),
            sender = Organisasjon(
                navn = receivedSykmelding.legekontorOrgName ?: "Ukjent",
                type = OrganisasjonsType.IKKE_OPPGITT,
                ids = listOfNotNull(
                    receivedSykmelding.legekontorHerId?.let {
                        OrgId(
                            id = it,
                            type = OrgIdType.HER,
                        )
                    }, receivedSykmelding.legekontorOrgNr?.let {
                        OrgId(
                            id = it,
                            type = OrgIdType.ENH,
                        )
                    }, receivedSykmelding.legekontorReshId?.let {
                        OrgId(
                            id = it,
                            type = OrgIdType.RSH,
                        )
                    }
                ),

                adresse = null,
                kontaktinfo = null,
                underOrganisasjon = null,
                helsepersonell = null,
            ),
            vedlegg = receivedSykmelding.vedlegg,
        )

        val validation = mapValidationResult(receivedSykmelding)
        val sykmelding = Sykmelding(
            id = receivedSykmelding.sykmelding.id,
            metadata = SykmeldingMetadata(
                mottattDato = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                genDate = emottakEnkel.msgInfo.genDate,
                behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt.atOffset(ZoneOffset.UTC),
                regelsettVersjon = receivedSykmelding.rulesetVersion,
                strekkode = null,
                avsenderSystem = AvsenderSystem(
                    navn = receivedSykmelding.sykmelding.avsenderSystem.navn,
                    versjon = receivedSykmelding.sykmelding.avsenderSystem.versjon
                ),
            ),
            pasient = toPasient(receivedSykmelding),
            behandler = toBehandler(receivedSykmelding),
            signerendeBehandler = toSignerendeBehandler(receivedSykmelding),
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            medisinskVurdering = mapMedisinskVurdering(receivedSykmelding.sykmelding),
            prognose = mapPrognose(receivedSykmelding.sykmelding),
            tiltak = receivedSykmelding.sykmelding.tiltakNAV?.let {
                Tiltak(
                    tiltakNAV = it,
                    andreTiltak = receivedSykmelding.sykmelding.andreTiltak,
                )
            },
            bistandNav = receivedSykmelding.sykmelding.meldingTilNAV?.let {
                no.nav.tsm.sykmelding.BistandNav(
                    bistandUmiddelbart = it.bistandUmiddelbart,
                    beskrivBistand = it.beskrivBistand,
                )
            },
            tilbakedatering = receivedSykmelding.sykmelding.kontaktMedPasient.let {
                no.nav.tsm.sykmelding.Tilbakedatering(
                    kontaktDato = it.kontaktDato,
                    begrunnelse = it.begrunnelseIkkeKontakt,
                )
            },
            aktivitet = receivedSykmelding.sykmelding.perioder.map {
                mapAktivitet(it)
            },
            utdypendeOpplysninger = toUtdypendeOpplysninger(receivedSykmelding),
        )
        return SykmeldingMedBehandlingsutfall(
            meldingsInformasjon = emottakEnkel,
            sykmelding = sykmelding,
            validation = validation,
        )
    }

    private fun toSignerendeBehandler(receivedSykmelding: ReceivedSykmelding) : SignerendeBehandler {
        return SignerendeBehandler(
            ids = listOfNotNull(
                receivedSykmelding.legeHprNr?.let {
                    PersonId(
                        id = it,
                        type = PersonIdType.HPR,
                    )
                },
                receivedSykmelding.personNrLege.let {
                    PersonId(
                        id = it,
                        type = PersonIdType.FNR,
                    )
                },
            ),
            helsepersonellKategori = HelsepersonellKategori.parse(receivedSykmelding.legeHelsepersonellkategori),
        )
    }


    private fun toUtdypendeOpplysninger(receivedSykmelding: ReceivedSykmelding) =
        receivedSykmelding.sykmelding.utdypendeOpplysninger.mapValues { entry ->
            entry.value.mapValues { questions ->
                SporsmalSvar(
                    sporsmal = questions.key,
                    svar = questions.value.svar,
                    restriksjoner = questions.value.restriksjoner.map {
                        when (it) {
                            SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER -> no.nav.tsm.sykmelding.SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER
                            SvarRestriksjon.SKJERMET_FOR_PASIENT -> no.nav.tsm.sykmelding.SvarRestriksjon.SKJERMET_FOR_PASIENT
                            SvarRestriksjon.SKJERMET_FOR_NAV -> no.nav.tsm.sykmelding.SvarRestriksjon.SKJERMET_FOR_NAV
                        }
                    }
                )
            }
        }

    private fun toBehandler(receivedSykmelding: ReceivedSykmelding): Behandler {
        val behandler = receivedSykmelding.sykmelding.behandler
        val ids = mutableListOf(PersonId(behandler.fnr, getIdentType(behandler.fnr)))
        if (behandler.her != null) {
            ids.add(PersonId(behandler.her, PersonIdType.HER))
        }
        if (behandler.hpr != null) {
            ids.add(PersonId(behandler.hpr, PersonIdType.HPR))
        }

        val validAddress = behandler.adresse.gate != null
                || behandler.adresse.postboks != null
                || behandler.adresse.postnummer != null
                || behandler.adresse.kommune != null

        val adresse = when (validAddress) {
            false -> null
            true -> Adresse(
                type = AdresseType.UKJENT,
                gateadresse = behandler.adresse.gate,
                postnummer = behandler.adresse.postnummer?.let {
                    it.toString().padStart(4, '0')
                },
                kommune = behandler.adresse.kommune,
                postboks = behandler.adresse.postboks,
                land = behandler.adresse.land?.let {
                    if (it == "Country") {
                        null
                    } else {
                        it
                    }
                },
                poststed = null,
            )
        }

        return Behandler(
            navn = Navn(
                behandler.fornavn,
                behandler.mellomnavn,
                behandler.etternavn,
            ),
            ids = ids,
            adresse = adresse,
            kontaktinfo = behandler.tlf?.let { listOf(Kontaktinfo(KontaktinfoType.TLF, it)) } ?: emptyList(),
        )
    }

    private fun toPasient(receivedSykmelding: ReceivedSykmelding): no.nav.tsm.sykmelding.Pasient {
        val ident = receivedSykmelding.personNrPasient

        return no.nav.tsm.sykmelding.Pasient(
            fnr = ident,
            navn = null,
            kontaktinfo = receivedSykmelding.tlfPasient?.let { listOf(Kontaktinfo(KontaktinfoType.TLF, it)) }
                ?: emptyList(),
            navnFastlege = receivedSykmelding.sykmelding.navnFastlege,
            navKontor = null,
        )
    }


    private fun mapValidationResult(
        receivedSykmelding: ReceivedSykmelding,
    ): ValidationResult {
        return when (receivedSykmelding.validationResult!!.status) {
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
            rules = receivedSykmelding.validationResult!!.ruleHits.mapNotNull {
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
        if (receivedSykmelding.merknader.isNullOrEmpty()) {
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
                description = it.beskrivelse ?: "Delvis godkjent tilbakedatering"
            )
        }
    }

}

private fun mapMedisinskVurdering(sykmelding: no.nav.tsm.smregister.models.Sykmelding): MedisinskVurdering {
    return MedisinskVurdering(
        hovedDiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.let(toDiagnoseInfo()),
        biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser.map(toDiagnoseInfo()),
        annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak?.let {
            AnnenFraverArsak(
                it.beskrivelse, when (it.grunn.single()) {
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
                }
            )
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
            "2.16.578.1.12.4.1.1.7170" -> DiagnoseSystem.ICPC2
            "2.16.578.1.12.4.1.1.7110" -> DiagnoseSystem.ICD10
            else -> throw IllegalArgumentException("Ukjent diagnose system")
        },
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

private fun mapArbeid(
    erIArbeid: no.nav.tsm.smregister.models.ErIArbeid?,
    erIkkeIArbeid: no.nav.tsm.smregister.models.ErIkkeIArbeid?
): IArbeid? {
    if (erIArbeid != null) {
        return ErIArbeid(
            egetArbeidPaSikt = erIArbeid.egetArbeidPaSikt,
            annetArbeidPaSikt = erIArbeid.annetArbeidPaSikt,
            arbeidFOM = erIArbeid.arbeidFOM,
            vurderingsdato = erIArbeid.vurderingsdato,
        )
    }

    if (erIkkeIArbeid != null) {
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
        if (periode.aktivitetIkkeMulig.medisinskArsak == null && periode.aktivitetIkkeMulig.arbeidsrelatertArsak == null) {
            throw IllegalArgumentException("Mangler medisinsk eller arbeidsrelatert årsak")
        }
        return AktivitetIkkeMulig(
            medisinskArsak = periode.aktivitetIkkeMulig.medisinskArsak?.let {
                MedisinskArsak(
                    it.beskrivelse,
                    MedisinskArsakType.ANNET
                )
            },
            arbeidsrelatertArsak = periode.aktivitetIkkeMulig.arbeidsrelatertArsak?.let {
                ArbeidsrelatertArsak(
                    it.beskrivelse,
                    ArbeidsrelatertArsakType.ANNET
                )
            },
            fom = periode.fom,
            tom = periode.tom
        )
    }

    if (periode.reisetilskudd) {
        return Reisetilskudd(
            fom = periode.fom,
            tom = periode.tom,
        )
    }

    if (periode.gradert != null) {
        return Gradert(
            fom = periode.fom,
            tom = periode.tom,
            grad = periode.gradert.grad,
            reisetilskudd = periode.gradert.reisetilskudd,
        )
    }

    if (periode.behandlingsdager != null) {
        return Behandlingsdager(
            fom = periode.fom,
            tom = periode.tom,
            antallBehandlingsdager = periode.behandlingsdager,
        )
    }

    if (periode.avventendeInnspillTilArbeidsgiver != null) {
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

