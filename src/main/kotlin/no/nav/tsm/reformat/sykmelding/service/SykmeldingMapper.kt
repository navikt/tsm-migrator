package no.nav.tsm.reformat.sykmelding.service

import no.nav.helse.eiFellesformat.XMLMottakenhetBlokk
import no.nav.helse.msgHead.*
import no.nav.helse.sm2013.Address
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.tsm.digital.spmUke7Mapping
import no.nav.tsm.digital.uke7Prefix
import no.nav.tsm.reformat.sykmelding.model.metadata.*
import no.nav.tsm.reformat.sykmelding.util.XmlStuff
import no.nav.tsm.reformat.sykmelding.util.get
import no.nav.tsm.reformat.sykmelding.util.getIdentType
import no.nav.tsm.smregister.models.*
import no.nav.tsm.smregister.models.SvarRestriksjon
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsak
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.ErIArbeid
import no.nav.tsm.sykmelding.input.core.model.ErIkkeIArbeid
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsak
import no.nav.tsm.sykmelding.input.core.model.Prognose
import no.nav.tsm.sykmelding.input.core.model.SporsmalSvar
import no.nav.tsm.sykmelding.input.core.model.UtenlandskInfo
import no.nav.tsm.sykmelding.input.core.model.metadata.*
import no.nav.tsm.sykmelding.input.core.model.metadata.Adresse
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeParseException
import java.util.*

typealias SykmeldingPasient = no.nav.tsm.sykmelding.input.core.model.Pasient
typealias MetadataPasient = no.nav.tsm.sykmelding.input.core.model.metadata.Pasient

enum class OldTilbakedatertMerknad {
    UNDER_BEHANDLING,
    UGYLDIG_TILBAKEDATERING,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    DELVIS_GODKJENT,
    TILBAKEDATERT_PAPIRSYKMELDING,
}


