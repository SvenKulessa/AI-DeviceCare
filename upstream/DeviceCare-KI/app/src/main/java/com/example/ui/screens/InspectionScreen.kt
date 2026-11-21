package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.evidence.ObservationStatus
import com.example.devicecare.InspectionResult
import com.example.devicecare.ObservationDisplayRow
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.openDeviceSettings
import com.example.devicecare.ownAppSettings
import com.example.ui.DeviceCareUiState
import com.example.ui.components.MetricRingCard
import com.example.ui.theme.MonoTokenTextStyle
import com.example.ui.theme.ThemeMode

@Composable
fun InspectionScreen(
  uiState: DeviceCareUiState,
  onStartInspection: () -> Unit,
  onCycleThemeMode: () -> Unit,
  onSettingsMessage: (String?) -> Unit,
  onOpenBatteryMonitor: () -> Unit,
  onOpenAiAdvisor: () -> Unit,
  onOpenCloudSync: () -> Unit,
  onOpenSystemSecurity: () -> Unit,
  onOpenPrivacySummary: () -> Unit,
  onOpenAppPermissions: () -> Unit,
  onOpenRoadmap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val compact = uiState.preferences.compactDensity
  val spacing = if (compact) 12.dp else 18.dp

  fun launchSettings(intent: Intent, fallbackHint: String) {
    val message =
      when (openDeviceSettings(intent) { context.startActivity(it) }) {
        SettingsLaunchResult.DIRECT -> null
        SettingsLaunchResult.GENERAL_SETTINGS -> "Die Zielseite ist nicht verfügbar. $fallbackHint"
        SettingsLaunchResult.UNAVAILABLE ->
          "Android-Einstellungen konnten nicht geöffnet werden. $fallbackHint"
      }
    onSettingsMessage(message)
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth >= 640.dp

    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("devicecare_scroll_content"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(spacing),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item {
        Column(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
          InspectionHeader(
            enterprisePreview = uiState.enterprisePreviewMode,
            themeMode = uiState.preferences.themeMode,
            onCycleThemeMode = onCycleThemeMode,
          )
        }
      }

      item {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
          HeroInspectionCard(
            enterprisePreview = uiState.enterprisePreviewMode,
            inspectionInProgress = uiState.inspectionInProgress,
            onStartInspection = onStartInspection,
            onOpenBatteryMonitor = onOpenBatteryMonitor,
            onOpenAiAdvisor = onOpenAiAdvisor,
            onOpenCloudSync = onOpenCloudSync,
            onOpenSystemSecurity = onOpenSystemSecurity,
            onOpenPrivacySummary = onOpenPrivacySummary,
            onOpenRoadmap = onOpenRoadmap,
            compact = compact,
          )
        }
      }

      if (uiState.enterprisePreviewMode) {
        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
            EnterprisePrototypeNoticeCard(onOpenRoadmap = onOpenRoadmap)
          }
        }
      } else {
        // Live inspection results (only after explicit button tap; never persisted)
        item {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .widthIn(max = 860.dp)
                .testTag("inspection_results")
                .semantics { liveRegion = LiveRegionMode.Polite }
          ) {
            val result = uiState.inspectionResult
            AnimatedVisibility(
              visible = result != null,
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              if (result != null) {
                InspectionResultSection(
                  result = result,
                  isStale = uiState.isInspectionStale,
                  isWide = isWide,
                  settingsMessage = uiState.settingsMessage,
                  onOpenBatterySettings = {
                    launchSettings(
                      Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                      "Suche in den Einstellungen nach Energiesparmodus.",
                    )
                  },
                  onOpenAppSettings = {
                    launchSettings(
                      ownAppSettings(context.packageName),
                      "Öffne in den Einstellungen Apps → AI-DeviceCare.",
                    )
                  },
                )
              }
            }
          }
        }
      }

      // Quality & Scoring Pipeline Truth Cards (always visible per DeviceCareHome contract)
      item {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
          if (isWide) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              PipelineTruthCard(
                title = "Quality pipeline",
                badge = "NOT_PROVEN",
                body =
                  "Build-Evidenz ist NOT_PROVEN, bis dieses Artefakt exakt an einen geprüften Commit-SHA gebunden wurde.",
                modifier = Modifier.weight(1f),
              )
              PipelineTruthCard(
                title = "Device scoring pipeline",
                badge = "NOT_ASSESSABLE",
                body =
                  "Risk score: NOT_ASSESSABLE — kein kalibriertes Modell und keine vollständige Geräte-Evidenz in M1.",
                modifier = Modifier.weight(1f),
              )
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              PipelineTruthCard(
                title = "Quality pipeline",
                badge = "NOT_PROVEN",
                body =
                  "Build-Evidenz ist NOT_PROVEN, bis dieses Artefakt exakt an einen geprüften Commit-SHA gebunden wurde.",
              )
              PipelineTruthCard(
                title = "Device scoring pipeline",
                badge = "NOT_ASSESSABLE",
                body =
                  "Risk score: NOT_ASSESSABLE — kein kalibriertes Modell und keine vollständige Geräte-Evidenz in M1.",
              )
            }
          }
        }
      }

      // Android Permissions Disclosure & Roadmap Summary
      item {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
          PermissionsDisclosureCard(
            verifiedCount = uiState.verifiedCount,
            totalTasks = uiState.allTasks.size,
            onOpenAppPermissions = onOpenAppPermissions,
            onOpenRoadmap = onOpenRoadmap,
          )
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun InspectionHeader(
  enterprisePreview: Boolean,
  themeMode: ThemeMode,
  onCycleThemeMode: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text =
              if (enterprisePreview) "AI-DeviceCare+ · Business Prototyp"
              else "AI-DeviceCare · M1 Vorschau",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      Text(
        text = if (enterprisePreview) "AI-DeviceCare+" else "AI-DeviceCare",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text =
          if (enterprisePreview) "Business prototype — internal only"
          else "Akku & App-Berechtigungen · Datenschutz zuerst",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    FilledTonalIconButton(
      onClick = onCycleThemeMode,
      modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_theme_toggle"),
    ) {
      Icon(
        imageVector =
          when (themeMode) {
            ThemeMode.DARK -> Icons.Filled.DarkMode
            ThemeMode.LIGHT -> Icons.Filled.LightMode
            ThemeMode.SYSTEM -> Icons.Filled.AutoAwesome
          },
        contentDescription = stringResource(R.string.action_toggle_theme),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroInspectionCard(
  enterprisePreview: Boolean,
  inspectionInProgress: Boolean,
  onStartInspection: () -> Unit,
  onOpenBatteryMonitor: () -> Unit,
  onOpenAiAdvisor: () -> Unit,
  onOpenCloudSync: () -> Unit,
  onOpenSystemSecurity: () -> Unit,
  onOpenPrivacySummary: () -> Unit,
  onOpenRoadmap: () -> Unit,
  compact: Boolean,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("hero_inspection_card"),
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Box(
      modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 232.dp else 264.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_hero_atelier),
        contentDescription = stringResource(R.string.hero_title),
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.Crop,
      )
      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              Brush.verticalGradient(
                colors =
                  listOf(
                    Color.Black.copy(alpha = 0.25f),
                    Color.Black.copy(alpha = 0.55f),
                    Color.Black.copy(alpha = 0.88f),
                  )
              )
            )
      )

      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primary,
        ) {
          Text(
            text = stringResource(R.string.hero_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.hero_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
          )
          Text(
            text = stringResource(R.string.hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.90f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          if (!enterprisePreview) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = onStartInspection,
                enabled = !inspectionInProgress,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("inspection_start_button"),
              ) {
                if (inspectionInProgress) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(stringResource(R.string.action_running_inspection))
                } else {
                  Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.action_start_inspection))
                }
              }

              FilledTonalButton(
                onClick = onOpenBatteryMonitor,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("open_battery_monitor_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.BatteryChargingFull,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Akku-Monitor")
              }

              FilledTonalButton(
                onClick = onOpenAiAdvisor,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("open_ai_advisor_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("KI-Berater")
              }

              FilledTonalButton(
                onClick = onOpenCloudSync,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("open_cloud_sync_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.CloudQueue,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cloud-Sync")
              }

              FilledTonalButton(
                onClick = onOpenSystemSecurity,
                modifier = Modifier.minimumInteractiveComponentSize()
                  .testTag("open_system_security_button"),
              ) {
                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("System- & Google-Check")
              }

              FilledTonalButton(
                onClick = onOpenPrivacySummary,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("open_privacy_summary_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Shield,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Datenschutz-Cockpit")
              }
            }
          } else {
            Button(
              onClick = onOpenRoadmap,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text("M4 Business-Plan ansehen")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EnterprisePrototypeNoticeCard(onOpenRoadmap: () -> Unit) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "AI-DeviceCare+ (Internal Prototype Only)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text =
          "Fleet enrollment and organization features: NOT IMPLEMENTED. Mandantenfähiges Supabase-Backend, RBAC und Fleet-Policies sind für Meilenstein M4 eingeplant.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
      FilledTonalButton(
        onClick = onOpenRoadmap,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("M4/M5 Anforderungen öffnen")
      }
    }
  }
}

