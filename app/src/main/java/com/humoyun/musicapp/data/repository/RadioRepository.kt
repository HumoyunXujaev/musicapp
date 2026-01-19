package com.humoyun.musicapp.data.repository

import com.humoyun.musicapp.data.api.RadioApi
import com.humoyun.musicapp.model.Radio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class RadioRepository(private val api: RadioApi) {
    fun getRadios(): Flow<List<Radio>> = flow {
        try {
            val response = api.getRadios()
            if (response.code == 200) {
                val radios = response.data.map { dto ->
                    Radio(
                        id = dto.id,
                        title = dto.title,
                        fmNumber = dto.fmNumber,
                        imageUrl = dto.imageUrl,
                        streamUrl = dto.streamUrl,
                        priority = dto.priority
                    )
                }.sortedBy { it.priority }
                emit(radios)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}