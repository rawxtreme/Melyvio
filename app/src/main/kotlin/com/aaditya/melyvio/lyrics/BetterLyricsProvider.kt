/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.lyrics

import android.content.Context
import com.aaditya.melyvio.betterlyrics.BetterLyrics
import com.aaditya.melyvio.constants.EnableBetterLyricsKey
import com.aaditya.melyvio.utils.dataStore
import com.aaditya.melyvio.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album)
}

