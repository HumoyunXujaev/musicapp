package com.humoyun.musicapp.core.base

import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseListAdapter<T : BaseItem, VB : ViewBinding>(
    private val bindingFactory: (LayoutInflater, ViewGroup, Boolean) -> VB,
    private val onItemClick: ((T) -> Unit)? = null,
    private val onLongClick: ((T) -> Boolean)? = null
) : ListAdapter<T, BaseViewHolder<T, VB>>(BaseDiffCallback<T>()) {

    private var lastClickTime: Long = 0
    private val clickDebounce: Long = 100

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, VB> {
        val binding = bindingFactory(LayoutInflater.from(parent.context), parent, false)
        val holder = createViewHolder(binding)

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                // Защита от двойного клика
                if (SystemClock.elapsedRealtime() - lastClickTime < clickDebounce) return@setOnClickListener
                lastClickTime = SystemClock.elapsedRealtime()

                onItemClick?.invoke(getItem(pos))
            }
        }

        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onLongClick.invoke(getItem(pos))
                } else false
            }
        }
        return holder
    }

    abstract fun createViewHolder(binding: VB): BaseViewHolder<T, VB>

    override fun onBindViewHolder(holder: BaseViewHolder<T, VB>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<T, VB>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindPayload(getItem(position), payloads)
        }
    }
}