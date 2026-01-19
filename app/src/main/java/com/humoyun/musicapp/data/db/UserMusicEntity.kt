package com.humoyun.musicapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_music")
data class UserMusicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val contentUri: String,
    val albumArtUri: String,
    val duration: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)