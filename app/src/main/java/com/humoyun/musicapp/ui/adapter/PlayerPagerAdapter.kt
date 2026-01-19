package com.humoyun.musicapp.ui.adapter

import coil.load
import coil.transform.RoundedCornersTransformation
import com.humoyun.musicapp.R
import com.humoyun.musicapp.core.base.BaseListAdapter
import com.humoyun.musicapp.core.base.BaseViewHolder
import com.humoyun.musicapp.databinding.ItemPlayerArtworkBinding
import com.humoyun.musicapp.model.Music

class PlayerPagerAdapter : BaseListAdapter<Music, ItemPlayerArtworkBinding>(
    bindingFactory = ItemPlayerArtworkBinding::inflate
) {
    override fun createViewHolder(binding: ItemPlayerArtworkBinding): BaseViewHolder<Music, ItemPlayerArtworkBinding> {
        return ArtworkViewHolder(binding)
    }

    class ArtworkViewHolder(binding: ItemPlayerArtworkBinding) :
        BaseViewHolder<Music, ItemPlayerArtworkBinding>(binding) {
        override fun bind(item: Music) {
            binding.ivArtworkPage.load(item.albumArtUri) {
                crossfade(true)
                placeholder(R.drawable.ic_music_note)
                error(R.drawable.ic_music_note)
                transformations(RoundedCornersTransformation(32f))
            }
        }
    }
}