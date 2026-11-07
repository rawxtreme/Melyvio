/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.lyrics

import android.content.Context
import com.aaditya.melyvio.kugou.KuGou
import com.aaditya.melyvio.constants.EnableKugouKey
import com.aaditya.melyvio.utils.dataStore
import com.aaditya.melyvio.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> =
        KuGou.getLyrics(title, artist, duration, album)

    override suspend fun getAllLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, album, callback)
    }
}


