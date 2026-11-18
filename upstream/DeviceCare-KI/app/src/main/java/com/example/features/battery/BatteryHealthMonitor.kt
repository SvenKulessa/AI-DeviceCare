package com.example.features.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ChargingStatus(
  val label: String,
  val detail: String,
) {
  CHARGING("Lädt", "Stromquelle verbunden, Akku wird geladen"),
  DISCHARGING("Entlädt", "Gerät läuft im regulären Akkubetrieb"),
  NOT_CHARGING("Angeschlossen (kein Ladevorgang)", "Stromquelle erkannt, lädt aktuell nicht"),
  FULL("Voll geladen", "Ladezustand bei 100 % abgeschlossen"),
  UNKNOWN("Unbekannt", "System-Ladestatus wird nicht gemeldet"),
}

enum class PowerSource(val label: String) {
  NONE("Akkubetrieb"),
  AC("Netzteil (AC)"),
  USB("USB-Anschluss"),
  WIRELESS("Kabellos (Qi)"),
  DOCK("Dockingstation"),
  UNKNOWN("Unbekannte Quelle"),
}

enum class BatteryHealthCondition(
  val label: String,
  val isHealthy: Boolean,
) {
  GOOD("Gut", true),
  OVERHEAT("Überhitzt", false),
  DEAD("Erschöpft / Defekt", false),
  OVER_VOLTAGE("Überspannung", false),
  COLD("Zu kalt", false),
  UNSPECIFIED_FAILURE("Systemfehler", false),
  UNKNOWN("Nicht verfügbar", true),
}

enum class RemainingTimeSource(val label: String) {
  OS_CHARGE_TIME_REMAINING("Android BatteryManager.computeChargeTimeRemaining()"),
  FUEL_GAUGE_CURRENT_DIV("BatteryManager Fuel-Gauge (CHARGE_COUNTER ÷ Stromstärke)"),
  UNAVAILABLE("Nicht verfügbar (vom System/OEM nicht gemeldet)"),
}

data class RemainingTimeEstimate(
  val millis: Long?,
  val formattedDuration: String,
  val isChargingEstimate: Boolean,
  val source: RemainingTimeSource,
  val limitationNote: String?,
)

data class BatteryTelemetryRaw(
  val observedAtMillis: Long,
  val apiLevel: Int,
  val level: Int?,
  val scale: Int?,
  val propertyCapacity: Int?,
  val statusCode: Int?,
  val pluggedMask: Int,
  val healthCode: Int?,
  val temperatureTenthsCelsius: Int?,
  val voltageMillivolts: Int?,
  val technology: String?,
  val chargeCounterMicroAh: Long?,
  val currentNowMicroA: Long?,
  val currentAverageMicroA: Long?,
  val osChargeTimeRemainingMillis: Long?,
  val powerSaveMode: Boolean?,
)

data class BatteryDashboardSnapshot(
  val observedAtMillis: Long = 0L,
  val percent: Int? = null,
  val chargingStatus: ChargingStatus = ChargingStatus.UNKNOWN,
  val powerSource: PowerSource = PowerSource.NONE,
  val healthCondition: BatteryHealthCondition = BatteryHealthCondition.UNKNOWN,
  val temperatureCelsius: Float? = null,
  val voltageMillivolts: Int? = null,
  val technology: String? = null,
  val chargeCounterMah: Int? = null,
  val currentNowMilliamps: Int? = null,
  val currentAverageMilliamps: Int? = null,
  val remainingTime: RemainingTimeEstimate =
    RemainingTimeEstimate(
      millis = null,
      formattedDuration = "Noch nicht abgefragt",
      isChargingEstimate = false,
      source = RemainingTimeSource.UNAVAILABLE,
      limitationNote = null,
    ),
  val powerSaveMode: Boolean? = null,
  val errorMessage: String? = null,
)

object BatteryHealthEvaluator {
  private const val MIN_PLAUSIBLE_CURRENT_UA = 20_000L // 20 mA
  private const val MAX_PLAUSIBLE_CURRENT_UA = 12_000_000L // 12 A
  private const val MIN_ESTIMATE_MILLIS = 60_000L // 1 minute
  private const val MAX_ESTIMATE_MILLIS = 240L * 3_600_000L // 240 hours

