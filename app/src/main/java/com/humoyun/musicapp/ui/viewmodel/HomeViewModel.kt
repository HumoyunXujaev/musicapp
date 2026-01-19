package com.humoyun.musicapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humoyun.musicapp.data.repository.MusicRepository
import com.humoyun.musicapp.model.Music
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserImportData(
    val title: String,
    val artist: String,
    val url: String,
    val imageUri: Uri?
)

class HomeViewModel(private val repository: MusicRepository) : ViewModel() {

    private val _onlineMusic = MutableStateFlow<List<Music>>(emptyList())
    val onlineMusic: StateFlow<List<Music>> = _onlineMusic

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var scrollY: Int = 0

    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false
    private var currentSortOption = MusicRepository.SortOption.TITLE

    init {
        loadMoreMusic()
    }

    fun refresh() {
        currentPage = 0
        isLastPage = false
        _onlineMusic.value = emptyList()
        loadMoreMusic()
    }

    fun updateSortOption(option: MusicRepository.SortOption) {
        if (currentSortOption != option) {
            currentSortOption = option
            refresh()
        }
    }

    fun importMusicFromUrls(dataList: List<UserImportData>) {
        viewModelScope.launch {
            dataList.forEach { item ->
                repository.addUserOnlineMusic(item.title, item.artist, item.url, item.imageUri)
            }
            refresh()
        }
    }

    fun loadMoreMusic() {
        if (_isLoading.value || isLastPage) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val newItems =
                    repository.getOnlineAudioFiles(currentPage, pageSize, currentSortOption)
                if (newItems.size < pageSize) {
                    isLastPage = true
                }
                val currentList = _onlineMusic.value.toMutableList()
                currentList.addAll(newItems)
                _onlineMusic.value = currentList
                currentPage++
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}