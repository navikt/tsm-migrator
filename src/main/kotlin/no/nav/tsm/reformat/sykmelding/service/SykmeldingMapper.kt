package no.nav.tsm.reformat.sykmelding.service

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
import no.nav.tsm.reformat.sykmelding.SporsmalSvar
import no.nav.tsm.reformat.sykmelding.model.Aktivitet
import no.nav.tsm.reformat.sykmelding.model.AktivitetIkkeMulig
import no.nav.tsm.reformat.sykmelding.model.AnnenFravarArsakType
import no.nav.tsm.reformat.sykmelding.model.AnnenFraverArsak
import no.nav.tsm.reformat.sykmelding.model.ArbeidsgiverInfo
import no.nav.tsm.reformat.sykmelding.model.ArbeidsrelatertArsak
import no.nav.tsm.reformat.sykmelding.model.ArbeidsrelatertArsakType
import no.nav.tsm.reformat.sykmelding.model.AvsenderSystem
import no.nav.tsm.reformat.sykmelding.model.Avventende
import no.nav.tsm.reformat.sykmelding.model.Behandler
import no.nav.tsm.reformat.sykmelding.model.Behandlingsdager
import no.nav.tsm.reformat.sykmelding.model.BistandNav
import no.nav.tsm.reformat.sykmelding.model.DiagnoseInfo
import no.nav.tsm.reformat.sykmelding.model.DiagnoseSystem
import no.nav.tsm.reformat.sykmelding.model.EnArbeidsgiver
import no.nav.tsm.reformat.sykmelding.model.ErIArbeid
import no.nav.tsm.reformat.sykmelding.model.ErIkkeIArbeid
import no.nav.tsm.reformat.sykmelding.model.FlereArbeidsgivere
import no.nav.tsm.reformat.sykmelding.model.Gradert
import no.nav.tsm.reformat.sykmelding.model.IArbeid
import no.nav.tsm.reformat.sykmelding.model.IngenArbeidsgiver
import no.nav.tsm.reformat.sykmelding.model.MedisinskArsak
import no.nav.tsm.reformat.sykmelding.model.MedisinskArsakType
import no.nav.tsm.reformat.sykmelding.model.MedisinskVurdering
import no.nav.tsm.reformat.sykmelding.model.Papirsykmelding
import no.nav.tsm.reformat.sykmelding.model.Prognose
import no.nav.tsm.reformat.sykmelding.model.Reisetilskudd
import no.nav.tsm.reformat.sykmelding.model.Sykmelder
import no.nav.tsm.reformat.sykmelding.model.Sykmelding
import no.nav.tsm.reformat.sykmelding.model.XmlSykmelding
import no.nav.tsm.reformat.sykmelding.model.SykmeldingMetadata
import no.nav.tsm.reformat.sykmelding.model.SykmeldingRecord
import no.nav.tsm.reformat.sykmelding.model.Tilbakedatering
import no.nav.tsm.reformat.sykmelding.model.Tiltak
import no.nav.tsm.reformat.sykmelding.model.UtenlandskSykmelding
import no.nav.tsm.reformat.sykmelding.model.Yrkesskade
import no.nav.tsm.reformat.sykmelding.model.metadata.Ack
import no.nav.tsm.reformat.sykmelding.model.metadata.AckType
import no.nav.tsm.reformat.sykmelding.model.metadata.Adresse
import no.nav.tsm.reformat.sykmelding.model.metadata.AdresseType
import no.nav.tsm.reformat.sykmelding.model.metadata.EDIEmottak
import no.nav.tsm.reformat.sykmelding.model.metadata.Egenmeldt
import no.nav.tsm.reformat.sykmelding.model.metadata.EmottakEnkel
import no.nav.tsm.reformat.sykmelding.model.metadata.Helsepersonell
import no.nav.tsm.reformat.sykmelding.model.metadata.HelsepersonellKategori
import no.nav.tsm.reformat.sykmelding.model.metadata.Kjonn
import no.nav.tsm.reformat.sykmelding.model.metadata.Kontaktinfo
import no.nav.tsm.reformat.sykmelding.model.metadata.KontaktinfoType
import no.nav.tsm.reformat.sykmelding.model.metadata.MessageInfo
import no.nav.tsm.reformat.sykmelding.model.metadata.Meldingstype
import no.nav.tsm.reformat.sykmelding.model.metadata.MottakenhetBlokk
import no.nav.tsm.reformat.sykmelding.model.metadata.Navn
import no.nav.tsm.reformat.sykmelding.model.metadata.OrgId
import no.nav.tsm.reformat.sykmelding.model.metadata.OrgIdType
import no.nav.tsm.reformat.sykmelding.model.metadata.Organisasjon
import no.nav.tsm.reformat.sykmelding.model.metadata.OrganisasjonsType
import no.nav.tsm.reformat.sykmelding.model.metadata.Papir
import no.nav.tsm.reformat.sykmelding.model.metadata.PersonId
import no.nav.tsm.reformat.sykmelding.model.metadata.PersonIdType
import no.nav.tsm.reformat.sykmelding.model.metadata.RolleTilPasient
import no.nav.tsm.reformat.sykmelding.model.metadata.UnderOrganisasjon
import no.nav.tsm.reformat.sykmelding.model.metadata.Utenlandsk
import no.nav.tsm.reformat.sykmelding.util.XmlStuff
import no.nav.tsm.reformat.sykmelding.util.get
import no.nav.tsm.reformat.sykmelding.util.getIdentType
import no.nav.tsm.reformat.sykmelding.validation.InvalidRule
import no.nav.tsm.reformat.sykmelding.validation.OKRule
import no.nav.tsm.reformat.sykmelding.validation.PendingRule
import no.nav.tsm.reformat.sykmelding.validation.Reason
import no.nav.tsm.reformat.sykmelding.validation.Rule
import no.nav.tsm.reformat.sykmelding.validation.RuleType
import no.nav.tsm.reformat.sykmelding.validation.ValidationResult
import no.nav.tsm.reformat.sykmelding.validation.ValidationType
import no.nav.tsm.smregister.models.AnnenFraverGrunn
import no.nav.tsm.smregister.models.Diagnose
import no.nav.tsm.smregister.models.HarArbeidsgiver
import no.nav.tsm.smregister.models.Merknad
import no.nav.tsm.smregister.models.Periode
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.smregister.models.Status
import no.nav.tsm.smregister.models.SvarRestriksjon
import no.nav.tsm.smregister.models.UtenlandskInfo
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeParseException
import java.util.GregorianCalendar

