package com.humoyun.musicapp.ui.adapter

import android.graphics.Color
import android.view.View
import com.humoyun.musicapp.R
import com.humoyun.musicapp.core.base.BaseItem
import com.humoyun.musicapp.core.base.BaseListAdapter
import com.humoyun.musicapp.core.base.BaseViewHolder
import com.humoyun.musicapp.databinding.ItemMusicBinding
import com.humoyun.musicapp.model.Album
import com.humoyun.musicapp.model.Artist
import com.humoyun.musicapp.model.Folder
import coil.load
import coil.transform.RoundedCornersTransformation

class UniversalMediaAdapter(
    onItemClick: (BaseItem) -> Unit
) : BaseListAdapter<BaseItem, ItemMusicBinding>(
    bindingFactory = ItemMusicBinding::inflate,
    onItemClick = onItemClick
) {

    override fun createViewHolder(binding: ItemMusicBinding): BaseViewHolder<BaseItem, ItemMusicBinding> {
        return MediaViewHolder(binding)
    }

    inner class MediaViewHolder(binding: ItemMusicBinding) :
        BaseViewHolder<BaseItem, ItemMusicBinding>(binding) {
        override fun bind(item: BaseItem) = with(binding) {
            lottieWave.visibility = View.GONE
            ivIcon.visibility = View.VISIBLE

            when (item) {
                is Album -> {
                    tvTitle.text = item.title
                    tvArtist.text = "${item.trackCount} Songs • ${item.artist}"
                    ivIcon.load(item.artworkUri) {
                        crossfade(true)
                        placeholder(R.drawable.ic_launcher_foreground)
                        transformations(RoundedCornersTransformation(16f))
                    }
                }

                is Artist -> {
                    tvTitle.text = item.name
                    tvArtist.text = "${item.trackCount} Tracks"
                    ivIcon.setImageResource(R.drawable.ic_launcher_foreground)
                }

                is Folder -> {
                    tvTitle.text = item.name
                    tvArtist.text = "${item.trackCount} Files"
                    ivIcon.setImageResource(R.drawable.ic_library)
                    ivIcon.setColorFilter(Color.GRAY)
                }
            }
        }
    }
}