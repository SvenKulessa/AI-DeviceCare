package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.NextStepActionHint
import com.example.ai.NextStepPriority
import com.example.ai.PermissionPrivacyAnalysisState
import com.example.ai.PostureNextStep
import com.example.ai.PostureNextStepsState
import com.example.devicecare.AdvicePriority
import com.example.devicecare.PatchAgeSignal
import com.example.devicecare.PrivacyAdviceAction
import com.example.devicecare.PrivacyAdviceItem
import com.example.devicecare.PrivacyPostureTier
import com.example.devicecare.PrivacySummaryAggregator
import com.example.devicecare.PrivacySummaryReport
import com.example.devicecare.SensorExposureMetric
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.openDeviceSettings
import com.example.devicecare.ownAppSettings
import com.example.features.permissions.AppExportFormat
import com.example.features.permissions.InstalledAppsExporter
import com.example.features.permissions.InstalledAppsScanResult
import com.example.ui.DeviceCareUiState
import com.example.ui.components.MetricRingCard
import com.example.ui.theme.MonoTokenTextStyle
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySummaryScreen(
  uiState: DeviceCareUiState,
  privacyAnalysisState: PermissionPrivacyAnalysisState,
  postureNextStepsState: PostureNextStepsState = PostureNextStepsState.Idle,
  onBack: () -> Unit,
  onRunCombinedCheck: () -> Unit,
  onAnalyzePostureWithGemini: (PrivacySummaryReport) -> Unit = {},
  onClearPostureNextSteps: () -> Unit = {},
  onOpenAppPermissions: () -> Unit,
  onOpenSystemSecurity: () -> Unit,
  onOpenAiAdvisor: () -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
  onScanInstalledApps: () -> Unit = onRunCombinedCheck,
  onShareExportRequested: ((AppExportFormat, Boolean) -> Unit)? = null,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val compact = uiState.preferences.compactDensity
  val spacing = if (compact) 12.dp else 16.dp
  val isRunning =
    uiState.installedAppsScanning || uiState.systemSecurityChecking || uiState.inspectionInProgress
  var showExportDialog by remember { mutableStateOf(false) }
  var exportIncludeSystemApps by remember { mutableStateOf(true) }
  var isPreparingShareFile by remember { mutableStateOf(false) }
  val chooserTitle = stringResource(R.string.export_apps_chooser_title)

  val report =
    remember(
      uiState.installedAppsScanResult,
      uiState.systemSecuritySnapshot,
      uiState.inspectionResult,
      privacyAnalysisState,
    ) {
      PrivacySummaryAggregator.aggregate(
        appsScanResult = uiState.installedAppsScanResult,
        securitySnapshot = uiState.systemSecuritySnapshot,
        inspectionResult = uiState.inspectionResult,
        privacyAnalysisState = privacyAnalysisState,
      )
    }

  fun launchExternalSettings(intent: Intent, fallbackHint: String) {
    when (openDeviceSettings(intent) { context.startActivity(it) }) {
      SettingsLaunchResult.DIRECT -> Unit
      SettingsLaunchResult.GENERAL_SETTINGS ->
        onShowSnackbar("Zielseite nicht direkt verfügbar. $fallbackHint")
      SettingsLaunchResult.UNAVAILABLE ->
        onShowSnackbar("Einstellungen konnten nicht geöffnet werden. $fallbackHint")
    }
  }

  fun handleAdviceAction(action: PrivacyAdviceAction) {
    when (action) {
      PrivacyAdviceAction.RunCombinedCheck -> onRunCombinedCheck()
      PrivacyAdviceAction.OpenAppPermissions -> onOpenAppPermissions()
      is PrivacyAdviceAction.OpenSpecificAppSettings ->
        launchExternalSettings(
          ownAppSettings(action.packageName),
          "Öffne Einstellungen → Apps → ${action.appName} → Berechtigungen.",
        )
      PrivacyAdviceAction.OpenSecuritySettings ->
        launchExternalSettings(
          Intent(Settings.ACTION_SECURITY_SETTINGS),
          "Öffne Sicherheit → Google Play-Systemupdate & Hersteller-OTA.",
        )
      PrivacyAdviceAction.OpenSystemSecurityScreen -> onOpenSystemSecurity()
      PrivacyAdviceAction.OpenPlayProtectInStore -> {
        val intent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
        if (intent == null) {
          onShowSnackbar("Play Store nicht verfügbar. Play Protect manuell im Store öffnen.")
        } else {
          launchExternalSettings(intent, "Play Store → Profil → Play Protect → Scannen.")
        }
      }
      PrivacyAdviceAction.OpenAiAdvisor -> onOpenAiAdvisor()
    }
  }

  fun handleNextStepHint(hint: NextStepActionHint) {
    when (hint) {
      NextStepActionHint.OPEN_APP_PERMISSIONS -> onOpenAppPermissions()
      NextStepActionHint.OPEN_SECURITY_SETTINGS ->
        launchExternalSettings(
          Intent(Settings.ACTION_SECURITY_SETTINGS),
          "Öffne Sicherheit → Google Play-Systemupdate & Hersteller-OTA.",
        )
      NextStepActionHint.OPEN_PLAY_STORE -> {
        val intent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
        if (intent == null) {
          onShowSnackbar("Play Store nicht verfügbar. Play Protect manuell im Store öffnen.")
        } else {
          launchExternalSettings(intent, "Play Store → Profil → Play Protect → Scannen.")
        }
      }
      NextStepActionHint.OPEN_SYSTEM_SECURITY -> onOpenSystemSecurity()
      NextStepActionHint.RUN_LOCAL_CHECKS -> onRunCombinedCheck()
      NextStepActionHint.NONE -> Unit
    }
  }

  fun shareGeneratedAppList(format: AppExportFormat) {
    val includeSystem = exportIncludeSystemApps
    val activeScan = uiState.installedAppsScanResult
    showExportDialog = false
    if (onShareExportRequested != null) {
      onShareExportRequested(format, includeSystem)
      return
    }
    if (activeScan == null || activeScan.errorMessage != null || activeScan.apps.isEmpty()) {
      onShowSnackbar("Keine gescannten Apps zum Teilen vorhanden.")
      return
    }
    val exportedCount =
      InstalledAppsExporter.summarize(activeScan, includeSystem).exportAppsCount
    isPreparingShareFile = true
    coroutineScope.launch {
      val fileResult =
        InstalledAppsExporter.writeTempExportFile(
          context = context,
          scanResult = activeScan,
          format = format,
          includeSystemApps = includeSystem,
        )
      isPreparingShareFile = false
      fileResult.fold(
        onSuccess = { file ->
          val shareIntent =
            InstalledAppsExporter.buildShareFileIntent(
              context = context,
              file = file,
              format = format,
              exportedCount = exportedCount,
              includeSystemApps = includeSystem,
            )
          val launched =
            InstalledAppsExporter.launchShareFileChooser(
              context = context,
              shareIntent = shareIntent,
              chooserTitle = chooserTitle,
            )
          if (!launched) {
            onShowSnackbar("Keine App zum Teilen von Dateien gefunden.")
          }
        },
        onFailure = { err ->
          onShowSnackbar(
            "Exportdatei konnte nicht erzeugt werden: ${err.localizedMessage ?: "Unbekannter Fehler"}"
          )
        },
      )
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("privacy_summary_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.privacy_summary_title),
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = stringResource(R.string.privacy_summary_subtitle),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("privacy_summary_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
        actions = {
          IconButton(
            onClick = onRunCombinedCheck,
            enabled = !isRunning,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("privacy_summary_refresh_icon"),
          ) {
            if (isRunning) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.privacy_summary_run_all),
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      val isWide = maxWidth >= 640.dp

      LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("privacy_summary_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            PrivacyPostureHeroCard(
              report = report,
              isRunning = isRunning,
              onRunCombinedCheck = onRunCombinedCheck,
            )
          }
        }

        if (uiState.systemSecurityErrorMessage != null ||
          uiState.installedAppsScanResult?.errorMessage != null
        ) {
          item {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
              val errorText =
                listOfNotNull(
                    uiState.installedAppsScanResult?.errorMessage,
                    uiState.systemSecurityErrorMessage,
                  )
                  .joinToString(" ")
              Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                  )
                  Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }
          }
        }

        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            PrivacyMetricsGrid(report = report, isWide = isWide)
          }
        }

        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            GeminiPostureNextStepsCard(
              state = postureNextStepsState,
              report = report,
              onAnalyze = { onAnalyzePostureWithGemini(report) },
              onClear = onClearPostureNextSteps,
              onTriggerNextStepHint = ::handleNextStepHint,
            )
          }
        }

        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            if (isWide) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                PermissionsAggregationCard(
                  report = report,
                  onOpenAppPermissions = onOpenAppPermissions,
                  onOpenExportDialog = { showExportDialog = true },
                  isExportBusy = isPreparingShareFile,
                  modifier = Modifier.weight(1f),
                )
                SecurityCheckAggregationCard(
                  report = report,
                  collectedAtMillis = uiState.systemSecuritySnapshot?.collectedAtMillis,
                  onOpenSystemSecurity = onOpenSystemSecurity,
                  modifier = Modifier.weight(1f),
                )
              }
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                PermissionsAggregationCard(
                  report = report,
                  onOpenAppPermissions = onOpenAppPermissions,
                  onOpenExportDialog = { showExportDialog = true },
                  isExportBusy = isPreparingShareFile,
                )
                SecurityCheckAggregationCard(
                  report = report,
                  collectedAtMillis = uiState.systemSecuritySnapshot?.collectedAtMillis,
                  onOpenSystemSecurity = onOpenSystemSecurity,
                )
              }
            }
          }
        }

        if (report.aiPrivacyReport != null) {
          item {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
              GeminiInsightSummaryCard(
                summary = report.aiPrivacyReport.summary,
                suggestionCount = report.aiPrivacyReport.suggestions.size,
                analyzedAppCount = report.aiPrivacyReport.analyzedAppCount,
                onOpenAppPermissions = onOpenAppPermissions,
              )
            }
          }
        }

        item {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .widthIn(max = 880.dp)
                .testTag("privacy_summary_advice_list"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "Konkrete Sicherheitsempfehlungen (${report.adviceItems.size})",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text =
                "Priorisierte Maßnahmen auf Basis lokal beobachteter Rechte und System-Metadaten — ohne automatische Eingriffe.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        items(report.adviceItems, key = { it.id }) { advice ->
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            PrivacyAdviceCard(
              advice = advice,
              compact = compact,
              onTriggerAction = { handleAdviceAction(advice.action) },
            )
          }
        }

        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            Surface(
              shape = MaterialTheme.shapes.large,
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text =
                    "Datenschutzprinzip: Alle Zusammenfassungen entstehen ausschließlich lokal im Arbeitsspeicher. " +
                      "Kein Hintergrund-Scan, kein automatischer Cloud-Upload und keine erfundenen Risiko-Scores.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
      }
    }
  }

  if (showExportDialog) {
    PrivacySummaryExportDialog(
      scanResult = uiState.installedAppsScanResult,
      scanning = uiState.installedAppsScanning,
      includeSystemApps = exportIncludeSystemApps,
      onToggleIncludeSystemApps = { exportIncludeSystemApps = it },
      onScanAppsWithConsent = onScanInstalledApps,
      onShareFormat = ::shareGeneratedAppList,
      onDismiss = { showExportDialog = false },
    )
  }
}

