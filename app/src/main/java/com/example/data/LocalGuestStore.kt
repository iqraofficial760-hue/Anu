package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * Persistent local store for instant zero-login entry into ANU LIVE.
 * Automatically credits every new user with 1,000,000 FREE welcome coins + 500 Diamonds
 * and provides full reactive state for Live rooms, 4/6/8-seat Party lounges, VIP/SVIP,
 * 7-day Newcomer Tasks, Moments, Messages, Wallet, and Settings until the user optionally
 * connects Google Sign-In for cloud Firestore sync.
 */
object LocalGuestStore {
    private const val PREFS_NAME = "anu_live_guest_store"
    private const val KEY_GUEST_ID = "guest_id"
    private const val KEY_NUMERIC_ID = "numeric_id"
    private const val KEY_NEXT_USER_SEQ = "next_user_seq"
    private const val KEY_NAME = "display_name"
    private const val KEY_BIO = "bio"
    private const val KEY_COINS = "coin_balance"
    private const val KEY_DIAMONDS = "diamond_balance"
    private const val KEY_AVATAR = "avatar_id"
    private const val KEY_CUSTOM_AVATAR_URI = "custom_avatar_uri"
    private const val KEY_VIP_TIER = "vip_tier_id"
    private const val KEY_VIP_DAYS = "vip_days_remaining"
    private const val KEY_VIP_LAST_CLAIM = "vip_last_claim_day"
    private const val KEY_NEWCOMER_CLAIMED = "newcomer_claimed_days"
    private const val KEY_DAILY_CLAIMED = "daily_claimed_ids"
    private const val KEY_DAILY_DAY = "daily_claimed_day"
    private const val KEY_WITHDRAW_METHOD = "withdraw_method"
    private const val KEY_WITHDRAW_ACCOUNT = "withdraw_account"
    private const val KEY_XP = "xp"
    private const val KEY_GIFTS_SENT = "gifts_sent"
    private const val KEY_ROOMS_HOSTED = "rooms_hosted"

    private var prefs: SharedPreferences? = null
    private var initialized = false

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _rooms = MutableStateFlow<List<PartyRoom>>(emptyList())
    val rooms: StateFlow<List<PartyRoom>> = _rooms.asStateFlow()

    private val _roomMessages = MutableStateFlow<Map<String, List<RoomMessage>>>(emptyMap())
    val roomMessages: StateFlow<Map<String, List<RoomMessage>>> = _roomMessages.asStateFlow()

    private val _roomSeats = MutableStateFlow<Map<String, List<RoomSeat>>>(emptyMap())
    val roomSeats: StateFlow<Map<String, List<RoomSeat>>> = _roomSeats.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<RoomBookmark>>(emptyList())
    val bookmarks: StateFlow<List<RoomBookmark>> = _bookmarks.asStateFlow()

    private val _following = MutableStateFlow<List<FollowedHost>>(emptyList())
    val following: StateFlow<List<FollowedHost>> = _following.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _moments = MutableStateFlow<List<MomentPost>>(emptyList())
    val moments: StateFlow<List<MomentPost>> = _moments.asStateFlow()

    private val _momentComments = MutableStateFlow<Map<String, List<MomentComment>>>(emptyMap())
    val momentComments: StateFlow<Map<String, List<MomentComment>>> = _momentComments.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _directMessages = MutableStateFlow<Map<String, List<DirectMessage>>>(emptyMap())
    val directMessages: StateFlow<Map<String, List<DirectMessage>>> = _directMessages.asStateFlow()

