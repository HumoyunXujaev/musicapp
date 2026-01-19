package com.humoyun.musicapp.ui.adapter

import android.graphics.Color
import android.view.View
import coil.load
import coil.transform.RoundedCornersTransformation
import com.humoyun.musicapp.core.base.BaseListAdapter
import com.humoyun.musicapp.core.base.BaseViewHolder
import com.humoyun.musicapp.databinding.ItemRadioBinding
import com.humoyun.musicapp.model.Radio

class RadioAdapter(
    onItemClick: (Radio) -> Unit
) : BaseListAdapter<Radio, ItemRadioBinding>(
    bindingFactory = ItemRadioBinding::inflate,
    onItemClick = onItemClick
) {

    fun updatePlayingState(mediaId: String?, isPlaying: Boolean) {
        val currentList = currentList
        if (currentList.isEmpty()) return

        val newList = currentList.map { radio ->
            val radioMediaId = "radio_${radio.id}"
            val isCurrent = radioMediaId == mediaId

            if (radio.isCurrent != isCurrent || (isCurrent && radio.isPlaying != isPlaying)) {
                radio.copy(isCurrent = isCurrent, isPlaying = if (isCurrent) isPlaying else false)
            } else {
                radio
            }
        }
        if (currentList != newList) {
            submitList(newList)
        }
    }

    override fun createViewHolder(binding: ItemRadioBinding): BaseViewHolder<Radio, ItemRadioBinding> {
        return RadioViewHolder(binding)
    }

    inner class RadioViewHolder(binding: ItemRadioBinding) :
        BaseViewHolder<Radio, ItemRadioBinding>(binding) {
        override fun bind(item: Radio) = with(binding) {
            tvTitle.text = item.title
            tvFm.text = item.fmNumber

            ivRadio.load(item.imageUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(16f))
            }

            if (item.isCurrent) {
                tvTitle.setTextColor(Color.parseColor("#2979FF"))
                lottieWave.visibility = View.VISIBLE
                if (item.isPlaying) lottieWave.playAnimation() else lottieWave.pauseAnimation()
            } else {
                tvTitle.setTextColor(Color.WHITE)
                lottieWave.visibility = View.INVISIBLE
                lottieWave.cancelAnimation()
            }
        }

        override fun bindPayload(item: Radio, payloads: List<Any>) {
            bind(item)
        }
    }
}