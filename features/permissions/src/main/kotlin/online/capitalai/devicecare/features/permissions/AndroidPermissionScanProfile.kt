package online.capitalai.devicecare.features.permissions

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

/** Read once per explicit scan. Only coarse OS signals leave this adapter. */
class AndroidPermissionScanProfile(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun read(): PermissionScanProfile {
        val observedAt = clock()
        return PermissionScanProfile(
            apiLevel = Build.VERSION.SDK_INT,
            observedAtMillis = observedAt,
            lowRam = readSignal {
                context.getSystemService(ActivityManager::class.java)?.isLowRamDevice.toSignal()
            },
            memoryPressure = readSignal {
                val manager = context.getSystemService(ActivityManager::class.java)
                manager?.let {
                    val memory = ActivityManager.MemoryInfo()
                    it.getMemoryInfo(memory)
                    memory.lowMemory
                }.toSignal()
            },
            powerSaver = readSignal {
                context.getSystemService(PowerManager::class.java)?.isPowerSaveMode.toSignal()
            },
            thermal = readSignal {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    ResourceSignal.UNSUPPORTED
                } else {
                    when (context.getSystemService(PowerManager::class.java)?.currentThermalStatus) {
                        PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> ResourceSignal.CLEAR
                        PowerManager.THERMAL_STATUS_MODERATE -> ResourceSignal.ACTIVE
                        PowerManager.THERMAL_STATUS_SEVERE, PowerManager.THERMAL_STATUS_CRITICAL,
                        PowerManager.THERMAL_STATUS_EMERGENCY, PowerManager.THERMAL_STATUS_SHUTDOWN -> ResourceSignal.HIGH
                        else -> ResourceSignal.UNKNOWN
                    }
                }
            },
        )
    }

    private fun readSignal(read: () -> ResourceSignal): ResourceSignal = try {
        read()
    } catch (_: SecurityException) {
        ResourceSignal.DENIED
    } catch (_: RuntimeException) {
        ResourceSignal.ERROR
    }

    private fun Boolean?.toSignal(): ResourceSignal = when (this) {
        true -> ResourceSignal.ACTIVE
        false -> ResourceSignal.CLEAR
        null -> ResourceSignal.UNKNOWN
    }
}
