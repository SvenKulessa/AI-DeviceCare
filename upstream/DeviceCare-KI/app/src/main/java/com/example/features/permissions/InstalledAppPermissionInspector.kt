package com.example.features.permissions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build

enum class SensitivityCategory(
  val label: String,
  val isHighSensitivity: Boolean,
) {
  CAMERA("Kamera", true),
  LOCATION("Standort", true),
  MICROPHONE("Mikrofon", true),
  PHONE_SMS("Telefon & SMS", true),
  CONTACTS("Kontakte", true),
  SENSORS_ACTIVITY("Sensoren & Aktivität", false),
  STORAGE_MEDIA("Dateien & Medien", false),
  CALENDAR("Kalender", false),
  NEARBY("Geräte in der Nähe", false),
  NOTIFICATIONS("Benachrichtigungen", false),
  OTHER("Sonstige Laufzeitrechte", false),
}

enum class AppPermissionFilter(val label: String) {
  ALL("Alle"),
  SENSITIVE_ONLY("Sensibel"),
  CAMERA("Kamera"),
  LOCATION("Standort"),
  MICROPHONE("Mikrofon"),
  PHONE_SMS("Telefon/SMS"),
  CONTACTS("Kontakte"),
}

enum class AppSectionCategory(
  val title: String,
  val badge: String,
  val description: String,
  val isCriticalTier: Boolean,
) {
  MULTI_CRITICAL(
    title = "Mehrfach kritisch (CAMERA / LOCATION / RECORD_AUDIO)",
    badge = "KRITISCH · MULTI",
    description = "Apps mit kombiniertem Zugriff auf zwei oder mehr kritische Sensoren.",
    isCriticalTier = true,
  ),
  CAMERA(
    title = "Kamera-Zugriff (CAMERA)",
    badge = "KRITISCH · CAMERA",
    description = "Apps mit aktiv gewährter Kamera-Berechtigung.",
    isCriticalTier = true,
  ),
  LOCATION(
    title = "Standort-Zugriff (LOCATION)",
    badge = "KRITISCH · LOCATION",
    description = "Apps mit aktiv gewährter Standort-Berechtigung.",
    isCriticalTier = true,
  ),
  MICROPHONE(
    title = "Mikrofon-Zugriff (RECORD_AUDIO)",
    badge = "KRITISCH · RECORD_AUDIO",
    description = "Apps mit aktiv gewährter Mikrofon-/Audioaufnahme-Berechtigung.",
    isCriticalTier = true,
  ),
  OTHER_SENSITIVE(
    title = "Weitere gefährliche Laufzeitrechte",
    badge = "SENSIBEL",
    description = "Telefon/SMS, Kontakte, Speicher/Medien, Kalender, Sensoren oder Nähe.",
    isCriticalTier = false,
  ),
  STANDARD(
    title = "Ohne kritische Laufzeitrechte",
    badge = "STANDARD",
    description = "Sichtbare Apps ohne gewährte gefährliche Laufzeitberechtigungen.",
    isCriticalTier = false,
  ),
}

data class CategorizedAppGroup(
  val category: AppSectionCategory,
  val apps: List<InstalledAppPermissionInfo>,
)

data class GrantedDangerousPermission(
  val fullName: String,
  val shortName: String,
  val category: SensitivityCategory,
  val description: String?,
)

data class InstalledAppPermissionInfo(
  val packageName: String,
  val appName: String,
  val isSystemApp: Boolean,
  val totalRequestedPermissions: Int,
  val grantedDangerousPermissions: List<GrantedDangerousPermission>,
) {
  val hasHighSensitivityAccess: Boolean
    get() = grantedDangerousPermissions.any { it.category.isHighSensitivity }

  val categories: Set<SensitivityCategory>
    get() = grantedDangerousPermissions.mapTo(mutableSetOf()) { it.category }

  val criticalSensorTags: List<String>
    get() =
      buildList {
        if (SensitivityCategory.CAMERA in categories) add("CAMERA")
        if (SensitivityCategory.LOCATION in categories) add("LOCATION")
        if (SensitivityCategory.MICROPHONE in categories) add("RECORD_AUDIO")
      }

  val isCriticalSensorApp: Boolean
    get() = criticalSensorTags.isNotEmpty()

  val primarySectionCategory: AppSectionCategory
    get() =
      when {
        criticalSensorTags.size >= 2 -> AppSectionCategory.MULTI_CRITICAL
        SensitivityCategory.CAMERA in categories -> AppSectionCategory.CAMERA
        SensitivityCategory.LOCATION in categories -> AppSectionCategory.LOCATION
        SensitivityCategory.MICROPHONE in categories -> AppSectionCategory.MICROPHONE
        grantedDangerousPermissions.isNotEmpty() -> AppSectionCategory.OTHER_SENSITIVE
        else -> AppSectionCategory.STANDARD
      }
}

