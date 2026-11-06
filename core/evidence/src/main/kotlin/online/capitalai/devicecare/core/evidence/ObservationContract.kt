package online.capitalai.devicecare.core.evidence

/**
 * M1 contract: local diagnostic values only. The allowlist prevents accidental
 * package names, free-form device identifiers or exception text from entering
 * customer-facing observations. These observations are NEVER risk scores.
 */
object ObservationContract {
    const val VERSION = "devicecare-observation/1"
    const val MAX_AGE_MILLIS = 60_000L

    private val batteryStates = setOf("CHARGING", "DISCHARGING", "NOT_CHARGING", "FULL")
    private val batteryHealth = setOf("GOOD", "OVERHEAT", "DEAD", "OVER_VOLTAGE", "UNSPECIFIED_FAILURE", "COLD")

    fun permittedValue(id: String, value: String, unit: String?): Boolean {
        fun boundedInteger(max: Int) =
            value.matches(Regex("0|[1-9][0-9]{0,6}")) && value.toIntOrNull()?.let { it in 0..max } == true
        return when (id) {
            "battery_percent" -> unit == "%" && boundedInteger(100)
            "battery_temperature" -> unit == "C" &&
                value.matches(Regex("-?[0-9]{1,2}\\.[0-9]")) &&
                value.toDoubleOrNull()?.let { it in -40.0..90.0 } == true
            "battery_charging" -> unit == null && value in batteryStates
            "battery_health_reported" -> unit == null && value in batteryHealth
            "visible_packages" -> unit == "apps" && boundedInteger(1_000_000)
            "sampled_packages" -> unit == "apps" && boundedInteger(250)
            "sampled_requested_permissions" -> unit == "count" && boundedInteger(1_000_000)
            "own_requested_permissions", "own_granted_permissions" ->
                unit == "count" && boundedInteger(100_000)
            "sample_truncated" -> unit == null && value in setOf("true", "false")
            else -> false
        }
    }
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
            require(ObservationContract.permittedValue(id, value, unit)) { "Unsupported signal or value" }
            require(expiresAtMillis != null && expiresAtMillis >= observedAtMillis &&
                expiresAtMillis - observedAtMillis <= ObservationContract.MAX_AGE_MILLIS) {
                "Observed signal requires short expiration"
            }
        } else {
            require(value == null && unit == null && expiresAtMillis == null) {
                "Unavailable signal must not carry a value"
            }
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
        require(collectorId in setOf("battery", "permissions")) { "Invalid collector" }
        require(collectedAtMillis >= 0) { "Invalid collection time" }
        require((collectorId == "battery" && scope == "LOCAL_BATTERY") ||
            (collectorId == "permissions" && scope == "ANDROID_VISIBLE_APPS")) { "Invalid scope" }
        require(observations.isNotEmpty() && observations.size <= 16) { "Invalid observation count" }
        require(observations.map { it.id }.distinct().size == observations.size) { "Duplicate evidence id" }
        require(observations.all { it.observedAtMillis == collectedAtMillis }) { "Time mismatch" }
        require(observations.all {
            if (collectorId == "battery") it.id.startsWith("battery_")
            else it.id in setOf("visible_packages", "sampled_packages", "sampled_requested_permissions",
                "own_requested_permissions", "own_granted_permissions", "sample_truncated")
        }) { "Collector/signal mismatch" }
    }
}

/** Fixed failure codes only; never pass exception strings, package names or IDs. */
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
    Math.addExact(nowMillis, ObservationContract.MAX_AGE_MILLIS), value, unit, code,
)