enum class OldTilbakedatertMerknad {
    UNDER_BEHANDLING,
    UGYLDIG_TILBAKEDATERING,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    DELVIS_GODKJENT,
    TILBAKEDATERT_PAPIRSYKMELDING,
}

enum class TilbakedatertMerknad {
    TILBAKEDATERING_UNDER_BEHANDLING,
    TILBAKEDATERING_UGYLDIG_TILBAKEDATERING,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    TILBAKEDATERING_DELVIS_GODKJENT,
    TILBAKEDATERING_TILBAKEDATERT_PAPIRSYKMELDING
}
class MappingException(val receivedSykmelding: ReceivedSykmelding, val exception: Exception) : Exception() {
    override val message: String?
        get() = exception.message
}
class SykmeldingMapper {
    private val xmlStuff = XmlStuff()
    fun toNewSykmelding(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        try {
            return when {
                receivedSykmelding.utenlandskSykmelding != null -> toUtenlandssykmeldingMedBehandlingsutfall(receivedSykmelding)
                receivedSykmelding.sykmelding.avsenderSystem.navn == "Papirsykmelding" -> toPapirsykmelding(
                    receivedSykmelding
                )
                receivedSykmelding.sykmelding.avsenderSystem.navn == "Egenmeldt" -> toEgenmeldtSykmelding(receivedSykmelding)
                !receivedSykmelding.fellesformat.isNullOrBlank() -> fromReceivedSykmeldignAndFellesformat(receivedSykmelding)
                else -> emottakEnkel(receivedSykmelding)
            }
        } catch (e: Exception) {

            throw MappingException(receivedSykmelding, e)
        }
    }

    private fun toEgenmeldtSykmelding(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        requireNotNull(receivedSykmelding.fellesformat) { "Fellesformat is required for egenmeldt sykmelding" }
        val unmashalledSykmelding = xmlStuff.unmarshal(receivedSykmelding.fellesformat)
        val msgHead = unmashalledSykmelding.get<XMLMsgHead>()
        return SykmeldingRecord(
            validation = mapValidationResult(receivedSykmelding),
            metadata = Egenmeldt(
                msgInfo = toMeldingMetadata(receivedSykmelding, msgHead),
            ),
            sykmelding = toSykmelding(receivedSykmelding)
        )
    }

    private fun toPapirsykmelding(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        requireNotNull(receivedSykmelding.fellesformat) { "Fellesformat is required for papirsykmelding" }

        val unmashalledSykmelding = xmlStuff.unmarshal(receivedSykmelding.fellesformat)
        val msgHead = unmashalledSykmelding.get<XMLMsgHead>()
        return SykmeldingRecord(
            validation = mapValidationResult(receivedSykmelding),
            metadata = Papir(
                sender = toSender(msgHead.msgInfo.sender),
                receiver = toReceiver(msgHead.msgInfo.receiver),
                msgInfo = toMeldingMetadata(receivedSykmelding, msgHead),
                journalPostId = receivedSykmelding.sykmelding.avsenderSystem.versjon
            ),
            sykmelding = toPapirSykmelding(receivedSykmelding),
        )
    }

