/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.models

import com.aaditya.melyvio.innertube.models.YTItem
import com.aaditya.melyvio.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)

