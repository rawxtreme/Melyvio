/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** A DNA double helix that keeps turning. Drawn in the draw phase only, so it is very cheap. */
@Composable
fun DnaHelix(
    modifier: Modifier = Modifier,
    strandA: Color = MaterialTheme.colorScheme.primary,
    strandB: Color = MaterialTheme.colorScheme.tertiary,
    rungColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
    periodMs: Int = 3200,
) {
    val transition = rememberInfiniteTransition(label = "dna")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "dnaPhase",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val steps = 26
        val amp = w * 0.36f
        val cx = w / 2f
        val stroke = max(1.6f, w * 0.085f)
        var px1 = 0f
        var px2 = 0f
        var py = 0f
        for (i in 0..steps) {
            val y = h * i / steps
            val a = phase + i * 0.45f
            val x1 = cx + amp * sin(a)
            val x2 = cx - amp * sin(a)
            val depth = cos(a)
            if (i % 2 == 0) {
                drawLine(rungColor, Offset(x1, y), Offset(x2, y), strokeWidth = stroke * 0.55f, cap = StrokeCap.Round)
            }
            if (i > 0) {
                drawLine(strandA.copy(alpha = 0.55f + 0.45f * depth.coerceIn(0f, 1f)), Offset(px1, py), Offset(x1, y), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(strandB.copy(alpha = 0.55f + 0.45f * (-depth).coerceIn(0f, 1f)), Offset(px2, py), Offset(x2, y), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            px1 = x1
            px2 = x2
            py = y
        }
    }
}

/** Small round animated DNA logo that opens Music DNA (shown next to the home greeting). */
@Composable
fun MiniDnaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 30.dp,
) {
    Box(
        modifier =
            modifier
                .size(diameter)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        DnaHelix(Modifier.size(width = diameter * 0.46f, height = diameter * 0.74f))
    }
}
