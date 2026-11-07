/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aaditya.melyvio.constants.ExperimentalLyricsKey
import com.aaditya.melyvio.utils.rememberPreference
import com.aaditya.melyvio.viewmodels.LyricsViewModel

@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel()
) {
    val (experimentalLyrics, _) = rememberPreference(key = ExperimentalLyricsKey, defaultValue = true)

    if (experimentalLyrics) {
        ExperimentalLyrics(
            sliderPositionProvider = sliderPositionProvider,
            modifier = modifier,
            showLyrics = showLyrics,
            lyricsViewModel = lyricsViewModel
        )
    } else {
        OriginalLyrics(
            sliderPositionProvider = sliderPositionProvider,
            modifier = modifier,
            showLyrics = showLyrics
        )
    }
}

