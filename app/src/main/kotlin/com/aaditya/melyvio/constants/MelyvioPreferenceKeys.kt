/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** Name shown in the home greeting (max 10 characters, empty = no name). */
val GreetingNameKey = stringPreferencesKey("greetingName")

/** Show the "Music for your evening" banner above Speed dial. */
val ShowHomeBannerKey = booleanPreferencesKey("showHomeBanner")

/** Glass level of the bottom bar (Home / Search / Library): 0 transparent, 1 soft, 2 frosted. */
val NavGlassLevelKey = intPreferencesKey("navGlassLevel")

/** Glass level of the floating mic + shuffle buttons: 0 transparent, 1 soft, 2 frosted. */
val FabGlassLevelKey = intPreferencesKey("fabGlassLevel")

/** Pause playback when headphones / Bluetooth audio disconnect. */
val PauseOnHeadphoneUnplugKey = booleanPreferencesKey("pauseOnHeadphoneUnplug")

/** 0 = no profile photo, otherwise the time it was last changed (used to refresh the images). */
val ProfilePhotoVersionKey = longPreferencesKey("profilePhotoVersion")

/** Playlist ids pinned to the top of Library > Playlists. */
val PinnedPlaylistsKey = stringSetPreferencesKey("pinnedPlaylists")

/** Extra loudness added on top of the system volume, in percent (0 to 100). */
val AudioBoostPercentKey = intPreferencesKey("audioBoostPercent")