  fun evaluate(raw: BatteryTelemetryRaw): BatteryDashboardSnapshot {
    val percent =
      if (
        raw.level != null && raw.scale != null && raw.scale > 0 && raw.level >= 0 &&
          raw.level <= raw.scale
      ) {
        ((raw.level.toDouble() * 100.0) / raw.scale).roundToInt().coerceIn(0, 100)
      } else {
        raw.propertyCapacity?.takeIf { it in 0..100 }
      }

    val chargingStatus =
      when (raw.statusCode) {
        BatteryManager.BATTERY_STATUS_CHARGING -> ChargingStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingStatus.NOT_CHARGING
        BatteryManager.BATTERY_STATUS_FULL -> ChargingStatus.FULL
        else -> ChargingStatus.UNKNOWN
      }

    val powerSource =
      when {
        (raw.pluggedMask and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> PowerSource.AC
        (raw.pluggedMask and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> PowerSource.USB
        (raw.pluggedMask and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> PowerSource.WIRELESS
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
          (raw.pluggedMask and BatteryManager.BATTERY_PLUGGED_DOCK) != 0 -> PowerSource.DOCK
        raw.pluggedMask == 0 -> PowerSource.NONE
        else -> PowerSource.UNKNOWN
      }

    val healthCondition =
      when (raw.healthCode) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthCondition.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthCondition.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthCondition.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthCondition.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthCondition.COLD
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE ->
          BatteryHealthCondition.UNSPECIFIED_FAILURE
        else -> BatteryHealthCondition.UNKNOWN
      }

    val temperatureCelsius =
      raw.temperatureTenthsCelsius?.takeIf { it in -400..900 }?.let { it / 10f }

    val voltageMv = raw.voltageMillivolts?.takeIf { it in 500..20_000 }
    val tech = raw.technology?.trim()?.takeIf { it.isNotEmpty() }

    val validChargeCounterUah =
      raw.chargeCounterMicroAh?.takeIf { it > 0L && it != Long.MAX_VALUE }
    val chargeCounterMah = validChargeCounterUah?.let { (it / 1000L).toInt() }

    val validNowUa = sanitizeCurrentMicroAmps(raw.currentNowMicroA)
    val validAvgUa = sanitizeCurrentMicroAmps(raw.currentAverageMicroA)

    val remainingTime =
      estimateRemainingTime(
        chargingStatus = chargingStatus,
        osChargeTimeRemainingMillis = raw.osChargeTimeRemainingMillis,
        chargeCounterUah = validChargeCounterUah,
        currentAverageUa = validAvgUa,
        currentNowUa = validNowUa,
      )

    return BatteryDashboardSnapshot(
      observedAtMillis = raw.observedAtMillis,
      percent = percent,
      chargingStatus = chargingStatus,
      powerSource = powerSource,
      healthCondition = healthCondition,
      temperatureCelsius = temperatureCelsius,
      voltageMillivolts = voltageMv,
      technology = tech,
      chargeCounterMah = chargeCounterMah,
      currentNowMilliamps = validNowUa?.let { (it / 1000L).toInt() },
      currentAverageMilliamps = validAvgUa?.let { (it / 1000L).toInt() },
      remainingTime = remainingTime,
      powerSaveMode = raw.powerSaveMode,
    )
  }

  private fun sanitizeCurrentMicroAmps(rawUa: Long?): Long? {
    if (rawUa == null) return null
    if (rawUa == Long.MIN_VALUE || rawUa == Int.MIN_VALUE.toLong() || rawUa == 0L) return null
    if (abs(rawUa) > MAX_PLAUSIBLE_CURRENT_UA) return null
    return rawUa
  }

  private fun estimateRemainingTime(
    chargingStatus: ChargingStatus,
    osChargeTimeRemainingMillis: Long?,
    chargeCounterUah: Long?,
    currentAverageUa: Long?,
    currentNowUa: Long?,
  ): RemainingTimeEstimate {
    if (chargingStatus == ChargingStatus.FULL) {
      return RemainingTimeEstimate(
        millis = 0L,
        formattedDuration = "Voll geladen (100 %)",
        isChargingEstimate = true,
        source = RemainingTimeSource.OS_CHARGE_TIME_REMAINING,
        limitationNote = "Akku ist vollständig geladen.",
      )
    }

    if (chargingStatus == ChargingStatus.CHARGING) {
      val chargeMs = osChargeTimeRemainingMillis?.takeIf { it in 1L..MAX_ESTIMATE_MILLIS }
      return if (chargeMs != null) {
        RemainingTimeEstimate(
          millis = chargeMs,
          formattedDuration = "ca. ${formatDuration(chargeMs)}",
          isChargingEstimate = true,
          source = RemainingTimeSource.OS_CHARGE_TIME_REMAINING,
          limitationNote =
            "Restladezeit direkt von Android BatteryManager.computeChargeTimeRemaining() gemeldet.",
        )
      } else {
        RemainingTimeEstimate(
          millis = null,
          formattedDuration = "Ladezeit nicht gemeldet",
          isChargingEstimate = true,
          source = RemainingTimeSource.UNAVAILABLE,
          limitationNote =
            "computeChargeTimeRemaining() liefert auf diesem Gerät/Ladegerät aktuell keinen Wert.",
        )
      }
    }

    // Discharging / not-charging: compute live fuel-gauge usage estimate from CHARGE_COUNTER / |CURRENT|
    val absCurrentUa =
      listOfNotNull(currentAverageUa, currentNowUa)
        .map { abs(it) }
        .firstOrNull { it in MIN_PLAUSIBLE_CURRENT_UA..MAX_PLAUSIBLE_CURRENT_UA }

    if (chargeCounterUah != null && absCurrentUa != null) {
      val rawMillis = ((chargeCounterUah.toDouble() / absCurrentUa.toDouble()) * 3_600_000.0).toLong()
      val clampedMillis = rawMillis.coerceIn(MIN_ESTIMATE_MILLIS, MAX_ESTIMATE_MILLIS)
      return RemainingTimeEstimate(
        millis = clampedMillis,
        formattedDuration = "ca. ${formatDuration(clampedMillis)}",
        isChargingEstimate = false,
        source = RemainingTimeSource.FUEL_GAUGE_CURRENT_DIV,
        limitationNote =
          "Live-Hochrechnung aus Restkapazität (${chargeCounterUah / 1000L} mAh) ÷ Entladestrom (${absCurrentUa / 1000L} mA).",
      )
    }

    return RemainingTimeEstimate(
      millis = null,
      formattedDuration = "Nicht messbar",
      isChargingEstimate = false,
      source = RemainingTimeSource.UNAVAILABLE,
      limitationNote =
        "Hardware-Fuel-Gauge (CHARGE_COUNTER / Stromaufnahme) vom System nicht verfügbar; keine Schätzung erfunden.",
    )
  }

  fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(1L)
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val minutes = totalMinutes % 60L
    return when {
      days > 0L -> String.format(Locale.GERMAN, "%d T. %d Std.", days, hours)
      hours > 0L -> String.format(Locale.GERMAN, "%d Std. %d Min.", hours, minutes)
      else -> String.format(Locale.GERMAN, "%d Min.", minutes)
    }
  }
}

class AndroidBatteryHealthMonitor(
  private val context: Context,
  private val clock: () -> Long = System::currentTimeMillis,
) {
  fun readSnapshot(batteryIntent: Intent? = null): BatteryDashboardSnapshot {
    val now = clock()
    return try {
      val intent =
        batteryIntent
          ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
          ?: return BatteryDashboardSnapshot(
            observedAtMillis = now,
            errorMessage = "Akkutelemetrie vom Android-System nicht verfügbar.",
          )

      val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
      val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

      fun intExtra(key: String): Int? =
        if (intent.hasExtra(key)) intent.getIntExtra(key, -1) else null

      fun safeLongProperty(prop: Int): Long? =
        bm?.let {
          val v = it.getLongProperty(prop)
          if (v == Long.MIN_VALUE) null else v
        }

      val capacity =
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }

      val chargeTimeRemaining =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          bm?.computeChargeTimeRemaining()?.takeIf { it > 0L }
        } else {
          null
        }

      val raw =
        BatteryTelemetryRaw(
          observedAtMillis = now,
          apiLevel = Build.VERSION.SDK_INT,
          level = intExtra(BatteryManager.EXTRA_LEVEL),
          scale = intExtra(BatteryManager.EXTRA_SCALE),
          propertyCapacity = capacity,
          statusCode = intExtra(BatteryManager.EXTRA_STATUS),
          pluggedMask = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
          healthCode = intExtra(BatteryManager.EXTRA_HEALTH),
          temperatureTenthsCelsius = intExtra(BatteryManager.EXTRA_TEMPERATURE),
          voltageMillivolts = intExtra(BatteryManager.EXTRA_VOLTAGE),
          technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
          chargeCounterMicroAh = safeLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
          currentNowMicroA = safeLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
          currentAverageMicroA = safeLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
          osChargeTimeRemainingMillis = chargeTimeRemaining,
          powerSaveMode = pm?.isPowerSaveMode,
        )

      BatteryHealthEvaluator.evaluate(raw)
    } catch (_: SecurityException) {
      BatteryDashboardSnapshot(
        observedAtMillis = now,
        errorMessage = "Zugriff auf BatteryManager vom System verweigert.",
      )
    } catch (_: RuntimeException) {
      BatteryDashboardSnapshot(
        observedAtMillis = now,
        errorMessage = "Akkudaten konnten lokal nicht gelesen werden.",
      )
    }
  }
}
