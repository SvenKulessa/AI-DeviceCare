package online.capitalai.devicecare.features.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import online.capitalai.devicecare.core.evidence.DeviceObservation
import online.capitalai.devicecare.core.evidence.DeviceObservationBatch
import online.capitalai.devicecare.core.evidence.ObservationStatus
import online.capitalai.devicecare.core.evidence.measuredObservation
import online.capitalai.devicecare.core.evidence.unavailableObservation

/** Raw Android input, deliberately free of package identifiers and serial numbers. */
data class BatteryReading(
    val level: Int?, val scale: Int?, val capacityPercent: Int?,
    val temperatureTenthsCelsius: Int?, val chargingStatus: Int?,
    val healthCode: Int?,
)

/** Pure mapper can be negative-tested without Android hardware or an OEM SDK. */
object BatteryEvidenceMapper {
    private val ids = listOf("battery_percent", "battery_temperature", "battery_charging", "battery_health_reported")

    fun missing(now: Long, status: ObservationStatus, reason: String): DeviceObservationBatch =
        DeviceObservationBatch("battery", now, "LOCAL_BATTERY",
            ids.map { unavailableObservation(it, status, now, reason) })

    fun map(now: Long, raw: BatteryReading): DeviceObservationBatch {
        val percent = if (raw.level != null && raw.scale != null &&
            raw.scale > 0 && raw.level >= 0 && raw.level <= raw.scale)
            ((raw.level.toDouble() * 100.0) / raw.scale).toInt()
        else raw.capacityPercent?.takeIf { it in 0..100 }
        fun signal(id: String, value: String?, unit: String? = null): DeviceObservation =
            if (value == null) unavailableObservation(id, ObservationStatus.UNSUPPORTED, now, "OEM_UNAVAILABLE")
            else measuredObservation(id, value, unit, now, "NONE")
        val temperature = raw.temperatureTenthsCelsius?.takeIf { it in -400..900 }
        val status = when (raw.chargingStatus) {
            2 -> "CHARGING"
            3 -> "DISCHARGING"
            4 -> "NOT_CHARGING"
            5 -> "FULL"
            else -> null
        }
        // Android health enums report a system condition, not capacity/wear percentage.
        val health = when (raw.healthCode) {
            2 -> "GOOD"
            3 -> "OVERHEAT"
            4 -> "DEAD"
            5 -> "OVER_VOLTAGE"
            6 -> "UNSPECIFIED_FAILURE"
            7 -> "COLD"
            else -> null
        }
        return DeviceObservationBatch("battery", now, "LOCAL_BATTERY", listOf(
            signal("battery_percent", percent?.toString(), "%"),
            signal("battery_temperature", temperature?.let { "%.1f".format(java.util.Locale.ROOT, it / 10.0) }, "C"),
            signal("battery_charging", status),
            signal("battery_health_reported", health),
        ))
    }
}

/**
 * One-shot sticky battery broadcast; no receiver retained, no wake lock,
 * background scanner, persistence, network access, or dangerous permission.
 */
class AndroidBatteryCollector(private val context: Context, private val clock: () -> Long = System::currentTimeMillis) {
    fun collect(): DeviceObservationBatch {
        val now = clock()
        return try {
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return BatteryEvidenceMapper.missing(now, ObservationStatus.UNSUPPORTED, "BROADCAST_UNAVAILABLE")
            val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
            fun extra(key: String): Int? =
                if (battery.hasExtra(key)) battery.getIntExtra(key, -1) else null
            BatteryEvidenceMapper.map(now, BatteryReading(
                level = extra(BatteryManager.EXTRA_LEVEL),
                scale = extra(BatteryManager.EXTRA_SCALE),
                capacityPercent = capacity,
                temperatureTenthsCelsius = extra(BatteryManager.EXTRA_TEMPERATURE),
                chargingStatus = extra(BatteryManager.EXTRA_STATUS),
                healthCode = extra(BatteryManager.EXTRA_HEALTH),
            ))
        } catch (_: SecurityException) {
            BatteryEvidenceMapper.missing(now, ObservationStatus.DENIED, "ACCESS_DENIED")
        } catch (_: RuntimeException) {
            BatteryEvidenceMapper.missing(now, ObservationStatus.ERROR, "READ_FAILED")
        }
    }
}
