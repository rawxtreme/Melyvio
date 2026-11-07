/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.aaditya.melyvio.R

/** Shared dark-glass button: soft fill, light edge, and a glow that appears when pressed / active. */
@Composable
private fun GlassButtonBase(
    onClick: () -> Unit,
    shape: Shape,
    glowColor: Color,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    label: String?,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val highlight by animateFloatAsState(
        targetValue = if (pressed) 1f else if (active) 0.7f else 0f,
        animationSpec = tween(180),
        label = "glassHighlight",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "glassScale",
    )
    val edge =
        Brush.linearGradient(
            listOf(
                lerp(Color.White.copy(alpha = 0.38f), glowColor, highlight),
                Color.White.copy(alpha = 0.06f),
                lerp(Color.White.copy(alpha = 0.22f), glowColor, highlight * 0.8f),
            ),
        )
    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else 0.4f
                }.shadow(
                    elevation = (14f * highlight).dp,
                    shape = shape,
                    clip = false,
                    ambientColor = glowColor,
                    spotColor = glowColor,
                ).clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f + 0.10f * highlight),
                            Color.White.copy(alpha = 0.03f + 0.07f * highlight),
                        ),
                    ),
                ).background(glowColor.copy(alpha = 0.22f * highlight))
                .border(1.dp, edge, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = label,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    glowColor: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 60.dp,
    enabled: Boolean = true,
    active: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassButtonBase(
        onClick = onClick,
        shape = CircleShape,
        glowColor = glowColor,
        active = active,
        enabled = enabled,
        modifier = modifier.size(diameter),
        label = null,
        content = content,
    )
}

@Composable
fun GlassSquareButton(
    onClick: () -> Unit,
    glowColor: Color,
    modifier: Modifier = Modifier,
    side: Dp = 44.dp,
    active: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassButtonBase(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        glowColor = glowColor,
        active = active,
        enabled = true,
        modifier = modifier.size(side),
        label = null,
        content = content,
    )
}

/**
 * Big play button: glowing glass disc with a bright ring that turns clockwise while the music is
 * playing and stops where it is when paused.
 */
@Composable
fun GlassPlayButton(
    isPlaying: Boolean,
    isEnded: Boolean,
    glowColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 88.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val angle = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                angle.animateTo(angle.value + 360f, tween(6000, easing = LinearEasing))
                angle.snapTo(angle.value % 360f)
            }
        }
    }
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(160),
        label = "playPress",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "playScale",
    )
    Box(
        modifier =
            modifier
                .size(diameter)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.drawBehind {
                    val r = size.minDimension / 2f
                    // soft outer glow
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                listOf(glowColor.copy(alpha = 0.40f + 0.25f * press), Color.Transparent),
                                center = center,
                                radius = r * 1.5f,
                            ),
                        radius = r * 1.5f,
                        center = center,
                    )
                    // glass body
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                listOf(glowColor.copy(alpha = 0.48f + 0.2f * press), Color(0xFF14070A).copy(alpha = 0.88f)),
                                center = Offset(size.width * 0.35f, size.height * 0.3f),
                                radius = r * 1.4f,
                            ),
                        radius = r,
                        center = center,
                    )
                    // ring that turns while playing
                    rotate(degrees = angle.value, pivot = center) {
                        drawCircle(
                            brush =
                                Brush.sweepGradient(
                                    listOf(
                                        glowColor,
                                        Color.White.copy(alpha = 0.95f),
                                        glowColor.copy(alpha = 0.15f),
                                        glowColor,
                                    ),
                                    center = center,
                                ),
                            radius = r - 1.5.dp.toPx(),
                            center = center,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                }.clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter =
                painterResource(
                    when {
                        isEnded -> R.drawable.replay
                        isPlaying -> R.drawable.pause
                        else -> R.drawable.play
                    },
                ),
            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
            tint = contentColor,
            modifier = Modifier.size(diameter * 0.43f),
        )
    }
}

/** previous  -  PLAY  -  next */
@Composable
fun GlassTransportControls(
    isPlaying: Boolean,
    isEnded: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    glowColor: Color,
    contentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircleButton(onClick = onPrevious, glowColor = glowColor, enabled = canSkipPrevious, diameter = 60.dp) {
            Icon(painterResource(R.drawable.skip_previous), contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
        }
        GlassPlayButton(
            isPlaying = isPlaying,
            isEnded = isEnded,
            glowColor = glowColor,
            contentColor = contentColor,
            onClick = onPlayPause,
            modifier = playButtonModifier,
        )
        GlassCircleButton(onClick = onNext, glowColor = glowColor, enabled = canSkipNext, diameter = 60.dp) {
            Icon(painterResource(R.drawable.skip_next), contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
        }
    }
}

/** queue - sleep - shuffle - lyrics - repeat ............ more */
@Composable
fun GlassBottomRow(
    sleepActive: Boolean,
    sleepText: String?,
    shuffleActive: Boolean,
    lyricsActive: Boolean,
    repeatMode: Int,
    glowColor: Color,
    contentColor: Color,
    onQueue: () -> Unit,
    onSleep: () -> Unit,
    onShuffle: () -> Unit,
    onLyrics: () -> Unit,
    onRepeat: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassSquareButton(onClick = onQueue, glowColor = glowColor) {
            Icon(painterResource(R.drawable.queue_music), contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
        GlassSquareButton(onClick = onSleep, glowColor = glowColor, active = sleepActive) {
            if (sleepText != null) {
                Text(text = sleepText, style = MaterialTheme.typography.labelSmall, color = contentColor, maxLines = 1)
            } else {
                Icon(painterResource(R.drawable.bedtime), contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            }
        }
        GlassSquareButton(onClick = onShuffle, glowColor = glowColor, active = shuffleActive) {
            Icon(painterResource(R.drawable.shuffle), contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
        GlassSquareButton(onClick = onLyrics, glowColor = glowColor, active = lyricsActive) {
            Icon(painterResource(R.drawable.lyrics), contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
        GlassSquareButton(onClick = onRepeat, glowColor = glowColor, active = repeatMode != Player.REPEAT_MODE_OFF) {
            Icon(
                painter = painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        GlassCircleButton(onClick = onMore, glowColor = glowColor, diameter = 44.dp) {
            Icon(painterResource(R.drawable.more_vert), contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
    }
}
