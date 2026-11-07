package com.aaditya.melyvio.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylistResponse(
    val playlistId: String
)

