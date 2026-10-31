package online.capitalai.devicecare.core.evidence

/**
 * Domain-neutral evidence: collectors only report data they can actually observe.
 * No permission or missing sensor may be silently mapped to a zero-risk value.
 */
enum class Availability { OBSERVED, DENIED, UNSUPPORTED, ERROR }
enum class RiskLevel { NONE, LOW, MEDIUM, HIGH }

data class DeviceEvidence(
    val id: String,
    val source: String,
    val availability: Availability,
    val observedAtMillis: Long?,
    val expiresAtMillis: Long?,
    val riskLevel: RiskLevel?,
) {
    fun isFresh(nowMillis: Long): Boolean =
        availability == Availability.OBSERVED &&
            observedAtMillis != null &&
            expiresAtMillis != null &&
            observedAtMillis <= nowMillis &&
            nowMillis <= expiresAtMillis &&
            riskLevel != null
}