@Composable
private fun PrivacyPostureHeroCard(
  report: PrivacySummaryReport,
  isRunning: Boolean,
  onRunCombinedCheck: () -> Unit,
) {
  val (badgeBg, badgeFg) =
    when (report.postureTier) {
      PrivacyPostureTier.CHECKED_BASELINE ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
      PrivacyPostureTier.REVIEW_RECOMMENDED ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
      PrivacyPostureTier.PARTIAL ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
      PrivacyPostureTier.UNASSESSED ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

  val animatedCoverage by
    animateFloatAsState(
      targetValue = report.coverageFraction.coerceIn(0f, 1f),
      animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
      label = "privacy_coverage_ring",
    )
  val ringColor =
    if (report.postureTier == PrivacyPostureTier.REVIEW_RECOMMENDED) {
      MaterialTheme.colorScheme.error
    } else {
      MaterialTheme.colorScheme.primary
    }
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("privacy_summary_hero_card")
        .semantics { liveRegion = LiveRegionMode.Polite },
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                  MaterialTheme.colorScheme.surfaceContainer,
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                )
            )
          )
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(shape = MaterialTheme.shapes.small, color = badgeBg) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Shield,
                  contentDescription = null,
                  tint = badgeFg,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = report.postureTier.badge,
                  style = MonoTokenTextStyle,
                  color = badgeFg,
                )
              }
            }
            Text(
              text = report.postureTier.title,
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text = report.postureTier.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val stroke = 8.dp.toPx()
              drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
              )
              drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedCoverage,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${report.checksCompletedCount}/${report.checksTotalCount}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Checks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Button(
          onClick = onRunCombinedCheck,
          enabled = !isRunning,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("privacy_summary_run_all_button"),
        ) {
          if (isRunning) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.privacy_summary_running))
          } else {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.privacy_summary_run_all))
          }
        }
      }
    }
  }
}

