package no.nav.tsm.reformat.sykmelding.model.metadata

import no.nav.tsm.sykmelding.input.core.model.metadata.AckType


fun parseAckType(value: String?): AckType {
    return when (value) {
        null -> AckType.IKKE_OPPGITT
        "J" -> AckType.JA
        "N" -> AckType.NEI
        "F" -> AckType.KUN_VED_FEIL
        "" -> AckType.UGYLDIG
        else -> throw IllegalArgumentException("Unrecognized ack type: $value")
    }
}
