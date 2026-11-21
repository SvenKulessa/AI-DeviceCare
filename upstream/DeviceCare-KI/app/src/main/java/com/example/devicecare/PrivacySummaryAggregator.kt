package com.example.devicecare

import com.example.ai.PermissionPrivacyAnalysisState
import com.example.ai.PrivacyAnalysisReport
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.SensitivityCategory

enum class PrivacyPostureTier(
  val title: String,
  val badge: String,
  val description: String,
) {
  UNASSESSED(
    title = "Prüfung ausstehend",
    badge = "NOT_PROVEN",
    description =
      "Starte den lokalen Komplett-Check, um App-Berechtigungen und System-Metadaten in dieser Sitzung auszuwerten.",
  ),
  PARTIAL(
    title = "Teilweise geprüft",
    badge = "TEILSTATUS",
    description =
      "Es liegen erste lokale Hinweise vor, aber mindestens eine Kernprüfung (Apps oder Systemcheck) fehlt noch.",
  ),
  REVIEW_RECOMMENDED(
    title = "Überprüfung empfohlen",
    badge = "PRÜFEN",
    description =
      "Lokale Signale zeigen kritische Rechte-Kombinationen, ältere Patch-Stände oder fehlende Systemnachweise.",
  ),
  CHECKED_BASELINE(
    title = "Lokale Basis geprüft",
    badge = "LOKAL OK",
    description =
      "Keine lokalen Alters- oder Mehrfachsensor-Warnungen gefunden. Externe Dienste (Play Protect, OEM-OTA) bleiben NOT_PROVEN.",
  ),
}

enum class AdvicePriority(val label: String) {
  HIGH("HOCH"),
  MEDIUM("MITTEL"),
  LOW("NIEDRIG"),
  INFO("HINWEIS"),
}

sealed interface PrivacyAdviceAction {
  data object RunCombinedCheck : PrivacyAdviceAction

  data object OpenAppPermissions : PrivacyAdviceAction

  data class OpenSpecificAppSettings(
    val packageName: String,
    val appName: String,
  ) : PrivacyAdviceAction

  data object OpenSecuritySettings : PrivacyAdviceAction

  data object OpenSystemSecurityScreen : PrivacyAdviceAction

  data object OpenPlayProtectInStore : PrivacyAdviceAction

  data object OpenAiAdvisor : PrivacyAdviceAction
}

data class PrivacyAdviceItem(
  val id: String,
  val title: String,
  val detail: String,
  val categoryLabel: String,
  val priority: AdvicePriority,
  val actionLabel: String,
  val action: PrivacyAdviceAction,
)

data class SensorExposureMetric(
  val id: String,
  val label: String,
  val appCount: Int,
  val filterTag: String,
)

data class PrivacySummaryReport(
  val postureTier: PrivacyPostureTier,
  val checksCompletedCount: Int,
  val checksTotalCount: Int,
  val coverageFraction: Float,
  val hasPermissionScan: Boolean,
  val hasSecurityCheck: Boolean,
  val hasInspectionRun: Boolean,
  val visibleAppsCount: Int,
  val appsWithDangerousCount: Int,
  val appsWithHighSensitivityCount: Int,
  val appsWithCriticalSensorsCount: Int,
  val multiCriticalApps: List<InstalledAppPermissionInfo>,
  val sensorMetrics: List<SensorExposureMetric>,
  val androidReleaseLabel: String?,
  val securityPatch: String?,
  val patchAgeSignal: PatchAgeSignal,
  val playServicesVersion: String?,
  val playStoreVersion: String?,
  val webViewVersion: String?,
  val webViewProvider: String?,
  val aiPrivacyReport: PrivacyAnalysisReport?,
  val adviceItems: List<PrivacyAdviceItem>,
) {
  val securitySnapshotStatusLabel: String
    get() =
      when (patchAgeSignal) {
        PatchAgeSignal.REVIEW_RECOMMENDED -> "Patch ≥ 60 Tage"
        PatchAgeSignal.NO_AGE_WARNING -> "Patch lokal unauffällig"
        PatchAgeSignal.NOT_PROVEN ->
          if (hasSecurityCheck) "Patch-Datum unklar" else "Noch nicht geprüft"
      }
}

object PrivacySummaryAggregator {
  private const val TOTAL_LOCAL_MODULES = 3

