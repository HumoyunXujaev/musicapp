package com.humoyun.musicapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class, UserMusicEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun userMusicDao(): UserMusicDao
}