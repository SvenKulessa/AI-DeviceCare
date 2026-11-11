package online.capitalai.devicecare.features.permissions

enum class ResourceSignal { CLEAR, ACTIVE, HIGH, UNSUPPORTED, DENIED, ERROR, UNKNOWN }

data class PermissionScanProfile(
    val apiLevel: Int,
    val observedAtMillis: Long,
    val lowRam: ResourceSignal,
    val memoryPressure: ResourceSignal,
    val powerSaver: ResourceSignal,
    val thermal: ResourceSignal,
)

enum class PermissionScanReason {
    MEMORY_PRESSURE, THERMAL_PRESSURE, LOW_RAM, POWER_SAVER, MODERATE_THERMAL,
    SIGNAL_UNAVAILABLE,
}

data class PermissionScanBudget(val packageLimit: Int, val reasons: List<PermissionScanReason>) {
    companion object {
        const val STANDARD_LIMIT = 250
        internal const val PRESSURE_LIMIT = 25

        fun forProfile(profile: PermissionScanProfile): PermissionScanBudget {
            val reasons = buildList {
                if (profile.memoryPressure == ResourceSignal.ACTIVE) add(PermissionScanReason.MEMORY_PRESSURE)
                if (profile.thermal == ResourceSignal.HIGH) add(PermissionScanReason.THERMAL_PRESSURE)
                if (profile.lowRam == ResourceSignal.ACTIVE) add(PermissionScanReason.LOW_RAM)
                if (profile.powerSaver == ResourceSignal.ACTIVE) add(PermissionScanReason.POWER_SAVER)
                if (profile.thermal == ResourceSignal.ACTIVE) add(PermissionScanReason.MODERATE_THERMAL)
                if (listOf(profile.lowRam, profile.memoryPressure, profile.powerSaver, profile.thermal).any {
                    it == ResourceSignal.UNSUPPORTED || it == ResourceSignal.DENIED ||
                        it == ResourceSignal.ERROR || it == ResourceSignal.UNKNOWN
                }) add(PermissionScanReason.SIGNAL_UNAVAILABLE)
            }
            val limit = when {
                PermissionScanReason.MEMORY_PRESSURE in reasons ||
                    PermissionScanReason.THERMAL_PRESSURE in reasons -> PRESSURE_LIMIT
                reasons.isNotEmpty() -> 75
                else -> STANDARD_LIMIT
            }
            return PermissionScanBudget(limit, reasons)
        }
    }
}
