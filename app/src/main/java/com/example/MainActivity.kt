package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.ErasureCatalog
import com.example.model.SelectiveCategory
import com.example.ui.components.PasscodeLockOverlay
import com.example.ui.screens.ErasureSettingsScreen
import com.example.ui.screens.FactoryResetScreen
import com.example.ui.screens.FileExplorerScreen
import com.example.ui.screens.GeneralSettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MethodSelectionScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.ReportHistoryScreen
import com.example.ui.screens.SecuritySuite2026Screen
import com.example.ui.screens.SelectiveEraseScreen
import com.example.ui.screens.SettingsRootScreen
import com.example.ui.screens.ShredSummaryScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.screens.ThemeUiScreen
import com.example.ui.theme.IShredderTheme
import com.example.ui.theme.LocalAppThemeSpec
import com.example.ui.theme.UiThemeStyle
import com.example.viewmodel.ShredderViewModel

object AppRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_SELECTIVE_ERASE = "selective_erase"
    const val ROUTE_FILE_EXPLORER = "file_explorer"
    const val ROUTE_METHOD_SELECTION = "method_selection"
    const val ROUTE_SHRED_SUMMARY = "shred_summary"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_SETTINGS_GENERAL = "settings_general"
    const val ROUTE_SETTINGS_ERASURE = "settings_erasure"
    const val ROUTE_SETTINGS_THEME = "settings_theme"
    const val ROUTE_REPORT_HISTORY = "report_history"
    const val ROUTE_REPORT_DETAIL = "report_detail/{reportId}"
    const val ROUTE_SUBSCRIPTION = "subscription"
    const val ROUTE_FACTORY_RESET = "factory_reset"
    const val ROUTE_SECURITY_SUITE_2026 = "security_suite_2026"

    fun reportDetail(id: Long) = "report_detail/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val viewModel: ShredderViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            IShredderTheme(
                themeStyle = UiThemeStyle.fromId(settings.uiThemeStyleId),
                dynamicColorEnabled = settings.dynamicColorEnabled,
                oledModeEnabled = settings.oledModeEnabled,
                accentId = settings.uiAccentId
            ) {
                IShredderAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun IShredderAppRoot(viewModel: ShredderViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val spec = LocalAppThemeSpec.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val mountedFolders by viewModel.mountedFolders.collectAsStateWithLifecycle()
    val storageOverview by viewModel.storageOverview.collectAsStateWithLifecycle()
    val selectiveBuckets by viewModel.selectiveBuckets.collectAsStateWithLifecycle()
    val contactsPermissionGranted by viewModel.contactsPermissionGranted.collectAsStateWithLifecycle()
    val queuedTargets by viewModel.queuedTargets.collectAsStateWithLifecycle()
    val selectedMethodId by viewModel.selectedMethodId.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val explorerBreadcrumbs by viewModel.explorerBreadcrumbs.collectAsStateWithLifecycle()
    val explorerEntries by viewModel.explorerEntries.collectAsStateWithLifecycle()
    val explorerSelectedIds by viewModel.explorerSelectedIds.collectAsStateWithLifecycle()
    val explorerSort by viewModel.explorerSort.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val usdtPurchases by viewModel.usdtPurchases.collectAsStateWithLifecycle()
    val usdtVerificationState by viewModel.usdtVerificationState.collectAsStateWithLifecycle()
    val privacyAudit by viewModel.privacyAudit.collectAsStateWithLifecycle()
    val exifScanResults by viewModel.exifScanResults.collectAsStateWithLifecycle()
    val lastScrubbedResult by viewModel.lastScrubbedResult.collectAsStateWithLifecycle()
    val benchmarkResult by viewModel.benchmarkResult.collectAsStateWithLifecycle()
    val isRunning2026Task by viewModel.isRunning2026Task.collectAsStateWithLifecycle()

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = spec.bgBase,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { _ ->
            NavHost(
                navController = navController,
                startDestination = AppRoutes.ROUTE_HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(AppRoutes.ROUTE_HOME) {
                    HomeScreen(
                        storageOverview = storageOverview,
                        reportCount = reports.size,
                        privacyScore = privacyAudit.score,
                        onSelectiveEraseClick = {
                            viewModel.refreshStorageOverview()
                            viewModel.syncMethodWithDefault()
                            navController.navigate(AppRoutes.ROUTE_SELECTIVE_ERASE)
                        },
                        onFileExplorerClick = {
                            viewModel.openExplorerRoot()
                            navController.navigate(AppRoutes.ROUTE_FILE_EXPLORER)
                        },
                        onSecuritySuite2026Click = {
                            viewModel.refreshStorageOverview()
                            navController.navigate(AppRoutes.ROUTE_SECURITY_SUITE_2026)
                        },
                        onFactoryResetClick = {
                            viewModel.refreshStorageOverview()
                            viewModel.resetExecutionState()
                            navController.navigate(AppRoutes.ROUTE_FACTORY_RESET)
                        },
                        onThemeClick = {
                            navController.navigate(AppRoutes.ROUTE_SETTINGS_THEME)
                        },
                        onSettingsClick = {
                            navController.navigate(AppRoutes.ROUTE_SETTINGS)
                        },
                        onReportsClick = {
                            navController.navigate(AppRoutes.ROUTE_REPORT_HISTORY)
                        }
                    )
                }

                composable(AppRoutes.ROUTE_SELECTIVE_ERASE) {
                    SelectiveEraseScreen(
                        storageOverview = storageOverview,
                        settings = settings,
                        selectiveBuckets = selectiveBuckets,
                        queuedTargets = queuedTargets,
                        contactsPermissionGranted = contactsPermissionGranted,
                        onContactsPermissionResult = viewModel::onContactsPermissionResult,
                        onToggleParentGroup = viewModel::toggleParentGroupAll,
                        onToggleSubBucketAll = viewModel::toggleSubBucketAll,
                        onToggleSingleItem = viewModel::toggleSingleQueuedItem,
                        onPickedUrisForSubCategory = viewModel::addPickedUrisToSubCategory,
                        onMountFolder = { uri -> viewModel.mountSafFolder(uri, alsoQueueForShred = false) },
                        onSetFreeSpaceMb = viewModel::setFreeSpaceOverwriteMb,
                        onBack = { navController.popBackStack() },
                        onContinueToMethod = {
                            viewModel.syncMethodWithDefault()
                            navController.navigate(AppRoutes.ROUTE_METHOD_SELECTION)
                        }
                    )
                }

                composable(AppRoutes.ROUTE_FILE_EXPLORER) {
                    FileExplorerScreen(
                        storageOverview = storageOverview,
                        settings = settings,
                        breadcrumbs = explorerBreadcrumbs,
                        entries = explorerEntries,
                        selectedIds = explorerSelectedIds,
                        currentSort = explorerSort,
                        onBack = { navController.popBackStack() },
                        onNavigateUp = viewModel::navigateExplorerUp,
                        onBreadcrumbClick = viewModel::navigateExplorerBreadcrumb,
                        onEntryClick = viewModel::openExplorerEntry,
                        onToggleSelect = viewModel::toggleExplorerSelection,
                        onSelectAll = viewModel::selectAllExplorer,
                        onClearSelection = viewModel::clearExplorerSelection,
                        onSortChange = viewModel::setExplorerSort,
                        onMountFolder = { uri -> viewModel.mountSafFolder(uri, alsoQueueForShred = false) },
                        onUnmountFolder = viewModel::unmountSafFolder,
                        onPickFilesDirect = { uris ->
                            viewModel.addPickedDocumentUris(uris, SelectiveCategory.DOCUMENTS_FILES)
                            viewModel.syncMethodWithDefault()
                            navController.navigate(AppRoutes.ROUTE_METHOD_SELECTION)
                        },
                        onComputeSha256 = viewModel::computeFileSha256,
                        onShredSelected = {
                            if (viewModel.queueExplorerSelectionForShred()) {
                                navController.navigate(AppRoutes.ROUTE_METHOD_SELECTION)
                            }
                        }
                    )
                }

                composable(AppRoutes.ROUTE_METHOD_SELECTION) {
                    MethodSelectionScreen(
                        selectedMethodId = selectedMethodId,
                        recommendedMethod = viewModel.getRecommendedMethod(),
                        queuedTargets = queuedTargets,
                        onSelectMethod = viewModel::selectMethod,
                        onStepNavigate = { step ->
                            if (step == 1) {
                                navController.popBackStack()
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onConfirmShred = {
                            viewModel.resetExecutionState()
                            viewModel.startShredding()
                            navController.navigate(AppRoutes.ROUTE_SHRED_SUMMARY)
                        }
                    )
                }

                composable(AppRoutes.ROUTE_SHRED_SUMMARY) {
                    val activeMethod = ErasureCatalog.findById(selectedMethodId)
                    ShredSummaryScreen(
                        method = activeMethod,
                        queuedTargets = queuedTargets,
                        executionState = executionState,
                        reportEnabled = settings.enableErasureReport,
                        onStepNavigate = { step ->
                            when (step) {
                                1 -> navController.popBackStack(AppRoutes.ROUTE_SELECTIVE_ERASE, inclusive = false)
                                2 -> navController.popBackStack()
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onStartShredding = viewModel::startShredding,
                        onCancelShredding = viewModel::cancelShredding,
                        onViewReport = { reportId ->
                            viewModel.resetExecutionState()
                            navController.navigate(AppRoutes.reportDetail(reportId))
                        },
                        onDoneHome = {
                            viewModel.resetExecutionState()
                            navController.popBackStack(AppRoutes.ROUTE_HOME, inclusive = false)
                        }
                    )
                }

                composable(AppRoutes.ROUTE_SETTINGS) {
                    val baseTitle = UiThemeStyle.fromId(settings.uiThemeStyleId).title
                    val badgeTitle = if (settings.oledModeEnabled && settings.uiThemeStyleId != "oled") {
                        "$baseTitle • OLED"
                    } else baseTitle
                    SettingsRootScreen(
                        activeThemeTitle = badgeTitle,
                        onBack = { navController.popBackStack() },
                        onOpenThemeUi = { navController.navigate(AppRoutes.ROUTE_SETTINGS_THEME) },
                        onOpenSecuritySuite2026 = {
                            viewModel.refreshStorageOverview()
                            navController.navigate(AppRoutes.ROUTE_SECURITY_SUITE_2026)
                        },
                        onOpenGeneral = { navController.navigate(AppRoutes.ROUTE_SETTINGS_GENERAL) },
                        onOpenErasure = { navController.navigate(AppRoutes.ROUTE_SETTINGS_ERASURE) },
                        onOpenReportHistory = { navController.navigate(AppRoutes.ROUTE_REPORT_HISTORY) },
                        onOpenSubscription = { navController.navigate(AppRoutes.ROUTE_SUBSCRIPTION) },
                        onOpenFactoryReset = {
                            viewModel.refreshStorageOverview()
                            viewModel.resetExecutionState()
                            navController.navigate(AppRoutes.ROUTE_FACTORY_RESET)
                        }
                    )
                }

                composable(AppRoutes.ROUTE_SETTINGS_THEME) {
                    ThemeUiScreen(
                        settings = settings,
                        onSelectThemeStyle = viewModel::setUiThemeStyle,
                        onSetOledMode = viewModel::setOledModeEnabled,
                        onSelectAccentId = viewModel::setUiAccentId,
                        onSetDynamicColor = viewModel::setDynamicColorEnabled,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(AppRoutes.ROUTE_SETTINGS_GENERAL) {
                    GeneralSettingsScreen(
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onEnablePasscode = viewModel::enablePasscode,
                        onDisablePasscode = viewModel::disablePasscode,
                        onSelectLanguage = viewModel::setLanguage
                    )
                }

                composable(AppRoutes.ROUTE_SETTINGS_ERASURE) {
                    ErasureSettingsScreen(
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onSetSkipScan = viewModel::setSkipScan,
                        onSetDefaultMethod = viewModel::setDefaultMethod,
                        onSetFreeSpaceMb = viewModel::setFreeSpaceOverwriteMb,
                        onSetEnableReport = viewModel::setEnableErasureReport,
                        onSetThumbnailPreview = viewModel::setThumbnailPreview,
                        onSetShowHidden = viewModel::setShowHiddenFiles
                    )
                }

                composable(AppRoutes.ROUTE_REPORT_HISTORY) {
                    ReportHistoryScreen(
                        reports = reports,
                        onBack = { navController.popBackStack() },
                        onOpenReport = { id -> navController.navigate(AppRoutes.reportDetail(id)) },
                        onClearAll = viewModel::clearAllReports
                    )
                }

                composable(
                    route = AppRoutes.ROUTE_REPORT_DETAIL,
                    arguments = listOf(navArgument("reportId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
                    ReportDetailScreen(
                        reportId = reportId,
                        onLoadReport = viewModel::getReportById,
                        buildShareText = viewModel::buildShareableReportText,
                        onDeleteReport = viewModel::deleteReport,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(AppRoutes.ROUTE_SUBSCRIPTION) {
                    SubscriptionScreen(
                        settings = settings,
                        purchases = usdtPurchases,
                        verificationState = usdtVerificationState,
                        onSaveMerchantAddress = viewModel::setUsdtMerchantAddress,
                        onVerifyUsdtPurchase = viewModel::verifyUsdtPurchase,
                        onResetVerificationState = viewModel::resetUsdtVerificationState,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(AppRoutes.ROUTE_FACTORY_RESET) {
                    FactoryResetScreen(
                        settings = settings,
                        storageOverview = storageOverview,
                        mountedFolders = mountedFolders,
                        contactsPermissionGranted = contactsPermissionGranted,
                        executionState = executionState,
                        reportCount = reports.size,
                        usdtPurchaseCount = usdtPurchases.size,
                        onContactsPermissionResult = viewModel::onContactsPermissionResult,
                        onMountFolder = { uri -> viewModel.mountSafFolder(uri, alsoQueueForShred = false) },
                        onStartDevicePreReset = viewModel::startPreResetDeviceWipe,
                        onCancelShredding = viewModel::cancelShredding,
                        onViewReport = { reportId ->
                            viewModel.resetExecutionState()
                            navController.navigate(AppRoutes.reportDetail(reportId))
                        },
                        onPerformAppFactoryReset = { clearUsdt ->
                            viewModel.performAppFactoryReset(clearUsdtReceipts = clearUsdt) {
                                navController.popBackStack(AppRoutes.ROUTE_HOME, inclusive = false)
                            }
                        },
                        onResetExecutionState = viewModel::resetExecutionState,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(AppRoutes.ROUTE_SECURITY_SUITE_2026) {
                    SecuritySuite2026Screen(
                        settings = settings,
                        privacyAudit = privacyAudit,
                        exifScanResults = exifScanResults,
                        lastScrubbedResult = lastScrubbedResult,
                        benchmarkResult = benchmarkResult,
                        isBusy = isRunning2026Task,
                        onRunDeepHygieneSweep = viewModel::runImmediate2026HygieneSweep,
                        onInspectExifUris = viewModel::inspectExifPhotos,
                        onScrubExifToUri = viewModel::scrubExifPhoto,
                        onClearExifResults = viewModel::clearExifScanResults,
                        onUpdateAutoShred = viewModel::updateAutoShredConfig,
                        onRunBenchmark = viewModel::runHardwareBenchmark,
                        onApplyRecommendedMethod = viewModel::setDefaultMethod,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (isAppLocked) {
            PasscodeLockOverlay(onVerifyPin = viewModel::verifyPasscode)
        }
    }
}
