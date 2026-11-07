/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.LocalPlayerAwareWindowInsets
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.FabGlassLevelKey
import com.aaditya.melyvio.ui.utils.isScrollingUp
import com.aaditya.melyvio.utils.rememberPreference

/**
 * Floating mic + shuffle buttons. They are rounded glass buttons: the user can make them fully
 * transparent, softly frosted or frosted from Settings > Appearance.
 */
@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyListState,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)? = null,
) {
    val (glassLevel) = rememberPreference(FabGlassLevelKey, 1)
    val base = MaterialTheme.colorScheme.surfaceContainer
    AnimatedVisibility(
        visible = visible && lazyListState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            if (onRecognitionClick != null) {
                GlassFab(
                    size = 44.dp,
                    shape = CircleShape,
                    level = glassLevel,
                    base = base,
                    onClick = onRecognitionClick,
                    label = stringResource(R.string.recognize_music),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            GlassFab(
                size = 56.dp,
                shape = RoundedCornerShape(20.dp),
                level = glassLevel,
                base = base,
                onClick = onClick,
                label = null,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun GlassFab(
    size: Dp,
    shape: Shape,
    level: Int,
    base: Color,
    onClick: () -> Unit,
    label: String?,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .glass(level, base, shape)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
            content()
        }
    }
}
