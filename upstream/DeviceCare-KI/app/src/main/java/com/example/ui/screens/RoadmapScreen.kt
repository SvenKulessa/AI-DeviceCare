package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GovernanceStatus
import com.example.model.PlanCategory
import com.example.ui.DeviceCareUiState
import com.example.ui.components.MetricRingCard
import com.example.ui.components.RoadmapTaskCard
import com.example.ui.theme.MonoTokenTextStyle

@Composable
fun RoadmapScreen(
  uiState: DeviceCareUiState,
  onSearchQueryChange: (String) -> Unit,
  onSelectCategory: (PlanCategory) -> Unit,
  onSelectStatusFilter: (GovernanceStatus?) -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onToggleBookmark: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val compact = uiState.preferences.compactDensity

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns = if (maxWidth >= 640.dp) 2 else 1

    Column(
      modifier = Modifier.fillMaxSize().testTag("roadmap_screen"),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 920.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Aufgaben & Planungen",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text =
            "Meilensteine M0–M5, P0–P2 Behebungen, Android 15 QA-Matrix & Berechtigungs-Gates.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = onSearchQueryChange,
          modifier = Modifier.fillMaxWidth().testTag("roadmap_search_input"),
          placeholder = { Text("Nach M1, QA-04, VpnService oder Aufgabe suchen...") },
          leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_search_button"),
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
          items(PlanCategory.entries) { category ->
            FilterChip(
              selected = category == uiState.selectedCategory,
              onClick = { onSelectCategory(category) },
              label = { Text(category.label) },
              modifier = Modifier.testTag("plan_cat_${category.name.lowercase()}"),
            )
          }
        }

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 2.dp),
        ) {
          items(GovernanceStatus.entries) { status ->
            FilterChip(
              selected = uiState.selectedStatusFilter == status,
              onClick = { onSelectStatusFilter(status) },
              label = { Text(status.label, style = MonoTokenTextStyle) },
              modifier = Modifier.testTag("status_filter_${status.name.lowercase()}"),
            )
          }
        }
      }

      if (uiState.filteredTasks.isEmpty()) {
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
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(R.string.empty_search_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.empty_search_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(columns),
          modifier = Modifier.fillMaxWidth().widthIn(max = 920.dp).weight(1f),
          contentPadding = PaddingValues(16.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
          item(span = { GridItemSpan(columns) }) {
            RoadmapSummaryRow(uiState = uiState)
          }

          items(uiState.filteredTasks, key = { it.id }) { task ->
            val locallyVerified = task.id in uiState.preferences.localVerifiedTaskIds
            val effectiveStatus =
              if (locallyVerified) GovernanceStatus.VERIFIED else task.status
            RoadmapTaskCard(
              task = task,
              effectiveStatus = effectiveStatus,
              isBookmarked = task.id in uiState.preferences.bookmarkedTaskIds,
              isLocallyVerified = locallyVerified,
              compact = compact,
              onClick = { onOpenTaskDetail(task.id) },
              onToggleBookmark = { onToggleBookmark(task.id) },
            )
          }

          item(span = { GridItemSpan(columns) }) { Spacer(modifier = Modifier.height(12.dp)) }
        }
      }
    }
  }
}

@Composable
private fun RoadmapSummaryRow(uiState: DeviceCareUiState) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    MetricRingCard(
      title = "Plan-Fortschritt",
      progress = uiState.readinessFraction,
      subtitle = "${uiState.verifiedCount}/${uiState.allTasks.size} VERIFIED",
      modifier = Modifier.weight(1f),
    )
    Card(
      modifier = Modifier.weight(1.25f),
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
          text = "Android 15 Geräte-QA",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
        )
        Text(
          text = "${uiState.qaCompletedCount} / ${uiState.qaTotalCount} Tests",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text =
            "Tippe auf eine Aufgabe, um Akzeptanzkriterien, Blocker & lokale QA-Prüfung zu verwalten.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}
