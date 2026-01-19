package com.humoyun.musicapp

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.humoyun.musicapp.R

class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(5000)
            .setReadTimeoutMs(5000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(Util.getUserAgent(this, "HumoMusicApp"))

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
            )
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val openActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(pendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .apply {
                try {
                    val method =
                        this::class.java.getMethod("setSmallIconResourceId", Int::class.java)
                    method.invoke(this, R.drawable.ic_music_note)
                } catch (e: Exception) {
                    try {
                        val method = this::class.java.getMethod("setIcon", Int::class.java)
                        method.invoke(this, R.drawable.ic_music_note)
                    } catch (e2: Exception) {
                    }
                }
            }
            .build()

        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibraryService.MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Root")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                ).build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}