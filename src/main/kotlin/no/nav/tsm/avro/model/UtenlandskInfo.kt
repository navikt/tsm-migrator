package no.nav.tsm.avro.model

import kotlinx.serialization.Serializable

@Serializable
data class UtenlandskInfo(
    val land: String,
    val folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean,
    val erAdresseUtland: Boolean?,
)
