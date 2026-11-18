package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PartyCategory(val key: String, val label: String) {
    ALL("all", "All"),
    POPULAR("popular", "Popular"),
    MUSIC("music", "Music"),
    CHAT("chat", "Talk"),
    KARAOKE("karaoke", "Karaoke"),
    GAMES("games", "Gaming"),
    CHILL("chill", "Chill");

    companion object {
        val creatableCategories: List<PartyCategory> = listOf(POPULAR, MUSIC, CHAT, KARAOKE, GAMES, CHILL)

        fun fromKey(raw: String): PartyCategory =
            entries.firstOrNull { it.key == raw } ?: POPULAR
    }
}

enum class GiftCategory(val key: String, val label: String) {
    HOT("hot", "Hot"),
    LUCKY("lucky", "Lucky"),
    VIP("vip", "VIP"),
    SVIP("svip", "SVIP"),
    NEWCOMER("newcomer", "Newcomer"),
    CP("cp", "CP")
}

enum class GiftAnimationStyle {
    LION_SOVEREIGN,
    LUCKY_JACKPOT,
    FIREWORKS_BURST,
    CP_HEARTS,
    ROYAL_CROWN,
    EMERALD_COMET
}

fun formatCompactCoins(amount: Long): String {
    return when {
        amount >= 1_000_000_000L -> {
            val billions = amount / 1_000_000_000.0
            if (amount % 1_000_000_000L == 0L) "${amount / 1_000_000_000L}B"
            else String.format("%.2fB", billions).trimEnd('0').trimEnd('.')
        }
        amount >= 1_000_000L -> {
            val millions = amount / 1_000_000.0
            if (amount % 1_000_000L == 0L) "${amount / 1_000_000L}M"
            else String.format("%.2fM", millions).trimEnd('0').trimEnd('.')
        }
        amount >= 1_000L -> {
            val thousands = amount / 1_000.0
            if (amount % 1_000L == 0L) "${amount / 1_000L}K"
            else String.format("%.1fK", thousands).trimEnd('0').trimEnd('.')
        }
        else -> amount.toString()
    }
}

data class VirtualGift(
    val id: String,
    val name: String,
    val coins: Long,
    val xpReward: Long,
    val badge: String,
    val category: GiftCategory = GiftCategory.HOT,
    val animationStyle: GiftAnimationStyle = GiftAnimationStyle.FIREWORKS_BURST,
    val isLucky: Boolean = false
)

data class GiftSendOutcome(
    val leveledUp: Boolean,
    val luckyMultiplier: Int = 0,
    val luckyWinCoins: Long = 0L
)

data class RechargePack(
    val id: String,
    val coins: Long,
    val bonusCoins: Long,
    val priceDisplay: String,
    val label: String
)

data class VipTier(
    val id: String,
    val name: String,
    val isSvip: Boolean,
    val priceCoins: Long,
    val dailyCashbackCoins: Long,
    val durationDays: Int = 7,
    val perks: List<String>
)

data class NewcomerTaskDay(
    val day: Int,
    val title: String,
    val description: String,
    val rewardCoins: Long
)

data class DailyTaskItem(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Long,
    val xpReward: Long
)

data class AvatarPreset(
    val id: String,
    val label: String,
    val badge: String,
    val isLionEmblem: Boolean = false
)

object AvatarCatalog {
    val presets: List<AvatarPreset> = listOf(
        AvatarPreset("royal_lion", "Royal Lion", "LUXURY", isLionEmblem = true),
        AvatarPreset("emerald_crown", "Emerald Crown", "VIP"),
        AvatarPreset("gold_obsidian", "Obsidian Gold", "SVIP"),
        AvatarPreset("star_host", "Star Host", "LIVE"),
        AvatarPreset("velvet_mic", "Velvet Mic", "PARTY"),
        AvatarPreset("imperial_crest", "Imperial Crest", "KING")
    )

    fun byId(id: String): AvatarPreset = presets.firstOrNull { it.id == id } ?: presets.first()
}