class MappingException(val receivedSykmelding: ReceivedSykmelding, val exception: Exception) : Exception() {
    override val message: String?
        get() = exception.message
}
class SykmeldingMapper {
    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingMapper::class.java)
    }
    private val xmlStuff = XmlStuff()

    fun toNewSykmelding(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        try {
            return when {
                receivedSykmelding.sykmelding.avsenderSystem.navn == "syk-inn" -> toDigitalSykmelding(receivedSykmelding)
                receivedSykmelding.sykmelding.avsenderSystem.navn.contains("FHIR") -> toDigitalSykmelding(receivedSykmelding)
                receivedSykmelding.utenlandskSykmelding != null -> toUtenlandssykmeldingMedBehandlingsutfall(receivedSykmelding)
                receivedSykmelding.sykmelding.avsenderSystem.navn == "Papirsykmelding" -> toPapirsykmelding(
                    receivedSykmelding
                )
                receivedSykmelding.sykmelding.avsenderSystem.navn == "Egenmeldt" -> toEgenmeldtSykmelding(receivedSykmelding)
                receivedSykmelding.sykmelding.avsenderSystem.navn.lowercase().contains("dolly") -> emottakEnkel(receivedSykmelding)
                !receivedSykmelding.fellesformat.isNullOrBlank() -> fromReceivedSykmeldignAndFellesformat(receivedSykmelding)
                else -> emottakEnkel(receivedSykmelding)
            }
        } catch (e: Exception) {

            throw MappingException(receivedSykmelding, e)
        }
    }

    private fun toDigitalSykmelding(receivedSykmelding: ReceivedSykmelding): SykmeldingRecord {
        requireNotNull(receivedSykmelding.fellesformat)
        val unmashalledSykmelding = xmlStuff.unmarshal(receivedSykmelding.fellesformat)
        val msgHead = unmashalledSykmelding.get<XMLMsgHead>()
        val xmlSykmelding = msgHead.document.single().refDoc.content.any.single()
                as HelseOpplysningerArbeidsuforhet
        val pasientNavn = Navn(
            fornavn = xmlSykmelding.pasient.navn.fornavn,
            mellomnavn = xmlSykmelding.pasient.navn.mellomnavn,
            etternavn = xmlSykmelding.pasient.navn.etternavn)

        val digitalSykmelding = DigitalSykmelding(
            id = receivedSykmelding.sykmelding.id,
            metadata = DigitalSykmeldingMetadata(
                mottattDato = receivedSykmelding.mottattDato.atOffset(ZoneOffset.UTC),
                genDate = receivedSykmelding.sykmelding.signaturDato.atOffset(ZoneOffset.UTC),
                avsenderSystem = AvsenderSystem(receivedSykmelding.sykmelding.avsenderSystem.navn, receivedSykmelding.sykmelding.avsenderSystem.versjon),
                ),
            pasient = toPasient(receivedSykmelding, pasientNavn),
            medisinskVurdering = mapDigitalMedisinskVurdering(receivedSykmelding.sykmelding),
            aktivitet = receivedSykmelding.sykmelding.perioder.map { mapAktivitet(it) },
            behandler = toBehandler(receivedSykmelding),
            sykmelder = toSignerendeBehandler(receivedSykmelding),
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            tilbakedatering = toTilbakedatering(receivedSykmelding),
            bistandNav = toBistandNav(receivedSykmelding),
            utdypendeSporsmal = toDigitalUtdypendeSporsmal(receivedSykmelding.sykmelding.id, receivedSykmelding.sykmelding.utdypendeOpplysninger)
        )
        val digital = Digital(
            receivedSykmelding.legekontorOrgNr ?: throw IllegalArgumentException("missing legekontorOrgNr"),
        )
        val validation = mapValidationResult(receivedSykmelding)
        return SykmeldingRecord(
            validation = validation,
            metadata = digital,
            sykmelding = digitalSykmelding,
        )
    }

    fun toDigitalUtdypendeSporsmal(sykmeldingId: String, utdypendeOpplysninger: Map<String, Map<String, no.nav.tsm.smregister.models.SporsmalSvar>>): List<UtdypendeSporsmal>? {
        val uke7spm = utdypendeOpplysninger[uke7Prefix] ?: return null

        return uke7spm.mapNotNull { (key, utdypendeSpm) ->
            val spm = spmUke7Mapping.entries.singleOrNull { it.value.first == key }
            if(spm == null) {
                log.warn("Could not find uke7 spm for sykmelding: $sykmeldingId, for key: $key. Skipping.")
                null
            } else {
                UtdypendeSporsmal(
                    svar = utdypendeSpm.svar,
                    type = spm.key,
                )
            }
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
            pasient = toPasient(receivedSykmelding, null),
            medisinskVurdering = mapLegacyMedisinskVurdering(receivedSykmelding.sykmelding),
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
        type = organisation.typeOrganisation?.let { parseOrganisasjonsType(it.v) }
            ?: OrganisasjonsType.IKKE_OPPGITT,
        ids = organisation.ident.map {
            OrgId(
                id = it.id,
                type = parseOrgIdType(it.typeId.v),
            )
        },
        adresse = toAdresse(organisation.address),
        kontaktinfo = organisation.teleCom.map {
            Kontaktinfo(
                type = parseKontaktinfoType(it.typeTelecom?.v),
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
                    type = parsePersonIdType(it.typeId.v),
                )
            },
            navn = name,
            adresse = toAdresse(healthcareProfessional.address),
            kontaktinfo = healthcareProfessional.teleCom.map {
                Kontaktinfo(
                    type = parseKontaktinfoType(it.typeTelecom?.v),
                    value = it.teleAddress.v,
                )
            },
            kjonn = healthcareProfessional.sex?.let { parseKjonn(it.v) },
            nasjonalitet = healthcareProfessional.nationality?.v,
            fodselsdato = healthcareProfessional.dateOfBirth,
            helsepersonellKategori = parseHelsepersonellKategori(healthcareProfessional.typeHealthcareProfessional?.v),
            rolleTilPasient = parseRolleTilPasient(healthcareProfessional.roleToPatient?.v),
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
            type = parseAdresseType(address.type?.v),
        )
    }

    private fun toAdresse(address: Address) = Adresse(
        gateadresse = address.streetAdr,
        postnummer = address.postalCode,
        poststed = address.city,
        postboks = address.postbox,
        kommune = address.county?.v,
        land = address.country?.v,
        type = parseAdresseType(address.type?.v),
    )

    private fun tilUnderorganisasjon(organisation: XMLOrganisation?): UnderOrganisasjon? {
        if (organisation == null) return null
        return UnderOrganisasjon(
            navn = organisation.organisationName,
            type = organisation.typeOrganisation?.let { parseOrganisasjonsType(it.v) }
                ?: OrganisasjonsType.IKKE_OPPGITT,
            kontaktinfo = organisation.teleCom.map {
                Kontaktinfo(
                    type = parseKontaktinfoType(it.typeTelecom?.v),
                    value = it.teleAddress.v,
                )
            },
            ids = organisation.ident.map {
                OrgId(
                    id = it.id,
                    type = parseOrgIdType(it.typeId.v),
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

        val helseOpplysningerArbeidsuforhet = msgHead.document.flatMap { it.refDoc.content.any }.filter { it is HelseOpplysningerArbeidsuforhet }.filterIsInstance<HelseOpplysningerArbeidsuforhet>()
        if(helseOpplysningerArbeidsuforhet.size > 1) {
            throw IllegalStateException("Forventet kun en HelseOpplysningerArbeidsuforhet for ${receivedSykmelding.sykmelding.id}")
        }
        val xmlSykmelding = helseOpplysningerArbeidsuforhet.single()
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
                    type = parsePersonIdType(it.typeId.v),
                )
            },
            kontaktinfo = xmlSykmelding.behandler.kontaktInfo.filter {
                it.teleAddress.v != null
            }.map {
                Kontaktinfo(
                    type = parseKontaktinfoType(it.typeTelecom?.v),
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
            medisinskVurdering = mapLegacyMedisinskVurdering(receivedSykmelding.sykmelding),
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
        } catch (_ : DateTimeParseException) {
            LocalDateTime.parse(genDate).atZone(ZoneId.of("Europe/Oslo")).toOffsetDateTime().withOffsetSameInstant(UTC)
        }
    }

    private fun toOffsetDateTime(date: GregorianCalendar): OffsetDateTime {
       return date.toZonedDateTime().toOffsetDateTime().withOffsetSameInstant(UTC)
    }


    private fun toSykmeldingPasient(pasient: HelseOpplysningerArbeidsuforhet.Pasient): SykmeldingPasient {

        return SykmeldingPasient(
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
                    type = parseKontaktinfoType(it.typeTelecom?.v),
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
        ack = Ack(parseAckType(msgHead.msgInfo.ack?.v))
    )

    private fun toPasient(xmlPasient: XMLPatient?): MetadataPasient? {
        if (xmlPasient == null) return null

        return MetadataPasient(
            ids = xmlPasient.ident.map {
                PersonId(
                    id = it.id,
                    type = parsePersonIdType(it.typeId.v),
                )
            },
            navn = Navn(
                fornavn = xmlPasient.givenName,
                mellomnavn = xmlPasient.middleName,
                etternavn = xmlPasient.familyName,
            ),
            fodselsdato = xmlPasient.dateOfBirth,
            kjonn = parseKjonn(xmlPasient.sex?.v),
            nasjonalitet = xmlPasient.nationality?.v,
            adresse = toAdresse(xmlPasient.address),
            kontaktinfo = xmlPasient.teleCom.map {
                Kontaktinfo(
                    type = parseKontaktinfoType(it.typeTelecom?.v),
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
            pasient = toPasient(receivedSykmelding, null),
            behandler = toBehandler(receivedSykmelding),
            sykmelder = toSignerendeBehandler(receivedSykmelding),
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            medisinskVurdering = mapLegacyMedisinskVurdering(receivedSykmelding.sykmelding),
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
            pasient = toPasient(receivedSykmelding, null),
            behandler = toBehandler(receivedSykmelding),
            sykmelder = toSignerendeBehandler(receivedSykmelding),
            arbeidsgiver = mapArbeidsgiver(receivedSykmelding.sykmelding),
            medisinskVurdering = mapLegacyMedisinskVurdering(receivedSykmelding.sykmelding),
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
            return Tiltak(
                tiltakNav = tiltakNav,
                andreTiltak = andreTiltak,
            )
        }
        return null
    }

    private fun toSignerendeBehandler(receivedSykmelding: ReceivedSykmelding): Sykmelder {
        return Sykmelder(
            ids = listOfNotNull(
                PersonId(
                    id = receivedSykmelding.personNrLege,
                    type = PersonIdType.FNR,
                ),
                receivedSykmelding.legeHprNr?.let {
                    PersonId(
                        id = it,
                        type = PersonIdType.HPR,
                    )
                },

            ),
            helsepersonellKategori = parseHelsepersonellKategori(receivedSykmelding.legeHelsepersonellkategori),
        )
    }


    private fun toUtdypendeOpplysninger(receivedSykmelding: ReceivedSykmelding) =
        receivedSykmelding.sykmelding.utdypendeOpplysninger.mapValues { entry ->
            entry.value.mapValues { questions ->
                SporsmalSvar(
                    sporsmal = questions.value.sporsmal ?: questions.key,
                    svar = questions.value.svar,
                    restriksjoner = questions.value.restriksjoner.map {
                        when (it) {
                            SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER -> no.nav.tsm.sykmelding.input.core.model.SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER
                            SvarRestriksjon.SKJERMET_FOR_PASIENT -> no.nav.tsm.sykmelding.input.core.model.SvarRestriksjon.SKJERMET_FOR_PASIENT
                            SvarRestriksjon.SKJERMET_FOR_NAV -> no.nav.tsm.sykmelding.input.core.model.SvarRestriksjon.SKJERMET_FOR_NAV
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

    private fun toPasient(receivedSykmelding: ReceivedSykmelding, navn: Navn?): SykmeldingPasient {
        val ident = receivedSykmelding.personNrPasient

        return SykmeldingPasient(
            fnr = ident,
            navn = navn,
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
                when (it.ruleStatus) {
                    Status.INVALID -> InvalidRule(
                        name = it.ruleName,
                        timestamp = receivedSykmelding.validationResult.timestamp
                            ?: receivedSykmelding.mottattDato.atOffset(UTC),
                        validationType = ValidationType.AUTOMATIC,
                        reason = Reason(
                            sykmeldt = it.messageForUser,
                            sykmelder = it.messageForSender
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


private fun AnnenFraverGrunn?.toAnnenFravarsgrunn() : AnnenFravarsgrunn? {
    if (this == null) return null
    return when (this) {
        AnnenFraverGrunn.GODKJENT_HELSEINSTITUSJON -> AnnenFravarsgrunn.GODKJENT_HELSEINSTITUSJON
        AnnenFraverGrunn.ARBEIDSRETTET_TILTAK -> AnnenFravarsgrunn.ARBEIDSRETTET_TILTAK
        AnnenFraverGrunn.BEHANDLING_FORHINDRER_ARBEID -> throw IllegalArgumentException("AnnenFraverGrunn.BEHANDLING_FORHINDRER_ARBEID is not suuported in digital sykmelding yet")
        AnnenFraverGrunn.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND -> AnnenFravarsgrunn.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
        AnnenFraverGrunn.NODVENDIG_KONTROLLUNDENRSOKELSE -> AnnenFravarsgrunn.NODVENDIG_KONTROLLUNDENRSOKELSE
        AnnenFraverGrunn.SMITTEFARE -> AnnenFravarsgrunn.SMITTEFARE
        AnnenFraverGrunn.ABORT -> AnnenFravarsgrunn.ABORT
        AnnenFraverGrunn.UFOR_GRUNNET_BARNLOSHET -> AnnenFravarsgrunn.UFOR_GRUNNET_BARNLOSHET
        AnnenFraverGrunn.DONOR -> AnnenFravarsgrunn.DONOR
        AnnenFraverGrunn.BEHANDLING_STERILISERING -> AnnenFravarsgrunn.BEHANDLING_STERILISERING
    }
}

private fun mapDigitalMedisinskVurdering(sykmelding: no.nav.tsm.smregister.models.SykmeldingLegacy): DigitalMedisinskVurdering {
    return DigitalMedisinskVurdering(
        hovedDiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.let(toDiagnoseInfo()),
        biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser.map(toDiagnoseInfo()),
        annenFravarsgrunn = sykmelding.medisinskVurdering.annenFraversArsak?.grunn?.firstOrNull().toAnnenFravarsgrunn(),
        svangerskap = sykmelding.medisinskVurdering.svangerskap,
        yrkesskade = when(sykmelding.medisinskVurdering.yrkesskade) {
            true -> Yrkesskade(sykmelding.medisinskVurdering.yrkesskadeDato)
            false -> null
        },
        skjermetForPasient = sykmelding.skjermesForPasient,
        )
}

private fun mapLegacyMedisinskVurdering(sykmelding: no.nav.tsm.smregister.models.SykmeldingLegacy): LegacyMedisinskVurdering {
    return LegacyMedisinskVurdering(
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

private fun mapPrognose(sykmelding: no.nav.tsm.smregister.models.SykmeldingLegacy): Prognose? {
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

fun toArbeidsrelatertArsakType(arsak: no.nav.tsm.smregister.models.ArbeidsrelatertArsakTypeLegacy) : ArbeidsrelatertArsakType {
    return when (arsak) {
        no.nav.tsm.smregister.models.ArbeidsrelatertArsakTypeLegacy.MANGLENDE_TILRETTELEGGING -> ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING
        no.nav.tsm.smregister.models.ArbeidsrelatertArsakTypeLegacy.ANNET -> ArbeidsrelatertArsakType.ANNET
    }
}

fun toMedisinskArsakType(arsak: no.nav.tsm.smregister.models.MedisinskArsakTypeLegacy) : MedisinskArsakType {
    return when (arsak) {
        no.nav.tsm.smregister.models.MedisinskArsakTypeLegacy.TILSTAND_HINDRER_AKTIVITET -> MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET
        no.nav.tsm.smregister.models.MedisinskArsakTypeLegacy.AKTIVITET_FORVERRER_TILSTAND -> MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND
        no.nav.tsm.smregister.models.MedisinskArsakTypeLegacy.AKTIVITET_FORHINDRER_BEDRING -> MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING
        no.nav.tsm.smregister.models.MedisinskArsakTypeLegacy.ANNET -> MedisinskArsakType.ANNET
    }
}

private fun mapArbeidsgiver(sykmelding: no.nav.tsm.smregister.models.SykmeldingLegacy): ArbeidsgiverInfo {
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

