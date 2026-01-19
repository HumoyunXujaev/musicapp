package com.humoyun.musicapp.model

import com.humoyun.musicapp.core.base.BaseItem


data class Artist(
    val id: Long,
    val name: String,
    val trackCount: Int
) : BaseItem {
    override val uniqueId: String get() = "artist_$id"
}
