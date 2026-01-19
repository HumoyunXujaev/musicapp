package com.humoyun.musicapp.data.repository

import android.content.Context
import android.net.Uri
import com.humoyun.musicapp.data.db.FavoriteEntity
import com.humoyun.musicapp.data.db.FavoritesDao
import com.humoyun.musicapp.data.db.UserMusicDao
import com.humoyun.musicapp.data.db.UserMusicEntity
import com.humoyun.musicapp.data.source.LocalAudioDataSource
import com.humoyun.musicapp.data.source.RemoteAudioDataSource
import com.humoyun.musicapp.model.Album
import com.humoyun.musicapp.model.Artist
import com.humoyun.musicapp.model.Folder
import com.humoyun.musicapp.model.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val localSource: LocalAudioDataSource,
    private val remoteSource: RemoteAudioDataSource,
    private val favoritesDao: FavoritesDao,
    private val userMusicDao: UserMusicDao
) {

    enum class SortOption { TITLE, ARTIST, DURATION }

    suspend fun getLocalAudioFiles(): List<Music> = withContext(Dispatchers.IO) {
        localSource.getLocalTracks()
    }

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        localSource.getAlbums()
    }

    suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        localSource.getArtists()
    }

    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        localSource.getFolders()
    }

    suspend fun getAlbumSongs(albumId: Long): List<Music> = withContext(Dispatchers.IO) {
        localSource.getAlbumSongs(albumId)
    }

    suspend fun getArtistSongs(artistId: Long): List<Music> = withContext(Dispatchers.IO) {
        localSource.getArtistSongs(artistId)
    }

    suspend fun getFolderSongs(path: String): List<Music> = withContext(Dispatchers.IO) {
        localSource.getFolderSongs(path)
    }

    suspend fun getOnlineAudioFiles(
        page: Int,
        pageSize: Int,
        sortOption: SortOption = SortOption.TITLE
    ): List<Music> = withContext(Dispatchers.IO) {
        val dbUserMusic = userMusicDao.getAllUserMusic().map { entity ->
            Music(
                id = entity.id,
                title = entity.title,
                artist = entity.artist,
                duration = entity.duration,
                contentUri = Uri.parse(entity.contentUri),
                albumArtUri = Uri.parse(entity.albumArtUri),
                isOnline = true
            )
        }

        val remoteMusic = remoteSource.getOnlineCatalog(page, pageSize)

        val sortedRemote = if (sortOption == SortOption.TITLE) {
            remoteMusic.sortedBy { it.title }
        } else {
            remoteMusic
        }

        dbUserMusic + sortedRemote
    }


    suspend fun addUserOnlineMusic(title: String, artist: String, url: String, imageUri: Uri?) =
        withContext(Dispatchers.IO) {
            val newId = "user_${System.currentTimeMillis()}"
            var finalArtUri = "https://ui-avatars.com/api/?name=$title&background=random"

            if (imageUri != null) {
                try {
                    val fileName = "cover_${UUID.randomUUID()}.jpg"
                    val file = File(context.filesDir, fileName)
                    context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    finalArtUri = Uri.fromFile(file).toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Если не вышло сохранить, оставляем как есть или дефолт
                }
            }

            userMusicDao.insertUserMusic(
                UserMusicEntity(
                    id = newId,
                    title = title.ifBlank { "Unknown Title" },
                    artist = artist.ifBlank { "Imported Artist" },
                    contentUri = url,
                    albumArtUri = finalArtUri,
                    duration = 0L
                )
            )
        }

    fun getFavorites(): Flow<List<Music>> = favoritesDao.getAllFavorites().map { entities ->
        entities.map { entity ->
            Music(
                id = entity.id,
                title = entity.title,
                artist = entity.artist,
                duration = entity.duration,
                contentUri = Uri.parse(entity.contentUri),
                albumArtUri = Uri.parse(entity.albumArtUri),
                isOnline = entity.contentUri.startsWith("http")
            )
        }
    }

    suspend fun addToFavorites(music: Music) = withContext(Dispatchers.IO) {
        favoritesDao.addFavorite(
            FavoriteEntity(
                id = music.id,
                title = music.title,
                artist = music.artist,
                duration = music.duration,
                contentUri = music.contentUri.toString(),
                albumArtUri = music.albumArtUri.toString()
            )
        )
    }

    suspend fun removeFromFavorites(music: Music) = withContext(Dispatchers.IO) {
        favoritesDao.removeFavorite(
            FavoriteEntity(
                id = music.id,
                title = music.title,
                artist = music.artist,
                duration = music.duration,
                contentUri = music.contentUri.toString(),
                albumArtUri = music.albumArtUri.toString()
            )
        )
    }

    fun isFavorite(id: String): Flow<Boolean> = favoritesDao.isFavorite(id)
}