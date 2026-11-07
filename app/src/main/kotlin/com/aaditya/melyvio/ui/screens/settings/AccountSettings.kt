/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.screens.settings

import com.aaditya.melyvio.ui.component.MelyvioMark
import com.aaditya.melyvio.ui.component.ProfileLeadingTile
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import android.view.WindowManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.aaditya.melyvio.BuildConfig
import com.aaditya.melyvio.LocalNavController
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.GreetingNameKey
import com.aaditya.melyvio.ui.component.Material3SettingsGroup
import com.aaditya.melyvio.ui.component.Material3SettingsItem
import com.aaditya.melyvio.utils.rememberPreference

/** Profile-button popup: aligned card that opens from the top-right corner. */
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    latestVersionName: String,
) {
    val navController = LocalNavController.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        // Blur + dim whatever is behind the popup (real blur on Android 12+, dim only on older phones)
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setDimAmount(0.28f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = window.attributes
                    params.blurBehindRadius = 28
                    window.attributes = params
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
                    .padding(top = 56.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter =
                    fadeIn(tween(200)) +
                        scaleIn(tween(260), initialScale = 0.92f, transformOrigin = TransformOrigin(1f, 0f)),
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
                        ),
                    modifier =
                        Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .pointerInput(Unit) { detectTapGestures(onTap = { }) },
                ) {
                    AccountSettings(
                        navController = navController,
                        onClose = onDismiss,
                        latestVersionName = latestVersionName,
                    )
                }
            }
        }
    }
}

/**
 * Melyvio is fully anonymous (no login / accounts). This menu only has the greeting-name option
 * (a login-style screen without any login) and Settings.
 */
@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String,
) {
    val (greetingName) = rememberPreference(GreetingNameKey, "")
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MelyvioMark(
                size = 38.dp,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                tile = MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.close), contentDescription = null)
            }
        }

        Spacer(Modifier.height(14.dp))

        Material3SettingsGroup(
            items =
                listOf(
                    Material3SettingsItem(
                        leadingContent = { ProfileLeadingTile() },
                        title = { Text(stringResource(R.string.greeting_item_title)) },
                        description = {
                            Text(
                                if (greetingName.isBlank()) {
                                    stringResource(R.string.greeting_item_hint)
                                } else {
                                    greetingName
                                },
                            )
                        },
                        onClick = {
                            onClose()
                            navController.navigate("greeting")
                        },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.settings),
                        title = { Text(stringResource(R.string.settings)) },
                        showBadge =
                            BuildConfig.UPDATER_AVAILABLE &&
                                latestVersionName != BuildConfig.BASE_VERSION_NAME,
                        onClick = {
                            onClose()
                            navController.navigate("settings")
                        },
                    ),
                ),
            useLowContrast = true,
        )
    }
}
