package com.humoyun.musicapp.model

import android.net.Uri
import com.humoyun.musicapp.core.base.BaseItem

data class Music(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val isOnline: Boolean = false,
    val isPlaying: Boolean = false,
    val isCurrent: Boolean = false
) : BaseItem {
    override val uniqueId: String get() = id

    override fun isSameContent(other: BaseItem): Boolean {
        if (other !is Music) return false
        return this == other
    }
}