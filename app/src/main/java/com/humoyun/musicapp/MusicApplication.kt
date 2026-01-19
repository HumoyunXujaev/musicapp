package com.humoyun.musicapp

import android.app.Application
import com.humoyun.musicapp.di.appModule
import com.humoyun.musicapp.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MusicApplication)
            modules(appModule, networkModule)
        }
    }
}


