package com.humoyun.musicapp.core.base

import androidx.recyclerview.widget.DiffUtil

class BaseDiffCallback<T : BaseItem> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem.uniqueId == newItem.uniqueId
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem.isSameContent(newItem)
    }

    override fun getChangePayload(oldItem: T, newItem: T): Any? {
        return if (oldItem.uniqueId == newItem.uniqueId) true else null
    }
}