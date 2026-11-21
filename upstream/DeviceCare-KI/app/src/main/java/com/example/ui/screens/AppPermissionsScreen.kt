package com.example.ui.screens

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ai.PermissionPrivacyAnalysisState
import com.example.ai.PrivacyAnalysisReport
import com.example.ai.PrivacyRecommendationPriority
import com.example.ai.PrivacySuggestion
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.appPermissionSettingsIntent
import com.example.devicecare.openDeviceSettings
import com.example.features.permissions.AppExportFormat
import com.example.features.permissions.AppPermissionFilter
import com.example.features.permissions.AppSectionCategory
import com.example.features.permissions.CategorizedAppGroup
import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsExporter
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.SensitivityCategory
import com.example.ui.DeviceCareUiState
import com.example.ui.components.MetricRingCard
import com.example.ui.theme.MonoTokenTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionsScreen(
  uiState: DeviceCareUiState,
  onStartScan: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSelectFilter: (AppPermissionFilter) -> Unit,
  onToggleHideSystemApps: () -> Unit,
  onToggleSectionCollapsed: (AppSectionCategory) -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
  privacyAnalysisState: PermissionPrivacyAnalysisState = PermissionPrivacyAnalysisState.Idle,
  onAnalyzePrivacy: () -> Unit = {},
  onClearPrivacyAnalysis: () -> Unit = {},
  onExportRequested: ((AppExportFormat, Boolean) -> Unit)? = null,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val compact = uiState.preferences.compactDensity
  var selectedApp by remember { mutableStateOf<InstalledAppPermissionInfo?>(null) }
  var pendingReturnAppName by remember { mutableStateOf<String?>(null) }
  var showExportDialog by remember { mutableStateOf(false) }
  var exportIncludeSystemApps by remember { mutableStateOf(true) }
  var isWritingExport by remember { mutableStateOf(false) }
  val currentOnStartScan by rememberUpdatedState(onStartScan)
  val currentOnShowSnackbar by rememberUpdatedState(onShowSnackbar)

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME && pendingReturnAppName != null) {
        val appName = pendingReturnAppName
        pendingReturnAppName = null
        currentOnStartScan()
        currentOnShowSnackbar("Berechtigungen für $appName aktualisiert.")
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  fun openSystemAppInfo(app: InstalledAppPermissionInfo) {
    when (
      openDeviceSettings(appPermissionSettingsIntent(app.packageName)) {
        context.startActivity(it)
      }
    ) {
      SettingsLaunchResult.DIRECT -> {
        pendingReturnAppName = app.appName
      }
      SettingsLaunchResult.GENERAL_SETTINGS ->
        onShowSnackbar("App-Infoseite nicht direkt verfügbar. Öffne Apps in den Einstellungen.")
      SettingsLaunchResult.UNAVAILABLE ->
        onShowSnackbar("Android-Einstellungen konnten nicht geöffnet werden.")
    }
  }

  val scanResult = uiState.installedAppsScanResult

  fun writeExportToDestination(uri: Uri?, format: AppExportFormat) {
    val activeScan = scanResult ?: return
    if (uri == null) {
      currentOnShowSnackbar("Export abgebrochen.")
      return
    }
    val includeSystem = exportIncludeSystemApps
    val exportedAppsCount =
      InstalledAppsExporter.summarize(activeScan, includeSystem).exportAppsCount
    val payload =
      InstalledAppsExporter.export(
        scanResult = activeScan,
        format = format,
        includeSystemApps = includeSystem,
      )
    isWritingExport = true
    coroutineScope.launch {
      val result = InstalledAppsExporter.writeTextToUri(context, uri, payload)
      isWritingExport = false
      result.fold(
        onSuccess = {
          val scopeHint = if (includeSystem) "inkl. System-Apps" else "ohne System-Apps"
          currentOnShowSnackbar(
            "$exportedAppsCount Apps ($scopeHint) als ${format.label} exportiert."
          )
        },
        onFailure = { error ->
          currentOnShowSnackbar(
            "Export fehlgeschlagen: ${error.localizedMessage ?: "Unbekannter Fehler"}"
          )
        },
      )
    }
  }

  val createCsvLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      writeExportToDestination(uri, AppExportFormat.CSV)
    }
  val createJsonLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
      writeExportToDestination(uri, AppExportFormat.JSON)
    }

  fun launchExport(format: AppExportFormat) {
    val includeSystem = exportIncludeSystemApps
    showExportDialog = false
    if (onExportRequested != null) {
      onExportRequested(format, includeSystem)
      return
    }
    val suggestedName = InstalledAppsExporter.suggestedFileName(format)
    when (format) {
      AppExportFormat.CSV -> createCsvLauncher.launch(suggestedName)
      AppExportFormat.JSON -> createJsonLauncher.launch(suggestedName)
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns = if (maxWidth >= 680.dp) 2 else 1

    Column(
      modifier = Modifier.fillMaxSize().testTag("app_permissions_screen"),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Header + Search + Filters
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 920.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "App-Berechtigungen",
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text = "Installierte (OS-sichtbare) Apps & gewährte Laufzeitrechte.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (scanResult != null) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (scanResult.errorMessage == null && scanResult.apps.isNotEmpty()) {
                FilledTonalIconButton(
                  onClick = { showExportDialog = true },
                  enabled = !isWritingExport,
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("export_apps_button"),
                ) {
                  if (isWritingExport) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                  } else {
                    Icon(
                      imageVector = Icons.Filled.Download,
                      contentDescription = stringResource(R.string.action_export_app_list),
                    )
                  }
                }
              }

              FilledTonalIconButton(
                onClick = onStartScan,
                enabled = !uiState.installedAppsScanning,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("rescan_apps_permissions_button"),
              ) {
                if (uiState.installedAppsScanning) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                  Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Apps erneut scannen",
                  )
                }
              }
            }
          }
        }

        if (scanResult != null && scanResult.errorMessage == null) {
          OutlinedTextField(
            value = uiState.appSearchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().testTag("apps_search_input"),
            placeholder = { Text("App, Paket oder Recht (z. B. Kamera, Standort)...") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
              if (uiState.appSearchQuery.isNotEmpty()) {
                IconButton(
                  onClick = { onSearchQueryChange("") },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("clear_apps_search_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_clear_search),
                  )
                }
              }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
          )

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
          ) {
            item {
              FilterChip(
                selected = uiState.hideSystemApps,
                onClick = onToggleHideSystemApps,
                label = { Text("Nur Nutzer-Apps") },
                leadingIcon =
                  if (uiState.hideSystemApps) {
                    {
                      Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  } else null,
                modifier = Modifier.testTag("hide_system_apps_chip"),
              )
            }
            items(AppPermissionFilter.entries) { filter ->
              FilterChip(
                selected = filter == uiState.selectedAppFilter,
                onClick = { onSelectFilter(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.testTag("app_filter_${filter.name.lowercase()}"),
              )
            }
          }
        }
      }

      when {
        scanResult == null -> {
          InitialScanPromptView(
            scanning = uiState.installedAppsScanning,
            onStartScan = onStartScan,
            modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).weight(1f),
          )
        }
        scanResult.errorMessage != null -> {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .widthIn(max = 860.dp)
                .weight(1f)
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            contentAlignment = Alignment.Center,
          ) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = scanResult.errorMessage,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Button(
                  onClick = onStartScan,
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Text("Erneut versuchen")
                }
              }
            }
          }
        }
        uiState.filteredInstalledApps.isEmpty() -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Surface(
              shape = MaterialTheme.shapes.extraLarge,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
              Icon(
                imageVector = Icons.Outlined.LayersClear,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(20.dp).size(40.dp),
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Keine passenden Apps gefunden",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Passe Suchbegriff, Kategorie-Filter oder den System-App-Schalter an.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
        else -> {
          LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier =
              Modifier.fillMaxWidth()
                .widthIn(max = 920.dp)
                .weight(1f)
                .testTag("installed_apps_grid")
                .semantics { liveRegion = LiveRegionMode.Polite },
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
          ) {
            item(span = { GridItemSpan(columns) }) {
              ScanSummaryHeader(
                scanResult = scanResult,
                visibleAfterFilter = uiState.filteredInstalledApps.size,
                activeFilter = uiState.selectedAppFilter,
                onSelectFilter = onSelectFilter,
                onOpenExport = { showExportDialog = true },
              )
            }

            item(key = "gemini_privacy_card", span = { GridItemSpan(columns) }) {
              GeminiPrivacyAdvisorCard(
                scanResult = scanResult,
                analysisState = privacyAnalysisState,
                onAnalyze = onAnalyzePrivacy,
                onReset = onClearPrivacyAnalysis,
                onOpenAppSettings = { app -> openSystemAppInfo(app) },
              )
            }

            uiState.categorizedAppSections.forEach { group ->
              val isCollapsed = group.category in uiState.collapsedAppSections
              item(
                key = "header_${group.category.name}",
                span = { GridItemSpan(columns) },
              ) {
                CategorySectionHeaderCard(
                  group = group,
                  isCollapsed = isCollapsed,
                  onToggleCollapse = { onToggleSectionCollapsed(group.category) },
                )
              }

              if (!isCollapsed) {
                items(group.apps, key = { it.packageName }) { app ->
                  InstalledAppPermissionCard(
                    app = app,
                    compact = compact,
                    onClick = { selectedApp = app },
                    onRevokeInSettings = { openSystemAppInfo(app) },
                  )
                }
              }
            }

            item(span = { GridItemSpan(columns) }) { Spacer(modifier = Modifier.height(16.dp)) }
          }
        }
      }
    }
  }

  if (showExportDialog && scanResult != null && scanResult.errorMessage == null) {
    ExportInstalledAppsDialog(
      scanResult = scanResult,
      hideSystemAppsFilterActive = uiState.hideSystemApps,
      hasActiveFilterOrSearch =
        uiState.appSearchQuery.isNotBlank() || uiState.selectedAppFilter != AppPermissionFilter.ALL,
      includeSystemApps = exportIncludeSystemApps,
      onToggleIncludeSystemApps = { exportIncludeSystemApps = it },
      onExportFormat = ::launchExport,
      onDismiss = { showExportDialog = false },
    )
  }

  val activeDetailApp = selectedApp
  if (activeDetailApp != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { selectedApp = null },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      AppPermissionDetailSheet(
        app = activeDetailApp,
        onOpenSettings = {
          selectedApp = null
          openSystemAppInfo(activeDetailApp)
        },
      )
    }
  }
}

