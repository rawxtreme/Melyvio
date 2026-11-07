/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aaditya.melyvio.LocalPlayerAwareWindowInsets
import com.aaditya.melyvio.R
import com.aaditya.melyvio.ui.component.IconButton
import com.aaditya.melyvio.ui.utils.backToMain
import kotlinx.coroutines.delay

/**
 * "Buy me a coffee" screen. Every colour comes from MaterialTheme.colorScheme, so it follows
 * whatever theme / dynamic colour / pure-black option the user picked in Appearance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(navController: NavController) {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val secondary = colors.secondary
    val tertiary = colors.tertiary

    val infinite = rememberInfiniteTransition(label = "donate")
    val glowScale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowScale",
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    val floatY by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val borderAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "borderAngle",
    )
    val heartScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heartScale",
    )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )
        Spacer(Modifier.height(24.dp))

        // 1. Animated coffee badge
        StaggerIn(index = 0) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(136.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(136.dp)
                            .graphicsLayer {
                                scaleX = glowScale
                                scaleY = glowScale
                                alpha = glowAlpha
                            }.background(Brush.radialGradient(listOf(primary, Color.Transparent)), CircleShape),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(92.dp)
                            .graphicsLayer { translationY = floatY.dp.toPx() }
                            .shadow(8.dp, CircleShape)
                            .background(colors.primaryContainer, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.buymeacoffee),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. Title + message
        StaggerIn(index = 1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.donate_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.donate_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // 3. QR card with a slowly rotating theme-coloured border
        StaggerIn(index = 2) {
            val qrShape = RoundedCornerShape(32.dp)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .shadow(10.dp, qrShape)
                        .clip(qrShape)
                        .drawBehind {
                            rotate(degrees = borderAngle, pivot = center) {
                                drawCircle(
                                    brush =
                                        Brush.sweepGradient(
                                            colors = listOf(primary, tertiary, secondary, primary),
                                            center = center,
                                        ),
                                    radius = size.maxDimension * 0.75f,
                                    center = center,
                                )
                            }
                        }.padding(4.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White),
            ) {
                Image(
                    painter = painterResource(R.drawable.donate_qr),
                    contentDescription = stringResource(R.string.donate_qr_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggerIn(index = 3) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.donate_scan),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.donate_scan_tip),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // 4. Thank-you card with a heartbeat
        StaggerIn(index = 4) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(20.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.favorite),
                        contentDescription = null,
                        tint = colors.onSecondaryContainer,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .graphicsLayer {
                                    scaleX = heartScale
                                    scaleY = heartScale
                                },
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.donate_thanks),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSecondaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.donate_thanks_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSecondaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.donate_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

/** Fades + slides a section in after a small staggered delay. */
@Composable
private fun StaggerIn(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 110L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(tween(500)) +
                slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 5 },
    ) {
        content()
    }
}