@Composable
private fun PrivacyMetricsGrid(report: PrivacySummaryReport, isWide: Boolean) {
  val criticalAppsProgress =
    if (report.visibleAppsCount > 0) {
      report.appsWithCriticalSensorsCount.toFloat() / report.visibleAppsCount.toFloat()
    } else {
      0f
    }
  val securityProgress =
    when (report.patchAgeSignal) {
      PatchAgeSignal.NO_AGE_WARNING -> 1f
      PatchAgeSignal.REVIEW_RECOMMENDED -> 0.45f
      PatchAgeSignal.NOT_PROVEN -> if (report.hasSecurityCheck) 0.5f else 0f
    }

  val sensorSubtitle =
    if (report.hasPermissionScan) {
      "${report.multiCriticalApps.size} mehrfach kritisch"
    } else {
      "Scan ausstehend"
    }
  val securityCenter =
    when (report.patchAgeSignal) {
      PatchAgeSignal.NO_AGE_WARNING -> "OK"
      PatchAgeSignal.REVIEW_RECOMMENDED -> "ALT"
      PatchAgeSignal.NOT_PROVEN -> "—"
    }

  Column(
    modifier = Modifier.fillMaxWidth().testTag("privacy_summary_metrics_grid"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (isWide) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        MetricRingCard(
          title = "Prüfabdeckung",
          progress = report.coverageFraction,
          centerText = "${report.checksCompletedCount}/${report.checksTotalCount}",
          subtitle = "Lokale Module",
          modifier = Modifier.weight(1f),
        )
        MetricRingCard(
          title = "Kritische Sensor-Apps",
          progress = criticalAppsProgress,
          centerText = if (report.hasPermissionScan) "${report.appsWithCriticalSensorsCount}" else "—",
          subtitle = sensorSubtitle,
          modifier = Modifier.weight(1f),
        )
        MetricRingCard(
          title = "Patch-Signal",
          progress = securityProgress,
          centerText = securityCenter,
          subtitle = report.securitySnapshotStatusLabel,
          modifier = Modifier.weight(1f),
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        MetricRingCard(
          title = "Prüfabdeckung",
          progress = report.coverageFraction,
          centerText = "${report.checksCompletedCount}/${report.checksTotalCount}",
          subtitle = "Lokale Module",
          modifier = Modifier.weight(1f),
        )
        MetricRingCard(
          title = "Sensor-Apps",
          progress = criticalAppsProgress,
          centerText = if (report.hasPermissionScan) "${report.appsWithCriticalSensorsCount}" else "—",
          subtitle = sensorSubtitle,
          modifier = Modifier.weight(1f),
        )
      }
      MetricRingCard(
        title = "System- & Patch-Status",
        progress = securityProgress,
        centerText = securityCenter,
        subtitle = report.securitySnapshotStatusLabel,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionsAggregationCard(
  report: PrivacySummaryReport,
  onOpenAppPermissions: () -> Unit,
  onOpenExportDialog: () -> Unit,
  isExportBusy: Boolean,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("privacy_summary_permissions_card"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Apps,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "App-Berechtigungen",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )
      }

      if (!report.hasPermissionScan) {
        Text(
          text =
            "Noch kein App-Berechtigungsscan in dieser Sitzung gestartet (NOT_PROVEN). Tippe oben auf Komplett-Check, um gewährte Laufzeitrechte sichtbar zu machen.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          text =
            "${report.visibleAppsCount} sichtbare Apps geprüft · ${report.appsWithHighSensitivityCount} mit hochsensiblen Rechten · ${report.multiCriticalApps.size} mehrfach kritisch.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        report.sensorMetrics.forEach { metric ->
          SensorExposureBarRow(
            metric = metric,
            totalVisible = report.visibleAppsCount,
          )
        }

        AnimatedVisibility(
          visible = report.multiCriticalApps.isNotEmpty(),
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
              text = "Mehrfach kritische Apps",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.error,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              report.multiCriticalApps.take(4).forEach { app ->
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                ) {
                  Text(
                    text = "${app.appName} (${app.criticalSensorTags.joinToString("+")})",
                    style = MonoTokenTextStyle,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      FilledTonalButton(
        onClick = onOpenAppPermissions,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("privacy_summary_open_apps_button"),
      ) {
        Text(stringResource(R.string.privacy_summary_open_apps))
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }

      OutlinedButton(
        onClick = onOpenExportDialog,
        enabled = !isExportBusy,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("privacy_summary_export_apps_button"),
      ) {
        if (isExportBusy) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_export_app_list))
      }
    }
  }
}

@Composable
private fun PrivacySummaryExportDialog(
  scanResult: InstalledAppsScanResult?,
  scanning: Boolean,
  includeSystemApps: Boolean,
  onToggleIncludeSystemApps: (Boolean) -> Unit,
  onScanAppsWithConsent: () -> Unit,
  onShareFormat: (AppExportFormat) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("privacy_summary_export_dialog"),
    icon = {
      Icon(
        imageVector = Icons.Filled.Share,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = stringResource(R.string.export_apps_dialog_title),
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text =
            "Erzeugt lokal eine Export-Datei aller über Android <queries> sichtbaren installierten Apps (ohne QUERY_ALL_PACKAGES und ohne Speicherberechtigung) und öffnet den Android-Teilen-Dialog.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (scanResult == null || scanResult.errorMessage != null || scanResult.apps.isEmpty()) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text =
                  scanResult?.errorMessage
                    ?: "Es liegt noch kein lokaler App-Scan vor. Bestätige unten, um die OS-sichtbaren Pakete lokal auszulesen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Button(
                onClick = onScanAppsWithConsent,
                enabled = !scanning,
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("privacy_summary_scan_for_export_button"),
              ) {
                if (scanning) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Apps werden gelesen …")
                } else {
                  Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.export_apps_scan_first_button))
                }
              }
            }
          }
        } else {
          val summary =
            remember(scanResult, includeSystemApps) {
              InstalledAppsExporter.summarize(scanResult, includeSystemApps)
            }
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Export-Umfang: ${summary.exportAppsCount} Apps",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("privacy_summary_export_count_label"),
              )
              Text(
                text =
                  "${summary.userAppsCount} Nutzer-Apps · ${summary.systemAppsCount} System-Apps",
                style = MonoTokenTextStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable { onToggleIncludeSystemApps(!includeSystemApps) }
                .testTag("privacy_summary_export_system_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text(
                text = "System-Apps einschließen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "${summary.systemAppsCount} OS-Systempakete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = includeSystemApps,
              onCheckedChange = onToggleIncludeSystemApps,
              modifier = Modifier.testTag("privacy_summary_export_system_switch"),
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onShareFormat(AppExportFormat.CSV) },
              enabled = summary.exportAppsCount > 0,
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("privacy_summary_share_csv_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.export_apps_share_csv_button))
            }

            FilledTonalButton(
              onClick = { onShareFormat(AppExportFormat.JSON) },
              enabled = summary.exportAppsCount > 0,
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("privacy_summary_share_json_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.export_apps_share_json_button))
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("privacy_summary_export_cancel_button"),
      ) {
        Text(stringResource(R.string.export_apps_cancel))
      }
    },
  )
}

