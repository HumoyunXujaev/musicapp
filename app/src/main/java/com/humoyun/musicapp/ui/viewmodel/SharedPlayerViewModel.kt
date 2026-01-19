package com.humoyun.musicapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.humoyun.musicapp.MusicServiceConnection
import com.humoyun.musicapp.PlaybackState
import com.humoyun.musicapp.data.repository.MusicRepository
import com.humoyun.musicapp.model.Music
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class PlayerEvent {
    data class ShowToast(val message: String) : PlayerEvent()
    data class ShowErrorDialog(val title: String, val message: String) : PlayerEvent()
    data object ExpandPlayer : PlayerEvent()
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentMediaId: String? = null,
    val currentTitle: String = "Not Playing",
    val currentArtist: String = "",
    val currentArtworkUri: Uri? = null,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val isFavorite: Boolean = false,
    val isRadio: Boolean = false,
    val isPlayerVisible: Boolean = false,
    val currentPlaylist: List<Music> = emptyList(),
    val waveform: List<Float> = emptyList()
)

class SharedPlayerViewModel(
    private val musicServiceConnection: MusicServiceConnection,
    private val repository: MusicRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playerEvent = Channel<PlayerEvent>()
    val playerEvent = _playerEvent.receiveAsFlow()

    private var progressJob: Job? = null
    private var currentExtractionJob: Job? = null

    init {
        observeService()
        observeFavorites()
        startProgressUpdater()
        observeErrors()
    }

    private fun observeService() {
        viewModelScope.launch {
            musicServiceConnection.currentMediaItem.collectLatest { item ->
                val mediaId = item?.mediaId
                val isRadio = mediaId?.startsWith("radio_") == true

                _uiState.update { currentState ->
                    currentState.copy(
                        currentMediaId = mediaId,
                        currentTitle = item?.mediaMetadata?.title?.toString() ?: "Not Playing",
                        currentArtist = item?.mediaMetadata?.artist?.toString() ?: "",
                        currentArtworkUri = item?.mediaMetadata?.artworkUri,
                        isRadio = isRadio,
                        isPlayerVisible = mediaId != null,
                        // Reset waveform momentarily, then generate new one
                        waveform = emptyList(),
                        currentPlaylist = updatePlaylistState(
                            currentState.currentPlaylist,
                            mediaId,
                            currentState.isPlaying
                        )
                    )
                }

                if (mediaId != null && !isRadio) {
                    generateInstantWaveform()
                }
            }
        }

        viewModelScope.launch {
            musicServiceConnection.playbackState.collectLatest { state ->
                val isPlaying = state is PlaybackState.Playing
                _uiState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        currentPlaylist = updatePlaylistState(
                            it.currentPlaylist,
                            it.currentMediaId,
                            isPlaying
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            musicServiceConnection.duration.collectLatest { dur ->
                _uiState.update { it.copy(duration = dur) }
            }
        }

        viewModelScope.launch {
            musicServiceConnection.currentPosition.collectLatest { pos ->
                _uiState.update { it.copy(currentPosition = pos) }
            }
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isPlaying && !_uiState.value.isRadio) {
                    musicServiceConnection.updateCurrentPosition()
                }
                delay(200L)
            }
        }
    }

    private fun generateInstantWaveform() {
        currentExtractionJob?.cancel()
        currentExtractionJob = viewModelScope.launch {
            // Generate 60 to 100 bars
            val count = 80
            val randomWave = List(count) {
                0.3f + (Random.nextFloat() * 0.7f)
            }

            if (isActive) {
                _uiState.update { it.copy(waveform = randomWave) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            _uiState.map { it.currentMediaId }
                .distinctUntilChanged()
                .flatMapLatest { mediaId ->
                    if (mediaId == null) flowOf(false) else repository.isFavorite(mediaId)
                }
                .collectLatest { isFav ->
                    _uiState.update { it.copy(isFavorite = isFav) }
                }
        }
    }

    private fun observeErrors() {
        viewModelScope.launch {
            musicServiceConnection.playerError.collectLatest { errorMsg ->
                if (errorMsg != null) {
                    if (_uiState.value.isRadio) {
                        _playerEvent.send(
                            PlayerEvent.ShowErrorDialog("Station Unavailable", "Trying next...")
                        )
                    } else {
                        _playerEvent.send(PlayerEvent.ShowToast(errorMsg))
                    }
                    musicServiceConnection.clearError()
                }
            }
        }
    }

    fun playMusic(musicList: List<Music>, index: Int) {
        val selectedMusic = musicList.getOrNull(index) ?: return

        _uiState.update { it.copy(currentPlaylist = musicList) }

        if (selectedMusic.id == _uiState.value.currentMediaId) {
            musicServiceConnection.togglePlayPause()
            viewModelScope.launch { _playerEvent.send(PlayerEvent.ExpandPlayer) }
            return
        }

        val mediaItems = musicList.map { music ->
            MediaItem.Builder()
                .setMediaId(music.id)
                .setUri(music.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(music.title)
                        .setArtist(music.artist)
                        .setArtworkUri(music.albumArtUri)
                        .build()
                )
                .build()
        }

        musicServiceConnection.playMedia(mediaItems, index)
        viewModelScope.launch { _playerEvent.send(PlayerEvent.ExpandPlayer) }
    }

    fun toggleFavorite() {
        val currentId = _uiState.value.currentMediaId ?: return
        val music =
            _uiState.value.currentPlaylist.find { it.id == currentId } ?: createFallbackMusic()

        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                repository.removeFromFavorites(music)
                _playerEvent.send(PlayerEvent.ShowToast("Removed from favorites"))
            } else {
                repository.addToFavorites(music)
                _playerEvent.send(PlayerEvent.ShowToast("Added to favorites"))
            }
        }
    }

    fun onPlayPauseClick() = musicServiceConnection.togglePlayPause()
    fun onNextClick() = musicServiceConnection.skipToNext()
    fun onPreviousClick() = musicServiceConnection.skipToPrevious()
    fun onSeekTo(pos: Long) = musicServiceConnection.seekTo(pos)

    fun skipTo(index: Int) {
        if (index in _uiState.value.currentPlaylist.indices) {
            musicServiceConnection.skipTo(index)
        }
    }

    private fun updatePlaylistState(
        playlist: List<Music>,
        mediaId: String?,
        isPlaying: Boolean
    ): List<Music> {
        if (playlist.isEmpty()) return emptyList()
        return playlist.map { music ->
            val isCurrent = music.id == mediaId
            if (music.isCurrent != isCurrent || (isCurrent && music.isPlaying != isPlaying)) {
                music.copy(isCurrent = isCurrent, isPlaying = if (isCurrent) isPlaying else false)
            } else {
                music
            }
        }
    }

    private fun createFallbackMusic(): Music {
        val s = _uiState.value
        return Music(
            s.currentMediaId ?: "",
            s.currentTitle,
            s.currentArtist,
            s.duration,
            Uri.EMPTY,
            s.currentArtworkUri ?: Uri.EMPTY
        )
    }
}