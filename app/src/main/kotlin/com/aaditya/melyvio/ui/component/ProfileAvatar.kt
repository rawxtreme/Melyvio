/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.ProfilePhotoVersionKey
import com.aaditya.melyvio.utils.ProfilePhoto
import com.aaditya.melyvio.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The user's profile photo (null when none is set). [large] = the bigger copy for the name page. */
@Composable
fun rememberProfileBitmap(large: Boolean): ImageBitmap? {
    val context = LocalContext.current
    val (version) = rememberPreference(ProfilePhotoVersionKey, 0L)
    val bitmap: State<ImageBitmap?> =
        produceState<ImageBitmap?>(initialValue = null, version, large) {
            value =
                if (version == 0L) {
                    null
                } else {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            BitmapFactory.decodeFile(ProfilePhoto.file(context, large).absolutePath)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
        }
    return bitmap.value
}

/** Top-bar profile button content: the small round photo, or the default account icon. */
@Composable
fun ProfileTopBarContent() {
    val photo = rememberProfileBitmap(large = false)
    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = stringResource(R.string.account),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(36.dp).clip(CircleShape),
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.account),
            contentDescription = stringResource(R.string.account),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Leading tile of the "Your name" row: the user's photo (round) or the default account icon. */
@Composable
fun ProfileLeadingTile() {
    val photo = rememberProfileBitmap(large = false)
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(if (photo != null) CircleShape else RoundedCornerShape(12.dp))
                .background(primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.account),
                contentDescription = null,
                tint = primary.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Small icon inside the name field: the user's photo or the default account icon. */
@Composable
fun ProfileFieldIcon() {
    val photo = rememberProfileBitmap(large = false)
    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(28.dp).clip(CircleShape),
        )
    } else {
        Icon(painter = painterResource(R.drawable.account), contentDescription = null)
    }
}
