/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaditya.melyvio.R
import com.aaditya.melyvio.utils.ImportStage
import com.aaditya.melyvio.utils.ImportState
import com.aaditya.melyvio.utils.ParsedPlaylist
import com.aaditya.melyvio.utils.PlaylistImportManager

/** Loading screen and result of a playlist import. Put once near the top of the app. */
@Composable
fun PlaylistImportHost() {
    val state by PlaylistImportManager.state.collectAsStateWithLifecycle()
    when (val current = state) {
        is ImportState.Idle -> Unit
        is ImportState.Running -> {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            ) {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.import_running_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = current.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(20.dp))
                        if (current.total > 0) {
                            LinearProgressIndicator(
                                progress = { current.done.toFloat() / current.total },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text =
                                stringResource(
                                    when (current.stage) {
                                        ImportStage.CHECKING -> R.string.import_stage_check
                                        ImportStage.FETCHING -> R.string.import_stage_fetch
                                        ImportStage.SEARCHING -> R.string.import_stage_search
                                        ImportStage.ADDING -> R.string.import_stage_add
                                    },
                                ) + if (current.total > 0) "  ${current.done}/${current.total}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.import_keep_open),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        is ImportState.Finished -> {
            AlertDialog(
                onDismissRequest = { PlaylistImportManager.dismiss() },
                title = { Text(stringResource(R.string.import_done_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.import_done_message, current.added, current.total, current.name))
                        if (current.failed.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.import_failed_intro),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                                current.failed.forEach { entry ->
                                    val line = if (entry.artist.isBlank()) entry.title else entry.title + " - " + entry.artist
                                    Text(
                                        text = "\u2022 " + line.ifBlank { entry.videoId },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { PlaylistImportManager.dismiss() }) { Text(stringResource(R.string.import_done_button)) }
                },
            )
        }
    }
}

/** Opens when "Import playlist" is tapped: explains why it is helpful, then lets the user pick the file. */
@Composable
fun ImportIntroDialog(
    onChooseFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.restore), contentDescription = null) },
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Text(
                text = stringResource(R.string.import_help),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton(onClick = onChooseFile) { Text(stringResource(R.string.import_choose_file)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

/** "Import playlist" row for the create-playlist dialog. */
@Composable
fun ImportPlaylistRow(
    parsed: ParsedPlaylist?,
    errorRes: Int?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            painter = painterResource(R.drawable.restore),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.import_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text =
                    when {
                        parsed != null -> stringResource(R.string.import_ready, parsed.name, parsed.entries.size)
                        errorRes != null -> stringResource(errorRes)
                        else -> stringResource(R.string.import_row_hint)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = if (errorRes != null && parsed == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (parsed != null) {
            TextButton(onClick = onRemove) { Text(stringResource(R.string.banner_remove)) }
        }
    }
}