  fun aggregate(
    appsScanResult: InstalledAppsScanResult?,
    securitySnapshot: SystemSecuritySnapshot?,
    inspectionResult: InspectionResult?,
    privacyAnalysisState: PermissionPrivacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
  ): PrivacySummaryReport {
    val validAppsScan = appsScanResult?.takeIf { it.errorMessage == null }
    val validInspection = inspectionResult?.takeIf { it.errorMessage == null }

    val hasPermissionScan = validAppsScan != null
    val hasSecurityCheck = securitySnapshot != null
    val hasInspectionRun = validInspection != null

    val completedCount =
      listOf(hasPermissionScan, hasSecurityCheck, hasInspectionRun).count { it }
    val coverageFraction = completedCount.toFloat() / TOTAL_LOCAL_MODULES.toFloat()

    val apps = validAppsScan?.apps.orEmpty()
    val multiCriticalApps =
      apps
        .filter { it.criticalSensorTags.size >= 2 }
        .sortedWith(
          compareByDescending<InstalledAppPermissionInfo> { it.criticalSensorTags.size }
            .thenByDescending { it.grantedDangerousPermissions.size }
            .thenBy { it.appName.lowercase() }
        )

    val phoneSmsCount = apps.count { SensitivityCategory.PHONE_SMS in it.categories }
    val contactsCount = apps.count { SensitivityCategory.CONTACTS in it.categories }

    val sensorMetrics =
      listOf(
        SensorExposureMetric(
          id = "camera",
          label = "Kamera",
          appCount = validAppsScan?.cameraAccessCount ?: 0,
          filterTag = "CAMERA",
        ),
        SensorExposureMetric(
          id = "location",
          label = "Standort",
          appCount = validAppsScan?.locationAccessCount ?: 0,
          filterTag = "LOCATION",
        ),
        SensorExposureMetric(
          id = "microphone",
          label = "Mikrofon",
          appCount = validAppsScan?.microphoneAccessCount ?: 0,
          filterTag = "RECORD_AUDIO",
        ),
        SensorExposureMetric(
          id = "phone_sms",
          label = "Telefon/SMS",
          appCount = phoneSmsCount,
          filterTag = "PHONE_SMS",
        ),
        SensorExposureMetric(
          id = "contacts",
          label = "Kontakte",
          appCount = contactsCount,
          filterTag = "CONTACTS",
        ),
      )

    val patchSignal =
      if (securitySnapshot != null) {
        PatchAgePolicy.classify(securitySnapshot.securityPatch, securitySnapshot.collectedAtMillis)
      } else {
        PatchAgeSignal.NOT_PROVEN
      }

    val aiReport =
      (privacyAnalysisState as? PermissionPrivacyAnalysisState.Success)?.report

    val adviceItems =
      buildAdviceItems(
        validAppsScan = validAppsScan,
        multiCriticalApps = multiCriticalApps,
        phoneSmsCount = phoneSmsCount,
        contactsCount = contactsCount,
        securitySnapshot = securitySnapshot,
        patchSignal = patchSignal,
        hasInspectionRun = hasInspectionRun,
        aiReport = aiReport,
      )

    val postureTier =
      determinePostureTier(
        hasPermissionScan = hasPermissionScan,
        hasSecurityCheck = hasSecurityCheck,
        patchSignal = patchSignal,
        multiCriticalCount = multiCriticalApps.size,
        securitySnapshot = securitySnapshot,
      )

    return PrivacySummaryReport(
      postureTier = postureTier,
      checksCompletedCount = completedCount,
      checksTotalCount = TOTAL_LOCAL_MODULES,
      coverageFraction = coverageFraction,
      hasPermissionScan = hasPermissionScan,
      hasSecurityCheck = hasSecurityCheck,
      hasInspectionRun = hasInspectionRun,
      visibleAppsCount = validAppsScan?.visiblePackageCount ?: 0,
      appsWithDangerousCount = validAppsScan?.appsWithDangerousCount ?: 0,
      appsWithHighSensitivityCount = validAppsScan?.appsWithHighSensitivityCount ?: 0,
      appsWithCriticalSensorsCount = validAppsScan?.appsWithCriticalSensorsCount ?: 0,
      multiCriticalApps = multiCriticalApps,
      sensorMetrics = sensorMetrics,
      androidReleaseLabel =
        securitySnapshot?.let { "Android ${it.androidRelease} (API ${it.apiLevel})" },
      securityPatch = securitySnapshot?.securityPatch,
      patchAgeSignal = patchSignal,
      playServicesVersion = securitySnapshot?.playServicesVersion,
      playStoreVersion = securitySnapshot?.playStoreVersion,
      webViewVersion = securitySnapshot?.webViewVersion,
      webViewProvider = securitySnapshot?.webViewProvider,
      aiPrivacyReport = aiReport,
      adviceItems = adviceItems,
    )
  }

