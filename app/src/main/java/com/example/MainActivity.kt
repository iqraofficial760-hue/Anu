package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
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
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutUser
import com.example.data.LocalGuestStore
import com.example.data.PartyRepository
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.formatCompactCoins
import com.example.ui.DirectChatViewModel
import com.example.ui.LiveRoomViewModel
import com.example.ui.PartyLobbyViewModel
import com.example.ui.components.CreateRoomDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DirectChatScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LiveDiscoverScreen
import com.example.ui.screens.LiveRoomScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.MomentsScreen
import com.example.ui.screens.NewcomerTaskScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PartyHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VipStoreScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.DeepObsidianText
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.MidnightPurple
import com.example.ui.theme.MutedLavender
import com.example.ui.theme.PartyGold
import com.example.ui.theme.PartyGoldDim
import com.example.ui.theme.PartyLiveTheme
import com.example.ui.theme.RoyalPlumSurface
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_LIVE = "live"
const val ROUTE_PARTY = "party"
const val ROUTE_MOMENTS = "moments"
const val ROUTE_MESSAGES = "messages"
const val ROUTE_PROFILE = "profile"
const val ROUTE_ROOM = "room/{roomId}"
const val ROUTE_CHAT = "chat/{conversationId}"
const val ROUTE_WALLET = "wallet"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_ADMIN = "admin"
const val ROUTE_VIP = "vip"
const val ROUTE_TASKS = "tasks"
const val ROUTE_SETTINGS = "settings"

