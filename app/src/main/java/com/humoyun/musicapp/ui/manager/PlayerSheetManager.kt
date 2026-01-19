package com.humoyun.musicapp.ui.manager

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.LayoutBottomSheetPlayerBinding
import com.humoyun.musicapp.ui.adapter.PlayerPagerAdapter
import com.humoyun.musicapp.ui.viewmodel.PlayerEvent
import com.humoyun.musicapp.ui.viewmodel.PlayerUiState
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import com.humoyun.musicapp.utils.RealBlurTransformation
import com.humoyun.musicapp.utils.bounceClick
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class PlayerSheetManager(
    private val activity: AppCompatActivity,
    private val binding: LayoutBottomSheetPlayerBinding,
    private val viewModel: SharedPlayerViewModel,
    private val onSheetStateChanged: (Boolean) -> Unit,
    private val onSlide: ((Float) -> Unit)? = null
) {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var isProgrammaticScroll = false
    private var isPlayerSynced = false
    private var isUserSeeking = false

    private val density = activity.resources.displayMetrics.density
    private val isPortrait =
        activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    private val bottomNavHeight = if (isPortrait) (80 * density).toInt() else 0
    private val miniPlayerHeight = (92 * density).toInt()

    fun setup(savedInstanceState: Bundle?) {
        setupBottomSheet()
        setupControls()
        observeState()
        binding.root.post { restoreState(savedInstanceState) }
    }

    fun saveState(outState: Bundle) {
        if (::bottomSheetBehavior.isInitialized) {
            outState.putInt("sheet_state", bottomSheetBehavior.state)
        }
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (!::bottomSheetBehavior.isInitialized) return
        val savedState =
            savedInstanceState?.getInt("sheet_state") ?: BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = miniPlayerHeight + bottomNavHeight

        if (viewModel.uiState.value.currentMediaId != null) {
            if (savedState == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.fullPlayerContainer.alpha = 1f
                binding.fullPlayerContainer.visibility = View.VISIBLE
                binding.miniPlayerCard?.visibility = View.INVISIBLE
                onSheetStateChanged(true)
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.fullPlayerContainer.visibility = View.GONE
                binding.miniPlayerCard?.visibility = View.VISIBLE
                binding.miniPlayerCard?.alpha = 1f
                onSheetStateChanged(false)
            }
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            onSheetStateChanged(false)
        }
    }

    fun handleBackPress(): Boolean {
        if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
        ) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return false
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.root)
        bottomSheetBehavior.peekHeight = miniPlayerHeight + bottomNavHeight
        bottomSheetBehavior.isHideable = true

        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.miniPlayerCard?.alpha = 1f - slideOffset
                binding.fullPlayerContainer.alpha = slideOffset

                if (slideOffset > 0.5f) {
                    binding.miniPlayerCard?.visibility = View.INVISIBLE
                    binding.fullPlayerContainer.visibility = View.VISIBLE
                } else {
                    binding.miniPlayerCard?.visibility = View.VISIBLE
                    binding.fullPlayerContainer.visibility = View.VISIBLE
                }
                onSlide?.invoke(slideOffset)
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val isExpanded = newState == BottomSheetBehavior.STATE_EXPANDED
                onSheetStateChanged(isExpanded)

                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.fullPlayerContainer.visibility = View.GONE
                    binding.miniPlayerCard?.visibility = View.VISIBLE
                    binding.miniPlayerCard?.alpha = 1f
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    val state = viewModel.uiState.value
                    loadArtwork(state.currentArtworkUri)
                }
            }
        })
    }

    private fun renderPlayerState(state: PlayerUiState) {
        if (state.currentMediaId == null) {
            bottomSheetBehavior.isHideable = true
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        } else {
            bottomSheetBehavior.isHideable = false
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                binding.root.post {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }

        binding.tvMiniTitle.text = state.currentTitle
        binding.tvMiniArtist.text = state.currentArtist
        binding.tvFullTitle.text = state.currentTitle
        binding.tvFullArtist.text = state.currentArtist

        loadArtwork(state.currentArtworkUri)

        val iconRes = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.btnMiniPlayPause.setImageResource(iconRes)
        binding.btnFullPlayPause.setImageResource(iconRes)

        val favColor = if (state.isFavorite) R.color.red_error else R.color.white
        val favTint = ContextCompat.getColor(activity, favColor)
        binding.btnFavorite.setColorFilter(favTint)
        binding.btnMiniFavorite?.setColorFilter(favTint)

        binding.circularProgress?.isVisible = !state.isRadio
        binding.layoutMusicProgress.isVisible = !state.isRadio
        binding.layoutRadioStatus.isVisible = state.isRadio

        if (state.isRadio) {
            binding.liveVisualizer.setPlaying(state.isPlaying)
        } else {
            binding.waveformSeekBar?.setWaveform(state.waveform, state.duration)
            if (!isUserSeeking) {
                binding.tvCurrentTime.text = formatDuration(state.currentPosition)
                binding.tvTotalTime.text = formatDuration(state.duration)
                binding.waveformSeekBar?.setProgress(state.currentPosition)
                val progressPercent =
                    if (state.duration > 0) state.currentPosition.toFloat() / state.duration else 0f
                binding.circularProgress?.progress = (progressPercent * 1000).toInt()
            }
        }

        updateViewPager(state)
    }

    private var lastLoadedArtUri: Uri? = null

    private fun loadArtwork(uri: Uri?) {
        if (uri == null || uri == lastLoadedArtUri) return
        lastLoadedArtUri = uri

        binding.ivMiniArtwork.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_music_note)
            size(150, 150)
        }
        binding.ivMiniBlurBackground?.load(uri) {
            crossfade(true)
            transformations(RealBlurTransformation(activity, radius = 25f, sampling = 4f))
        }
        binding.ivBlurredBackground.load(uri) {
            crossfade(true)
            transformations(RealBlurTransformation(activity, radius = 25f, sampling = 8f))
        }
    }

    private fun updateViewPager(state: PlayerUiState) {
        val adapter = binding.viewPagerPlayer.adapter as? PlayerPagerAdapter ?: return

        // If the playlist has changed, update the adapter
        if (adapter.currentList != state.currentPlaylist) {
            adapter.submitList(state.currentPlaylist)
        }

        // Find where the currently playing song is in the NEW list
        val currentIndex = state.currentPlaylist.indexOfFirst { it.id == state.currentMediaId }

        if (currentIndex != -1) {
            // If the song IS in the list, scroll to it
            if (binding.viewPagerPlayer.currentItem != currentIndex) {
                isProgrammaticScroll = true
                binding.viewPagerPlayer.setCurrentItem(currentIndex, false)
                isProgrammaticScroll = false
            }
            // Mark as synced so user swipes are registered as "Skip" commands
            isPlayerSynced = true
        } else {
            isPlayerSynced = false
        }
    }

    private fun setupControls() {
        binding.miniPlayerCard?.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding.btnCollapse?.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val pagerAdapter = PlayerPagerAdapter()
        binding.viewPagerPlayer.adapter = pagerAdapter
        binding.viewPagerPlayer.apply {
            adapter = pagerAdapter
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 3
            // val paddingPx = (40 * density).toInt()
            // setPadding(paddingPx, 0, paddingPx, 0)
            (getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
        }

        val compositePageTransformer = CompositePageTransformer()
        val marginPx = (40 * density).toInt()
        compositePageTransformer.addTransformer(MarginPageTransformer(marginPx))
        compositePageTransformer.addTransformer { page, position ->
            val absPos = abs(position)
            page.scaleY = 0.85f + (1 - absPos) * 0.15f
            page.alpha = 0.5f + (1 - absPos) * 0.5f
            page.rotation = position * -10f
        }
        binding.viewPagerPlayer.setPageTransformer(compositePageTransformer)

        binding.viewPagerPlayer.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Only skip to track if the scroll was NOT programmatic AND we are synced
                if (!isProgrammaticScroll && isPlayerSynced) {
                    val state = viewModel.uiState.value
                    val currentId = state.currentMediaId
                    val playlist = state.currentPlaylist
                    val selectedItem = playlist.getOrNull(position)

                    if (selectedItem != null && selectedItem.id != currentId) {
                        viewModel.skipTo(position)
                    }
                }
            }
        })

        val playPauseAction = View.OnClickListener { view ->
            view.bounceClick { viewModel.onPlayPauseClick() }
        }
        val favAction = View.OnClickListener { view ->
            view.bounceClick { viewModel.toggleFavorite() }
        }

        binding.btnMiniPlayPause.setOnClickListener(playPauseAction)
        binding.btnFullPlayPause.setOnClickListener(playPauseAction)
        binding.btnFavorite.setOnClickListener(favAction)
        binding.btnMiniFavorite?.setOnClickListener(favAction)

        binding.btnFullNext.setOnClickListener {
            it.bounceClick { viewModel.onNextClick() }
        }
        binding.btnFullPrevious.setOnClickListener {
            it.bounceClick { viewModel.onPreviousClick() }
        }

        binding.waveformSeekBar?.setOnSeekListener { seekPosition ->
            viewModel.onSeekTo(seekPosition)
            isUserSeeking = false
        }
    }

    private fun observeState() {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { renderPlayerState(it) }
                }
                launch {
                    viewModel.playerEvent.collectLatest { handleEvent(it) }
                }
            }
        }
    }

    private fun handleEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.ShowToast -> Toast.makeText(
                activity,
                event.message,
                Toast.LENGTH_SHORT
            ).show()

            is PlayerEvent.ExpandPlayer -> bottomSheetBehavior.state =
                BottomSheetBehavior.STATE_EXPANDED

            is PlayerEvent.ShowErrorDialog -> {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}