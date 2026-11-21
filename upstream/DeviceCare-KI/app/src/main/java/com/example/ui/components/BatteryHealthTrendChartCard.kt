package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.features.battery.BatteryTrendChartMath
import com.example.features.battery.BatteryTrendMetric
import com.example.features.battery.BatteryTrendPoint
import com.example.ui.theme.MonoTokenTextStyle
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class PlottedBatteryPoint(
  val point: BatteryTrendPoint,
  val value: Float,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatteryHealthTrendChartCard(
  history: List<BatteryTrendPoint>,
  modifier: Modifier = Modifier,
) {
  var selectedMetric by remember { mutableStateOf(BatteryTrendMetric.LEVEL) }
  var inspectedIndex by remember(history.size, selectedMetric) { mutableStateOf<Int?>(null) }

  val plottedPoints =
    remember(history, selectedMetric) {
      history.mapNotNull { point ->
        BatteryTrendChartMath.extractValue(point, selectedMetric)?.let { value ->
          PlottedBatteryPoint(point = point, value = value)
        }
      }
    }

  val values = remember(plottedPoints) { plottedPoints.map { it.value } }
  val yScale =
    remember(values, selectedMetric) { BatteryTrendChartMath.computeYScale(values, selectedMetric) }
  val stats = remember(values) { BatteryTrendChartMath.computeStats(values) }
  val latestHealthAlert = plottedPoints.lastOrNull()?.point?.isHealthAlert == true

  val lineColor =
    when {
      latestHealthAlert -> MaterialTheme.colorScheme.error
      selectedMetric == BatteryTrendMetric.TEMPERATURE -> MaterialTheme.colorScheme.tertiary
      selectedMetric == BatteryTrendMetric.VOLTAGE -> MaterialTheme.colorScheme.secondary
      else -> MaterialTheme.colorScheme.primary
    }
  val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
  val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceBgColor = MaterialTheme.colorScheme.surfaceContainerLow
  val alertDotColor = MaterialTheme.colorScheme.error
  val textMeasurer = rememberTextMeasurer()
  val axisTextStyle = MaterialTheme.typography.labelSmall.copy(color = axisLabelColor)

  Card(
    modifier = modifier.fillMaxWidth().testTag("battery_health_trend_card"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = surfaceBgColor),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
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
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ShowChart,
            contentDescription = null,
            tint = lineColor,
          )
          Column {
            Text(
              text = "Akku-Zustand & Verlauf",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text = "Live-Trend aus BatteryManager",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
          Text(
            text = "${plottedPoints.size} Messpunkte",
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier =
              Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("battery_trend_sample_count"),
          )
        }
      }

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
      ) {
        items(BatteryTrendMetric.entries) { metric ->
          val selected = metric == selectedMetric
          FilterChip(
            selected = selected,
            onClick = { selectedMetric = metric },
            label = { Text("${metric.label} (${metric.unit})") },
            leadingIcon =
              if (selected) {
                {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                }
              } else null,
            modifier = Modifier.testTag("trend_metric_${metric.name.lowercase(Locale.ROOT)}"),
          )
        }
      }

      if (plottedPoints.isEmpty()) {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("battery_trend_empty_state"),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Timeline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
            Text(
              text = "Noch keine Messreihe für ${selectedMetric.label} erfasst",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                "Tippe oben auf Aktualisieren oder lass das Dashboard geöffnet — jede Live-Änderung von Android BatteryManager ergänzt den Verlauf.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        if (stats != null) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            TrendStatChip(
              label = "Aktuell",
              value = BatteryTrendChartMath.formatValue(stats.latestValue, selectedMetric),
            )
            TrendStatChip(
              label = "Min / Max",
              value =
                "${BatteryTrendChartMath.formatValue(stats.minValue, selectedMetric)} – ${
                  BatteryTrendChartMath.formatValue(stats.maxValue, selectedMetric)
                }",
            )
            TrendStatChip(
              label = "Verlauf",
              value = BatteryTrendChartMath.formatDelta(stats.deltaValue, selectedMetric),
            )
            plottedPoints.lastOrNull()?.point?.let { latest ->
              TrendStatChip(
                label = "Zustand",
                value = latest.healthCondition.label,
                highlightError = latest.isHealthAlert,
              )
            }
          }
        }

        val activePoint =
          inspectedIndex?.let { idx -> plottedPoints.getOrNull(idx) } ?: plottedPoints.last()
        val timeFormatter =
          remember { DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.GERMAN) }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.fillMaxWidth(),
        ) {
          val timestampText = timeFormatter.format(Date(activePoint.point.observedAtMillis))
          val valueText = BatteryTrendChartMath.formatValue(activePoint.value, selectedMetric)
          Text(
            text =
              "${if (inspectedIndex != null) "Ausgewählt" else "Letzte Messung"} ($timestampText): $valueText · ${activePoint.point.chargingStatus.label} · Zustand: ${activePoint.point.healthCondition.label}",
            style = MonoTokenTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
              Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("battery_trend_active_point_banner"),
          )
        }

        val minTime = plottedPoints.first().point.observedAtMillis
        val maxTime = plottedPoints.last().point.observedAtMillis

        fun pickNearestPoint(touchX: Float, plotLeft: Float, plotWidth: Float) {
          if (plottedPoints.isEmpty() || plotWidth <= 0f) return
          val clampedFraction = ((touchX - plotLeft) / plotWidth).coerceIn(0f, 1f)
          var bestIndex = 0
          var bestDistance = Float.MAX_VALUE
          plottedPoints.forEachIndexed { index, item ->
            val xFrac =
              BatteryTrendChartMath.mapXNormalized(item.point.observedAtMillis, minTime, maxTime)
            val distance = abs(xFrac - clampedFraction)
            if (distance < bestDistance) {
              bestDistance = distance
              bestIndex = index
            }
          }
          inspectedIndex = bestIndex
        }

        Box(modifier = Modifier.fillMaxWidth()) {
          Canvas(
            modifier =
              Modifier.fillMaxWidth()
                .height(210.dp)
                .testTag("battery_trend_chart_canvas")
                .pointerInput(plottedPoints, minTime, maxTime) {
                  detectTapGestures { offset ->
                    val leftPad = 48.dp.toPx()
                    val rightPad = 12.dp.toPx()
                    val plotW = (size.width.toFloat() - leftPad - rightPad).coerceAtLeast(1f)
                    pickNearestPoint(offset.x, leftPad, plotW)
                  }
                }
                .pointerInput(plottedPoints, minTime, maxTime) {
                  detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val leftPad = 48.dp.toPx()
                    val rightPad = 12.dp.toPx()
                    val plotW = (size.width.toFloat() - leftPad - rightPad).coerceAtLeast(1f)
                    pickNearestPoint(change.position.x, leftPad, plotW)
                  }
                }
          ) {
            val leftPadding = 48.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 12.dp.toPx()
            val bottomPadding = 26.dp.toPx()

            val plotWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
            val plotHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val plotRight = leftPadding + plotWidth
            val plotBottom = topPadding + plotHeight

            val gridDash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // Horizontal grid lines + Y-axis labels
            yScale.ticks.forEach { tickValue ->
              val yNorm = BatteryTrendChartMath.mapYNormalized(tickValue, yScale)
              val yPos = plotBottom - (yNorm * plotHeight)

              drawLine(
                color = gridColor,
                start = Offset(leftPadding, yPos),
                end = Offset(plotRight, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridDash,
              )

              val labelText = BatteryTrendChartMath.formatValue(tickValue, selectedMetric)
              val measured = textMeasurer.measure(labelText, style = axisTextStyle)
              drawText(
                textLayoutResult = measured,
                topLeft =
                  Offset(
                    x = (leftPadding - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                    y = (yPos - measured.size.height / 2f).coerceIn(0f, plotBottom - measured.size.height),
                  ),
              )
            }

            // Threshold reference line (42°C thermal warning or 15% low battery)
            val thresholdValue =
              when (selectedMetric) {
                BatteryTrendMetric.TEMPERATURE -> 42f
                BatteryTrendMetric.LEVEL -> 15f
                BatteryTrendMetric.VOLTAGE -> null
              }
            if (
              thresholdValue != null &&
                thresholdValue in yScale.minDomain..yScale.maxDomain
            ) {
              val yNorm = BatteryTrendChartMath.mapYNormalized(thresholdValue, yScale)
              val yPos = plotBottom - (yNorm * plotHeight)
              drawLine(
                color = alertDotColor.copy(alpha = 0.65f),
                start = Offset(leftPadding, yPos),
                end = Offset(plotRight, yPos),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f),
              )
            }

            // Map data points to screen coordinates
            val coords =
              plottedPoints.map { item ->
                val xNorm =
                  BatteryTrendChartMath.mapXNormalized(item.point.observedAtMillis, minTime, maxTime)
                val yNorm = BatteryTrendChartMath.mapYNormalized(item.value, yScale)
                Offset(
                  x = leftPadding + xNorm * plotWidth,
                  y = plotBottom - yNorm * plotHeight,
                )
              }

            if (coords.size >= 2) {
              val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.lastIndex) {
                  val p0 = coords[i]
                  val p1 = coords[i + 1]
                  val midX = (p0.x + p1.x) / 2f
                  cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
              }
              val areaPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, plotBottom)
                lineTo(coords.first().x, plotBottom)
                close()
              }
              drawPath(
                path = areaPath,
                brush =
                  Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                    startY = topPadding,
                    endY = plotBottom,
                  ),
              )
              drawPath(
                path = linePath,
                color = lineColor,
                style =
                  Stroke(
                    width = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                  ),
              )
            }

            // Crosshair for selected point
            val highlightIdx =
              inspectedIndex?.takeIf { it in coords.indices } ?: coords.lastIndex
            val highlightCoord = coords[highlightIdx]
            drawLine(
              color = lineColor.copy(alpha = 0.45f),
              start = Offset(highlightCoord.x, topPadding),
              end = Offset(highlightCoord.x, plotBottom),
              strokeWidth = 1.dp.toPx(),
              pathEffect = gridDash,
            )

            // Draw data markers
            coords.forEachIndexed { index, pt ->
              val isAlert = plottedPoints[index].point.isHealthAlert
              val dotColor = if (isAlert) alertDotColor else lineColor
              val isHighlighted = index == highlightIdx
              if (isHighlighted) {
                drawCircle(
                  color = dotColor.copy(alpha = 0.22f),
                  radius = 9.dp.toPx(),
                  center = pt,
                )
              }
              drawCircle(
                color = surfaceBgColor,
                radius = if (isHighlighted) 5.dp.toPx() else 3.8.dp.toPx(),
                center = pt,
              )
              drawCircle(
                color = dotColor,
                radius = if (isHighlighted) 3.5.dp.toPx() else 2.5.dp.toPx(),
                center = pt,
              )
            }

            // X-axis first and latest timestamps
            val shortTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN)
            val startLabel = shortTime.format(Date(minTime))
            val startMeasured = textMeasurer.measure(startLabel, style = axisTextStyle)
            drawText(
              textLayoutResult = startMeasured,
              topLeft = Offset(leftPadding, plotBottom + 6.dp.toPx()),
            )
            if (maxTime > minTime) {
              val endLabel = shortTime.format(Date(maxTime))
              val endMeasured = textMeasurer.measure(endLabel, style = axisTextStyle)
              drawText(
                textLayoutResult = endMeasured,
                topLeft = Offset(plotRight - endMeasured.size.width, plotBottom + 6.dp.toPx()),
              )
            }
          }
        }

        if (plottedPoints.size == 1) {
          Text(
            text =
              "1 Messpunkt erfasst. Tippe auf Aktualisieren oder halte den Akku-Monitor offen, um die Trendlinie über weitere Zeitpunkte fortzuschreiben.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text =
          "Verlauf aus lokalen Live-Messwerten (max. 60 Punkte im RAM, keine Persistenz, keine simulierten Daten).",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun TrendStatChip(
  label: String,
  value: String,
  highlightError: Boolean = false,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color =
      if (highlightError) MaterialTheme.colorScheme.errorContainer
      else MaterialTheme.colorScheme.surfaceContainerHighest,
  ) {
    Text(
      text = "$label: $value",
      style = MonoTokenTextStyle,
      color =
        if (highlightError) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}
