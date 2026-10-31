package online.capitalai.devicecare.core.scoring

import online.capitalai.devicecare.core.evidence.Availability
import online.capitalai.devicecare.core.evidence.DeviceEvidence
import online.capitalai.devicecare.core.evidence.RiskLevel

/** Independent device-scoring contract. No finance factors or model fallback. */
enum class AssessmentStatus { ASSESSED, NOT_ASSESSABLE }
data class DeviceModel(
    val modelId: String,
    val version: String,
    val requiredSignals: Set<String>,
)
data class DeviceAssessment(
    val schemaVersion: String,
    val modelId: String,
    val modelVersion: String,
    val status: AssessmentStatus,
    val score: Int?,
    val coverage: Double,
    val evidenceIds: List<String>,
    val missingSignals: List<String>,
)
interface DeviceModelRegistry {
    fun resolve(modelId: String): DeviceModel?
}
class StaticDeviceModelRegistry(private val models: Map<String, DeviceModel>) : DeviceModelRegistry {
    override fun resolve(modelId: String): DeviceModel? = models[modelId]
}
class DeviceScoringDispatcher(private val registry: DeviceModelRegistry) {
    companion object { const val VERSION = "devicecare-device-score/1" }

    /**
     * P0 reference contract for synthetic fixtures, not a calibrated malware
     * or battery-health model. No measured app/user risk is claimed.
     */
    fun assess(modelId: String, evidence: List<DeviceEvidence>, nowMillis: Long): DeviceAssessment {
        val model = registry.resolve(modelId)
        if (model == null || model.requiredSignals.isEmpty()) {
            return DeviceAssessment(VERSION, modelId, model?.version ?: "unresolved",
                AssessmentStatus.NOT_ASSESSABLE, null, 0.0, emptyList(), listOf("MODEL_UNAVAILABLE"))
        }
        val unique = evidence.groupBy { it.id }
        val complete = model.requiredSignals.sorted().filter { id ->
            val values = unique[id]
            values?.size == 1 && values.single().isFresh(nowMillis)
        }
        val missing = (model.requiredSignals - complete.toSet()).sorted()
        val coverage = complete.size.toDouble() / model.requiredSignals.size
        if (missing.isNotEmpty()) {
            return DeviceAssessment(VERSION, model.modelId, model.version,
                AssessmentStatus.NOT_ASSESSABLE, null, coverage, complete, missing)
        }
        // Fixed reference mapping for fully observed fixture-only signals.
        val penalties = mapOf(RiskLevel.NONE to 0, RiskLevel.LOW to 10,
            RiskLevel.MEDIUM to 35, RiskLevel.HIGH to 65)
        val penalty = complete.sumOf { id -> penalties.getValue(unique.getValue(id).single().riskLevel!!) }
        val score = (100 - penalty / complete.size).coerceIn(0, 100)
        return DeviceAssessment(VERSION, model.modelId, model.version,
            AssessmentStatus.ASSESSED, score, coverage, complete, emptyList())
    }
}
