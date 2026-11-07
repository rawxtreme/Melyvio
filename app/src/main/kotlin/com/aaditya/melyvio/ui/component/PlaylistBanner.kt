/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aaditya.melyvio.LocalDatabase
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.PinnedPlaylistsKey
import com.aaditya.melyvio.db.entities.Playlist
import com.aaditya.melyvio.db.entities.PlaylistEntity
import com.aaditya.melyvio.utils.ProfilePhoto
import com.aaditya.melyvio.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/**
 * Custom playlist banner. The photo is centre-cropped to a square and kept in the app's own
 * storage. It is saved as the playlist's thumbnail, so it stays until the user removes it
 * (adding songs never replaces it). Removing it brings back the latest-song banner.
 */
object PlaylistBanner {
    private const val DIR = "playlist_banners"
    private const val SIZE_PX = 720

    /** Only the user's own local playlists: not Liked / Downloaded / Top, and not YouTube-synced ones. */
    fun canHaveBanner(entity: PlaylistEntity): Boolean =
        entity.browseId == null &&
            entity.isEditable &&
            entity.id != PlaylistEntity.LIKED_PLAYLIST_ID &&
            entity.id != PlaylistEntity.DOWNLOADED_PLAYLIST_ID &&
            entity.id != PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID &&
            entity.id != PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID

    fun isCustom(thumbnailUrl: String?): Boolean = thumbnailUrl?.startsWith("content://") == true

    /** Crops + stores the photo and returns the thumbnail url to save on the playlist (null on failure). */
    suspend fun save(
        context: Context,
        playlistId: String,
        source: Uri,
    ): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = ProfilePhoto.decodeSquare(context, source, SIZE_PX) ?: return@runCatching null
                val dir = File(context.filesDir, DIR).apply { mkdirs() }
                dir.listFiles { f -> f.name.startsWith(playlistId + "_") }?.forEach { it.delete() }
                val file = File(dir, playlistId + "_" + System.currentTimeMillis() + ".jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                FileProvider.getUriForFile(context, context.packageName + ".FileProvider", file).toString()
            }.getOrNull()
        }

    suspend fun deleteFile(
        context: Context,
        thumbnailUrl: String?,
    ) {
        if (thumbnailUrl == null) return
        withContext(Dispatchers.IO) {
            runCatching {
                val name = Uri.parse(thumbnailUrl).lastPathSegment ?: return@runCatching
                File(File(context.filesDir, DIR), name).delete()
            }
        }
    }
}

/**
 * Extra long-press / 3-dots entries for a local playlist: pin to top, add / change / remove banner.
 * Used by both PlaylistMenu and LocalPlaylistMenu.
 */
@Composable
fun rememberPlaylistExtraMenuItems(
    playlist: Playlist,
    showPin: Boolean,
    showBanner: Boolean,
    onDismiss: () -> Unit,
): List<Material3MenuItemData> {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val dbPlaylist by database.playlist(playlist.id).collectAsStateWithLifecycle(initialValue = playlist)
    val entity = dbPlaylist?.playlist ?: playlist.playlist
    val (pinnedIds, setPinnedIds) = rememberPreference(PinnedPlaylistsKey, emptySet<String>())
    val isPinned = playlist.id in pinnedIds
    val hasBanner = PlaylistBanner.isCustom(entity.thumbnailUrl)
    val errorText = stringResource(R.string.banner_error)

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    val url = PlaylistBanner.save(context, entity.id, uri)
                    if (url != null) {
                        database.query { update(entity.copy(thumbnailUrl = url, lastUpdateTime = LocalDateTime.now())) }
                    } else {
                        Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            }
        }

    return buildList {
        if (showPin) {
            add(
                Material3MenuItemData(
                    title = { Text(stringResource(if (isPinned) R.string.playlist_unpin else R.string.playlist_pin)) },
                    description = { Text(stringResource(R.string.playlist_pin_desc)) },
                    icon = { Icon(painterResource(R.drawable.ic_push_pin), contentDescription = null) },
                    onClick = {
                        setPinnedIds(if (isPinned) pinnedIds - playlist.id else pinnedIds + playlist.id)
                        onDismiss()
                    },
                ),
            )
        }
        if (showBanner && PlaylistBanner.canHaveBanner(entity)) {
            add(
                Material3MenuItemData(
                    title = { Text(stringResource(if (hasBanner) R.string.banner_change else R.string.banner_add)) },
                    description = { Text(stringResource(R.string.banner_menu_desc)) },
                    icon = { Icon(painterResource(R.drawable.insert_photo), contentDescription = null) },
                    // the menu stays open until the photo is saved, so the result is not lost
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                ),
            )
            if (hasBanner) {
                add(
                    Material3MenuItemData(
                        title = { Text(stringResource(R.string.banner_remove)) },
                        description = { Text(stringResource(R.string.banner_remove_desc)) },
                        icon = { Icon(painterResource(R.drawable.hide_image), contentDescription = null) },
                        onClick = {
                            scope.launch {
                                PlaylistBanner.deleteFile(context, entity.thumbnailUrl)
                                database.query { update(entity.copy(thumbnailUrl = null, lastUpdateTime = LocalDateTime.now())) }
                                onDismiss()
                            }
                        },
                    ),
                )
            }
        }
    }
}

/** "Add custom banner" row for the create-playlist dialog. */
@Composable
fun BannerPickerRow(
    bannerUri: Uri?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onPick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (bannerUri != null) {
                AsyncImage(
                    model = bannerUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp),
                )
            } else {
                Icon(painterResource(R.drawable.insert_photo), contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(if (bannerUri == null) R.string.banner_add else R.string.banner_change),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.banner_optional),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (bannerUri != null) {
            TextButton(onClick = onRemove) { Text(stringResource(R.string.banner_remove)) }
        }
    }
}
