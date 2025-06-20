package no.nav.tsm.reformat.sykmelding.model.metadata

import no.nav.tsm.sykmelding.input.core.model.metadata.AdresseType
import no.nav.tsm.sykmelding.input.core.model.metadata.AdresseType.*
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType.*

fun parseAdresseType(v: String?): AdresseType {
            return when (v) {
                "BAD" -> UBRUKELIG_ADRESSE
                "H" -> BOSTEDSADRESSE
                "HP" -> FOLKEREGISTERADRESSE
                "HV" -> FERIEADRESSE
                "INV" -> FAKTURERINGSADRESSE
                "PST" -> POSTADRESSE
                "RES" -> BESOKSADRESSE
                "TMP" -> MIDLERTIDIG_ADRESSE
                "WP" -> ARBEIDSADRESSE
                "2" -> AdresseType.UGYLDIG
                "token" -> AdresseType.UGYLDIG
                "OTHER" -> AdresseType.UGYLDIG
                null -> UKJENT
                else -> throw IllegalArgumentException("Ukjent adressestype: $v")
            }
        }

fun parseKontaktinfoType(v: String?): KontaktinfoType {
    return when (v) {
        "AS" -> TELEFONSVARER
        "EC" -> NODNUMMER
        "F" -> FAX_TELEFAKS
        "H" -> HJEMME_ELLER_UKJENT
        "HP" -> HOVEDTELEFON
        "HV" -> FERIETELEFON
        "MC" -> MOBILTELEFON
        "PG" -> PERSONSOKER
        "WC" -> ARBEIDSPLASS_SENTRALBORD
        "WD" -> ARBEIDSPLASS_DIREKTENUMMER
        "WP" -> ARBEIDSPLASS
        null -> IKKE_OPPGITT
        "Tel" -> KontaktinfoType.UGYLDIG
        "vTelecomToken1" -> KontaktinfoType.UGYLDIG
        "vTelecomToken2" -> KontaktinfoType.UGYLDIG
        "NONE" -> KontaktinfoType.UGYLDIG
        else -> throw IllegalArgumentException("Ukjent kontaktinfotype: $v")
    }
}
