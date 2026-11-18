package com.example.devicecare

import com.example.core.evidence.DeviceObservation
import com.example.core.evidence.DeviceObservationBatch
import com.example.core.evidence.ObservationStatus
import com.example.features.permissions.PermissionScanBudget
import com.example.features.permissions.PermissionScanProfile
import com.example.features.permissions.PermissionScanReason
import com.example.features.permissions.ResourceSignal

data class ObservationDisplayRow(
  val id: String,
  val label: String,
  val valueText: String,
  val status: ObservationStatus,
  val limitationCode: String,
)

data class InspectionResult(
  val collectedAtMillis: Long = 0L,
  val apiLevel: Int = 0,
  val packageLimit: Int = PermissionScanBudget.STANDARD_LIMIT,
  val budgetSummary: String = "",
  val optionalReadsDeferred: Boolean = false,
  val elapsedMs: Long = 0L,
  val cpuMs: Long = 0L,
  val profileMetrics: List<Pair<String, String>> = emptyList(),
  val batteryObservations: List<ObservationDisplayRow> = emptyList(),
  val permissionObservations: List<ObservationDisplayRow> = emptyList(),
  val batteryPercent: Int? = null,
  val batteryChargingState: String? = null,
  val errorMessage: String? = null,
)

fun presentInspection(
  profile: PermissionScanProfile,
  budget: PermissionScanBudget,
  battery: DeviceObservationBatch,
  permissions: DeviceObservationBatch,
  optionalReadsDeferred: Boolean,
  elapsedMs: Long,
  cpuMs: Long,
): InspectionResult {
  val batteryPercentObs = battery.observations.firstOrNull { it.id == "battery_percent" }
  val batteryPercent =
    if (batteryPercentObs?.status == ObservationStatus.OBSERVED) {
      batteryPercentObs.value?.toIntOrNull()
    } else null
  val chargingObs = battery.observations.firstOrNull { it.id == "battery_charging" }
  val chargingState =
    if (chargingObs?.status == ObservationStatus.OBSERVED) chargingObs.value else null

  return InspectionResult(
    collectedAtMillis = maxOf(battery.collectedAtMillis, permissions.collectedAtMillis),
    apiLevel = profile.apiLevel,
    packageLimit = budget.packageLimit,
    budgetSummary = budget.describe(profile.apiLevel),
    optionalReadsDeferred = optionalReadsDeferred,
    elapsedMs = elapsedMs,
    cpuMs = cpuMs,
    profileMetrics =
      listOf(
        "RAM-Geräteklasse" to profile.lowRam.describe("kein Low-RAM-Gerät", "Low-RAM-Gerät"),
        "Speicherdruck" to profile.memoryPressure.describe("nicht gemeldet", "aktiv"),
        "Energiesparmodus" to profile.powerSaver.describe("aus", "ein"),
        "Thermische Belastung" to profile.thermal.describe("keine oder leicht", "moderat"),
        "Akkustand" to battery.availabilityText("battery_percent"),
        "Ladezustand" to battery.availabilityText("battery_charging"),
        "Akkutemperatur" to battery.availabilityText("battery_temperature"),
        "Android-Akkuzustand" to
          "${battery.availabilityText("battery_health_reported")} (Systemstatus)",
        "App-Berechtigungen" to
          permissions.availabilityText(
            "sampled_packages",
            "nur für die von Android sichtbare Stichprobe verfügbar",
          ),
      ),
    batteryObservations = battery.observations.map { it.toDisplayRow() },
    permissionObservations = permissions.observations.map { it.toDisplayRow() },
    batteryPercent = batteryPercent,
    batteryChargingState = chargingState,
  )
}

private fun DeviceObservation.toDisplayRow(): ObservationDisplayRow {
  val label =
    when (id) {
      "battery_percent" -> "Akkuladestand"
      "battery_temperature" -> "Akkutemperatur"
      "battery_charging" -> "Ladestatus"
      "battery_health_reported" -> "System-Akkuzustand"
      "visible_packages" -> "Sichtbare Apps (OS-gefiltert)"
      "sampled_packages" -> "Geprüfte Stichprobe"
      "sampled_requested_permissions" -> "Angeforderte Berechtigungen (Stichprobe)"
      "own_requested_permissions" -> "Eigene angeforderte Berechtigungen"
      "own_granted_permissions" -> "Eigene gewährte Berechtigungen"
      "sample_truncated" -> "Stichprobe gekürzt"
      else -> id
    }
  val display =
    if (status == ObservationStatus.OBSERVED) {
      "${value}${unit?.let { " $it" } ?: ""}"
    } else {
      "${status.name} ($limitationCode)"
    }
  return ObservationDisplayRow(
    id = id,
    label = label,
    valueText = display,
    status = status,
    limitationCode = limitationCode,
  )
}

private fun ResourceSignal.describe(clear: String, active: String): String =
  when (this) {
    ResourceSignal.CLEAR -> clear
    ResourceSignal.ACTIVE -> active
    ResourceSignal.HIGH -> "hoch"
    ResourceSignal.UNSUPPORTED -> "nicht unterstützt"
    ResourceSignal.DENIED -> "Zugriff verweigert"
    ResourceSignal.ERROR -> "Abfrage fehlgeschlagen"
    ResourceSignal.UNKNOWN -> "unbekannt"
  }

private fun DeviceObservationBatch.availabilityText(
  id: String,
  observed: String = "von Android gemeldet",
): String =
  when (observations.firstOrNull { it.id == id }?.status) {
    ObservationStatus.OBSERVED -> observed
    ObservationStatus.UNSUPPORTED -> "nicht verfügbar"
    ObservationStatus.DENIED -> "Zugriff verweigert"
    ObservationStatus.ERROR -> "Abfrage fehlgeschlagen"
    null -> "unbekannt"
  }

private fun PermissionScanBudget.describe(apiLevel: Int): String {
  val reason =
    if (reasons.isEmpty()) "Standardumfang"
    else
      reasons.joinToString(", ") {
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
