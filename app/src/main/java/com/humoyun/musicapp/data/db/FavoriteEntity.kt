package com.humoyun.musicapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val contentUri: String,
    val albumArtUri: String,
)