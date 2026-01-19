package com.humoyun.musicapp.model

import com.humoyun.musicapp.core.base.BaseItem

data class Radio(
    val id: Int,
    val title: String,
    val fmNumber: String,
    val imageUrl: String,
    val streamUrl: String,
    val priority: Int,
    val isPlaying: Boolean = false,
    val isCurrent: Boolean = false
) : BaseItem {
    override val uniqueId: String get() = "radio_$id"
}