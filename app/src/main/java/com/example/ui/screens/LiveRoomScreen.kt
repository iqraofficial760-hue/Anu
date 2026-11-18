package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.GiftCatalog
import com.example.data.PartyRoom
import com.example.data.RoomMessage
import com.example.data.RoomSeat
import com.example.data.UiState
import com.example.data.VirtualGift
import com.example.streaming.BeautyFilter
import com.example.streaming.formatDuration
import com.example.ui.LiveRoomViewModel
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.components.ReportDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomScreen(
    viewModel: LiveRoomViewModel,
    currentUserDisplayName: String,
    currentUserCoins: Long,
    currentUserXp: Long,
    isFollowingHost: Boolean,
    onToggleFollowHost: (hostId: String, hostName: String) -> Unit,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val roomState by viewModel.roomState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val seatsState by viewModel.seatsState.collectAsStateWithLifecycle()
    val streamState by viewModel.streamState.collectAsStateWithLifecycle()
    val giftBanner by viewModel.activeGiftBanner.collectAsStateWithLifecycle()
    val activeGiftEffect by viewModel.activeGiftEffect.collectAsStateWithLifecycle()

    var chatInput by remember { mutableStateOf("") }
    var showGiftSheet by remember { mutableStateOf(false) }
    var showBeautyBar by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAudioPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.onAudioPermissionResult(true)
        } else {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> onShowMessage(msg) }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onExitRoom() }
    }

    val room = (roomState as? UiState.Success)?.data
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val seats = (seatsState as? UiState.Success)?.data ?: emptyList()
    val seatByIndex = remember(seats) { seats.associateBy { it.seatIndex } }
    val mySeat = remember(seats, viewModel.currentUserId) {
        seats.firstOrNull { it.occupantId == viewModel.currentUserId }
    }
    val isHost = room?.hostId == viewModel.currentUserId
    val isRoomLive = room?.status == "live"
    val isVideoLive = room?.roomMode == "live"
    val seatCapacity = (room?.seatCapacity ?: 8).coerceIn(4, 8)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CardPlumTop, MidnightPurple, MidnightPurple)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Room Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("room_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave room", tint = Color.White)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = room?.title ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRoomLive) {
                            AnimatedEqualizerBars(color = PartyGold)
                            Text(
                                text = formatDuration(streamState.durationSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = PartyGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = room?.shortCode ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedLavender
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Headphones, contentDescription = null, tint = SoftLavender, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${room?.listenerCount ?: 0}", style = MaterialTheme.typography.labelSmall, color = SoftLavender)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = PartyGold, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${room?.giftScore ?: 0}", style = MaterialTheme.typography.labelSmall, color = PartyGold)
                        }
                    }
                }

                if (room != null && !isHost) {
                    TextButton(
                        onClick = { onToggleFollowHost(room.hostId, room.hostName) }
                    ) {
                        Text(
                            text = if (isFollowingHost) "Following" else "+ Follow",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isFollowingHost) LiveEmerald else PartyGold
                        )
                    }
                }

                if (room != null) {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Join \"${room.title}\" (${room.shortCode}) live on ANU LIVE!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Invite to ANU LIVE"))
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share room", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Filled.Flag, contentDescription = "Report room", tint = SoftLavender, modifier = Modifier.size(20.dp))
                    }
                }

                if (isHost && isRoomLive) {
                    IconButton(
                        onClick = { viewModel.endRoom(onNavigateBack) },
                        modifier = Modifier.testTag("room_end_button")
                    ) {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = stringResource(R.string.end_live), tint = LiveCoral)
                    }
                }
            }

            if (roomState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PartyGold)
                }
                return@Column
            }

            // Real-Time Live Voice Transmission Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(com.example.ui.components.LuxuryObsidianBlack.copy(alpha = 0.85f))
                    .border(
                        1.dp,
                        if (streamState.audioEngineActive) com.example.ui.components.LuxuryEmeraldGreen else PartyGoldDim,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (streamState.audioEngineActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = "Live Audio Status",
                    tint = if (streamState.audioEngineActive) com.example.ui.components.LuxuryEmeraldGreen else LiveCoral,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (streamState.audioEngineActive) "ON-AIR LIVE AUDIO • 16kHz PCM" else "Mic Paused / Tap Mic to Speak",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (streamState.audioEngineActive) com.example.ui.components.LuxuryEmeraldGreen else SoftLavender,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { streamState.micAmplitude.coerceIn(0.05f, 1f) },
                        color = com.example.ui.components.LuxuryRoyalGold,
                        trackColor = ElevatedPlum,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50))
                    )
                }
                Surface(
                    color = if (streamState.audioMonitorEnabled) com.example.ui.components.LuxuryEmeraldGreen else ElevatedPlum,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { viewModel.toggleAudioMonitor() }
                ) {
                    Text(
                        text = if (streamState.audioMonitorEnabled) "Monitor ON" else "Hear Mic",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (streamState.audioMonitorEnabled) com.example.ui.components.LuxuryObsidianBlack else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Stage Area: Video Broadcast Stage OR Multi-Seat (4/6/8) Party Grid
            if (isVideoLive) {
                LiveBroadcastStageCard(
                    hostName = room?.hostName ?: "Host",
                    streamState = streamState,
                    isHost = isHost,
                    onToggleCamera = viewModel::toggleCamera,
                    onToggleMic = viewModel::toggleMic,
                    onSwitchCamera = viewModel::switchCamera,
                    onToggleBeauty = { showBeautyBar = !showBeautyBar }
                )

                if (showBeautyBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BeautyFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = streamState.beautyFilter == filter,
                                onClick = { viewModel.selectBeautyFilter(filter) },
                                label = { Text(filter.label) },
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
            } else {
                // Multi-Seat Party Stage (4, 6, or 8 seats)
                val columns = if (seatCapacity == 4) 2 else if (seatCapacity == 6) 3 else 4
                val rows = (seatCapacity + columns - 1) / columns
                Surface(
                    color = RoyalPlumSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, PartyGoldDim, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (c in 0 until columns) {
                                    val seatIdx = r * columns + c
                                    if (seatIdx < seatCapacity) {
                                        val seat = seatByIndex[seatIdx]
                                        SpeakerSeatItem(
                                            seatIndex = seatIdx,
                                            seat = seat,
                                            isHostSeat = seatIdx == 0,
                                            isMine = seat?.occupantId == viewModel.currentUserId,
                                            canInteract = isRoomLive,
                                            onClick = {
                                                if (!isRoomLive) return@SpeakerSeatItem
                                                when {
                                                    seat == null && mySeat == null -> {
                                                        viewModel.claimSeat(seatIdx, currentUserDisplayName)
                                                    }
                                                    seat?.occupantId == viewModel.currentUserId -> {
                                                        viewModel.toggleSeatMute(seatIdx, seat.muted)
                                                    }
                                                    seat == null && mySeat != null -> {
                                                        onShowMessage("You are already on Seat #${mySeat.seatIndex + 1}.")
                                                    }
                                                    else -> {
                                                        if (isHost && seat != null && seat.occupantId != viewModel.currentUserId) {
                                                            viewModel.leaveSeat(seatIdx)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Comments & Gift Feed
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(message = msg)
                }
            }

            // Bottom Interactive Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoyalPlumSurface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isVideoLive) {
                    // Seat / Mic button
                    Surface(
                        color = if (mySeat != null && !mySeat.muted) LiveEmerald.copy(alpha = 0.2f) else ElevatedPlum,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, if (mySeat != null && !mySeat.muted) LiveEmerald else PartyGoldDim, CircleShape)
                            .clickable(enabled = isRoomLive) {
                                if (mySeat != null) {
                                    viewModel.leaveSeat(mySeat.seatIndex)
                                } else {
                                    val firstFree = (1 until seatCapacity).firstOrNull { seatByIndex[it] == null }
                                    if (firstFree != null) viewModel.claimSeat(firstFree, currentUserDisplayName)
                                    else onShowMessage("All $seatCapacity seats are full.")
                                }
                            }
                            .testTag("room_stage_action_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (mySeat == null) Icons.Filled.Mic else if (mySeat.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Seat Mic",
                                tint = if (mySeat != null && !mySeat.muted) LiveEmerald else PartyGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Seat Camera Toggle when on a seat
                    if (mySeat != null) {
                        Surface(
                            color = if (mySeat.cameraOn) PartyGold.copy(alpha = 0.2f) else ElevatedPlum,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, PartyGoldDim, CircleShape)
                                .clickable(enabled = isRoomLive) {
                                    viewModel.toggleSeatCamera(mySeat.seatIndex, mySeat.cameraOn)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (mySeat.cameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                    contentDescription = "Toggle seat camera",
                                    tint = if (mySeat.cameraOn) PartyGold else MutedLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it.take(280) },
                    placeholder = { Text(if (isRoomLive) "Comment..." else "Ended", style = MaterialTheme.typography.bodyMedium) },
                    enabled = isRoomLive,
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PartyGold,
                        unfocusedBorderColor = PartyGoldDim,
                        focusedContainerColor = MidnightPurple,
                        unfocusedContainerColor = MidnightPurple
                    ),
                    modifier = Modifier.weight(1f).testTag("room_chat_input")
                )

                IconButton(
                    onClick = {
                        val trimmed = chatInput.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.sendChat(currentUserDisplayName, trimmed)
                            chatInput = ""
                        }
                    },
                    enabled = isRoomLive && chatInput.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (chatInput.isNotBlank() && isRoomLive) PartyGold else ElevatedPlum)
                        .testTag("room_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (chatInput.isNotBlank() && isRoomLive) DeepObsidianText else MutedLavender,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { showGiftSheet = true },
                    enabled = isRoomLive,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(LiveCoral, PartyGold)))
                        .testTag("room_gift_button")
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = stringResource(R.string.send_gift), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Animated Gift Celebration Overlay
        AnimatedVisibility(
            visible = giftBanner != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                color = CardPlumTop.copy(alpha = 0.94f),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.border(2.dp, PartyGold, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = PartyGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = giftBanner.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = PartyGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Per-Gift Custom Animated Overlay (Lion Sovereign, Lucky 50x/100x, Fireworks, CP Hearts, Crown, Comet)
        com.example.ui.components.GiftAnimationOverlay(
            effect = activeGiftEffect,
            onDismiss = viewModel::dismissGiftEffect
        )
    }

    if (showGiftSheet && isRoomLive && room != null) {
        ModalBottomSheet(
            onDismissRequest = { showGiftSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RoyalPlumSurface
        ) {
            GiftPickerSheetContent(
                coinBalance = currentUserCoins,
                onSelectGift = { gift ->
                    viewModel.sendGift(
                        senderName = currentUserDisplayName,
                        receiverName = room.hostName,
                        gift = gift,
                        currentCoins = currentUserCoins,
                        currentXp = currentUserXp
                    )
                    showGiftSheet = false
                }
            )
        }
    }

    if (showReportDialog && room != null) {
        ReportDialog(
            targetLabel = room.title,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                viewModel.submitReport(reason)
                showReportDialog = false
            }
        )
    }
}

@Composable
private fun LiveBroadcastStageCard(
    hostName: String,
    streamState: com.example.streaming.StreamSessionState,
    isHost: Boolean,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleBeauty: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, PartyGoldDim, RoundedCornerShape(20.dp))
    ) {
        if (streamState.cameraEnabled) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_party_1795004938799),
                contentDescription = "Live video stream stage",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MidnightPurple),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VideocamOff, contentDescription = null, tint = MutedLavender, modifier = Modifier.size(36.dp))
                    Text("Camera Paused", style = MaterialTheme.typography.labelMedium, color = MutedLavender)
                }
            }
        }

        // Telemetry overlay top-left
        Surface(
            color = MidnightPurple.copy(alpha = 0.75f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
        ) {
            Text(
                text = "${streamState.providerLabel.take(18)} • ${streamState.bitrateKbps} kbps • ${streamState.beautyFilter.label}",
                style = MaterialTheme.typography.labelSmall,
                color = PartyGold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Host broadcast controls bottom bar
        if (isHost) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MidnightPurple.copy(alpha = 0.72f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onToggleCamera) {
                    Icon(
                        imageVector = if (streamState.cameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        contentDescription = "Camera",
                        tint = if (streamState.cameraEnabled) LiveEmerald else LiveCoral
                    )
                }
                IconButton(onClick = onToggleMic) {
                    Icon(
                        imageVector = if (streamState.micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = "Mic",
                        tint = if (streamState.micEnabled) LiveEmerald else LiveCoral
                    )
                }
                IconButton(onClick = onSwitchCamera) {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
                }
                IconButton(onClick = onToggleBeauty) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Beauty filters", tint = PartyGold)
                }
            }
        } else {
            Surface(
                color = MidnightPurple.copy(alpha = 0.75f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            ) {
                Text(
                    text = "Watching $hostName",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeakerSeatItem(
    seatIndex: Int,
    seat: RoomSeat?,
    isHostSeat: Boolean,
    isMine: Boolean,
    canInteract: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = canInteract) { onClick() }
            .padding(vertical = 4.dp)
            .testTag("speaker_seat_$seatIndex")
    ) {
        Box(contentAlignment = Alignment.Center) {
            val borderColor = when {
                seat != null && !seat.muted -> PartyGold
                seat != null && seat.muted -> MutedLavender
                else -> PartyGoldDim
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (seat != null) Brush.linearGradient(listOf(CardPlumTop, ElevatedPlum))
                        else Brush.linearGradient(listOf(MidnightPurple, MidnightPurple))
                    )
                    .border(if (seat != null) 2.dp else 1.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (seat != null) {
                    val initials = seat.occupantName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("").ifEmpty { "S" }
                    Text(initials, style = MaterialTheme.typography.titleMedium, color = PartyGold, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "Take seat ${seatIndex + 1}", tint = MutedLavender, modifier = Modifier.size(20.dp))
                }
            }

            if (seat != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (seat.muted) LiveCoral else LiveEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (seat.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (seat.cameraOn) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(PartyGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Camera On", tint = DeepObsidianText, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when {
                seat != null && isMine -> "You"
                seat != null -> seat.occupantName
                isHostSeat -> "Host"
                else -> "Seat ${seatIndex + 1}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (seat != null) Color.White else MutedLavender,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatMessageBubble(message: RoomMessage) {
    when (message.messageType) {
        "gift" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(PartyGold.copy(alpha = 0.24f), LiveCoral.copy(alpha = 0.2f))))
                    .border(1.dp, PartyGold, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = PartyGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${message.senderName} ${message.text}", style = MaterialTheme.typography.labelLarge, color = PartyGold)
            }
        }
        "system" -> {
            Surface(color = ElevatedPlum.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(message.text, style = MaterialTheme.typography.bodySmall, color = SoftLavender, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        else -> {
            Surface(color = RoyalPlumSurface.copy(alpha = 0.85f), shape = RoundedCornerShape(14.dp)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Text("${message.senderName}: ", style = MaterialTheme.typography.labelMedium, color = PartyGold, fontWeight = FontWeight.Bold)
                    Text(message.text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun GiftPickerSheetContent(
    coinBalance: Long,
    onSelectGift: (VirtualGift) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(com.example.data.GiftCategory.HOT) }
    val giftsInTab = remember(selectedCategory) { GiftCatalog.byCategory(selectedCategory) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 18.dp)
    ) {
        // Header + Coin Balance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Luxury Gift Lounge", style = MaterialTheme.typography.titleLarge, color = com.example.ui.components.LuxuryRoyalGold, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = if (selectedCategory == com.example.data.GiftCategory.LUCKY) "Lucky Gifts: Win 50x or 100x coins back!" else "100 to 1B Coins • Every gift has its own animation",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedCategory == com.example.data.GiftCategory.LUCKY) com.example.ui.components.LuxuryEmeraldGreen else SoftLavender
                )
            }
            Surface(
                color = com.example.ui.components.LuxuryObsidianBlack,
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, com.example.ui.components.LuxuryRoyalGold, RoundedCornerShape(50))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = com.example.ui.components.LuxuryRoyalGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(com.example.data.formatCompactCoins(coinBalance), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Tabs: Hot | Lucky | VIP | SVIP | Newcomer | CP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.example.data.GiftCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = if (cat == com.example.data.GiftCategory.LUCKY) "Lucky 100x" else cat.label,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (cat == com.example.data.GiftCategory.LUCKY) com.example.ui.components.LuxuryEmeraldGreen else com.example.ui.components.LuxuryRoyalGold,
                        selectedLabelColor = com.example.ui.components.LuxuryObsidianBlack,
                        containerColor = com.example.ui.components.LuxuryObsidianBlack,
                        labelColor = SoftLavender
                    ),
                    modifier = Modifier.testTag("gift_tab_${cat.key}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Scrollable Gift Cards Row (Side-by-Side)
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(giftsInTab, key = { it.id }) { gift ->
                val canAfford = coinBalance >= gift.coins
                Column(
                    modifier = Modifier
                        .width(122.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    com.example.ui.components.LuxuryObsidianBlack,
                                    if (gift.isLucky) com.example.ui.components.LuxuryDarkEmerald.copy(alpha = 0.65f) else CardPlumTop
                                )
                            )
                        )
                        .border(
                            width = if (canAfford) 1.4.dp else 1.dp,
                            color = when {
                                !canAfford -> MutedLavender.copy(alpha = 0.25f)
                                gift.isLucky -> com.example.ui.components.LuxuryEmeraldGreen
                                else -> com.example.ui.components.LuxuryRoyalGold
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(enabled = canAfford) { onSelectGift(gift) }
                        .padding(10.dp)
                        .testTag("gift_item_${gift.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = if (gift.isLucky) com.example.ui.components.LuxuryEmeraldGreen else com.example.ui.components.LuxuryRoyalGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = gift.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (gift.isLucky) com.example.ui.components.LuxuryObsidianBlack else com.example.ui.components.LuxuryRoyalGold,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        (if (gift.isLucky) com.example.ui.components.LuxuryEmeraldGreen else com.example.ui.components.LuxuryRoyalGold).copy(alpha = 0.3f),
                                        com.example.ui.components.LuxuryObsidianBlack
                                    )
                                )
                            )
                            .border(1.dp, if (canAfford) com.example.ui.components.LuxuryRoyalGold else MutedLavender, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = gift.name,
                            tint = if (gift.isLucky) com.example.ui.components.LuxuryEmeraldGreen else if (canAfford) com.example.ui.components.LuxuryRoyalGold else MutedLavender,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = gift.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (canAfford) Color.White else MutedLavender,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${com.example.data.formatCompactCoins(gift.coins)} Coins",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (canAfford) com.example.ui.components.LuxuryRoyalGold else MutedLavender,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (canAfford) (if (gift.isLucky) com.example.ui.components.LuxuryEmeraldGreen else com.example.ui.components.LuxuryRoyalGold) else ElevatedPlum,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (canAfford) "Send" else "Low Coins",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canAfford) com.example.ui.components.LuxuryObsidianBlack else MutedLavender,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
