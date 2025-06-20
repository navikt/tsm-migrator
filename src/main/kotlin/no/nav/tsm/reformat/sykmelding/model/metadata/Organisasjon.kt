package no.nav.tsm.reformat.sykmelding.model.metadata

import no.nav.tsm.sykmelding.input.core.model.metadata.OrgIdType
import no.nav.tsm.sykmelding.input.core.model.metadata.OrgIdType.*
import no.nav.tsm.sykmelding.input.core.model.metadata.OrganisasjonsType
import no.nav.tsm.sykmelding.input.core.model.metadata.OrganisasjonsType.*

fun parseOrgIdType(type: String): OrgIdType {
            return when(type) {
                "AKO" -> AKO
                "APO" -> APO
                "AVD" -> AVD
                "ENH" -> ENH
                "HER" -> HER
                "LAV" -> LAV
                "LIN" -> LIN
                "LOK" -> LOK
                "NPR" -> NPR
                "RSH" -> RSH
                "SYS" -> SYS
                "her" -> OrgIdType.UGYLDIG
                else -> throw IllegalArgumentException("Unknown OrgIdType: $type")
            }
        }

fun parseOrganisasjonsType(v: String?): OrganisasjonsType {
            return when(v) {
                "4" -> PRIVATE_SPESIALISTER_MED_DRIFTSAVTALER
                "110" -> TANNLEGE_TANNHELSE
                "NXU:IT" ->OrganisasjonsType.UGYLDIG
                "NXU:IT," -> OrganisasjonsType.UGYLDIG
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent organisasjonstype: $v")
            }
        }
