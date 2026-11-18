package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ai.AiAdvisorViewModel
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.AppPermissionsScreen
import com.example.ui.screens.ArchitectureScreen
import com.example.ui.screens.BatteryDashboardScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DesignSystemScreen
import com.example.ui.screens.InspectionScreen
import com.example.ui.screens.SystemSecurityFeatureScreen
import com.example.ui.screens.RoadmapScreen
import com.example.ui.screens.TaskDetailScreen
import com.example.ui.theme.FormwerkTheme
import kotlinx.coroutines.launch

object Routes {
  const val ROUTE_INSPECTION = "inspection"
  const val ROUTE_BATTERY = "battery"
  const val ROUTE_AI_ADVISOR = "ai_advisor"
  const val ROUTE_CLOUD_SYNC = "cloud_sync"
  const val ROUTE_SYSTEM_SECURITY = "system_security"
  const val ROUTE_APPS = "apps"
  const val ROUTE_ROADMAP = "roadmap"
  const val ROUTE_ARCHITECTURE = "architecture"
  const val ROUTE_DESIGN = "design"
  const val ROUTE_TASK_DETAIL = "task_detail/{taskId}"

  fun taskDetail(taskId: String): String = "task_detail/$taskId"
}

private data class NavDestinationItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val topLevelDestinations =
  listOf(
    NavDestinationItem(
      route = Routes.ROUTE_INSPECTION,
      labelRes = R.string.nav_inspection,
      selectedIcon = Icons.Filled.HealthAndSafety,
      unselectedIcon = Icons.Outlined.HealthAndSafety,
      testTag = "nav_inspection",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_APPS,
      labelRes = R.string.nav_apps,
      selectedIcon = Icons.Filled.Apps,
      unselectedIcon = Icons.Outlined.Apps,
      testTag = "nav_apps",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_ROADMAP,
      labelRes = R.string.nav_roadmap,
      selectedIcon = Icons.Filled.Checklist,
      unselectedIcon = Icons.Outlined.Checklist,
      testTag = "nav_roadmap",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_ARCHITECTURE,
      labelRes = R.string.nav_architecture,
      selectedIcon = Icons.Filled.AccountTree,
      unselectedIcon = Icons.Outlined.AccountTree,
      testTag = "nav_architecture",
    ),
    NavDestinationItem(
      route = Routes.ROUTE_DESIGN,
      labelRes = R.string.nav_design,
      selectedIcon = Icons.Filled.Palette,
      unselectedIcon = Icons.Outlined.Palette,
      testTag = "nav_design",
    ),
  )

