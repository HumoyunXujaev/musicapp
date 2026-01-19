package com.humoyun.musicapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humoyun.musicapp.data.repository.RadioRepository
import com.humoyun.musicapp.model.Radio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RadioViewModel(private val repository: RadioRepository) : ViewModel() {
    private val _radios = MutableStateFlow<List<Radio>>(emptyList())
    val radios: StateFlow<List<Radio>> = _radios

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadRadios()
    }

    fun loadRadios() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getRadios().collect {
                _radios.value = it
                _isLoading.value = false
            }
        }
    }
}