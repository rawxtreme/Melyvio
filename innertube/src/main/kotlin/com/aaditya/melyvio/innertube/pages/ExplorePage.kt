package com.aaditya.melyvio.innertube.pages

import com.aaditya.melyvio.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)

