package no.nav.tsm.sykmeldinger.kafka.model

import java.time.LocalDateTime

data class FellesformatInput(
    val sykmeldingId: String,
    val mottattDato: LocalDateTime,
    val fellesformat: String,
)