    private val _reports = MutableStateFlow<List<ModerationReport>>(emptyList())
    val reports: StateFlow<List<ModerationReport>> = _reports.asStateFlow()

    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    @Synchronized
    fun init(context: Context): String {
        val sp = prefs ?: context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { prefs = it }

        val guestId = sp.getString(KEY_GUEST_ID, null) ?: "guest_${Random.nextInt(100000, 999999)}".also {
            sp.edit().putString(KEY_GUEST_ID, it).apply()
        }

        if (!initialized) {
            initialized = true
            val isFirstLaunch = !sp.contains(KEY_COINS)
            val assignedNumericId = if (sp.contains(KEY_NUMERIC_ID)) {
                sp.getLong(KEY_NUMERIC_ID, 1L)
            } else {
                val seq = sp.getLong(KEY_NEXT_USER_SEQ, 1L)
                sp.edit().putLong(KEY_NUMERIC_ID, seq).putLong(KEY_NEXT_USER_SEQ, seq + 1L).apply()
                seq
            }
            val coins = sp.getLong(KEY_COINS, NewcomerTaskCatalog.REGISTRATION_WELCOME_COINS)
            val diamonds = sp.getLong(KEY_DIAMONDS, 500L)
            val name = sp.getString(KEY_NAME, "Anu Royal #$assignedNumericId") ?: "Anu Royal #$assignedNumericId"
            val bio = sp.getString(KEY_BIO, "Live streaming & VIP partying on ANU LIVE!") ?: ""
            val avatarId = sp.getString(KEY_AVATAR, "royal_lion") ?: "royal_lion"
            val customAvatarUri = sp.getString(KEY_CUSTOM_AVATAR_URI, "") ?: ""
            val vipTierId = sp.getString(KEY_VIP_TIER, "") ?: ""
            val vipDays = sp.getInt(KEY_VIP_DAYS, 0)
            val lastVipClaim = sp.getString(KEY_VIP_LAST_CLAIM, "") ?: ""
            val claimedDaysRaw = sp.getString(KEY_NEWCOMER_CLAIMED, "") ?: ""
            val claimedDays = claimedDaysRaw.split(",").mapNotNull { it.trim().toIntOrNull() }
            val dailyDayKey = sp.getString(KEY_DAILY_DAY, "") ?: ""
            val dailyIdsRaw = if (dailyDayKey == todayKey()) sp.getString(KEY_DAILY_CLAIMED, "") ?: "" else ""
            val claimedDailyIds = dailyIdsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val withdrawMethod = sp.getString(KEY_WITHDRAW_METHOD, "Telebirr") ?: "Telebirr"
            val withdrawAccount = sp.getString(KEY_WITHDRAW_ACCOUNT, "") ?: ""
            val xp = sp.getLong(KEY_XP, 150L)
            val giftsSent = sp.getLong(KEY_GIFTS_SENT, 0L)
            val roomsHosted = sp.getLong(KEY_ROOMS_HOSTED, 0L)

            if (isFirstLaunch) {
                sp.edit()
                    .putLong(KEY_COINS, coins)
                    .putLong(KEY_DIAMONDS, diamonds)
                    .putString(KEY_NAME, name)
                    .putString(KEY_BIO, bio)
                    .putString(KEY_AVATAR, avatarId)
                    .putLong(KEY_XP, xp)
                    .apply()
            }

            _profile.value = UserProfile(
                id = guestId,
                userId = guestId,
                numericId = assignedNumericId,
                displayName = name,
                bio = bio,
                role = "user",
                coinBalance = coins,
                diamondBalance = diamonds,
                avatarId = avatarId,
                customAvatarUri = customAvatarUri,
                vipTierId = vipTierId,
                vipDaysRemaining = vipDays,
                lastVipClaimDayKey = lastVipClaim,
                claimedNewcomerDays = claimedDays,
                claimedDailyTaskIds = claimedDailyIds,
                lastDailyTaskDayKey = dailyDayKey,
                withdrawalMethod = withdrawMethod,
                withdrawalAccount = withdrawAccount,
                roomsHosted = roomsHosted,
                giftsSent = giftsSent,
                level = LevelCalculator.levelForXp(xp),
                hostLevel = (roomsHosted.toInt() + 1).coerceAtLeast(1),
                xp = xp,
                online = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            seedInitialContent(guestId)
        }
        return guestId
    }

    private fun seedInitialContent(guestId: String) {
        val now = Timestamp.now()
        _transactions.value = listOf(
            WalletTransaction(
                id = "tx_welcome",
                userId = guestId,
                txType = "bonus",
                amount = NewcomerTaskCatalog.REGISTRATION_WELCOME_COINS,
                description = "Newcomer Instant Welcome Bonus (1M FREE Coins!)",
                createdAt = now
            )
        )
        _notifications.value = listOf(
            AppNotification(
                id = "notif_welcome",
                userId = guestId,
                notifType = "system",
                title = "1,000,000 FREE Coins Credited!",
                body = "Welcome to ANU LIVE! Open Me > Newcomer Tasks for 7 more days of free coins, or unlock VIP 1-4 & SVIP 1-3.",
                read = false,
                createdAt = now
            )
        )

        val live1 = PartyRoom(
            id = "room_royal_live",
            hostId = "host_selam",
            hostName = "Selam VIP",
            title = "Addis Midnight Royal Stage",
            topic = "Live voice & music lounge — tap a mic seat and speak live!",
            category = "music",
            roomMode = "live",
            seatCapacity = 8,
            shortCode = "ANU-8801",
            status = "live",
            coverTheme = "hero",
            listenerCount = 128L,
            giftScore = 450_000L,
            createdAt = now,
            updatedAt = now
        )
        val party1 = PartyRoom(
            id = "room_party_8",
            hostId = "host_dawit",
            hostName = "King Dawit",
            title = "Gold Lion 8-Seat Voice Party",
            topic = "8-seat open mic lounge! Tap any seat to broadcast live audio.",
            category = "popular",
            roomMode = "party",
            seatCapacity = 8,
            shortCode = "ANU-7742",
            status = "live",
            coverTheme = "music",
            listenerCount = 86L,
            giftScore = 220_000L,
            createdAt = now,
            updatedAt = now
        )
        val party2 = PartyRoom(
            id = "room_party_6",
            hostId = "host_hana",
            hostName = "Hana Lounge",
            title = "Habesha Chill 6-Seat Karaoke",
            topic = "6 seats open — grab the mic and sing live!",
            category = "karaoke",
            roomMode = "party",
            seatCapacity = 6,
            shortCode = "ANU-6619",
            status = "live",
            coverTheme = "lounge",
            listenerCount = 54L,
            giftScore = 115_000L,
            createdAt = now,
            updatedAt = now
        )
        val party3 = PartyRoom(
            id = "room_party_4",
            hostId = "host_abel",
            hostName = "Abel VIP",
            title = "4-Seat SVIP Lion Lounge",
            topic = "Exclusive 4-seat voice talk & luxury lion gifts.",
            category = "chat",
            roomMode = "party",
            seatCapacity = 4,
            shortCode = "ANU-4405",
            status = "live",
            coverTheme = "hero",
            listenerCount = 39L,
            giftScore = 310_000L,
            createdAt = now,
            updatedAt = now
        )
        _rooms.value = listOf(live1, party1, party2, party3)

        _roomSeats.value = mapOf(
            live1.id to listOf(
                RoomSeat("seat_0", live1.id, 0, live1.hostId, live1.hostName, muted = false, cameraOn = true, seatRole = "host", updatedAt = now)
            ),
            party1.id to listOf(
                RoomSeat("seat_0", party1.id, 0, party1.hostId, party1.hostName, muted = false, cameraOn = false, seatRole = "host", updatedAt = now),
                RoomSeat("seat_1", party1.id, 1, "speaker_liya", "Liya", muted = false, cameraOn = false, seatRole = "speaker", updatedAt = now)
            ),
            party2.id to listOf(
                RoomSeat("seat_0", party2.id, 0, party2.hostId, party2.hostName, muted = false, cameraOn = false, seatRole = "host", updatedAt = now)
            ),
            party3.id to listOf(
                RoomSeat("seat_0", party3.id, 0, party3.hostId, party3.hostName, muted = false, cameraOn = true, seatRole = "host", updatedAt = now)
            )
        )

        _roomMessages.value = _rooms.value.associate { room ->
            room.id to listOf(
                RoomMessage(
                    id = "sys_${room.id}",
                    roomId = room.id,
                    senderId = room.hostId,
                    senderName = room.hostName,
                    text = room.topic,
                    messageType = "system",
                    createdAt = now
                )
            )
        }

        _moments.value = listOf(
            MomentPost(
                id = "moment_1",
                authorId = "host_selam",
                authorName = "Selam VIP",
                caption = "Just unlocked the Gold + Black + Green Royal Lion aura on ANU LIVE! Come join our live stage tonight.",
                mediaType = "photo",
                coverTheme = "hero",
                likesCount = 248L,
                commentsCount = 12L,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun persistProfile(p: UserProfile) {
        prefs?.edit()
            ?.putLong(KEY_NUMERIC_ID, p.numericId)
            ?.putString(KEY_NAME, p.displayName)
            ?.putString(KEY_BIO, p.bio)
            ?.putLong(KEY_COINS, p.coinBalance)
            ?.putLong(KEY_DIAMONDS, p.diamondBalance)
            ?.putString(KEY_AVATAR, p.avatarId)
            ?.putString(KEY_CUSTOM_AVATAR_URI, p.customAvatarUri)
            ?.putString(KEY_VIP_TIER, p.vipTierId)
            ?.putInt(KEY_VIP_DAYS, p.vipDaysRemaining)
            ?.putString(KEY_VIP_LAST_CLAIM, p.lastVipClaimDayKey)
            ?.putString(KEY_NEWCOMER_CLAIMED, p.claimedNewcomerDays.joinToString(","))
            ?.putString(KEY_DAILY_CLAIMED, p.claimedDailyTaskIds.joinToString(","))
            ?.putString(KEY_DAILY_DAY, p.lastDailyTaskDayKey)
            ?.putString(KEY_WITHDRAW_METHOD, p.withdrawalMethod)
            ?.putString(KEY_WITHDRAW_ACCOUNT, p.withdrawalAccount)
            ?.putLong(KEY_XP, p.xp)
            ?.putLong(KEY_GIFTS_SENT, p.giftsSent)
            ?.putLong(KEY_ROOMS_HOSTED, p.roomsHosted)
            ?.apply()
    }

    fun updateProfile(transform: (UserProfile) -> UserProfile): UserProfile {
        var updated = _profile.value
        _profile.update { current ->
            updated = transform(current).copy(updatedAt = Timestamp.now())
            updated
        }
        persistProfile(updated)
        return updated
    }

    fun addTransaction(txType: String, amount: Long, description: String, counterpartyName: String? = null) {
        val tx = WalletTransaction(
            id = "tx_${UUID.randomUUID().toString().take(8)}",
            userId = _profile.value.userId,
            txType = txType,
            amount = amount,
            description = description,
            counterpartyName = counterpartyName,
            createdAt = Timestamp.now()
        )
        _transactions.update { listOf(tx) + it }
    }

    fun addNotification(type: String, title: String, body: String) {
        val notif = AppNotification(
            id = "notif_${UUID.randomUUID().toString().take(8)}",
            userId = _profile.value.userId,
            notifType = type,
            title = title,
            body = body,
            read = false,
            createdAt = Timestamp.now()
        )
        _notifications.update { listOf(notif) + it }
    }

    fun markNotificationRead(notifId: String) {
        _notifications.update { list ->
            list.map { if (it.id == notifId) it.copy(read = true) else it }
        }
    }

    fun toggleBookmark(room: PartyRoom, currentlyBookmarked: Boolean) {
        _bookmarks.update { list ->
            if (currentlyBookmarked) list.filterNot { it.roomId == room.id }
            else listOf(RoomBookmark(room.id, room.id, room.title, room.hostName, room.category, Timestamp.now())) + list
        }
    }

    fun toggleFollow(targetUserId: String, targetName: String, currentlyFollowing: Boolean) {
        _following.update { list ->
            if (currentlyFollowing) list.filterNot { it.targetUserId == targetUserId }
            else listOf(FollowedHost(targetUserId, targetUserId, targetName, Timestamp.now())) + list
        }
        updateProfile { it.copy(followingCount = (it.followingCount + if (currentlyFollowing) -1L else 1L).coerceAtLeast(0L)) }
    }

    fun createRoom(
        title: String,
        topic: String,
        category: PartyCategory,
        hostName: String,
        roomMode: String,
        seatCapacity: Int
    ): String {
        val now = Timestamp.now()
        val roomId = "room_${UUID.randomUUID().toString().take(8)}"
        val safeCategory = if (category == PartyCategory.ALL) PartyCategory.POPULAR.key else category.key
        val safeMode = if (roomMode == "live") "live" else "party"
        val safeCap = if (seatCapacity in listOf(4, 6, 8)) seatCapacity else 8
        val shortCode = "ANU-${Random.nextInt(1000, 9999)}"
        val coverTheme = when (safeCategory) {
            "music", "karaoke" -> "music"
            "popular" -> "hero"
            else -> "lounge"
        }
        val uid = _profile.value.userId
        val newRoom = PartyRoom(
            id = roomId,
            hostId = uid,
            hostName = hostName,
            title = title.trim().take(60),
            topic = topic.trim().take(140),
            category = safeCategory,
            roomMode = safeMode,
            seatCapacity = safeCap,
            shortCode = shortCode,
            status = "live",
            coverTheme = coverTheme,
            listenerCount = 1L,
            giftScore = 0L,
            createdAt = now,
            updatedAt = now
        )
        _rooms.update { listOf(newRoom) + it }
        _roomSeats.update { map ->
            map + (roomId to listOf(
                RoomSeat("seat_0", roomId, 0, uid, hostName, muted = false, cameraOn = (safeMode == "live"), seatRole = "host", updatedAt = now)
            ))
        }
        _roomMessages.update { map ->
            map + (roomId to listOf(
                RoomMessage(
                    id = "sys_$roomId",
                    roomId = roomId,
                    senderId = uid,
                    senderName = hostName,
                    text = topic.ifBlank { "Welcome to ${newRoom.title} ($shortCode)! Live audio is ON." },
                    messageType = "system",
                    createdAt = now
                )
            ))
        }
        updateProfile { it.copy(roomsHosted = it.roomsHosted + 1L, xp = it.xp + 100L, hostLevel = (it.roomsHosted.toInt() + 2)) }
        addNotification("live_started", if (safeMode == "live") "You went Live!" else "Party Room Open!", "${newRoom.title} ($shortCode) is now live.")
        return roomId
    }

    fun endRoom(roomId: String) {
        _rooms.update { list -> list.map { if (it.id == roomId) it.copy(status = "ended") else it } }
    }

    fun adjustListeners(roomId: String, delta: Long) {
        _rooms.update { list ->
            list.map { if (it.id == roomId && it.status == "live") it.copy(listenerCount = (it.listenerCount + delta).coerceAtLeast(1L)) else it }
        }
    }

    fun sendRoomMessage(roomId: String, senderName: String, text: String, type: String = "chat", giftName: String? = null, giftCoins: Long? = null) {
        val msg = RoomMessage(
            id = "msg_${UUID.randomUUID().toString().take(8)}",
            roomId = roomId,
            senderId = _profile.value.userId,
            senderName = senderName,
            text = text,
            messageType = type,
            giftName = giftName,
            giftCoins = giftCoins,
            createdAt = Timestamp.now()
        )
        _roomMessages.update { map ->
            val current = map[roomId] ?: emptyList()
            map + (roomId to (current + msg))
        }
    }

    fun addRoomGiftScore(roomId: String, coins: Long) {
        _rooms.update { list ->
            list.map { if (it.id == roomId) it.copy(giftScore = it.giftScore + coins) else it }
        }
    }

    fun claimSeat(roomId: String, seatIndex: Int, occupantName: String, cameraOn: Boolean) {
        val uid = _profile.value.userId
        val seat = RoomSeat("seat_$seatIndex", roomId, seatIndex, uid, occupantName, muted = false, cameraOn = cameraOn, seatRole = if (seatIndex == 0) "host" else "speaker", updatedAt = Timestamp.now())
        _roomSeats.update { map ->
            val current = (map[roomId] ?: emptyList()).filterNot { it.seatIndex == seatIndex }
            map + (roomId to (current + seat).sortedBy { it.seatIndex })
        }
    }

    fun toggleSeatMute(roomId: String, seatIndex: Int, muted: Boolean) {
        _roomSeats.update { map ->
            val current = map[roomId] ?: emptyList()
            map + (roomId to current.map { if (it.seatIndex == seatIndex) it.copy(muted = muted) else it })
        }
    }

    fun toggleSeatCamera(roomId: String, seatIndex: Int, cameraOn: Boolean) {
        _roomSeats.update { map ->
            val current = map[roomId] ?: emptyList()
            map + (roomId to current.map { if (it.seatIndex == seatIndex) it.copy(cameraOn = cameraOn) else it })
        }
    }

    fun leaveSeat(roomId: String, seatIndex: Int) {
        _roomSeats.update { map ->
            val current = map[roomId] ?: emptyList()
            map + (roomId to current.filterNot { it.seatIndex == seatIndex })
        }
    }

    fun createMoment(authorName: String, caption: String, mediaType: String, coverTheme: String) {
        val now = Timestamp.now()
        val post = MomentPost(
            id = "moment_${UUID.randomUUID().toString().take(8)}",
            authorId = _profile.value.userId,
            authorName = authorName,
            caption = caption.trim().take(300),
            mediaType = mediaType,
            coverTheme = coverTheme,
            createdAt = now,
            updatedAt = now
        )
        _moments.update { listOf(post) + it }
    }

    fun likeMoment(momentId: String) {
        _moments.update { list -> list.map { if (it.id == momentId) it.copy(likesCount = it.likesCount + 1L) else it } }
    }

    fun addMomentComment(momentId: String, authorName: String, text: String) {
        val c = MomentComment(
            id = "cmt_${UUID.randomUUID().toString().take(8)}",
            momentId = momentId,
            authorId = _profile.value.userId,
            authorName = authorName,
            text = text.trim().take(240),
            createdAt = Timestamp.now()
        )
        _momentComments.update { map -> map + (momentId to ((map[momentId] ?: emptyList()) + c)) }
        _moments.update { list -> list.map { if (it.id == momentId) it.copy(commentsCount = it.commentsCount + 1L) else it } }
    }

    fun startConversation(myName: String, peerName: String, initialMessage: String): String {
        val now = Timestamp.now()
        val convId = "conv_${UUID.randomUUID().toString().take(8)}"
        val uid = _profile.value.userId
        val participants = listOf(uid, "peer_${peerName.lowercase().take(10)}")
        val conv = Conversation(
            id = convId,
            participantIds = participants,
            participantNames = listOf(myName, peerName),
            lastMessage = initialMessage.ifBlank { "Hey!" },
            unreadCount = 0L,
            peerOnline = true,
            createdAt = now,
            updatedAt = now
        )
        _conversations.update { listOf(conv) + it }
        val dm = DirectMessage("dm_${UUID.randomUUID().toString().take(8)}", convId, participants, uid, myName, conv.lastMessage, now)
        _directMessages.update { it + (convId to listOf(dm)) }
        return convId
    }

    fun sendDirectMessage(conversationId: String, participantIds: List<String>, senderName: String, text: String) {
        val now = Timestamp.now()
        val dm = DirectMessage("dm_${UUID.randomUUID().toString().take(8)}", conversationId, participantIds, _profile.value.userId, senderName, text.trim().take(500), now)
        _directMessages.update { map -> map + (conversationId to ((map[conversationId] ?: emptyList()) + dm)) }
        _conversations.update { list -> list.map { if (it.id == conversationId) it.copy(lastMessage = dm.text, updatedAt = now) else it } }
    }

    fun submitReport(targetId: String, targetType: String, reason: String) {
        val rep = ModerationReport(
            id = "rep_${UUID.randomUUID().toString().take(8)}",
            reporterId = _profile.value.userId,
            targetId = targetId,
            targetType = targetType,
            reason = reason.trim().take(240),
            status = "open",
            createdAt = Timestamp.now()
        )
        _reports.update { listOf(rep) + it }
    }
}
