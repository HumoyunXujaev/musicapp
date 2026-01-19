package com.humoyun.musicapp

import android.content.ComponentName
import android.content.Context
import androidx.concurrent.futures.await
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicServiceConnection(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState = _playbackState.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private var mediaController: MediaController? = null

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError = _playerError.asStateFlow()

    init {
        scope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            try {
                mediaController = MediaController.Builder(context, sessionToken)
                    .buildAsync()
                    .await()

                mediaController?.addListener(PlayerListener())
                syncState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun syncState() {
        val player = mediaController ?: return
        _currentMediaItem.update { player.currentMediaItem }
        _duration.update { player.duration.coerceAtLeast(0) }
        _playbackState.update {
            if (player.isPlaying) PlaybackState.Playing else PlaybackState.Paused
        }
    }

    fun playMedia(mediaItems: List<MediaItem>, startIndex: Int = 0) {
        mediaController?.apply {
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        mediaController?.apply {
            val mediaId = currentMediaItem?.mediaId
            val isRadio = mediaId?.startsWith("radio_") == true

            if (isPlaying) {
                if (isRadio) {
                    stop()
                } else {
                    pause()
                }
            } else {
                if (isRadio) {
                    seekToDefaultPosition()
                    prepare()
                    play()
                } else {
                    play()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.update { positionMs }
    }

    fun skipTo(index: Int) {
        mediaController?.apply {
            seekToDefaultPosition(index)
            if (!isPlaying) play()
        }
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun updateCurrentPosition() {
        mediaController?.let { player ->
            _currentPosition.value = player.currentPosition
        }
    }

    private inner class PlayerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED
                )
            ) {
                syncState()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val message = "Playback Error: ${error.message ?: "Unknown error"}"
            _playerError.value = message
        }
    }

    fun clearError() {
        _playerError.value = null
    }
}

sealed class PlaybackState {
    object Idle : PlaybackState()
    object Playing : PlaybackState()
    object Paused : PlaybackState()
}