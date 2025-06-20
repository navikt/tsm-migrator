package no.nav.tsm.reformat.sykmelding.model.metadata

import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.Kjonn
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.sykmelding.input.core.model.metadata.RolleTilPasient

fun parsePersonIdType(type: String?): PersonIdType {
            return when (type) {
                "FNR" -> PersonIdType.FNR
                "DNR" -> PersonIdType.DNR
                "HNR" -> PersonIdType.HNR
                "HPR" -> PersonIdType.HPR
                "HER" -> PersonIdType.HER
                "PNR" -> PersonIdType.PNR
                "SEF" -> PersonIdType.SEF
                "DKF" -> PersonIdType.DKF
                "SSN" -> PersonIdType.SSN
                "FPN" -> PersonIdType.FPN
                "DUF" -> PersonIdType.DUF
                "XXX" -> PersonIdType.XXX
                "" -> PersonIdType.UGYLDIG
                null -> return PersonIdType.IKKE_OPPGITT
                else -> throw IllegalArgumentException("PersonIdType $type not supported")
            }
        }


fun parseKjonn(type: String?) : Kjonn {
            return when (type) {
                "1" -> Kjonn.MANN
                "2" -> Kjonn.KVINNE
                "9" -> Kjonn.USPESIFISERT
                "K" -> Kjonn.UGYLDIG
                "M" -> Kjonn.UGYLDIG
                "0" -> Kjonn.UGYLDIG
                "U" -> Kjonn.UGYLDIG
                null -> Kjonn.IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent kjønn: $type")
            }
        }


fun parseHelsepersonellKategori(v: String?): HelsepersonellKategori {
            return when(v) {
                "HE" ->	HelsepersonellKategori.HELSESEKRETAR
                "KI" ->	HelsepersonellKategori.KIROPRAKTOR
                "LE" ->	HelsepersonellKategori.LEGE
                "MT" ->	HelsepersonellKategori.MANUELLTERAPEUT
                "TL" ->	HelsepersonellKategori.TANNLEGE
                "TH" -> HelsepersonellKategori.TANNHELSESEKRETAR
                "FT" -> HelsepersonellKategori.FYSIOTERAPEUT
                "SP" -> HelsepersonellKategori.SYKEPLEIER
                "HP" -> HelsepersonellKategori.HJELPEPLEIER
                "HF" -> HelsepersonellKategori.HELSEFAGARBEIDER
                "JO" -> HelsepersonellKategori.JORDMOR
                "AU" -> HelsepersonellKategori.AUDIOGRAF
                "NP" -> HelsepersonellKategori.NAPRAPAT
                "PS" -> HelsepersonellKategori.PSYKOLOG
                "FO" -> HelsepersonellKategori.FOTTERAPEUT
                "AA" -> HelsepersonellKategori.AMBULANSEARBEIDER
                "XX" ->  HelsepersonellKategori.USPESIFISERT
                "HS" -> HelsepersonellKategori.UGYLDIG
                "token" -> HelsepersonellKategori.UGYLDIG
                null -> HelsepersonellKategori.IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent helsepersonellkategori: $v")
            }
        }

fun parseRolleTilPasient(v: String?): RolleTilPasient {
           return when (v) {
               "4" -> RolleTilPasient.JOURNALANSVARLIG
               "6" -> RolleTilPasient.FASTLEGE
               null -> RolleTilPasient.IKKE_OPPGITT
               else -> throw IllegalArgumentException("Ukjent rolle til pasient: $v")
           }
        }
