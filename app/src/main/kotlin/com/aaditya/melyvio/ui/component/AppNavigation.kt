/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.ui.graphics.graphicsLayer
import com.aaditya.melyvio.R
import com.aaditya.melyvio.LocalPlayerConnection
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.constants.NavGlassLevelKey
import com.aaditya.melyvio.constants.NavigationBarHeight
import com.aaditya.melyvio.utils.rememberPreference
import com.aaditya.melyvio.constants.SlimNavBarHeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.aaditya.melyvio.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Floating "glass" navigation bar.
 *
 * - The bar has NO background of its own: the app content stays visible behind it (scrolling or not).
 * - Home + Library live inside one cylinder (pill); Search is a separate round button on the right.
 * - Long-press behaviour is unchanged: Search long-press -> music recognition, Home long-hold -> profile menu.
 */
@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
    compact: Boolean = false,
    onMiniPlayerClick: () -> Unit = {},
) {
    val pillItems = remember(navigationItems) { navigationItems.filter { it != Screens.Search } }
    val searchItem = remember(navigationItems) { navigationItems.firstOrNull { it == Screens.Search } }
    val rowHeight = if (slimNav) SlimNavBarHeight else NavigationBarHeight
    val glassHeight = rowHeight - 16.dp

    val baseColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val (navGlassLevel) = rememberPreference(NavGlassLevelKey, 1)
    val glassBrush = remember(baseColor, navGlassLevel) { Glass.containerBrush(navGlassLevel, baseColor) }
    val borderBrush = remember(navGlassLevel) { Glass.borderBrush(navGlassLevel) }
    val activeColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface
    val inactiveColor = activeColor.copy(alpha = 0.6f)

    // 0 = full bar, 1 = small bar (shown while scrolling down)
    val compactT by animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "navCompact",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (compactT > 0.01f) {
            CompactNavBar(
                navigationItems = navigationItems,
                currentRoute = currentRoute,
                onItemClick = onItemClick,
                onSearchLongClick = onSearchLongClick,
                onHomeLongHold = onHomeLongHold,
                onMiniPlayerClick = onMiniPlayerClick,
                glassBrush = glassBrush,
                borderBrush = borderBrush,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                rowHeight = rowHeight,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = ((compactT - 0.25f) / 0.75f).coerceIn(0f, 1f)
                            val sc = 0.82f + 0.18f * compactT
                            scaleX = sc
                            scaleY = sc
                        },
            )
        }
        if (compactT < 0.5f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = (1f - compactT * 2f).coerceIn(0f, 1f)
                            val sc = 1f - 0.12f * compactT
                            scaleX = sc
                            scaleY = sc
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
            ) {
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(glassHeight)
                        .clip(CircleShape)
                        .background(glassBrush)
                        .border(1.dp, borderBrush, CircleShape),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pillItems.forEach { screen ->
                    FloatingNavItem(
                        screen = screen,
                        isSelected = isRouteSelected(currentRoute, screen.route, navigationItems),
                        showLabel = !slimNav,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                        onItemClick = onItemClick,
                        onSearchLongClick = onSearchLongClick,
                        onHomeLongHold = onHomeLongHold,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            if (searchItem != null) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier =
                        Modifier
                            .size(glassHeight)
                            .clip(CircleShape)
                            .background(glassBrush)
                            .border(1.dp, borderBrush, CircleShape),
                ) {
                    FloatingNavItem(
                        screen = searchItem,
                        isSelected = isRouteSelected(currentRoute, searchItem.route, navigationItems),
                        showLabel = false,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                        onItemClick = onItemClick,
                        onSearchLongClick = onSearchLongClick,
                        onHomeLongHold = onHomeLongHold,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
            }
        }
    }
}

/** Small bar for scroll-down: icons only, with the playing song (picture + play/pause) between Home and Search. */
@Composable
private fun CompactNavBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    onSearchLongClick: (() -> Unit)?,
    onHomeLongHold: (() -> Unit)?,
    onMiniPlayerClick: () -> Unit,
    glassBrush: Brush,
    borderBrush: Brush,
    activeColor: Color,
    inactiveColor: Color,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(rowHeight),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .height(rowHeight - 20.dp)
                    .clip(CircleShape)
                    .background(glassBrush)
                    .border(1.dp, borderBrush, CircleShape)
                    .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationItems.forEach { screen ->
                FloatingNavItem(
                    screen = screen,
                    isSelected = isRouteSelected(currentRoute, screen.route, navigationItems),
                    showLabel = false,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onItemClick = onItemClick,
                    onSearchLongClick = onSearchLongClick,
                    onHomeLongHold = onHomeLongHold,
                    modifier = Modifier.size(46.dp),
                )
                if (screen == Screens.Home) {
                    CompactMiniPlayer(activeColor = activeColor, onClick = onMiniPlayerClick)
                }
            }
        }
    }
}

@Composable
private fun CompactMiniPlayer(
    activeColor: Color,
    onClick: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val playing by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val song = metadata ?: return
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.12f))
                    .clickable(role = Role.Button, onClick = onClick),
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.14f))
                    .clickable(role = Role.Button) { playerConnection.togglePlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

/** True while the bars should be shown small (scrolling down). Read by the mini player card. */
val LocalNavCompact = androidx.compose.runtime.compositionLocalOf { false }

@Composable
private fun FloatingNavItem(
    screen: Screens,
    isSelected: Boolean,
    showLabel: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onItemClick: (Screens, Boolean) -> Unit,
    onSearchLongClick: (() -> Unit)?,
    onHomeLongHold: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val currentIsSelected by rememberUpdatedState(isSelected)
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    val isSearchItem = screen == Screens.Search && onSearchLongClick != null
    val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
    val interactionSource = remember { MutableInteractionSource() }

    // Long press detection using InteractionSource (same behaviour as before)
    if (isSearchItem || isHomeHoldItem) {
        LaunchedEffect(interactionSource) {
            var isLongClick = false
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        isLongClick = false
                        delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                        isLongClick = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isHomeHoldItem) onHomeLongHold?.invoke() else onSearchLongClick?.invoke()
                    }
                    is PressInteraction.Release -> {
                        if (!isLongClick) {
                            currentOnItemClick(screen, currentIsSelected)
                        }
                    }
                    is PressInteraction.Cancel -> {
                        isLongClick = false
                    }
                }
            }
        }
    }

    val tint = if (isSelected) activeColor else inactiveColor
    val iconRes = if (isSelected) screen.iconIdActive else screen.iconIdInactive

    Column(
        modifier =
            modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = {
                        if (!isSearchItem && !isHomeHoldItem) {
                            currentOnItemClick(screen, currentIsSelected)
                        }
                    },
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(screen.titleId),
            tint = tint,
            modifier = Modifier.size(if (showLabel) 24.dp else 26.dp),
        )
        if (showLabel) {
            Text(
                text = stringResource(screen.titleId),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