object VipCatalog {
    val tiers: List<VipTier> = listOf(
        VipTier(
            id = "vip1",
            name = "VIP 1",
            isSvip = false,
            priceCoins = 500_000L,
            dailyCashbackCoins = 80_000L,
            durationDays = 7,
            perks = listOf(
                "80K Coin daily cashback x 7 days (560K total)",
                "Gold-Black-Green Emerald Avatar Ring",
                "VIP 1 Chat Badge & Priority Seat"
            )
        ),
        VipTier(
            id = "vip2",
            name = "VIP 2",
            isSvip = false,
            priceCoins = 1_000_000L,
            dailyCashbackCoins = 160_000L,
            durationDays = 7,
            perks = listOf(
                "160K Coin daily cashback x 7 days (1.12M total)",
                "Obsidian & Gold Crown Avatar Aura",
                "VIP 2 Room Entrance Banner & +15% XP"
            )
        ),
        VipTier(
            id = "vip3",
            name = "VIP 3",
            isSvip = false,
            priceCoins = 2_000_000L,
            dailyCashbackCoins = 320_000L,
            durationDays = 7,
            perks = listOf(
                "320K Coin daily cashback x 7 days (2.24M total)",
                "Luxury Gold-Black-Green Lion Entrance Effect",
                "VIP 3 Shield & Exclusive Room Frame"
            )
        ),
        VipTier(
            id = "vip4",
            name = "VIP 4",
            isSvip = false,
            priceCoins = 5_000_000L,
            dailyCashbackCoins = 800_000L,
            durationDays = 7,
            perks = listOf(
                "800K Coin daily cashback x 7 days (5.6M total)",
                "Sovereign Lion Animated Avatar Frame",
                "Anti-Kick Room Guard & +30% XP Boost"
            )
        ),
        VipTier(
            id = "svip1",
            name = "SVIP 1",
            isSvip = true,
            priceCoins = 10_000_000L,
            dailyCashbackCoins = 1_600_000L,
            durationDays = 7,
            perks = listOf(
                "1.6M Coin daily cashback x 7 days (11.2M total)",
                "Apex Gold-Black-Green Lion Roar Full Entrance",
                "SVIP 1 Diamond Halo & Global Broadcast Banner"
            )
        ),
        VipTier(
            id = "svip2",
            name = "SVIP 2",
            isSvip = true,
            priceCoins = 20_000_000L,
            dailyCashbackCoins = 3_200_000L,
            durationDays = 7,
            perks = listOf(
                "3.2M Coin daily cashback x 7 days (22.4M total)",
                "Imperial Obsidian-Emerald Lion Throne Aura",
                "SVIP 2 Private Concierge & Custom Seat Effects"
            )
        ),
        VipTier(
            id = "svip3",
            name = "SVIP 3",
            isSvip = true,
            priceCoins = 30_000_000L,
            dailyCashbackCoins = 5_000_000L,
            durationDays = 7,
            perks = listOf(
                "5M Coin daily cashback x 7 days (35M total)",
                "Supreme 24K Gold + Black + Emerald Lion Sovereign",
                "SVIP 3 Legend Status, Invisible Visit & +100% XP"
            )
        )
    )

    fun byId(id: String): VipTier? = tiers.firstOrNull { it.id == id }
}

object NewcomerTaskCatalog {
    const val REGISTRATION_WELCOME_COINS: Long = 1_000_000L

    val days: List<NewcomerTaskDay> = listOf(
        NewcomerTaskDay(1, "Day 1: Royal Arrival", "Enter ANU LIVE & claim your Day 1 starter gift", 100_000L),
        NewcomerTaskDay(2, "Day 2: Live Explorer", "Visit a Live or Party room and say hello", 120_000L),
        NewcomerTaskDay(3, "Day 3: Voice Star", "Take a speaker seat & test live mic audio", 140_000L),
        NewcomerTaskDay(4, "Day 4: Social Spark", "Like or publish a Moment post", 150_000L),
        NewcomerTaskDay(5, "Day 5: Golden Gifter", "Send any virtual gift in a live room", 160_000L),
        NewcomerTaskDay(6, "Day 6: Party Host", "Create or join a 4/6/8-seat party lounge", 160_000L),
        NewcomerTaskDay(7, "Day 7: Royal Champion", "Complete your 7-day newcomer streak!", 170_000L)
    )

    val dailyTasks: List<DailyTaskItem> = listOf(
        DailyTaskItem("daily_checkin", "Daily Royal Check-In", "Sign in to ANU LIVE today", 25_000L, 100L),
        DailyTaskItem("daily_room", "Join a Live / Party Room", "Listen or speak in any live lounge", 20_000L, 80L),
        DailyTaskItem("daily_gift", "Send a Lucky or Hot Gift", "Bless a host with any virtual gift", 35_000L, 150L),
        DailyTaskItem("daily_moment", "Engage on Moments", "Post or like a community Moment", 20_000L, 90L)
    )
}

