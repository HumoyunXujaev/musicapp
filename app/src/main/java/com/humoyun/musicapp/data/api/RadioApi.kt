package com.humoyun.musicapp.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)

data class RadioDto(
    val id: Int,
    val title: String,
    val fmNumber: String,
    val priority: Int,
    val imageUrl: String,
    val streamUrl: String
)

interface RadioApi {
    @GET("radios")
    suspend fun getRadios(): ApiResponse<List<RadioDto>>

    @GET("radios/{id}")
    suspend fun getRadioDetails(@Path("id") id: Int): ApiResponse<RadioDto>
}