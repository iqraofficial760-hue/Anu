package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Conversation
import com.example.data.DirectMessage
import com.example.data.FollowedHost
import com.example.data.MomentComment
import com.example.data.MomentPost
import com.example.data.UiState
import com.example.ui.DirectChatViewModel
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
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    momentsState: UiState<List<MomentPost>>,
    followingState: UiState<List<FollowedHost>>,
    currentUserId: String,
    onCreateMoment: (caption: String, mediaType: String, coverTheme: String) -> Unit,
    onLikeMoment: (String) -> Unit,
    onToggleFollow: (String, String, Boolean) -> Unit,
    observeComments: (String) -> Flow<List<MomentComment>>,
    onAddComment: (momentId: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val moments = (momentsState as? UiState.Success)?.data ?: emptyList()
    val followedIds = remember(followingState) {
        ((followingState as? UiState.Success)?.data ?: emptyList()).map { it.targetUserId }.toSet()
    }
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeCommentMoment by remember { mutableStateOf<MomentPost?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MidnightPurple),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ANU Moments", style = MaterialTheme.typography.titleLarge, color = PartyGold)
                    Text("Short video clips & backstage photos", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("post_moment_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (moments.isEmpty()) {
            item {
                Surface(
                    color = RoyalPlumSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, PartyGoldDim, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No Moments posted yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Share your first photo or short video clip with the ANU LIVE community!", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                    }
                }
            }
        } else {
            items(moments, key = { it.id }) { post ->
                val isFollowing = post.authorId in followedIds
                val imgRes = when (post.coverTheme) {
                    "music" -> R.drawable.img_room_music_1795004937885
                    "lounge" -> R.drawable.img_room_lounge_1795004937709
                    else -> R.drawable.img_hero_party_1795004938799
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(CardPlumTop, CardPlumBottom)))
                        .border(1.dp, PartyGoldDim, RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(PartyGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.authorName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepObsidianText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(post.authorName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(if (post.mediaType == "video") "Short Video" else "Photo Moment", style = MaterialTheme.typography.labelSmall, color = SoftLavender)
                        }
                        if (post.authorId != currentUserId) {
                            TextButton(
                                onClick = { onToggleFollow(post.authorId, post.authorName, isFollowing) }
                            ) {
                                Text(if (isFollowing) "Following" else "+ Follow", color = if (isFollowing) LiveEmerald else PartyGold)
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                        Image(
                            painter = painterResource(id = imgRes),
                            contentDescription = post.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (post.mediaType == "video") {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(54.dp).align(Alignment.Center)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(post.caption, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onLikeMoment(post.id) }
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Like", tint = LiveCoral, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activeCommentMoment = post }
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Comment", tint = PartyGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${post.commentsCount}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Check out ${post.authorName}'s moment on ANU LIVE: ${post.caption}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Moment"))
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = SoftLavender, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var caption by remember { mutableStateOf("") }
        var mediaType by remember { mutableStateOf("photo") }
        var theme by remember { mutableStateOf("hero") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = RoyalPlumSurface,
            title = { Text("Share a Moment", color = PartyGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mediaType == "photo", onClick = { mediaType = "photo" }, label = { Text("Photo") })
                        FilterChip(selected = mediaType == "video", onClick = { mediaType = "video" }, label = { Text("Short Video") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("hero" to "Stage", "music" to "Concert", "lounge" to "Lounge").forEach { (k, l) ->
                            FilterChip(selected = theme == k, onClick = { theme = k }, label = { Text(l) })
                        }
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(300) },
                        label = { Text("Caption") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (caption.isNotBlank()) {
                            onCreateMoment(caption, mediaType, theme)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText)
                ) { Text("Publish", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = SoftLavender) }
            }
        )
    }

    val commentingPost = activeCommentMoment
    if (commentingPost != null) {
        val comments by observeComments(commentingPost.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var commentText by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { activeCommentMoment = null },
            containerColor = RoyalPlumSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium, color = PartyGold)
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments, key = { it.id }) { c ->
                        Surface(color = ElevatedPlum, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(c.authorName, style = MaterialTheme.typography.labelMedium, color = PartyGold)
                                Text(c.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it.take(240) },
                        placeholder = { Text("Write a comment...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentingPost.id, commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PartyGold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun MessagesScreen(
    conversationsState: UiState<List<Conversation>>,
    currentUserId: String,
    onConversationClick: (Conversation) -> Unit,
    onStartConversation: (peerName: String, initialMessage: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()
    var showNewChatDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MidnightPurple),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Messages", style = MaterialTheme.typography.titleLarge, color = PartyGold)
                    Text("Private 1-to-1 chats", style = MaterialTheme.typography.bodySmall, color = SoftLavender)
                }
                Button(
                    onClick = { showNewChatDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Chat", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (conversations.isEmpty()) {
            item {
                Surface(
                    color = RoyalPlumSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, PartyGoldDim, RoundedCornerShape(18.dp))
                ) {
                    Text(
                        text = "No conversations yet. Tap New Chat to message a host or friend!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftLavender,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(conversations, key = { it.id }) { conv ->
                val peerName = conv.participantNames.lastOrNull() ?: "Friend"
                val timeStr = conv.updatedAt?.toDate()?.let {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                } ?: "Now"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RoyalPlumSurface)
                        .border(1.dp, PartyGoldDim, RoundedCornerShape(16.dp))
                        .clickable { onConversationClick(conv) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Box(
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(CardPlumTop),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(peerName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = PartyGold, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (conv.peerOnline) LiveEmerald else MutedLavender)
                                .border(2.dp, RoyalPlumSurface, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(peerName, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(conv.lastMessage, style = MaterialTheme.typography.bodySmall, color = SoftLavender, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MutedLavender)
                        if (conv.unreadCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(color = LiveCoral, shape = CircleShape) {
                                Text(
                                    text = "${conv.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        var peerName by remember { mutableStateOf("") }
        var firstMsg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            containerColor = RoyalPlumSurface,
            title = { Text("Start 1-to-1 Chat", color = PartyGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = peerName,
                        onValueChange = { peerName = it.take(40) },
                        label = { Text("Recipient Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = firstMsg,
                        onValueChange = { firstMsg = it.take(280) },
                        label = { Text("Message") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (peerName.isNotBlank()) {
                            onStartConversation(peerName, firstMsg.ifBlank { "Hi there!" })
                            showNewChatDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PartyGold, contentColor = DeepObsidianText)
                ) { Text("Send", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("Cancel", color = SoftLavender) }
            }
        )
    }
}

@Composable
fun DirectChatScreen(
    viewModel: DirectChatViewModel,
    peerName: String,
    participantIds: List<String>,
    currentUserName: String,
    onBack: () -> Unit
) {
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightPurple)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(RoyalPlumSurface).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column {
                Text(peerName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Online", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == viewModel.currentUserId
                val ts = msg.createdAt?.toDate() ?: Date()
                val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isMine) PartyGold else RoyalPlumSurface)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = if (isMine) DeepObsidianText else Color.White)
                        Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = if (isMine) DeepObsidianText.copy(alpha = 0.7f) else MutedLavender)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(RoyalPlumSurface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(500) },
                placeholder = { Text("Type a message...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PartyGold,
                    unfocusedBorderColor = PartyGoldDim,
                    focusedContainerColor = MidnightPurple,
                    unfocusedContainerColor = MidnightPurple
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.send(participantIds, currentUserName, input)
                        input = ""
                    }
                },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(PartyGold)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = DeepObsidianText)
            }
        }
    }
}
