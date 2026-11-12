package online.capitalai.devicecare.features.permissions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import online.capitalai.devicecare.core.evidence.DeviceObservation
import online.capitalai.devicecare.core.evidence.DeviceObservationBatch
import online.capitalai.devicecare.core.evidence.ObservationStatus
import online.capitalai.devicecare.core.evidence.measuredObservation
import online.capitalai.devicecare.core.evidence.unavailableObservation

/**
 * Android 11+ applies package visibility filters. Counts are scoped to packages
 * returned to this ordinary consumer app; NEVER label as all installed apps.
 */
data class PermissionReading(
    val visiblePackages: Int,
    val sampledPackages: Int,
    val requestedInSample: Int,
    val ownRequested: Int,
    val ownGranted: Int?,
    val truncatedByLimit: Boolean,
)

object PermissionEvidenceMapper {
    private val ids = listOf(
        "visible_packages", "sampled_packages", "sampled_requested_permissions",
        "own_requested_permissions", "own_granted_permissions", "sample_truncated",
    )

    fun missing(now: Long, status: ObservationStatus, reason: String): DeviceObservationBatch =
        DeviceObservationBatch("permissions", now, "ANDROID_VISIBLE_APPS",
            ids.map { unavailableObservation(it, status, now, reason) })

    fun map(now: Long, raw: PermissionReading): DeviceObservationBatch {
        require(raw.visiblePackages >= 0 && raw.sampledPackages >= 0 && raw.requestedInSample >= 0)
        require(raw.ownRequested >= 0 && (raw.ownGranted == null ||
            raw.ownGranted in 0..raw.ownRequested))
        require(raw.sampledPackages <= raw.visiblePackages)
        require(raw.truncatedByLimit == (raw.sampledPackages < raw.visiblePackages))
        return DeviceObservationBatch("permissions", now, "ANDROID_VISIBLE_APPS", listOf(
            measuredObservation("visible_packages", raw.visiblePackages.toString(), "apps", now, "ANDROID_VISIBILITY_FILTERED"),
            measuredObservation("sampled_packages", raw.sampledPackages.toString(), "apps", now, "ANDROID_VISIBILITY_FILTERED"),
            measuredObservation("sampled_requested_permissions", raw.requestedInSample.toString(), "count", now, "ANDROID_VISIBILITY_FILTERED"),
            measuredObservation("own_requested_permissions", raw.ownRequested.toString(), "count", now, "NONE"),
            if (raw.ownGranted == null)
                unavailableObservation("own_granted_permissions", ObservationStatus.UNSUPPORTED, now, "FLAGS_UNAVAILABLE")
            else measuredObservation("own_granted_permissions", raw.ownGranted.toString(), "count", now, "NONE"),
            measuredObservation("sample_truncated", raw.truncatedByLimit.toString(), null, now, "ANDROID_VISIBILITY_FILTERED"),
        ))
    }
}

/** On-demand aggregate only. Does not persist or log the installed-app list. */
class AndroidPermissionCollector(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object { const val MAX_SAMPLE = 250 }

    @Suppress("DEPRECATION")
    private fun packages(pm: PackageManager): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

    @Suppress("DEPRECATION")
    private fun ownInfo(pm: PackageManager): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }

    fun collect(): DeviceObservationBatch {
        val now = clock()
        return try {
            val pm = context.packageManager
            val visible = packages(pm)
            val sampled = visible.take(MAX_SAMPLE)
            val own = ownInfo(pm)
            val ownRequested = own.requestedPermissions?.size ?: 0
            val flags = own.requestedPermissionsFlags
            val ownGranted: Int? = when {
                ownRequested == 0 -> 0
                flags == null || flags.size < ownRequested -> null
                else -> flags.take(ownRequested).count {
                    it and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
                }
            }
            PermissionEvidenceMapper.map(now, PermissionReading(
                visiblePackages = visible.size,
                sampledPackages = sampled.size,
                requestedInSample = sampled.sumOf { it.requestedPermissions?.size ?: 0 },
                ownRequested = ownRequested,
                ownGranted = ownGranted,
                truncatedByLimit = visible.size > sampled.size,
            ))
        } catch (_: SecurityException) {
            PermissionEvidenceMapper.missing(now, ObservationStatus.DENIED, "ACCESS_DENIED")
        } catch (_: PackageManager.NameNotFoundException) {
            PermissionEvidenceMapper.missing(now, ObservationStatus.ERROR, "OWN_PACKAGE_UNAVAILABLE")
        } catch (_: RuntimeException) {
            PermissionEvidenceMapper.missing(now, ObservationStatus.ERROR, "READ_FAILED")
        }
    }
}
