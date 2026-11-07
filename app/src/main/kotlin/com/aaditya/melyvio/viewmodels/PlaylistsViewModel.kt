/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.aaditya.melyvio.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditya.melyvio.constants.AddToPlaylistSortDescendingKey
import com.aaditya.melyvio.constants.AddToPlaylistSortTypeKey
import com.aaditya.melyvio.constants.PlaylistSortType
import com.aaditya.melyvio.db.MusicDatabase
import com.aaditya.melyvio.extensions.toEnum
import com.aaditya.melyvio.utils.SyncUtils
import com.aaditya.melyvio.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allPlaylists =
        context.dataStore.data
            .map {
                it[AddToPlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (it[AddToPlaylistSortDescendingKey]
                    ?: true)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.playlists(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Suspend function that waits for sync to complete
    suspend fun sync() {
        syncUtils.syncSavedPlaylists()
    }
}

