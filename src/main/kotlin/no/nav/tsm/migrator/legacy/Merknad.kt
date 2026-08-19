package no.nav.tsm.migrator.legacy

import java.time.OffsetDateTime

data class Merknad(
    val type: String,
    val beskrivelse: String?,
    val timestamp: OffsetDateTime?
)

