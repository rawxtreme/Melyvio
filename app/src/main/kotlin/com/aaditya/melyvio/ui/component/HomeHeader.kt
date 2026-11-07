/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.PI
import kotlinx.coroutines.delay
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.LocalNavController
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.GreetingNameKey
import com.aaditya.melyvio.ui.utils.greetingAnnotated
import com.aaditya.melyvio.ui.utils.rememberDayPart
import com.aaditya.melyvio.utils.rememberPreference

/** "Home" + "Good evening, Name" (tap the greeting to change the name). */
@Composable
fun HomeTitle(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    val (name) = rememberPreference(GreetingNameKey, "")
    val part = rememberDayPart()
    Column(
        modifier =
            modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { navController.navigate("greeting") },
    ) {
        MelyvioTitle()
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = name,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }) togetherWith fadeOut(tween(150))
                },
                label = "greeting",
            ) { shown ->
                Text(
                    text = greetingAnnotated(shown, part),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            MiniDnaButton(onClick = { navController.navigate("music_dna") })
        }
    }
}

private enum class TitleAnim(val durationMs: Int) {
    EMERGE(1400), // letters slide out from behind the logo
    WAVE(1300),
    POP(1300),
    FLIP(1400),
    SHIMMER(1500),
    DROP(1500),
    SPIN(1400), // logo spins in 3D
    TYPE(1200),
    RISE(1300),
}

private fun easeOutCubic(x: Float): Float {
    val t = 1f - x
    return 1f - t * t * t
}

private fun easeInOut(x: Float): Float = x * x * (3f - 2f * x)

private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val t = x - 1f
    return 1f + c3 * t * t * t + c1 * t * t
}

private fun easeOutBounce(x: Float): Float {
    val n1 = 7.5625f
    val d1 = 2.75f
    return when {
        x < 1f / d1 -> n1 * x * x
        x < 2f / d1 -> {
            val t = x - 1.5f / d1
            n1 * t * t + 0.75f
        }
        x < 2.5f / d1 -> {
            val t = x - 2.25f / d1
            n1 * t * t + 0.9375f
        }
        else -> {
            val t = x - 2.625f / d1
            n1 * t * t + 0.984375f
        }
    }
}

/**
 * "Melyvio" with the app logo. Every 5 seconds it plays a different animation (letters coming out
 * of the logo, wave, pop, flip, shimmer, drop, 3D logo spin, typing, rise) and then rests.
 */
@Composable
private fun MelyvioTitle() {
    val letters = remember { "Melyvio".toList() }
    val count = letters.size
    val progress = remember { Animatable(1f) }
    var anim by remember { mutableStateOf(TitleAnim.EMERGE) }
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(Unit) {
        // first thing the user sees: the text comes out of the logo
        anim = TitleAnim.EMERGE
        progress.snapTo(0f)
        progress.animateTo(1f, tween(TitleAnim.EMERGE.durationMs, easing = FastOutSlowInEasing))
        var last = TitleAnim.EMERGE.ordinal
        while (true) {
            delay(5000)
            val all = TitleAnim.values()
            var next = Random.nextInt(all.size)
            while (next == last) next = Random.nextInt(all.size)
            last = next
            anim = all[next]
            progress.snapTo(0f)
            progress.animateTo(1f, tween(all[next].durationMs, easing = LinearEasing))
        }
    }

    // shimmer needs the colour while composing; every other animation runs in the draw phase only
    val shimmerPos = if (anim == TitleAnim.SHIMMER) progress.value * (count + 2) - 1f else -10f

    Row(verticalAlignment = Alignment.CenterVertically) {
        MelyvioMark(
            size = 34.dp,
            tint = primary,
            tile = primary.copy(alpha = 0.14f),
            modifier =
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        val p = progress.value
                        cameraDistance = 14f * density
                        when (anim) {
                            TitleAnim.EMERGE -> {
                                rotationZ = 360f * easeOutCubic(p)
                                val s = 1f + 0.18f * sin(p * PI.toFloat())
                                scaleX = s
                                scaleY = s
                            }
                            TitleAnim.SPIN -> rotationY = 720f * easeInOut(p)
                            else -> {
                                val s = 1f + 0.10f * sin(p * PI.toFloat())
                                scaleX = s
                                scaleY = s
                            }
                        }
                    },
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.clipToBounds().padding(vertical = 8.dp)) {
            Row(
                modifier =
                    Modifier.graphicsLayer {
                        if (anim == TitleAnim.EMERGE) {
                            translationX = -size.width * (1f - easeOutCubic(progress.value))
                        }
                    },
            ) {
                letters.forEachIndexed { index, ch ->
                    val glow = (1f - abs(index - shimmerPos) / 1.6f).coerceIn(0f, 1f)
                    Text(
                        text = ch.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = lerp(onSurface, primary, glow),
                        maxLines = 1,
                        modifier =
                            Modifier.graphicsLayer {
                                val p = progress.value
                                val lp = ((p * (count + 3) - index) / 3f).coerceIn(0f, 1f)
                                cameraDistance = 12f * density
                                when (anim) {
                                    TitleAnim.WAVE -> translationY = -sin(lp * PI.toFloat()) * 10.dp.toPx()
                                    TitleAnim.POP -> {
                                        val sc = easeOutBack(lp)
                                        scaleX = sc
                                        scaleY = sc
                                    }
                                    TitleAnim.FLIP -> {
                                        rotationX = (1f - easeOutCubic(lp)) * 90f
                                        alpha = lp
                                    }
                                    TitleAnim.DROP -> {
                                        translationY = -(1f - easeOutBounce(lp)) * 26.dp.toPx()
                                        alpha = min(1f, lp * 4f)
                                    }
                                    TitleAnim.TYPE -> {
                                        alpha = if (lp > 0f) 1f else 0f
                                        translationX = (1f - lp) * 6.dp.toPx()
                                    }
                                    TitleAnim.RISE -> {
                                        translationY = (1f - easeOutCubic(lp)) * 20.dp.toPx()
                                        alpha = lp
                                    }
                                    TitleAnim.SPIN -> {
                                        val sc = 1f + 0.08f * sin(p * PI.toFloat())
                                        scaleX = sc
                                        scaleY = sc
                                    }
                                    else -> {}
                                }
                            },
                    )
                }
            }
        }
    }
}

/** Round, softly outlined top-bar action button (history / stats / profile). */
@Composable
fun RoundTopBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .padding(end = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
