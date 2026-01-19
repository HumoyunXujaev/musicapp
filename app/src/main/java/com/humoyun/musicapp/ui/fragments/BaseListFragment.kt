package com.humoyun.musicapp.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.FragmentListBinding
import com.humoyun.musicapp.model.Music
import com.humoyun.musicapp.ui.adapter.MusicAdapter
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

abstract class BaseListFragment : Fragment(R.layout.fragment_list) {

    protected lateinit var binding: FragmentListBinding
    protected val playerViewModel: SharedPlayerViewModel by activityViewModel()

    protected val musicAdapter: MusicAdapter by lazy {
        MusicAdapter { music ->
            val list = musicAdapter.currentList
            val index = list.indexOfFirst { it.id == music.id }
            if (index != -1) {
                playerViewModel.playMusic(list, index)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentListBinding.bind(view)

        setupRecyclerView()
        observePlayerState()
        observeData()
    }

    protected fun setHeaderTitle(title: String) {
        binding.tvHeaderTitle.text = title
    }

    protected fun scrollToTop() {
        binding.recyclerView.stopScroll()
        binding.recyclerView.smoothScrollToPosition(0)
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = musicAdapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0
                    ) {
                        onLoadMore()
                    }
                }
            }
        })
    }

    private fun observePlayerState() {
        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.uiState.collectLatest { state ->
                musicAdapter.updatePlayingState(state.currentMediaId, state.isPlaying)
            }
        }
    }

    protected fun updateList(list: List<Music>) {
        musicAdapter.submitList(ArrayList(list))
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    abstract fun observeData()
    open fun onLoadMore() {}
}