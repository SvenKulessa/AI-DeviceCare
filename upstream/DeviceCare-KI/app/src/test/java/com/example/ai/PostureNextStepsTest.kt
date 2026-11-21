package com.example.ai

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.devicecare.PrivacySummaryAggregator
import com.example.devicecare.PrivacySummaryReport
import com.example.devicecare.SystemSecuritySnapshot
import com.example.features.battery.BatteryDashboardSnapshot
import com.example.features.permissions.AppExportFormat
import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.SensitivityCategory
import com.example.ui.DeviceCareUiState
import com.example.ui.screens.PrivacySummaryScreen
import com.example.ui.theme.FormwerkTheme
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PostureNextStepsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val nowMillis = Instant.parse("2026-11-18T12:00:00Z").toEpochMilli()

  private val sampleScan =
    InstalledAppsScanResult(
      scannedAtMillis = nowMillis,
      visiblePackageCount = 3,
      apps =
        listOf(
          InstalledAppPermissionInfo(
            packageName = "com.example.secretcam",
            appName = "SecretCam",
            isSystemApp = false,
            totalRequestedPermissions = 3,
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
              ),
          )
        ),
    )

  private val sampleSecurity =
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

  private val sampleReport: PrivacySummaryReport =
    PrivacySummaryAggregator.aggregate(
      appsScanResult = sampleScan,
      securitySnapshot = sampleSecurity,
      inspectionResult = null,
    )

  @Test
  fun `buildDeviceStatePrompt summarizes posture without leaking package names`() {
    val prompt = GeminiPostureNextStepsService.buildDeviceStatePrompt(sampleReport)

    assertTrue(prompt.contains("Überprüfung empfohlen"))
    assertTrue(prompt.contains("Sichtbare Apps insgesamt: 3"))
    assertTrue(prompt.contains("Mehrfach kritische Apps (>=2 Sensoren kombiniert): 1"))
    assertTrue(prompt.contains("Sicherheitspatch: 2026-08-05"))
    assertTrue(prompt.contains("REVIEW_RECOMMENDED"))
    assertFalse(prompt.contains("com.example.secretcam"))
    assertFalse(prompt.contains("SecretCam"))
  }

  @Test
  fun `parseNextStepsJson sorts priorities renumbers ranks and rejects malformed payloads`() {
    val rawJson =
      """
      {
        "headline": "Zwei wichtige Sicherheitsprüfungen empfohlen",
        "postureOverview": "Dein Sicherheitspatch liegt über 60 Tage zurück und eine App nutzt mehrere kritische Sensoren gleichzeitig.",
        "nextSteps": [
          {
            "title": "Play Protect manuell anstoßen",
            "priority": "LOW",
            "plainLanguageSummary": "Öffne den Play Store und starte einmal manuell einen Play-Protect-Scan.",
            "estimatedEffort": "~1 Min.",
            "actionHint": "OPEN_PLAY_STORE"
          },
          {
            "title": "System- und Play-Updates prüfen",
            "priority": "CRITICAL",
            "plainLanguageSummary": "Öffne die Android-Sicherheitseinstellungen und prüfe auf ein neueres Play-Systemupdate oder Hersteller-Update.",
            "estimatedEffort": "2 Min.",
            "actionHint": "OPEN_SECURITY_SETTINGS"
          },
          {
            "title": "Sensor-Zugriffe einschränken",
            "priority": "HIGH",
            "plainLanguageSummary": "Reduziere Kamera- und Standortfreigaben auf 'Nur während der Nutzung'.",
            "estimatedEffort": "2 Min.",
            "actionHint": "OPEN_APP_PERMISSIONS"
          }
        ]
      }
      """.trimIndent()

    val parsed = GeminiPostureNextStepsService.parseNextStepsJson(rawJson, generatedAtMillis = 42L)
    assertEquals("Zwei wichtige Sicherheitsprüfungen empfohlen", parsed.headline)
    assertEquals(3, parsed.nextSteps.size)
    assertEquals(NextStepPriority.CRITICAL, parsed.nextSteps[0].priority)
    assertEquals(1, parsed.nextSteps[0].rank)
    assertEquals(NextStepActionHint.OPEN_SECURITY_SETTINGS, parsed.nextSteps[0].actionHint)
    assertEquals(NextStepPriority.HIGH, parsed.nextSteps[1].priority)
    assertEquals(2, parsed.nextSteps[1].rank)
    assertEquals(NextStepPriority.LOW, parsed.nextSteps[2].priority)
    assertEquals(3, parsed.nextSteps[2].rank)

    assertThrows(IllegalStateException::class.java) {
      GeminiPostureNextStepsService.parseNextStepsJson("{\"headline\":\"Leer\",\"postureOverview\":\"Ohne Schritte\",\"nextSteps\":[]}")
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `aiAdvisorViewModel updates postureNextStepsState across success error and clear`() = runTest {
    val fakeReport =
      PostureNextStepsReport(
        headline = "Gerätestatus geprüft",
        postureOverview = "Alle lokalen Basisprüfungen sind vorhanden; zwei manuelle Schritte werden empfohlen.",
        nextSteps =
          listOf(
            PostureNextStep(
              rank = 1,
              title = "Standortzugriffe überprüfen",
              priority = NextStepPriority.HIGH,
              plainLanguageSummary = "Prüfe, ob alle Apps dauerhaften Standortzugriff brauchen.",
              estimatedEffort = "~2 Min.",
              actionHint = NextStepActionHint.OPEN_APP_PERMISSIONS,
            )
          ),
      )

    val successVm =
      AiAdvisorViewModel(
        privacyAnalyzer =
          object : PermissionPrivacyAnalyzerService {
            override suspend fun analyzeGrantedPermissions(scanResult: InstalledAppsScanResult) =
              throw UnsupportedOperationException()
          },
        nextStepsService =
          object : PostureNextStepsService {
            override suspend fun analyzeDevicePosture(
              summaryReport: PrivacySummaryReport,
              batterySnapshot: BatteryDashboardSnapshot?,
            ): PostureNextStepsReport = fakeReport
          },
      )

    successVm.generatePostureNextSteps(sampleReport, null)
    composeTestRule.waitForIdle()
    val state = successVm.postureNextStepsState.value
    assertTrue(state is PostureNextStepsState.Success)
    assertEquals(fakeReport, (state as PostureNextStepsState.Success).report)

    successVm.clearPostureNextSteps()
    assertEquals(PostureNextStepsState.Idle, successVm.postureNextStepsState.value)

    val failingVm =
      AiAdvisorViewModel(
        privacyAnalyzer =
          object : PermissionPrivacyAnalyzerService {
            override suspend fun analyzeGrantedPermissions(scanResult: InstalledAppsScanResult) =
              throw UnsupportedOperationException()
          },
        nextStepsService =
          object : PostureNextStepsService {
            override suspend fun analyzeDevicePosture(
              summaryReport: PrivacySummaryReport,
              batterySnapshot: BatteryDashboardSnapshot?,
            ): PostureNextStepsReport {
              throw IllegalStateException("App Check attestation unavailable")
            }
          },
      )

    failingVm.generatePostureNextSteps(sampleReport, null)
    composeTestRule.waitForIdle()
    val errState = failingVm.postureNextStepsState.value
    assertTrue(errState is PostureNextStepsState.Error)
    assertEquals("App Check attestation unavailable", (errState as PostureNextStepsState.Error).message)
  }

  @Test
  fun `privacySummaryScreen renders gemini idle and loading states`() {
    var analyzeClicks = 0
    composeTestRule.setContent {
      FormwerkTheme {
        PrivacySummaryScreen(
          uiState =
            DeviceCareUiState(
              installedAppsScanResult = sampleScan,
              systemSecuritySnapshot = sampleSecurity,
            ),
          privacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
          postureNextStepsState = PostureNextStepsState.Idle,
          onBack = {},
          onRunCombinedCheck = {},
          onAnalyzePostureWithGemini = { analyzeClicks++ },
          onClearPostureNextSteps = {},
          onOpenAppPermissions = {},
          onOpenSystemSecurity = {},
          onOpenAiAdvisor = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("privacy_summary_list")
      .performScrollToNode(hasTestTag("privacy_summary_gemini_analyze_button"))
    composeTestRule.onNodeWithTag("privacy_summary_gemini_analyze_button").performScrollTo().performClick()
    assertEquals(1, analyzeClicks)
  }

  @Test
  fun `privacySummaryScreen renders error retry and prioritized next steps success state`() {
    var retryClicks = 0
    var openAppsClicks = 0
    val errorUiState =
      DeviceCareUiState(
        installedAppsScanResult = sampleScan,
        systemSecuritySnapshot = sampleSecurity,
      )

    composeTestRule.setContent {
      FormwerkTheme {
        PrivacySummaryScreen(
          uiState = errorUiState,
          privacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
          postureNextStepsState = PostureNextStepsState.Error("Netzwerkfehler beim Gemini-Aufruf"),
          onBack = {},
          onRunCombinedCheck = {},
          onAnalyzePostureWithGemini = { retryClicks++ },
          onClearPostureNextSteps = {},
          onOpenAppPermissions = { openAppsClicks++ },
          onOpenSystemSecurity = {},
          onOpenAiAdvisor = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("privacy_summary_list")
      .performScrollToNode(hasTestTag("privacy_summary_gemini_error"))
    composeTestRule.onNodeWithText("Netzwerkfehler beim Gemini-Aufruf").assertExists()
    composeTestRule.onNodeWithTag("privacy_summary_gemini_retry_button").performScrollTo().performClick()
    assertEquals(1, retryClicks)
  }

  @Test
  fun `privacySummaryScreen renders prioritized plain language next steps and navigates on action`() {
    var openAppsClicks = 0
    val nextStepsReport =
      PostureNextStepsReport(
        headline = "Fokus auf App-Berechtigungen legen",
        postureOverview = "Eine App greift gleichzeitig auf Kamera und Standort zu.",
        nextSteps =
          listOf(
            PostureNextStep(
              rank = 1,
              title = "Sensor-Freigaben prüfen",
              priority = NextStepPriority.HIGH,
              plainLanguageSummary = "Begrenze Kamera und Standort auf 'Nur während der Nutzung'.",
              estimatedEffort = "~2 Min.",
              actionHint = NextStepActionHint.OPEN_APP_PERMISSIONS,
            )
          ),
      )

    composeTestRule.setContent {
      FormwerkTheme {
        PrivacySummaryScreen(
          uiState =
            DeviceCareUiState(
              installedAppsScanResult = sampleScan,
              systemSecuritySnapshot = sampleSecurity,
            ),
          privacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
          postureNextStepsState = PostureNextStepsState.Success(nextStepsReport),
          onBack = {},
          onRunCombinedCheck = {},
          onAnalyzePostureWithGemini = {},
          onClearPostureNextSteps = {},
          onOpenAppPermissions = { openAppsClicks++ },
          onOpenSystemSecurity = {},
          onOpenAiAdvisor = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("privacy_summary_list")
      .performScrollToNode(hasTestTag("privacy_summary_gemini_results"))
    composeTestRule.onNodeWithText("Fokus auf App-Berechtigungen legen").assertExists()
    composeTestRule
      .onNodeWithTag("privacy_summary_list")
      .performScrollToNode(hasTestTag("privacy_summary_next_step_action_0"))
    composeTestRule
      .onNodeWithTag("privacy_summary_next_step_action_0")
      .performScrollTo()
      .performClick()
    assertEquals(1, openAppsClicks)
  }

  @Test
  fun `privacySummaryScreen export button requests local scan when missing and shares file when scanned`() {
    var scanClicks = 0
    var sharedFormat: AppExportFormat? = null
    var sharedIncludeSystem: Boolean? = null
    val uiStateHolder =
      androidx.compose.runtime.mutableStateOf(DeviceCareUiState(installedAppsScanResult = null))

    composeTestRule.setContent {
      FormwerkTheme {
        PrivacySummaryScreen(
          uiState = uiStateHolder.value,
          privacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
          postureNextStepsState = PostureNextStepsState.Idle,
          onBack = {},
          onRunCombinedCheck = {},
          onOpenAppPermissions = {},
          onOpenSystemSecurity = {},
          onOpenAiAdvisor = {},
          onShowSnackbar = {},
          onScanInstalledApps = { scanClicks++ },
          onShareExportRequested = { format, includeSystem ->
            sharedFormat = format
            sharedIncludeSystem = includeSystem
          },
        )
      }
    }

    composeTestRule
      .onNodeWithTag("privacy_summary_list")
      .performScrollToNode(hasTestTag("privacy_summary_export_apps_button"))
    composeTestRule
      .onNodeWithTag("privacy_summary_export_apps_button")
      .performScrollTo()
      .performClick()

    composeTestRule.onNodeWithTag("privacy_summary_export_dialog").assertExists()
    composeTestRule
      .onNodeWithTag("privacy_summary_scan_for_export_button")
      .performScrollTo()
      .performClick()
    assertEquals(1, scanClicks)

    // Update UI state to simulate completed local scan with user + system apps
    val systemApp =
      InstalledAppPermissionInfo(
        packageName = "com.android.systemui",
        appName = "System-UI",
        isSystemApp = true,
        totalRequestedPermissions = 4,
        grantedDangerousPermissions = emptyList(),
      )
    uiStateHolder.value =
      DeviceCareUiState(
        installedAppsScanResult =
          InstalledAppsScanResult(
            scannedAtMillis = 12_000L,
            visiblePackageCount = 2,
            apps = sampleScan.apps + systemApp,
          )
      )
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("privacy_summary_share_csv_button")
      .performScrollTo()
      .performClick()
    assertEquals(AppExportFormat.CSV, sharedFormat)
    assertEquals(true, sharedIncludeSystem)
  }
}
