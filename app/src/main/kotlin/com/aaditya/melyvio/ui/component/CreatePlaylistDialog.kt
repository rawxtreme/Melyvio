/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.runtime.key
import com.aaditya.melyvio.utils.PlaylistImportParser
import com.aaditya.melyvio.utils.PlaylistImportManager
import com.aaditya.melyvio.utils.ParsedPlaylist
import com.aaditya.melyvio.utils.ParseResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import com.aaditya.melyvio.LocalDatabase
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.LocalSyncUtils
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.InnerTubeCookieKey
import com.aaditya.melyvio.db.entities.PlaylistEntity
import com.aaditya.melyvio.extensions.isSyncEnabled
import com.aaditya.melyvio.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
) {
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    val database = LocalDatabase.current
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var importParsed by remember { mutableStateOf<ParsedPlaylist?>(null) }
    var importErrorRes by remember { mutableStateOf<Int?>(null) }
    var showImportIntro by remember { mutableStateOf(false) }
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) { PlaylistImportParser.readAndParse(context, uri) }
                    when (result) {
                        is ParseResult.Ok -> {
                            importParsed = result.playlist
                            importErrorRes = null
                        }
                        ParseResult.NotMelyvio -> {
                            importParsed = null
                            importErrorRes = R.string.import_err_not_melyvio
                        }
                        ParseResult.Empty -> {
                            importParsed = null
                            importErrorRes = R.string.import_err_empty
                        }
                        ParseResult.TooLarge -> {
                            importParsed = null
                            importErrorRes = R.string.import_err_large
                        }
                        ParseResult.Unreadable -> {
                            importParsed = null
                            importErrorRes = R.string.import_err_read
                        }
                    }
                }
            }
        }
    val bannerPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) bannerUri = uri
        }

    val notLoggedInYoutubeStr = stringResource(R.string.not_logged_in_youtube)
    val syncDisabledStr = stringResource(R.string.sync_disabled)
    val playlistCreatedLocallyStr = stringResource(R.string.playlist_created_locally)

    if (showImportIntro) {
        ImportIntroDialog(
            onChooseFile = {
                showImportIntro = false
                importPicker.launch(arrayOf("*/*"))
            },
            onDismiss = { showImportIntro = false },
        )
    }

    // a new key re-creates the dialog so the name field starts with the imported playlist's name
    key(importParsed?.name) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(text = stringResource(R.string.create_playlist)) },
            initialTextFieldValue = TextFieldValue(importParsed?.name ?: initialTextFieldValue ?: ""),
            onDismiss = onDismiss,
            onDone = { playlistName ->
                val pickedBanner = bannerUri
                val pickedImport = importParsed
                val appContext = context.applicationContext
                syncUtils.createPlaylist(
                    playlist = PlaylistEntity(
                        name = playlistName,
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true,
                    ),
                    syncWithYouTube = syncedPlaylist,
                ) { playlistId, remoteCreated ->
                    if (syncedPlaylist && !remoteCreated) {
                        Toast.makeText(context, playlistCreatedLocallyStr, Toast.LENGTH_LONG).show()
                    }
                    if (pickedBanner != null) {
                        // runs on its own scope: this dialog is already closed when the playlist exists
                        CoroutineScope(Dispatchers.IO).launch {
                            val url = PlaylistBanner.save(appContext, playlistId, pickedBanner)
                            if (url != null) {
                                repeat(10) {
                                    val entity = database.playlist(playlistId).first()?.playlist
                                    if (entity != null) {
                                        database.query { update(entity.copy(thumbnailUrl = url)) }
                                        return@launch
                                    }
                                    delay(200)
                                }
                            }
                        }
                    }
                    if (pickedImport != null) {
                        if (PlaylistImportManager.isBusy()) {
                            Toast.makeText(appContext, appContext.getString(R.string.import_busy), Toast.LENGTH_SHORT).show()
                        } else {
                            PlaylistImportManager.start(database, playlistId, playlistName, pickedImport.entries)
                        }
                    }
                    onPlaylistCreated?.invoke(playlistId)
                }
            },
            extraContent = {
                Column {
                    if (allowSyncing && isSignedIn) {
                        Row(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 40.dp),
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.sync_playlist),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = stringResource(R.string.allows_for_sync_witch_youtube),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Switch(
                                    checked = syncedPlaylist,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val isYtmSyncEnabled = withContext(Dispatchers.IO) { context.isSyncEnabled() }
                                            if (!isSignedIn && !syncedPlaylist) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        notLoggedInYoutubeStr,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            } else if (!isYtmSyncEnabled) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        syncDisabledStr,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            } else {
                                                syncedPlaylist = !syncedPlaylist
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
        
                    BannerPickerRow(
                        bannerUri = bannerUri,
                        onPick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRemove = { bannerUri = null },
                    )
                    ImportPlaylistRow(
                        parsed = importParsed,
                        errorRes = importErrorRes,
                        onClick = { showImportIntro = true },
                        onRemove = {
                            importParsed = null
                            importErrorRes = null
                        },
                    )
                }
            },
        )
    }
}

