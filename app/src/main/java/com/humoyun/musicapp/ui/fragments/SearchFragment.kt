package com.humoyun.musicapp.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.cast.framework.CastButtonFactory
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.FragmentSearchBinding
import com.humoyun.musicapp.model.SearchResultItem
import com.humoyun.musicapp.ui.adapter.CategoryAdapter
import com.humoyun.musicapp.ui.adapter.UniversalSearchAdapter
import com.humoyun.musicapp.ui.viewmodel.SearchViewModel
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var binding: FragmentSearchBinding
    private val searchViewModel: SearchViewModel by viewModel()
    private val playerViewModel: SharedPlayerViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view)

        setupHeader()
        setupCategoryGrid()
        setupSearchResults()
        setupSearchInput()
        observeData()
    }

    private fun setupHeader() {
//        binding.btnCast.setOnClickListener {
//            Toast.makeText(context, "Cast device search...", Toast.LENGTH_SHORT).show()
//        }

        val mediaRouteButton = binding.root.findViewById<MediaRouteButton>(R.id.btnCast)
        CastButtonFactory.setUpMediaRouteButton(requireContext(), mediaRouteButton)
    }

    private fun setupCategoryGrid() {
        // Masonry Layout (Pinterest)
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        // Prevent items from jumping around when scrolling up
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE

        binding.rvCategories.layoutManager = layoutManager
        val adapter = CategoryAdapter { category ->
            // делаею вид что работает и просто заполняю инпут
            binding.etSearch.setText(category.name)
        }
        binding.rvCategories.adapter = adapter
    }

    private fun setupSearchResults() {
        val adapter = UniversalSearchAdapter(
            onMusicClick = { music ->
                val fullList = (binding.rvSearchResults.adapter as? UniversalSearchAdapter)
                    ?.items
                    ?.filterIsInstance<SearchResultItem.MusicItem>()
                    ?.map { it.music } ?: emptyList()

                val index = fullList.indexOfFirst { it.id == music.id }

                if (index != -1) {
                    playerViewModel.playMusic(fullList, index)
                }

            },
            onRadioClick = { radio ->
                Toast.makeText(context, "Playing Radio: ${radio.title}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvSearchResults.layoutManager = LinearLayoutManager(context)
        binding.rvSearchResults.adapter = adapter
    }

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                searchViewModel.onSearchQueryChanged(query)
                toggleViews(query.isNotEmpty())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun toggleViews(isSearching: Boolean) {
        if (isSearching) {
            binding.rvCategories.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
        } else {
            binding.rvCategories.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
            binding.tvNoResults.visibility = View.GONE
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.categories.collectLatest { list ->
                (binding.rvCategories.adapter as? CategoryAdapter)?.submitList(list)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.searchResults.collectLatest { list ->
                val adapter = binding.rvSearchResults.adapter as? UniversalSearchAdapter
                adapter?.submitList(list)

                if (binding.etSearch.text.isNotEmpty()) {
                    binding.tvNoResults.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}