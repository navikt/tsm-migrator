package no.nav.tsm.sykmeldinger.kafka.model

import java.time.LocalDateTime

data class HistoriskSykmeldingInput(
    val sykmeldingId: String,
    val mottattDato: LocalDateTime,
    val receivedSykmelding: String?,
    val sykmeldingSource: String = "REGDUMP"
)
