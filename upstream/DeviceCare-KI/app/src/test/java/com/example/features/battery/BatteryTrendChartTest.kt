package com.example.features.battery

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.ui.DeviceCareUiState
import com.example.ui.screens.BatteryDashboardScreen
import com.example.ui.theme.FormwerkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BatteryTrendChartTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `batteryTrendRecorder ignores errors deduplicates identical snapshots and enforces max points`() {
    val errorSnapshot =
      BatteryDashboardSnapshot(
        observedAtMillis = 1_000L,
        percent = 80,
        errorMessage = "Read failed",
      )
    val initial = BatteryTrendRecorder.append(emptyList(), errorSnapshot)
    assertTrue(initial.isEmpty())

    val first =
      BatteryDashboardSnapshot(
        observedAtMillis = 10_000L,
        percent = 82,
        temperatureCelsius = 29.4f,
        voltageMillivolts = 4110,
        healthCondition = BatteryHealthCondition.GOOD,
        chargingStatus = ChargingStatus.DISCHARGING,
      )
    val afterFirst = BatteryTrendRecorder.append(initial, first)
    assertEquals(1, afterFirst.size)

    // Duplicate timestamp & values should not duplicate entry
    val afterDuplicate = BatteryTrendRecorder.append(afterFirst, first)
    assertEquals(1, afterDuplicate.size)

    // Bounded capacity cap
    var series = emptyList<BatteryTrendPoint>()
    for (i in 1..70) {
      series =
        BatteryTrendRecorder.append(
          series,
          first.copy(observedAtMillis = i * 1_000L, percent = (100 - (i % 50))),
          maxPoints = 60,
        )
    }
    assertEquals(60, series.size)
    assertEquals(11_000L, series.first().observedAtMillis)
    assertEquals(70_000L, series.last().observedAtMillis)
  }

  @Test
  fun `batteryTrendChartMath computes normalized scales and statistics accurately`() {
    val levelScale = BatteryTrendChartMath.computeYScale(listOf(72f, 84f), BatteryTrendMetric.LEVEL)
    assertEquals(4, levelScale.ticks.size)
    assertTrue(levelScale.minDomain <= 70f)
    assertTrue(levelScale.maxDomain >= 90f)

    val tempScale =
      BatteryTrendChartMath.computeYScale(listOf(28.2f, 36.8f), BatteryTrendMetric.TEMPERATURE)
    assertEquals(4, tempScale.ticks.size)
    assertTrue(tempScale.minDomain <= 25f)
    assertTrue(tempScale.maxDomain >= 40f)

    assertEquals(0.5f, BatteryTrendChartMath.mapXNormalized(5_000L, 5_000L, 5_000L), 0.001f)
    assertEquals(0.5f, BatteryTrendChartMath.mapXNormalized(15_000L, 10_000L, 20_000L), 0.001f)

    val stats = BatteryTrendChartMath.computeStats(listOf(80f, 76f, 82f))
    assertNotNull(stats)
    assertEquals(76f, stats!!.minValue, 0.001f)
    assertEquals(82f, stats.maxValue, 0.001f)
    assertEquals(82f, stats.latestValue, 0.001f)
    assertEquals(2f, stats.deltaValue, 0.001f)
    assertEquals(3, stats.sampleCount)

    assertNull(BatteryTrendChartMath.computeStats(emptyList()))
  }

  @Test
  fun `batteryDashboardScreen renders empty trend state and switches chart metrics cleanly`() {
    val emptyUiState = androidx.compose.runtime.mutableStateOf(DeviceCareUiState())

    composeTestRule.setContent {
      FormwerkTheme {
        BatteryDashboardScreen(
          uiState = emptyUiState.value,
          onBack = {},
          onRefresh = {},
          onShowSnackbar = {},
        )
      }
    }

    composeTestRule
      .onNodeWithTag("battery_dashboard_list")
      .performScrollToNode(hasTestTag("battery_health_trend_card"))
    composeTestRule.onNodeWithTag("battery_trend_empty_state").assertExists()

    val history =
      listOf(
        BatteryTrendPoint(
          observedAtMillis = 1_700_000_000_000L,
          percent = 80,
          temperatureCelsius = 28.5f,
          voltageMillivolts = 4080,
          healthCondition = BatteryHealthCondition.GOOD,
          chargingStatus = ChargingStatus.CHARGING,
        ),
        BatteryTrendPoint(
          observedAtMillis = 1_700_000_060_000L,
          percent = 83,
          temperatureCelsius = 30.2f,
          voltageMillivolts = 4150,
          healthCondition = BatteryHealthCondition.GOOD,
          chargingStatus = ChargingStatus.CHARGING,
        ),
        BatteryTrendPoint(
          observedAtMillis = 1_700_000_120_000L,
          percent = 86,
          temperatureCelsius = 31.0f,
          voltageMillivolts = 4210,
          healthCondition = BatteryHealthCondition.GOOD,
          chargingStatus = ChargingStatus.CHARGING,
        ),
      )

    emptyUiState.value =
      DeviceCareUiState(
        batterySnapshot =
          BatteryDashboardSnapshot(
            observedAtMillis = 1_700_000_120_000L,
            percent = 86,
            chargingStatus = ChargingStatus.CHARGING,
            healthCondition = BatteryHealthCondition.GOOD,
            temperatureCelsius = 31.0f,
            voltageMillivolts = 4210,
          ),
        batteryTrendHistory = history,
      )
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("battery_trend_chart_canvas").assertExists()
    composeTestRule
      .onNodeWithTag("battery_trend_sample_count")
      .assertTextContains("3 Messpunkte")

    composeTestRule.onNodeWithTag("trend_metric_temperature").performScrollTo().performClick()
    composeTestRule
      .onNodeWithTag("battery_trend_active_point_banner")
      .assertTextContains("31,0 °C", substring = true)

    composeTestRule.onNodeWithTag("trend_metric_voltage").performScrollTo().performClick()
    composeTestRule
      .onNodeWithTag("battery_trend_active_point_banner")
      .assertTextContains("4,21 V", substring = true)
  }
}