@Composable
private fun InitialScanPromptView(
  scanning: Boolean,
  onStartScan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Card(
      shape = MaterialTheme.shapes.extraLarge,
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Icon(
            imageVector = Icons.Filled.AdminPanelSettings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(16.dp).size(36.dp),
          )
        }

        Text(
          text = "Gewährte Laufzeitrechte prüfen",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )

        Text(
          text =
            "Kategorisiert alle OS-sichtbaren installierten Apps und hebt kritische Zugriffe auf CAMERA, LOCATION und RECORD_AUDIO deutlich hervor.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text =
                "Ohne QUERY_ALL_PACKAGES (nutzt <queries> für Launcher-Apps). Keine Speicherung, kein Upload, kein Hintergrundscan.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }

        Button(
          onClick = onStartScan,
          enabled = !scanning,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("scan_apps_permissions_button"),
        ) {
          if (scanning) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Installierte Apps werden gelesen...")
          } else {
            Icon(
              imageVector = Icons.Filled.VerifiedUser,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_scan_app_permissions))
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScanSummaryHeader(
  scanResult: InstalledAppsScanResult,
  visibleAfterFilter: Int,
  activeFilter: AppPermissionFilter,
  onSelectFilter: (AppPermissionFilter) -> Unit,
  onOpenExport: () -> Unit,
) {
  val summary = remember(scanResult) { InstalledAppsExporter.summarize(scanResult, true) }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      val ratio =
        if (scanResult.visiblePackageCount == 0) 0f
        else
          scanResult.appsWithCriticalSensorsCount.toFloat() /
            scanResult.visiblePackageCount.toFloat()
      MetricRingCard(
        title = "Kritische Sensoren",
        progress = ratio,
        centerText = "${scanResult.appsWithCriticalSensorsCount}",
        subtitle = "von ${scanResult.visiblePackageCount} Apps",
        modifier = Modifier.weight(1f),
      )

      Card(
        modifier = Modifier.weight(1.35f),
        shape = MaterialTheme.shapes.large,
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Kritische Zugriffe schnell filtern",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SensitivityStatPill(
              icon = Icons.Outlined.CameraAlt,
              label = "CAMERA · ${scanResult.cameraAccessCount}",
              selected = activeFilter == AppPermissionFilter.CAMERA,
              onClick = { onSelectFilter(AppPermissionFilter.CAMERA) },
            )
            SensitivityStatPill(
              icon = Icons.Outlined.LocationOn,
              label = "LOCATION · ${scanResult.locationAccessCount}",
              selected = activeFilter == AppPermissionFilter.LOCATION,
              onClick = { onSelectFilter(AppPermissionFilter.LOCATION) },
            )
            SensitivityStatPill(
              icon = Icons.Outlined.Mic,
              label = "RECORD_AUDIO · ${scanResult.microphoneAccessCount}",
              selected = activeFilter == AppPermissionFilter.MICROPHONE,
              onClick = { onSelectFilter(AppPermissionFilter.MICROPHONE) },
            )
          }
          Text(
            text =
              "Angezeigt: $visibleAfterFilter · Gesamt: ${summary.totalVisibleApps} (${summary.userAppsCount} Nutzer / ${summary.systemAppsCount} System)",
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    if (scanResult.apps.isNotEmpty()) {
      FilledTonalButton(
        onClick = onOpenExport,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("export_apps_summary_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Download,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            "${stringResource(R.string.action_export_app_list_with_system)} (${summary.totalVisibleApps})",
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun ExportInstalledAppsDialog(
  scanResult: InstalledAppsScanResult,
  hideSystemAppsFilterActive: Boolean,
  hasActiveFilterOrSearch: Boolean,
  includeSystemApps: Boolean,
  onToggleIncludeSystemApps: (Boolean) -> Unit,
  onExportFormat: (AppExportFormat) -> Unit,
  onDismiss: () -> Unit,
) {
  val summary =
    remember(scanResult, includeSystemApps) {
      InstalledAppsExporter.summarize(scanResult, includeSystemApps)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("export_apps_dialog"),
    icon = {
      Icon(
        imageVector = Icons.Filled.Download,
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
              modifier = Modifier.testTag("export_apps_count_label"),
            )
            Text(
              text =
                "${summary.userAppsCount} Nutzer-Apps · ${summary.systemAppsCount} System-Apps (OS-sichtbar)",
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        if (hideSystemAppsFilterActive || hasActiveFilterOrSearch) {
          Text(
            text =
              "Hinweis: Aktive Such- und Ansichtsfilter im Tab werden für einen vollständigen Geräte-Export ignoriert.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { onToggleIncludeSystemApps(!includeSystemApps) }
              .testTag("export_include_system_apps_row"),
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
              text = "${summary.systemAppsCount} vorinstallierte/OS-Systempakete",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = includeSystemApps,
            onCheckedChange = onToggleIncludeSystemApps,
            modifier = Modifier.testTag("export_include_system_apps_switch"),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { onExportFormat(AppExportFormat.CSV) },
            enabled = summary.exportAppsCount > 0,
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("export_csv_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Description,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.export_apps_csv_button))
          }

          FilledTonalButton(
            onClick = { onExportFormat(AppExportFormat.JSON) },
            enabled = summary.exportAppsCount > 0,
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("export_json_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Download,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.export_apps_json_button))
          }
        }

        Text(
          text =
            "Speichert App-Namen, Paketnamen, Typ (Nutzer/System), Version und gewährte Laufzeitrechte ausschließlich lokal in eine von dir gewählte Datei (ohne QUERY_ALL_PACKAGES, ohne Speicherberechtigung, ohne Cloud-Upload).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("export_cancel_button"),
      ) {
        Text(stringResource(R.string.export_apps_cancel))
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiPrivacyAdvisorCard(
  scanResult: InstalledAppsScanResult,
  analysisState: PermissionPrivacyAnalysisState,
  onAnalyze: () -> Unit,
  onReset: () -> Unit,
  onOpenAppSettings: (InstalledAppPermissionInfo) -> Unit,
) {
  val accentBrush =
    Brush.linearGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          MaterialTheme.colorScheme.surfaceContainerLow,
        )
    )

  Card(
    modifier = Modifier.fillMaxWidth().testTag("gemini_privacy_card"),
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().background(accentBrush).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(10.dp).size(20.dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = stringResource(R.string.privacy_ai_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text = "Gemini 3.8 Flash · Opt-in Empfehlungen für gewährte Rechte",
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        if (analysisState is PermissionPrivacyAnalysisState.Success) {
          IconButton(
            onClick = onReset,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("clear_privacy_analysis_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Analyse schließen",
            )
          }
        }
      }

      when (analysisState) {
        is PermissionPrivacyAnalysisState.Idle -> {
          Text(
            text =
              "Lass deine ${scanResult.appsWithDangerousCount} App(s) mit gewährten gefährlichen Berechtigungen on-demand von Gemini auswerten, um verständliche Vorschläge zur Härtung der Privatsphäre zu erhalten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.PrivacyTip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text =
                  "Nur nach deinem Tipp werden App-Namen & gewährte Berechtigungen über Firebase AI Logic an Gemini gesendet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
          Button(
            onClick = onAnalyze,
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("analyze_privacy_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.privacy_ai_analyze_button))
          }
        }

        is PermissionPrivacyAnalysisState.Loading -> {
          Row(
            modifier =
              Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("privacy_analysis_loading"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Gemini wertet Berechtigungen aus...",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "Prüft ${scanResult.appsWithDangerousCount} Apps auf kritische Kombinationen (Kamera, Standort, Mikrofon).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        is PermissionPrivacyAnalysisState.Error -> {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth().testTag("privacy_analysis_error"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.WarningAmber,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = analysisState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
            }
          }
          FilledTonalButton(
            onClick = onAnalyze,
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("analyze_privacy_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.privacy_ai_retry_button))
          }
        }

        is PermissionPrivacyAnalysisState.Success -> {
          PrivacyReportContent(
            report = analysisState.report,
            scannedApps = scanResult.apps,
            onAnalyzeAgain = onAnalyze,
            onOpenAppSettings = onOpenAppSettings,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivacyReportContent(
  report: PrivacyAnalysisReport,
  scannedApps: List<InstalledAppPermissionInfo>,
  onAnalyzeAgain: () -> Unit,
  onOpenAppSettings: (InstalledAppPermissionInfo) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text =
            "${report.analyzedAppCount} Apps · ${report.analyzedPermissionCount} gewährte Rechte analysiert" +
              if (report.omittedDangerousAppCount > 0)
                " (+${report.omittedDangerousAppCount} gekürzt)"
              else "",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = report.summary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("privacy_analysis_summary"),
        )
      }
    }

    if (report.keyRisks.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Wichtigste Beobachtungen",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        report.keyRisks.forEach { risk ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Text(
              text = "•",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = risk,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    if (report.suggestions.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Empfohlene Schritte",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        report.suggestions.forEachIndexed { index, suggestion ->
          PrivacySuggestionCard(
            index = index,
            suggestion = suggestion,
            scannedApps = scannedApps,
            onOpenAppSettings = onOpenAppSettings,
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Hinweis: KI-Orientierung, kein Malware-Urteil.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = onAnalyzeAgain,
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("analyze_privacy_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Neu prüfen")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivacySuggestionCard(
  index: Int,
  suggestion: PrivacySuggestion,
  scannedApps: List<InstalledAppPermissionInfo>,
  onOpenAppSettings: (InstalledAppPermissionInfo) -> Unit,
) {
  val (badgeBg, badgeFg) =
    when (suggestion.priority) {
      PrivacyRecommendationPriority.HIGH ->
        MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
      PrivacyRecommendationPriority.MEDIUM ->
        MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
      PrivacyRecommendationPriority.LOW ->
        MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("privacy_suggestion_$index"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = MaterialTheme.shapes.extraSmall,
          color = badgeBg,
        ) {
          Text(
            text = suggestion.priority.label,
            style = MonoTokenTextStyle,
            color = badgeFg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
        if (!suggestion.targetPermission.isNullOrBlank()) {
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
          ) {
            Text(
              text = suggestion.targetPermission,
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Text(
        text = suggestion.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Text(
        text = suggestion.recommendation,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (suggestion.affectedApps.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          suggestion.affectedApps.forEach { appName ->
            val matchedApp =
              scannedApps.firstOrNull {
                it.appName.equals(appName, ignoreCase = true) ||
                  it.packageName.equals(appName, ignoreCase = true)
              }
            if (matchedApp != null) {
              OutlinedButton(
                onClick = { onOpenAppSettings(matchedApp) },
                modifier = Modifier.minimumInteractiveComponentSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Settings,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${matchedApp.appName} · Rechte",
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                )
              }
            } else {
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Text(
                  text = appName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SensitivityStatPill(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val bg =
    if (selected) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.errorContainer
  val fg =
    if (selected) MaterialTheme.colorScheme.onError
    else MaterialTheme.colorScheme.onErrorContainer

  Surface(
    shape = MaterialTheme.shapes.small,
    color = bg,
    modifier =
      Modifier.clip(MaterialTheme.shapes.small)
        .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MonoTokenTextStyle,
        color = fg,
      )
    }
  }
}

@Composable
private fun CategorySectionHeaderCard(
  group: CategorizedAppGroup,
  isCollapsed: Boolean,
  onToggleCollapse: () -> Unit,
) {
  val category = group.category
  val containerColor =
    when {
      category.isCriticalTier -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.48f)
      category == AppSectionCategory.OTHER_SENSITIVE ->
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
      else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
  val accentColor =
    when {
      category.isCriticalTier -> MaterialTheme.colorScheme.error
      category == AppSectionCategory.OTHER_SENSITIVE -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.outline
    }
  val sectionIcon =
    when (category) {
      AppSectionCategory.MULTI_CRITICAL -> Icons.Outlined.WarningAmber
      AppSectionCategory.CAMERA -> Icons.Outlined.CameraAlt
      AppSectionCategory.LOCATION -> Icons.Outlined.LocationOn
      AppSectionCategory.MICROPHONE -> Icons.Outlined.Mic
      AppSectionCategory.OTHER_SENSITIVE -> Icons.Outlined.Security
      AppSectionCategory.STANDARD -> Icons.Filled.VerifiedUser
    }

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("section_header_${category.name.lowercase()}")
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onToggleCollapse),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, accentColor.copy(alpha = if (category.isCriticalTier) 0.55f else 0.35f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = accentColor.copy(alpha = 0.16f),
      ) {
        Icon(
          imageVector = sectionIcon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.padding(8.dp).size(20.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = accentColor,
          ) {
            Text(
              text = category.badge,
              style = MonoTokenTextStyle,
              color =
                if (category.isCriticalTier) MaterialTheme.colorScheme.onError
                else MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "${group.apps.size} Apps",
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = category.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = category.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Icon(
        imageVector = if (isCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
        contentDescription = if (isCollapsed) "Kategorie aufklappen" else "Kategorie zuklappen",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.minimumInteractiveComponentSize(),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstalledAppPermissionCard(
  app: InstalledAppPermissionInfo,
  compact: Boolean,
  onClick: () -> Unit,
  onRevokeInSettings: () -> Unit,
) {
  val iconBitmap = rememberAppIconBitmap(app.packageName)

  val isCritical = app.isCriticalSensorApp
  val accentColor =
    when {
      isCritical -> MaterialTheme.colorScheme.error
      app.hasHighSensitivityAccess -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.outlineVariant
    }

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("installed_app_card_${app.packageName}")
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isCritical) MaterialTheme.colorScheme.surfaceContainer
          else MaterialTheme.colorScheme.surfaceContainerLow
      ),
    border =
      BorderStroke(
        width = if (isCritical) 1.5.dp else 1.dp,
        color =
          when {
            isCritical -> MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
            app.hasHighSensitivityAccess -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
          },
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(if (compact) 14.dp else 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (isCritical) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.width(4.dp)
                .height(18.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(accentColor)
          )
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.error,
          ) {
            Text(
              text = "KRITISCH",
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.onError,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = app.criticalSensorTags.joinToString(" · "),
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (iconBitmap != null) {
          Image(
            bitmap = iconBitmap,
            contentDescription = app.appName,
            modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.medium),
          )
        } else {
          Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = app.appName,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = app.appName,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (app.isSystemApp) {
              Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
              ) {
                Text(
                  text = "System",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = app.packageName,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            when {
              app.hasHighSensitivityAccess -> MaterialTheme.colorScheme.errorContainer
              app.grantedDangerousPermissions.isNotEmpty() ->
                MaterialTheme.colorScheme.tertiaryContainer
              else -> MaterialTheme.colorScheme.secondaryContainer
            },
        ) {
          Text(
            text = "${app.grantedDangerousPermissions.size} gewährt",
            style = MonoTokenTextStyle,
            color =
              when {
                app.hasHighSensitivityAccess -> MaterialTheme.colorScheme.onErrorContainer
                app.grantedDangerousPermissions.isNotEmpty() ->
                  MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSecondaryContainer
              },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      if (app.grantedDangerousPermissions.isEmpty()) {
        Text(
          text = "Keine gefährlichen Laufzeitrechte gewährt (${app.totalRequestedPermissions} angefordert).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          app.grantedDangerousPermissions.take(6).forEach { perm ->
            PermissionSensitivityChip(perm = perm)
          }
              if (app.grantedDangerousPermissions.size > 6) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
              Text(
                text = "+${app.grantedDangerousPermissions.size - 6} weitere",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onClick,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("app_details_button_${app.packageName}"),
        ) {
          Text("Details")
        }

        if (app.grantedDangerousPermissions.isNotEmpty()) {
          FilledTonalButton(
            onClick = onRevokeInSettings,
            colors =
              if (isCritical) {
                ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
              } else {
                ButtonDefaults.filledTonalButtonColors()
              },
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("revoke_permissions_button_${app.packageName}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Rechte entziehen", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
            )
          }
        } else {
          OutlinedButton(
            onClick = onRevokeInSettings,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("app_settings_button_${app.packageName}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("App-Info", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionSensitivityChip(perm: GrantedDangerousPermission) {
  val bg =
    if (perm.category.isHighSensitivity) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.secondaryContainer
  val fg =
    if (perm.category.isHighSensitivity) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

  Surface(shape = MaterialTheme.shapes.small, color = bg) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = perm.category.icon(),
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = "${perm.category.label} · ${perm.shortName}",
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AppPermissionDetailSheet(
  app: InstalledAppPermissionInfo,
  onOpenSettings: () -> Unit,
) {
  val iconBitmap = rememberAppIconBitmap(app.packageName)

  LazyColumn(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 28.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        if (iconBitmap != null) {
          Image(
            bitmap = iconBitmap,
            contentDescription = app.appName,
            modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.medium),
          )
        } else {
          Surface(
            modifier = Modifier.size(52.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = app.appName,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = app.appName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = app.packageName,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text =
              "${app.grantedDangerousPermissions.size} gefährliche gewährt · ${app.totalRequestedPermissions} gesamt angefordert",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector =
              if (app.hasHighSensitivityAccess) Icons.Outlined.WarningAmber
              else Icons.Outlined.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
          )
          Text(
            text =
              "Eine gewährte Berechtigung ist kein Malware-Nachweis. Du kannst Berechtigungen direkt in den Android-Systemeinstellungen verwalten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }
    }

    if (app.grantedDangerousPermissions.isEmpty()) {
      item {
        Text(
          text = "Dieser App wurden aktuell keine gefährlichen Laufzeitberechtigungen gewährt.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(app.grantedDangerousPermissions, key = { it.fullName }) { perm ->
        Card(
          shape = MaterialTheme.shapes.medium,
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = perm.category.icon(),
                  contentDescription = null,
                  tint =
                    if (perm.category.isHighSensitivity) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = perm.category.label,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              if (perm.category.isHighSensitivity) {
                Surface(
                  shape = MaterialTheme.shapes.extraSmall,
                  color = MaterialTheme.colorScheme.errorContainer,
                ) {
                  Text(
                    text = "Sensibel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
            Text(
              text = perm.fullName,
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.primary,
            )
            if (!perm.description.isNullOrBlank()) {
              Text(
                text = perm.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    item {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text =
          "Tipp: Öffne dort „Berechtigungen“, um Kamera, Standort oder Mikrofon direkt zu entziehen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = onOpenSettings,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("sheet_open_app_settings_button"),
      ) {
        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("In Systemeinstellungen Rechte entziehen")
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          Icons.AutoMirrored.Filled.OpenInNew,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

private fun SensitivityCategory.icon(): ImageVector =
  when (this) {
    SensitivityCategory.CAMERA -> Icons.Outlined.CameraAlt
    SensitivityCategory.LOCATION -> Icons.Outlined.LocationOn
    SensitivityCategory.MICROPHONE -> Icons.Outlined.Mic
    SensitivityCategory.PHONE_SMS -> Icons.Outlined.Call
    SensitivityCategory.CONTACTS -> Icons.Outlined.Contacts
    SensitivityCategory.SENSORS_ACTIVITY -> Icons.Outlined.Sensors
    SensitivityCategory.STORAGE_MEDIA -> Icons.Outlined.Folder
    SensitivityCategory.CALENDAR -> Icons.Outlined.CalendarMonth
    SensitivityCategory.NEARBY -> Icons.Outlined.Devices
    SensitivityCategory.NOTIFICATIONS -> Icons.Outlined.Notifications
    SensitivityCategory.OTHER -> Icons.Outlined.Security
  }

@Composable
private fun rememberAppIconBitmap(packageName: String): ImageBitmap? {
  val context = LocalContext.current
  val bitmap by
    produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
      value =
        withContext(Dispatchers.IO) {
          try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toSmallImageBitmap(96)
          } catch (_: PackageManager.NameNotFoundException) {
            null
          } catch (_: RuntimeException) {
            null
          }
        }
    }
  return bitmap
}

private fun Drawable.toSmallImageBitmap(sizePx: Int): ImageBitmap {
  if (this is BitmapDrawable && bitmap != null && bitmap.width <= sizePx * 2) {
    return bitmap.asImageBitmap()
  }
  val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bmp)
  setBounds(0, 0, canvas.width, canvas.height)
  draw(canvas)
  return bmp.asImageBitmap()
}
