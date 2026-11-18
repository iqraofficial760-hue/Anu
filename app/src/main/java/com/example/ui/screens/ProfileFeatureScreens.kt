package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppNotification
import com.example.data.GiftCatalog
import com.example.data.ModerationReport
import com.example.data.PartyRoom
import com.example.data.RechargePack
import com.example.data.UiState
import com.example.data.WalletTransaction
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
private fun SubScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoyalPlumSurface)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = PartyGold)
    }
}

@Composable
fun WalletScreen(
    coinBalance: Long,
    transactionsState: UiState<List<WalletTransaction>>,
    onMockRecharge: (RechargePack) -> Unit,
    onBack: () -> Unit
) {
    val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().background(MidnightPurple).navigationBarsPadding()) {
        SubScreenTopBar("Coin Wallet", onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(CardPlumTop, CardPlumBottom)))
                        .border(1.dp, PartyGold, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Text("Available Balance", style = MaterialTheme.typography.labelMedium, color = SoftLavender)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = PartyGold, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$coinBalance", style = MaterialTheme.typography.displaySmall, color = PartyGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coins", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = LiveEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DEV MOCK PAYMENT MODE — TODO: Connect Google Play Billing / Stripe",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiveEmerald
                        )
                    }
                }
            }

            item {
                Text("Recharge Coins (Mock Checkout)", style = MaterialTheme.typography.titleMedium, color = PartyGold)
            }

            items(GiftCatalog.rechargePacks, key = { it.id }) { pack ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoyalPlumSurface)
                        .border(1.dp, PartyGoldDim, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${pack.coins} + ${pack.bonusCoins} Bonus Coins", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(pack.label, style = MaterialTheme.typography.bodySmall, color = PartyGold)
                    }
                    Button(
                        onClick = { onMockRecharge(pack) },
                        colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("recharge_${pack.id}")
                    ) {
                        Text(pack.priceDisplay, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text("Transaction & Gift History", style = MaterialTheme.typography.titleMedium, color = PartyGold)
            }

            if (transactions.isEmpty()) {
                item {
                    Surface(color = RoyalPlumSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("No transactions yet.", style = MaterialTheme.typography.bodySmall, color = SoftLavender, modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val isPositive = tx.amount >= 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoyalPlumSurface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text(tx.txType.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedLavender)
                        }
                        Text(
                            text = if (isPositive) "+${tx.amount}" else "${tx.amount}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isPositive) LiveEmerald else LiveCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    liveRoomsState: UiState<List<PartyRoom>>,
    onRoomClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var period by remember { mutableStateOf("Daily") }
    var boardType by remember { mutableStateOf("Top Hosts") }
    val rooms = remember(liveRoomsState, boardType) {
        val list = (liveRoomsState as? UiState.Success)?.data ?: emptyList()
        if (boardType == "Top Hosts") {
            list.sortedByDescending { it.giftScore + it.listenerCount * 10L }
        } else {
            list.sortedByDescending { it.giftScore }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MidnightPurple).navigationBarsPadding()) {
        SubScreenTopBar("ANU Leaderboard", onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Daily", "Weekly", "Monthly").forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p) },
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

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Top Hosts", "Top Gifters").forEach { t ->
                        FilterChip(
                            selected = boardType == t,
                            onClick = { boardType = t },
                            label = { Text(t) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LiveCoral,
                                selectedLabelColor = Color.White,
                                containerColor = RoyalPlumSurface,
                                labelColor = SoftLavender
                            )
                        )
                    }
                }
            }

            if (rooms.isEmpty()) {
                item {
                    Surface(color = RoyalPlumSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("No rankings yet for $period $boardType. Go live and send gifts to climb the chart!", style = MaterialTheme.typography.bodyMedium, color = SoftLavender, modifier = Modifier.padding(20.dp))
                    }
                }
            } else {
                itemsIndexed(rooms, key = { _, r -> r.id }) { idx, room ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(CardPlumTop, RoyalPlumSurface)))
                            .border(1.dp, PartyGoldDim, RoundedCornerShape(16.dp))
                            .clickable { onRoomClick(room.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (idx < 3) PartyGold else ElevatedPlum),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.labelLarge, color = if (idx < 3) DeepObsidianText else Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(room.hostName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(room.title, style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = PartyGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${room.giftScore} coins", style = MaterialTheme.typography.labelMedium, color = PartyGold, fontWeight = FontWeight.Bold)
                            }
                            Text("${room.listenerCount} viewers", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<AppNotification>>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    val items = (notificationsState as? UiState.Success)?.data ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().background(MidnightPurple).navigationBarsPadding()) {
        SubScreenTopBar("Notifications", onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    Surface(color = RoyalPlumSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("No notifications yet.", style = MaterialTheme.typography.bodyMedium, color = SoftLavender, modifier = Modifier.padding(20.dp))
                    }
                }
            } else {
                items(items, key = { it.id }) { notif ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (notif.read) RoyalPlumSurface else ElevatedPlum)
                            .border(1.dp, if (notif.read) PartyGoldDim else PartyGold, RoundedCornerShape(14.dp))
                            .clickable { if (!notif.read) onMarkRead(notif.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (notif.read) MutedLavender else PartyGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(notif.body, style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                        if (!notif.read) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(LiveCoral))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    liveRoomsState: UiState<List<PartyRoom>>,
    reportsState: UiState<List<ModerationReport>>,
    userRole: String,
    onEndRoom: (String) -> Unit,
    onBack: () -> Unit
) {
    val rooms = (liveRoomsState as? UiState.Success)?.data ?: emptyList()
    val reports = (reportsState as? UiState.Success)?.data ?: emptyList()
    val totalGiftCoins = rooms.sumOf { it.giftScore }
    val totalViewers = rooms.sumOf { it.listenerCount }

    Column(modifier = Modifier.fillMaxSize().background(MidnightPurple).navigationBarsPadding()) {
        SubScreenTopBar("Admin & Moderation", onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = ElevatedPlum,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, PartyGold, RoundedCornerShape(14.dp))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = PartyGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Role: ${userRole.uppercase()} • Protected by Firestore RBAC", style = MaterialTheme.typography.labelLarge, color = PartyGold)
                            Text("Admin routes enforce trusted /admins/{uid} & verified email checks in firestore.rules.", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                    }
                }
            }

            // Basic Analytics
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMetricCard("Active Rooms", "${rooms.size}", LiveEmerald, Modifier.weight(1f))
                    AdminMetricCard("Live Viewers", "$totalViewers", PartyGold, Modifier.weight(1f))
                    AdminMetricCard("Gift Coins", "$totalGiftCoins", LiveCoral, Modifier.weight(1f))
                }
            }

            // Live & Party Room Management
            item {
                Text("Live & Party Room Moderation", style = MaterialTheme.typography.titleMedium, color = PartyGold)
            }
            if (rooms.isEmpty()) {
                item {
                    Text("No active rooms to moderate.", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                }
            } else {
                items(rooms, key = { it.id }) { room ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoyalPlumSurface)
                            .border(1.dp, PartyGoldDim, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${room.title} (${room.shortCode})", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("Host: ${room.hostName} • ${room.roomMode.uppercase()}", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                        TextButton(onClick = { onEndRoom(room.id) }) {
                            Text("End Room", color = LiveCoral)
                        }
                    }
                }
            }

            // Gift Catalog Management Overview
            item {
                Text("Configured Virtual Gifts (${GiftCatalog.gifts.size})", style = MaterialTheme.typography.titleMedium, color = PartyGold)
            }
            items(GiftCatalog.gifts, key = { it.id }) { gift ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RoyalPlumSurface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = PartyGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(gift.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Text("${gift.coins} coins • +${gift.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = PartyGold)
                }
            }

            // Reports Queue
            item {
                Text("Moderation Reports (${reports.size})", style = MaterialTheme.typography.titleMedium, color = PartyGold)
            }
            if (reports.isEmpty()) {
                item {
                    Text("No open reports.", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                }
            } else {
                items(reports, key = { it.id }) { rep ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RoyalPlumSurface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Flag, contentDescription = null, tint = LiveCoral, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${rep.targetType.uppercase()}: ${rep.targetId}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            Text(rep.reason, style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                        Text(rep.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = PartyGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RoyalPlumSurface)
            .border(1.dp, PartyGoldDim, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SoftLavender)
    }
}
