package com.humoyun.musicapp.model

import com.humoyun.musicapp.core.base.BaseItem

data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int
) : BaseItem {
    override val uniqueId: String get() = path
}


