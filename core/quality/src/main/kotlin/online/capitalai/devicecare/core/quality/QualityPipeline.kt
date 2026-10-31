package online.capitalai.devicecare.core.quality

/**
 * QA reports are build/CI evidence. The app may display them, but must never use
 * them as proof of device safety or to rewrite a device risk assessment.
 */
enum class CheckStatus { PASS, FAIL, NOT_PROVEN, NOT_APPLICABLE }
data class QualityCheck(val id: String, val status: CheckStatus)
data class QualityReport(
    val schemaVersion: String,
    val commitSha: String?,
    val checks: List<QualityCheck>,
    val status: CheckStatus,
)
object QualityPipeline {
    const val VERSION = "devicecare-core-quality/1"
    fun assess(checks: List<QualityCheck>, commitSha: String?): QualityReport {
        require(checks.map { it.id }.distinct().size == checks.size) { "Duplicate checks are not allowed" }
        val evaluated = checks.filter { it.status != CheckStatus.NOT_APPLICABLE }
        val result = when {
            checks.any { it.status == CheckStatus.FAIL } -> CheckStatus.FAIL
            commitSha == null || !Regex("[0-9a-f]{40}").matches(commitSha) -> CheckStatus.NOT_PROVEN
            evaluated.isEmpty() || evaluated.any { it.status == CheckStatus.NOT_PROVEN } -> CheckStatus.NOT_PROVEN
            else -> CheckStatus.PASS
        }
        return QualityReport(VERSION, commitSha, checks.toList(), result)
    }
}
