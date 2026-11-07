/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.CrossfadeDurationKey
import com.aaditya.melyvio.constants.CrossfadeEnabledKey
import com.aaditya.melyvio.utils.rememberPreference
import kotlin.math.abs

/**
 * Quick crossfade control for the player menu. It uses the same settings as Settings > Player,
 * so the playback engine only crossfades while the switch here is ON.
 */
@Composable
fun CrossfadeDialog(onDismiss: () -> Unit) {
    val (enabled, setEnabled) = rememberPreference(CrossfadeEnabledKey, false)
    val (duration, setDuration) = rememberPreference(CrossfadeDurationKey, 5f)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.linear_scale), contentDescription = null) },
        title = { Text(stringResource(R.string.crossfade)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.crossfade_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.crossfade), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = enabled, onCheckedChange = setEnabled)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.crossfade_duration),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3f, 5f, 8f).forEach { seconds ->
                        FilterChip(
                            selected = enabled && abs(duration - seconds) < 0.5f,
                            onClick = { setDuration(seconds) },
                            enabled = enabled,
                            label = { Text(seconds.toInt().toString() + " s") },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text =
                        if (enabled) {
                            stringResource(R.string.crossfade_quick_current, ((duration * 10).toInt() / 10f).toString())
                        } else {
                            stringResource(R.string.crossfade_quick_hint)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.crossfade_quick_done)) }
        },
    )
}
