package no.nav.tsm.avro.model

import no.nav.tsm.smregister.models.Periode
import no.nav.tsm.smregister.models.ReceivedSykmelding
import java.time.ZoneOffset

fun toAvroModel(receivedSykmelding: ReceivedSykmelding) : no.nav.tsm.avro.model.ReceivedSykmelding {
    return no.nav.tsm.avro.model.ReceivedSykmelding(
        sykmelding = toAvroSykmelding(receivedSykmelding.sykmelding),
        personNrPasient = receivedSykmelding.personNrPasient,
        tlfPasient = receivedSykmelding.tlfPasient,
        personNrLege = receivedSykmelding.personNrLege,
        legeHelsepersonellkategori = receivedSykmelding.legeHelsepersonellkategori,
        legeHprNr = receivedSykmelding.legeHprNr,
        navLogId = receivedSykmelding.navLogId,
        msgId = receivedSykmelding.msgId,
        legekontorOrgName = receivedSykmelding.legekontorOrgName,
        legekontorHerId = receivedSykmelding.legekontorHerId,
        legekontorReshId = receivedSykmelding.legekontorReshId,
        mottattDato = receivedSykmelding.mottattDato.toInstant(ZoneOffset.UTC),
        fellesformat = receivedSykmelding.fellesformat,
        tssid = receivedSykmelding.tssid,
        utenlandskSykmelding = toAvroUtenlandskInfo(receivedSykmelding.utenlandskSykmelding),
        partnerreferanse = receivedSykmelding.partnerreferanse,
        merknader = receivedSykmelding.merknader?.map { Merknad(
            type = it.type, beskrivelse = it.beskrivelse, tidspunkt = it.tidspunkt?.toInstant()
        ) },
        rulesetVersion = receivedSykmelding.rulesetVersion,
        legekontorOrgNr = receivedSykmelding.legekontorOrgNr,
        vedlegg = receivedSykmelding.vedlegg,
        validationResult = toAvroValidationResult(receivedSykmelding.validationResult))
}

fun toAvroSykmelding(sykmelding: no.nav.tsm.smregister.models.Sykmelding): Sykmelding {
    return Sykmelding(
        id = sykmelding.id,
        msgId = sykmelding.id,
        pasientAktoerId = sykmelding.pasientAktoerId,
        medisinskVurdering = toAvroMedisinskVurdering(sykmelding.medisinskVurdering),
        skjermesForPasient = sykmelding.skjermesForPasient,
        arbeidsgiver = toAvroArbeidsgiver(sykmelding.arbeidsgiver),
        perioder = sykmelding.perioder.map { toAvroPeriode(it) },
        prognose = toAvroPrognose(sykmelding.prognose),
        utdypendeOpplysninger = sykmelding.utdypendeOpplysninger,
        tiltakArbeidsplassen = sykmelding.tiltakArbeidsplassen,
        tiltakNAV = sykmelding.tiltakNAV,
        andreTiltak = sykmelding.andreTiltak,
        meldingTilNAV = toAvroMeldingTilNav(sykmelding.meldingTilNAV),
        meldingTilArbeidsgiver = sykmelding.meldingTilArbeidsgiver,
        kontaktMedPasient = toAvroKontaktMedPasient(sykmelding.kontaktMedPasient),
        behandletTidspunkt = sykmelding.behandletTidspunkt.toInstant(ZoneOffset.UTC),
        behandler = toAvroBehandler(sykmelding.behandler),
        avsenderSystem = AvsenderSystem (
            navn = sykmelding.avsenderSystem.navn,
            versjon = sykmelding.avsenderSystem.versjon
        ),
        syketilfelleStartDato = sykmelding.syketilfelleStartDato,
        signaturDato = sykmelding.signaturDato.toInstant(ZoneOffset.UTC),
        navnFastlege = sykmelding.navnFastlege

    )
}

fun toAvroMedisinskVurdering(medisinskVurdering: no.nav.tsm.smregister.models.MedisinskVurdering): MedisinskVurdering {
    return MedisinskVurdering(
        hovedDiagnose = medisinskVurdering.hovedDiagnose?.let { Diagnose(
            system = it.system, kode = it.kode, tekst = it.tekst
        ) },
        biDiagnoser = medisinskVurdering.biDiagnoser.map {
             Diagnose(
                system = it.system, kode = it.kode, tekst = it.tekst
            ) },
        svangerskap = medisinskVurdering.svangerskap,
        yrkesskadeDato = medisinskVurdering.yrkesskadeDato,
        yrkesskade = medisinskVurdering.yrkesskade,
        annenFraversArsak = medisinskVurdering.annenFraversArsak?.let { AnnenFraversArsak(
            beskrivelse = it.beskrivelse,
            grunn = it.grunn.map {
                AnnenFraverGrunn.valueOf(it.name)
            }
        ) }
    )
}

