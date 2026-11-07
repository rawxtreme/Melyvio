/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.aaditya.melyvio.R

/**
 * The Melyvio logo as a single-colour mark (the one shown in About). It takes the colour of the
 * place it is used in, optionally sitting on a soft rounded tile.
 */
@Composable
fun MelyvioMark(
    modifier: Modifier = Modifier,
    size: Dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    tile: Color? = null,
) {
    if (tile != null) {
        Box(
            modifier =
                modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.28f))
                    .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.about_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(size * 0.72f),
            )
        }
    } else {
        Image(
            painter = painterResource(R.drawable.about_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = modifier.size(size),
        )
    }
}
