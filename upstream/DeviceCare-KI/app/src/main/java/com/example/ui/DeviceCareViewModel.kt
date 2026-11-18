package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.evidence.Availability
import com.example.core.evidence.DeviceEvidence
import com.example.core.evidence.ObservationContract
import com.example.core.evidence.RiskLevel
import com.example.core.evidence.SyntheticFixtureOracle
import com.example.core.quality.CheckStatus
import com.example.core.quality.QualityCheck
import com.example.core.quality.QualityPipeline
import com.example.core.quality.QualityReport
import com.example.core.quality.StatisticalValidation
import com.example.core.scoring.DeviceAssessment
import com.example.core.scoring.DeviceModel
import com.example.core.scoring.DeviceScoringDispatcher
import com.example.core.scoring.StaticDeviceModelRegistry
import com.example.data.DesignPreferences
import com.example.data.DesignPreferencesRepository
import com.example.devicecare.InspectionResult
import com.example.devicecare.presentInspection
import com.example.features.battery.AndroidBatteryCollector
import com.example.features.battery.AndroidBatteryHealthMonitor
import com.example.features.battery.BatteryDashboardSnapshot
import com.example.features.permissions.AndroidPermissionCollector
import com.example.features.permissions.AndroidPermissionScanProfile
import com.example.features.permissions.AppPermissionFilter
import com.example.features.permissions.AppSectionCategory
import com.example.features.permissions.CategorizedAppGroup
import com.example.features.permissions.InstalledAppPermissionInfo
import com.example.features.permissions.InstalledAppPermissionInspector
import com.example.features.permissions.InstalledAppsScanResult
import com.example.features.permissions.PermissionScanBudget
import com.example.features.permissions.PermissionSensitivityClassifier
import com.example.features.permissions.SensitivityCategory
import com.example.model.CommercialTierPlan
import com.example.model.DeviceCareRoadmapData
import com.example.model.GovernanceStatus
import com.example.model.GradleModuleSpec
import com.example.model.PlanCategory
import com.example.model.RoadmapTask
import com.example.model.SafeguardRule
import com.example.ui.theme.CornerStyle
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ArchitectureTab(val label: String) {
  PIPELINES("Pipelines & QA-Labor"),
  SAFEGUARDS("10 Schutzregeln"),
  TIERS_MODULES("Module & Stufen"),
}

data class DeviceCareUiState(
  val preferences: DesignPreferences = DesignPreferences(),
  val inspectionInProgress: Boolean = false,
  val inspectionResult: InspectionResult? = null,
  val isInspectionStale: Boolean = false,
  val settingsMessage: String? = null,
  val searchQuery: String = "",
  val selectedCategory: PlanCategory = PlanCategory.ALL,
  val selectedStatusFilter: GovernanceStatus? = null,
  val allTasks: List<RoadmapTask> = DeviceCareRoadmapData.tasks,
  val filteredTasks: List<RoadmapTask> = DeviceCareRoadmapData.tasks,
  val safeguards: List<SafeguardRule> = DeviceCareRoadmapData.safeguards,
  val gradleModules: List<GradleModuleSpec> = DeviceCareRoadmapData.gradleModules,
  val tierPlans: List<CommercialTierPlan> = DeviceCareRoadmapData.tierPlans,
  val selectedArchitectureTab: ArchitectureTab = ArchitectureTab.PIPELINES,
  val enterprisePreviewMode: Boolean = false,
  // Interactive Pipeline Sandbox states
  val qualitySimulateShaPresent: Boolean = false,
  val qualityReport: QualityReport =
    QualityPipeline.assess(
      checks =
        listOf(
          QualityCheck("governance_policy", CheckStatus.PASS),
          QualityCheck("core_unit_tests", CheckStatus.PASS),
          QualityCheck("android15_physical_qa", CheckStatus.NOT_PROVEN),
        ),
      commitSha = null,
    ),
  val scoringSimulateMissingSignal: Boolean = true,
  val sandboxScoringAssessment: DeviceAssessment =
    DeviceScoringDispatcher(
        StaticDeviceModelRegistry(
          mapOf(
            "p0_fixture_ref" to
              DeviceModel("p0_fixture_ref", "1.0.0", setOf("battery_state", "permission_posture"))
          )
        )
      )
      .assess("p0_fixture_ref", emptyList(), 1_000L),
  val selectedFixtureId: String = "SYN-004",
  val statisticalFalsePositives: Int = 1,
  val statisticalNegativesTotal: Int = 500,
  // Installed Apps & Granted Dangerous Permissions (on-demand, RAM-only)
  val installedAppsScanning: Boolean = false,
  val installedAppsScanResult: InstalledAppsScanResult? = null,
  val appSearchQuery: String = "",
  val selectedAppFilter: AppPermissionFilter = AppPermissionFilter.ALL,
  val hideSystemApps: Boolean = false,
  val filteredInstalledApps: List<InstalledAppPermissionInfo> = emptyList(),
  val categorizedAppSections: List<CategorizedAppGroup> = emptyList(),
  val collapsedAppSections: Set<AppSectionCategory> = setOf(AppSectionCategory.STANDARD),
  // Live Battery Health Monitor Dashboard (foreground/on-demand, RAM-only)
  val batteryMonitorLoading: Boolean = false,
  val batterySnapshot: BatteryDashboardSnapshot? = null,
) {
  val verifiedCount: Int
    get() =
      allTasks.count {
        it.status == GovernanceStatus.VERIFIED || it.id in preferences.localVerifiedTaskIds
      }

  val qaCompletedCount: Int
    get() =
      allTasks
        .filter { it.category == PlanCategory.ANDROID15_QA }
        .count { it.id in preferences.localVerifiedTaskIds }

  val qaTotalCount: Int
    get() = allTasks.count { it.category == PlanCategory.ANDROID15_QA }

  val readinessFraction: Float
    get() =
      if (allTasks.isEmpty()) 0f else verifiedCount.toFloat() / allTasks.size.toFloat()
}

class DeviceCareViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = DesignPreferencesRepository(application.applicationContext)

  private val inspectionInProgressFlow = MutableStateFlow(false)
  private val inspectionResultFlow = MutableStateFlow<InspectionResult?>(null)
  private val isStaleFlow = MutableStateFlow(false)
  private val settingsMessageFlow = MutableStateFlow<String?>(null)

  private val searchQueryFlow = MutableStateFlow("")
  private val selectedCategoryFlow = MutableStateFlow(PlanCategory.ALL)
  private val selectedStatusFlow = MutableStateFlow<GovernanceStatus?>(null)
  private val selectedArchTabFlow = MutableStateFlow(ArchitectureTab.PIPELINES)
  private val enterprisePreviewFlow = MutableStateFlow(false)

  // Pipeline Sandbox Flows
  private val qualityShaPresentFlow = MutableStateFlow(false)
  private val scoringMissingSignalFlow = MutableStateFlow(true)
  private val selectedFixtureIdFlow = MutableStateFlow("SYN-004")
  private val statFpFlow = MutableStateFlow(1)
  private val statNegTotalFlow = MutableStateFlow(500)

  // Installed Apps & Permissions Flows (RAM-only, never persisted)
  private val installedAppsScanningFlow = MutableStateFlow(false)
  private val installedAppsResultFlow = MutableStateFlow<InstalledAppsScanResult?>(null)
  private val appSearchQueryFlow = MutableStateFlow("")
  private val selectedAppFilterFlow = MutableStateFlow(AppPermissionFilter.ALL)
  private val hideSystemAppsFlow = MutableStateFlow(false)
  private val collapsedAppSectionsFlow =
    MutableStateFlow(setOf(AppSectionCategory.STANDARD))
  private val batteryMonitorLoadingFlow = MutableStateFlow(false)
  private val batterySnapshotFlow = MutableStateFlow<BatteryDashboardSnapshot?>(null)

  private val sandboxDispatcher =
    DeviceScoringDispatcher(
      StaticDeviceModelRegistry(
        mapOf(
          "p0_fixture_ref" to
            DeviceModel("p0_fixture_ref", "1.0.0", setOf("battery_state", "permission_posture"))
        )
      )
    )

  private data class FilterTuple(
    val query: String,
    val category: PlanCategory,
    val status: GovernanceStatus?,
    val archTab: ArchitectureTab,
    val enterprise: Boolean,
  )

  private data class SandboxTuple(
    val shaPresent: Boolean,
    val missingSignal: Boolean,
    val fixtureId: String,
    val fp: Int,
    val negTotal: Int,
  )

  private val filterFlow =
    combine(
      searchQueryFlow,
      selectedCategoryFlow,
      selectedStatusFlow,
      selectedArchTabFlow,
      enterprisePreviewFlow,
    ) { query, category, status, archTab, enterprise ->
      FilterTuple(query, category, status, archTab, enterprise)
    }

  private val sandboxFlow =
    combine(
      qualityShaPresentFlow,
      scoringMissingSignalFlow,
      selectedFixtureIdFlow,
      statFpFlow,
      statNegTotalFlow,
    ) { sha, missing, fixtureId, fp, negTotal ->
      SandboxTuple(sha, missing, fixtureId, fp, negTotal)
    }

  private val batteryTupleFlow =
    combine(batteryMonitorLoadingFlow, batterySnapshotFlow) { loading, snapshot ->
      loading to snapshot
    }

  private val inspectionTupleFlow =
    combine(
      inspectionInProgressFlow,
      inspectionResultFlow,
      isStaleFlow,
      settingsMessageFlow,
      batteryTupleFlow,
    ) { busy, result, stale, msg, batteryPair ->
      InspectionBundle(busy, result, stale, msg, batteryPair.first, batteryPair.second)
    }

  private data class InspectionBundle(
    val busy: Boolean,
    val result: InspectionResult?,
    val stale: Boolean,
    val msg: String?,
    val batteryLoading: Boolean,
    val batterySnapshot: BatteryDashboardSnapshot?,
  )

  private data class AppPermissionsBundle(
    val scanning: Boolean,
    val result: InstalledAppsScanResult?,
    val query: String,
    val filter: AppPermissionFilter,
    val hideSystem: Boolean,
    val collapsedSections: Set<AppSectionCategory>,
  )

  private val appFilterSubFlow =
    combine(
      appSearchQueryFlow,
      selectedAppFilterFlow,
      hideSystemAppsFlow,
      collapsedAppSectionsFlow,
    ) { query, filter, hideSystem, collapsedSections ->
      Triple(query, filter, hideSystem to collapsedSections)
    }

  private val appPermissionsTupleFlow =
    combine(
      installedAppsScanningFlow,
      installedAppsResultFlow,
      appFilterSubFlow,
    ) { scanning, result, (query, filter, flags) ->
      AppPermissionsBundle(
        scanning = scanning,
        result = result,
        query = query,
        filter = filter,
        hideSystem = flags.first,
        collapsedSections = flags.second,
      )
    }

  val uiState: StateFlow<DeviceCareUiState> =
    combine(
        repository.preferencesFlow,
        inspectionTupleFlow,
        filterFlow,
        sandboxFlow,
        appPermissionsTupleFlow,
      ) { prefs, insp, filter, sandbox, appPerms ->
        val filtered =
          DeviceCareRoadmapData.tasks.filter { task ->
            val effectiveStatus =
              if (task.id in prefs.localVerifiedTaskIds) GovernanceStatus.VERIFIED else task.status
            val matchesCat =
              filter.category == PlanCategory.ALL || task.category == filter.category
            val matchesStatus = filter.status == null || effectiveStatus == filter.status
            val matchesQuery =
              filter.query.isBlank() ||
                task.title.contains(filter.query, ignoreCase = true) ||
                task.subtitle.contains(filter.query, ignoreCase = true) ||
                task.code.contains(filter.query, ignoreCase = true) ||
                task.description.contains(filter.query, ignoreCase = true)
            matchesCat && matchesStatus && matchesQuery
          }

        val qualityReport =
          QualityPipeline.assess(
            checks =
              listOf(
                QualityCheck("governance_policy", CheckStatus.PASS),
                QualityCheck("core_unit_tests", CheckStatus.PASS),
                QualityCheck(
                  "android15_physical_qa",
                  if (sandbox.shaPresent) CheckStatus.PASS else CheckStatus.NOT_PROVEN,
                ),
              ),
            commitSha =
              if (sandbox.shaPresent) "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678" else null,
          )

        val now = 10_000L
        val sampleEvidence =
          if (sandbox.missingSignal) {
            listOf(
              DeviceEvidence(
                id = "battery_state",
                source = "synthetic_fixture",
                availability = Availability.OBSERVED,
                observedAtMillis = 9_000L,
                expiresAtMillis = 69_000L,
                riskLevel = RiskLevel.NONE,
              )
            )
          } else {
            listOf(
              DeviceEvidence(
                id = "battery_state",
                source = "synthetic_fixture",
                availability = Availability.OBSERVED,
                observedAtMillis = 9_000L,
                expiresAtMillis = 69_000L,
                riskLevel = RiskLevel.NONE,
              ),
              DeviceEvidence(
                id = "permission_posture",
                source = "synthetic_fixture",
                availability = Availability.OBSERVED,
                observedAtMillis = 9_000L,
                expiresAtMillis = 69_000L,
                riskLevel = RiskLevel.LOW,
              ),
            )
          }

        DeviceCareUiState(
          preferences = prefs,
          inspectionInProgress = insp.busy,
          inspectionResult = insp.result,
          isInspectionStale = insp.stale,
          settingsMessage = insp.msg,
          searchQuery = filter.query,
          selectedCategory = filter.category,
          selectedStatusFilter = filter.status,
          filteredTasks = filtered,
          selectedArchitectureTab = filter.archTab,
          enterprisePreviewMode = filter.enterprise,
          qualitySimulateShaPresent = sandbox.shaPresent,
          qualityReport = qualityReport,
          scoringSimulateMissingSignal = sandbox.missingSignal,
          sandboxScoringAssessment =
            sandboxDispatcher.assess("p0_fixture_ref", sampleEvidence, now),
          selectedFixtureId = sandbox.fixtureId,
          statisticalFalsePositives = sandbox.fp,
          statisticalNegativesTotal = sandbox.negTotal,
          installedAppsScanning = appPerms.scanning,
          installedAppsScanResult = appPerms.result,
          appSearchQuery = appPerms.query,
          selectedAppFilter = appPerms.filter,
          hideSystemApps = appPerms.hideSystem,
          filteredInstalledApps =
            filterInstalledApps(
              apps = appPerms.result?.apps.orEmpty(),
              query = appPerms.query,
              filter = appPerms.filter,
              hideSystemApps = appPerms.hideSystem,
            ),
          categorizedAppSections =
            PermissionSensitivityClassifier.categorizeApps(
              filterInstalledApps(
                apps = appPerms.result?.apps.orEmpty(),
                query = appPerms.query,
                filter = appPerms.filter,
                hideSystemApps = appPerms.hideSystem,
              )
            ),
          collapsedAppSections =
            if (appPerms.query.isNotBlank() || appPerms.filter != AppPermissionFilter.ALL) {
              emptySet()
            } else {
              appPerms.collapsedSections
            },
          batteryMonitorLoading = insp.batteryLoading,
          batterySnapshot = insp.batterySnapshot,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeviceCareUiState(),
      )

  /**
   * Expliziter On-Demand-Scan auf Dispatchers.IO.
   * Kein Auto-Start beim App-Öffnen, keine Persistenz der Rohdaten.
   */
  fun startManualInspection() {
    if (inspectionInProgressFlow.value) return
    inspectionInProgressFlow.value = true
    isStaleFlow.value = false
    settingsMessageFlow.value = null

    viewModelScope.launch {
      val startElapsed = SystemClock.elapsedRealtime()
      val startCpu = Process.getElapsedCpuTime()
      try {
        val context = getApplication<Application>().applicationContext
        val result =
          withContext(Dispatchers.IO) {
            val scanProfile = AndroidPermissionScanProfile(context)
            val profile = scanProfile.read()
            val budget = PermissionScanBudget.forProfile(profile)
            val battery = AndroidBatteryCollector(context).collect()
            var deferred = false
            val permissions =
              AndroidPermissionCollector(context)
                .collect(
                  sampleLimit = budget.packageLimit,
                  shouldDeferOptionalReads = {
                    scanProfile.hasHighPressure().also { deferred = it }
                  },
                  checkCancellation = { coroutineContext.ensureActive() },
                )
            val elapsedMs = (SystemClock.elapsedRealtime() - startElapsed).coerceAtLeast(0)
            val cpuMs = (Process.getElapsedCpuTime() - startCpu).coerceAtLeast(0)
            presentInspection(
              profile = profile,
              budget = budget,
              battery = battery,
              permissions = permissions,
              optionalReadsDeferred = deferred,
              elapsedMs = elapsedMs,
              cpuMs = cpuMs,
            )
          }
        inspectionResultFlow.value = result
        // Mark snapshot stale after ObservationContract.MAX_AGE_MILLIS (60s)
        launch {
          delay(ObservationContract.MAX_AGE_MILLIS)
          if (inspectionResultFlow.value?.collectedAtMillis == result.collectedAtMillis) {
            isStaleFlow.value = true
          }
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: RuntimeException) {
        inspectionResultFlow.value =
          InspectionResult(
            errorMessage = "Lokale Prüfung nicht verfügbar. Keine Daten gespeichert."
          )
      } finally {
        inspectionInProgressFlow.value = false
      }
    }
  }

  /**
   * Expliziter On-Demand-Scan sichtbarer installierter Apps und deren gewährter
   * Laufzeitberechtigungen. Bleibt rein im Arbeitsspeicher (keine Persistenz, kein Auto-Start).
   */
  fun startInstalledAppsScan() {
    if (installedAppsScanningFlow.value) return
    installedAppsScanningFlow.value = true

    viewModelScope.launch {
      try {
        val context = getApplication<Application>().applicationContext
        val result =
          withContext(Dispatchers.IO) {
            InstalledAppPermissionInspector(context).inspectVisibleApps()
          }
        installedAppsResultFlow.value = result
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: RuntimeException) {
        installedAppsResultFlow.value =
          InstalledAppsScanResult(
            scannedAtMillis = System.currentTimeMillis(),
            visiblePackageCount = 0,
            apps = emptyList(),
            errorMessage = "Installierte Apps konnten lokal nicht gelesen werden.",
          )
      } finally {
        installedAppsScanningFlow.value = false
      }
    }
  }

  fun refreshBatteryMonitor(batteryIntent: Intent? = null) {
    if (batteryIntent == null && batteryMonitorLoadingFlow.value) return
    if (batteryIntent == null) {
      batteryMonitorLoadingFlow.value = true
    }

    viewModelScope.launch {
      try {
        val context = getApplication<Application>().applicationContext
        val snapshot =
          withContext(Dispatchers.IO) {
            AndroidBatteryHealthMonitor(context).readSnapshot(batteryIntent)
          }
        batterySnapshotFlow.value = snapshot
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: RuntimeException) {
        batterySnapshotFlow.value =
          BatteryDashboardSnapshot(
            observedAtMillis = System.currentTimeMillis(),
            errorMessage = "Akkudaten konnten lokal nicht gelesen werden.",
          )
      } finally {
        batteryMonitorLoadingFlow.value = false
      }
    }
  }

  fun updateAppSearchQuery(query: String) {
    appSearchQueryFlow.value = query
  }

  fun selectAppFilter(filter: AppPermissionFilter) {
    selectedAppFilterFlow.value =
      if (filter != AppPermissionFilter.ALL && selectedAppFilterFlow.value == filter) {
        AppPermissionFilter.ALL
      } else {
        filter
      }
  }

  fun toggleHideSystemApps() {
    hideSystemAppsFlow.value = !hideSystemAppsFlow.value
  }

  fun toggleAppSectionCollapsed(category: AppSectionCategory) {
    val current = collapsedAppSectionsFlow.value
    collapsedAppSectionsFlow.value =
      if (category in current) current - category else current + category
  }

  private fun filterInstalledApps(
    apps: List<InstalledAppPermissionInfo>,
    query: String,
    filter: AppPermissionFilter,
    hideSystemApps: Boolean,
  ): List<InstalledAppPermissionInfo> {
    val trimmedQuery = query.trim()
    return apps.filter { app ->
      val matchesSystem = !hideSystemApps || !app.isSystemApp
      val matchesCategory =
        when (filter) {
          AppPermissionFilter.ALL -> true
          AppPermissionFilter.SENSITIVE_ONLY -> app.hasHighSensitivityAccess
          AppPermissionFilter.CAMERA -> SensitivityCategory.CAMERA in app.categories
          AppPermissionFilter.LOCATION -> SensitivityCategory.LOCATION in app.categories
          AppPermissionFilter.MICROPHONE -> SensitivityCategory.MICROPHONE in app.categories
          AppPermissionFilter.PHONE_SMS -> SensitivityCategory.PHONE_SMS in app.categories
          AppPermissionFilter.CONTACTS -> SensitivityCategory.CONTACTS in app.categories
        }
      val matchesQuery =
        trimmedQuery.isEmpty() ||
          app.appName.contains(trimmedQuery, ignoreCase = true) ||
          app.packageName.contains(trimmedQuery, ignoreCase = true) ||
          app.grantedDangerousPermissions.any { perm ->
            perm.shortName.contains(trimmedQuery, ignoreCase = true) ||
              perm.fullName.contains(trimmedQuery, ignoreCase = true) ||
              perm.category.label.contains(trimmedQuery, ignoreCase = true)
          }
      matchesSystem && matchesCategory && matchesQuery
    }
  }

  fun clearSettingsMessage() {
    settingsMessageFlow.value = null
  }

  fun setSettingsMessage(message: String?) {
    settingsMessageFlow.value = message
  }

  fun updateSearchQuery(query: String) {
    searchQueryFlow.value = query
  }

  fun selectCategory(category: PlanCategory) {
    selectedCategoryFlow.value = category
  }

  fun selectStatusFilter(status: GovernanceStatus?) {
    selectedStatusFlow.value = if (selectedStatusFlow.value == status) null else status
  }

  fun selectArchitectureTab(tab: ArchitectureTab) {
    selectedArchTabFlow.value = tab
  }

  fun setEnterprisePreviewMode(enabled: Boolean) {
    enterprisePreviewFlow.value = enabled
  }

  fun toggleQualityShaSimulation() {
    qualityShaPresentFlow.value = !qualityShaPresentFlow.value
  }

  fun toggleScoringMissingSignalSimulation() {
    scoringMissingSignalFlow.value = !scoringMissingSignalFlow.value
  }

  fun selectFixture(id: String) {
    selectedFixtureIdFlow.value = id
  }

  fun setStatisticalFalsePositives(fp: Int) {
    statFpFlow.value = fp.coerceIn(0, 15)
  }

  fun computeStatisticalCounts(): StatisticalValidation.Counts {
    val neg = statNegTotalFlow.value.toLong()
    val fp = statFpFlow.value.toLong().coerceAtMost(neg)
    val tn = neg - fp
    return StatisticalValidation.Counts(tp = 95L, fp = fp, tn = tn, fn = 5L)
  }

  fun getTaskById(id: String): RoadmapTask? = DeviceCareRoadmapData.tasks.find { it.id == id }

  fun getSelectedFixture(): SyntheticFixtureOracle.Fixture =
    SyntheticFixtureOracle.referenceCases.find { it.id == selectedFixtureIdFlow.value }
      ?: SyntheticFixtureOracle.referenceCases.first()

  fun toggleLocalTaskVerified(taskId: String) {
    viewModelScope.launch { repository.toggleLocalTaskVerified(taskId) }
  }

  fun toggleBookmark(taskId: String) {
    viewModelScope.launch { repository.toggleBookmark(taskId) }
  }

  fun setPalette(palette: ThemePalette) {
    viewModelScope.launch { repository.setPalette(palette) }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { repository.setThemeMode(mode) }
  }

  fun cycleThemeMode() {
    val next =
      when (uiState.value.preferences.themeMode) {
        ThemeMode.SYSTEM -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.SYSTEM
      }
    setThemeMode(next)
  }

  fun setCornerStyle(style: CornerStyle) {
    viewModelScope.launch { repository.setCornerStyle(style) }
  }

  fun setCompactDensity(enabled: Boolean) {
    viewModelScope.launch { repository.setCompactDensity(enabled) }
  }

  fun setHapticPreview(enabled: Boolean) {
    viewModelScope.launch { repository.setHapticPreview(enabled) }
  }

  fun resetDesignDefaults() {
    viewModelScope.launch { repository.resetDefaults() }
  }
}
