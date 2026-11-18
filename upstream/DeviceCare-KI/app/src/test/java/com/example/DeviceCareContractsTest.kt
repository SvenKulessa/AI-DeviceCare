package com.example

import com.example.core.evidence.Availability
import com.example.core.evidence.DeviceEvidence
import com.example.core.evidence.DeviceObservationBatch
import com.example.core.evidence.ObservationContract
import com.example.core.evidence.ObservationStatus
import com.example.core.evidence.RiskLevel
import com.example.core.evidence.SyntheticFixtureOracle
import com.example.core.evidence.measuredObservation
import com.example.core.evidence.unavailableObservation
import com.example.core.quality.CheckStatus
import com.example.core.quality.QualityCheck
import com.example.core.quality.QualityPipeline
import com.example.core.quality.StatisticalValidation
import com.example.core.scoring.AssessmentStatus
import com.example.core.scoring.DeviceModel
import com.example.core.scoring.DeviceScoringDispatcher
import com.example.core.scoring.StaticDeviceModelRegistry
import android.content.pm.PackageInfo
import android.os.BatteryManager
import com.example.ai.AiAdvisorViewModel
import com.example.ai.GeminiPermissionPrivacyAnalyzerService
import com.example.features.battery.BatteryDashboardSnapshot
import com.example.features.battery.BatteryEvidenceMapper
import com.example.features.battery.BatteryHealthCondition
import com.example.features.battery.BatteryHealthEvaluator
import com.example.features.battery.BatteryReading
import com.example.features.battery.BatteryTelemetryRaw
import com.example.features.battery.ChargingStatus
import com.example.features.battery.PowerSource
import com.example.features.battery.RemainingTimeEstimate
import com.example.features.battery.RemainingTimeSource
import com.example.features.permissions.AppSectionCategory
import com.example.features.permissions.GrantedDangerousPermission
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.PermissionScanBudget
import com.example.features.permissions.PermissionScanProfile
import com.example.features.permissions.PermissionSensitivityClassifier
import com.example.features.permissions.ResourceSignal
import com.example.features.permissions.SensitivityCategory
import com.example.model.DeviceCareRoadmapData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCareContractsTest {
  @Test
  fun observationContractRejectsPiiAndExpiredValues() {
    val now = 1_000L
    val obs = measuredObservation("sampled_packages", "42", "apps", now, "ANDROID_VISIBILITY_FILTERED")
    assertTrue(obs.isFresh(now + 10_000L))
    assertFalse(obs.isFresh(now + ObservationContract.MAX_AGE_MILLIS + 1L))

    assertThrows(IllegalArgumentException::class.java) {
      measuredObservation("visible_packages", "com.example.secret", "apps", now, "NONE")
    }
    val unavailable = unavailableObservation("battery_percent", ObservationStatus.UNSUPPORTED, now, "OEM_UNAVAILABLE")
    assertNull(unavailable.value)
    assertThrows(IllegalArgumentException::class.java) {
      DeviceObservationBatch("malware", now, "LOCAL_MALWARE", listOf(unavailable))
    }
  }

  @Test
  fun qualityPipelineFailClosesWithoutShaOrPassingChecks() {
    val sha = "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678"
    assertEquals(
      CheckStatus.NOT_PROVEN,
      QualityPipeline.assess(listOf(QualityCheck("unit", CheckStatus.PASS)), null).status,
    )
    assertEquals(
      CheckStatus.FAIL,
      QualityPipeline.assess(listOf(QualityCheck("lint", CheckStatus.FAIL)), sha).status,
    )
    assertEquals(
      CheckStatus.PASS,
      QualityPipeline.assess(listOf(QualityCheck("unit", CheckStatus.PASS)), sha).status,
    )
  }

  @Test
  fun deviceScoringDispatcherFailsClosedOnMissingOrStaleEvidence() {
    val registry =
      StaticDeviceModelRegistry(
        mapOf("ref" to DeviceModel("ref", "1.0.0", setOf("battery_state", "permission_posture")))
      )
    val dispatcher = DeviceScoringDispatcher(registry)
    val partial =
      listOf(
        DeviceEvidence("battery_state", "test", Availability.OBSERVED, 100L, 60_000L, RiskLevel.NONE)
      )
    val missingResult = dispatcher.assess("ref", partial, 500L)
    assertEquals(AssessmentStatus.NOT_ASSESSABLE, missingResult.status)
    assertNull(missingResult.score)

    val full =
      partial +
        DeviceEvidence("permission_posture", "test", Availability.OBSERVED, 100L, 60_000L, RiskLevel.LOW)
    val assessed = dispatcher.assess("ref", full, 500L)
    assertEquals(AssessmentStatus.ASSESSED, assessed.status)
    assertEquals(95, assessed.score)
  }

  @Test
  fun permissionScanBudgetAppliesConservative250_75_25Caps() {
    val clear =
      PermissionScanProfile(
        apiLevel = 35,
        observedAtMillis = 1_000L,
        lowRam = ResourceSignal.CLEAR,
        memoryPressure = ResourceSignal.CLEAR,
        powerSaver = ResourceSignal.CLEAR,
        thermal = ResourceSignal.CLEAR,
      )
    assertEquals(250, PermissionScanBudget.forProfile(clear).packageLimit)
    assertEquals(75, PermissionScanBudget.forProfile(clear.copy(powerSaver = ResourceSignal.ACTIVE)).packageLimit)
    assertEquals(25, PermissionScanBudget.forProfile(clear.copy(thermal = ResourceSignal.HIGH)).packageLimit)
  }

  @Test
  fun batteryMapperHandlesValidAndUnavailableSignals() {
    val batch =
      BatteryEvidenceMapper.map(
        1_000L,
        BatteryReading(level = 75, scale = 100, capacityPercent = null, temperatureTenthsCelsius = 315, chargingStatus = 2, healthCode = 2),
      )
    assertEquals("75", batch.observations.first { it.id == "battery_percent" }.value)
    assertEquals("31.5", batch.observations.first { it.id == "battery_temperature" }.value)
    assertEquals("CHARGING", batch.observations.first { it.id == "battery_charging" }.value)
    assertEquals("GOOD", batch.observations.first { it.id == "battery_health_reported" }.value)
  }

  @Test
  fun allNineSyntheticFixturesMatchExpectedGates() {
    assertEquals(9, SyntheticFixtureOracle.referenceCases.size)
    SyntheticFixtureOracle.referenceCases.forEach { fixture ->
      assertEquals(fixture.id, fixture.expected, SyntheticFixtureOracle.assessFixture(fixture))
    }
  }

  @Test
  fun statisticalValidationComputesUpperFpr95Target() {
    val passCounts = StatisticalValidation.Counts(tp = 95, fp = 1, tn = 499, fn = 5)
    assertTrue(StatisticalValidation.malwareFprTargets(passCounts))
    val failCounts = StatisticalValidation.Counts(tp = 95, fp = 4, tn = 496, fn = 5)
    assertFalse(StatisticalValidation.malwareFprTargets(failCounts))
  }

  @Test
  fun roadmapDataContainsMilestonesAndAllEightAndroid15QaTasks() {
    assertEquals(6, DeviceCareRoadmapData.tasks.count { it.code.startsWith("M") })
    assertEquals(8, DeviceCareRoadmapData.tasks.count { it.code.startsWith("QA-") })
    assertEquals(10, DeviceCareRoadmapData.safeguards.size)
  }

  @Test
  fun permissionSensitivityClassifierFiltersGrantedDangerousAndFlagsCameraLocation() {
    val dangerousSet =
      setOf(
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CALENDAR",
      )
    val requested =
      arrayOf(
        "android.permission.INTERNET",
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CALENDAR",
      )
    val flags =
      intArrayOf(
        PackageInfo.REQUESTED_PERMISSION_GRANTED, // Normal permission -> excluded
        PackageInfo.REQUESTED_PERMISSION_GRANTED, // Granted dangerous CAMERA
        PackageInfo.REQUESTED_PERMISSION_GRANTED, // Granted dangerous LOCATION
        0, // Denied RECORD_AUDIO -> excluded
        PackageInfo.REQUESTED_PERMISSION_GRANTED, // Granted dangerous CALENDAR (non-high-sensitivity)
      )

    val granted =
      PermissionSensitivityClassifier.buildGrantedDangerousList(
        requestedPermissions = requested,
        requestedPermissionsFlags = flags,
        isDangerousResolver = { it in dangerousSet },
        descriptionResolver = { "desc:$it" },
      )

    assertEquals(3, granted.size)
    assertEquals(
      listOf("CAMERA", "ACCESS_FINE_LOCATION", "READ_CALENDAR"),
      granted.map { it.shortName },
    )
    assertTrue(granted[0].category == SensitivityCategory.CAMERA && granted[0].category.isHighSensitivity)
    assertTrue(granted[1].category == SensitivityCategory.LOCATION && granted[1].category.isHighSensitivity)
    assertFalse(granted[2].category.isHighSensitivity)

    val appInfo =
      InstalledAppPermissionInfo(
        packageName = "com.example.cam",
        appName = "Cam",
        isSystemApp = false,
        totalRequestedPermissions = requested.size,
        grantedDangerousPermissions = granted,
      )
    val scanResult =
      InstalledAppsScanResult(
        scannedAtMillis = 1_000L,
        visiblePackageCount = 1,
        apps = listOf(appInfo),
      )
    assertTrue(appInfo.hasHighSensitivityAccess)
    assertEquals(1, scanResult.appsWithHighSensitivityCount)
    assertEquals(1, scanResult.cameraAccessCount)
    assertEquals(1, scanResult.locationAccessCount)
    assertEquals(0, scanResult.microphoneAccessCount)
  }

  @Test
  fun categorizeAppsGroupsByCriticalCameraLocationRecordAudioPriority() {
    fun perm(name: String, cat: SensitivityCategory) =
      GrantedDangerousPermission(name, name.substringAfterLast('.'), cat, null)

    val multiApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.multi",
        appName = "Video Messenger",
        isSystemApp = false,
        totalRequestedPermissions = 5,
        grantedDangerousPermissions =
          listOf(
            perm("android.permission.CAMERA", SensitivityCategory.CAMERA),
            perm("android.permission.RECORD_AUDIO", SensitivityCategory.MICROPHONE),
          ),
      )
    val cameraApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.camera",
        appName = "Scanner",
        isSystemApp = false,
        totalRequestedPermissions = 2,
        grantedDangerousPermissions =
          listOf(perm("android.permission.CAMERA", SensitivityCategory.CAMERA)),
      )
    val locationApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.maps",
        appName = "Maps",
        isSystemApp = false,
        totalRequestedPermissions = 2,
        grantedDangerousPermissions =
          listOf(perm("android.permission.ACCESS_FINE_LOCATION", SensitivityCategory.LOCATION)),
      )
    val micApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.voice",
        appName = "Voice Notes",
        isSystemApp = false,
        totalRequestedPermissions = 2,
        grantedDangerousPermissions =
          listOf(perm("android.permission.RECORD_AUDIO", SensitivityCategory.MICROPHONE)),
      )
    val calendarApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.cal",
        appName = "Events",
        isSystemApp = false,
        totalRequestedPermissions = 1,
        grantedDangerousPermissions =
          listOf(perm("android.permission.READ_CALENDAR", SensitivityCategory.CALENDAR)),
      )
    val standardApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.calc",
        appName = "Rechner",
        isSystemApp = true,
        totalRequestedPermissions = 0,
        grantedDangerousPermissions = emptyList(),
      )

    val groups =
      PermissionSensitivityClassifier.categorizeApps(
        listOf(standardApp, calendarApp, micApp, locationApp, cameraApp, multiApp)
      )

    assertEquals(
      listOf(
        AppSectionCategory.MULTI_CRITICAL,
        AppSectionCategory.CAMERA,
        AppSectionCategory.LOCATION,
        AppSectionCategory.MICROPHONE,
        AppSectionCategory.OTHER_SENSITIVE,
        AppSectionCategory.STANDARD,
      ),
      groups.map { it.category },
    )
    assertEquals(listOf("CAMERA", "RECORD_AUDIO"), multiApp.criticalSensorTags)
    assertTrue(multiApp.isCriticalSensorApp)
    assertFalse(calendarApp.isCriticalSensorApp)
    assertFalse(standardApp.isCriticalSensorApp)
  }

  @Test
  fun batteryHealthEvaluatorComputesFuelGaugeUsageTimeChargingTimeAndFailClosedUnavailable() {
    val dischargingSnapshot =
      BatteryHealthEvaluator.evaluate(
        BatteryTelemetryRaw(
          observedAtMillis = 10_000L,
          apiLevel = 36,
          level = 64,
          scale = 100,
          propertyCapacity = null,
          statusCode = BatteryManager.BATTERY_STATUS_DISCHARGING,
          pluggedMask = 0,
          healthCode = BatteryManager.BATTERY_HEALTH_GOOD,
          temperatureTenthsCelsius = 312,
          voltageMillivolts = 3890,
          technology = "Li-ion",
          chargeCounterMicroAh = 3_000_000L,
          currentNowMicroA = -480_000L,
          currentAverageMicroA = -500_000L,
          osChargeTimeRemainingMillis = null,
          powerSaveMode = false,
        )
      )
    assertEquals(64, dischargingSnapshot.percent)
    assertEquals(ChargingStatus.DISCHARGING, dischargingSnapshot.chargingStatus)
    assertEquals(PowerSource.NONE, dischargingSnapshot.powerSource)
    assertEquals(BatteryHealthCondition.GOOD, dischargingSnapshot.healthCondition)
    assertEquals(RemainingTimeSource.FUEL_GAUGE_CURRENT_DIV, dischargingSnapshot.remainingTime.source)
    assertFalse(dischargingSnapshot.remainingTime.isChargingEstimate)
    assertEquals(6L * 3_600_000L, dischargingSnapshot.remainingTime.millis)
    assertEquals("ca. 6 Std. 0 Min.", dischargingSnapshot.remainingTime.formattedDuration)

    val chargingSnapshot =
      BatteryHealthEvaluator.evaluate(
        BatteryTelemetryRaw(
          observedAtMillis = 20_000L,
          apiLevel = 36,
          level = 55,
          scale = 100,
          propertyCapacity = null,
          statusCode = BatteryManager.BATTERY_STATUS_CHARGING,
          pluggedMask = BatteryManager.BATTERY_PLUGGED_AC,
          healthCode = BatteryManager.BATTERY_HEALTH_GOOD,
          temperatureTenthsCelsius = 335,
          voltageMillivolts = 4210,
          technology = "Li-poly",
          chargeCounterMicroAh = null,
          currentNowMicroA = 1_500_000L,
          currentAverageMicroA = null,
          osChargeTimeRemainingMillis = 2_700_000L,
          powerSaveMode = false,
        )
      )
    assertEquals(ChargingStatus.CHARGING, chargingSnapshot.chargingStatus)
    assertEquals(PowerSource.AC, chargingSnapshot.powerSource)
    assertEquals(RemainingTimeSource.OS_CHARGE_TIME_REMAINING, chargingSnapshot.remainingTime.source)
    assertTrue(chargingSnapshot.remainingTime.isChargingEstimate)
    assertEquals(2_700_000L, chargingSnapshot.remainingTime.millis)
    assertEquals("ca. 45 Min.", chargingSnapshot.remainingTime.formattedDuration)

    val unavailableSnapshot =
      BatteryHealthEvaluator.evaluate(
        BatteryTelemetryRaw(
          observedAtMillis = 30_000L,
          apiLevel = 36,
          level = null,
          scale = null,
          propertyCapacity = 42,
          statusCode = BatteryManager.BATTERY_STATUS_DISCHARGING,
          pluggedMask = 0,
          healthCode = null,
          temperatureTenthsCelsius = null,
          voltageMillivolts = null,
          technology = null,
          chargeCounterMicroAh = Long.MIN_VALUE,
          currentNowMicroA = Long.MIN_VALUE,
          currentAverageMicroA = null,
          osChargeTimeRemainingMillis = null,
          powerSaveMode = null,
        )
      )
    assertEquals(42, unavailableSnapshot.percent)
    assertEquals(RemainingTimeSource.UNAVAILABLE, unavailableSnapshot.remainingTime.source)
    assertNull(unavailableSnapshot.remainingTime.millis)
    assertEquals("Nicht messbar", unavailableSnapshot.remainingTime.formattedDuration)
  }

  @Test
  fun aiContextSummaryIncludesAggregateCountsAndExcludesPackageNames() {
    val battery =
      BatteryDashboardSnapshot(
        observedAtMillis = 5_000L,
        percent = 78,
        chargingStatus = ChargingStatus.DISCHARGING,
        powerSource = PowerSource.NONE,
        healthCondition = BatteryHealthCondition.GOOD,
        temperatureCelsius = 29.4f,
        remainingTime =
          RemainingTimeEstimate(
            millis = 3_600_000L,
            formattedDuration = "ca. 1 Std. 0 Min.",
            isChargingEstimate = false,
            source = RemainingTimeSource.FUEL_GAUGE_CURRENT_DIV,
            limitationNote = null,
          ),
        powerSaveMode = false,
      )
    val secretPkg = "com.example.privateapp.secret"
    val appsScan =
      InstalledAppsScanResult(
        scannedAtMillis = 5_000L,
        visiblePackageCount = 12,
        apps =
          listOf(
            InstalledAppPermissionInfo(
              packageName = secretPkg,
              appName = "Secret App",
              isSystemApp = false,
              totalRequestedPermissions = 2,
              grantedDangerousPermissions =
                listOf(
                  GrantedDangerousPermission(
                    fullName = "android.permission.CAMERA",
                    shortName = "CAMERA",
                    category = SensitivityCategory.CAMERA,
                    description = null,
                  )
                ),
            )
          ),
      )

    val summary = AiAdvisorViewModel.buildAnonymizedContextSummary(battery, appsScan)
    assertTrue(summary != null && summary.contains("78%"))
    assertTrue(summary!!.contains("Kamera=1"))
    assertFalse(summary.contains(secretPkg))
    assertFalse(summary.contains("Secret App"))
  }

  @Test
  fun permissionDigestBuilderPrioritizesCriticalUserAppsAndAppliesMaxCap() {
    fun perm(name: String, cat: SensitivityCategory) =
      GrantedDangerousPermission(name, name.substringAfterLast('.'), cat, null)

    val safeApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.safe",
        appName = "Clean Calc",
        isSystemApp = false,
        totalRequestedPermissions = 1,
        grantedDangerousPermissions = emptyList(),
      )
    val systemCamApp =
      InstalledAppPermissionInfo(
        packageName = "com.android.sys.cam",
        appName = "System Camera",
        isSystemApp = true,
        totalRequestedPermissions = 2,
        grantedDangerousPermissions =
          listOf(perm("android.permission.CAMERA", SensitivityCategory.CAMERA)),
      )
    val userMultiApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.social",
        appName = "Social Recorder",
        isSystemApp = false,
        totalRequestedPermissions = 4,
        grantedDangerousPermissions =
          listOf(
            perm("android.permission.CAMERA", SensitivityCategory.CAMERA),
            perm("android.permission.RECORD_AUDIO", SensitivityCategory.MICROPHONE),
            perm("android.permission.ACCESS_FINE_LOCATION", SensitivityCategory.LOCATION),
          ),
      )
    val userCalApp =
      InstalledAppPermissionInfo(
        packageName = "com.example.planner",
        appName = "Planner",
        isSystemApp = false,
        totalRequestedPermissions = 1,
        grantedDangerousPermissions =
          listOf(perm("android.permission.READ_CALENDAR", SensitivityCategory.CALENDAR)),
      )

    val scanResult =
      InstalledAppsScanResult(
        scannedAtMillis = 9_000L,
        visiblePackageCount = 4,
        apps = listOf(safeApp, userCalApp, systemCamApp, userMultiApp),
      )

    val digest = GeminiPermissionPrivacyAnalyzerService.buildPermissionDigestForGemini(scanResult, maxApps = 2)
    assertEquals(2, digest.analyzedAppCount)
    assertEquals(4, digest.analyzedPermissionCount)
    assertEquals(1, digest.omittedDangerousAppCount)
    assertTrue(digest.promptText.contains("Social Recorder (Nutzer-App)"))
    assertTrue(digest.promptText.contains("System Camera (System-App)"))
    assertFalse(digest.promptText.contains("Clean Calc"))
    assertFalse(digest.promptText.contains("Planner (Nutzer-App)"))
  }
}
