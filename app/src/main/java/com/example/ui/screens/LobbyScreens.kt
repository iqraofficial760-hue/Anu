package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FollowedHost
import com.example.data.LevelCalculator
import com.example.data.PartyCategory
import com.example.data.PartyRoom
import com.example.data.RoomBookmark
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.CategoryTabStrip
import com.example.ui.components.FeaturedHeroCard
import com.example.ui.components.PartyRoomCard
import com.example.ui.theme.CardPlumBottom
import com.example.ui.theme.CardPlumTop
import com.example.ui.theme.DeepObsidianText
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.LiveCoral
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MidnightPurple
import com.example.ui.theme.MutedLavender
import com.example.ui.theme.PartyGold
import com.example.ui.theme.PartyGoldDim
import com.example.ui.theme.RoyalPlumSurface
import com.example.ui.theme.SoftLavender

@Composable
fun LiveDiscoverScreen(
    liveRoomsState: UiState<List<PartyRoom>>,
    bookmarksState: UiState<List<RoomBookmark>>,
    followingState: UiState<List<FollowedHost>>,
    selectedCategory: PartyCategory,
    searchQuery: String,
    followedOnly: Boolean,
    onSelectCategory: (PartyCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleFollowedOnly: () -> Unit,
    onRoomClick: (String) -> Unit,
    onToggleBookmark: (PartyRoom, Boolean) -> Unit,
    onStartLiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarkedIds = remember(bookmarksState) {
        ((bookmarksState as? UiState.Success)?.data ?: emptyList()).map { it.roomId }.toSet()
    }
    val followedIds = remember(followingState) {
        ((followingState as? UiState.Success)?.data ?: emptyList()).map { it.targetUserId }.toSet()
    }
    val allLive = (liveRoomsState as? UiState.Success)?.data ?: emptyList()
    val filteredRooms = remember(allLive, selectedCategory, searchQuery, followedOnly, followedIds) {
        val q = searchQuery.trim().lowercase()
        allLive.filter { room ->
            val matchCategory = selectedCategory == PartyCategory.ALL || room.category == selectedCategory.key
            val matchQuery = q.isEmpty() ||
                room.title.lowercase().contains(q) ||
                room.hostName.lowercase().contains(q) ||
                room.shortCode.lowercase().contains(q)
            val matchFollowed = !followedOnly || room.hostId in followedIds
            matchCategory && matchQuery && matchFollowed
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightPurple)
    ) {
        CategoryTabStrip(selected = selectedCategory, onSelect = onSelectCategory)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search live streams or ID...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PartyGold) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = SoftLavender)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PartyGold,
                            unfocusedBorderColor = PartyGoldDim,
                            focusedContainerColor = RoyalPlumSurface,
                            unfocusedContainerColor = RoyalPlumSurface
                        ),
                        modifier = Modifier.weight(1f).testTag("live_search_input")
                    )
                    FilterChip(
                        selected = followedOnly,
                        onClick = onToggleFollowedOnly,
                        label = { Text("Following") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LiveCoral,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = RoyalPlumSurface,
                            labelColor = SoftLavender
                        )
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedHeroCard(
                    title = "Go Live on ANU LIVE",
                    subtitle = "Broadcast video with beauty filters, live comments & animated gifts.",
                    liveCount = allLive.size,
                    ctaLabel = stringResource(R.string.start_live),
                    onCtaClick = onStartLiveClick
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (followedOnly) "Followed Hosts Live" else "Featured Live Streams",
                        style = MaterialTheme.typography.titleMedium,
                        color = PartyGold
                    )
                    Text(
                        text = "${filteredRooms.size} live",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftLavender
                    )
                }
            }

            when (liveRoomsState) {
                is UiState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PartyGold)
                        }
                    }
                }
                is UiState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyStateCard(
                            title = "Could not load live streams",
                            subtitle = liveRoomsState.message,
                            actionLabel = stringResource(R.string.start_live),
                            onAction = onStartLiveClick
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredRooms.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyStateCard(
                                title = "No live streams here yet",
                                subtitle = "Tap Start Live to launch your own broadcast!",
                                actionLabel = stringResource(R.string.start_live),
                                onAction = onStartLiveClick
                            )
                        }
                    } else {
                        items(filteredRooms, key = { it.id }) { room ->
                            val isSaved = room.id in bookmarkedIds
                            PartyRoomCard(
                                room = room,
                                isBookmarked = isSaved,
                                onRoomClick = { onRoomClick(room.id) },
                                onBookmarkClick = { onToggleBookmark(room, isSaved) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartyHubScreen(
    liveRoomsState: UiState<List<PartyRoom>>,
    bookmarksState: UiState<List<RoomBookmark>>,
    onRoomClick: (String) -> Unit,
    onToggleBookmark: (PartyRoom, Boolean) -> Unit,
    onCreatePartyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var seatFilter by remember { mutableIntStateOf(0) } // 0 = All, 4, 6, 8
    val bookmarkedIds = remember(bookmarksState) {
        ((bookmarksState as? UiState.Success)?.data ?: emptyList()).map { it.roomId }.toSet()
    }
    val partyRooms = remember(liveRoomsState, seatFilter) {
        val all = ((liveRoomsState as? UiState.Success)?.data ?: emptyList())
            .filter { it.roomMode == "party" || it.seatCapacity in listOf(4, 6, 8) }
        if (seatFilter == 0) all else all.filter { it.seatCapacity == seatFilter }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize().background(MidnightPurple)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FeaturedHeroCard(
                title = "Multi-Seat Voice & Video Party",
                subtitle = "Host 4, 6, or 8-seat lounges with camera seats & moderator controls.",
                liveCount = partyRooms.size,
                ctaLabel = stringResource(R.string.create_party),
                onCtaClick = onCreatePartyClick
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0 to "All Seats", 4 to "4 Seats", 6 to "6 Seats", 8 to "8 Seats").forEach { (cap, label) ->
                    FilterChip(
                        selected = seatFilter == cap,
                        onClick = { seatFilter = cap },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PartyGold,
                            selectedLabelColor = DeepObsidianText,
                            containerColor = RoyalPlumSurface,
                            labelColor = SoftLavender
                        )
                    )
                }
            }
        }

        if (partyRooms.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyStateCard(
                    title = "No party rooms in this seat layout",
                    subtitle = "Open a 4, 6, or 8-seat party lounge and invite friends!",
                    actionLabel = stringResource(R.string.create_party),
                    onAction = onCreatePartyClick
                )
            }
        } else {
            items(partyRooms, key = { it.id }) { room ->
                val isSaved = room.id in bookmarkedIds
                PartyRoomCard(
                    room = room,
                    isBookmarked = isSaved,
                    onRoomClick = { onRoomClick(room.id) },
                    onBookmarkClick = { onToggleBookmark(room, isSaved) }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    hostedRoomsState: UiState<List<PartyRoom>>,
    unreadNotificationsCount: Int,
    isGoogleSignedIn: Boolean,
    onUpdateCustomAvatarUri: (String) -> Unit,
    onOpenVip: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdmin: () -> Unit,
    onRoomClick: (String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val hostedRooms = (hostedRoomsState as? UiState.Success)?.data ?: emptyList()
    var showLionPreview by remember { mutableStateOf(false) }

    var showInstallDialog by remember { mutableStateOf(false) }

    val quickAvatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUpdateCustomAvatarUri(uri.toString())
    }

    val xp = profile.xp
    val level = com.example.data.LevelCalculator.levelForXp(xp)
    val levelProgress = com.example.data.LevelCalculator.progressInCurrentLevel(xp)
    val xpRemaining = com.example.data.LevelCalculator.xpToNextLevel(xp)
    val activeVip = com.example.data.VipCatalog.byId(profile.vipTierId)

    Box(modifier = modifier.fillMaxSize().background(MidnightPurple)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Luxury Gold + Black + Green Me Header Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    com.example.ui.components.LuxuryObsidianBlack,
                                    com.example.ui.components.LuxuryDarkEmerald.copy(alpha = 0.55f),
                                    com.example.ui.components.LuxuryObsidianBlack
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    com.example.ui.components.LuxuryRoyalGold,
                                    com.example.ui.components.LuxuryEmeraldGreen,
                                    com.example.ui.components.LuxuryRoyalGold
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.clickable {
                            quickAvatarPicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        com.example.ui.components.LuxuryLionAvatar(
                            displayName = profile.displayName,
                            avatarId = profile.avatarId,
                            customAvatarUri = profile.customAvatarUri,
                            vipBadge = activeVip?.name ?: "ROYAL",
                            size = 88.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tap avatar to set Photo or Animated GIF",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.example.ui.components.LuxuryEmeraldGreen
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Animated Luxury Gold + Black + Green Badges (ID: 1, LV.X, VIP/SVIP)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.LuxuryBadgePill(
                            text = "ID: ${profile.numericId}",
                            emeraldAccent = true
                        )
                        com.example.ui.components.LuxuryBadgePill(
                            text = "LV.$level",
                            icon = Icons.Filled.Star
                        )
                        if (activeVip != null) {
                            com.example.ui.components.LuxuryBadgePill(
                                text = activeVip.name,
                                emeraldAccent = activeVip.isSvip
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = profile.bio.ifBlank { "Live streaming & VIP partying on ANU LIVE!" },
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftLavender,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Level & XP Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "User Lv.$level • Host Lv.${profile.hostLevel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.example.ui.components.LuxuryRoyalGold
                            )
                            Text(
                                text = "$xp XP ($xpRemaining to Lv.${level + 1})",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.example.ui.components.LuxuryEmeraldGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            color = com.example.ui.components.LuxuryRoyalGold,
                            trackColor = com.example.ui.components.LuxuryObsidianBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            border = BorderStroke(1.dp, com.example.ui.components.LuxuryRoyalGold),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = com.example.ui.components.LuxuryRoyalGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settings", color = com.example.ui.components.LuxuryRoyalGold)
                        }
                        Button(
                            onClick = { showLionPreview = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.components.LuxuryEmeraldGreen,
                                contentColor = com.example.ui.components.LuxuryObsidianBlack
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Lion Effect", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Dual Luxury Coins & Diamonds Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(com.example.ui.components.LuxuryObsidianBlack, CardPlumTop)))
                            .border(1.dp, com.example.ui.components.LuxuryRoyalGold, RoundedCornerShape(18.dp))
                            .clickable { onOpenWallet() }
                            .padding(14.dp)
                            .testTag("me_coins_card")
                    ) {
                        Text("COINS", style = MaterialTheme.typography.labelSmall, color = com.example.ui.components.LuxuryRoyalGold, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = com.example.data.formatCompactCoins(profile.coinBalance),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("Tap to Recharge", style = MaterialTheme.typography.labelSmall, color = SoftLavender)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(com.example.ui.components.LuxuryObsidianBlack, com.example.ui.components.LuxuryDarkEmerald.copy(alpha = 0.6f))))
                            .border(1.dp, com.example.ui.components.LuxuryEmeraldGreen, RoundedCornerShape(18.dp))
                            .clickable { onOpenSettings() }
                            .padding(14.dp)
                            .testTag("me_diamonds_card")
                    ) {
                        Text("DIAMONDS", style = MaterialTheme.typography.labelSmall, color = com.example.ui.components.LuxuryEmeraldGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${profile.diamondBalance}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text("Exchange / Withdraw", style = MaterialTheme.typography.labelSmall, color = SoftLavender)
                    }
                }
            }

            // Social Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileStatCard("Followers", "${profile.followersCount}", LiveEmerald, Modifier.weight(1f))
                    ProfileStatCard("Following", "${profile.followingCount}", Color.White, Modifier.weight(1f))
                    ProfileStatCard("Gifts Sent", "${profile.giftsSent}", LiveCoral, Modifier.weight(1f))
                }
            }

            // Primary Me Feature Menu Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMenuRow(
                        icon = Icons.Filled.Star,
                        title = "VIP & SVIP Royal Store",
                        subtitle = if (activeVip != null) "${activeVip.name} Active • ${profile.vipDaysRemaining}d daily cashback" else "VIP 1–4 (500K–5M) • SVIP 1–3 (10M–30M) + 7d Cashback",
                        accent = com.example.ui.components.LuxuryRoyalGold,
                        onClick = onOpenVip,
                        testTag = "profile_open_vip"
                    )
                    ProfileMenuRow(
                        icon = Icons.Filled.Favorite,
                        title = "Task Center (7-Day 1M + Daily Tasks)",
                        subtitle = "7-Day Newcomer ${profile.claimedNewcomerDays.size}/7 • Daily ${profile.claimedDailyTaskIds.size}/4 • 1M FREE Coins",
                        accent = com.example.ui.components.LuxuryEmeraldGreen,
                        onClick = onOpenTasks,
                        testTag = "profile_open_tasks"
                    )
                    ProfileMenuRow(
                        icon = Icons.Filled.Edit,
                        title = "Settings, Avatar & Withdrawal",
                        subtitle = "Edit name, bio, Gold-Black-Green avatar, Diamond exchange & payout",
                        accent = PartyGold,
                        onClick = onOpenSettings,
                        testTag = "profile_open_settings"
                    )
                    ProfileMenuRow(
                        icon = Icons.Filled.Star,
                        title = "Install & Update ANU LIVE (${com.example.data.AppInstallerManager.getAppVersionLabel()})",
                        subtitle = "Download latest APK update, share installer & check app info",
                        accent = com.example.ui.components.LuxuryEmeraldGreen,
                        onClick = { showInstallDialog = true },
                        testTag = "profile_open_installer"
                    )
                    ProfileMenuRow(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "Coin Wallet & Recharge",
                        subtitle = "${com.example.data.formatCompactCoins(profile.coinBalance)} coins • History & Recharge",
                        accent = PartyGold,
                        onClick = onOpenWallet,
                        testTag = "profile_open_wallet"
                    )
                    ProfileMenuRow(
                        icon = Icons.Filled.EmojiEvents,
                        title = "Leaderboard",
                        subtitle = "Daily, Weekly & Monthly Top Hosts & Gifters",
                        accent = LiveEmerald,
                        onClick = onOpenLeaderboard,
                        testTag = "profile_open_leaderboard"
                    )
                    ProfileMenuRow(
                        icon = Icons.Filled.Notifications,
                        title = "Notifications",
                        subtitle = if (unreadNotificationsCount > 0) "$unreadNotificationsCount unread alerts" else "All caught up",
                        accent = LiveCoral,
                        onClick = onOpenNotifications,
                        testTag = "profile_open_notifications"
                    )
                    ProfileMenuRow(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "Admin & Moderation Console",
                        subtitle = "Rooms, Reports, Gifts & Platform Analytics",
                        accent = SoftLavender,
                        onClick = onOpenAdmin,
                        testTag = "profile_open_admin"
                    )
                }
            }

            // Live History
            item {
                Text(
                    text = "Your Live & Party History (${hostedRooms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = PartyGold
                )
            }

            if (hostedRooms.isEmpty()) {
                item {
                    Surface(
                        color = RoyalPlumSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, PartyGoldDim, RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = "No broadcasts yet. Start a Live Stream or Party Room to see your history here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftLavender,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(hostedRooms.take(5), key = { it.id }) { room ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoyalPlumSurface)
                            .border(1.dp, PartyGoldDim, RoundedCornerShape(14.dp))
                            .clickable(enabled = room.status == "live") { onRoomClick(room.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (room.roomMode == "live") Icons.Filled.Videocam else Icons.Filled.Groups,
                            contentDescription = null,
                            tint = PartyGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(room.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${room.listenerCount} viewers • ${com.example.data.formatCompactCoins(room.giftScore)} gift coins", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                        Text(
                            text = room.status.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (room.status == "live") LiveEmerald else MutedLavender,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Optional Google Cloud Sync / Sign Out
            item {
                if (isGoogleSignedIn) {
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = ElevatedPlum, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, LiveCoral.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = LiveCoral, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_out), color = Color.White)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSignInWithGoogle,
                        border = BorderStroke(1.dp, PartyGoldDim),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("optional_google_sync_button")
                    ) {
                        Text("Optional: Sync Account with Google Cloud", color = PartyGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        com.example.ui.components.LuxuryLionShowcaseOverlay(
            visible = showLionPreview,
            title = "Gold • Black • Emerald Royal Lion",
            subtitle = "${profile.displayName} • ANU LIVE Luxury Aura",
            onDismiss = { showLionPreview = false }
        )

        if (showInstallDialog) {
            com.example.ui.components.InstallUpdateDialog(
                onDismiss = { showInstallDialog = false }
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RoyalPlumSurface)
            .border(1.dp, PartyGoldDim, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SoftLavender, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedLavender)
    }
}

@Composable
private fun ProfileStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RoyalPlumSurface)
            .border(1.dp, PartyGoldDim, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SoftLavender, maxLines = 1)
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RoyalPlumSurface)
            .border(1.dp, PartyGoldDim, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = PartyGold, modifier = Modifier.size(34.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SoftLavender, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(actionLabel, fontWeight = FontWeight.Bold)
        }
    }
}