data class InstalledAppsScanResult(
  val scannedAtMillis: Long,
  val visiblePackageCount: Int,
  val apps: List<InstalledAppPermissionInfo>,
  val errorMessage: String? = null,
) {
  val appsWithDangerousCount: Int
    get() = apps.count { it.grantedDangerousPermissions.isNotEmpty() }

  val appsWithHighSensitivityCount: Int
    get() = apps.count { it.hasHighSensitivityAccess }

  val appsWithCriticalSensorsCount: Int
    get() = apps.count { it.isCriticalSensorApp }

  val cameraAccessCount: Int
    get() = apps.count { SensitivityCategory.CAMERA in it.categories }

  val locationAccessCount: Int
    get() = apps.count { SensitivityCategory.LOCATION in it.categories }

  val microphoneAccessCount: Int
    get() = apps.count { SensitivityCategory.MICROPHONE in it.categories }
}

object PermissionSensitivityClassifier {
  fun classify(permissionName: String): SensitivityCategory =
    when (permissionName) {
      "android.permission.CAMERA" -> SensitivityCategory.CAMERA
      "android.permission.ACCESS_FINE_LOCATION",
      "android.permission.ACCESS_COARSE_LOCATION",
      "android.permission.ACCESS_BACKGROUND_LOCATION" -> SensitivityCategory.LOCATION
      "android.permission.RECORD_AUDIO" -> SensitivityCategory.MICROPHONE
      "android.permission.READ_SMS",
      "android.permission.SEND_SMS",
      "android.permission.RECEIVE_SMS",
      "android.permission.RECEIVE_MMS",
      "android.permission.RECEIVE_WAP_PUSH",
      "android.permission.READ_CALL_LOG",
      "android.permission.WRITE_CALL_LOG",
      "android.permission.READ_PHONE_STATE",
      "android.permission.READ_PHONE_NUMBERS",
      "android.permission.CALL_PHONE",
      "android.permission.ANSWER_PHONE_CALLS",
      "android.permission.PROCESS_OUTGOING_CALLS",
      "android.permission.ADD_VOICEMAIL",
      "android.permission.USE_SIP" -> SensitivityCategory.PHONE_SMS
      "android.permission.READ_CONTACTS",
      "android.permission.WRITE_CONTACTS",
      "android.permission.GET_ACCOUNTS" -> SensitivityCategory.CONTACTS
      "android.permission.BODY_SENSORS",
      "android.permission.BODY_SENSORS_BACKGROUND",
      "android.permission.ACTIVITY_RECOGNITION" -> SensitivityCategory.SENSORS_ACTIVITY
      "android.permission.READ_EXTERNAL_STORAGE",
      "android.permission.WRITE_EXTERNAL_STORAGE",
      "android.permission.READ_MEDIA_IMAGES",
      "android.permission.READ_MEDIA_VIDEO",
      "android.permission.READ_MEDIA_AUDIO",
      "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
      "android.permission.ACCESS_MEDIA_LOCATION" -> SensitivityCategory.STORAGE_MEDIA
      "android.permission.READ_CALENDAR",
      "android.permission.WRITE_CALENDAR" -> SensitivityCategory.CALENDAR
      "android.permission.BLUETOOTH_SCAN",
      "android.permission.BLUETOOTH_CONNECT",
      "android.permission.BLUETOOTH_ADVERTISE",
      "android.permission.NEARBY_WIFI_DEVICES",
      "android.permission.UWB_RANGING" -> SensitivityCategory.NEARBY
      "android.permission.POST_NOTIFICATIONS" -> SensitivityCategory.NOTIFICATIONS
      else -> SensitivityCategory.OTHER
    }

  fun shortPermissionName(fullName: String): String =
    fullName.substringAfterLast('.')

