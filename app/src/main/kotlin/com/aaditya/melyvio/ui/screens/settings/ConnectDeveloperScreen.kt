/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aaditya.melyvio.LocalPlayerAwareWindowInsets
import com.aaditya.melyvio.R
import com.aaditya.melyvio.ui.component.IconButton
import com.aaditya.melyvio.ui.utils.backToMain

private const val TELEGRAM_URL = "https://t.me/rawxtremestudios"

/** Plain, calm page: where to follow Melyvio updates (Telegram). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectDeveloperScreen(navController: NavController) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(300)) }
    val errorText = stringResource(R.string.connect_dev_error)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .graphicsLayer { alpha = appear.value }
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.telegram),
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(42.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.connect_dev_heading),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.connect_dev_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.connect_dev_list_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                listOf(
                    R.string.connect_dev_item1,
                    R.string.connect_dev_item2,
                    R.string.connect_dev_item3,
                    R.string.connect_dev_item4,
                ).forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = stringResource(item), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, TELEGRAM_URL.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Icon(painterResource(R.drawable.telegram), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.connect_dev_button), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "t.me/rawxtremestudios",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.connect_dev_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}