@Composable
private fun InspectionResultSection(
  result: InspectionResult,
  isStale: Boolean,
  isWide: Boolean,
  settingsMessage: String?,
  onOpenBatterySettings: () -> Unit,
  onOpenAppSettings: () -> Unit,
) {
  if (result.errorMessage != null) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
      Text(
        text = result.errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(16.dp),
      )
    }
    return
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Freshness / Snapshot Notice
    Surface(
      shape = MaterialTheme.shapes.medium,
      color =
        if (isStale) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint =
            if (isStale) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text =
            if (isStale)
              "Momentaufnahme abgelaufen (> 60 Sekunden). Starte bei Bedarf eine neue lokale Prüfung."
            else
              "Momentaufnahme gültig (max. 60 Sekunden) · Keine Speicherung, kein Hintergrundscan.",
          style = MaterialTheme.typography.bodySmall,
          color =
            if (isStale) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }

    // Battery Ring + Messaufwand Bento
    val pct = result.batteryPercent
    if (isWide) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        MetricRingCard(
          title = "Akkustand (System)",
          progress = (pct ?: 0) / 100f,
          centerText = if (pct != null) "$pct%" else "N/A",
          subtitle = result.batteryChargingState ?: "UNSUPPORTED",
          modifier = Modifier.weight(1f),
        )
        DeviceProfileSummaryCard(
          result = result,
          modifier = Modifier.weight(1.6f),
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        MetricRingCard(
          title = "Akku",
          progress = (pct ?: 0) / 100f,
          centerText = if (pct != null) "$pct%" else "N/A",
          subtitle = result.batteryChargingState ?: "UNSUPPORTED",
          modifier = Modifier.weight(1f),
        )
        BudgetHighlightCard(result = result, modifier = Modifier.weight(1f))
      }
      DeviceProfileSummaryCard(result = result, modifier = Modifier.fillMaxWidth())
    }

    if (result.optionalReadsDeferred) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
      ) {
        Text(
          text =
            "Die Stichprobe wurde wegen Speicherdruck oder hoher thermischer Belastung während der Prüfung verkürzt.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.padding(14.dp),
        )
      }
    }

    ObservationGroupCard(
      title = "Akku-Beobachtungen (LOCAL_BATTERY)",
      rows = result.batteryObservations,
    )

    ObservationGroupCard(
      title = "Berechtigungs-Stichprobe (ANDROID_VISIBLE_APPS)",
      rows = result.permissionObservations,
    )

    // System-guided Android Settings
    Card(
      shape = MaterialTheme.shapes.large,
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Android-Einstellungen prüfen",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = "Öffnet Android-Einstellungen. Änderungen nimmst du dort selbst vor.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilledTonalButton(
            onClick = onOpenBatterySettings,
            modifier =
              Modifier.weight(1f).minimumInteractiveComponentSize().testTag("open_battery_settings"),
          ) {
            Icon(
              Icons.Filled.BatteryChargingFull,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Energiesparmodus", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          FilledTonalButton(
            onClick = onOpenAppSettings,
            modifier =
              Modifier.weight(1f).minimumInteractiveComponentSize().testTag("open_app_settings"),
          ) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("App-Info", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
        if (settingsMessage != null) {
          Text(
            text = settingsMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          )
        }
      }
    }
  }
}

