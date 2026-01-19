package com.humoyun.musicapp.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.humoyun.musicapp.ui.viewmodel.FavoritesViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoritesFragment : BaseListFragment() {
    private val viewModel: FavoritesViewModel by viewModel()

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoriteMusic.collectLatest { list -> updateList(list) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeaderTitle("Favorites")
    }
}