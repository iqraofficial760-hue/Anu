package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.example.data.AvatarCatalog
import com.example.data.LocalGuestStore
import com.example.data.NewcomerTaskCatalog
import com.example.data.NewcomerTaskDay
import com.example.data.UserProfile
import com.example.data.VipCatalog
import com.example.data.VipTier
import com.example.data.formatCompactCoins
import com.example.ui.components.LuxuryDarkEmerald
import com.example.ui.components.LuxuryEmeraldGreen
import com.example.ui.components.LuxuryLionAvatar
import com.example.ui.components.LuxuryLionShowcaseOverlay
import com.example.ui.components.LuxuryObsidianBlack
import com.example.ui.components.LuxuryRoyalGold
import com.example.ui.theme.CardPlumBottom
import com.example.ui.theme.CardPlumTop
import com.example.ui.theme.DeepObsidianText
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MidnightPurple
import com.example.ui.theme.MutedLavender
import com.example.ui.theme.PartyGold
import com.example.ui.theme.PartyGoldDim
import com.example.ui.theme.RoyalPlumSurface
import com.example.ui.theme.SoftLavender

@Composable
private fun MeSubTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LuxuryObsidianBlack)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LuxuryRoyalGold)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = LuxuryRoyalGold, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VipStoreScreen(
    profile: UserProfile,
    onPurchaseVip: (VipTier) -> Unit,
    onClaimDailyCashback: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("VIP") } // "VIP" or "SVIP"
    var previewLion by remember { mutableStateOf<Pair<String, String>?>(null) }

    val activeTier = VipCatalog.byId(profile.vipTierId)
    val claimedToday = profile.lastVipClaimDayKey == LocalGuestStore.todayKey()
    val filteredTiers = remember(selectedTab) {
        VipCatalog.tiers.filter { if (selectedTab == "SVIP") it.isSvip else !it.isSvip }
    }

    Box(modifier = Modifier.fillMaxSize().background(MidnightPurple)) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            MeSubTopBar("VIP & SVIP Royal Club", onBack)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Balance & Active VIP Status Banner
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(LuxuryObsidianBlack, LuxuryDarkEmerald.copy(alpha = 0.65f), LuxuryObsidianBlack)))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(LuxuryRoyalGold, LuxuryEmeraldGreen, LuxuryRoyalGold)),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (activeTier != null) "ACTIVE: ${activeTier.name}" else "UNLOCK ROYAL STATUS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LuxuryEmeraldGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatCompactCoins(profile.coinBalance)} Coins Available",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = LuxuryRoyalGold,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeTier != null) {
                                    Text(
                                        text = "${formatCompactCoins(activeTier.dailyCashbackCoins)}/day cashback • ${profile.vipDaysRemaining} days left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                            LuxuryLionAvatar(
                                displayName = profile.displayName,
                                avatarId = "royal_lion",
                                vipBadge = activeTier?.name ?: "LION",
                                size = 62.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (activeTier != null && profile.vipDaysRemaining > 0) {
                                Button(
                                    onClick = onClaimDailyCashback,
                                    enabled = !claimedToday,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LuxuryEmeraldGreen,
                                        contentColor = LuxuryObsidianBlack
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("claim_vip_cashback_btn")
                                ) {
                                    Text(
                                        text = if (claimedToday) "Claimed Today" else "Claim +${formatCompactCoins(activeTier.dailyCashbackCoins)}",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    previewLion = "Gold • Black • Emerald Royal Lion" to "Luxury VIP/SVIP Entrance & Avatar Aura Preview"
                                },
                                border = BorderStroke(1.dp, LuxuryRoyalGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = LuxuryRoyalGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lion Animation", color = LuxuryRoyalGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // VIP / SVIP Tab Switcher
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("VIP" to "VIP 1 – 4 (500K–5M)", "SVIP" to "SVIP 1 – 3 (10M–30M)").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedTab == key,
                                onClick = { selectedTab = key },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LuxuryRoyalGold,
                                    selectedLabelColor = LuxuryObsidianBlack,
                                    containerColor = RoyalPlumSurface,
                                    labelColor = SoftLavender
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // VIP / SVIP Tier Cards
                items(filteredTiers, key = { it.id }) { tier ->
                    val isCurrent = profile.vipTierId == tier.id && profile.vipDaysRemaining > 0
                    val canAfford = profile.coinBalance >= tier.priceCoins
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    if (tier.isSvip) listOf(LuxuryObsidianBlack, Color(0xFF102A1E), LuxuryObsidianBlack)
                                    else listOf(CardPlumTop, CardPlumBottom)
                                )
                            )
                            .border(
                                width = if (tier.isSvip) 1.5.dp else 1.dp,
                                brush = Brush.horizontalGradient(listOf(LuxuryRoyalGold, LuxuryEmeraldGreen)),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (tier.isSvip) LuxuryEmeraldGreen else LuxuryRoyalGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(tier.name, style = MaterialTheme.typography.titleLarge, color = LuxuryRoyalGold, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        text = "Daily Return: +${formatCompactCoins(tier.dailyCashbackCoins)} x ${tier.durationDays} Days",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LuxuryEmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                color = LuxuryObsidianBlack,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, LuxuryRoyalGold, RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = "${formatCompactCoins(tier.priceCoins)} Coins",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LuxuryRoyalGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        tier.perks.forEach { perk ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LuxuryEmeraldGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(perk, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onPurchaseVip(tier)
                                if (canAfford) {
                                    previewLion = "${tier.name} Activated!" to "Daily +${formatCompactCoins(tier.dailyCashbackCoins)} coins for 7 days!"
                                }
                            },
                            enabled = !isCurrent,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) LuxuryRoyalGold else ElevatedPlum,
                                contentColor = if (canAfford) LuxuryObsidianBlack else SoftLavender
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("buy_${tier.id}")
                        ) {
                            Text(
                                text = when {
                                    isCurrent -> "Currently Active (${profile.vipDaysRemaining}d left)"
                                    canAfford -> "Buy ${tier.name} • ${formatCompactCoins(tier.priceCoins)} Coins"
                                    else -> "Need ${formatCompactCoins(tier.priceCoins)} Coins"
                                },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        LuxuryLionShowcaseOverlay(
            visible = previewLion != null,
            title = previewLion?.first ?: "",
            subtitle = previewLion?.second ?: "",
            onDismiss = { previewLion = null }
        )
    }
}

@Composable
fun NewcomerTaskScreen(
    profile: UserProfile,
    onClaimDay: (NewcomerTaskDay) -> Unit,
    onClaimDailyTask: (com.example.data.DailyTaskItem) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("newcomer") } // "newcomer" | "daily"
    val claimedSet = profile.claimedNewcomerDays.toSet()
    val claimedTotal = NewcomerTaskCatalog.days.filter { it.day in claimedSet }.sumOf { it.rewardCoins }
    val today = LocalGuestStore.todayKey()
    val claimedDailySet = if (profile.lastDailyTaskDayKey == today) profile.claimedDailyTaskIds.toSet() else emptySet()

    Column(modifier = Modifier.fillMaxSize().background(MidnightPurple).navigationBarsPadding()) {
        MeSubTopBar("Task Center • Free Coins", onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(LuxuryObsidianBlack, LuxuryDarkEmerald.copy(alpha = 0.7f))))
                        .border(1.5.dp, LuxuryRoyalGold, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = LuxuryRoyalGold, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("1,000,000 FREE Welcome Coins!", style = MaterialTheme.typography.titleMedium, color = LuxuryRoyalGold, fontWeight = FontWeight.ExtraBold)
                            Text("Instant signup bonus + 7-Day Newcomer & Daily Tasks", style = MaterialTheme.typography.bodySmall, color = LuxuryEmeraldGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Newcomer: ${formatCompactCoins(claimedTotal)} / 1M (${claimedSet.size}/7) • Daily: ${claimedDailySet.size}/${NewcomerTaskCatalog.dailyTasks.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedTab == "newcomer",
                        onClick = { selectedTab = "newcomer" },
                        label = { Text("7-Day Newcomer (1M)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LuxuryRoyalGold,
                            selectedLabelColor = LuxuryObsidianBlack,
                            containerColor = RoyalPlumSurface,
                            labelColor = SoftLavender
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == "daily",
                        onClick = { selectedTab = "daily" },
                        label = { Text("Daily Tasks (+100K)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LuxuryEmeraldGreen,
                            selectedLabelColor = LuxuryObsidianBlack,
                            containerColor = RoyalPlumSurface,
                            labelColor = SoftLavender
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (selectedTab == "newcomer") {
                items(NewcomerTaskCatalog.days, key = { it.day }) { task ->
                    val isClaimed = task.day in claimedSet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RoyalPlumSurface)
                            .border(1.dp, if (isClaimed) LiveEmerald else PartyGoldDim, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isClaimed) LuxuryDarkEmerald else LuxuryObsidianBlack,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, LuxuryRoyalGold, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("DAY ${task.day}", style = MaterialTheme.typography.labelSmall, color = LuxuryEmeraldGreen, fontWeight = FontWeight.Bold)
                                Text("+${formatCompactCoins(task.rewardCoins)}", style = MaterialTheme.typography.titleSmall, color = LuxuryRoyalGold, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(task.description, style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onClaimDay(task) },
                            enabled = !isClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isClaimed) ElevatedPlum else LuxuryRoyalGold,
                                contentColor = if (isClaimed) LiveEmerald else LuxuryObsidianBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("claim_task_day_${task.day}")
                        ) {
                            Text(if (isClaimed) "Done" else "Claim", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(NewcomerTaskCatalog.dailyTasks, key = { it.id }) { daily ->
                    val isDone = daily.id in claimedDailySet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RoyalPlumSurface)
                            .border(1.dp, if (isDone) LiveEmerald else LuxuryEmeraldGreen.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = LuxuryObsidianBlack,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, LuxuryEmeraldGreen, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("DAILY", style = MaterialTheme.typography.labelSmall, color = LuxuryEmeraldGreen, fontWeight = FontWeight.Bold)
                                Text("+${formatCompactCoins(daily.rewardCoins)}", style = MaterialTheme.typography.titleSmall, color = LuxuryRoyalGold, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(daily.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${daily.description} • +${daily.xpReward} XP", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onClaimDailyTask(daily) },
                            enabled = !isDone,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDone) ElevatedPlum else LuxuryEmeraldGreen,
                                contentColor = if (isDone) LiveEmerald else LuxuryObsidianBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("claim_daily_${daily.id}")
                        ) {
                            Text(if (isDone) "Done" else "Claim", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    profile: UserProfile,
    isGoogleSignedIn: Boolean,
    onSaveSettings: (displayName: String, bio: String, avatarId: String, customAvatarUri: String, withdrawalMethod: String, withdrawalAccount: String) -> Unit,
    onExchangeDiamonds: (Long) -> Unit,
    onRequestWithdrawal: (diamonds: Long, method: String, account: String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onBack: () -> Unit
) {
    var nameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var bioInput by remember(profile.bio) { mutableStateOf(profile.bio) }
    var selectedAvatarId by remember(profile.avatarId) { mutableStateOf(profile.avatarId) }
    var customUriInput by remember(profile.customAvatarUri) { mutableStateOf(profile.customAvatarUri) }
    var selectedMethod by remember(profile.withdrawalMethod) { mutableStateOf(profile.withdrawalMethod.ifBlank { "Telebirr" }) }
    var accountInput by remember(profile.withdrawalAccount) { mutableStateOf(profile.withdrawalAccount) }
    var withdrawDiamondsInput by remember { mutableStateOf("100") }

    // Zero-permission Android Photo Picker supporting Photos & Animated GIFs!
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customUriInput = uri.toString()
        }
    }

    val methods = listOf("Telebirr", "CBE Birr", "Bank Transfer", "PayPal")

    Column(modifier = Modifier.fillMaxSize().background(MidnightPurple).navigationBarsPadding()) {
        MeSubTopBar("Settings • Avatar/GIF • Exchange", onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Luxury Avatar & Photo/GIF Picker
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(RoyalPlumSurface)
                        .border(1.dp, PartyGoldDim, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Avatar & Photo / GIF", style = MaterialTheme.typography.titleMedium, color = LuxuryRoyalGold, fontWeight = FontWeight.Bold)
                        com.example.ui.components.LuxuryBadgePill(text = "ID: ${profile.numericId}", emeraldAccent = true)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LuxuryLionAvatar(
                        displayName = nameInput.ifBlank { "Anu" },
                        avatarId = selectedAvatarId,
                        customAvatarUri = customUriInput,
                        vipBadge = VipCatalog.byId(profile.vipTierId)?.name ?: "ROYAL",
                        size = 86.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryEmeraldGreen, contentColor = LuxuryObsidianBlack),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("pick_photo_gif_button")
                        ) {
                            Text("Choose Photo / GIF", fontWeight = FontWeight.ExtraBold)
                        }
                        if (customUriInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = { customUriInput = "" },
                                border = BorderStroke(1.dp, LuxuryRoyalGold),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Use Preset", color = LuxuryRoyalGold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AvatarCatalog.presets.forEach { preset ->
                            FilterChip(
                                selected = selectedAvatarId == preset.id && customUriInput.isBlank(),
                                onClick = {
                                    selectedAvatarId = preset.id
                                    customUriInput = ""
                                },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LuxuryRoyalGold,
                                    selectedLabelColor = LuxuryObsidianBlack,
                                    containerColor = ElevatedPlum,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 2. Profile Edit (Name & Bio)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(RoyalPlumSurface)
                        .border(1.dp, PartyGoldDim, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Edit Profile", style = MaterialTheme.typography.titleMedium, color = PartyGold, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(40) },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PartyGold, unfocusedBorderColor = PartyGoldDim),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it.take(160) },
                        label = { Text("Bio") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PartyGold, unfocusedBorderColor = PartyGoldDim),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSaveSettings(nameInput, bioInput, selectedAvatarId, customUriInput, selectedMethod, accountInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                    ) {
                        Text("Save Profile & Avatar", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. Diamond -> Coin Instant Exchange
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(LuxuryObsidianBlack, CardPlumTop)))
                        .border(1.dp, LuxuryEmeraldGreen, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CurrencyExchange, contentDescription = null, tint = LuxuryEmeraldGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exchange Diamonds -> Coins", style = MaterialTheme.typography.titleMedium, color = LuxuryEmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Diamond, contentDescription = null, tint = Color(0xFF40C4FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${profile.diamondBalance} Diamonds", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = LuxuryRoyalGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${formatCompactCoins(profile.coinBalance)} Coins", color = LuxuryRoyalGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Rate: 1 Diamond = 100 Coins (Instant swap)", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(50L, 100L, 250L).forEach { amt ->
                            Button(
                                onClick = { onExchangeDiamonds(amt) },
                                enabled = profile.diamondBalance >= amt,
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryEmeraldGreen, contentColor = LuxuryObsidianBlack),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${amt}D -> ${formatCompactCoins(amt * 100L)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // 4. Withdrawal Method & Payout Request
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(RoyalPlumSurface)
                        .border(1.dp, PartyGoldDim, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = PartyGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Withdrawal Method (Diamonds)", style = MaterialTheme.typography.titleMedium, color = PartyGold, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        methods.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PartyGold,
                                    selectedLabelColor = DeepObsidianText,
                                    containerColor = ElevatedPlum,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = accountInput,
                        onValueChange = { accountInput = it.take(60) },
                        label = { Text("$selectedMethod Phone / Account Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PartyGold, unfocusedBorderColor = PartyGoldDim),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = withdrawDiamondsInput,
                        onValueChange = { withdrawDiamondsInput = it.filter { ch -> ch.isDigit() }.take(7) },
                        label = { Text("Diamonds to Withdraw (min 100)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PartyGold, unfocusedBorderColor = PartyGoldDim),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Dev Mock Payout Mode — TODO: Connect Telebirr / CBE Birr / Stripe Connect API in production.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedLavender
                    )
                    Button(
                        onClick = {
                            val amt = withdrawDiamondsInput.toLongOrNull() ?: 0L
                            onRequestWithdrawal(amt, selectedMethod, accountInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit $selectedMethod Withdrawal", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Termux Bridge & CLI Integration
            item {
                TermuxBridgeCard(profile = profile)
            }

            // 6. Optional Cloud Backup via Google Sign-In
            item {
                Surface(
                    color = ElevatedPlum,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, if (isGoogleSignedIn) LiveEmerald else PartyGoldDim, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CloudSync, contentDescription = null, tint = if (isGoogleSignedIn) LiveEmerald else PartyGold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGoogleSignedIn) "Cloud Sync Connected" else "Instant Guest Mode Active",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGoogleSignedIn) "Synced with Google & Firebase Firestore." else "Optional: Connect Google to back up your coins & VIP across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftLavender
                            )
                        }
                        if (!isGoogleSignedIn) {
                            Button(
                                onClick = onSignInWithGoogle,
                                colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Sync", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermuxBridgeCard(profile: UserProfile) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var bridgeUrl by remember { mutableStateOf(com.example.data.TermuxBridgeManager.getSavedUrl(context)) }
    var bridgeStatus by remember { mutableStateOf<com.example.data.TermuxBridgeStatus?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var feedbackNote by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(LuxuryObsidianBlack, Color(0xFF0A2419))))
            .border(1.dp, LuxuryEmeraldGreen, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Termux Bridge & CLI Control",
                style = MaterialTheme.typography.titleMedium,
                color = LuxuryEmeraldGreen,
                fontWeight = FontWeight.ExtraBold
            )
            com.example.ui.components.LuxuryBadgePill(
                text = if (bridgeStatus?.connected == true) "CONNECTED" else "TERMUX READY",
                emeraldAccent = bridgeStatus?.connected == true
            )
        }

        Text(
            text = "Launch ANU LIVE from Termux with anulive:// deep links or connect to your local Termux HTTP server.",
            style = MaterialTheme.typography.bodySmall,
            color = SoftLavender
        )

        OutlinedTextField(
            value = bridgeUrl,
            onValueChange = { bridgeUrl = it.take(120) },
            label = { Text("Termux Local Server URL") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LuxuryEmeraldGreen,
                unfocusedBorderColor = PartyGoldDim
            ),
            modifier = Modifier.fillMaxWidth().testTag("termux_url_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    com.example.data.TermuxBridgeManager.saveUrl(context, bridgeUrl)
                    isTesting = true
                    scope.launch {
                        bridgeStatus = com.example.data.TermuxBridgeManager.testConnection(bridgeUrl)
                        isTesting = false
                    }
                },
                enabled = !isTesting,
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryEmeraldGreen, contentColor = LuxuryObsidianBlack),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("termux_test_button")
            ) {
                Text(if (isTesting) "Testing..." else "Test Bridge", fontWeight = FontWeight.ExtraBold)
            }

            Button(
                onClick = {
                    com.example.data.TermuxBridgeManager.saveUrl(context, bridgeUrl)
                    isTesting = true
                    scope.launch {
                        bridgeStatus = com.example.data.TermuxBridgeManager.sendEvent(
                            rawUrl = bridgeUrl,
                            eventType = "ping_from_anu_live",
                            userId = profile.numericId,
                            displayName = profile.displayName,
                            coinBalance = profile.coinBalance
                        )
                        isTesting = false
                    }
                },
                enabled = !isTesting,
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryRoyalGold, contentColor = LuxuryObsidianBlack),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("termux_ping_button")
            ) {
                Text("Send Ping", fontWeight = FontWeight.ExtraBold)
            }
        }

        OutlinedButton(
            onClick = {
                val opened = com.example.data.TermuxBridgeManager.openTermuxApp(context)
                feedbackNote = if (opened) "Opening Termux..." else "Termux app not installed on this device yet."
            },
            border = BorderStroke(1.dp, LuxuryRoyalGold),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("open_termux_button")
        ) {
            Text("Open Termux App", color = LuxuryRoyalGold, fontWeight = FontWeight.Bold)
        }

        if (bridgeStatus != null) {
            Text(
                text = bridgeStatus!!.message,
                style = MaterialTheme.typography.labelMedium,
                color = if (bridgeStatus!!.connected) LuxuryEmeraldGreen else Color(0xFFFF8A80)
            )
        }

        if (feedbackNote != null) {
            Text(
                text = feedbackNote!!,
                style = MaterialTheme.typography.labelSmall,
                color = LuxuryRoyalGold
            )
        }

        Text("Copyable Termux Commands:", style = MaterialTheme.typography.labelMedium, color = LuxuryRoyalGold, fontWeight = FontWeight.Bold)

        val commands = listOf(
            "Launch App" to com.example.data.TermuxBridgeManager.CMD_LAUNCH_APP,
            "Open Party Tab" to com.example.data.TermuxBridgeManager.CMD_OPEN_PARTY,
            "Open Me Tab" to com.example.data.TermuxBridgeManager.CMD_OPEN_ME,
            "Start Node Bridge (:8080)" to com.example.data.TermuxBridgeManager.CMD_TERMUX_SERVER
        )

        commands.forEach { (label, cmd) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RoyalPlumSurface)
                    .border(1.dp, PartyGoldDim.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(cmd))
                        feedbackNote = "Copied '$label' command for Termux!"
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = LuxuryEmeraldGreen, fontWeight = FontWeight.Bold)
                    Text(cmd, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("COPY", style = MaterialTheme.typography.labelSmall, color = LuxuryRoyalGold, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

