package no.nav.tsm.smregister.models

import java.time.OffsetDateTime

data class Merknad(
    val type: String,
    val beskrivelse: String?,
    val timestamp: OffsetDateTime?
)

