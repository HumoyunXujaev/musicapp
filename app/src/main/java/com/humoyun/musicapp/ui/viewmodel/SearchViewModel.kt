package com.humoyun.musicapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humoyun.musicapp.data.repository.MusicRepository
import com.humoyun.musicapp.data.repository.RadioRepository
import com.humoyun.musicapp.model.Category
import com.humoyun.musicapp.model.SearchResultItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val radioRepository: RadioRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults

    init {
        loadCategories()
        observeSearch()
    }

    private fun loadCategories() {
        val cats = listOf(
            "Rock",
            "Pop",
            "Jazz",
            "Classical",
            "Hip Hop",
            "Electronic",
            "Ambient",
            "Folk",
            "Indie",
            "Metal",
            "Blues",
            "Country"
        )
        val list = cats.mapIndexed { index, name ->
            Category(
                id = index.toLong(),
                name = name,
                imageUrl = "https://picsum.photos/seed/${name}music/400/${
                    Random.nextInt(
                        400,
                        800
                    )
                }", // Random height for masonry effect
                heightRatio = Random.nextFloat() * 0.5f + 1.0f // Ratio between 1.0 and 1.5
            )
        }
        _categories.value = list
    }

    private fun observeSearch() {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        flow { emit(musicRepository.getLocalAudioFiles()) },
                        radioRepository.getRadios().catch { emit(emptyList()) },
                        flow { emit(musicRepository.getOnlineAudioFiles(0, 50)) }
                    ) { local, radios, online ->
                        val results = mutableListOf<SearchResultItem>()

                        // Filter
                        val filteredLocal = local.filter {
                            it.title.contains(query, true) || it.artist.contains(query, true)
                        }.take(5).map { SearchResultItem.MusicItem(it) }

                        val filteredRadios = radios.filter {
                            it.title.contains(query, true)
                        }.take(3).map { SearchResultItem.RadioItem(it) }

                        val filteredOnline = online.filter {
                            it.title.contains(query, true) || it.artist.contains(query, true)
                        }.take(10).map { SearchResultItem.MusicItem(it) }

                        // Combine results
                        if (filteredRadios.isNotEmpty()) {
                            results.addAll(filteredRadios)
                        }
                        if (filteredLocal.isNotEmpty()) {
                            results.addAll(filteredLocal)
                        }
                        if (filteredOnline.isNotEmpty()) {
                            results.addAll(filteredOnline)
                        }
                        results
                    }
                }
            }
            .onEach { _searchResults.value = it }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}