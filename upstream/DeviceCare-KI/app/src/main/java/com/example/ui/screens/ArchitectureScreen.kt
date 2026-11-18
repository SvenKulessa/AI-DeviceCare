package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.evidence.SyntheticFixtureOracle
import com.example.core.quality.CheckStatus
import com.example.core.quality.StatisticalValidation
import com.example.core.scoring.AssessmentStatus
import com.example.ui.ArchitectureTab
import com.example.ui.DeviceCareUiState
import com.example.ui.components.GovernanceStatusBadge
import com.example.ui.theme.MonoTokenTextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ArchitectureScreen(
  uiState: DeviceCareUiState,
  onSelectTab: (ArchitectureTab) -> Unit,
  onToggleQualitySha: () -> Unit,
  onToggleScoringMissing: () -> Unit,
  onSelectFixture: (String) -> Unit,
  onChangeStatisticalFp: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("architecture_screen"),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Architektur & QA-Labor",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text =
            "ADR-0001 Twin-Pipelines, Clopper-Pearson Statistik, SYN-001–009 Orakel, Schutzregeln & Stufenmodell.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          ArchitectureTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
              selected = uiState.selectedArchitectureTab == tab,
              onClick = { onSelectTab(tab) },
              shape =
                SegmentedButtonDefaults.itemShape(
                  index = index,
                  count = ArchitectureTab.entries.size,
                ),
              modifier = Modifier.testTag("arch_tab_${tab.name.lowercase()}"),
            ) {
              Text(text = tab.label, maxLines = 1)
            }
          }
        }
      }
    }

    when (uiState.selectedArchitectureTab) {
      ArchitectureTab.PIPELINES -> {
        item {
          BoxWithCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp)
          ) {
            PipelineASandboxCard(
              uiState = uiState,
              onToggleQualitySha = onToggleQualitySha,
            )
          }
        }
        item {
          BoxWithCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp)
          ) {
            PipelineBSandboxCard(
              uiState = uiState,
              onToggleScoringMissing = onToggleScoringMissing,
            )
          }
        }
        item {
          BoxWithCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp)
          ) {
            StatisticalValidationCard(
              uiState = uiState,
              onChangeFp = onChangeStatisticalFp,
            )
          }
        }
        item {
          BoxWithCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp)
          ) {
            SyntheticOracleCard(
              selectedFixtureId = uiState.selectedFixtureId,
              onSelectFixture = onSelectFixture,
            )
          }
        }
      }

      ArchitectureTab.SAFEGUARDS -> {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_module_layout),
                contentDescription = "Architektur-Komposition",
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentScale = ContentScale.Crop,
              )
            }

            uiState.safeguards.forEach { rule ->
              Card(
                shape = MaterialTheme.shapes.medium,
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                  ),
                border =
                  BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                  ) {
                    Text(
                      text = "${rule.number}",
                      style = MonoTokenTextStyle,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                  }
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = rule.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = rule.summary,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }

      ArchitectureTab.TIERS_MODULES -> {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Gradle Mono-Projekt Module",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.semantics { heading() },
            )
            Card(
              shape = MaterialTheme.shapes.large,
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
              border =
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                uiState.gradleModules.forEachIndexed { idx, mod ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(
                      modifier = Modifier.weight(1f).padding(end = 8.dp),
                      verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                      Text(
                        text = mod.path,
                        style = MonoTokenTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      Text(
                        text = mod.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    GovernanceStatusBadge(status = mod.state)
                  }
                  if (idx < uiState.gradleModules.lastIndex) {
                    HorizontalDivider(
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                  }
                }
              }
            }
          }
        }

        item {
          Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 880.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Produktstufen & Preisplanung (Nicht aktiv)",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text =
                "* Monatliche Zielpreise sind unbestätigte Annahmen ohne aktiviertes Billing oder Steuer-/COGS-Freigabe.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            uiState.tierPlans.forEach { tier ->
              Card(
                shape = MaterialTheme.shapes.large,
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                  ),
                border =
                  BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "${tier.product} · ${tier.tierName}",
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                      shape = MaterialTheme.shapes.small,
                      color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                      Text(
                        text = "${tier.targetPriceMonthly} (${tier.milestone})",
                        style = MonoTokenTextStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      )
                    }
                  }
                  Text(
                    text = tier.scopeSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = tier.statusNote,
                    style = MonoTokenTextStyle,
                    color = MaterialTheme.colorScheme.tertiary,
                  )
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun BoxWithCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(modifier = modifier) { content() }
}

