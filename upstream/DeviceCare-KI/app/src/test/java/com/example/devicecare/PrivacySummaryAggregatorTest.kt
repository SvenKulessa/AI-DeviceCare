package com.example.devicecare

import com.example.ai.PermissionPrivacyAnalysisState
import com.example.ai.PrivacyAnalysisReport
import com.example.ai.PrivacyRecommendationPriority
import com.example.ai.PrivacySuggestion
import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.SensitivityCategory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySummaryAggregatorTest {
  private val nowMillis = Instant.parse("2026-11-18T12:00:00Z").toEpochMilli()

  @Test
  fun `empty inputs stay unassessed and recommend completing local checks`() {
    val report =
      PrivacySummaryAggregator.aggregate(
        appsScanResult = null,
        securitySnapshot = null,
        inspectionResult = null,
      )

    assertEquals(PrivacyPostureTier.UNASSESSED, report.postureTier)
    assertEquals(0, report.checksCompletedCount)
    assertEquals(3, report.checksTotalCount)
    assertFalse(report.hasPermissionScan)
    assertFalse(report.hasSecurityCheck)
    assertTrue(report.adviceItems.any { it.id == "run_missing_checks" && it.priority == AdvicePriority.HIGH })
    assertTrue(report.adviceItems.any { it.id == "manual_play_protect" })
  }

  @Test
  fun `stale security patch and multi critical apps trigger high priority advice`() {
    val appsScan =
      InstalledAppsScanResult(
        scannedAtMillis = nowMillis,
        visiblePackageCount = 4,
        apps =
          listOf(
            InstalledAppPermissionInfo(
              packageName = "com.example.social",
              appName = "Social Recorder",
              isSystemApp = false,
              totalRequestedPermissions = 5,
              grantedDangerousPermissions =
                listOf(
                  GrantedDangerousPermission(
                    fullName = "android.permission.CAMERA",
                    shortName = "CAMERA",
                    category = SensitivityCategory.CAMERA,
                    description = null,
                  ),
                  GrantedDangerousPermission(
                    fullName = "android.permission.ACCESS_FINE_LOCATION",
                    shortName = "ACCESS_FINE_LOCATION",
                    category = SensitivityCategory.LOCATION,
                    description = null,
                  ),
                  GrantedDangerousPermission(
                    fullName = "android.permission.RECORD_AUDIO",
                    shortName = "RECORD_AUDIO",
                    category = SensitivityCategory.MICROPHONE,
                    description = null,
                  ),
                ),
            ),
            InstalledAppPermissionInfo(
              packageName = "com.example.dialer",
              appName = "Dialer Plus",
              isSystemApp = false,
              totalRequestedPermissions = 2,
              grantedDangerousPermissions =
                listOf(
                  GrantedDangerousPermission(
                    fullName = "android.permission.READ_CONTACTS",
                    shortName = "READ_CONTACTS",
                    category = SensitivityCategory.CONTACTS,
                    description = null,
                  )
                ),
            ),
          ),
      )

    val security =
      SystemSecuritySnapshot(
        collectedAtMillis = nowMillis,
        androidRelease = "15",
        apiLevel = 35,
        securityPatch = "2026-08-05",
        playServicesVersion = "26.44.12",
        playStoreVersion = "54.0",
        webViewVersion = "158.0",
        webViewProvider = "com.google.android.webview",
      )

    val report =
      PrivacySummaryAggregator.aggregate(
        appsScanResult = appsScan,
        securitySnapshot = security,
        inspectionResult = InspectionResult(collectedAtMillis = nowMillis, apiLevel = 35),
      )

    assertEquals(PrivacyPostureTier.REVIEW_RECOMMENDED, report.postureTier)
    assertEquals(3, report.checksCompletedCount)
    assertEquals(1, report.multiCriticalApps.size)
    assertEquals("Social Recorder", report.multiCriticalApps.first().appName)
    assertEquals(PatchAgeSignal.REVIEW_RECOMMENDED, report.patchAgeSignal)
    assertTrue(
      report.adviceItems.any {
        it.id == "review_security_patch" && it.priority == AdvicePriority.HIGH
      }
    )
    assertTrue(
      report.adviceItems.any {
        it.id == "restrict_multi_critical_apps" &&
          it.priority == AdvicePriority.HIGH &&
          it.action is PrivacyAdviceAction.OpenSpecificAppSettings
      }
    )
    assertTrue(report.adviceItems.any { it.id == "review_comms_contacts" })
  }

  @Test
  fun `clean recent local baseline keeps play protect and ota as manual advice and surfaces gemini report`() {
    val cleanScan =
      InstalledAppsScanResult(
        scannedAtMillis = nowMillis,
        visiblePackageCount = 2,
        apps =
          listOf(
            InstalledAppPermissionInfo(
              packageName = "com.example.notes",
              appName = "Offline Notes",
              isSystemApp = false,
              totalRequestedPermissions = 0,
              grantedDangerousPermissions = emptyList(),
            )
          ),
      )
    val recentSecurity =
      SystemSecuritySnapshot(
        collectedAtMillis = nowMillis,
        androidRelease = "15",
        apiLevel = 35,
        securityPatch = "2026-11-05",
        playServicesVersion = "26.44.12",
        playStoreVersion = "54.0",
        webViewVersion = "158.0",
        webViewProvider = "com.google.android.webview",
      )
    val aiReport =
      PrivacyAnalysisReport(
        summary = "Keine kritischen Rechte erkannt.",
        keyRisks = emptyList(),
        suggestions =
          listOf(
            PrivacySuggestion(
              title = "Status beibehalten",
              priority = PrivacyRecommendationPriority.LOW,
              recommendation = "Weiterhin minimale Rechte vergeben.",
              targetPermission = "NONE",
              affectedApps = emptyList(),
            )
          ),
        analyzedAppCount = 1,
        analyzedPermissionCount = 0,
      )

    val report =
      PrivacySummaryAggregator.aggregate(
        appsScanResult = cleanScan,
        securitySnapshot = recentSecurity,
        inspectionResult = null,
        privacyAnalysisState = PermissionPrivacyAnalysisState.Success(aiReport),
      )

    assertEquals(PrivacyPostureTier.CHECKED_BASELINE, report.postureTier)
    assertEquals(2, report.checksCompletedCount)
    assertNotNull(report.aiPrivacyReport)
    assertFalse(report.adviceItems.any { it.id == "optional_ai_review" })
    assertTrue(report.adviceItems.any { it.id == "manual_play_protect" })
  }
}
