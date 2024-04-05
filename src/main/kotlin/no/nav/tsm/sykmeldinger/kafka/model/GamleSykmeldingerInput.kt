package no.nav.tsm.sykmeldinger.kafka.model

import java.time.LocalDateTime

data class GamleSykmeldingerInput(
    val sykmeldingId: String,
    val mottattDato: LocalDateTime,
    val sykmelding: String,

)