    private fun toUtenlandssykmeldingMedBehandlingsutfall(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        requireNotNull(receivedSykmelding.utenlandskSykmelding) { "UtenlandskSykmelding is required for utenlandssykmelding" }

        val sykmeldingMedBehandlingsutfall = SykmeldingRecord(
            validation = mapValidationResult(receivedSykmelding),
            metadata = Utenlandsk(
                land = receivedSykmelding.utenlandskSykmelding.land,
                journalPostId = receivedSykmelding.sykmelding.avsenderSystem.versjon
            ),
            sykmelding = toUtenlandskSykmelding(receivedSykmelding),
        )
        return sykmeldingMedBehandlingsutfall
    }

    private fun toUtenlandskSykmelding(
        receivedSykmelding: ReceivedSykmelding,
    ): UtenlandskSykmelding {
        requireNotNull(receivedSykmelding.utenlandskSykmelding)
        return UtenlandskSykmelding(
            id = receivedSykmelding.sykmelding.id,
            metadata = SykmeldingMetadata(
                mottattDato = receivedSykmelding.mottattDato.atOffset(UTC),
                genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(UTC),
                behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt.atOffset(UTC),
                regelsettVersjon = receivedSykmelding.rulesetVersion,
                strekkode = null,
                avsenderSystem = AvsenderSystem(
                    navn = receivedSykmelding.sykmelding.avsenderSystem.navn,
                    versjon = receivedSykmelding.sykmelding.avsenderSystem.versjon
                ),
            ),
            pasient = toPasient(receivedSykmelding),
            medisinskVurdering = mapMedisinskVurdering(receivedSykmelding.sykmelding),
            aktivitet = receivedSykmelding.sykmelding.perioder.map {
                mapAktivitet(it)
            },
            utenlandskInfo = UtenlandskInfo(
                land = receivedSykmelding.utenlandskSykmelding.land,
                folkeRegistertAdresseErBrakkeEllerTilsvarende = receivedSykmelding.utenlandskSykmelding.folkeRegistertAdresseErBrakkeEllerTilsvarende,
                erAdresseUtland = receivedSykmelding.utenlandskSykmelding.erAdresseUtland
            )
        )
    }

    private fun toMeldingMetadata(
        receivedSykmelding: ReceivedSykmelding,
        msgHead: XMLMsgHead
    ) = MessageInfo(
        type = Meldingstype.SYKMELDING,
        genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(UTC),
        msgId = receivedSykmelding.msgId ?: throw IllegalArgumentException("Mangler msgId"),
        migVersjon = msgHead.msgInfo.miGversion,
    )

    private fun toSender(sender: XMLSender): Organisasjon {
        return toOrganisasjon(sender.organisation)
    }

    private fun toOrganisasjon(organisation: XMLOrganisation) = Organisasjon(
        navn = organisation.organisationName,
        type = organisation.typeOrganisation?.let { OrganisasjonsType.parse(it.v) }
            ?: OrganisasjonsType.IKKE_OPPGITT,
        ids = organisation.ident.map {
            OrgId(
                id = it.id,
                type = OrgIdType.parse(it.typeId.v),
            )
        },
        adresse = toAdresse(organisation.address),
        kontaktinfo = organisation.teleCom.map {
            Kontaktinfo(
                type = KontaktinfoType.parse(it.typeTelecom?.v),
                value = it.teleAddress.v,
            )
        },
        underOrganisasjon = tilUnderorganisasjon(organisation.organisation),
        helsepersonell = tilHelsepersonell(organisation.healthcareProfessional),
    )

    private fun tilHelsepersonell(healthcareProfessional: XMLHealthcareProfessional?): Helsepersonell? {
        if (healthcareProfessional == null) return null
        val invalidName = healthcareProfessional.givenName == null || healthcareProfessional.familyName == null
        val name = if(!invalidName) {
            Navn(
                fornavn = healthcareProfessional.givenName,
                mellomnavn = healthcareProfessional.middleName,
                etternavn = healthcareProfessional.familyName,
            )
        } else {
            null
        }
        return Helsepersonell(
            ids = healthcareProfessional.ident.map {
                PersonId(
                    id = it.id,
                    type = PersonIdType.parse(it.typeId.v),
                )
            },
            navn = name,
            adresse = toAdresse(healthcareProfessional.address),
            kontaktinfo = healthcareProfessional.teleCom.map {
                Kontaktinfo(
                    type = KontaktinfoType.parse(it.typeTelecom?.v),
                    value = it.teleAddress.v,
                )
            },
            kjonn = healthcareProfessional.sex?.let { Kjonn.parse(it.v) },
            nasjonalitet = healthcareProfessional.nationality?.v,
            fodselsdato = healthcareProfessional.dateOfBirth,
            helsepersonellKategori = HelsepersonellKategori.parse(healthcareProfessional.typeHealthcareProfessional?.v),
            rolleTilPasient = RolleTilPasient.parse(healthcareProfessional.roleToPatient?.v),

            )
    }

