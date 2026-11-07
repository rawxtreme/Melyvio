package com.aaditya.melyvio.innertube.pages

import com.aaditya.melyvio.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)