@Composable
private fun PipelineASandboxCard(
  uiState: DeviceCareUiState,
  onToggleQualitySha: () -> Unit,
) {
  val report = uiState.qualityReport
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Pipeline A · QualityPipeline@1",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Kein PASS ohne gültigen 40-stelligen Commit-SHA und geprüfte Checks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (report.status == CheckStatus.PASS) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = report.status.name,
            style = MonoTokenTextStyle,
            color =
              if (report.status == CheckStatus.PASS) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Commit-SHA & Hardware-QA simulieren",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
          checked = uiState.qualitySimulateShaPresent,
          onCheckedChange = { onToggleQualitySha() },
          modifier = Modifier.testTag("pipeline_a_sha_switch"),
        )
      }

      report.checks.forEach { check ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = check.id,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = check.status.name,
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun PipelineBSandboxCard(
  uiState: DeviceCareUiState,
  onToggleScoringMissing: () -> Unit,
) {
  val assessment = uiState.sandboxScoringAssessment
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Pipeline B · DeviceScoringDispatcher@1",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Fail-Closed: Fehlendes oder veraltetes Signal erzwingt NOT_ASSESSABLE.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (assessment.status == AssessmentStatus.ASSESSED)
              MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ) {
          Text(
            text =
              if (assessment.score != null) "${assessment.status.name} (${assessment.score})"
              else assessment.status.name,
            style = MonoTokenTextStyle,
            color =
              if (assessment.status == AssessmentStatus.ASSESSED)
                MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Fehlendes Signal simulieren (Sandbox)",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
          checked = uiState.scoringSimulateMissingSignal,
          onCheckedChange = { onToggleScoringMissing() },
          modifier = Modifier.testTag("pipeline_b_missing_switch"),
        )
      }

      Text(
        text =
          "Abdeckung: ${(assessment.coverage * 100).roundToInt()}% · Fehlend: ${
            if (assessment.missingSignals.isEmpty()) "keine" else assessment.missingSignals.joinToString()
          }",
        style = MonoTokenTextStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun StatisticalValidationCard(
  uiState: DeviceCareUiState,
  onChangeFp: (Int) -> Unit,
) {
  val neg = uiState.statisticalNegativesTotal
  val fp = uiState.statisticalFalsePositives.coerceIn(0, neg)
  val tn = neg - fp
  val counts = StatisticalValidation.Counts(tp = 95L, fp = fp.toLong(), tn = tn.toLong(), fn = 5L)
  val fpr = StatisticalValidation.fpr(counts) ?: 0.0
  val upper95 = StatisticalValidation.upperFpr95(counts) ?: 1.0
  val passesM2Gate = StatisticalValidation.malwareFprTargets(counts)

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Science,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Statistische Validierung (Clopper-Pearson 95%)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text =
          "M2-Gate fordert FPR <= 0,50% und obere 95%-Konfidenzgrenze <= 1,00% (bei N=$neg Negativen).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Text(
        text = "Simulierte False Positives (FP): $fp von $neg",
        style = MonoTokenTextStyle,
        color = MaterialTheme.colorScheme.primary,
      )
      Slider(
        value = fp.toFloat(),
        onValueChange = { onChangeFp(it.roundToInt()) },
        valueRange = 0f..15f,
        steps = 14,
        modifier = Modifier.fillMaxWidth().testTag("stat_fp_slider"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "FPR: ${String.format(Locale.ROOT, "%.2f%%", fpr * 100.0)}",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "upperFpr95: ${String.format(Locale.ROOT, "%.2f%%", upper95 * 100.0)}",
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            if (passesM2Gate) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ) {
          Text(
            text = if (passesM2Gate) "M2 GATE PASS" else "M2 GATE FAIL",
            style = MonoTokenTextStyle,
            color =
              if (passesM2Gate) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SyntheticOracleCard(
  selectedFixtureId: String,
  onSelectFixture: (String) -> Unit,
) {
  val selected =
    SyntheticFixtureOracle.referenceCases.find { it.id == selectedFixtureId }
      ?: SyntheticFixtureOracle.referenceCases.first()
  val gate = SyntheticFixtureOracle.assessFixture(selected)

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Referenz-Orakel SYN-001–SYN-009 (Nur Test-Fixtures)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text =
          "Isolierter Referenzvertrag für Issue #13 (${SyntheticFixtureOracle.SCHEMA}). Kein echter Malware-Scanner.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SyntheticFixtureOracle.referenceCases) { fixture ->
          FilterChip(
            selected = fixture.id == selected.id,
            onClick = { onSelectFixture(fixture.id) },
            label = { Text(fixture.id, style = MonoTokenTextStyle) },
            modifier = Modifier.testTag("fixture_chip_${fixture.id.lowercase()}"),
          )
        }
      }

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {},
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = selected.id,
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.primary,
            )
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = gate.label,
                style = MonoTokenTextStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
          Text(
            text = selected.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              "coverage=${selected.coverage} · fresh=${selected.evidenceFresh} · perms=${selected.permissions.size} · events=${selected.events.size}",
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
