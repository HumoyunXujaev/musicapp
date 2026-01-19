package com.humoyun.musicapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humoyun.musicapp.data.repository.MusicRepository
import com.humoyun.musicapp.model.Music
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(repository: MusicRepository) : ViewModel() {
    val favoriteMusic: StateFlow<List<Music>> = repository.getFavorites().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), emptyList()
    )
}
