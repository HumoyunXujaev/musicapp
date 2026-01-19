package com.humoyun.musicapp.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.FragmentListBinding
import com.humoyun.musicapp.model.Album
import com.humoyun.musicapp.model.Artist
import com.humoyun.musicapp.model.Folder
import com.humoyun.musicapp.ui.adapter.UniversalMediaAdapter
import com.humoyun.musicapp.ui.viewmodel.LocalViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class GenericGridFragment : Fragment(R.layout.fragment_list) {
    private val viewModel: LocalViewModel by activityViewModel()
    private var type: String = "ALBUMS"

    companion object {
        fun newInstance(type: String): GenericGridFragment {
            return GenericGridFragment().apply {
                arguments = Bundle().apply { putString("TYPE", type) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getString("TYPE") ?: "ALBUMS"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentListBinding.bind(view)

        binding.tvHeaderTitle.text = when (type) {
            "ALBUMS" -> "Albums"
            "ARTISTS" -> "Artists"
            "FOLDERS" -> "Folders"
            else -> "Library"
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val adapter = UniversalMediaAdapter { item ->
            when (item) {
                is Album -> navigateToDetails("ALBUM", item.id, item.title, "")
                is Artist -> navigateToDetails("ARTIST", item.id, item.name, "")
                is Folder -> navigateToDetails("FOLDER", -1, item.name, item.path)
            }
        }
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            when (type) {
                "ALBUMS" -> viewModel.albums.collectLatest { list ->
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }

                "ARTISTS" -> viewModel.artists.collectLatest { list ->
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }

                "FOLDERS" -> viewModel.folders.collectLatest { list ->
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun navigateToDetails(type: String, id: Long, name: String, path: String) {
        val bundle = bundleOf("TYPE" to type, "ID" to id, "NAME" to name, "PATH" to path)
        findNavController().navigate(R.id.nav_playlist_detail, bundle)
    }
}