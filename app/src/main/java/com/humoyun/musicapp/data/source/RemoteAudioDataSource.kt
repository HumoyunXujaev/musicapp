package com.humoyun.musicapp.data.source

import android.net.Uri
import com.humoyun.musicapp.model.Music
import kotlinx.coroutines.delay

class RemoteAudioDataSource {

    // Симуляция получения данных
    suspend fun getOnlineCatalog(page: Int, pageSize: Int): List<Music> {
        delay(500) // Имитация задержки сети

        val list = mutableListOf<Music>()
        val startId = (page * pageSize) + 1
        val endId = startId + pageSize

        for (i in startId until endId) {
            val formattedIndex = String.format("%03d", i)
            list.add(
                Music(
                    id = "onl_$i",
                    title = "Online Song $formattedIndex",
                    artist = "Artist $i",
                    duration = (200000L..300000L).random(),
                    contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                    albumArtUri = Uri.parse("https://picsum.photos/seed/$i/300/300"),
                    isOnline = true
                )
            )
        }
        return list
    }
}