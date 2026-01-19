package com.humoyun.musicapp.core.base

interface BaseItem {
    val uniqueId: String

    fun isSameContent(other: BaseItem): Boolean = this == other
}