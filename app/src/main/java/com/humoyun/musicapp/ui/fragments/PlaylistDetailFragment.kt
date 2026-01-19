package com.humoyun.musicapp.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.humoyun.musicapp.ui.viewmodel.LocalViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class PlaylistDetailFragment : BaseListFragment() {
    private val viewModel: LocalViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = arguments?.getString("TYPE") ?: return
        val id = arguments?.getLong("ID") ?: -1L
        val name = arguments?.getString("NAME") ?: "Details"
        val path = arguments?.getString("PATH") ?: ""

        viewModel.clearDetails()

        when (type) {
            "ALBUM" -> {
                setHeaderTitle("Album: $name")
                viewModel.loadAlbumSongs(id)
            }

            "ARTIST" -> {
                setHeaderTitle("Artist: $name")
                viewModel.loadArtistSongs(id)
            }

            "FOLDER" -> {
                setHeaderTitle("Folder: $name")
                viewModel.loadFolderSongs(path)
            }
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detailSongs.collectLatest { list -> updateList(list) }
        }
    }
}