object GiftCatalog {
    val gifts: List<VirtualGift> = listOf(
        // HOT (100 -> 1M)
        VirtualGift("hot_rose", "Velvet Rose", 100L, 20L, "ROSE", GiftCategory.HOT, GiftAnimationStyle.FIREWORKS_BURST),
        VirtualGift("hot_buna", "Golden Buna", 500L, 60L, "BUNA", GiftCategory.HOT, GiftAnimationStyle.FIREWORKS_BURST),
        VirtualGift("hot_crown", "Gold Crown", 2_000L, 200L, "CROWN", GiftCategory.HOT, GiftAnimationStyle.ROYAL_CROWN),
        VirtualGift("hot_fireworks", "Neon Fireworks", 10_000L, 1_000L, "SPARK", GiftCategory.HOT, GiftAnimationStyle.FIREWORKS_BURST),
        VirtualGift("hot_supercar", "Emerald GT Car", 100_000L, 8_000L, "GT", GiftCategory.HOT, GiftAnimationStyle.EMERALD_COMET),
        VirtualGift("hot_jet", "Royal Jet", 1_000_000L, 50_000L, "JET", GiftCategory.HOT, GiftAnimationStyle.EMERALD_COMET),

        // LUCKY (50x / 100x Jackpot Winning!)
        VirtualGift("lucky_clover", "Lucky Clover", 100L, 25L, "50x/100x", GiftCategory.LUCKY, GiftAnimationStyle.LUCKY_JACKPOT, isLucky = true),
        VirtualGift("lucky_dice", "Golden Dice", 500L, 80L, "50x/100x", GiftCategory.LUCKY, GiftAnimationStyle.LUCKY_JACKPOT, isLucky = true),
        VirtualGift("lucky_goldbar", "Emerald Gold Bar", 2_000L, 250L, "50x/100x", GiftCategory.LUCKY, GiftAnimationStyle.LUCKY_JACKPOT, isLucky = true),
        VirtualGift("lucky_chest", "Lucky Lion Chest", 10_000L, 1_200L, "50x/100x", GiftCategory.LUCKY, GiftAnimationStyle.LUCKY_JACKPOT, isLucky = true),
        VirtualGift("lucky_vault", "Mega 100x Vault", 50_000L, 5_000L, "100x MAX", GiftCategory.LUCKY, GiftAnimationStyle.LUCKY_JACKPOT, isLucky = true),

        // VIP (50K -> 5M)
        VirtualGift("vip_ring", "Emerald Ring", 50_000L, 4_000L, "VIP", GiftCategory.VIP, GiftAnimationStyle.ROYAL_CROWN),
        VirtualGift("vip_falcon", "Gold Falcon", 250_000L, 15_000L, "VIP 2", GiftCategory.VIP, GiftAnimationStyle.EMERALD_COMET),
        VirtualGift("vip_lion", "Royal Gold Lion", 1_000_000L, 60_000L, "LION", GiftCategory.VIP, GiftAnimationStyle.LION_SOVEREIGN),
        VirtualGift("vip_throne", "Obsidian Throne", 5_000_000L, 200_000L, "VIP 4", GiftCategory.VIP, GiftAnimationStyle.LION_SOVEREIGN),

        // SVIP (10M -> 1,000,000,000 1B!)
        VirtualGift("svip_apex_lion", "Apex Lion Roar", 10_000_000L, 400_000L, "SVIP 1", GiftCategory.SVIP, GiftAnimationStyle.LION_SOVEREIGN),
        VirtualGift("svip_dragon", "Obsidian Dragon", 50_000_000L, 1_500_000L, "SVIP 2", GiftCategory.SVIP, GiftAnimationStyle.EMERALD_COMET),
        VirtualGift("svip_galaxy", "Imperial Galaxy", 100_000_000L, 3_000_000L, "100M", GiftCategory.SVIP, GiftAnimationStyle.FIREWORKS_BURST),
        VirtualGift("svip_universe", "Emerald Universe", 500_000_000L, 10_000_000L, "500M", GiftCategory.SVIP, GiftAnimationStyle.ROYAL_CROWN),
        VirtualGift("svip_empire", "Supreme Lion Empire", 1_000_000_000L, 25_000_000L, "1B MAX", GiftCategory.SVIP, GiftAnimationStyle.LION_SOVEREIGN),

        // NEWCOMER (Cumer)
        VirtualGift("cumer_spark", "Welcome Spark", 100L, 30L, "NEW", GiftCategory.NEWCOMER, GiftAnimationStyle.FIREWORKS_BURST),
        VirtualGift("cumer_bouquet", "Starter Bouquet", 300L, 60L, "NEW", GiftCategory.NEWCOMER, GiftAnimationStyle.FIREWORKS_BURST),
        VirtualGift("cumer_mic", "New Star Mic", 1_000L, 180L, "STAR", GiftCategory.NEWCOMER, GiftAnimationStyle.ROYAL_CROWN),
        VirtualGift("cumer_crown", "Rising Crown", 5_000L, 700L, "RISE", GiftCategory.NEWCOMER, GiftAnimationStyle.ROYAL_CROWN),

        // CP (Couple)
        VirtualGift("cp_ring", "CP Promise Ring", 1_000L, 150L, "CP", GiftCategory.CP, GiftAnimationStyle.CP_HEARTS),
        VirtualGift("cp_hearts", "Twin Gold Hearts", 5_000L, 750L, "LOVE", GiftCategory.CP, GiftAnimationStyle.CP_HEARTS),
        VirtualGift("cp_castle", "Forever Castle", 50_000L, 5_000L, "CP VIP", GiftCategory.CP, GiftAnimationStyle.CP_HEARTS),
        VirtualGift("cp_yacht", "Eternal Love Yacht", 500_000L, 35_000L, "YACHT", GiftCategory.CP, GiftAnimationStyle.CP_HEARTS),
        VirtualGift("cp_palace", "Royal CP Palace", 5_000_000L, 250_000L, "CP KING", GiftCategory.CP, GiftAnimationStyle.CP_HEARTS)
    )