@Composable
fun DeviceCareApp(
  viewModel: DeviceCareViewModel = viewModel(),
  aiAdvisorViewModel: AiAdvisorViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val aiState by aiAdvisorViewModel.uiState.collectAsStateWithLifecycle()
  val privacyAnalysisState by aiAdvisorViewModel.privacyAnalysisState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current

  fun performHaptic() {
    if (uiState.preferences.hapticPreview) {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }

  fun showMessage(message: String) {
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(message)
    }
  }

  fun navigateTopLevel(route: String) {
    performHaptic()
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val showBottomOrRailNav =
    currentDestination?.route == null ||
      currentDestination.route in topLevelDestinations.map { it.route }

  FormwerkTheme(
    palette = uiState.preferences.palette,
    themeMode = uiState.preferences.themeMode,
    cornerStyle = uiState.preferences.cornerStyle,
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          bottomBar = {
            if (!useNavRail && showBottomOrRailNav) {
              NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ) {
                topLevelDestinations.forEach { dest ->
                  val selected =
                    currentDestination?.hierarchy?.any { it.route == dest.route } == true
                  val label = stringResource(dest.labelRes)
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateTopLevel(dest.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    },
                    label = { Text(text = label, maxLines = 1) },
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useNavRail && showBottomOrRailNav) {
              NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ) {
                Spacer(modifier = Modifier.weight(1f))
                topLevelDestinations.forEach { dest ->
                  val selected =
                    currentDestination?.hierarchy?.any { it.route == dest.route } == true
                  val label = stringResource(dest.labelRes)
                  NavigationRailItem(
                    selected = selected,
                    onClick = { navigateTopLevel(dest.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    },
                    label = { Text(text = label, maxLines = 1) },
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
                Spacer(modifier = Modifier.weight(1f))
              }
              VerticalDivider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
              )
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
              NavHost(
                navController = navController,
                startDestination = Routes.ROUTE_INSPECTION,
                modifier = Modifier.fillMaxSize(),
              ) {
                composable(Routes.ROUTE_INSPECTION) {
                  InspectionScreen(
                    uiState = uiState,
                    onStartInspection = {
                      performHaptic()
                      viewModel.startManualInspection()
                    },
                    onCycleThemeMode = {
                      performHaptic()
                      viewModel.cycleThemeMode()
                    },
                    onSettingsMessage = viewModel::setSettingsMessage,
                    onOpenBatteryMonitor = {
                      performHaptic()
                      navController.navigate(Routes.ROUTE_BATTERY)
                    },
                    onOpenAiAdvisor = {
                      performHaptic()
                      navController.navigate(Routes.ROUTE_AI_ADVISOR)
                    },
                    onOpenCloudSync = {
                      performHaptic()
                      navController.navigate(Routes.ROUTE_CLOUD_SYNC)
                    },
                    onOpenSystemSecurity = {
                      performHaptic()
                      navController.navigate(Routes.ROUTE_SYSTEM_SECURITY)
                    },
                    onOpenAppPermissions = { navigateTopLevel(Routes.ROUTE_APPS) },
                    onOpenRoadmap = { navigateTopLevel(Routes.ROUTE_ROADMAP) },
                  )
                }

                composable(Routes.ROUTE_BATTERY) {
                  BatteryDashboardScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onRefresh = viewModel::refreshBatteryMonitor,
                    onShowSnackbar = ::showMessage,
                  )
                }

                composable(Routes.ROUTE_AI_ADVISOR) {
                  AiAdvisorScreen(
                    aiState = aiState,
                    hasOptionalContextAvailable =
                      uiState.batterySnapshot != null || uiState.installedAppsScanResult != null,
                    onBack = { navController.popBackStack() },
                    onSendPrompt = { prompt ->
                      performHaptic()
                      aiAdvisorViewModel.sendPrompt(
                        rawPrompt = prompt,
                        batterySnapshot = uiState.batterySnapshot,
                        appsScanResult = uiState.installedAppsScanResult,
                      )
                    },
                    onToggleContext = {
                      performHaptic()
                      aiAdvisorViewModel.toggleIncludeAnonymizedContext()
                    },
                    onClearChat = {
                      performHaptic()
                      aiAdvisorViewModel.clearChat()
                    },
                  )
                }

                composable(Routes.ROUTE_CLOUD_SYNC) {
                  CloudSyncScreen(
                    onBack = { navController.popBackStack() },
                    onShowSnackbar = ::showMessage,
                  )
                }

                composable(Routes.ROUTE_SYSTEM_SECURITY) {
                  SystemSecurityFeatureScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.ROUTE_APPS) {
                  AppPermissionsScreen(
                    uiState = uiState,
                    privacyAnalysisState = privacyAnalysisState,
                    onStartScan = {
                      performHaptic()
                      viewModel.startInstalledAppsScan()
                    },
                    onAnalyzePrivacy = {
                      performHaptic()
                      aiAdvisorViewModel.analyzePermissionsPrivacy(uiState.installedAppsScanResult)
                    },
                    onClearPrivacyAnalysis = {
                      performHaptic()
                      aiAdvisorViewModel.clearPrivacyAnalysis()
                    },
                    onSearchQueryChange = viewModel::updateAppSearchQuery,
                    onSelectFilter = { filter ->
                      performHaptic()
                      viewModel.selectAppFilter(filter)
                    },
                    onToggleHideSystemApps = {
                      performHaptic()
                      viewModel.toggleHideSystemApps()
                    },
                    onToggleSectionCollapsed = { section ->
                      performHaptic()
                      viewModel.toggleAppSectionCollapsed(section)
                    },
                    onShowSnackbar = ::showMessage,
                  )
                }

                composable(Routes.ROUTE_ROADMAP) {
                  RoadmapScreen(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSelectCategory = { cat ->
                      performHaptic()
                      viewModel.selectCategory(cat)
                    },
                    onSelectStatusFilter = { status ->
                      performHaptic()
                      viewModel.selectStatusFilter(status)
                    },
                    onOpenTaskDetail = { taskId ->
                      performHaptic()
                      navController.navigate(Routes.taskDetail(taskId))
                    },
                    onToggleBookmark = { taskId ->
                      performHaptic()
                      viewModel.toggleBookmark(taskId)
                    },
                  )
                }

                composable(Routes.ROUTE_ARCHITECTURE) {
                  ArchitectureScreen(
                    uiState = uiState,
                    onSelectTab = { tab ->
                      performHaptic()
                      viewModel.selectArchitectureTab(tab)
                    },
                    onToggleQualitySha = {
                      performHaptic()
                      viewModel.toggleQualityShaSimulation()
                    },
                    onToggleScoringMissing = {
                      performHaptic()
                      viewModel.toggleScoringMissingSignalSimulation()
                    },
                    onSelectFixture = { id ->
                      performHaptic()
                      viewModel.selectFixture(id)
                    },
                    onChangeStatisticalFp = viewModel::setStatisticalFalsePositives,
                  )
                }

                composable(Routes.ROUTE_DESIGN) {
                  val bookmarkedTasks =
                    uiState.allTasks.filter { it.id in uiState.preferences.bookmarkedTaskIds }
                  DesignSystemScreen(
                    uiState = uiState,
                    bookmarkedTasks = bookmarkedTasks,
                    onSelectPalette = { palette ->
                      performHaptic()
                      viewModel.setPalette(palette)
                      showMessage("Palette: ${palette.displayName}")
                    },
                    onSelectThemeMode = { mode ->
                      performHaptic()
                      viewModel.setThemeMode(mode)
                    },
                    onSelectCornerStyle = { style ->
                      performHaptic()
                      viewModel.setCornerStyle(style)
                    },
                    onToggleCompactDensity = { enabled ->
                      performHaptic()
                      viewModel.setCompactDensity(enabled)
                    },
                    onToggleHapticPreview = { enabled -> viewModel.setHapticPreview(enabled) },
                    onToggleEnterprisePreview = { enabled ->
                      performHaptic()
                      viewModel.setEnterprisePreviewMode(enabled)
                    },
                    onOpenTaskDetail = { taskId ->
                      performHaptic()
                      navController.navigate(Routes.taskDetail(taskId))
                    },
                    onResetDefaults = {
                      performHaptic()
                      viewModel.resetDesignDefaults()
                      showMessage("Standarddesign wiederhergestellt")
                    },
                    onShowSnackbar = { msg ->
                      performHaptic()
                      showMessage(msg)
                    },
                  )
                }

                composable(
                  route = Routes.ROUTE_TASK_DETAIL,
                  arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                ) { backStackEntry ->
                  val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
                  val task = viewModel.getTaskById(taskId) ?: uiState.allTasks.first()
                  TaskDetailScreen(
                    task = task,
                    isBookmarked = task.id in uiState.preferences.bookmarkedTaskIds,
                    isLocallyVerified = task.id in uiState.preferences.localVerifiedTaskIds,
                    onBack = { navController.popBackStack() },
                    onToggleBookmark = {
                      performHaptic()
                      viewModel.toggleBookmark(task.id)
                    },
                    onToggleLocalVerified = {
                      performHaptic()
                      viewModel.toggleLocalTaskVerified(task.id)
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
