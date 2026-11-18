package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PartyCategory
import com.example.data.PartyRoom
import com.example.ui.theme.CardPlumBottom
import com.example.ui.theme.CardPlumTop
import com.example.ui.theme.DeepObsidianText
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.LiveCoral
import com.example.ui.theme.MidnightPurple
import com.example.ui.theme.MutedLavender
import com.example.ui.theme.PartyGold
import com.example.ui.theme.PartyGoldDim
import com.example.ui.theme.RoyalPlumSurface
import com.example.ui.theme.SoftLavender

@Composable
fun CategoryTabStrip(
    selected: PartyCategory,
    onSelect: (PartyCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RoyalPlumSurface)
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PartyCategory.entries.forEach { category ->
            val isSelected = category == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(category) }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("category_tab_${category.key}")
            ) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PartyGold else MutedLavender
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 24.dp else 0.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) PartyGold else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun FeaturedHeroCard(
    title: String,
    subtitle: String,
    liveCount: Int,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, PartyGoldDim, RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_party_1795004938799),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MidnightPurple.copy(alpha = 0.90f),
                            MidnightPurple.copy(alpha = 0.50f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = LiveCoral, shape = RoundedCornerShape(50)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedEqualizerBars(color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ANU LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "$liveCount active",
                    style = MaterialTheme.typography.labelMedium,
                    color = PartyGold
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftLavender,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onCtaClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PartyGold,
                        contentColor = DeepObsidianText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("hero_cta_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ctaLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PartyRoomCard(
    room: PartyRoom,
    isBookmarked: Boolean,
    onRoomClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverRes = when (room.coverTheme) {
        "music" -> R.drawable.img_room_music_1795004937885
        "hero" -> R.drawable.img_hero_party_1795004938799
        else -> R.drawable.img_room_lounge_1795004937709
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors = listOf(CardPlumTop, CardPlumBottom)))
            .border(1.dp, PartyGoldDim, RoundedCornerShape(16.dp))
            .clickable { onRoomClick() }
            .testTag("room_card_${room.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
        ) {
            Image(
                painter = painterResource(id = coverRes),
                contentDescription = room.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MidnightPurple.copy(alpha = 0.82f))
                        )
                    )
            )
            Surface(
                color = if (room.roomMode == "live") LiveCoral.copy(alpha = 0.88f) else MidnightPurple.copy(alpha = 0.78f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedEqualizerBars(color = if (room.roomMode == "live") Color.White else PartyGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (room.roomMode == "live") "LIVE" else "${room.seatCapacity} SEATS",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (room.roomMode == "live") Color.White else PartyGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .testTag("bookmark_button_${room.id}")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) PartyGold else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${room.listenerCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (room.giftScore > 0L) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = PartyGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${room.giftScore}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PartyGold
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = room.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${room.hostName} • ${room.shortCode}",
                style = MaterialTheme.typography.bodySmall,
                color = SoftLavender,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnimatedEqualizerBars(
    color: Color = PartyGold,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val h1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val h2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(540, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    Row(
        modifier = modifier.height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(Modifier.width(2.5.dp).height((12 * h1).dp).clip(RoundedCornerShape(50)).background(color))
        Box(Modifier.width(2.5.dp).height((12 * h2).dp).clip(RoundedCornerShape(50)).background(color))
        Box(Modifier.width(2.5.dp).height((12 * h3).dp).clip(RoundedCornerShape(50)).background(color))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRoomDialog(
    initialMode: String = "party",
    onDismiss: () -> Unit,
    onCreate: (title: String, topic: String, category: PartyCategory, roomMode: String, seatCapacity: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PartyCategory.MUSIC) }
    var roomMode by remember { mutableStateOf(initialMode) }
    var seatCapacity by remember { mutableIntStateOf(8) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoyalPlumSurface,
        titleContentColor = PartyGold,
        textContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PartyGold.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (roomMode == "live") Icons.Filled.Videocam else Icons.Filled.Groups,
                        contentDescription = null,
                        tint = PartyGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (roomMode == "live") "Start Live Stream" else "Create Party Room",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = roomMode == "live",
                        onClick = { roomMode = "live" },
                        label = { Text("Video Live") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LiveCoral,
                            selectedLabelColor = Color.White,
                            containerColor = ElevatedPlum,
                            labelColor = SoftLavender
                        )
                    )
                    FilterChip(
                        selected = roomMode == "party",
                        onClick = { roomMode = "party" },
                        label = { Text("Multi-Seat Party") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PartyGold,
                            selectedLabelColor = DeepObsidianText,
                            containerColor = ElevatedPlum,
                            labelColor = SoftLavender
                        )
                    )
                }

                if (roomMode == "party") {
                    Text(
                        text = "Party Seat Layout",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftLavender
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 6, 8).forEach { count ->
                            FilterChip(
                                selected = seatCapacity == count,
                                onClick = { seatCapacity = count },
                                label = { Text("$count Seats") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PartyGold,
                                    selectedLabelColor = DeepObsidianText,
                                    containerColor = ElevatedPlum,
                                    labelColor = SoftLavender
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(60)
                        validationError = null
                    },
                    label = { Text("Room Title") },
                    placeholder = { Text("e.g., ANU Midnight Stage") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PartyGold,
                        unfocusedBorderColor = PartyGoldDim,
                        focusedLabelColor = PartyGold,
                        cursorColor = PartyGold
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("create_room_title_input")
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it.take(140) },
                    label = { Text("Topic / Welcome Message") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PartyGold,
                        unfocusedBorderColor = PartyGoldDim,
                        focusedLabelColor = PartyGold,
                        cursorColor = PartyGold
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("create_room_topic_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PartyCategory.creatableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PartyGold,
                                selectedLabelColor = DeepObsidianText,
                                containerColor = ElevatedPlum,
                                labelColor = SoftLavender
                            )
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = LiveCoral
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.length < 2) {
                        validationError = "Please enter at least 2 characters."
                    } else {
                        onCreate(trimmed, topic.trim(), selectedCategory, roomMode, seatCapacity)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PartyGold,
                    contentColor = DeepObsidianText
                ),
                modifier = Modifier.testTag("confirm_create_room_button")
            ) {
                Text(
                    text = if (roomMode == "live") "Go Live" else "Open Party",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = SoftLavender)
            }
        }
    )
}

@Composable
fun ReportDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoyalPlumSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Flag, contentDescription = null, tint = LiveCoral)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report $targetLabel", color = Color.White)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Describe the issue so our moderation team can review it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftLavender
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(240) },
                    label = { Text("Reason") },
                    placeholder = { Text("Spam, harassment, inappropriate content...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.trim().length >= 2) {
                        onSubmit(reason.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LiveCoral, contentColor = Color.White)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SoftLavender)
            }
        }
    )
}