  fun buildGrantedDangerousList(
    requestedPermissions: Array<out String>?,
    requestedPermissionsFlags: IntArray?,
    isDangerousResolver: (String) -> Boolean,
    descriptionResolver: (String) -> String? = { null },
  ): List<GrantedDangerousPermission> {
    if (requestedPermissions == null || requestedPermissionsFlags == null) return emptyList()
    val count = minOf(requestedPermissions.size, requestedPermissionsFlags.size)
    val result = mutableListOf<GrantedDangerousPermission>()
    for (i in 0 until count) {
      val permName = requestedPermissions[i]
      val granted =
        (requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
      if (granted && isDangerousResolver(permName)) {
        result.add(
          GrantedDangerousPermission(
            fullName = permName,
            shortName = shortPermissionName(permName),
            category = classify(permName),
            description = descriptionResolver(permName),
          )
        )
      }
    }
    return result.sortedWith(
      compareByDescending<GrantedDangerousPermission> { it.category.isHighSensitivity }
        .thenBy { it.category.label }
        .thenBy { it.shortName }
    )
  }

  fun categorizeApps(apps: List<InstalledAppPermissionInfo>): List<CategorizedAppGroup> {
    if (apps.isEmpty()) return emptyList()
    val grouped = apps.groupBy { it.primarySectionCategory }
    return AppSectionCategory.entries.mapNotNull { category ->
      val matching = grouped[category]
      if (matching.isNullOrEmpty()) {
        null
      } else {
        CategorizedAppGroup(
          category = category,
          apps =
            matching.sortedWith(
              compareByDescending<InstalledAppPermissionInfo> { it.criticalSensorTags.size }
                .thenByDescending { it.grantedDangerousPermissions.size }
                .thenBy { it.appName.lowercase() }
            ),
        )
      }
    }
  }
}

/**
 * Lokaler On-Demand-Inspektor für installierte (OS-sichtbare) Apps und deren gewährte
 * gefährliche Laufzeitberechtigungen. Keine Persistenz, kein Netzwerk-Upload, keine
 * QUERY_ALL_PACKAGES-Berechtigung.
 */
class InstalledAppPermissionInspector(
  private val context: Context,
  private val clock: () -> Long = System::currentTimeMillis,
) {
  @Suppress("DEPRECATION")
  private fun queryInstalledPackages(pm: PackageManager): List<PackageInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.getInstalledPackages(
        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
      )
    } else {
      pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    }

  private fun isDangerousPermission(pm: PackageManager, permissionName: String): Boolean {
    val info = lookupPermissionInfo(pm, permissionName) ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      info.protection == PermissionInfo.PROTECTION_DANGEROUS
    } else {
      @Suppress("DEPRECATION")
      (info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) ==
        PermissionInfo.PROTECTION_DANGEROUS
    }
  }

  private fun lookupPermissionInfo(pm: PackageManager, permissionName: String): PermissionInfo? =
    try {
      pm.getPermissionInfo(permissionName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
      null
    } catch (_: RuntimeException) {
      null
    }

  fun inspectVisibleApps(): InstalledAppsScanResult {
    val now = clock()
    return try {
      val pm = context.packageManager
      val packages = queryInstalledPackages(pm)
      val permDangerousCache = HashMap<String, Boolean>()
      val permDescCache = HashMap<String, String?>()

      val appInfos =
        packages.map { pkg ->
          val appInfo = pkg.applicationInfo
          val label =
            try {
              appInfo?.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg.packageName
            } catch (_: RuntimeException) {
              pkg.packageName
            }
          val isSystem =
            appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

          val grantedDangerous =
            PermissionSensitivityClassifier.buildGrantedDangerousList(
              requestedPermissions = pkg.requestedPermissions,
              requestedPermissionsFlags = pkg.requestedPermissionsFlags,
              isDangerousResolver = { perm ->
                permDangerousCache.getOrPut(perm) { isDangerousPermission(pm, perm) }
              },
              descriptionResolver = { perm ->
                permDescCache.getOrPut(perm) {
                  lookupPermissionInfo(pm, perm)?.loadDescription(pm)?.toString()
                }
              },
            )

          InstalledAppPermissionInfo(
            packageName = pkg.packageName,
            appName = label,
            isSystemApp = isSystem,
            totalRequestedPermissions = pkg.requestedPermissions?.size ?: 0,
            grantedDangerousPermissions = grantedDangerous,
          )
        }
          .sortedWith(
            compareByDescending<InstalledAppPermissionInfo> { it.hasHighSensitivityAccess }
              .thenByDescending { it.grantedDangerousPermissions.size }
              .thenBy { it.appName.lowercase() }
          )

      InstalledAppsScanResult(
        scannedAtMillis = now,
        visiblePackageCount = packages.size,
        apps = appInfos,
      )
    } catch (_: SecurityException) {
      InstalledAppsScanResult(
        scannedAtMillis = now,
        visiblePackageCount = 0,
        apps = emptyList(),
        errorMessage = "Zugriff auf Paket-Metadaten vom System verweigert.",
      )
    } catch (_: RuntimeException) {
      InstalledAppsScanResult(
        scannedAtMillis = now,
        visiblePackageCount = 0,
        apps = emptyList(),
        errorMessage = "Installierte Apps konnten lokal nicht gelesen werden.",
      )
    }
  }
}