    fun byCategory(category: GiftCategory): List<VirtualGift> = gifts.filter { it.category == category }

    // TODO: Replace mock recharge packs with real Google Play Billing / Stripe Checkout SKUs
    val rechargePacks: List<RechargePack> = listOf(
        RechargePack("pack_500k", 500_000L, 50_000L, "$4.99", "VIP 1 Starter"),
        RechargePack("pack_1m", 1_000_000L, 150_000L, "$9.99", "Popular 1M"),
        RechargePack("pack_10m", 10_000_000L, 2_500_000L, "$74.99", "SVIP 10M"),
        RechargePack("pack_100m", 100_000_000L, 25_000_000L, "$299.99", "Imperial 100M")
    )
}

object LevelCalculator {
    const val XP_PER_LEVEL = 500L

    fun levelForXp(xp: Long): Int = ((xp.coerceAtLeast(0L) / XP_PER_LEVEL) + 1L).toInt().coerceIn(1, 200)

    fun progressInCurrentLevel(xp: Long): Float {
        val remainder = xp.coerceAtLeast(0L) % XP_PER_LEVEL
        return (remainder.toFloat() / XP_PER_LEVEL.toFloat()).coerceIn(0f, 1f)
    }

    fun xpToNextLevel(xp: Long): Long = XP_PER_LEVEL - (xp.coerceAtLeast(0L) % XP_PER_LEVEL)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val numericId: Long = 1L,
    val displayName: String = "Anu Royal Guest",
    val bio: String = "Live streaming & partying on ANU LIVE!",
    val role: String = "user",
    val coinBalance: Long = NewcomerTaskCatalog.REGISTRATION_WELCOME_COINS,
    val diamondBalance: Long = 500L,
    val avatarId: String = "royal_lion",
    val customAvatarUri: String = "",
    val vipTierId: String = "",
    val vipDaysRemaining: Int = 0,
    val lastVipClaimDayKey: String = "",
    val claimedNewcomerDays: List<Int> = emptyList(),
    val claimedDailyTaskIds: List<String> = emptyList(),
    val lastDailyTaskDayKey: String = "",
    val withdrawalMethod: String = "Telebirr",
    val withdrawalAccount: String = "",
    val roomsHosted: Long = 0L,
    val giftsSent: Long = 0L,
    val giftsReceived: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val level: Int = 1,
    val hostLevel: Int = 1,
    val xp: Long = 120L,
    val online: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomBookmark(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomTitle: String = "",
    val hostName: String = "",
    val category: String = "popular",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowedHost(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val targetName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "recharge",
    val amount: Long = 0L,
    val description: String = "",
    val counterpartyName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val notifType: String = "system",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PartyRoom(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val title: String = "",
    val topic: String = "",
    val category: String = "popular",
    val roomMode: String = "party",
    val seatCapacity: Int = 8,
    val shortCode: String = "ANU-100",
    val status: String = "live",
    val coverTheme: String = "music",
    val listenerCount: Long = 1L,
    val giftScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val messageType: String = "chat",
    val giftName: String? = null,
    val giftCoins: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomSeat(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val seatIndex: Int = 0,
    val occupantId: String = "",
    val occupantName: String = "",
    val muted: Boolean = false,
    val cameraOn: Boolean = false,
    val seatRole: String = "speaker",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MomentPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val caption: String = "",
    val mediaType: String = "photo",
    val coverTheme: String = "hero",
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MomentComment(
    @DocumentId val id: String = "",
    val momentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val lastMessage: String = "",
    val unreadCount: Long = 0L,
    val peerOnline: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetId: String = "",
    val targetType: String = "room",
    val reason: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null
)
