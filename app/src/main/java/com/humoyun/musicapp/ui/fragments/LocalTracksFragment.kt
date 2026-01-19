package com.humoyun.musicapp.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.humoyun.musicapp.ui.viewmodel.LocalViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LocalTracksFragment : BaseListFragment() {
    private val viewModel: LocalViewModel by activityViewModel()

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tracks.collectLatest { list -> updateList(list) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeaderTitle("Local Tracks")
    }
}