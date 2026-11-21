package com.example

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.ai.PermissionPrivacyAnalysisState
import com.example.ai.PrivacyAnalysisReport
import com.example.ai.PrivacyRecommendationPriority
import com.example.ai.PrivacySuggestion
import com.example.features.permissions.AppExportFormat
import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.PermissionSensitivityClassifier
import com.example.features.permissions.SensitivityCategory
import com.example.ui.DeviceCareUiState
import com.example.ui.screens.AppPermissionsScreen
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PermissionPrivacyUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleApp =
    InstalledAppPermissionInfo(
      packageName = "com.example.scanner",
      appName = "Doc Scanner",
      isSystemApp = false,
      totalRequestedPermissions = 4,
      grantedDangerousPermissions =
        listOf(
          GrantedDangerousPermission(
            fullName = "android.permission.CAMERA",
            shortName = "CAMERA",
            category = SensitivityCategory.CAMERA,
            description = "Kamera verwenden",
          ),
          GrantedDangerousPermission(
            fullName = "android.permission.ACCESS_FINE_LOCATION",
            shortName = "ACCESS_FINE_LOCATION",
            category = SensitivityCategory.LOCATION,
            description = "Genauer Standort",
          ),
        ),
    )

  private val sampleSystemApp =
    InstalledAppPermissionInfo(
      packageName = "com.android.settings",
      appName = "Einstellungen",
      isSystemApp = true,
      totalRequestedPermissions = 6,
      grantedDangerousPermissions = emptyList(),
      versionName = "15",
    )

  private val sampleScan =
    InstalledAppsScanResult(
      scannedAtMillis = 12_000L,
      visiblePackageCount = 1,
      apps = listOf(sampleApp),
    )

  private val sampleUiState =
    DeviceCareUiState(
      installedAppsScanResult = sampleScan,
      filteredInstalledApps = listOf(sampleApp),
      categorizedAppSections = PermissionSensitivityClassifier.categorizeApps(listOf(sampleApp)),
    )

  @Test
  fun `idle state shows analyze button and invokes callback on click`() {
    var analyzeClicks = 0
    composeTestRule.setContent {
      FormwerkTheme {
        AppPermissionsScreen(
          uiState = sampleUiState,
          privacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
          onStartScan = {},
          onAnalyzePrivacy = { analyzeClicks++ },
          onClearPrivacyAnalysis = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("gemini_privacy_card"))
    composeTestRule.onNodeWithTag("gemini_privacy_card").assertExists()
    composeTestRule.onNodeWithTag("analyze_privacy_button").performClick()
    assertEquals(1, analyzeClicks)
  }

  @Test
  fun `loading and error states surface real status and retry button`() {
    composeTestRule.setContent {
      FormwerkTheme {
        AppPermissionsScreen(
          uiState = sampleUiState,
          privacyAnalysisState = PermissionPrivacyAnalysisState.Loading,
          onStartScan = {},
          onAnalyzePrivacy = {},
          onClearPrivacyAnalysis = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("privacy_analysis_loading"))
    composeTestRule.onNodeWithTag("privacy_analysis_loading").assertExists()
  }

  @Test
  fun `error state surfaces real error message and retry action`() {
    var retryClicks = 0
    composeTestRule.setContent {
      FormwerkTheme {
        AppPermissionsScreen(
          uiState = sampleUiState,
          privacyAnalysisState =
            PermissionPrivacyAnalysisState.Error("HTTP 403 App Check token missing"),
          onStartScan = {},
          onAnalyzePrivacy = { retryClicks++ },
          onClearPrivacyAnalysis = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("privacy_analysis_error"))
    composeTestRule.onNodeWithTag("privacy_analysis_error").assertExists()
    composeTestRule.onNodeWithText("HTTP 403 App Check token missing").assertExists()
    composeTestRule.onNodeWithTag("analyze_privacy_button").performClick()
    assertEquals(1, retryClicks)
  }

  @Test
  fun `success state renders natural language summary suggestions and affected app settings link`() {
    val report =
      PrivacyAnalysisReport(
        summary = "Doc Scanner kombiniert Kamera- und genauen Standortzugriff.",
        keyRisks = listOf("Genauer Standort für Dokumenten-Scan oft unnötig"),
        suggestions =
          listOf(
            PrivacySuggestion(
              title = "Standort für Doc Scanner entziehen",
              priority = PrivacyRecommendationPriority.HIGH,
              recommendation =
                "Öffne die App-Berechtigungen und stelle den Standort auf 'Nicht zulassen'.",
              targetPermission = "ACCESS_FINE_LOCATION",
              affectedApps = listOf("Doc Scanner"),
            )
          ),
        analyzedAppCount = 1,
        analyzedPermissionCount = 2,
      )

    composeTestRule.setContent {
      FormwerkTheme {
        AppPermissionsScreen(
          uiState = sampleUiState,
          privacyAnalysisState = PermissionPrivacyAnalysisState.Success(report),
          onStartScan = {},
          onAnalyzePrivacy = {},
          onClearPrivacyAnalysis = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("privacy_analysis_summary"))
    composeTestRule
      .onNodeWithTag("privacy_analysis_summary")
      .assertTextContains("Doc Scanner kombiniert Kamera- und genauen Standortzugriff.")
    composeTestRule
      .onNodeWithTag("installed_apps_grid")
      .performScrollToNode(hasTestTag("privacy_suggestion_0"))
    composeTestRule.onNodeWithTag("privacy_suggestion_0").assertExists()
    composeTestRule.onNodeWithText("Doc Scanner · Rechte").assertExists()
  }

  @Test
  fun `export dialog includes system apps by default even when hideSystemApps filter is active`() {
    val mixedScan =
      InstalledAppsScanResult(
        scannedAtMillis = 12_000L,
        visiblePackageCount = 2,
        apps = listOf(sampleApp, sampleSystemApp),
      )
    val filteredState =
      DeviceCareUiState(
        installedAppsScanResult = mixedScan,
        hideSystemApps = true,
        filteredInstalledApps = listOf(sampleApp),
        categorizedAppSections = PermissionSensitivityClassifier.categorizeApps(listOf(sampleApp)),
      )
    var capturedFormat: AppExportFormat? = null
    var capturedIncludeSystem: Boolean? = null

    composeTestRule.setContent {
      FormwerkTheme {
        AppPermissionsScreen(
          uiState = filteredState,
          privacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
          onStartScan = {},
          onSearchQueryChange = {},
          onSelectFilter = {},
          onToggleHideSystemApps = {},
          onToggleSectionCollapsed = {},
          onShowSnackbar = {},
          onExportRequested = { format, includeSystem ->
            capturedFormat = format
            capturedIncludeSystem = includeSystem
          },
        )
      }
    }

    composeTestRule.onNodeWithTag("export_apps_button").performClick()
    composeTestRule.onNodeWithTag("export_apps_dialog").assertExists()
    composeTestRule
      .onNodeWithTag("export_apps_count_label")
      .assertTextContains("Export-Umfang: 2 Apps")

    composeTestRule.onNodeWithTag("export_csv_button").performScrollTo().performClick()
    assertEquals(AppExportFormat.CSV, capturedFormat)
    assertEquals(true, capturedIncludeSystem)

    // Reopen dialog, turn off system apps, and verify count and JSON export flag
    composeTestRule.onNodeWithTag("export_apps_button").performClick()
    composeTestRule.onNodeWithTag("export_include_system_apps_switch").performScrollTo().performClick()
    composeTestRule
      .onNodeWithTag("export_apps_count_label")
      .assertTextContains("Export-Umfang: 1 Apps")
    composeTestRule.onNodeWithTag("export_json_button").performScrollTo().performClick()
    assertEquals(AppExportFormat.JSON, capturedFormat)
    assertEquals(false, capturedIncludeSystem)
  }
}
