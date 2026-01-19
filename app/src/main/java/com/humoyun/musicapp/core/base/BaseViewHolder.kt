package com.humoyun.musicapp.core.base

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseViewHolder<T : BaseItem, VB : ViewBinding>(
    val binding: VB
) : RecyclerView.ViewHolder(binding.root) {

    val context: Context get() = itemView.context

    abstract fun bind(item: T)

    open fun bindPayload(item: T, payloads: List<Any>) {
        bind(item)
    }
}