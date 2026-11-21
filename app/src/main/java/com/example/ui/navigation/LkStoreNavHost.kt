package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.firebase.MarketplaceRepository
import com.example.data.model.MarketplaceApp
import com.example.data.model.UiState
import com.example.ui.components.LkStoreTopBar
import com.example.ui.screens.AccountHubScreen
import com.example.ui.screens.AppDetailScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DeveloperProfileScreen
import com.example.ui.screens.DownloadHistoryScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ReviewsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.viewmodel.AppDetailViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.google.firebase.firestore.FirebaseFirestore

object LkRoutes {
    const val HOME = "home"
    const val APPS = "apps"
    const val GAMES = "games"
    const val CATEGORIES = "categories"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val WISHLIST = "wishlist"
    const val DOWNLOADS = "downloads"
    const val NOTIFICATIONS = "notifications"
    const val ACCOUNT = "account"
    const val HELP = "help"
    const val APP_DETAIL = "app_detail/{appId}"
    const val DEVELOPER = "developer/{developerId}/{developerName}"
    const val REVIEWS = "reviews/{appId}"

    fun appDetail(appId: String) = "app_detail/${Uri.encode(appId)}"
    fun developer(developerId: String, developerName: String): String =
        "developer/${Uri.encode(developerId.ifBlank { "dev" })}/${Uri.encode(developerName.ifBlank { "Developer" })}"
    fun reviews(appId: String) = "reviews/${Uri.encode(appId)}"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(LkRoutes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    TopLevelDestination(LkRoutes.APPS, "Apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    TopLevelDestination(LkRoutes.GAMES, "Games", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    TopLevelDestination(LkRoutes.CATEGORIES, "Categories", Icons.Rounded.Category, Icons.Outlined.Category),
    TopLevelDestination(LkRoutes.LIBRARY, "Library", Icons.Rounded.LibraryAddCheck, Icons.Outlined.LibraryAddCheck)
)

@Composable
fun LkStoreMainShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    userPhotoUrl: String?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val storeViewModel: StoreViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(MarketplaceRepository(db), currentUserId)
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: LkRoutes.HOME
    val snackbarHostState = remember { SnackbarHostState() }

    val appsState by storeViewModel.appsState.collectAsStateWithLifecycle()
    val userProfileState by storeViewModel.userProfileState.collectAsStateWithLifecycle()
    val wishlistState by storeViewModel.wishlistState.collectAsStateWithLifecycle()
    val libraryState by storeViewModel.libraryState.collectAsStateWithLifecycle()
    val downloadHistoryState by storeViewModel.downloadHistoryState.collectAsStateWithLifecycle()
    val notificationsState by storeViewModel.notificationsState.collectAsStateWithLifecycle()

    val wishlistIds by storeViewModel.wishlistIds.collectAsStateWithLifecycle()
    val unreadNotifications by storeViewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val availableUpdates by storeViewModel.availableUpdates.collectAsStateWithLifecycle()
    val updateAppIds = remember(availableUpdates) { availableUpdates.map { it.latestApp.id }.toSet() }

    val searchQuery by storeViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedAppCategory by storeViewModel.selectedAppCategory.collectAsStateWithLifecycle()
    val selectedGameGenre by storeViewModel.selectedGameGenre.collectAsStateWithLifecycle()
    val sortOption by storeViewModel.sortOption.collectAsStateWithLifecycle()
    val searchOnlyVerified by storeViewModel.searchOnlyVerified.collectAsStateWithLifecycle()
    val searchMinRating by storeViewModel.searchMinRating.collectAsStateWithLifecycle()
    val searchTypeFilter by storeViewModel.searchTypeFilter.collectAsStateWithLifecycle()

    LaunchedEffect(storeViewModel) {
        storeViewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isTopLevel = TopLevelDestinations.any { it.route == currentRoute }
    val topBarTitle = when {
        currentRoute == LkRoutes.HOME -> null
        currentRoute == LkRoutes.APPS -> "Apps"
        currentRoute == LkRoutes.GAMES -> "Games"
        currentRoute == LkRoutes.CATEGORIES -> "Categories"
        currentRoute == LkRoutes.LIBRARY -> "Library & Updates"
        currentRoute == LkRoutes.SEARCH -> "Search Catalog"
        currentRoute == LkRoutes.WISHLIST -> "Wishlist"
        currentRoute == LkRoutes.DOWNLOADS -> "Download History"
        currentRoute == LkRoutes.NOTIFICATIONS -> "Notifications"
        currentRoute == LkRoutes.ACCOUNT -> "Account"
        currentRoute == LkRoutes.HELP -> "Help & Support"
        currentRoute.startsWith("app_detail") -> "App Details"
        currentRoute.startsWith("developer") -> "Developer Profile"
        currentRoute.startsWith("reviews") -> "Ratings & Reviews"
        else -> null
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LkStoreTopBar(
                    title = topBarTitle,
                    showBack = !isTopLevel,
                    unreadNotifications = unreadNotifications,
                    userPhotoUrl = userPhotoUrl,
                    onBack = { navController.popBackStack() },
                    onSearchClick = { navController.navigate(LkRoutes.SEARCH) { launchSingleTop = true } },
                    onNotificationsClick = { navController.navigate(LkRoutes.NOTIFICATIONS) { launchSingleTop = true } },
                    onAccountClick = { navController.navigate(LkRoutes.ACCOUNT) { launchSingleTop = true } }
                )
            },
            bottomBar = {
                if (!useNavRail && isTopLevel) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest.route == LkRoutes.LIBRARY && availableUpdates.isNotEmpty()) {
                                                Badge { Text(availableUpdates.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_bar_${dest.route}")
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
                if (useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest.route == LkRoutes.LIBRARY && availableUpdates.isNotEmpty()) {
                                                Badge { Text(availableUpdates.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    }
                                },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = LkRoutes.HOME
                    ) {
                        composable(LkRoutes.HOME) {
                            HomeScreen(
                                appsState = appsState,
                                wishlistIds = wishlistIds,
                                availableUpdates = availableUpdates,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onOpenAppsTab = { navigateTopLevel(LkRoutes.APPS) },
                                onOpenGamesTab = { navigateTopLevel(LkRoutes.GAMES) },
                                onOpenCategories = { navigateTopLevel(LkRoutes.CATEGORIES) },
                                onOpenLibrary = { navigateTopLevel(LkRoutes.LIBRARY) },
                                onOpenWishlist = { navController.navigate(LkRoutes.WISHLIST) },
                                onOpenDownloads = { navController.navigate(LkRoutes.DOWNLOADS) },
                                onOpenHelp = { navController.navigate(LkRoutes.HELP) },
                                onSelectCategory = { cat ->
                                    if (cat.equals("Games", true)) {
                                        navigateTopLevel(LkRoutes.GAMES)
                                    } else {
                                        storeViewModel.selectAppCategory(cat)
                                        navigateTopLevel(LkRoutes.APPS)
                                    }
                                },
                                onToggleWishlist = storeViewModel::toggleWishlist,
                                onQuickDownload = { app -> storeViewModel.downloadVerifiedApk(context, app) }
                            )
                        }

                        composable(LkRoutes.APPS) {
                            AppsScreen(
                                appsState = appsState,
                                selectedCategory = selectedAppCategory,
                                sortOption = sortOption,
                                wishlistIds = wishlistIds,
                                updateAppIds = updateAppIds,
                                onSelectCategory = storeViewModel::selectAppCategory,
                                onSelectSort = storeViewModel::selectSortOption,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onToggleWishlist = storeViewModel::toggleWishlist,
                                onQuickDownload = { app -> storeViewModel.downloadVerifiedApk(context, app) }
                            )
                        }

                        composable(LkRoutes.GAMES) {
                            GamesScreen(
                                appsState = appsState,
                                selectedGenre = selectedGameGenre,
                                sortOption = sortOption,
                                wishlistIds = wishlistIds,
                                updateAppIds = updateAppIds,
                                onSelectGenre = storeViewModel::selectGameGenre,
                                onSelectSort = storeViewModel::selectSortOption,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onToggleWishlist = storeViewModel::toggleWishlist,
                                onQuickDownload = { app -> storeViewModel.downloadVerifiedApk(context, app) }
                            )
                        }

                        composable(LkRoutes.CATEGORIES) {
                            CategoriesScreen(
                                appsState = appsState,
                                onCategorySelected = { category, isGame ->
                                    if (isGame) {
                                        storeViewModel.selectGameGenre("All")
                                        navigateTopLevel(LkRoutes.GAMES)
                                    } else {
                                        storeViewModel.selectAppCategory(category)
                                        navigateTopLevel(LkRoutes.APPS)
                                    }
                                }
                            )
                        }

                        composable(LkRoutes.SEARCH) {
                            SearchScreen(
                                appsState = appsState,
                                query = searchQuery,
                                typeFilter = searchTypeFilter,
                                onlyVerified = searchOnlyVerified,
                                minRating = searchMinRating,
                                wishlistIds = wishlistIds,
                                updateAppIds = updateAppIds,
                                onQueryChange = storeViewModel::updateSearchQuery,
                                onTypeFilterChange = storeViewModel::setSearchTypeFilter,
                                onOnlyVerifiedChange = storeViewModel::setSearchOnlyVerified,
                                onMinRatingChange = storeViewModel::setSearchMinRating,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onToggleWishlist = storeViewModel::toggleWishlist,
                                onQuickDownload = { app -> storeViewModel.downloadVerifiedApk(context, app) }
                            )
                        }

                        composable(
                            route = LkRoutes.APP_DETAIL,
                            arguments = listOf(navArgument("appId") { type = NavType.StringType })
                        ) { entry ->
                            val appId = entry.arguments?.getString("appId") ?: return@composable
                            val detailVm: AppDetailViewModel = viewModel(
                                key = "${currentUserId}_$appId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                        AppDetailViewModel(MarketplaceRepository(db), appId, currentUserId)
                                    }
                                }
                            )
                            val detailAppState by detailVm.appState.collectAsStateWithLifecycle()
                            val reviewsState by detailVm.reviewsState.collectAsStateWithLifecycle()
                            val installedVersion = (libraryState as? UiState.Success)?.data
                                ?.firstOrNull { it.appId == appId }?.versionCode

                            LaunchedEffect(detailVm) {
                                detailVm.events.collect { msg -> storeViewModel.emitMessage(msg) }
                            }

                            AppDetailScreen(
                                appState = detailAppState,
                                reviewsState = reviewsState,
                                isWishlisted = wishlistIds.contains(appId),
                                installedVersionCode = installedVersion,
                                onDeveloperClick = { devId, devName ->
                                    navController.navigate(LkRoutes.developer(devId, devName))
                                },
                                onOpenAllReviews = { id ->
                                    navController.navigate(LkRoutes.reviews(id))
                                },
                                onToggleWishlist = storeViewModel::toggleWishlist,
                                onDownloadApk = { app -> storeViewModel.downloadVerifiedApk(context, app) },
                                onSubmitReport = { app, reason, details, done ->
                                    detailVm.submitReport(app, reason, details, done)
                                }
                            )
                        }

                        composable(
                            route = LkRoutes.DEVELOPER,
                            arguments = listOf(
                                navArgument("developerId") { type = NavType.StringType },
                                navArgument("developerName") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val devId = entry.arguments?.getString("developerId") ?: ""
                            val devName = entry.arguments?.getString("developerName") ?: ""
                            val devProfileFlow = remember(devId) {
                                val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                                MarketplaceRepository(db).observeDeveloper(devId)
                            }
                            val devProfile by devProfileFlow.collectAsStateWithLifecycle(initialValue = null)

                            DeveloperProfileScreen(
                                developerId = devId,
                                developerName = devName,
                                developerProfile = devProfile,
                                appsState = appsState,
                                wishlistIds = wishlistIds,
                                updateAppIds = updateAppIds,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onToggleWishlist = storeViewModel::toggleWishlist,
                                onQuickDownload = { app -> storeViewModel.downloadVerifiedApk(context, app) }
                            )
                        }

                        composable(
                            route = LkRoutes.REVIEWS,
                            arguments = listOf(navArgument("appId") { type = NavType.StringType })
                        ) { entry ->
                            val appId = entry.arguments?.getString("appId") ?: return@composable
                            val detailVm: AppDetailViewModel = viewModel(
                                key = "${currentUserId}_$appId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                        AppDetailViewModel(MarketplaceRepository(db), appId, currentUserId)
                                    }
                                }
                            )
                            val detailAppState by detailVm.appState.collectAsStateWithLifecycle()
                            val reviewsState by detailVm.reviewsState.collectAsStateWithLifecycle()
                            val profileName = (userProfileState as? UiState.Success)?.data?.displayName
                                ?.ifBlank { userDisplayName } ?: userDisplayName

                            LaunchedEffect(detailVm) {
                                detailVm.events.collect { msg -> storeViewModel.emitMessage(msg) }
                            }

                            ReviewsScreen(
                                appState = detailAppState,
                                reviewsState = reviewsState,
                                currentUserId = currentUserId,
                                currentUserName = profileName,
                                currentUserAvatar = userPhotoUrl,
                                onSubmitReview = { versionName, rating, comment, done ->
                                    detailVm.submitReview(
                                        userName = profileName,
                                        userAvatarUrl = userPhotoUrl,
                                        versionName = versionName,
                                        rating = rating,
                                        comment = comment,
                                        onComplete = done
                                    )
                                },
                                onDeleteMyReview = detailVm::deleteMyReview
                            )
                        }

                        composable(LkRoutes.LIBRARY) {
                            LibraryScreen(
                                libraryState = libraryState,
                                appsState = appsState,
                                availableUpdates = availableUpdates,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onUpdateApp = { app -> storeViewModel.downloadVerifiedApk(context, app) },
                                onRemoveFromLibrary = storeViewModel::removeFromLibrary,
                                onBrowseStore = { navigateTopLevel(LkRoutes.HOME) }
                            )
                        }

                        composable(LkRoutes.WISHLIST) {
                            WishlistScreen(
                                wishlistState = wishlistState,
                                appsState = appsState,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onRemoveWishlist = storeViewModel::toggleWishlist,
                                onRemoveWishlistFallback = { wish ->
                                    storeViewModel.toggleWishlist(
                                        MarketplaceApp(id = wish.appId, title = wish.appTitle)
                                    )
                                },
                                onBrowseStore = { navigateTopLevel(LkRoutes.HOME) }
                            )
                        }

                        composable(LkRoutes.DOWNLOADS) {
                            DownloadHistoryScreen(
                                historyState = downloadHistoryState,
                                appsState = appsState,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) },
                                onRedownload = { app -> storeViewModel.downloadVerifiedApk(context, app) }
                            )
                        }

                        composable(LkRoutes.NOTIFICATIONS) {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                availableUpdates = availableUpdates,
                                onMarkRead = storeViewModel::markNotificationRead,
                                onMarkAllRead = storeViewModel::markAllNotificationsRead,
                                onDeleteNotification = storeViewModel::deleteNotification,
                                onOpenApp = { appId -> navController.navigate(LkRoutes.appDetail(appId)) }
                            )
                        }

