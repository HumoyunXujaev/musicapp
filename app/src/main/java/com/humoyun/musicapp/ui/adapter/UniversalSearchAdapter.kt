package com.humoyun.musicapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.ItemMusicBinding
import com.humoyun.musicapp.databinding.ItemRadioBinding
import com.humoyun.musicapp.model.Music
import com.humoyun.musicapp.model.Radio
import com.humoyun.musicapp.model.SearchResultItem

class UniversalSearchAdapter(
    private val onMusicClick: (Music) -> Unit,
    private val onRadioClick: (Radio) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<SearchResultItem>()

    fun submitList(newItems: List<SearchResultItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SearchResultItem.MusicItem -> R.layout.item_music
            is SearchResultItem.RadioItem -> R.layout.item_radio
            is SearchResultItem.Header -> 0 // Not implemented
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_radio -> {
                RadioViewHolder(ItemRadioBinding.inflate(inflater, parent, false))
            }

            else -> {
                MusicViewHolder(ItemMusicBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SearchResultItem.MusicItem -> (holder as MusicViewHolder).bind(item.music)
            is SearchResultItem.RadioItem -> (holder as RadioViewHolder).bind(item.radio)
            else -> {}
        }
    }

    override fun getItemCount(): Int = items.size

    inner class MusicViewHolder(val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Music) = with(binding) {
            tvTitle.text = item.title
            tvArtist.text = item.artist
            ivIcon.load(item.albumArtUri) {
                crossfade(true)
                placeholder(R.drawable.ic_music_note)
                transformations(RoundedCornersTransformation(16f))
            }
            root.setOnClickListener { onMusicClick(item) }
        }
    }

    inner class RadioViewHolder(val binding: ItemRadioBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Radio) = with(binding) {
            tvTitle.text = item.title
            tvFm.text = item.fmNumber
            ivRadio.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_circle_gray)
                transformations(RoundedCornersTransformation(16f))
            }
            root.setOnClickListener { onRadioClick(item) }
        }
    }
}