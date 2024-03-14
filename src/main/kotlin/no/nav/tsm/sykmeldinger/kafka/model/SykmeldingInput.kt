package no.nav.tsm.sykmeldinger.kafka.model

import java.time.LocalDateTime

data class SykmeldingInput(
    val sykmeldingId: String,
    val mottattDato: LocalDateTime,
)