private data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MainNavDestinations = listOf(
    BottomNavItem(ROUTE_LIVE, R.string.nav_live, Icons.Filled.Videocam, Icons.Outlined.Videocam),
    BottomNavItem(ROUTE_PARTY, R.string.nav_party, Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem(ROUTE_MOMENTS, R.string.nav_moments, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomNavItem(ROUTE_MESSAGES, R.string.nav_messages, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    BottomNavItem(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

private val TopLevelRoutes = setOf(ROUTE_LIVE, ROUTE_PARTY, ROUTE_MOMENTS, ROUTE_MESSAGES, ROUTE_PROFILE)

class MainActivity : ComponentActivity() {
    private var pendingDeepLinkRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalGuestStore.init(applicationContext)
        pendingDeepLinkRoute = parseDeepLinkRoute(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            PartyLiveTheme {
                AppNavigation(
                    deepLinkRoute = pendingDeepLinkRoute,
                    onDeepLinkConsumed = { pendingDeepLinkRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val parsed = parseDeepLinkRoute(intent)
        if (parsed != null) {
            pendingDeepLinkRoute = parsed
        }
    }

    private fun parseDeepLinkRoute(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "anulive") return null
        val host = data.host?.lowercase() ?: return null
        return when (host) {
            "live" -> ROUTE_LIVE
            "party" -> ROUTE_PARTY
            "moments" -> ROUTE_MOMENTS
            "messages" -> ROUTE_MESSAGES
            "me", "profile" -> ROUTE_PROFILE
            "wallet" -> ROUTE_WALLET
            "vip" -> ROUTE_VIP
            "tasks" -> ROUTE_TASKS
            "settings" -> ROUTE_SETTINGS
            "leaderboard" -> ROUTE_LEADERBOARD
            "notifications" -> ROUTE_NOTIFICATIONS
            "admin" -> ROUTE_ADMIN
            "room" -> {
                val roomId = data.pathSegments.firstOrNull()?.trim()
                if (!roomId.isNullOrBlank()) "room/$roomId" else ROUTE_LIVE
            }
            else -> ROUTE_LIVE
        }
    }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val guestId = remember(context) { LocalGuestStore.init(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    // Zero login wall: enter ANU LIVE immediately as a new user with 1M free coins.
    // Users can optionally connect Google Sign-In in Me > Settings for cloud sync.
    val activeUserId = currentUser?.uid ?: guestId
    val initialName = currentUser?.displayName ?: LocalGuestStore.profile.value.displayName
    val isGoogleSignedIn = currentUser != null

    key(activeUserId) {
        AnuLiveShell(
            currentUserId = activeUserId,
            initialDisplayName = initialName,
            isGoogleSignedIn = isGoogleSignedIn,
            deepLinkRoute = deepLinkRoute,
            onDeepLinkConsumed = onDeepLinkConsumed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnuLiveShell(
    currentUserId: String,
    initialDisplayName: String,
    isGoogleSignedIn: Boolean,
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val lobbyViewModel: PartyLobbyViewModel = viewModel(
        key = "anu_lobby_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                PartyLobbyViewModel(PartyRepository(db), currentUserId, initialDisplayName)
            }
        }
    )

    val userProfileState by lobbyViewModel.userProfile.collectAsStateWithLifecycle()
    val liveRoomsState by lobbyViewModel.liveRooms.collectAsStateWithLifecycle()
    val hostedRoomsState by lobbyViewModel.hostedRooms.collectAsStateWithLifecycle()
    val bookmarksState by lobbyViewModel.bookmarks.collectAsStateWithLifecycle()
    val followingState by lobbyViewModel.following.collectAsStateWithLifecycle()
    val transactionsState by lobbyViewModel.transactions.collectAsStateWithLifecycle()
    val notificationsState by lobbyViewModel.notifications.collectAsStateWithLifecycle()
    val momentsState by lobbyViewModel.moments.collectAsStateWithLifecycle()
    val conversationsState by lobbyViewModel.conversations.collectAsStateWithLifecycle()
    val reportsState by lobbyViewModel.reports.collectAsStateWithLifecycle()
    val selectedCategory by lobbyViewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by lobbyViewModel.searchQuery.collectAsStateWithLifecycle()
    val followedOnly by lobbyViewModel.followedOnly.collectAsStateWithLifecycle()

    var createDialogMode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        lobbyViewModel.events.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(deepLinkRoute) {
        if (!deepLinkRoute.isNullOrBlank()) {
            navController.navigate(deepLinkRoute) {
                launchSingleTop = true
            }
            snackbarHostState.showSnackbar("Termux opened: $deepLinkRoute")
            onDeepLinkConsumed()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTopLevel = currentRoute in TopLevelRoutes

    val userProfile = (userProfileState as? UiState.Success)?.data ?: UserProfile(id = currentUserId, userId = currentUserId, displayName = initialDisplayName)
    val displayName = userProfile.displayName
    val coinBalance = userProfile.coinBalance
    val currentXp = userProfile.xp
    val followedIds = remember(followingState) {
        ((followingState as? UiState.Success)?.data ?: emptyList()).map { it.targetUserId }.toSet()
    }
    val unreadNotifCount = remember(notificationsState) {
        ((notificationsState as? UiState.Success)?.data ?: emptyList()).count { !it.read }
    }
    val conversationsList = (conversationsState as? UiState.Success)?.data ?: emptyList()

    val triggerGoogleSync: () -> Unit = {
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                scope.launch { snackbarHostState.showSnackbar("Google Cloud Sync connected!") }
            },
            onAuthError = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            scope = scope
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MidnightPurple)) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MidnightPurple,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (isTopLevel) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = RoyalPlumSurface,
                            titleContentColor = PartyGold
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Videocam, contentDescription = null, tint = PartyGold, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PartyGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            Surface(
                                color = ElevatedPlum,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .border(1.dp, PartyGoldDim, RoundedCornerShape(50))
                                    .clickable { navController.navigate(ROUTE_WALLET) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = "Wallet", tint = PartyGold, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(formatCompactCoins(coinBalance), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (currentRoute == ROUTE_LIVE || currentRoute == ROUTE_PARTY) {
                    ExtendedFloatingActionButton(
                        onClick = { createDialogMode = if (currentRoute == ROUTE_LIVE) "live" else "party" },
                        containerColor = PartyGold,
                        contentColor = DeepObsidianText,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = {
                            Text(
                                text = if (currentRoute == ROUTE_LIVE) stringResource(R.string.start_live) else stringResource(R.string.create_party),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("fab_go_live")
                    )
                }
            },
            bottomBar = {
                if (isTopLevel && !useNavRail) {
                    NavigationBar(containerColor = RoyalPlumSurface, tonalElevation = 0.dp) {
                        MainNavDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepObsidianText,
                                    selectedTextColor = PartyGold,
                                    indicatorColor = PartyGold,
                                    unselectedIconColor = MutedLavender,
                                    unselectedTextColor = MutedLavender
                                ),
                                modifier = Modifier.testTag("nav_item_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isTopLevel && useNavRail) {
                    NavigationRail(containerColor = RoyalPlumSurface) {
                        MainNavDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DeepObsidianText,
                                    selectedTextColor = PartyGold,
                                    indicatorColor = PartyGold,
                                    unselectedIconColor = MutedLavender,
                                    unselectedTextColor = MutedLavender
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_LIVE,
                        enterTransition = {
                            fadeIn(animationSpec = tween(260)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(260)) +
                                slideInHorizontally(initialOffsetX = { it / 14 }, animationSpec = tween(260))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(200)) +
                                slideOutHorizontally(targetOffsetX = { -it / 16 }, animationSpec = tween(200))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(240)) +
                                scaleIn(initialScale = 0.97f, animationSpec = tween(240))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(200)) +
                                slideOutHorizontally(targetOffsetX = { it / 12 }, animationSpec = tween(200))
                        }
                    ) {
                        composable(ROUTE_LIVE) {
                            LiveDiscoverScreen(
                                liveRoomsState = liveRoomsState,
                                bookmarksState = bookmarksState,
                                followingState = followingState,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                followedOnly = followedOnly,
                                onSelectCategory = lobbyViewModel::selectCategory,
                                onSearchQueryChange = lobbyViewModel::updateSearchQuery,
                                onToggleFollowedOnly = lobbyViewModel::toggleFollowedOnly,
                                onRoomClick = { roomId -> navController.navigate("room/$roomId") },
                                onToggleBookmark = lobbyViewModel::toggleBookmark,
                                onStartLiveClick = { createDialogMode = "live" }
                            )
                        }
                        composable(ROUTE_PARTY) {
                            PartyHubScreen(
                                liveRoomsState = liveRoomsState,
                                bookmarksState = bookmarksState,
                                onRoomClick = { roomId -> navController.navigate("room/$roomId") },
                                onToggleBookmark = lobbyViewModel::toggleBookmark,
                                onCreatePartyClick = { createDialogMode = "party" }
                            )
                        }
                        composable(ROUTE_MOMENTS) {
                            MomentsScreen(
                                momentsState = momentsState,
                                followingState = followingState,
                                currentUserId = currentUserId,
                                onCreateMoment = lobbyViewModel::createMoment,
                                onLikeMoment = lobbyViewModel::likeMoment,
                                onToggleFollow = lobbyViewModel::toggleFollow,
                                observeComments = lobbyViewModel::observeMomentComments,
                                onAddComment = lobbyViewModel::addMomentComment
                            )
                        }
                        composable(ROUTE_MESSAGES) {
                            MessagesScreen(
                                conversationsState = conversationsState,
                                currentUserId = currentUserId,
                                onConversationClick = { conv -> navController.navigate("chat/${conv.id}") },
                                onStartConversation = { peer, msg ->
                                    lobbyViewModel.startConversation(peer, msg) { convId ->
                                        navController.navigate("chat/$convId")
                                    }
                                }
                            )
                        }
                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                profileState = userProfileState,
                                hostedRoomsState = hostedRoomsState,
                                unreadNotificationsCount = unreadNotifCount,
                                isGoogleSignedIn = isGoogleSignedIn,
                                onUpdateCustomAvatarUri = lobbyViewModel::updateCustomAvatarUri,
                                onOpenVip = { navController.navigate(ROUTE_VIP) },
                                onOpenTasks = { navController.navigate(ROUTE_TASKS) },
                                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onOpenWallet = { navController.navigate(ROUTE_WALLET) },
                                onOpenLeaderboard = { navController.navigate(ROUTE_LEADERBOARD) },
                                onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                onOpenAdmin = { navController.navigate(ROUTE_ADMIN) },
                                onRoomClick = { roomId -> navController.navigate("room/$roomId") },
                                onSignInWithGoogle = triggerGoogleSync,
                                onSignOut = { signOutUser(credentialManager, scope) }
                            )
                        }
                        composable(ROUTE_VIP) {
                            VipStoreScreen(
                                profile = userProfile,
                                onPurchaseVip = { tier -> lobbyViewModel.purchaseVip(tier) {} },
                                onClaimDailyCashback = lobbyViewModel::claimVipDailyCashback,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_TASKS) {
                            NewcomerTaskScreen(
                                profile = userProfile,
                                onClaimDay = lobbyViewModel::claimNewcomerDay,
                                onClaimDailyTask = lobbyViewModel::claimDailyTask,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                profile = userProfile,
                                isGoogleSignedIn = isGoogleSignedIn,
                                onSaveSettings = lobbyViewModel::updateSettings,
                                onExchangeDiamonds = lobbyViewModel::exchangeDiamonds,
                                onRequestWithdrawal = lobbyViewModel::requestWithdrawal,
                                onSignInWithGoogle = triggerGoogleSync,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = ROUTE_ROOM,
                            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
                        ) { entry ->
                            val roomId = entry.arguments?.getString("roomId") ?: return@composable
                            val roomViewModel: LiveRoomViewModel = viewModel(
                                key = "anu_room_${currentUserId}_$roomId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                        LiveRoomViewModel(PartyRepository(db), roomId, currentUserId)
                                    }
                                }
                            )
                            val activeRoom = (roomViewModel.roomState.collectAsStateWithLifecycle().value as? UiState.Success)?.data
                            LiveRoomScreen(
                                viewModel = roomViewModel,
                                currentUserDisplayName = displayName,
                                currentUserCoins = coinBalance,
                                currentUserXp = currentXp,
                                isFollowingHost = activeRoom?.hostId in followedIds,
                                onToggleFollowHost = { hostId, hostName ->
                                    lobbyViewModel.toggleFollow(hostId, hostName, hostId in followedIds)
                                },
                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = ROUTE_CHAT,
                            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                        ) { entry ->
                            val convId = entry.arguments?.getString("conversationId") ?: return@composable
                            val conv = conversationsList.firstOrNull { it.id == convId }
                            val chatVm: DirectChatViewModel = viewModel(
                                key = "anu_chat_${currentUserId}_$convId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                        DirectChatViewModel(PartyRepository(db), convId, currentUserId)
                                    }
                                }
                            )
                            DirectChatScreen(
                                viewModel = chatVm,
                                peerName = conv?.participantNames?.lastOrNull() ?: "Chat",
                                participantIds = conv?.participantIds ?: listOf(currentUserId),
                                currentUserName = displayName,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_WALLET) {
                            WalletScreen(
                                coinBalance = coinBalance,
                                transactionsState = transactionsState,
                                onMockRecharge = lobbyViewModel::mockRecharge,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_LEADERBOARD) {
                            LeaderboardScreen(
                                liveRoomsState = liveRoomsState,
                                onRoomClick = { roomId -> navController.navigate("room/$roomId") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_NOTIFICATIONS) {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                onMarkRead = lobbyViewModel::markNotificationRead,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_ADMIN) {
                            AdminDashboardScreen(
                                liveRoomsState = liveRoomsState,
                                reportsState = reportsState,
                                userRole = userProfile.role,
                                onEndRoom = lobbyViewModel::endRoom,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    val dialogMode = createDialogMode
    if (dialogMode != null) {
        CreateRoomDialog(
            initialMode = dialogMode,
            onDismiss = { createDialogMode = null },
            onCreate = { title, topic, category, mode, seats ->
                createDialogMode = null
                lobbyViewModel.createRoom(title, topic, category, mode, seats) { createdRoomId ->
                    navController.navigate("room/$createdRoomId")
                }
            }
        )
    }
}
