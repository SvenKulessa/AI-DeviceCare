package com.example

import android.content.ActivityNotFoundException
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiAdvisorViewModel
import com.example.ai.GeminiPermissionPrivacyAnalyzerService
import com.example.ai.PermissionPrivacyAnalysisState
import com.example.ai.PermissionPrivacyAnalyzerService
import com.example.ai.PrivacyAnalysisReport
import com.example.ai.PrivacyRecommendationPriority
import com.example.ai.PrivacySuggestion
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.appPermissionSettingsIntent
import com.example.devicecare.openDeviceSettings
import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.SensitivityCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI-DeviceCare", appName)
  }

  @Test
  fun `appPermissionSettingsIntent targets specific app details settings`() {
    val targetPkg = "com.example.cameraapp"
    val intent = appPermissionSettingsIntent(targetPkg)
    assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
    assertEquals("package:$targetPkg", intent.dataString)

    val directResult = openDeviceSettings(intent) { launched ->
      assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, launched.action)
    }
    assertEquals(SettingsLaunchResult.DIRECT, directResult)

    var fallbackAction: String? = null
    val fallbackResult =
      openDeviceSettings(intent) { launched ->
        if (launched.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
          throw ActivityNotFoundException("no handler")
        }
        fallbackAction = launched.action
      }
    assertEquals(SettingsLaunchResult.GENERAL_SETTINGS, fallbackResult)
    assertEquals(Settings.ACTION_SETTINGS, fallbackAction)
  }

  @Test
  fun `mainActivity starts and navigates across all tabs without crashing`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_inspection_card").assertExists()

    composeTestRule.onNodeWithTag("inspection_start_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_apps").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_permissions_screen").assertExists()
    composeTestRule.onNodeWithTag("scan_apps_permissions_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_roadmap").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_architecture").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_design").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `battery dashboard opens from inspection hero displays cards and navigates back`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("open_battery_monitor_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("battery_dashboard_screen").assertExists()
    composeTestRule.onNodeWithTag("battery_level_card").assertExists()
    composeTestRule.onNodeWithTag("remaining_time_card").assertExists()
    composeTestRule
      .onNodeWithTag("battery_dashboard_list")
      .performScrollToNode(hasTestTag("battery_health_telemetry_grid"))
    composeTestRule.onNodeWithTag("battery_health_telemetry_grid").assertExists()

    composeTestRule.onNodeWithTag("battery_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_inspection_card").assertExists()
  }

  @Test
  fun `ai advisor opens from inspection hero renders controls and navigates back`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("open_ai_advisor_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ai_advisor_screen").assertExists()
    composeTestRule.onNodeWithTag("ai_context_toggle").assertExists()
    composeTestRule.onNodeWithTag("ai_prompt_input").assertExists()
    composeTestRule.onNodeWithTag("ai_send_button").assertExists()

    composeTestRule.onNodeWithTag("ai_context_toggle").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ai_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_inspection_card").assertExists()
  }

  @Test
  fun `cloud sync screen opens from inspection hero shows google sign in gate and navigates back`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("open_cloud_sync_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("cloud_sync_screen").assertExists()
    composeTestRule.onNodeWithTag("cloud_auth_gate").assertExists()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()

    composeTestRule.onNodeWithTag("cloud_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hero_inspection_card").assertExists()
  }

  @Test
  fun `parsePrivacyAnalysisJson parses valid structured response and rejects invalid payload`() {
    val rawJson =
      """
      {
        "summary": "Zwei Apps haben kombinierten Zugriff auf Standort und Mikrofon.",
        "keyRisks": ["Hintergrund-Standort bei Social App", "Mikrofon-Zugriff nicht dauerhaft nötig"],
        "suggestions": [
          {
            "title": "Mikrofon-Zugriff einschränken",
            "priority": "LOW",
            "recommendation": "Auf 'Nur bei Nutzung' umstellen.",
            "targetPermission": "RECORD_AUDIO",
            "affectedApps": ["Voice Notes"]
          },
          {
            "title": "Kamera und Standort entziehen",
            "priority": "HIGH",
            "recommendation": "In den App-Einstellungen Kamera- und Standortrechte prüfen.",
            "targetPermission": "ACCESS_FINE_LOCATION",
            "affectedApps": ["Social Recorder"]
          }
        ]
      }
      """.trimIndent()

    val report =
      GeminiPermissionPrivacyAnalyzerService.parsePrivacyAnalysisJson(
        jsonString = rawJson,
        analyzedAppCount = 2,
        analyzedPermissionCount = 3,
      )
    assertEquals("Zwei Apps haben kombinierten Zugriff auf Standort und Mikrofon.", report.summary)
    assertEquals(2, report.keyRisks.size)
    assertEquals(2, report.suggestions.size)
    // Sorted HIGH before LOW
    assertEquals(PrivacyRecommendationPriority.HIGH, report.suggestions[0].priority)
    assertEquals("Social Recorder", report.suggestions[0].affectedApps.first())
    assertEquals(PrivacyRecommendationPriority.LOW, report.suggestions[1].priority)

    assertThrows(IllegalStateException::class.java) {
      GeminiPermissionPrivacyAnalyzerService.parsePrivacyAnalysisJson("{\"summary\":\"\"}", 1, 1)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `aiAdvisorViewModel updates privacyAnalysisState across success and error`() = runTest {
    val sampleScan =
      InstalledAppsScanResult(
        scannedAtMillis = 1_000L,
        visiblePackageCount = 1,
        apps =
          listOf(
            InstalledAppPermissionInfo(
              packageName = "com.example.cam",
              appName = "Cam",
              isSystemApp = false,
              totalRequestedPermissions = 1,
              grantedDangerousPermissions =
                listOf(
                  GrantedDangerousPermission(
                    "android.permission.CAMERA",
                    "CAMERA",
                    SensitivityCategory.CAMERA,
                    null,
                  )
                ),
            )
          ),
      )

    val expectedReport =
      PrivacyAnalysisReport(
        summary = "Eine Kamera-Berechtigung aktiv.",
        keyRisks = listOf("Kamera-Zugriff für Cam"),
        suggestions =
          listOf(
            PrivacySuggestion(
              title = "Kamera-Recht prüfen",
              priority = PrivacyRecommendationPriority.MEDIUM,
              recommendation = "Nur bei Bedarf erlauben.",
              targetPermission = "CAMERA",
              affectedApps = listOf("Cam"),
            )
          ),
        analyzedAppCount = 1,
        analyzedPermissionCount = 1,
      )

    val successVm =
      AiAdvisorViewModel(
        privacyAnalyzer =
          object : PermissionPrivacyAnalyzerService {
            override suspend fun analyzeGrantedPermissions(
              scanResult: InstalledAppsScanResult
            ): PrivacyAnalysisReport = expectedReport
          }
      )

    successVm.analyzePermissionsPrivacy(null)
    assertTrue(successVm.privacyAnalysisState.value is PermissionPrivacyAnalysisState.Error)

    successVm.clearPrivacyAnalysis()
    assertEquals(PermissionPrivacyAnalysisState.Idle, successVm.privacyAnalysisState.value)

    successVm.analyzePermissionsPrivacy(sampleScan)
    // Allow MainScope/viewModelScope coroutine on Robolectric main looper to settle
    composeTestRule.waitForIdle()
    val state = successVm.privacyAnalysisState.value
    assertTrue(state is PermissionPrivacyAnalysisState.Success)
    assertEquals(expectedReport, (state as PermissionPrivacyAnalysisState.Success).report)

    val failingVm =
      AiAdvisorViewModel(
        privacyAnalyzer =
          object : PermissionPrivacyAnalyzerService {
            override suspend fun analyzeGrantedPermissions(
              scanResult: InstalledAppsScanResult
            ): PrivacyAnalysisReport {
              throw IllegalStateException("App Check failed")
            }
          }
      )
    failingVm.analyzePermissionsPrivacy(sampleScan)
    composeTestRule.waitForIdle()
    val errState = failingVm.privacyAnalysisState.value
    assertTrue(errState is PermissionPrivacyAnalysisState.Error)
    assertEquals("App Check failed", (errState as PermissionPrivacyAnalysisState.Error).message)
  }
}
