package com.example.features.battery

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class BatteryTrendMetric(
  val label: String,
  val unit: String,
) {
  LEVEL("Ladestand", "%"),
  TEMPERATURE("Temperatur", "°C"),
  VOLTAGE("Spannung", "V"),
}

data class BatteryTrendPoint(
  val observedAtMillis: Long,
  val percent: Int?,
  val temperatureCelsius: Float?,
  val voltageMillivolts: Int?,
  val healthCondition: BatteryHealthCondition,
  val chargingStatus: ChargingStatus,
) {
  val isHealthAlert: Boolean
    get() = !healthCondition.isHealthy || (temperatureCelsius != null && temperatureCelsius >= 42f)

  companion object {
    fun fromSnapshot(snapshot: BatteryDashboardSnapshot): BatteryTrendPoint? {
      if (snapshot.errorMessage != null || snapshot.observedAtMillis <= 0L) return null
      val hasAnyNumericMetric =
        snapshot.percent != null ||
          snapshot.temperatureCelsius != null ||
          snapshot.voltageMillivolts != null
      if (!hasAnyNumericMetric) return null
      return BatteryTrendPoint(
        observedAtMillis = snapshot.observedAtMillis,
        percent = snapshot.percent,
        temperatureCelsius = snapshot.temperatureCelsius,
        voltageMillivolts = snapshot.voltageMillivolts,
        healthCondition = snapshot.healthCondition,
        chargingStatus = snapshot.chargingStatus,
      )
    }
  }
}

object BatteryTrendRecorder {
  const val MAX_POINTS = 60

  fun append(
    existing: List<BatteryTrendPoint>,
    snapshot: BatteryDashboardSnapshot,
    maxPoints: Int = MAX_POINTS,
  ): List<BatteryTrendPoint> {
    val point = BatteryTrendPoint.fromSnapshot(snapshot) ?: return existing
    val last = existing.lastOrNull()
    if (
      last != null &&
        last.observedAtMillis == point.observedAtMillis &&
        last.percent == point.percent &&
        last.temperatureCelsius == point.temperatureCelsius &&
        last.voltageMillivolts == point.voltageMillivolts &&
        last.healthCondition == point.healthCondition &&
        last.chargingStatus == point.chargingStatus
    ) {
      return existing
    }
    return (existing + point).takeLast(maxPoints.coerceAtLeast(1))
  }
}

data class ChartAxisScale(
  val minDomain: Float,
  val maxDomain: Float,
  val ticks: List<Float>,
)

data class TrendSeriesStats(
  val minValue: Float,
  val maxValue: Float,
  val latestValue: Float,
  val deltaValue: Float,
  val sampleCount: Int,
)

/**
 * Kontinuierliche Skalen-, Tick- und Statistikberechnung (D3-Skalenprinzip) für die
 * Jetpack-Compose-Canvas-Trendgrafik.
 */
object BatteryTrendChartMath {
  fun extractValue(point: BatteryTrendPoint, metric: BatteryTrendMetric): Float? =
    when (metric) {
      BatteryTrendMetric.LEVEL -> point.percent?.toFloat()
      BatteryTrendMetric.TEMPERATURE -> point.temperatureCelsius
      BatteryTrendMetric.VOLTAGE -> point.voltageMillivolts?.let { it / 1000f }
    }

  fun computeYScale(values: List<Float>, metric: BatteryTrendMetric): ChartAxisScale {
    if (values.isEmpty()) {
      return when (metric) {
        BatteryTrendMetric.LEVEL -> ChartAxisScale(0f, 100f, listOf(0f, 33.3f, 66.7f, 100f))
        BatteryTrendMetric.TEMPERATURE -> ChartAxisScale(15f, 45f, listOf(15f, 25f, 35f, 45f))
        BatteryTrendMetric.VOLTAGE -> ChartAxisScale(3.4f, 4.4f, listOf(3.4f, 3.73f, 4.07f, 4.4f))
      }
    }

    val rawMin = values.minOrNull() ?: 0f
    val rawMax = values.maxOrNull() ?: 100f

    val (domainMin, domainMax) =
      when (metric) {
        BatteryTrendMetric.LEVEL -> {
          val paddedMin = max(0f, floor((rawMin - 5f) / 10f) * 10f)
          val paddedMax = min(100f, ceil((rawMax + 5f) / 10f) * 10f)
          var lo = paddedMin
          var hi = paddedMax
          if (hi - lo < 20f) {
            lo = max(0f, lo - 10f)
            hi = min(100f, lo + 20f)
            if (hi - lo < 20f) lo = max(0f, hi - 20f)
          }
          lo to hi
        }
        BatteryTrendMetric.TEMPERATURE -> {
          var lo = floor((rawMin - 2.5f) / 5f) * 5f
          var hi = ceil((rawMax + 2.5f) / 5f) * 5f
          if (hi - lo < 10f) {
            lo -= 5f
            hi = lo + 10f
          }
          lo to hi
        }
        BatteryTrendMetric.VOLTAGE -> {
          var lo = floor((rawMin - 0.1f) * 5f) / 5f
          var hi = ceil((rawMax + 0.1f) * 5f) / 5f
          if (hi - lo < 0.6f) {
            lo -= 0.2f
            hi = lo + 0.6f
          }
          lo to hi
        }
      }

    val step = (domainMax - domainMin) / 3f
    val ticks = listOf(domainMin, domainMin + step, domainMin + step * 2f, domainMax)
    return ChartAxisScale(minDomain = domainMin, maxDomain = domainMax, ticks = ticks)
  }

  fun mapXNormalized(timestampMillis: Long, minTime: Long, maxTime: Long): Float {
    if (maxTime <= minTime) return 0.5f
    return ((timestampMillis - minTime).toDouble() / (maxTime - minTime).toDouble())
      .toFloat()
      .coerceIn(0f, 1f)
  }

  fun mapYNormalized(value: Float, scale: ChartAxisScale): Float {
    val span = scale.maxDomain - scale.minDomain
    if (span <= 0f) return 0.5f
    return ((value - scale.minDomain) / span).coerceIn(0f, 1f)
  }

  fun computeStats(values: List<Float>): TrendSeriesStats? {
    if (values.isEmpty()) return null
    val minVal = values.minOrNull() ?: return null
    val maxVal = values.maxOrNull() ?: return null
    val firstVal = values.first()
    val latestVal = values.last()
    return TrendSeriesStats(
      minValue = minVal,
      maxValue = maxVal,
      latestValue = latestVal,
      deltaValue = latestVal - firstVal,
      sampleCount = values.size,
    )
  }

  fun formatValue(value: Float, metric: BatteryTrendMetric): String =
    when (metric) {
      BatteryTrendMetric.LEVEL -> String.format(Locale.GERMAN, "%.0f %%", value)
      BatteryTrendMetric.TEMPERATURE -> String.format(Locale.GERMAN, "%.1f °C", value)
      BatteryTrendMetric.VOLTAGE -> String.format(Locale.GERMAN, "%.2f V", value)
    }

  fun formatDelta(delta: Float, metric: BatteryTrendMetric): String {
    val prefix = if (delta > 0f) "+" else ""
    return when (metric) {
      BatteryTrendMetric.LEVEL -> String.format(Locale.GERMAN, "%s%.0f %%", prefix, delta)
      BatteryTrendMetric.TEMPERATURE -> String.format(Locale.GERMAN, "%s%.1f °C", prefix, delta)
      BatteryTrendMetric.VOLTAGE -> String.format(Locale.GERMAN, "%s%.2f V", prefix, delta)
    }
  }
}
