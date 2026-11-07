/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Glass levels shared by the bottom bar and the floating buttons. 0 = transparent, 1 = soft, 2 = frosted. */
object Glass {
    fun containerBrush(
        level: Int,
        base: Color,
    ): Brush =
        when (level.coerceIn(0, 2)) {
            0 -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.02f)))
            1 -> Brush.verticalGradient(listOf(base.copy(alpha = 0.55f), base.copy(alpha = 0.38f)))
            else -> Brush.verticalGradient(listOf(base.copy(alpha = 0.84f), base.copy(alpha = 0.70f)))
        }

    fun borderBrush(level: Int): Brush {
        val a = when (level.coerceIn(0, 2)) {
            0 -> 0.42f
            1 -> 0.36f
            else -> 0.30f
        }
        return Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = a),
                Color.White.copy(alpha = a * 0.18f),
                Color.White.copy(alpha = a * 0.65f),
            ),
        )
    }
}

fun Modifier.glass(
    level: Int,
    base: Color,
    shape: Shape,
): Modifier =
    this
        .clip(shape)
        .background(Glass.containerBrush(level, base))
        .border(1.dp, Glass.borderBrush(level), shape)
