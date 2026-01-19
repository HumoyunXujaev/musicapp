package com.humoyun.musicapp.ui.adapter

import android.graphics.Color
import android.view.View
import coil.load
import coil.size.Scale
import com.humoyun.musicapp.R
import com.humoyun.musicapp.core.base.BaseListAdapter
import com.humoyun.musicapp.core.base.BaseViewHolder
import com.humoyun.musicapp.databinding.ItemMusicBinding
import com.humoyun.musicapp.model.Music

class MusicAdapter(
    onItemClick: (Music) -> Unit
) : BaseListAdapter<Music, ItemMusicBinding>(
    bindingFactory = ItemMusicBinding::inflate,
    onItemClick = onItemClick
) {

    override fun createViewHolder(binding: ItemMusicBinding): BaseViewHolder<Music, ItemMusicBinding> {
        return MusicViewHolder(binding)
    }

    fun updatePlayingState(mediaId: String?, isPlaying: Boolean) {
        val currentList = currentList
        if (currentList.isEmpty()) return

        val newList = currentList.map { music ->
            val isCurrent = music.id == mediaId
            if (music.isCurrent != isCurrent || (isCurrent && music.isPlaying != isPlaying)) {
                music.copy(isCurrent = isCurrent, isPlaying = if (isCurrent) isPlaying else false)
            } else {
                music
            }
        }

        if (currentList != newList) {
            submitList(newList)
        }
    }

    inner class MusicViewHolder(binding: ItemMusicBinding) :
        BaseViewHolder<Music, ItemMusicBinding>(binding) {
        override fun bind(item: Music) = with(binding) {
            tvTitle.text = item.title
            tvArtist.text = item.artist

            ivIcon.load(item.albumArtUri) {
                crossfade(true)
                scale(Scale.FILL)
                error(R.drawable.ic_music_note)
                placeholder(R.drawable.ic_music_note)
            }

            updateVisualState(item)
        }

        override fun bindPayload(item: Music, payloads: List<Any>) {
            updateVisualState(item)
        }

        private fun updateVisualState(item: Music) = with(binding) {
            if (item.isCurrent) {
                tvTitle.setTextColor(context.getColor(R.color.humo_primary))
                lottieWave.visibility = View.VISIBLE
                lottieWave.alpha = 1f
                if (item.isPlaying) {
                    if (!lottieWave.isAnimating) lottieWave.playAnimation()
                } else {
                    lottieWave.pauseAnimation()
                }
            } else {
                tvTitle.setTextColor(Color.WHITE)
                lottieWave.visibility = View.GONE
                lottieWave.cancelAnimation()
                lottieWave.progress = 0f
            }
        }
    }
}