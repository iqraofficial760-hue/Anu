package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.R
import com.example.data.AvatarCatalog
import com.example.data.GiftAnimationStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LuxuryObsidianBlack = Color(0xFF090B0A)
val LuxuryRoyalGold = Color(0xFFFFD700)
val LuxuryDeepGold = Color(0xFFB8860B)
val LuxuryEmeraldGreen = Color(0xFF00E676)
val LuxuryDarkEmerald = Color(0xFF00693E)

data class ActiveGiftEffect(
    val title: String,
    val subtitle: String,
    val style: GiftAnimationStyle,
    val multiplierBadge: String? = null
)

/**
 * Animated Gold + Obsidian Black + Emerald Green shimmering badge pill
 * for User ID (ID: 1), VIP/SVIP tiers, and Level indicators.
 */
@Composable
fun LuxuryBadgePill(
    text: String,
    icon: ImageVector? = null,
    emeraldAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "luxury_badge_shimmer")
    val shimmerX by infinite.animateFloat(
        initialValue = -120f,
        targetValue = 320f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(LuxuryRoyalGold, LuxuryEmeraldGreen, LuxuryRoyalGold),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 140f, 60f)
    )

    Surface(
        color = LuxuryObsidianBlack,
        shape = RoundedCornerShape(50),
        modifier = modifier.border(1.2.dp, borderBrush, RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LuxuryObsidianBlack,
                            if (emeraldAccent) LuxuryDarkEmerald.copy(alpha = 0.55f) else LuxuryDeepGold.copy(alpha = 0.35f),
                            LuxuryObsidianBlack
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (emeraldAccent) LuxuryEmeraldGreen else LuxuryRoyalGold,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (emeraldAccent) LuxuryEmeraldGreen else LuxuryRoyalGold,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * Luxury Gold + Obsidian Black + Emerald Green animated Avatar Frame.
 * Supports custom user-picked photos AND animated GIFs via Coil ImageDecoder/GifDecoder,
 * or curated Royal Lion Emblem & monogram presets.
 */
@Composable
fun LuxuryLionAvatar(
    displayName: String,
    avatarId: String = "royal_lion",
    customAvatarUri: String = "",
    vipBadge: String = "",
    size: Dp = 88.dp,
    voiceAmplitude: Float = 0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val infinite = rememberInfiniteTransition(label = "luxury_avatar_ring")
    val ringRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val preset = AvatarCatalog.byId(avatarId)
    val initials = displayName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifEmpty { "AN" }

    Box(
        modifier = modifier.size(size + 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) - 4.dp.toPx()

            if (voiceAmplitude > 0.05f) {
                drawCircle(
                    color = LuxuryEmeraldGreen.copy(alpha = (voiceAmplitude * 0.65f).coerceIn(0.15f, 0.7f)),
                    radius = baseRadius + (voiceAmplitude * 8.dp.toPx()),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            drawCircle(
                color = LuxuryObsidianBlack,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )

            rotate(degrees = ringRotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            LuxuryRoyalGold,
                            LuxuryObsidianBlack,
                            LuxuryEmeraldGreen,
                            LuxuryDeepGold,
                            LuxuryRoyalGold
                        ),
                        center = center
                    ),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                for (i in 0 until 4) {
                    val angleRad = (i * 90.0) * (PI / 180.0)
                    val dotX = center.x + (baseRadius * cos(angleRad)).toFloat()
                    val dotY = center.y + (baseRadius * sin(angleRad)).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) LuxuryRoyalGold else LuxuryEmeraldGreen,
                        radius = (2.8.dp.toPx() * glowPulse),
                        center = Offset(dotX, dotY)
                    )
                }
            }
        }

        // Inner Avatar Core (Supports Animated GIF / Photo URI or Luxury Preset)
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = when (preset.id) {
                            "emerald_crown" -> listOf(LuxuryDarkEmerald, LuxuryObsidianBlack)
                            "gold_obsidian" -> listOf(LuxuryDeepGold, LuxuryObsidianBlack)
                            "star_host" -> listOf(Color(0xFF3E185C), LuxuryObsidianBlack)
                            "velvet_mic" -> listOf(Color(0xFF1A3A2A), LuxuryObsidianBlack)
                            else -> listOf(Color(0xFF1A1610), LuxuryObsidianBlack)
                        }
                    )
                )
                .border(1.5.dp, LuxuryRoyalGold.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (customAvatarUri.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(customAvatarUri)
                        .crossfade(true)
                        .build(),
                    imageLoader = gifImageLoader,
                    contentDescription = "User Profile Photo or Animated GIF",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (preset.isLionEmblem) {
                Image(
                    painter = painterResource(id = R.drawable.royal_lion_emblem_1795013969406),
                    contentDescription = "Royal Gold Black Green Lion Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = LuxuryRoyalGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = preset.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = LuxuryEmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (vipBadge.isNotBlank()) {
            Surface(
                color = LuxuryObsidianBlack,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(LuxuryRoyalGold, LuxuryEmeraldGreen)),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = LuxuryEmeraldGreen,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = vipBadge.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = LuxuryRoyalGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Full-screen Luxury Gold + Black + Green Royal Lion Animation Showcase.
 */
@Composable
fun LuxuryLionShowcaseOverlay(
    visible: Boolean,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    GiftAnimationOverlay(
        effect = if (visible) ActiveGiftEffect(title, subtitle, GiftAnimationStyle.LION_SOVEREIGN) else null,
        onDismiss = onDismiss
    )
}

/**
 * Per-Gift Custom Animated Overlay supporting Lion Sovereign, Lucky 50x/100x Jackpot,
 * Fireworks Burst, CP Hearts, Royal Crown, and Emerald Comet styles.
 */
@Composable
fun GiftAnimationOverlay(
    effect: ActiveGiftEffect?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = effect != null,
        enter = fadeIn(tween(280)) + scaleIn(initialScale = 0.78f, animationSpec = tween(340)),
        exit = fadeOut(tween(240)) + scaleOut(targetScale = 0.92f, animationSpec = tween(240)),
        modifier = Modifier.fillMaxSize()
    ) {
        val current = effect ?: return@AnimatedVisibility
        val infinite = rememberInfiniteTransition(label = "gift_effect_anim")
        val rot by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(7200, easing = LinearEasing), RepeatMode.Restart),
            label = "rot"
        )
        val wave by infinite.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(1450, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "wave"
        )
        val pulse by infinite.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )

        val primaryAccent = when (current.style) {
            GiftAnimationStyle.CP_HEARTS -> Color(0xFFFF4D8D)
            GiftAnimationStyle.LUCKY_JACKPOT -> LuxuryEmeraldGreen
            GiftAnimationStyle.EMERALD_COMET -> LuxuryEmeraldGreen
            else -> LuxuryRoyalGold
        }
        val secondaryAccent = when (current.style) {
            GiftAnimationStyle.CP_HEARTS -> LuxuryRoyalGold
            GiftAnimationStyle.FIREWORKS_BURST -> Color(0xFFFF6E40)
            else -> LuxuryEmeraldGreen
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LuxuryObsidianBlack.copy(alpha = 0.86f)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f - 28.dp.toPx())
                val maxR = size.minDimension * 0.48f

                // Expanding shockwave rings
                drawCircle(
                    color = primaryAccent.copy(alpha = (1f - wave) * 0.6f),
                    radius = maxR * wave,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = secondaryAccent.copy(alpha = (1f - wave) * 0.45f),
                    radius = maxR * ((wave + 0.38f) % 1f),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Style-specific particle burst & rays
                rotate(degrees = rot, pivot = center) {
                    val rayCount = when (current.style) {
                        GiftAnimationStyle.FIREWORKS_BURST -> 16
                        GiftAnimationStyle.LUCKY_JACKPOT -> 14
                        GiftAnimationStyle.EMERALD_COMET -> 10
                        else -> 12
                    }
                    for (i in 0 until rayCount) {
                        val angle = (i * (360.0 / rayCount)) * (PI / 180.0)
                        val innerFrac = if (current.style == GiftAnimationStyle.EMERALD_COMET) 0.25f else 0.42f
                        val start = Offset(
                            center.x + (maxR * innerFrac * cos(angle)).toFloat(),
                            center.y + (maxR * innerFrac * sin(angle)).toFloat()
                        )
                        val end = Offset(
                            center.x + (maxR * 0.96f * cos(angle)).toFloat(),
                            center.y + (maxR * 0.96f * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = if (i % 2 == 0) primaryAccent.copy(alpha = 0.52f)
                            else secondaryAccent.copy(alpha = 0.46f),
                            start = start,
                            end = end,
                            strokeWidth = if (current.style == GiftAnimationStyle.EMERALD_COMET) 4.dp.toPx() else 2.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    for (j in 0 until 18) {
                        val angle = (j * 20.0) * (PI / 180.0)
                        val dist = maxR * (0.5f + (j % 3) * 0.16f)
                        val sparkPos = Offset(
                            center.x + (dist * cos(angle)).toFloat(),
                            center.y + (dist * sin(angle)).toFloat()
                        )
                        drawCircle(
                            color = if (j % 2 == 0) primaryAccent else secondaryAccent,
                            radius = if (j % 3 == 0) 4.5.dp.toPx() else 2.8.dp.toPx(),
                            center = sparkPos
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 18.dp)
                    .clip(CircleShape)
                    .background(LuxuryObsidianBlack.copy(alpha = 0.85f))
                    .border(1.dp, LuxuryRoyalGold, CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close animation", tint = LuxuryRoyalGold)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (current.multiplierBadge != null) {
                    Surface(
                        color = LuxuryEmeraldGreen,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .scale(pulse)
                            .border(2.dp, LuxuryRoyalGold, RoundedCornerShape(50))
                    ) {
                        Text(
                            text = current.multiplierBadge,
                            style = MaterialTheme.typography.headlineSmall,
                            color = LuxuryObsidianBlack,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Centerpiece Visual
                Box(
                    modifier = Modifier
                        .size(if (current.style == GiftAnimationStyle.LION_SOVEREIGN) 205.dp else 160.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(primaryAccent.copy(alpha = 0.35f), LuxuryObsidianBlack))
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(listOf(LuxuryRoyalGold, primaryAccent, LuxuryRoyalGold)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (current.style == GiftAnimationStyle.LION_SOVEREIGN) {
                        Image(
                            painter = painterResource(id = R.drawable.royal_lion_emblem_1795013969406),
                            contentDescription = "Luxury Gold Black Green Lion",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val icon = when (current.style) {
                            GiftAnimationStyle.LUCKY_JACKPOT -> Icons.Filled.EmojiEvents
                            GiftAnimationStyle.CP_HEARTS -> Icons.Filled.Favorite
                            GiftAnimationStyle.ROYAL_CROWN -> Icons.Filled.Shield
                            GiftAnimationStyle.EMERALD_COMET -> Icons.Filled.RocketLaunch
                            else -> Icons.Filled.CardGiftcard
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = current.title,
                            tint = primaryAccent,
                            modifier = Modifier.size(76.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    color = LuxuryObsidianBlack,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(LuxuryRoyalGold, primaryAccent, LuxuryRoyalGold)),
                        shape = RoundedCornerShape(20.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = secondaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = current.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = LuxuryRoyalGold,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.Star, contentDescription = null, tint = LuxuryRoyalGold, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = current.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
