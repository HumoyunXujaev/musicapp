package com.humoyun.musicapp.model

import android.net.Uri
import com.humoyun.musicapp.core.base.BaseItem

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri,
    val trackCount: Int
) : BaseItem {
    override val uniqueId: String get() = "album_$id"
}
