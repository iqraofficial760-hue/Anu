package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppNotification
import com.example.data.Conversation
import com.example.data.DirectMessage
import com.example.data.FollowedHost
import com.example.data.ModerationReport
import com.example.data.MomentComment
import com.example.data.MomentPost
import com.example.data.NewcomerTaskDay
import com.example.data.PartyCategory
import com.example.data.PartyRepository
import com.example.data.PartyRoom
import com.example.data.RechargePack
import com.example.data.RoomBookmark
import com.example.data.RoomMessage
import com.example.data.RoomSeat
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.VipTier
import com.example.data.VirtualGift
import com.example.data.WalletTransaction
import com.example.data.formatCompactCoins
import com.example.streaming.BeautyFilter
import com.example.streaming.LiveStreamingEngine
import com.example.streaming.StreamSessionState
import com.example.streaming.StreamingProviderFactory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val VM_TAG = "AnuLiveVM"
private const val STOP_TIMEOUT_MS = 5000L

private fun unwrapFirestoreError(error: Throwable): Throwable {
    val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError ?: error
}

private fun <T> Flow<T>.toUiStateFlow(
    scope: kotlinx.coroutines.CoroutineScope,
    errorFallback: String
): StateFlow<UiState<T>> = map<T, UiState<T>> { UiState.Success(it) }
    .catch { error ->
        val root = unwrapFirestoreError(error)
        Log.w(VM_TAG, errorFallback, root)
        emit(UiState.Error(root.message ?: errorFallback))
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

class PartyLobbyViewModel(
    private val repository: PartyRepository,
    val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(PartyCategory.ALL)
    val selectedCategory: StateFlow<PartyCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _followedOnly = MutableStateFlow(false)
    val followedOnly: StateFlow<Boolean> = _followedOnly.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow(viewModelScope, "Failed to load profile")

    val liveRooms: StateFlow<UiState<List<PartyRoom>>> =
        repository.observeLiveRooms().toUiStateFlow(viewModelScope, "Failed to load live rooms")

    val hostedRooms: StateFlow<UiState<List<PartyRoom>>> =
        repository.observeHostedRooms(currentUserId).toUiStateFlow(viewModelScope, "Failed to load hosted rooms")

    val bookmarks: StateFlow<UiState<List<RoomBookmark>>> =
        repository.observeBookmarks(currentUserId).toUiStateFlow(viewModelScope, "Failed to load bookmarks")

    val following: StateFlow<UiState<List<FollowedHost>>> =
        repository.observeFollowing(currentUserId).toUiStateFlow(viewModelScope, "Failed to load following")

    val transactions: StateFlow<UiState<List<WalletTransaction>>> =
        repository.observeTransactions(currentUserId).toUiStateFlow(viewModelScope, "Failed to load transactions")

    val notifications: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId).toUiStateFlow(viewModelScope, "Failed to load notifications")

    val moments: StateFlow<UiState<List<MomentPost>>> =
        repository.observeMoments().toUiStateFlow(viewModelScope, "Failed to load moments")

    val conversations: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId).toUiStateFlow(viewModelScope, "Failed to load conversations")

    val reports: StateFlow<UiState<List<ModerationReport>>> =
        repository.observeMyReports(currentUserId).toUiStateFlow(viewModelScope, "Failed to load reports")

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName)
        }
    }

    fun selectCategory(category: PartyCategory) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFollowedOnly() {
        _followedOnly.value = !_followedOnly.value
    }

    private fun currentProfile(): UserProfile =
        (userProfile.value as? UiState.Success)?.data ?: UserProfile(id = currentUserId, userId = currentUserId)

    private fun currentDisplayName(): String = currentProfile().displayName

    private fun currentCoins(): Long = currentProfile().coinBalance

    fun createRoom(
        title: String,
        topic: String,
        category: PartyCategory,
        roomMode: String,
        seatCapacity: Int,
        onRoomCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.createPartyRoom(title, topic, category, currentDisplayName(), roomMode, seatCapacity)
                .onSuccess { roomId -> onRoomCreated(roomId) }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not start room.") }
        }
    }

    fun toggleBookmark(room: PartyRoom, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(room, isCurrentlyBookmarked)
                .onSuccess {
                    _events.tryEmit(if (isCurrentlyBookmarked) "Removed from bookmarks" else "Saved room")
                }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not update bookmark.") }
        }
    }

    fun toggleFollow(targetUserId: String, targetName: String, currentlyFollowing: Boolean) {
        viewModelScope.launch {
            repository.toggleFollowHost(targetUserId, targetName, currentlyFollowing)
                .onSuccess {
                    _events.tryEmit(if (currentlyFollowing) "Unfollowed $targetName" else "Following $targetName")
                }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not update follow.") }
        }
    }

    fun mockRecharge(pack: RechargePack) {
        viewModelScope.launch {
            repository.mockRechargeCoins(pack, currentCoins())
                .onSuccess { _events.tryEmit("Recharged +${formatCompactCoins(pack.coins + pack.bonusCoins)} coins (Dev Mock)") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Recharge failed.") }
        }
    }

    fun purchaseVip(tier: VipTier, onActivated: (VipTier) -> Unit) {
        viewModelScope.launch {
            repository.purchaseVipTier(tier, currentCoins())
                .onSuccess {
                    _events.tryEmit("${tier.name} activated! Claim ${formatCompactCoins(tier.dailyCashbackCoins)}/day for 7 days.")
                    onActivated(tier)
                }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not purchase ${tier.name}.") }
        }
    }

    fun claimVipDailyCashback() {
        viewModelScope.launch {
            repository.claimVipDailyCashback(currentProfile())
                .onSuccess { coins -> _events.tryEmit("Claimed +${formatCompactCoins(coins)} VIP daily cashback!") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Cashback unavailable.") }
        }
    }

    fun claimNewcomerDay(task: NewcomerTaskDay) {
        viewModelScope.launch {
            repository.claimNewcomerTaskDay(task, currentProfile())
                .onSuccess { coins -> _events.tryEmit("Day ${task.day} claimed! +${formatCompactCoins(coins)} FREE coins!") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not claim reward.") }
        }
    }

    fun exchangeDiamonds(diamonds: Long) {
        viewModelScope.launch {
            repository.exchangeDiamondsToCoins(diamonds, currentProfile())
                .onSuccess { coins -> _events.tryEmit("Exchanged $diamonds Diamonds -> +${formatCompactCoins(coins)} Coins!") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Exchange failed.") }
        }
    }

    fun requestWithdrawal(diamonds: Long, method: String, account: String) {
        viewModelScope.launch {
            repository.requestDiamondWithdrawal(diamonds, method, account, currentProfile())
                .onSuccess { _events.tryEmit("Withdrawal request for $diamonds Diamonds submitted ($method).") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Withdrawal failed.") }
        }
    }

    fun claimDailyTask(task: com.example.data.DailyTaskItem) {
        viewModelScope.launch {
            repository.claimDailyTask(task, currentProfile())
                .onSuccess { coins -> _events.tryEmit("${task.title} claimed! +${formatCompactCoins(coins)} coins!") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Already claimed.") }
        }
    }

    fun updateCustomAvatarUri(uri: String) {
        viewModelScope.launch {
            repository.updateCustomAvatarUri(uri)
                .onSuccess { _events.tryEmit("Profile photo / GIF updated!") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not update photo.") }
        }
    }

    fun updateSettings(
        displayName: String,
        bio: String,
        avatarId: String,
        customAvatarUri: String,
        withdrawalMethod: String,
        withdrawalAccount: String
    ) {
        viewModelScope.launch {
            repository.updateUserSettings(displayName, bio, avatarId, customAvatarUri, withdrawalMethod, withdrawalAccount)
                .onSuccess { _events.tryEmit("Profile & Settings saved!") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not save settings.") }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun createMoment(caption: String, mediaType: String, coverTheme: String) {
        viewModelScope.launch {
            repository.createMoment(currentDisplayName(), caption, mediaType, coverTheme)
                .onSuccess { _events.tryEmit("Moment published!") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not post moment.") }
        }
    }

    fun likeMoment(momentId: String) {
        viewModelScope.launch {
            repository.likeMoment(momentId)
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not like moment.") }
        }
    }

    fun observeMomentComments(momentId: String): Flow<List<MomentComment>> =
        repository.observeMomentComments(momentId)

    fun addMomentComment(momentId: String, text: String) {
        viewModelScope.launch {
            repository.addMomentComment(momentId, currentDisplayName(), text)
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not post comment.") }
        }
    }

    fun startConversation(peerName: String, initialMessage: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repository.startConversation(currentDisplayName(), peerName, initialMessage)
                .onSuccess { convId -> onCreated(convId) }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not start chat.") }
        }
    }

    fun submitReport(targetId: String, targetType: String, reason: String) {
        viewModelScope.launch {
            repository.submitReport(targetId, targetType, reason)
                .onSuccess { _events.tryEmit("Report submitted to moderation team.") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not submit report.") }
        }
    }

    fun endRoom(roomId: String) {
        viewModelScope.launch {
            repository.endPartyRoom(roomId)
                .onSuccess { _events.tryEmit("Room ended.") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not end room.") }
        }
    }
}

class LiveRoomViewModel(
    private val repository: PartyRepository,
    val roomId: String,
    val currentUserId: String,
    private val streamingEngine: LiveStreamingEngine = StreamingProviderFactory.create()
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _activeGiftBanner = MutableStateFlow<String?>(null)
    val activeGiftBanner: StateFlow<String?> = _activeGiftBanner.asStateFlow()

    private val _activeGiftEffect = MutableStateFlow<com.example.ui.components.ActiveGiftEffect?>(null)
    val activeGiftEffect: StateFlow<com.example.ui.components.ActiveGiftEffect?> = _activeGiftEffect.asStateFlow()

    val streamState: StateFlow<StreamSessionState> = streamingEngine.sessionState

    val roomState: StateFlow<UiState<PartyRoom?>> =
        repository.observeRoom(roomId).toUiStateFlow(viewModelScope, "Failed to load room")

    val messagesState: StateFlow<UiState<List<RoomMessage>>> =
        repository.observeRoomMessages(roomId).toUiStateFlow(viewModelScope, "Failed to load chat")

    val seatsState: StateFlow<UiState<List<RoomSeat>>> =
        repository.observeRoomSeats(roomId).toUiStateFlow(viewModelScope, "Failed to load seats")

    init {
        viewModelScope.launch {
            repository.adjustListenerCount(roomId, +1L)
        }
        streamingEngine.connect(roomId, currentUserId, publishMedia = true, scope = viewModelScope)
    }

    fun onAudioPermissionResult(granted: Boolean) {
        streamingEngine.setAudioPermissionGranted(granted, viewModelScope)
        if (granted) {
            _events.tryEmit("Live microphone connected! Your voice is transmitting.")
        } else {
            _events.tryEmit("Microphone permission denied — listening only.")
        }
    }

    fun toggleCamera() {
        streamingEngine.setCameraEnabled(!streamState.value.cameraEnabled)
    }

    fun toggleMic() {
        streamingEngine.setMicEnabled(!streamState.value.micEnabled)
    }

    fun toggleAudioMonitor() {
        val next = !streamState.value.audioMonitorEnabled
        streamingEngine.setAudioMonitorEnabled(next)
        _events.tryEmit(if (next) "Live voice monitor ON (hearing your mic)" else "Live voice monitor OFF")
    }

    fun switchCamera() {
        streamingEngine.switchCamera()
    }

    fun selectBeautyFilter(filter: BeautyFilter) {
        streamingEngine.setBeautyFilter(filter)
    }

    fun dismissGiftEffect() {
        _activeGiftEffect.value = null
    }

    fun sendChat(senderName: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(roomId, senderName, text)
                .onFailure { e -> _events.tryEmit(e.message ?: "Failed to send message.") }
        }
    }

    fun sendGift(
        senderName: String,
        receiverName: String,
        gift: VirtualGift,
        currentCoins: Long,
        currentXp: Long
    ) {
        viewModelScope.launch {
            repository.sendVirtualGift(roomId, senderName, receiverName, gift, currentCoins, currentXp)
                .onSuccess { outcome ->
                    val winText = if (outcome.luckyMultiplier > 0) {
                        " • LUCKY ${outcome.luckyMultiplier}x WIN (+${formatCompactCoins(outcome.luckyWinCoins)} coins)!"
                    } else ""
                    _activeGiftBanner.value = "$senderName sent ${gift.name}$winText"
                    _activeGiftEffect.value = com.example.ui.components.ActiveGiftEffect(
                        title = gift.name,
                        subtitle = if (outcome.luckyMultiplier > 0) {
                            "$senderName WON +${formatCompactCoins(outcome.luckyWinCoins)} COINS (${outcome.luckyMultiplier}x)!"
                        } else {
                            "$senderName gifted $receiverName • ${formatCompactCoins(gift.coins)} Coins"
                        },
                        style = gift.animationStyle,
                        multiplierBadge = if (outcome.luckyMultiplier > 0) "${outcome.luckyMultiplier}x LUCKY WIN!" else null
                    )
                    if (outcome.luckyMultiplier >= 50) {
                        _events.tryEmit("JACKPOT! ${outcome.luckyMultiplier}x LUCKY WIN: +${formatCompactCoins(outcome.luckyWinCoins)} coins!")
                    } else if (outcome.leveledUp) {
                        _events.tryEmit("LEVEL UP! You reached a new ANU LIVE level!")
                    } else {
                        _events.tryEmit("Sent ${gift.name} (+${gift.xpReward} XP)!")
                    }
                    delay(3500L)
                    _activeGiftBanner.value = null
                    _activeGiftEffect.value = null
                }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not send gift.") }
        }
    }

    fun claimSeat(seatIndex: Int, occupantName: String) {
        viewModelScope.launch {
            repository.claimSeat(roomId, seatIndex, occupantName, cameraOn = false)
                .onSuccess {
                    streamingEngine.setMicEnabled(true)
                    _events.tryEmit("Joined Seat #${seatIndex + 1} • Live Mic ON")
                }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not take seat.") }
        }
    }

    fun toggleSeatMute(seatIndex: Int, currentMuted: Boolean) {
        viewModelScope.launch {
            val nextMuted = !currentMuted
            repository.toggleSeatMute(roomId, seatIndex, nextMuted)
                .onSuccess { streamingEngine.setMicEnabled(!nextMuted) }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not toggle mic.") }
        }
    }

    fun toggleSeatCamera(seatIndex: Int, currentCameraOn: Boolean) {
        viewModelScope.launch {
            repository.toggleSeatCamera(roomId, seatIndex, !currentCameraOn)
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not toggle camera.") }
        }
    }

    fun leaveSeat(seatIndex: Int) {
        viewModelScope.launch {
            repository.leaveSeat(roomId, seatIndex)
                .onSuccess {
                    streamingEngine.setMicEnabled(false)
                    _events.tryEmit("Seat vacated")
                }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not vacate seat.") }
        }
    }

    fun submitReport(reason: String) {
        viewModelScope.launch {
            repository.submitReport(roomId, "room", reason)
                .onSuccess { _events.tryEmit("Room reported for review.") }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not submit report.") }
        }
    }

    fun endRoom(onEnded: () -> Unit) {
        viewModelScope.launch {
            repository.endPartyRoom(roomId)
                .onSuccess {
                    streamingEngine.disconnect()
                    onEnded()
                }
                .onFailure { e -> _events.tryEmit(e.message ?: "Could not end room.") }
        }
    }

    fun onExitRoom() {
        streamingEngine.disconnect()
        viewModelScope.launch {
            repository.adjustListenerCount(roomId, -1L)
        }
    }

    override fun onCleared() {
        streamingEngine.disconnect()
        super.onCleared()
    }
}

class DirectChatViewModel(
    private val repository: PartyRepository,
    val conversationId: String,
    val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val messagesState: StateFlow<UiState<List<DirectMessage>>> =
        repository.observeDirectMessages(conversationId)
            .toUiStateFlow(viewModelScope, "Failed to load direct messages")

    fun send(participantIds: List<String>, senderName: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendDirectMessage(conversationId, participantIds, senderName, text)
                .onFailure { e -> _events.tryEmit(e.message ?: "Failed to send message.") }
        }
    }
}
