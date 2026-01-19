package com.humoyun.musicapp.model

import com.humoyun.musicapp.core.base.BaseItem

data class Category(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val heightRatio: Float = 1.0f
) : BaseItem {
    override val uniqueId: String get() = "cat_$id"
}

sealed class SearchResultItem : BaseItem {
    data class MusicItem(val music: Music) : SearchResultItem() {
        override val uniqueId: String get() = music.uniqueId
    }

    data class RadioItem(val radio: Radio) : SearchResultItem() {
        override val uniqueId: String get() = radio.uniqueId
    }

    data class Header(val title: String) : SearchResultItem() {
        override val uniqueId: String get() = "header_$title"
    }
}