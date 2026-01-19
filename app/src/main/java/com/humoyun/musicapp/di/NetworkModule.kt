package com.humoyun.musicapp.di

import com.humoyun.musicapp.data.api.RadioApi
import com.humoyun.musicapp.data.repository.RadioRepository
import com.humoyun.musicapp.ui.viewmodel.RadioViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://itrack.uz/api/v1/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioApi::class.java)
    }

    single { RadioRepository(get()) }
    viewModel { RadioViewModel(get()) }
}