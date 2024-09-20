package no.nav.tsm.sykmelding.validation

import java.time.OffsetDateTime


data class ValidationResult(
    val status: RuleResult,
    val rules: List<Rule>
)

enum class RuleType {
    INVALID, PENDING, RESOLVED
}

enum class RuleResult {
    OK, PENDING, INVALID
}

enum class ValidationType {
    AUTOMATIC, MANUAL
}

data class RuleOutcome(
    val outcome: RuleResult,
    val timestamp: OffsetDateTime
)

sealed interface Rule {
    val type: RuleType
    val name: String
    val description: String
    val timestamp: OffsetDateTime
    val validationType: ValidationType
    val outcome: RuleOutcome
}

data class InvalidRule(
    override val outcome: RuleOutcome,
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val description: String
) : Rule {
    override val validationType = ValidationType.AUTOMATIC
    override val type = RuleType.INVALID
}


data class PendingRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val description: String,
    ) : Rule {
    override val validationType = ValidationType.MANUAL
    override val type = RuleType.PENDING
    override val outcome = RuleOutcome(RuleResult.PENDING, timestamp)
}

data class ResolvedRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val description: String,
    override val outcome: RuleOutcome
) : Rule {
    override val type = RuleType.RESOLVED
    override val validationType = ValidationType.MANUAL
}
