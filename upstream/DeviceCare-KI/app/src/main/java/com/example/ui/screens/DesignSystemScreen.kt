package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.RoadmapTask
import com.example.ui.DeviceCareUiState
import com.example.ui.components.PaletteSwatchCard
import com.example.ui.theme.CornerStyle
import com.example.ui.theme.MonoTokenTextStyle
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePalette

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesignSystemScreen(
  uiState: DeviceCareUiState,
  bookmarkedTasks: List<RoadmapTask>,
  onSelectPalette: (ThemePalette) -> Unit,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onSelectCornerStyle: (CornerStyle) -> Unit,
  onToggleCompactDensity: (Boolean) -> Unit,
  onToggleHapticPreview: (Boolean) -> Unit,
  onToggleEnterprisePreview: (Boolean) -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onResetDefaults: () -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showPreviewSheet by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("design_studio_screen"),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = "Design-Studio & Vorgaben",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.semantics { heading() },
        )

        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_designer_avatar),
              contentDescription = "Workspace Avatar",
              modifier = Modifier.size(68.dp).clip(CircleShape),
              contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "AI-DeviceCare Designsystem",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "${uiState.preferences.palette.displayName} · ${uiState.preferences.cornerStyle.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "UI-Vorgaben gespeichert · Messwerte bleiben rein lokal im RAM",
                style = MonoTokenTextStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    // 1. Live Farbpaletten
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "1. Farbpalette wählen",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(ThemePalette.entries) { palette ->
            PaletteSwatchCard(
              palette = palette,
              selected = palette == uiState.preferences.palette,
              onSelect = { onSelectPalette(palette) },
            )
          }
        }
      }
    }

    // 2. Modus & Geometrie
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "2. Erscheinungsbild & Geometrie",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )

        Card(
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Farbmodus",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                  SegmentedButton(
                    selected = uiState.preferences.themeMode == mode,
                    onClick = { onSelectThemeMode(mode) },
                    shape =
                      SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                      ),
                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}"),
                  ) {
                    Text(text = mode.label, maxLines = 1)
                  }
                }
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Ecken-Geometrie",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CornerStyle.entries.forEachIndexed { index, style ->
                  SegmentedButton(
                    selected = uiState.preferences.cornerStyle == style,
                    onClick = { onSelectCornerStyle(style) },
                    shape =
                      SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = CornerStyle.entries.size,
                      ),
                    modifier = Modifier.testTag("corner_style_${style.name.lowercase()}"),
                  ) {
                    Text(text = style.label, maxLines = 1)
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Gemerkte Aufgaben Schnellzugriff
    if (bookmarkedTasks.isNotEmpty()) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Gemerkte Aufgaben & Meilensteine",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(bookmarkedTasks, key = { it.id }) { task ->
              Card(
                modifier =
                  Modifier.width(230.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable { onOpenTaskDetail(task.id) },
                shape = MaterialTheme.shapes.large,
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                  ),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = task.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${task.code} · ${task.category.label}",
                      style = MonoTokenTextStyle,
                      color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                      maxLines = 1,
                    )
                  }
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. App- & Vorschau-Einstellungen
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "3. System- & Vorschau-Optionen",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Card(
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            StudioToggleRow(
              icon = Icons.Outlined.Business,
              title = "AI-DeviceCare+ Business-Ansicht",
              subtitle = "Zeigt den internen M4-Enterprise-Prototyp im Prüf-Tab",
              checked = uiState.enterprisePreviewMode,
              onCheckedChange = onToggleEnterprisePreview,
              testTag = "pref_enterprise_switch",
            )
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
            StudioToggleRow(
              icon = Icons.Outlined.Compress,
              title = "Kompakte Abstände",
              subtitle = "Reduziert Karten-Padding für dichtere Listen",
              checked = uiState.preferences.compactDensity,
              onCheckedChange = onToggleCompactDensity,
              testTag = "pref_compact_switch",
            )
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
            StudioToggleRow(
              icon = Icons.Outlined.Vibration,
              title = "Haptik-Vorschau",
              subtitle = "Subtiles taktiles Feedback bei Auswahlaktionen",
              checked = uiState.preferences.hapticPreview,
              onCheckedChange = onToggleHapticPreview,
              testTag = "pref_haptic_switch",
            )
          }
        }
      }
    }

    // 5. Typografie & M3 Komponenten Vorschau
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "4. Typografie & M3 Komponenten",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Card(
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Display · Space Grotesk",
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Evidenz vor Urteil",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Body · Plus Jakarta Sans — Skalierbar in sp für 200% Android-Schriftgröße.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Mono · JetBrains Mono — devicecare-observation/1 | TTL = 60_000L",
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { onShowSnackbar("Primäre M3-Aktion bestätigt") },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Primär")
              }
              FilledTonalButton(
                onClick = { showPreviewSheet = true },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  Icons.Filled.Layers,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bottom Sheet")
              }
              OutlinedButton(
                onClick = onResetDefaults,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("reset_defaults_button"),
              ) {
                Icon(
                  Icons.Filled.RestartAlt,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Standarddesign")
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }

  if (showPreviewSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showPreviewSheet = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
          Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
        Text(
          text = "Aktives Theme: ${uiState.preferences.palette.displayName}",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            "Alle Farben, Typografie-Tokens und Ecken-Radien (${uiState.preferences.cornerStyle.label}) sind konsistent auf AI-DeviceCare angewendet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
          onClick = {
            showPreviewSheet = false
            onShowSnackbar("Design-Snapshot übernommen")
          },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        ) {
          Text("Übernehmen")
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun StudioToggleRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(10.dp).size(20.dp),
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag))
  }
}