  private fun determinePostureTier(
    hasPermissionScan: Boolean,
    hasSecurityCheck: Boolean,
    patchSignal: PatchAgeSignal,
    multiCriticalCount: Int,
    securitySnapshot: SystemSecuritySnapshot?,
  ): PrivacyPostureTier {
    if (!hasPermissionScan && !hasSecurityCheck) {
      return PrivacyPostureTier.UNASSESSED
    }
    val missingCorePackage =
      securitySnapshot != null &&
        (securitySnapshot.playServicesVersion == null || securitySnapshot.webViewVersion == null)
    if (patchSignal == PatchAgeSignal.REVIEW_RECOMMENDED || multiCriticalCount > 0 || missingCorePackage) {
      return PrivacyPostureTier.REVIEW_RECOMMENDED
    }
    if (!hasPermissionScan || !hasSecurityCheck) {
      return PrivacyPostureTier.PARTIAL
    }
    return PrivacyPostureTier.CHECKED_BASELINE
  }

  private fun buildAdviceItems(
    validAppsScan: InstalledAppsScanResult?,
    multiCriticalApps: List<InstalledAppPermissionInfo>,
    phoneSmsCount: Int,
    contactsCount: Int,
    securitySnapshot: SystemSecuritySnapshot?,
    patchSignal: PatchAgeSignal,
    hasInspectionRun: Boolean,
    aiReport: PrivacyAnalysisReport?,
  ): List<PrivacyAdviceItem> {
    val items = mutableListOf<PrivacyAdviceItem>()

    if (validAppsScan == null || securitySnapshot == null) {
      val missingParts =
        buildList {
          if (validAppsScan == null) add("App-Berechtigungsscan")
          if (securitySnapshot == null) add("System- & Google-Sicherheitscheck")
          if (!hasInspectionRun) add("Lokale Systemprüfung")
        }
      items +=
        PrivacyAdviceItem(
          id = "run_missing_checks",
          title = "Lokale Datengrundlage vervollständigen",
          detail =
            "Noch ausstehend: ${missingParts.joinToString(", ")}. Erst nach explizitem Scan liegen vollständige lokale Fakten vor.",
          categoryLabel = "PRÜFUNG",
          priority = if (validAppsScan == null && securitySnapshot == null) AdvicePriority.HIGH else AdvicePriority.MEDIUM,
          actionLabel = "Komplett-Check starten",
          action = PrivacyAdviceAction.RunCombinedCheck,
        )
    }

    if (patchSignal == PatchAgeSignal.REVIEW_RECOMMENDED) {
      items +=
        PrivacyAdviceItem(
          id = "review_security_patch",
          title = "Android- & Play-Systemupdate prüfen",
          detail =
            "Der lokale Sicherheitspatch (${securitySnapshot?.securityPatch ?: "unbekannt"}) liegt laut Geräteuhr mindestens 60 Tage zurück. Öffne die Sicherheitseinstellungen für Google Play-Systemupdates und Hersteller-OTA.",
          categoryLabel = "SYSTEM-PATCH",
          priority = AdvicePriority.HIGH,
          actionLabel = "Sicherheitseinstellungen",
          action = PrivacyAdviceAction.OpenSecuritySettings,
        )
    }

    if (multiCriticalApps.isNotEmpty()) {
      val topApp = multiCriticalApps.first()
      val exampleNames = multiCriticalApps.take(3).joinToString(", ") { it.appName }
      items +=
        PrivacyAdviceItem(
          id = "restrict_multi_critical_apps",
          title = "${multiCriticalApps.size} App(s) mit Mehrfach-Sensorzugriff einschränken",
          detail =
            "Apps wie $exampleNames besitzen gleichzeitig Zugriff auf zwei oder mehr kritische Sensoren (Kamera, Standort, Mikrofon). Stelle nicht benötigte Rechte auf „Nur bei Nutzung“ oder „Nicht zulassen“.",
          categoryLabel = "APP-RECHTE",
          priority = AdvicePriority.HIGH,
          actionLabel = "${topApp.appName} prüfen",
          action =
            PrivacyAdviceAction.OpenSpecificAppSettings(
              packageName = topApp.packageName,
              appName = topApp.appName,
            ),
        )
    }

    if (validAppsScan != null && validAppsScan.locationAccessCount > 0) {
      items +=
        PrivacyAdviceItem(
          id = "review_location_access",
          title = "Standortfreigaben auf Vordergrund begrenzen",
          detail =
            "${validAppsScan.locationAccessCount} sichtbare App(s) haben aktiven Standortzugriff. Prüfe insbesondere Hintergrund-Standort („Immer zulassen“) und genauen Standort.",
          categoryLabel = "STANDORT",
          priority = AdvicePriority.MEDIUM,
          actionLabel = "Apps & Rechte öffnen",
          action = PrivacyAdviceAction.OpenAppPermissions,
        )
    }

    if (validAppsScan != null && (validAppsScan.cameraAccessCount > 0 || validAppsScan.microphoneAccessCount > 0)) {
      items +=
        PrivacyAdviceItem(
          id = "review_camera_mic",
          title = "Kamera- & Mikrofonrechte kontrollieren",
          detail =
            "${validAppsScan.cameraAccessCount} App(s) dürfen die Kamera und ${validAppsScan.microphoneAccessCount} App(s) das Mikrofon nutzen. Entziehe Rechten bei Apps ohne aktiven Video-/Sprachzweck die Freigabe.",
          categoryLabel = "SENSOREN",
          priority = AdvicePriority.MEDIUM,
          actionLabel = "Sensor-Apps filtern",
          action = PrivacyAdviceAction.OpenAppPermissions,
        )
    }

    if (phoneSmsCount > 0 || contactsCount > 0) {
      items +=
        PrivacyAdviceItem(
          id = "review_comms_contacts",
          title = "Telefon-, SMS- & Kontaktzugriffe minimieren",
          detail =
            "$phoneSmsCount App(s) mit Telefon-/SMS-Zugriff und $contactsCount App(s) mit Kontakte-Zugriff erkannt. Prüfe, ob Adressbuch- und Anrufdaten wirklich benötigt werden.",
          categoryLabel = "DATENMINIMIERUNG",
          priority = AdvicePriority.LOW,
          actionLabel = "Berechtigungen prüfen",
          action = PrivacyAdviceAction.OpenAppPermissions,
        )
    }

    if (securitySnapshot != null && (securitySnapshot.webViewVersion == null || securitySnapshot.playServicesVersion == null)) {
      items +=
        PrivacyAdviceItem(
          id = "verify_google_components",
          title = "Google Play-Dienste & WebView-Provider kontrollieren",
          detail =
            "Mindestens eine Google-Systemkomponente war lokal nicht auslesbar (NOT_PROVEN). Prüfe den Status direkt im System- & Google-Check.",
          categoryLabel = "SYSTEMKOMPONENTEN",
          priority = AdvicePriority.MEDIUM,
          actionLabel = "System-Check öffnen",
          action = PrivacyAdviceAction.OpenSystemSecurityScreen,
        )
    }

    items +=
      PrivacyAdviceItem(
        id = "manual_play_protect",
        title = "Play Protect & Zertifizierung manuell bestätigen",
        detail =
          "Android bietet öffentlichen Apps keinen verlässlichen Lesezugriff auf den Play-Protect-Scanstatus (NOT_PROVEN). Starte im Play Store unter Profil → Play Protect einen manuellen Scan.",
        categoryLabel = "MANUELL · NOT_PROVEN",
        priority = AdvicePriority.INFO,
        actionLabel = "Play Store öffnen",
        action = PrivacyAdviceAction.OpenPlayProtectInStore,
      )

    if (aiReport == null) {
      items +=
        PrivacyAdviceItem(
          id = "optional_ai_review",
          title = "Optionale KI-Datenschutzberatung nutzen",
          detail =
            "Für maßgeschneiderte Handlungstipps ohne Paketnamen-Upload kannst du den lokalen Scan optional mit dem Gemini KI-Berater auswerten.",
          categoryLabel = "KI-BERATER (OPT-IN)",
          priority = AdvicePriority.INFO,
          actionLabel = "KI-Berater öffnen",
          action = PrivacyAdviceAction.OpenAiAdvisor,
        )
    }

    return items.sortedBy { it.priority.ordinal }
  }
}
