package no.nav.tsm.avro.model

import kotlinx.serialization.Serializable

@Serializable
data class ValidationResult(val status: Status, val ruleHits: List<RuleInfo>)

@Serializable
data class RuleInfo(
    val ruleName: String,
    val messageForSender: String,
    val messageForUser: String,
    val ruleStatus: Status
)

enum class Status {
    OK,
    MANUAL_PROCESSING,
    INVALID
}
