package com.humoyun.musicapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humoyun.musicapp.data.repository.MusicRepository
import com.humoyun.musicapp.model.Album
import com.humoyun.musicapp.model.Artist
import com.humoyun.musicapp.model.Folder
import com.humoyun.musicapp.model.Music
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LocalViewModel(private val repository: MusicRepository) : ViewModel() {

    private val _tracks = MutableStateFlow<List<Music>>(emptyList())
    val tracks: StateFlow<List<Music>> = _tracks

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders

    private val _detailSongs = MutableStateFlow<List<Music>>(emptyList())
    val detailSongs: StateFlow<List<Music>> = _detailSongs

    fun loadLibraryData() {
        viewModelScope.launch {
            launch { _tracks.value = repository.getLocalAudioFiles() }
            launch { _albums.value = repository.getAlbums() }
            launch { _artists.value = repository.getArtists() }
            launch { _folders.value = repository.getFolders() }
        }
    }

    fun loadAlbumSongs(albumId: Long) {
        viewModelScope.launch { _detailSongs.value = repository.getAlbumSongs(albumId) }
    }

    fun loadArtistSongs(artistId: Long) {
        viewModelScope.launch { _detailSongs.value = repository.getArtistSongs(artistId) }
    }

    fun loadFolderSongs(path: String) {
        viewModelScope.launch { _detailSongs.value = repository.getFolderSongs(path) }
    }

    fun clearDetails() {
        _detailSongs.value = emptyList()
    }
}