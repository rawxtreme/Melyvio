package com.aaditya.melyvio.innertube.pages

import com.aaditya.melyvio.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)

