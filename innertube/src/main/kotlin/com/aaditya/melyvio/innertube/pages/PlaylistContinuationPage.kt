package com.aaditya.melyvio.innertube.pages

import com.aaditya.melyvio.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)

