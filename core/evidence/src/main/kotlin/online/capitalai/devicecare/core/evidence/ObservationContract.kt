package online.capitalai.devicecare.core.evidence

/**
 * P0.3/M1 observation contract. Observations are measurements, not malware verdicts
 * or calibrated risk factors. Never map unavailable observations to RiskLevel.NONE.
 */
object ObservationContract {
    const val VERSION = "devicecare-observation/1"
    const val MAX_AGE_MILLIS = 60_000L
}

enum class ObservationStatus { OBSERVED, UNSUPPORTED, DENIED, ERROR }

data class DeviceObservation(
    val id: String,
    val status: ObservationStatus,
    val observedAtMillis: Long,
    val expiresAtMillis: Long?,
    val value: String?,
    val unit: String?,
    val limitationCode: String,
) {
    val contractVersion: String = ObservationContract.VERSION

    init {
        require(id.matches(Regex("[a-z][a-z0-9_]{1,63}"))) { "Invalid signal id" }
        require(observedAtMillis >= 0) { "Invalid observed time" }
        require(limitationCode.matches(Regex("[A-Z_]{2,60}"))) { "Invalid limitation code" }
        if (status == ObservationStatus.OBSERVED) {
            require(!value.isNullOrBlank() && value.length <= 64) { "Observed signal requires bounded value" }
            require(expiresAtMillis != null && expiresAtMillis >= observedAtMillis) { "Observed signal requires expiration" }
        } else {
            require(value == null && unit == null && expiresAtMillis == null) { "Unavailable signal must not carry a value" }
        }
        require(unit == null || unit.matches(Regex("[A-Za-z%]{1,12}"))) { "Invalid unit" }
    }

    fun isFresh(nowMillis: Long): Boolean =
        status == ObservationStatus.OBSERVED && nowMillis >= observedAtMillis &&
            expiresAtMillis != null && nowMillis <= expiresAtMillis
}

data class DeviceObservationBatch(
    val collectorId: String,
    val collectedAtMillis: Long,
    val scope: String,
    val observations: List<DeviceObservation>,
) {
    val contractVersion: String = ObservationContract.VERSION

    init {
        require(collectorId.matches(Regex("[a-z][a-z0-9_]{1,63}"))) { "Invalid collector id" }
        require(collectedAtMillis >= 0) { "Invalid collection time" }
        require(scope in setOf("LOCAL_BATTERY", "ANDROID_VISIBLE_APPS")) { "Invalid scope" }
        require(observations.isNotEmpty()) { "No observations" }
        require(observations.map { it.id }.distinct().size == observations.size) { "Duplicate evidence id" }
        require(observations.all { it.observedAtMillis == collectedAtMillis }) { "Time mismatch" }
    }
}

/** Constant-coded failures only: never pass exception messages, device IDs or package names. */
fun unavailableObservation(
    id: String, status: ObservationStatus, collectedAtMillis: Long, code: String,
): DeviceObservation {
    require(status != ObservationStatus.OBSERVED)
    return DeviceObservation(id, status, collectedAtMillis, null, null, null, code)
}

fun measuredObservation(
    id: String, value: String, unit: String?, nowMillis: Long, code: String,
): DeviceObservation = DeviceObservation(
    id, ObservationStatus.OBSERVED, nowMillis,
    nowMillis + ObservationContract.MAX_AGE_MILLIS, value, unit, code,
)
