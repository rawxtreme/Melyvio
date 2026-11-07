/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.LocalPlayerConnection
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.AudioBoostPercentKey
import com.aaditya.melyvio.utils.rememberPreference
import kotlin.math.roundToInt

/** Boost values up to this are the "safe" range. */
private const val SAFE_LIMIT = 25

private fun readVolume(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

/** Current phone (music) volume, 0..1, kept up to date while the dialog is open. */
@Composable
private fun rememberSystemVolume(): Float {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volume by remember { mutableFloatStateOf(readVolume(audio)) }
    DisposableEffect(Unit) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    volume = readVolume(audio)
                }
            }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return volume
}

/**
 * Audio booster: shows the phone volume first, then lets the user add extra loudness on top
 * (0 to 100% extra) with a safe range marked under the slider.
 */
@Composable
fun AudioBoosterDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current
    val (savedBoost, setSavedBoost) = rememberPreference(AudioBoostPercentKey, 0)
    var boost by remember { mutableIntStateOf(savedBoost) }
    LaunchedEffect(savedBoost) { boost = savedBoost }
    val systemVolume = rememberSystemVolume()
    val colors = MaterialTheme.colorScheme

    val loud = boost > SAFE_LIMIT
    val zoneColor by animateColorAsState(if (loud) colors.error else colors.primary, tween(250), label = "zone")
    val safeAlpha by animateFloatAsState(if (loud) 0.35f else 1f, tween(250), label = "safeAlpha")
    val loudAlpha by animateFloatAsState(if (loud) 1f else 0.35f, tween(250), label = "loudAlpha")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.booster_title)) },
        text = {
            Column {
                // 1) phone volume first
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.booster_system_volume),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(systemVolume * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { systemVolume },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.booster_system_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))

                // 2) boost slider
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.booster_boost),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "+$boost%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = zoneColor,
                    )
                }
                Slider(
                    value = boost.toFloat(),
                    onValueChange = {
                        boost = it.roundToInt()
                        playerConnection?.service?.setAudioBoostPercent(boost)
                    },
                    onValueChangeFinished = { setSavedBoost(boost) },
                    valueRange = 0f..100f,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = zoneColor,
                            activeTrackColor = zoneColor,
                        ),
                )

                // 3) safe limit under the slider, the zone you are in is highlighted
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(SAFE_LIMIT.toFloat())) {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = safeAlpha)),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.booster_safe, SAFE_LIMIT),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary.copy(alpha = safeAlpha),
                            maxLines = 1,
                        )
                    }
                    Column(Modifier.weight((100 - SAFE_LIMIT).toFloat())) {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.error.copy(alpha = loudAlpha)),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.booster_loud),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.error.copy(alpha = loudAlpha),
                            maxLines = 1,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.booster_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.booster_done)) }
        },
        dismissButton = {
            TextButton(
                enabled = boost > 0,
                onClick = {
                    boost = 0
                    playerConnection?.service?.setAudioBoostPercent(0)
                    setSavedBoost(0)
                },
            ) { Text(stringResource(R.string.booster_reset)) }
        },
    )
}
