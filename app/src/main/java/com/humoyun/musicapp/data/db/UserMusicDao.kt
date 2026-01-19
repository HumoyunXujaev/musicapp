package com.humoyun.musicapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserMusicDao {
    @Query("SELECT * FROM user_music ORDER BY timestamp DESC")
    suspend fun getAllUserMusic(): List<UserMusicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMusic(music: UserMusicEntity)
}