package no.nav.tsm.avro.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Merknad(
    val type: String,
    val beskrivelse: String?,
    @Contextual
    val tidspunkt: Instant?
)

