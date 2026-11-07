/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.R

/** Settings-row icon for "Buy me a coffee": same tile as the other rows, with a bouncing cup. */
@Composable
fun BouncingCoffeeLeading() {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "coffeeBounce")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes<Float> {
                        durationMillis = 2200
                        0f at 0
                        -7f at 260
                        0f at 520
                        -3f at 660
                        0f at 800
                        0f at 2200
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "lift",
    )
    val tilt by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes<Float> {
                        durationMillis = 2200
                        0f at 0
                        -9f at 300
                        8f at 480
                        -4f at 650
                        0f at 820
                        0f at 2200
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = "tilt",
    )
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.buymeacoffee),
            contentDescription = null,
            tint = primary.copy(alpha = 0.9f),
            modifier =
                Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        translationY = lift.dp.toPx()
                        rotationZ = tilt
                    },
        )
    }
}
