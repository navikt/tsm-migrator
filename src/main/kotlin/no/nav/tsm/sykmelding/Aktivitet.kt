package no.nav.tsm.sykmelding

import java.time.LocalDate

enum class Aktivitetstype {
    AKTIVITET_IKKE_MULIG, AVVENTENDE, BEHANDLINGSDAGER, GRADERT, REISETILSKUDD,
}

sealed interface Aktivitet {
    val fom: LocalDate
    val tom: LocalDate
    val type: Aktivitetstype
}

data class Behandlingsdager(
    val antallBehandlingsdager: Int,
    override val fom: LocalDate,
    override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.BEHANDLINGSDAGER
}

data class Gradert(
    val grad: Int, override val fom: LocalDate, override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.GRADERT
}

data class Reisetilskudd(
    val reisetilskudd: Boolean, override val fom: LocalDate, override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.REISETILSKUDD
}

data class Avventende(
    val innspillTilArbeidsgiver: String, override val fom: LocalDate, override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.AVVENTENDE
}

data class AktivitetIkkeMulig(
    val medisinskArsak: MedisinskArsak,
    val arbeidsrelatertArsak: ArbeidsrelatertArsak,
    override val fom: LocalDate,
    override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.AKTIVITET_IKKE_MULIG
}