@Composable
private fun BudgetHighlightCard(result: InspectionResult, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Scan-Budget",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
      )
      Text(
        text = "${result.packageLimit} Apps",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
      ) {
        Text(
          text = "API ${result.apiLevel}",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
      Text(
        text = "${result.elapsedMs} ms · CPU ${result.cpuMs} ms",
        style = MonoTokenTextStyle,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun DeviceProfileSummaryCard(
  result: InspectionResult,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Geräteprofil",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text = result.budgetSummary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      result.profileMetrics.forEach { (key, value) ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = value,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        text =
          "Messaufwand: ${result.elapsedMs} ms Laufzeit; ${result.cpuMs} ms Prozess-CPU (nur informativ, keine Akkuenergie-Messung).",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ObservationGroupCard(
  title: String,
  rows: List<ObservationDisplayRow>,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
      )
      rows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = row.label,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${row.id} · ${row.limitationCode}",
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Surface(
            shape = MaterialTheme.shapes.small,
            color =
              if (row.status == ObservationStatus.OBSERVED)
                MaterialTheme.colorScheme.secondaryContainer
              else MaterialTheme.colorScheme.tertiaryContainer,
          ) {
            Text(
              text = row.valueText,
              style = MonoTokenTextStyle,
              color =
                if (row.status == ObservationStatus.OBSERVED)
                  MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PipelineTruthCard(
  title: String,
  badge: String,
  body: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = badge,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
      Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun PermissionsDisclosureCard(
  verifiedCount: Int,
  totalTasks: Int,
  onOpenAppPermissions: () -> Unit,
  onOpenRoadmap: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(8.dp).size(18.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Android permissions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text = "Keine sensiblen Android-Berechtigungen in dieser M1-Vorschau angefordert.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text =
          "Cloud, Analytics, VPN, Malware-Scans und kostenpflichtiges Billing sind nicht aktiv. Fortschritt über alle Meilensteine & QA-Gates: $verifiedCount von $totalTasks Punkten.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Button(
        onClick = onOpenAppPermissions,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("open_app_permissions_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Apps,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Installierte Apps & Laufzeitrechte prüfen")
      }

      FilledTonalButton(
        onClick = onOpenRoadmap,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Filled.VerifiedUser,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Alle Aufgaben, Meilensteine & QA-Pläne ansehen")
      }
    }
  }
}
