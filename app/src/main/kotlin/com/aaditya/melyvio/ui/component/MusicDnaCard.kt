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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaditya.melyvio.R

/**
 * Highlighted entry at the top of Settings: glowing, slowly turning border, a turning DNA helix
 * and a "NEW" tag. Colours come from the theme, so it follows whatever theme the user picked.
 */
@Composable
fun MusicDnaFeaturedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val transition = rememberInfiniteTransition(label = "dnaCard")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "dnaAngle",
    )
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(14.dp, shape, ambientColor = primary, spotColor = primary)
                .clip(shape)
                .drawBehind {
                    rotate(degrees = angle, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(primary, tertiary, primary.copy(alpha = 0.2f), primary), center = center),
                            radius = size.maxDimension * 0.8f,
                            center = center,
                        )
                    }
                }.padding(2.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .background(Brush.horizontalGradient(listOf(primary.copy(alpha = 0.16f), tertiary.copy(alpha = 0.10f))))
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DnaHelix(Modifier.size(width = 44.dp, height = 64.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dna_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.dna_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = primary,
            )
        }
    }
}
