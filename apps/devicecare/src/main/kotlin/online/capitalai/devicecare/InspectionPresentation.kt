package online.capitalai.devicecare

import online.capitalai.devicecare.core.evidence.DeviceObservationBatch
import online.capitalai.devicecare.core.evidence.ObservationStatus
import online.capitalai.devicecare.features.permissions.PermissionScanBudget
import online.capitalai.devicecare.features.permissions.PermissionScanProfile
import online.capitalai.devicecare.features.permissions.PermissionScanReason
import online.capitalai.devicecare.features.permissions.ResourceSignal

internal data class InspectionDisplay(
    val profileLines: List<String> = emptyList(),
    val resultLines: List<String> = emptyList(),
)

internal fun presentInspection(
    profile: PermissionScanProfile,
    budget: PermissionScanBudget,
    battery: DeviceObservationBatch,
    permissions: DeviceObservationBatch,
    optionalReadsDeferred: Boolean,
): InspectionDisplay = InspectionDisplay(
    profileLines = listOf(
        budget.describe(profile.apiLevel),
        "RAM-Geräteklasse: ${profile.lowRam.describe("kein Low-RAM-Gerät", "Low-RAM-Gerät")}",
        "Speicherdruck: ${profile.memoryPressure.describe("nicht gemeldet", "aktiv")}",
        "Energiesparmodus: ${profile.powerSaver.describe("aus", "ein")}",
        "Thermische Belastung: ${profile.thermal.describe("keine oder leicht", "moderat")}",
        "Akkustand: ${battery.availability("battery_percent")}",
        "Ladezustand: ${battery.availability("battery_charging")}",
        "Akkutemperatur: ${battery.availability("battery_temperature")}",
        "Android-Akkuzustand: ${battery.availability("battery_health_reported")} (Systemstatus)",
        "App-Berechtigungen: ${permissions.availability("sampled_packages", "nur für die von Android sichtbare Stichprobe verfügbar")}",
    ),
    resultLines = buildList {
        if (optionalReadsDeferred) {
            add("Die Stichprobe wurde wegen Speicherdruck oder hoher thermischer Belastung während der Prüfung verkürzt.")
        }
        addAll(showBatch(battery))
        addAll(showBatch(permissions))
    },
)

private fun ResourceSignal.describe(clear: String, active: String): String = when (this) {
    ResourceSignal.CLEAR -> clear
    ResourceSignal.ACTIVE -> active
    ResourceSignal.HIGH -> "hoch"
    ResourceSignal.UNSUPPORTED -> "nicht unterstützt"
    ResourceSignal.DENIED -> "Zugriff verweigert"
    ResourceSignal.ERROR -> "Abfrage fehlgeschlagen"
    ResourceSignal.UNKNOWN -> "unbekannt"
}

private fun DeviceObservationBatch.availability(id: String, observed: String = "von Android gemeldet"): String =
    when (observations.firstOrNull { it.id == id }?.status) {
        ObservationStatus.OBSERVED -> observed
        ObservationStatus.UNSUPPORTED -> "nicht verfügbar"
        ObservationStatus.DENIED -> "Zugriff verweigert"
        ObservationStatus.ERROR -> "Abfrage fehlgeschlagen"
        null -> "unbekannt"
    }

/** Never display/record raw installed-package names, identifiers or exceptions. */
private fun showBatch(batch: DeviceObservationBatch): List<String> =
    listOf("Quelle: ${batch.collectorId} · ${batch.scope} · ${batch.contractVersion}") +
        batch.observations.map { observation ->
            val display = if (observation.status == ObservationStatus.OBSERVED)
                "${observation.value}${observation.unit?.let { " $it" } ?: ""}"
            else "${observation.status} (${observation.limitationCode})"
            "${observation.id}: $display"
        }

private fun PermissionScanBudget.describe(apiLevel: Int): String {
    val reason = if (reasons.isEmpty()) "Standardumfang" else reasons.joinToString(", ") {
        when (it) {
            PermissionScanReason.MEMORY_PRESSURE -> "Speicherdruck"
            PermissionScanReason.THERMAL_PRESSURE -> "hohe thermische Belastung"
            PermissionScanReason.LOW_RAM -> "Gerät mit wenig RAM"
            PermissionScanReason.POWER_SAVER -> "Energiesparmodus"
            PermissionScanReason.MODERATE_THERMAL -> "thermische Belastung"
            PermissionScanReason.SIGNAL_UNAVAILABLE -> "Ressourcensignale teilweise nicht verfügbar"
        }
    }
    return "Prüfumfang (Android API $apiLevel): bis zu $packageLimit sichtbare Apps · $reason."
}
