package no.nav.tsm.sykmeldinger.kafka.model

import java.time.LocalDateTime

data class MigrertSykmelding(
    val sykmeldingId: String,
    val mottattDato: LocalDateTime,
    val receivedSykmelding: String?,
)
