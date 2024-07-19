package no.nav.tsm.sykmeldinger.kafka.model

import no.nav.tsm.smregister.models.ReceivedSykmelding

data class SykmeldingInput(
    val receivedSykmelding: ReceivedSykmelding,
    val source: String,
    val sykmeldingId: String,
)