fun toAvroArbeidsgiver(arbeidsgiver: no.nav.tsm.smregister.models.Arbeidsgiver): Arbeidsgiver {
    return Arbeidsgiver(
        harArbeidsgiver = HarArbeidsgiver.valueOf(arbeidsgiver.harArbeidsgiver.name),
        navn = arbeidsgiver.navn,
        yrkesbetegnelse = arbeidsgiver.yrkesbetegnelse,
        stillingsprosent = arbeidsgiver.stillingsprosent
    )
}

fun toAvroPeriode(it: Periode) : no.nav.tsm.avro.model.Periode {
    return Periode(
        fom = it.fom,
        tom = it.tom,
        aktivitetIkkeMulig = it.aktivitetIkkeMulig?.let { AktivitetIkkeMulig(
            medisinskArsak = it.medisinskArsak?.let { MedisinskArsak(beskrivelse = it.beskrivelse, arsak = it.arsak.map { MedisinskArsakType.valueOf(it.name) }) },
            arbeidsrelatertArsak = it.arbeidsrelatertArsak?.let { ArbeidsrelatertArsak(
                beskrivelse = it.beskrivelse, arsak = it.arsak.map { ArbeidsrelatertArsakType.valueOf(it.name) }
            ) }) },
        avventendeInnspillTilArbeidsgiver = it.avventendeInnspillTilArbeidsgiver,
        behandlingsdager = it.behandlingsdager,
        gradert = it.gradert?.let { Gradert(
            reisetilskudd = it.reisetilskudd, grad = it.grad
        ) },
        reisetilskudd = it.reisetilskudd

    )
}

fun toAvroPrognose(prognose: no.nav.tsm.smregister.models.Prognose?): Prognose? {
    if (prognose == null) return null

    return Prognose(
        arbeidsforEtterPeriode = prognose.arbeidsforEtterPeriode,
        hensynArbeidsplassen = prognose.hensynArbeidsplassen,
        erIArbeid = prognose.erIArbeid?.let { ErIArbeid(egetArbeidPaSikt = it.egetArbeidPaSikt, annetArbeidPaSikt = it.annetArbeidPaSikt, arbeidFOM = it.arbeidFOM, vurderingsdato = it.vurderingsdato) },
        erIkkeIArbeid = prognose.erIkkeIArbeid?.let { ErIkkeIArbeid(arbeidsforPaSikt = it.arbeidsforPaSikt, arbeidsforFOM = it.arbeidsforFOM, vurderingsdato = it.vurderingsdato) }
    )
}

fun toAvroMeldingTilNav(meldingTilNAV: no.nav.tsm.smregister.models.MeldingTilNAV?): MeldingTilNAV? {
    if (meldingTilNAV == null) return null

    return MeldingTilNAV(
        bistandUmiddelbart = meldingTilNAV.bistandUmiddelbart,
        beskrivBistand = meldingTilNAV.beskrivBistand
    )
}

fun toAvroKontaktMedPasient(kontaktMedPasient: no.nav.tsm.smregister.models.KontaktMedPasient): KontaktMedPasient {
    return KontaktMedPasient(
        kontaktDato = kontaktMedPasient.kontaktDato,
        begrunnelseIkkeKontakt = kontaktMedPasient.begrunnelseIkkeKontakt
    )
}

fun toAvroBehandler(behandler: no.nav.tsm.smregister.models.Behandler): Behandler {
    return Behandler(
        fornavn = behandler.fornavn,
        mellomnavn = behandler.mellomnavn,
        etternavn = behandler.etternavn,
        aktoerId = behandler.aktoerId,
        fnr = behandler.fnr,
        hpr = behandler.hpr,
        her = behandler.her,
        adresse = Adresse(
            gate = behandler.adresse.gate,
            postnummer = behandler.adresse.postnummer,
            kommune = behandler.adresse.kommune,
            postboks = behandler.adresse.postboks,
            land = behandler.adresse.land,
        ),
        tlf = behandler.tlf
    )
}

fun toAvroUtenlandskInfo(utenlandskSykmelding: no.nav.tsm.smregister.models.UtenlandskInfo?): UtenlandskInfo? {
    if(utenlandskSykmelding == null) return null
    return UtenlandskInfo(
        land = utenlandskSykmelding.land,
        folkeRegistertAdresseErBrakkeEllerTilsvarende = utenlandskSykmelding.folkeRegistertAdresseErBrakkeEllerTilsvarende,
        erAdresseUtland = utenlandskSykmelding.erAdresseUtland,
    )
}

fun toAvroValidationResult(validationResult: no.nav.tsm.smregister.models.ValidationResult): ValidationResult {
    return ValidationResult(
        status = Status.valueOf(validationResult.status.name),
        ruleHits = validationResult.ruleHits.map { ruleInfo ->
            RuleInfo(
                ruleName = ruleInfo.ruleName,
                messageForSender = ruleInfo.messageForSender,
                messageForUser = ruleInfo.messageForUser,
                ruleStatus = Status.valueOf(ruleInfo.ruleStatus.name)
            )
        }
    )
}
