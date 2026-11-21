package com.example.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.openDeviceSettings
import com.example.features.battery.BatteryDashboardSnapshot
import com.example.features.battery.ChargingStatus
import com.example.features.battery.RemainingTimeSource
import com.example.ui.DeviceCareUiState
import com.example.ui.components.BatteryHealthTrendChartCard
import com.example.ui.theme.MonoTokenTextStyle
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryDashboardScreen(
  uiState: DeviceCareUiState,
  onBack: () -> Unit,
  onRefresh: (Intent?) -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOnRefresh by rememberUpdatedState(onRefresh)
  val compact = uiState.preferences.compactDensity

  LaunchedEffect(Unit) { currentOnRefresh(null) }

  DisposableEffect(context, lifecycleOwner) {
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            currentOnRefresh(intent)
          }
        }
      }
    try {
      ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_EXPORTED,
      )
    } catch (_: RuntimeException) {
      // Fallback safely on constrained environments; manual refresh remains active.
    }

    val lifecycleObserver = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        currentOnRefresh(null)
      }
    }
    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
      try {
        context.unregisterReceiver(receiver)
      } catch (_: IllegalArgumentException) {
        // Already unregistered
      }
    }
  }

  fun launchBatterySettings(intent: Intent, fallbackHint: String) {
    when (openDeviceSettings(intent) { context.startActivity(it) }) {
      SettingsLaunchResult.DIRECT -> Unit
      SettingsLaunchResult.GENERAL_SETTINGS ->
        onShowSnackbar("Zielseite nicht direkt verfügbar. $fallbackHint")
      SettingsLaunchResult.UNAVAILABLE ->
        onShowSnackbar("Android-Einstellungen konnten nicht geöffnet werden.")
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("battery_dashboard_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Akku-Monitor",
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "BatteryManager Live-Telemetrie",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("battery_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Zurück",
            )
          }
        },
        actions = {
          IconButton(
            onClick = { onRefresh(null) },
            enabled = !uiState.batteryMonitorLoading,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("refresh_battery_button"),
          ) {
            if (uiState.batteryMonitorLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Akkudaten aktualisieren",
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
      )
    },
  ) { innerPadding ->
    val snapshot = uiState.batterySnapshot ?: BatteryDashboardSnapshot()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      val isWide = maxWidth >= 640.dp

      LazyColumn(
        modifier =
          Modifier.fillMaxSize()
            .testTag("battery_dashboard_list")
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (snapshot.errorMessage != null) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp),
              shape = MaterialTheme.shapes.large,
              colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
              Text(
                text = snapshot.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(18.dp),
              )
            }
          }
        }

        // Hero Charge Level & Charging Status Card
        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            BatteryHeroStatusCard(snapshot = snapshot, compact = compact)
          }
        }

        // Estimated Remaining Usage Time / Charge Time Spotlight Card
        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            RemainingUsageTimeCard(snapshot = snapshot)
          }
        }

        // D3-style Battery Health & Telemetry Trend Line Chart
        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            BatteryHealthTrendChartCard(history = uiState.batteryTrendHistory)
          }
        }

        // Telemetry Bento Grid
        item {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .widthIn(max = 880.dp)
                .testTag("battery_health_telemetry_grid")
          ) {
            BatteryTelemetryGrid(snapshot = snapshot, isWide = isWide)
          }
        }

        // Power Saver & System Battery Shortcuts
        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp)) {
            BatterySystemShortcutsCard(
              snapshot = snapshot,
              onOpenBatterySaver = {
                launchBatterySettings(
                  Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                  "Suche in den Einstellungen nach Energiesparmodus.",
                )
              },
              onOpenPowerUsage = {
                launchBatterySettings(
                  Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
                  "Öffne Einstellungen → Akku.",
                )
              },
            )
          }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatteryHeroStatusCard(
  snapshot: BatteryDashboardSnapshot,
  compact: Boolean,
) {
  val pct = snapshot.percent
  val isCharging = snapshot.chargingStatus == ChargingStatus.CHARGING
  val isFull = snapshot.chargingStatus == ChargingStatus.FULL
  val isLow = pct != null && pct <= 15 && !isCharging
  val isUnhealthy = !snapshot.healthCondition.isHealthy

  val gaugeColor =
    when {
      isUnhealthy || isLow -> MaterialTheme.colorScheme.error
      isCharging || isFull -> MaterialTheme.colorScheme.secondary
      else -> MaterialTheme.colorScheme.primary
    }
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

  val animatedProgress by
    animateFloatAsState(
      targetValue = ((pct ?: 0) / 100f).coerceIn(0f, 1f),
      animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
      label = "battery_hero_ring",
    )

  Card(
    modifier = Modifier.fillMaxWidth().testTag("battery_level_card"),
    shape = MaterialTheme.shapes.extraLarge,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, gaugeColor.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(if (compact) 16.dp else 22.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            drawArc(
              color = trackColor,
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
              color = gaugeColor,
              startAngle = -90f,
              sweepAngle = 360f * animatedProgress,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector =
                when {
                  isCharging -> Icons.Filled.BatteryChargingFull
                  isFull -> Icons.Filled.BatteryFull
                  else -> Icons.Filled.BatteryStd
                },
              contentDescription = null,
              tint = gaugeColor,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = if (pct != null) "$pct%" else "N/A",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color =
              when {
                isUnhealthy || isLow -> MaterialTheme.colorScheme.errorContainer
                isCharging || isFull -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
              },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = if (isCharging) Icons.Filled.Bolt else Icons.Filled.Power,
                contentDescription = null,
                tint =
                  when {
                    isUnhealthy || isLow -> MaterialTheme.colorScheme.onErrorContainer
                    isCharging || isFull -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                  },
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = snapshot.chargingStatus.label,
                style = MonoTokenTextStyle,
                color =
                  when {
                    isUnhealthy || isLow -> MaterialTheme.colorScheme.onErrorContainer
                    isCharging || isFull -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                  },
              )
            }
          }

          Text(
            text = snapshot.chargingStatus.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
              Text(
                text = snapshot.powerSource.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
            Surface(
              shape = MaterialTheme.shapes.small,
              color =
                if (snapshot.healthCondition.isHealthy) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.errorContainer,
            ) {
              Text(
                text = "Zustand: ${snapshot.healthCondition.label}",
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (snapshot.healthCondition.isHealthy)
                    MaterialTheme.colorScheme.onSecondaryContainer
                  else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      if (snapshot.observedAtMillis > 0L) {
        val timeText =
          DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.GERMAN)
            .format(Date(snapshot.observedAtMillis))
        Text(
          text = "Live-Messung um $timeText · Nur lokal im RAM, keine Speicherung",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun RemainingUsageTimeCard(snapshot: BatteryDashboardSnapshot) {
  val estimate = snapshot.remainingTime
  val isAvailable = estimate.source != RemainingTimeSource.UNAVAILABLE

  val containerColor =
    when {
      estimate.isChargingEstimate && isAvailable -> MaterialTheme.colorScheme.secondaryContainer
      isAvailable -> MaterialTheme.colorScheme.primaryContainer
      else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
  val contentColor =
    when {
      estimate.isChargingEstimate && isAvailable -> MaterialTheme.colorScheme.onSecondaryContainer
      isAvailable -> MaterialTheme.colorScheme.onPrimaryContainer
      else -> MaterialTheme.colorScheme.onSurface
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("remaining_time_card"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = contentColor.copy(alpha = 0.14f),
        ) {
          Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.padding(8.dp).size(22.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              if (estimate.isChargingEstimate) "Geschätzte Restladezeit"
              else "Geschätzte verbleibende Nutzungszeit",
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = 0.85f),
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text = estimate.formattedDuration,
            style = MaterialTheme.typography.headlineMedium,
            color = contentColor,
          )
        }
      }

      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
      ) {
        Text(
          text = estimate.source.label,
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }

      if (!estimate.limitationNote.isNullOrBlank()) {
        Text(
          text = estimate.limitationNote,
          style = MaterialTheme.typography.bodySmall,
          color = contentColor.copy(alpha = 0.85f),
        )
      }
    }
  }
}

@Composable
private fun BatteryTelemetryGrid(
  snapshot: BatteryDashboardSnapshot,
  isWide: Boolean,
) {
  val healthTitle = "System-Akkuzustand"
  val healthValue = snapshot.healthCondition.label
  val healthDetail =
    "Gemeldeter Status (EXTRA_HEALTH). Android gibt keine prozentuale Zellabnutzung frei."

  val tempTitle = "Akkutemperatur"
  val tempValue =
    snapshot.temperatureCelsius?.let { String.format(Locale.GERMAN, "%.1f °C", it) } ?: "N/A"
  val tempDetail =
    when {
      snapshot.temperatureCelsius == null -> "Temperatursensor nicht gemeldet"
      snapshot.temperatureCelsius >= 42f -> "Erhöhte Temperatur (> 42 °C)"
      snapshot.temperatureCelsius <= 5f -> "Kühl (< 5 °C)"
      else -> "Normaler Betriebsbereich"
    }

  val voltTitle = "Spannung & Zelltyp"
  val voltValue =
    snapshot.voltageMillivolts?.let {
      String.format(Locale.GERMAN, "%.2f V (%d mV)", it / 1000f, it)
    } ?: "N/A"
  val voltDetail = "Technologie: ${snapshot.technology ?: "Unbekannt"}"

  val gaugeTitle = "Fuel-Gauge (Restladung & Strom)"
  val gaugeValue =
    snapshot.chargeCounterMah?.let { String.format(Locale.GERMAN, "%,d mAh", it) } ?: "N/A"
  val currentText =
    when {
      snapshot.currentAverageMilliamps != null ->
        "Durchschn. ${snapshot.currentAverageMilliamps} mA"
      snapshot.currentNowMilliamps != null -> "Momentan ${snapshot.currentNowMilliamps} mA"
      else -> "Stromstärke vom System nicht gemeldet"
    }

  if (isWide) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TelemetryMetricCard(
          icon = Icons.Outlined.HealthAndSafety,
          title = healthTitle,
          value = healthValue,
          subtitle = healthDetail,
          highlightError = !snapshot.healthCondition.isHealthy,
          modifier = Modifier.weight(1f),
        )
        TelemetryMetricCard(
          icon = Icons.Outlined.DeviceThermostat,
          title = tempTitle,
          value = tempValue,
          subtitle = tempDetail,
          highlightError = (snapshot.temperatureCelsius ?: 0f) >= 42f,
          modifier = Modifier.weight(1f),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TelemetryMetricCard(
          icon = Icons.Outlined.ElectricBolt,
          title = voltTitle,
          value = voltValue,
          subtitle = voltDetail,
          modifier = Modifier.weight(1f),
        )
        TelemetryMetricCard(
          icon = Icons.Outlined.Memory,
          title = gaugeTitle,
          value = gaugeValue,
          subtitle = currentText,
          modifier = Modifier.weight(1f),
        )
      }
    }
  } else {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      TelemetryMetricCard(
        icon = Icons.Outlined.HealthAndSafety,
        title = healthTitle,
        value = healthValue,
        subtitle = healthDetail,
        highlightError = !snapshot.healthCondition.isHealthy,
      )
      TelemetryMetricCard(
        icon = Icons.Outlined.DeviceThermostat,
        title = tempTitle,
        value = tempValue,
        subtitle = tempDetail,
        highlightError = (snapshot.temperatureCelsius ?: 0f) >= 42f,
      )
      TelemetryMetricCard(
        icon = Icons.Outlined.ElectricBolt,
        title = voltTitle,
        value = voltValue,
        subtitle = voltDetail,
      )
      TelemetryMetricCard(
        icon = Icons.Outlined.Memory,
        title = gaugeTitle,
        value = gaugeValue,
        subtitle = currentText,
      )
    }
  }
}

@Composable
private fun TelemetryMetricCard(
  icon: ImageVector,
  title: String,
  value: String,
  subtitle: String,
  highlightError: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (highlightError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.surfaceContainerLow
      ),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (highlightError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint =
            if (highlightError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun BatterySystemShortcutsCard(
  snapshot: BatteryDashboardSnapshot,
  onOpenBatterySaver: () -> Unit,
  onOpenPowerUsage: () -> Unit,
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Energie & Android-Einstellungen",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (snapshot.powerSaveMode == true) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
          Text(
            text =
              when (snapshot.powerSaveMode) {
                true -> "Sparmodus: EIN"
                false -> "Sparmodus: AUS"
                null -> "Sparmodus: N/A"
              },
            style = MonoTokenTextStyle,
            color =
              if (snapshot.powerSaveMode == true) MaterialTheme.colorScheme.onTertiaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "Öffnet die System-Akkuseiten von Android zur direkten Steuerung.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilledTonalButton(
          onClick = onOpenBatterySaver,
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("battery_open_saver_settings"),
        ) {
          Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sparmodus", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
          onClick = onOpenPowerUsage,
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("battery_open_usage_settings"),
        ) {
          Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Akkuverbrauch", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}