@Composable
private fun SensorExposureBarRow(
  metric: SensorExposureMetric,
  totalVisible: Int,
) {
  val fraction =
    if (totalVisible > 0) (metric.appCount.toFloat() / totalVisible.toFloat()).coerceIn(0f, 1f)
    else 0f
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = metric.label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "${metric.appCount} Apps",
        style = MonoTokenTextStyle,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    LinearProgressIndicator(
      progress = { fraction },
      modifier = Modifier.fillMaxWidth().height(6.dp),
      trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
  }
}

@Composable
private fun SecurityCheckAggregationCard(
  report: PrivacySummaryReport,
  collectedAtMillis: Long?,
  onOpenSystemSecurity: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("privacy_summary_security_card"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "System- & Google-Check",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )
      }

      if (!report.hasSecurityCheck) {
        Text(
          text =
            "Noch kein System- & Google-Sicherheitscheck ausgeführt (NOT_PROVEN). Er liest lokal Android-Version, Sicherheitspatch, Play-Dienste und WebView aus.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        if (collectedAtMillis != null) {
          val formatted =
            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN)
              .format(Date(collectedAtMillis))
          Text(
            text = "Letzte lokale Abfrage: $formatted · Keine CVE- oder Malware-Garantie.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        SecurityKeyValueRow("Betriebssystem", report.androidReleaseLabel ?: "NOT_PROVEN")
        SecurityKeyValueRow(
          "Sicherheitspatch",
          "${report.securityPatch ?: "nicht verfügbar"} · ${report.securitySnapshotStatusLabel}",
        )
        SecurityKeyValueRow(
          "Google Play-Dienste",
          report.playServicesVersion ?: "NOT_PROVEN (nicht auslesbar)",
        )
        SecurityKeyValueRow(
          "Google Play Store",
          report.playStoreVersion ?: "NOT_PROVEN (nicht auslesbar)",
        )
        SecurityKeyValueRow(
          "WebView-Provider",
          report.webViewVersion ?: "NOT_PROVEN (nicht auslesbar)",
        )
        SecurityKeyValueRow(
          "Play Protect / OEM-OTA",
          "NOT_PROVEN (nur manuell prüfbar)",
        )
      }

      FilledTonalButton(
        onClick = onOpenSystemSecurity,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("privacy_summary_open_security_button"),
      ) {
        Text(stringResource(R.string.privacy_summary_open_security))
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@Composable
private fun SecurityKeyValueRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.45f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = value,
      style = MonoTokenTextStyle,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(0.55f),
    )
  }
}

