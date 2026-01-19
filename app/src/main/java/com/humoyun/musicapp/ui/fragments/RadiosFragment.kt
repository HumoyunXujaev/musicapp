package com.humoyun.musicapp.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.FragmentListBinding
import com.humoyun.musicapp.model.Music
import com.humoyun.musicapp.model.Radio
import com.humoyun.musicapp.ui.adapter.RadioAdapter
import com.humoyun.musicapp.ui.viewmodel.RadioViewModel
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class RadiosFragment : Fragment(R.layout.fragment_list) {
    private val viewModel: RadioViewModel by viewModel()
    private val playerViewModel: SharedPlayerViewModel by activityViewModel()
    private lateinit var binding: FragmentListBinding

    private val radioAdapter: RadioAdapter by lazy {
        RadioAdapter { radio ->
            playRadio(radio, radioAdapter.currentList)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentListBinding.bind(view)
        binding.tvHeaderTitle.text = "Live Radios from itrack"

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = radioAdapter
        binding.recyclerView.setPadding(0, 0, 0, 300)

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.radios.collectLatest { list ->
                radioAdapter.submitList(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.uiState.collectLatest { state ->
                radioAdapter.updatePlayingState(state.currentMediaId, state.isPlaying)
            }
        }
    }

    private fun playRadio(currentRadio: Radio, allRadios: List<Radio>) {
        val musicList = allRadios.map { radioItem ->
            Music(
                id = "radio_${radioItem.id}",
                title = radioItem.title,
                artist = radioItem.fmNumber,
                duration = 0,
                contentUri = Uri.parse(radioItem.streamUrl),
                albumArtUri = Uri.parse(radioItem.imageUrl),
                isOnline = true
            )
        }
        val startIndex = musicList.indexOfFirst { it.id == "radio_${currentRadio.id}" }
        if (startIndex != -1) {
            playerViewModel.playMusic(musicList, startIndex)
        }
    }
}