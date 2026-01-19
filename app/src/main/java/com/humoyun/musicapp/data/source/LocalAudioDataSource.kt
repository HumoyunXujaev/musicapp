package com.humoyun.musicapp.data.source

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.humoyun.musicapp.model.Album
import com.humoyun.musicapp.model.Artist
import com.humoyun.musicapp.model.Folder
import com.humoyun.musicapp.model.Music
import java.io.File

class LocalAudioDataSource(private val context: Context) {

    fun getLocalTracks(): List<Music> {
        return queryMusic(null, null)
    }

    fun getAlbumSongs(albumId: Long): List<Music> {
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        return queryMusic(selection, selectionArgs)
    }

    fun getArtistSongs(artistId: Long): List<Music> {
        val selection = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(artistId.toString())
        return queryMusic(selection, selectionArgs)
    }

    fun getFolderSongs(folderPath: String): List<Music> {
        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath/%")
        return queryMusic(selection, selectionArgs)
    }

    fun getAlbums(): List<Album> {
        val albumList = mutableListOf<Album>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Albums.ALBUM} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val songsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumUri = ContentUris.withAppendedId(collection, id)

                    albumList.add(
                        Album(
                            id = id,
                            title = cursor.getString(nameCol) ?: "Unknown Album",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            artworkUri = albumUri,
                            trackCount = cursor.getInt(songsCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return albumList
    }

    fun getArtists(): List<Artist> {
        val artistList = mutableListOf<Artist>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Artists.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Artists.ARTIST} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val tracksCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

                while (cursor.moveToNext()) {
                    artistList.add(
                        Artist(
                            id = cursor.getLong(idCol),
                            name = cursor.getString(nameCol) ?: "Unknown Artist",
                            trackCount = cursor.getInt(tracksCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return artistList
    }

    fun getFolders(): List<Folder> {
        val foldersMap = mutableMapOf<String, Folder>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                collection, projection, selection, null, null
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (cursor.moveToNext()) {
                    val filePath = cursor.getString(dataCol)
                    if (filePath != null) {
                        val file = File(filePath)
                        val parentFile = file.parentFile
                        if (parentFile != null && parentFile.exists()) {
                            val folderPath = parentFile.absolutePath
                            val folderName = parentFile.name

                            val current = foldersMap[folderPath]
                            if (current != null) {
                                foldersMap[folderPath] =
                                    current.copy(trackCount = current.trackCount + 1)
                            } else {
                                foldersMap[folderPath] = Folder(folderPath, folderName, 1)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return foldersMap.values.toList().sortedBy { it.name }
    }

    private fun queryMusic(selection: String?, selectionArgs: Array<String>?): List<Music> {
        val musicList = mutableListOf<Music>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val baseSelection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val finalSelection =
            if (selection != null) "$baseSelection AND $selection" else baseSelection

        try {
            context.contentResolver.query(
                collection,
                projection,
                finalSelection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    val artworkUri = ContentUris.withAppendedId(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL)
                        else MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        albumId
                    )

                    val path = cursor.getString(dataCol)
                    if (path != null && File(path).exists()) {
                        musicList.add(
                            Music(
                                id = id.toString(),
                                title = cursor.getString(titleCol) ?: "Unknown",
                                artist = cursor.getString(artistCol) ?: "Unknown Artist",
                                duration = cursor.getLong(durationCol),
                                contentUri = contentUri,
                                albumArtUri = artworkUri,
                                isOnline = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return musicList
    }
}