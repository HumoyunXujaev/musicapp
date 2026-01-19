package com.humoyun.musicapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(track: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(track: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavorite(id: String): Flow<Boolean>
}