                        composable(LkRoutes.ACCOUNT) {
                            val wCount = (wishlistState as? UiState.Success)?.data?.size ?: 0
                            val lCount = (libraryState as? UiState.Success)?.data?.size ?: 0
                            val dCount = (downloadHistoryState as? UiState.Success)?.data?.size ?: 0

                            AccountHubScreen(
                                userEmail = userEmail,
                                fallbackDisplayName = userDisplayName,
                                fallbackPhotoUrl = userPhotoUrl,
                                userProfileState = userProfileState,
                                wishlistCount = wCount,
                                libraryCount = lCount,
                                updatesCount = availableUpdates.size,
                                downloadsCount = dCount,
                                unreadNotifications = unreadNotifications,
                                onUpdateProfile = storeViewModel::updateProfile,
                                onOpenLibrary = { navigateTopLevel(LkRoutes.LIBRARY) },
                                onOpenWishlist = { navController.navigate(LkRoutes.WISHLIST) },
                                onOpenDownloads = { navController.navigate(LkRoutes.DOWNLOADS) },
                                onOpenNotifications = { navController.navigate(LkRoutes.NOTIFICATIONS) },
                                onOpenHelp = { navController.navigate(LkRoutes.HELP) },
                                onSignOut = onSignOut
                            )
                        }

                        composable(LkRoutes.HELP) {
                            val profileName = (userProfileState as? UiState.Success)?.data?.displayName
                                ?.ifBlank { userDisplayName } ?: userDisplayName
                            HelpSupportScreen(
                                userName = profileName,
                                onSubmitTicket = { subject, category, message, done ->
                                    storeViewModel.submitSupportTicket(
                                        userName = profileName,
                                        subject = subject,
                                        category = category,
                                        message = message,
                                        onComplete = done
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
