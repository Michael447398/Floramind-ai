package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.configureAppCheck
import com.example.data.ai.PlantAiRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.FloraRepository
import com.example.data.research.BotanicalResearchCoordinator
import com.example.model.BotanicalReport
import com.example.ui.components.FloraAnimatedSplashScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AskFloraMindScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FullReportScreen
import com.example.ui.screens.IdentifyResultScreen
import com.example.ui.screens.IdentifyScreen
import com.example.ui.screens.LandingAndAuthScreen
import com.example.ui.screens.MyCollectionScreen
import com.example.ui.screens.MyPlantsScreen
import com.example.ui.screens.PricingUpgradeScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.theme.FloraMindTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AskFloraMindViewModel
import com.example.ui.viewmodel.IdentifyViewModel
import com.example.ui.viewmodel.LibraryCompareViewModel
import com.example.ui.viewmodel.MainSessionViewModel
import com.example.ui.viewmodel.SavedHistoryViewModel
import com.google.firebase.auth.FirebaseUser

private object FloraRoutes {
    const val DASHBOARD = "dashboard"
    const val IDENTIFY = "identify"
    const val DISCOVER = "discover"
    const val COLLECTION = "collection"
    const val MY_PLANTS = "my_plants"
    const val ASK = "ask"
    const val RESULT = "result"
    const val FULL_REPORT = "full_report"
    const val COMPARE = "compare"
    const val PROFILE = "profile"
    const val UPGRADE = "upgrade"
    const val ADMIN = "admin"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)

        setContent {
            val context = LocalContext.current
            val authRepository = remember { AuthRepository() }
            val floraRepository = remember { FloraRepository(context.applicationContext) }
            val aiRepository = remember { PlantAiRepository() }
            val researchCoordinator = remember { BotanicalResearchCoordinator() }

            var authChecked by remember { mutableStateOf(false) }
            val currentUser by authRepository.observeAuthState()
                .collectAsStateWithLifecycle(initialValue = authRepository.currentUser())

            LaunchedEffect(Unit) {
                authRepository.attemptSilentSignIn(context)
                authChecked = true
            }

            if (!authChecked) {
                FloraMindTheme(darkTheme = false) {
                    FloraAnimatedSplashScreen()
                }
            } else if (currentUser == null) {
                val publicSessionVm = remember {
                    MainSessionViewModel(floraRepository, null)
                }
                val appConfig by publicSessionVm.appConfig.collectAsStateWithLifecycle()
                val curatedPlants by publicSessionVm.curatedPlants.collectAsStateWithLifecycle()
                val announcements by publicSessionVm.announcements.collectAsStateWithLifecycle()

                FloraMindTheme(darkTheme = false) {
                    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                        LandingAndAuthScreen(
                            authRepository = authRepository,
                            curatedPlants = curatedPlants,
                            announcements = announcements,
                            appConfig = appConfig,
                            onSignedIn = { },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            } else {
                val signedInUser = currentUser!!
                androidx.compose.runtime.key(signedInUser.uid) {
                    AuthenticatedAppShell(
                        user = signedInUser,
                        authRepository = authRepository,
                        floraRepository = floraRepository,
                        aiRepository = aiRepository,
                        researchCoordinator = researchCoordinator
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    user: FirebaseUser,
    authRepository: AuthRepository,
    floraRepository: FloraRepository,
    aiRepository: PlantAiRepository,
    researchCoordinator: BotanicalResearchCoordinator
) {
    val context = LocalContext.current
    val sessionVm = remember(user.uid) {
        MainSessionViewModel(floraRepository, user, context.applicationContext)
    }
    val identifyVm = remember(user.uid) {
        IdentifyViewModel(aiRepository, researchCoordinator, floraRepository, user.uid)
    }
    val libraryCompareVm = remember(user.uid) {
        LibraryCompareViewModel(aiRepository, researchCoordinator)
    }
    val savedHistoryVm = remember(user.uid) {
        SavedHistoryViewModel(floraRepository, user.uid)
    }
    val askVm = remember(user.uid) {
        AskFloraMindViewModel(aiRepository, floraRepository)
    }
    val adminVm = remember(user.uid, sessionVm.isVerifiedOwnerAdmin) {
        if (sessionVm.isVerifiedOwnerAdmin) AdminViewModel(floraRepository) else null
    }

    val appConfig by sessionVm.appConfig.collectAsStateWithLifecycle()
    val curatedPlants by sessionVm.curatedPlants.collectAsStateWithLifecycle()
    val announcements by sessionVm.announcements.collectAsStateWithLifecycle()
    val userProfile by sessionVm.userProfile.collectAsStateWithLifecycle()
    val entitlement by sessionVm.entitlement.collectAsStateWithLifecycle()
    val productLoadState by sessionVm.productLoadState.collectAsStateWithLifecycle()
    val billingFlowState by sessionVm.billingFlowState.collectAsStateWithLifecycle()
    val themeOverride by sessionVm.themeOverrideDark.collectAsStateWithLifecycle()
    val statusMessage by sessionVm.statusBannerMessage.collectAsStateWithLifecycle()
    val isPremium = entitlement.isPremiumActive()

    val scansState by savedHistoryVm.scansState.collectAsStateWithLifecycle()
    val savedPlantsState by savedHistoryVm.savedPlantsState.collectAsStateWithLifecycle()
    val folders by savedHistoryVm.folders.collectAsStateWithLifecycle()

    val activeReport by identifyVm.activeReport.collectAsStateWithLifecycle()
    val activeThumb by identifyVm.activeThumbnailBase64.collectAsStateWithLifecycle()
    val activeLocalPath by identifyVm.activeLocalPath.collectAsStateWithLifecycle()
    val compareList by libraryCompareVm.compareList.collectAsStateWithLifecycle()

    val isDark = themeOverride ?: userProfile?.darkMode ?: false
    var identifyStartInCamera by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: FloraRoutes.DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userProfile?.advancedBotanyMode) {
        userProfile?.let { askVm.setAdvancedMode(it.advancedBotanyMode) }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            sessionVm.clearStatusBanner()
        }
    }

    val tabs = remember {
        listOf(
            BottomNavTab(FloraRoutes.DASHBOARD, "Home", Icons.Filled.Home, "nav_home"),
            BottomNavTab(FloraRoutes.IDENTIFY, "Identify", Icons.Filled.CameraAlt, "nav_identify"),
            BottomNavTab(FloraRoutes.DISCOVER, "Discover", Icons.Filled.Explore, "nav_discover"),
            BottomNavTab(FloraRoutes.COLLECTION, "Collection", Icons.Filled.Bookmarks, "nav_saved"),
            BottomNavTab(FloraRoutes.ASK, "Ask AI", Icons.Filled.AutoAwesome, "nav_ask")
        )
    }
    val showTopLevelNav = currentRoute in tabs.map { it.route }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    FloraMindTheme(darkTheme = isDark) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showTopLevelNav && !useRail) {
                        NavigationBar {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = {
                                        if (tab.route == FloraRoutes.IDENTIFY) {
                                            identifyStartInCamera = false
                                        }
                                        navigateTopTab(tab.route)
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showTopLevelNav && useRail) {
                        NavigationRail {
                            tabs.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentRoute == tab.route,
                                    onClick = {
                                        if (tab.route == FloraRoutes.IDENTIFY) {
                                            identifyStartInCamera = false
                                        }
                                        navigateTopTab(tab.route)
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = FloraRoutes.DASHBOARD,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                        ) {
                            composable(FloraRoutes.DASHBOARD) {
                                val recentScans = (scansState as? com.example.ui.viewmodel.CollectionUiState.Success)?.items.orEmpty()
                                val savedList = (savedPlantsState as? com.example.ui.viewmodel.CollectionUiState.Success)?.items.orEmpty()
                                DashboardScreen(
                                    userProfile = userProfile,
                                    fallbackDisplayName = user.displayName ?: "Botanist",
                                    photoUrl = user.photoUrl?.toString() ?: userProfile?.photoUrl,
                                    appConfig = appConfig,
                                    recentScans = recentScans,
                                    savedPlants = savedList,
                                    curatedPlants = curatedPlants,
                                    onNavigateIdentifyCamera = {
                                        identifyStartInCamera = true
                                        navigateTopTab(FloraRoutes.IDENTIFY)
                                    },
                                    onNavigateIdentifyGallery = {
                                        identifyStartInCamera = false
                                        navigateTopTab(FloraRoutes.IDENTIFY)
                                    },
                                    onNavigateDiscoverSearch = { q, cat ->
                                        libraryCompareVm.updateSearchQuery(q)
                                        libraryCompareVm.selectCategory(cat)
                                        navigateTopTab(FloraRoutes.DISCOVER)
                                    },
                                    onNavigateAskFloraMind = { navigateTopTab(FloraRoutes.ASK) },
                                    onNavigateCompare = { navController.navigate(FloraRoutes.COMPARE) },
                                    onNavigateCollection = { navigateTopTab(FloraRoutes.COLLECTION) },
                                    onNavigateMyPlants = { navController.navigate(FloraRoutes.MY_PLANTS) },
                                    onNavigateProfile = { navController.navigate(FloraRoutes.PROFILE) },
                                    entitlement = entitlement,
                                    onNavigateUpgrade = { navController.navigate(FloraRoutes.UPGRADE) },
                                    onOpenReport = { rep, thumb ->
                                        identifyVm.openExistingReport(rep, thumb, null)
                                        libraryCompareVm.recordRecentlyViewed(rep)
                                        navController.navigate(FloraRoutes.FULL_REPORT)
                                    },
                                    onOpenCuratedPlant = { plant ->
                                        libraryCompareVm.openFullDossier(
                                            scientificName = plant.scientificName,
                                            commonName = plant.commonName,
                                            regionContext = userProfile?.region ?: "Global"
                                        ) { full ->
                                            identifyVm.openExistingReport(full, null, null)
                                            navController.navigate(FloraRoutes.FULL_REPORT)
                                        }
                                    }
                                )
                            }

                            composable(FloraRoutes.IDENTIFY) {
                                IdentifyScreen(
                                    viewModel = identifyVm,
                                    appConfig = appConfig,
                                    userProfile = userProfile,
                                    initialCameraMode = identifyStartInCamera,
                                    onResultReady = {
                                        navController.navigate(FloraRoutes.RESULT)
                                    },
                                    entitlement = entitlement,
                                    onNavigateUpgrade = { navController.navigate(FloraRoutes.UPGRADE) }
                                )
                            }

                            composable(FloraRoutes.RESULT) {
                                val report = activeReport ?: BotanicalReport()
                                IdentifyResultScreen(
                                    report = report,
                                    thumbnailBase64 = activeThumb,
                                    localImagePath = activeLocalPath,
                                    folders = folders,
                                    onBack = {
                                        identifyVm.startNewScan()
                                        navController.popBackStack()
                                    },
                                    onOpenFullReport = { navController.navigate(FloraRoutes.FULL_REPORT) },
                                    onSavePlant = { fId, fName, tags, notes ->
                                        identifyVm.saveActivePlantToCollection(fId, fName, tags, notes)
                                    },
                                    onAddToCompare = {
                                        libraryCompareVm.addToCompare(report)
                                        navController.navigate(FloraRoutes.COMPARE)
                                    },
                                    onAskFloraMind = { navigateTopTab(FloraRoutes.ASK) },
                                    onNewScan = {
                                        identifyVm.startNewScan()
                                        navController.popBackStack()
                                    },
                                    onSubmitFeedback = { cat, plantName, msg ->
                                        sessionVm.submitFeedback(cat, plantName, msg)
                                    },
                                    isPremium = isPremium,
                                    onNavigateUpgrade = { navController.navigate(FloraRoutes.UPGRADE) }
                                )
                            }

                            composable(FloraRoutes.FULL_REPORT) {
                                val report = activeReport ?: BotanicalReport()
                                FullReportScreen(
                                    report = report,
                                    thumbnailBase64 = activeThumb,
                                    localImagePath = activeLocalPath,
                                    folders = folders,
                                    onBack = { navController.popBackStack() },
                                    onSavePlant = { fId, fName, tags, notes ->
                                        identifyVm.saveActivePlantToCollection(fId, fName, tags, notes)
                                    },
                                    onAddToCompare = {
                                        libraryCompareVm.addToCompare(report)
                                        navController.navigate(FloraRoutes.COMPARE)
                                    },
                                    onAskFloraMind = { navigateTopTab(FloraRoutes.ASK) },
                                    isPremium = isPremium,
                                    onNavigateUpgrade = { navController.navigate(FloraRoutes.UPGRADE) }
                                )
                            }

                            composable(FloraRoutes.DISCOVER) {
                                DiscoverScreen(
                                    viewModel = libraryCompareVm,
                                    curatedPlants = curatedPlants,
                                    userRegion = userProfile?.region ?: "Global",
                                    compareCount = compareList.size,
                                    onOpenDossierReport = { full ->
                                        identifyVm.openExistingReport(full, null, null)
                                        navController.navigate(FloraRoutes.FULL_REPORT)
                                    },
                                    onNavigateCompare = { navController.navigate(FloraRoutes.COMPARE) }
                                )
                            }

                            composable(FloraRoutes.COMPARE) {
                                CompareScreen(
                                    viewModel = libraryCompareVm,
                                    curatedPlants = curatedPlants,
                                    userRegion = userProfile?.region ?: "Global",
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(FloraRoutes.COLLECTION) {
                                MyCollectionScreen(
                                    currentUser = user,
                                    userProfile = userProfile,
                                    viewModel = savedHistoryVm,
                                    onOpenReport = { rep, thumb ->
                                        identifyVm.openExistingReport(rep, thumb, null)
                                        libraryCompareVm.recordRecentlyViewed(rep)
                                        navController.navigate(FloraRoutes.FULL_REPORT)
                                    },
                                    onAddToCompare = { rep ->
                                        libraryCompareVm.addToCompare(rep)
                                        navController.navigate(FloraRoutes.COMPARE)
                                    },
                                    onNavigateIdentify = {
                                        identifyStartInCamera = false
                                        navigateTopTab(FloraRoutes.IDENTIFY)
                                    },
                                    onOpenGridView = { navController.navigate(FloraRoutes.MY_PLANTS) }
                                )
                            }

                            composable(FloraRoutes.MY_PLANTS) {
                                MyPlantsScreen(
                                    viewModel = savedHistoryVm,
                                    onOpenReport = { rep, thumb ->
                                        identifyVm.openExistingReport(rep, thumb, null)
                                        libraryCompareVm.recordRecentlyViewed(rep)
                                        navController.navigate(FloraRoutes.FULL_REPORT)
                                    },
                                    onAddToCompare = { rep ->
                                        libraryCompareVm.addToCompare(rep)
                                        navController.navigate(FloraRoutes.COMPARE)
                                    },
                                    onNavigateIdentify = {
                                        identifyStartInCamera = false
                                        navigateTopTab(FloraRoutes.IDENTIFY)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(FloraRoutes.ASK) {
                                AskFloraMindScreen(
                                    viewModel = askVm,
                                    activeReport = activeReport,
                                    onClearActiveReport = { identifyVm.clearActiveReport() },
                                    userRegion = userProfile?.region ?: "Global"
                                )
                            }

                            composable(FloraRoutes.PROFILE) {
                                ProfileAndSettingsScreen(
                                    currentUser = user,
                                    userProfile = userProfile,
                                    appConfig = appConfig,
                                    announcements = announcements,
                                    isVerifiedOwnerAdmin = sessionVm.isVerifiedOwnerAdmin,
                                    authRepository = authRepository,
                                    floraRepository = floraRepository,
                                    onSavePreferences = { name, region, dark, thumb, adv ->
                                        sessionVm.updateProfilePreferences(name, region, dark, thumb, adv)
                                    },
                                    onDarkModeToggleImmediate = { dark ->
                                        sessionVm.setLocalDarkOverride(dark)
                                    },
                                    onSubmitFeedback = { cat, plantName, msg ->
                                        sessionVm.submitFeedback(cat, plantName, msg)
                                    },
                                    onOpenAdminConsole = { navController.navigate(FloraRoutes.ADMIN) },
                                    onSignedOut = { /* auth state listener swaps back to LandingAndAuthScreen */ },
                                    entitlement = entitlement,
                                    billingFlowState = billingFlowState,
                                    onNavigateUpgrade = { navController.navigate(FloraRoutes.UPGRADE) },
                                    onRestorePurchases = { sessionVm.restorePurchases() }
                                )
                            }

                            composable(FloraRoutes.UPGRADE) {
                                PricingUpgradeScreen(
                                    entitlement = entitlement,
                                    productLoadState = productLoadState,
                                    billingFlowState = billingFlowState,
                                    onPurchasePlan = { activity, plan ->
                                        sessionVm.purchasePlan(activity, plan)
                                    },
                                    onRestorePurchases = { sessionVm.restorePurchases() },
                                    onDismissBillingBanner = { sessionVm.clearBillingState() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(FloraRoutes.ADMIN) {
                                if (adminVm != null) {
                                    AdminDashboardScreen(
                                        viewModel = adminVm,
                                        appConfig = appConfig,
                                        isVerifiedOwnerAdmin = sessionVm.isVerifiedOwnerAdmin,
                                        onBack = { navController.popBackStack() }
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