@Composable
private fun GeminiInsightSummaryCard(
  summary: String,
  suggestionCount: Int,
  analyzedAppCount: Int,
  onOpenAppPermissions: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
          text = "Gemini Datenschutz-Einordnung ($analyzedAppCount Apps · $suggestionCount Tipps)",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      OutlinedButton(
        onClick = onOpenAppPermissions,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("Details im Apps-Tab ansehen")
      }
    }
  }
}

@Composable
private fun PrivacyAdviceCard(
  advice: PrivacyAdviceItem,
  compact: Boolean,
  onTriggerAction: () -> Unit,
) {
  val (badgeBg, badgeFg, icon) =
    when (advice.priority) {
      AdvicePriority.HIGH ->
        Triple(
          MaterialTheme.colorScheme.errorContainer,
          MaterialTheme.colorScheme.onErrorContainer,
          Icons.Filled.Warning,
        )
      AdvicePriority.MEDIUM ->
        Triple(
          MaterialTheme.colorScheme.primaryContainer,
          MaterialTheme.colorScheme.onPrimaryContainer,
          Icons.Filled.VerifiedUser,
        )
      AdvicePriority.LOW ->
        Triple(
          MaterialTheme.colorScheme.secondaryContainer,
          MaterialTheme.colorScheme.onSecondaryContainer,
          Icons.Filled.CheckCircle,
        )
      AdvicePriority.INFO ->
        Triple(
          MaterialTheme.colorScheme.surfaceContainerHighest,
          MaterialTheme.colorScheme.onSurfaceVariant,
          Icons.Outlined.Info,
        )
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("privacy_advice_card_${advice.id}"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(if (compact) 14.dp else 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(shape = CircleShape, color = badgeBg) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = badgeFg,
              modifier = Modifier.padding(6.dp).size(16.dp),
            )
          }
          Surface(shape = MaterialTheme.shapes.small, color = badgeBg) {
            Text(
              text = "${advice.priority.label} · ${advice.categoryLabel}",
              style = MonoTokenTextStyle,
              color = badgeFg,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              maxLines = 1,
            )
          }
        }
      }

      Text(
        text = advice.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Text(
        text = advice.detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      FilledTonalButton(
        onClick = onTriggerAction,
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("privacy_advice_action_${advice.id}"),
      ) {
        Text(advice.actionLabel)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiPostureNextStepsCard(
  state: PostureNextStepsState,
  report: PrivacySummaryReport,
  onAnalyze: () -> Unit,
  onClear: () -> Unit,
  onTriggerNextStepHint: (NextStepActionHint) -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("privacy_summary_gemini_card")
        .semantics { liveRegion = LiveRegionMode.Polite },
    shape = MaterialTheme.shapes.extraLarge,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(7.dp).size(18.dp),
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = stringResource(R.string.privacy_summary_gemini_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text =
                "Basis: ${report.checksCompletedCount}/${report.checksTotalCount} lokale Checks (anonymisiert, ohne Paketnamen)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (state is PostureNextStepsState.Success || state is PostureNextStepsState.Error) {
          IconButton(
            onClick = onClear,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("privacy_summary_gemini_clear"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Gemini-Auswertung ausblenden",
            )
          }
        }
      }

      when (state) {
        PostureNextStepsState.Idle -> {
          Text(
            text =
              "Lass Gemini den aktuellen Gerätestatus aus Berechtigungen und Systemcheck in verständlicher Alltagssprache einordnen und priorisierte 'Nächste Schritte' erstellen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FilledTonalButton(
            onClick = onAnalyze,
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("privacy_summary_gemini_analyze_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.privacy_summary_gemini_analyze))
          }
        }

        PostureNextStepsState.Loading -> {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("privacy_summary_gemini_loading"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Gemini wertet Gerätestatus aus …",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Erstellt priorisierte Nächste Schritte in Alltagssprache.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        is PostureNextStepsState.Error -> {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("privacy_summary_gemini_error"),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
              FilledTonalButton(
                onClick = onAnalyze,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("privacy_summary_gemini_retry_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.privacy_summary_gemini_retry))
              }
            }
          }
        }

        is PostureNextStepsState.Success -> {
          Column(
            modifier = Modifier.fillMaxWidth().testTag("privacy_summary_gemini_results"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = state.report.headline,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = state.report.postureOverview,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            state.report.nextSteps.forEachIndexed { index, step ->
              NextStepCardItem(
                step = step,
                index = index,
                onTriggerHint = { onTriggerNextStepHint(step.actionHint) },
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              TextButton(
                onClick = onAnalyze,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("privacy_summary_gemini_reanalyze_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.privacy_summary_gemini_reanalyze))
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NextStepCardItem(
  step: PostureNextStep,
  index: Int,
  onTriggerHint: () -> Unit,
) {
  val (badgeBg, badgeFg) =
    when (step.priority) {
      NextStepPriority.CRITICAL ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
      NextStepPriority.HIGH ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
      NextStepPriority.MEDIUM ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
      NextStepPriority.LOW ->
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("privacy_summary_next_step_$index"),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(shape = MaterialTheme.shapes.small, color = badgeBg) {
          Text(
            text = "#${step.rank} · ${step.priority.label}",
            style = MonoTokenTextStyle,
            color = badgeFg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
          Text(
            text = step.estimatedEffort,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text = step.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Text(
        text = step.plainLanguageSummary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      val actionLabel = step.actionHint.buttonLabel
      if (actionLabel != null) {
        OutlinedButton(
          onClick = onTriggerHint,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("privacy_summary_next_step_action_$index"),
        ) {
          Text(actionLabel)
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

