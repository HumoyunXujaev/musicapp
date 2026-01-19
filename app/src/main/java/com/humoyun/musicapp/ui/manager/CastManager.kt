package com.humoyun.musicapp.ui.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import com.humoyun.musicapp.MusicServiceConnection
import com.humoyun.musicapp.model.Music
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CastManager(
    private val context: Context,
    private val viewModel: SharedPlayerViewModel,
    private val musicServiceConnection: MusicServiceConnection
) {
    private var castContext: CastContext? = null
    private var currentSession: CastSession? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var observerJob: Job? = null

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            currentSession = session
            onCastConnected()
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionEnded(session: CastSession, error: Int) {
            currentSession = null
            onCastDisconnected()
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            currentSession = session
            onCastConnected()
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    fun init() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(
                sessionListener,
                CastSession::class.java
            )
        } catch (e: Exception) {
            Log.e("CastManager", "Cast not available on this device", e)
        }
    }

    private fun onCastConnected() {
        Toast.makeText(context, "Connected to Cast Device", Toast.LENGTH_SHORT).show()

        observerJob?.cancel()
        observerJob = scope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.currentMediaId != null && currentSession != null && currentSession!!.isConnected) {
                    val currentMusic = state.currentPlaylist.find { it.id == state.currentMediaId }
                    if (currentMusic != null) {
                        loadMediaToCast(currentMusic, state.currentPosition, state.isPlaying)
                    }
                }
            }
        }
    }

    private fun onCastDisconnected() {
        observerJob?.cancel()
        Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show()
    }

    private var lastLoadedMediaId: String? = null

    private fun loadMediaToCast(music: Music, currentPosition: Long, isPlaying: Boolean) {
        if (music.id == lastLoadedMediaId) return

        // Chromecast cannot play local file paths (content://) directly
        // unless you run a local HTTP server.
        if (!music.isOnline && !music.contentUri.toString().startsWith("http")) {
            Toast.makeText(context, "Cannot cast local files without a server", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        metadata.putString(MediaMetadata.KEY_TITLE, music.title)
        metadata.putString(MediaMetadata.KEY_ARTIST, music.artist)
        metadata.addImage(WebImage(music.albumArtUri))

        val mediaInfo = MediaInfo.Builder(music.contentUri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mpeg")
            .setMetadata(metadata)
            .setStreamDuration(music.duration)
            .build()

        val remoteMediaClient = currentSession?.remoteMediaClient ?: return

        // Pause local playback since we are moving to TV
        musicServiceConnection.togglePlayPause()

        val options = MediaLoadOptions.Builder()
            .setAutoplay(true)
            .setPlayPosition(currentPosition)
            .build()

        remoteMediaClient.load(mediaInfo, options)
        lastLoadedMediaId = music.id
    }
}