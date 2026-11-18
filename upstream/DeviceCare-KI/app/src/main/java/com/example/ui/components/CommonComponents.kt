package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.GovernanceStatus
import com.example.model.RoadmapTask
import com.example.ui.theme.MonoTokenTextStyle
import com.example.ui.theme.ThemePalette
import com.example.ui.theme.previewSwatches
import kotlin.math.roundToInt

@Composable
fun MetricRingCard(
  title: String,
  progress: Float,
  centerText: String? = null,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  val animatedProgress by
    animateFloatAsState(
      targetValue = progress.coerceIn(0f, 1f),
      animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
      label = "ring_progress",
    )

  val primaryColor = MaterialTheme.colorScheme.primary
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

  Card(
    modifier = modifier.testTag("metric_ring_card"),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val strokeWidth = 8.dp.toPx()
          drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
          drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )
        }
        Text(
          text = centerText ?: "${(animatedProgress * 100).roundToInt()}%",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = subtitle,
        style = MonoTokenTextStyle,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun GovernanceStatusBadge(
  status: GovernanceStatus,
  modifier: Modifier = Modifier,
) {
  val (bg, fg) =
    when (status) {
      GovernanceStatus.VERIFIED ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
      GovernanceStatus.IN_PROGRESS ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
      GovernanceStatus.NOT_PROVEN ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
      GovernanceStatus.BLOCKED ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
      GovernanceStatus.REVIEW_REQUIRED ->
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }

  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    color = bg,
  ) {
    Text(
      text = status.label,
      style = MonoTokenTextStyle,
      color = fg,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      maxLines = 1,
    )
  }
}

@Composable
fun RoadmapTaskCard(
  task: RoadmapTask,
  effectiveStatus: GovernanceStatus,
  isBookmarked: Boolean,
  isLocallyVerified: Boolean,
  compact: Boolean,
  onClick: () -> Unit,
  onToggleBookmark: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("task_card_${task.id}")
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(if (compact) 14.dp else 18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
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
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
          ) {
            Text(
              text = task.code,
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          GovernanceStatusBadge(status = effectiveStatus)
          if (isLocallyVerified) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = "Lokal geprüft",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        IconButton(
          onClick = onToggleBookmark,
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("task_bookmark_${task.id}"),
        ) {
          Icon(
            imageVector =
              if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isBookmarked) "Lesezeichen entfernen" else "Aufgabe merken",
            tint =
              if (isBookmarked) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text = task.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = task.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(2.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = task.owner,
          style = MonoTokenTextStyle,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (task.blockingRelease) "Release-Blocker" else task.category.label,
          style = MaterialTheme.typography.labelSmall,
          color =
            if (task.blockingRelease) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun PaletteSwatchCard(
  palette: ThemePalette,
  selected: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
      label = "palette_border",
    )
  val containerColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
      label = "palette_bg",
    )

  Card(
    modifier =
      modifier
        .width(210.dp)
        .testTag("palette_card_${palette.name.lowercase()}")
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onSelect),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
          palette.previewSwatches().forEach { swatch ->
            Box(
              modifier =
                Modifier.size(28.dp)
                  .clip(CircleShape)
                  .background(swatch)
                  .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
          }
        }
        if (selected) {
          Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = "Aktive Palette",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(4.dp).size(16.dp),
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = palette.displayName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = palette.tagline,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
