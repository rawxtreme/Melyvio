/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaditya.melyvio.R

private class PendingChip<E>(val value: E?)

private fun iconFor(title: String): Int {
    val t = title.lowercase()
    return when {
        "podcast" in t -> R.drawable.mic
        "romance" in t || "love" in t -> R.drawable.favorite
        "feel good" in t || "happy" in t -> R.drawable.ic_mono_sun
        "workout" in t || "gym" in t || "fitness" in t -> R.drawable.ic_mono_dumbbell
        "energ" in t -> R.drawable.ic_mono_bolt
        "relax" in t || "chill" in t -> R.drawable.ic_mono_leaf
        "focus" in t || "study" in t -> R.drawable.ic_mono_target
        "commute" in t || "drive" in t || "travel" in t -> R.drawable.ic_mono_car
        "party" in t || "dance" in t -> R.drawable.graphic_eq
        "sad" in t || "melanchol" in t -> R.drawable.ic_mono_drop
        "sleep" in t || "night" in t -> R.drawable.bedtime
        else -> R.drawable.music_note
    }
}

/**
 * Home chips: "All" + the feed's mood chips. Selected / pressed chips get a golden (theme primary)
 * outline and glow, so it follows whatever theme colour the user picked.
 */
@Composable
fun <E> HomeChipsRow(
    chips: List<Pair<E, String>>,
    selected: E?,
    onChipClick: (E) -> Unit,
    onAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Show the tapped chip as selected immediately; the feed (and the real selection) follows
    // when the network load finishes. Reset as soon as the real selection changes.
    var pending by remember(selected) { mutableStateOf<PendingChip<E>?>(null) }
    LaunchedEffect(pending) {
        if (pending != null) {
            delay(10_000)
            pending = null
        }
    }
    val shown: E? = pending.let { if (it != null) it.value else selected }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
    ) {
        Spacer(Modifier.width(16.dp))
        GlowChip(
            label = stringResource(R.string.home_chip_all),
            selected = shown == null,
            onClick = {
                pending = PendingChip(null)
                onAllClick()
            },
            leading = { tint -> FourDots(tint) },
        )
        chips.forEach { (value, label) ->
            Spacer(Modifier.width(10.dp))
            GlowChip(
                label = label,
                selected = shown == value,
                onClick = {
                    pending = PendingChip(if (shown == value) null else value)
                    onChipClick(value)
                },
                leading = { tint ->
                    Icon(
                        painter = painterResource(iconFor(label)),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun FourDots(color: Color) {
    Box(
        Modifier.size(18.dp).drawBehind {
            val r = size.minDimension * 0.2f
            val a = size.minDimension * 0.27f
            val b = size.minDimension * 0.73f
            drawCircle(color, r, Offset(a, a))
            drawCircle(color, r, Offset(b, a))
            drawCircle(color, r, Offset(a, b))
            drawCircle(color, r, Offset(b, b))
        },
    )
}

@Composable
private fun GlowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable (Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val glow by animateFloatAsState(
        targetValue = if (selected || pressed) 1f else 0f,
        animationSpec = tween(300),
        label = "chipGlow",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "chipScale",
    )
    val shape = RoundedCornerShape(50)
    val borderColor = lerp(colors.onSurface.copy(alpha = 0.12f), primary, glow)
    Row(
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.shadow(
                    elevation = (10f * glow).dp,
                    shape = shape,
                    clip = false,
                    ambientColor = primary,
                    spotColor = primary,
                ).clip(shape)
                .background(colors.surfaceContainerHigh)
                .background(primary.copy(alpha = 0.16f * glow))
                .border((1f + 0.8f * glow).dp, borderColor, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading(lerp(colors.onSurface.copy(alpha = 0.78f), primary, glow))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = colors.onSurface,
            maxLines = 1,
        )
    }
}
