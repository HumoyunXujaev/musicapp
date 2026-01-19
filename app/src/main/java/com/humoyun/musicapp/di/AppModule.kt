package com.humoyun.musicapp.di

import androidx.room.Room
import com.humoyun.musicapp.MusicServiceConnection
import com.humoyun.musicapp.data.db.AppDatabase
import com.humoyun.musicapp.data.repository.MusicRepository
import com.humoyun.musicapp.data.source.LocalAudioDataSource
import com.humoyun.musicapp.data.source.RemoteAudioDataSource
import com.humoyun.musicapp.ui.manager.ThemeManager
import com.humoyun.musicapp.ui.viewmodel.FavoritesViewModel
import com.humoyun.musicapp.ui.viewmodel.HomeViewModel
import com.humoyun.musicapp.ui.viewmodel.LocalViewModel
import com.humoyun.musicapp.ui.viewmodel.SearchViewModel
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "music_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().favoritesDao() }
    single { get<AppDatabase>().userMusicDao() }

    single { LocalAudioDataSource(androidContext()) }
    single { RemoteAudioDataSource() }

    single {
        MusicRepository(
            context = androidContext(),
            localSource = get(),
            remoteSource = get(),
            favoritesDao = get(),
            userMusicDao = get()
        )
    }

    single { MusicServiceConnection(androidContext()) }
    single { ThemeManager(androidContext()) }

    viewModel { HomeViewModel(get()) }
    viewModel { LocalViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { SharedPlayerViewModel(get(), get(), androidContext()) }

}