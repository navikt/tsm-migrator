package no.nav.tsm.reformat.sykmelding.validation

import java.time.OffsetDateTime


data class ValidationResult(
    val status: RuleType,
    val timestamp: OffsetDateTime,
    val rules: List<Rule>
)

enum class RuleType {
    OK, PENDING, INVALID
}


enum class ValidationType {
    AUTOMATIC, MANUAL
}

sealed interface Rule {
    val type: RuleType
    val name: String
    val description: String
    val validationType: ValidationType
}

data class InvalidRule(
    override val name: String,
    override val description: String,
    val timestamp: OffsetDateTime,
    override val validationType: ValidationType,
) : Rule {
    override val type = RuleType.INVALID
}

data class PendingRule(
    override val name: String,
    val timestamp: OffsetDateTime,
    override val description: String,
    override val validationType: ValidationType
    ) : Rule {
    override val type = RuleType.PENDING
}

data class OKRule(
    override val name: String,
    override val description: String,
    val timestamp: OffsetDateTime,
    override val validationType: ValidationType
) : Rule {
    override val type = RuleType.OK
}
