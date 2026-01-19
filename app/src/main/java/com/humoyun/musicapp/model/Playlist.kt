package com.humoyun.musicapp.model

import android.net.Uri

data class Playlist(
    val id: Long,
    val name: String,
    val count: Int,
    val artUri: Uri? = null
)