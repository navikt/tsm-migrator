package no.nav.tsm.sykmelding

enum class Result {
    OK, MANUAL_PROCESSING, INVALID
}

data class RuleInfo(
    val ruleName: String, val messageForSender: String, val messageForUser: String, val ruleResult: Result
)

data class ValidationResult(
    val result: Result,
    val rules: List<RuleInfo>,
)