    private fun toAdresse(address: XMLAddress?) : Adresse? {
        if (address == null) return null
        return Adresse(
            gateadresse = address.streetAdr,
            postnummer = address.postalCode,
            poststed = address.city,
            postboks = address.postbox,
            kommune = address.county?.v,
            land = address.country?.v,
            type = AdresseType.parse(address.type?.v),
        )
    }

    private fun toAdresse(address: Address) = Adresse(
        gateadresse = address.streetAdr,
        postnummer = address.postalCode,
        poststed = address.city,
        postboks = address.postbox,
        kommune = address.county?.v,
        land = address.country?.v,
        type = AdresseType.parse(address.type?.v),
    )

    private fun tilUnderorganisasjon(organisation: XMLOrganisation?): UnderOrganisasjon? {
        if (organisation == null) return null
        return UnderOrganisasjon(
            navn = organisation.organisationName,
            type = organisation.typeOrganisation?.let { OrganisasjonsType.parse(it.v) }
                ?: OrganisasjonsType.IKKE_OPPGITT,
            kontaktinfo = organisation.teleCom.map {
                Kontaktinfo(
                    type = KontaktinfoType.parse(it.typeTelecom?.v),
                    value = it.teleAddress.v,
                )
            },
            ids = organisation.ident.map {
                OrgId(
                    id = it.id,
                    type = OrgIdType.parse(it.typeId.v),
                )
            },
            adresse = toAdresse(organisation.address),
        )
    }

    private fun toReceiver(receiver: XMLReceiver): Organisasjon {
        return toOrganisasjon(receiver.organisation)
    }

    private fun fromReceivedSykmeldignAndFellesformat(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        requireNotNull(receivedSykmelding.fellesformat)
        val unmashalledSykmelding = xmlStuff.unmarshal(receivedSykmelding.fellesformat)
        val msgHead = unmashalledSykmelding.get<XMLMsgHead>()
        val mottakenhetBlokk = unmashalledSykmelding.get<XMLMottakenhetBlokk>()


        if (msgHead.document.size > 1) {
            throw IllegalArgumentException("Forventet kun en dokument for ${receivedSykmelding.sykmelding.id}")
        }
        if (msgHead.document.single().refDoc.content.any.size > 1) {
            throw IllegalArgumentException("Forventet kun en helseopplysninger for ${receivedSykmelding.sykmelding.id}")
        }

        val xmlSykmelding = msgHead.document.single().refDoc.content.any.single()
                as HelseOpplysningerArbeidsuforhet

        val sykmeldingPasient = toSykmeldingPasient(xmlSykmelding.pasient)
        val sykmelder = toSignerendeBehandler(receivedSykmelding)
        val behandler = Behandler(
            navn = Navn(
                fornavn = receivedSykmelding.sykmelding.behandler.fornavn,
                mellomnavn = receivedSykmelding.sykmelding.behandler.mellomnavn,
                etternavn = receivedSykmelding.sykmelding.behandler.etternavn,
            ),
            adresse = toAdresse(xmlSykmelding.behandler.adresse),
            ids = xmlSykmelding.behandler.id.map {
                PersonId(
                    id = it.id,
                    type = PersonIdType.parse(it.typeId.v),
                )
            },
            kontaktinfo = xmlSykmelding.behandler.kontaktInfo.filter {
                it.teleAddress.v != null
            }.map {
                Kontaktinfo(
                    type = KontaktinfoType.parse(it.typeTelecom?.v),
                    value = it.teleAddress.v,
                )
            },
        )

        val ediEmottak = toEdiEmottak(mottakenhetBlokk, msgHead, receivedSykmelding)
        val sykmelding = XmlSykmelding(
            id = receivedSykmelding.sykmelding.id,
            metadata = SykmeldingMetadata(
                mottattDato = receivedSykmelding.mottattDato.atOffset(UTC),
                genDate = ediEmottak.msgInfo.genDate,
                behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt.atOffset(UTC),
                regelsettVersjon = xmlSykmelding.regelSettVersjon,
                avsenderSystem = AvsenderSystem(
                    navn = receivedSykmelding.sykmelding.avsenderSystem.navn,
                    versjon = receivedSykmelding.sykmelding.avsenderSystem.versjon,
                ),
                strekkode = xmlSykmelding.strekkode,
            ),
            pasient = sykmeldingPasient,
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            medisinskVurdering = mapMedisinskVurdering(receivedSykmelding.sykmelding),
            prognose = mapPrognose(receivedSykmelding.sykmelding),
            tiltak = toTiltak(receivedSykmelding),
            bistandNav = toBistandNav(receivedSykmelding),
            tilbakedatering = toTilbakedatering(receivedSykmelding),
            aktivitet = receivedSykmelding.sykmelding.perioder.map { periode ->
                mapAktivitet(periode)
            },
            utdypendeOpplysninger = toUtdypendeOpplysninger(receivedSykmelding),
            behandler = behandler,
            sykmelder = sykmelder
        )

        return SykmeldingRecord(
            metadata = ediEmottak,
            sykmelding = sykmelding,
            validation = mapValidationResult(receivedSykmelding),
        )
    }

    private fun toOffsetDateTime(genDate: String): OffsetDateTime {
        return try {
            OffsetDateTime.parse(genDate).withOffsetSameInstant(UTC)
        } catch (ex : DateTimeParseException) {
            LocalDateTime.parse(genDate).atZone(ZoneId.of("Europe/Oslo")).toOffsetDateTime().withOffsetSameInstant(UTC);
        }
    }

    private fun toOffsetDateTime(date: GregorianCalendar): OffsetDateTime {
       return date.toZonedDateTime().toOffsetDateTime().withOffsetSameInstant(UTC)
    }


    private fun toSykmeldingPasient(pasient: HelseOpplysningerArbeidsuforhet.Pasient): no.nav.tsm.reformat.sykmelding.model.Pasient {

        return no.nav.tsm.reformat.sykmelding.model.Pasient(
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
                    type = KontaktinfoType.parse(it.typeTelecom?.v),
                    value = it.teleAddress.v,
                )
            },
        )
    }

    private fun toEdiEmottak(
        mottakenhetBlokk: XMLMottakenhetBlokk,
        msgHead: XMLMsgHead,
        receivedSykmelding: ReceivedSykmelding,
    ) = EDIEmottak(
        mottakenhetBlokk = MottakenhetBlokk(
            ediLogid = mottakenhetBlokk.ediLoggId,
            avsender = mottakenhetBlokk.avsender,
            ebXMLSamtaleId = mottakenhetBlokk.ebXMLSamtaleId,
            mottaksId = mottakenhetBlokk.mottaksId,
            meldingsType = mottakenhetBlokk.meldingsType,
            avsenderRef = mottakenhetBlokk.avsenderRef,
            avsenderFnrFraDigSignatur = mottakenhetBlokk.avsenderFnrFraDigSignatur,
            mottattDato = toOffsetDateTime(mottakenhetBlokk.mottattDatotid.toGregorianCalendar()),
            orgnummer = mottakenhetBlokk.orgNummer,
            avsenderOrgNrFraDigSignatur = mottakenhetBlokk.avsenderOrgNrFraDigSignatur,
            partnerReferanse = mottakenhetBlokk.partnerReferanse,
            herIdentifikator = mottakenhetBlokk.herIdentifikator,
            ebRole = mottakenhetBlokk.ebRole,
            ebService = mottakenhetBlokk.ebService,
            ebAction = mottakenhetBlokk.ebAction,
        ),
        msgInfo = MessageInfo(
            type = Meldingstype.parse(msgHead.msgInfo.type.v),
            genDate = toOffsetDateTime(msgHead.msgInfo.genDate),
            msgId = receivedSykmelding.msgId ?: throw IllegalArgumentException("Mangler msgId"),
            migVersjon = msgHead.msgInfo.msgId,
        ),
        sender = toSender(msgHead.msgInfo.sender),
        receiver = toReceiver(msgHead.msgInfo.receiver),
        pasient = toPasient(msgHead.msgInfo.patient),
        vedlegg = receivedSykmelding.vedlegg,
        ack = Ack(AckType.parse(msgHead.msgInfo.ack?.v))
    )

    private fun toPasient(xmlPasient: XMLPatient?): no.nav.tsm.reformat.sykmelding.model.metadata.Pasient? {
        if (xmlPasient == null) return null

        return no.nav.tsm.reformat.sykmelding.model.metadata.Pasient(
            ids = xmlPasient.ident.map {
                PersonId(
                    id = it.id,
                    type = PersonIdType.parse(it.typeId.v),
                )
            },
            navn = Navn(
                fornavn = xmlPasient.givenName,
                mellomnavn = xmlPasient.middleName,
                etternavn = xmlPasient.familyName,
            ),
            fodselsdato = xmlPasient.dateOfBirth,
            kjonn = Kjonn.parse(xmlPasient.sex?.v),
            nasjonalitet = xmlPasient.nationality?.v,
            adresse = toAdresse(xmlPasient.address),
            kontaktinfo = xmlPasient.teleCom.map {
                Kontaktinfo(
                    type = KontaktinfoType.parse(it.typeTelecom?.v),
                    value = it.teleAddress.v,
                )
            },
        )
    }


    private fun emottakEnkel(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        val emottakEnkel = EmottakEnkel(
            msgInfo = MessageInfo(
                type = Meldingstype.SYKMELDING,
                genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(UTC),
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
                ),
                adresse = null,
                kontaktinfo = null,
                underOrganisasjon = null,
                helsepersonell = null,
            ),
            sender = Organisasjon(
                navn = receivedSykmelding.legekontorOrgName,
                type = OrganisasjonsType.IKKE_OPPGITT,
                ids = listOfNotNull(
                    when {
                        receivedSykmelding.legekontorHerId.isNullOrBlank() -> null
                        else -> OrgId(
                            id = receivedSykmelding.legekontorHerId,
                            type = OrgIdType.HER,
                        )
                    },
                    when {
                        receivedSykmelding.legekontorOrgNr.isNullOrBlank() -> null
                        else -> OrgId(
                            id = receivedSykmelding.legekontorOrgNr,
                            type = OrgIdType.ENH,
                        )
                    },
                    when {
                        receivedSykmelding.legekontorReshId.isNullOrBlank() -> null
                        else -> OrgId(
                            id = receivedSykmelding.legekontorReshId,
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
        val sykmelding = toSykmelding(receivedSykmelding)
        return SykmeldingRecord(
            metadata = emottakEnkel,
            sykmelding = sykmelding,
            validation = validation,
        )
    }
    private fun toPapirSykmelding(
        receivedSykmelding: ReceivedSykmelding,
    ): Sykmelding {
        return Papirsykmelding(
            id = receivedSykmelding.sykmelding.id,
            metadata = SykmeldingMetadata(
                mottattDato = receivedSykmelding.mottattDato.atOffset(UTC),
                genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(UTC),
                behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt.atOffset(UTC),
                regelsettVersjon = receivedSykmelding.rulesetVersion,
                strekkode = null,
                avsenderSystem = AvsenderSystem(
                    navn = receivedSykmelding.sykmelding.avsenderSystem.navn,
                    versjon = receivedSykmelding.sykmelding.avsenderSystem.versjon
                ),
            ),
            pasient = toPasient(receivedSykmelding),
            behandler = toBehandler(receivedSykmelding),
            sykmelder = toSignerendeBehandler(receivedSykmelding),
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            medisinskVurdering = mapMedisinskVurdering(receivedSykmelding.sykmelding),
            prognose = mapPrognose(receivedSykmelding.sykmelding),
            tiltak = toTiltak(receivedSykmelding),
            bistandNav = toBistandNav(receivedSykmelding),
            tilbakedatering = toTilbakedatering(receivedSykmelding),
            aktivitet = receivedSykmelding.sykmelding.perioder.map {
                mapAktivitet(it)
            },
            utdypendeOpplysninger = toUtdypendeOpplysninger(receivedSykmelding),
        )
    }

    private fun toSykmelding(
        receivedSykmelding: ReceivedSykmelding,
    ): Sykmelding {
        return XmlSykmelding(
            id = receivedSykmelding.sykmelding.id,
            metadata = SykmeldingMetadata(
                mottattDato = receivedSykmelding.mottattDato.atOffset(UTC),
                genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(UTC),
                behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt.atOffset(UTC),
                regelsettVersjon = receivedSykmelding.rulesetVersion,
                strekkode = null,
                avsenderSystem = AvsenderSystem(
                    navn = receivedSykmelding.sykmelding.avsenderSystem.navn,
                    versjon = receivedSykmelding.sykmelding.avsenderSystem.versjon
                ),
            ),
            pasient = toPasient(receivedSykmelding),
            behandler = toBehandler(receivedSykmelding),
            sykmelder = toSignerendeBehandler(receivedSykmelding),
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            medisinskVurdering = mapMedisinskVurdering(receivedSykmelding.sykmelding),
            prognose = mapPrognose(receivedSykmelding.sykmelding),
            tiltak = toTiltak(receivedSykmelding),
            bistandNav = toBistandNav(receivedSykmelding),
            tilbakedatering = toTilbakedatering(receivedSykmelding),
            aktivitet = receivedSykmelding.sykmelding.perioder.map {
                mapAktivitet(it)
            },
            utdypendeOpplysninger = toUtdypendeOpplysninger(receivedSykmelding),
        )
    }


    private fun toTilbakedatering(receivedSykmelding: ReceivedSykmelding): Tilbakedatering? {
        val kontaktMedPasient = receivedSykmelding.sykmelding.kontaktMedPasient
        if (kontaktMedPasient.kontaktDato == null && kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrBlank()) return null

        return Tilbakedatering(
            kontaktDato = kontaktMedPasient.kontaktDato,
            begrunnelse = kontaktMedPasient.begrunnelseIkkeKontakt,
        )
    }


    private fun toBistandNav(receivedSykmelding: ReceivedSykmelding): BistandNav? {

        val meldingTilNav = receivedSykmelding.sykmelding.meldingTilNAV ?: return null
        if (meldingTilNav.bistandUmiddelbart || !meldingTilNav.beskrivBistand.isNullOrBlank()) {
            return BistandNav(
                bistandUmiddelbart = meldingTilNav.bistandUmiddelbart,
                beskrivBistand = meldingTilNav.beskrivBistand,
            )
        }
        return null
    }

    private fun toTiltak(receivedSykmelding: ReceivedSykmelding): Tiltak? {
        val tiltakNav = receivedSykmelding.sykmelding.tiltakNAV
        val andreTiltak = receivedSykmelding.sykmelding.andreTiltak
        if(!tiltakNav.isNullOrBlank() || !andreTiltak.isNullOrBlank()) {
            Tiltak(
                tiltakNav = tiltakNav,
                andreTiltak = andreTiltak,
            )
        }
        return null
    }

    private fun toSignerendeBehandler(receivedSykmelding: ReceivedSykmelding): Sykmelder {
        return Sykmelder(
            ids = listOfNotNull(
                receivedSykmelding.legeHprNr?.let {
                    PersonId(
                        id = it,
                        type = PersonIdType.HPR,
                    )
                },
                PersonId(
                    id = receivedSykmelding.personNrLege,
                    type = PersonIdType.FNR,
                ),
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
                            SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER -> no.nav.tsm.reformat.sykmelding.SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER
                            SvarRestriksjon.SKJERMET_FOR_PASIENT -> no.nav.tsm.reformat.sykmelding.SvarRestriksjon.SKJERMET_FOR_PASIENT
                            SvarRestriksjon.SKJERMET_FOR_NAV -> no.nav.tsm.reformat.sykmelding.SvarRestriksjon.SKJERMET_FOR_NAV
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
                postnummer = behandler.adresse.postnummer?.toString()?.padStart(4, '0'),
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

    private fun toPasient(receivedSykmelding: ReceivedSykmelding): no.nav.tsm.reformat.sykmelding.model.Pasient {
        val ident = receivedSykmelding.personNrPasient

        return no.nav.tsm.reformat.sykmelding.model.Pasient(
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
        return when (receivedSykmelding.validationResult.status) {
            Status.OK -> mapOkOrPendingValidation(receivedSykmelding)
            Status.INVALID -> mapInvalidValidation(receivedSykmelding)
            Status.MANUAL_PROCESSING -> ValidationResult(
                status = RuleType.OK,
                timestamp = receivedSykmelding.validationResult.timestamp ?: receivedSykmelding.mottattDato.atOffset(UTC),
                rules = emptyList()
            )
        }
    }

    private fun mapInvalidValidation(receivedSykmelding: ReceivedSykmelding): ValidationResult {
        return ValidationResult(
            status = RuleType.INVALID,
            timestamp = receivedSykmelding.mottattDato.atOffset(UTC),
            rules = receivedSykmelding.validationResult.ruleHits.mapNotNull {
                val rule = it
                when (rule.ruleStatus) {
                    Status.INVALID -> InvalidRule(
                        name = rule.ruleName,
                        timestamp = receivedSykmelding.validationResult.timestamp ?: receivedSykmelding.mottattDato.atOffset(UTC),
                        validationType = ValidationType.AUTOMATIC,
                        reason = Reason(
                            sykmeldt = rule.messageForUser,
                            sykmelder = rule.messageForSender
                        )
                    )

                    else -> null
                }
            }
        )
    }

    private fun mapOkOrPendingValidation(receivedSykmelding: ReceivedSykmelding): ValidationResult {
        val validationResultTimestamp = receivedSykmelding.validationResult.timestamp ?: receivedSykmelding.mottattDato.atOffset(UTC)
        if (receivedSykmelding.merknader.isNullOrEmpty()) {
            return ValidationResult(
                status = RuleType.OK,
                timestamp = validationResultTimestamp,
                rules = emptyList()
            )
        }
        val rules: List<Rule> = mapTilbakedatertRules(receivedSykmelding.merknader, validationResultTimestamp)
        return ValidationResult(
            status = rules.maxOf { it.type },
            timestamp = validationResultTimestamp,
            rules = rules
        )
    }

    private fun mapTilbakedatertRules(
        merknader: List<Merknad>,
        timestamp: OffsetDateTime,
    ) = merknader.map {
        when (OldTilbakedatertMerknad.valueOf(it.type)) {
            OldTilbakedatertMerknad.UNDER_BEHANDLING -> PendingRule(
                name = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
                timestamp = timestamp,
                ValidationType.AUTOMATIC,
                reason = Reason(
                    sykmelder = "Sykmeldingen er til manuell behandling",
                    sykmeldt = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert"
                )
            )
            OldTilbakedatertMerknad.UGYLDIG_TILBAKEDATERING -> InvalidRule(
                name = TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING.name,
                timestamp = timestamp,
                validationType = ValidationType.MANUAL,
                reason = Reason(
                    sykmeldt = "Sykmeldingen er tilbakedatert uten tilstrekkelig begrunnelse fra den som sykmeldte deg.",
                    "Ugyldig tilbakedatering")
            )
            OldTilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER -> PendingRule(
                name = TilbakedatertMerknad.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER.name,
                timestamp = timestamp,
                validationType = ValidationType.MANUAL,
                reason = Reason(sykmeldt = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
                    "Tilbakedatering krever flere opplysninger")
            )
            OldTilbakedatertMerknad.DELVIS_GODKJENT -> OKRule(
                name = TilbakedatertMerknad.TILBAKEDATERING_DELVIS_GODKJENT.name,
                timestamp = timestamp,
                validationType = ValidationType.MANUAL,
            )
            OldTilbakedatertMerknad.TILBAKEDATERT_PAPIRSYKMELDING -> OKRule(
                name = TilbakedatertMerknad.TILBAKEDATERING_TILBAKEDATERT_PAPIRSYKMELDING.name,
                timestamp = timestamp,
                validationType = ValidationType.AUTOMATIC,
            )
        }
    }
}


private fun mapMedisinskVurdering(sykmelding: no.nav.tsm.smregister.models.Sykmelding): MedisinskVurdering {
    return MedisinskVurdering(
        hovedDiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.let(toDiagnoseInfo()),
        biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser.map(toDiagnoseInfo()),
        annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak?.let {
            if(it.beskrivelse == null && it.grunn.isEmpty()) {
                null
            }
            else {
                AnnenFraverArsak(
                    it.beskrivelse, it.grunn.map { grunn ->
                        when (grunn) {
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
                    }
                )
            }
        },
        svangerskap = sykmelding.medisinskVurdering.svangerskap,
        yrkesskade = when(sykmelding.medisinskVurdering.yrkesskade) {
            true -> Yrkesskade(sykmelding.medisinskVurdering.yrkesskadeDato)
            false -> null
        },
        skjermetForPasient = sykmelding.skjermesForPasient,
        syketilfelletStartDato = sykmelding.syketilfelleStartDato,
        )
}

private fun toDiagnoseInfo() = { diagnose: Diagnose ->
    DiagnoseInfo(
        kode = diagnose.kode,
        tekst = diagnose.tekst,
        system = when (diagnose.system) {
            "2.16.578.1.12.4.1.1.7170" -> DiagnoseSystem.ICPC2
            "2.16.578.1.12.4.1.1.7110" -> DiagnoseSystem.ICD10
            "2.16.578.1.12.4.1.1.7171" -> DiagnoseSystem.ICPC2B
            "2.16.578.1.12.4.1.1.7112" -> DiagnoseSystem.PHBU
            "" -> DiagnoseSystem.UGYLDIG
            else -> throw IllegalArgumentException("Ukjent diagnose system ${diagnose.system}")
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
        return AktivitetIkkeMulig(
            medisinskArsak = periode.aktivitetIkkeMulig.medisinskArsak?.let {
                MedisinskArsak(
                    it.beskrivelse,
                    it.arsak.map { arsak -> toMedisinskArsakType(arsak) }
                )
            },
            arbeidsrelatertArsak = periode.aktivitetIkkeMulig.arbeidsrelatertArsak?.let {
                ArbeidsrelatertArsak(
                    it.beskrivelse,
                    it.arsak.map { arsak -> toArbeidsrelatertArsakType(arsak) }
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

fun toArbeidsrelatertArsakType(arsak: no.nav.tsm.smregister.models.ArbeidsrelatertArsakType) : ArbeidsrelatertArsakType {
    return when (arsak) {
        no.nav.tsm.smregister.models.ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING -> ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING
        no.nav.tsm.smregister.models.ArbeidsrelatertArsakType.ANNET -> ArbeidsrelatertArsakType.ANNET
    }
}

fun toMedisinskArsakType(arsak: no.nav.tsm.smregister.models.MedisinskArsakType) : MedisinskArsakType {
    return when (arsak) {
        no.nav.tsm.smregister.models.MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET -> MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET
        no.nav.tsm.smregister.models.MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND -> MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND
        no.nav.tsm.smregister.models.MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING -> MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING
        no.nav.tsm.smregister.models.MedisinskArsakType.ANNET -> MedisinskArsakType.ANNET
    }
}

private fun mapArbeidsgiver(sykmelding: no.nav.tsm.smregister.models.Sykmelding): ArbeidsgiverInfo {
    return when (sykmelding.arbeidsgiver.harArbeidsgiver) {
        HarArbeidsgiver.EN_ARBEIDSGIVER -> EnArbeidsgiver(
            meldingTilArbeidsgiver = sykmelding.meldingTilArbeidsgiver,
            tiltakArbeidsplassen = sykmelding.tiltakArbeidsplassen,
            navn = sykmelding.arbeidsgiver.navn,
            yrkesbetegnelse = sykmelding.arbeidsgiver.yrkesbetegnelse,
            stillingsprosent = sykmelding.arbeidsgiver.stillingsprosent
        )

        HarArbeidsgiver.FLERE_ARBEIDSGIVERE -> FlereArbeidsgivere(
            meldingTilArbeidsgiver = sykmelding.meldingTilArbeidsgiver,
            tiltakArbeidsplassen = sykmelding.tiltakArbeidsplassen,
            navn = sykmelding.arbeidsgiver.navn,
            yrkesbetegnelse = sykmelding.arbeidsgiver.yrkesbetegnelse,
            stillingsprosent = sykmelding.arbeidsgiver.stillingsprosent,
        )

        HarArbeidsgiver.INGEN_ARBEIDSGIVER -> IngenArbeidsgiver()
    }
}

