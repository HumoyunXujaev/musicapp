package com.humoyun.musicapp.ui.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.mediarouter.app.MediaRouteButton
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.gms.cast.framework.CastButtonFactory
import com.humoyun.musicapp.R
import com.humoyun.musicapp.core.base.BaseListAdapter
import com.humoyun.musicapp.core.base.BaseViewHolder
import com.humoyun.musicapp.databinding.FragmentHomeBinding
import com.humoyun.musicapp.databinding.ItemHomeGridBinding
import com.humoyun.musicapp.databinding.ItemHomeHorizontalBinding
import com.humoyun.musicapp.databinding.ItemHomeSmallCardBinding
import com.humoyun.musicapp.model.Music
import com.humoyun.musicapp.ui.viewmodel.HomeViewModel
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import com.humoyun.musicapp.utils.bounceClick
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.random.Random

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private val homeViewModel: HomeViewModel by viewModel()
    private val playerViewModel: SharedPlayerViewModel by activityViewModel()

    private var isStateRestored = false

    private val gridAdapter by lazy {
        HomeGridAdapter { music ->
            playMusic(music, homeViewModel.onlineMusic.value.take(4))
        }
    }

    private val smallGridAdapter by lazy {
        HomeSmallCardAdapter { music ->
            playMusic(
                music,
                homeViewModel.onlineMusic.value.drop(4).take(4)
            )
        }
    }

    private val horizAdapter1 by lazy {
        HomeHorizontalAdapter { music ->
            playMusic(
                music,
                homeViewModel.onlineMusic.value.drop(9).take(10)
            )
        }
    }

    private val horizAdapter2 by lazy {
        HomeHorizontalAdapter { music ->
            playMusic(
                music,
                homeViewModel.onlineMusic.value.drop(19)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)
        isStateRestored = false

        setupHeader()
        setupAdapters()
        setupAuraButton()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        activity?.findViewById<View>(R.id.headerContainer)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        val scrollView = getScrollView()
        if (scrollView != null) {
            homeViewModel.scrollY = scrollView.scrollY
        }
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        activity?.findViewById<View>(R.id.headerContainer)?.visibility = View.VISIBLE
    }

    private fun getScrollView(): NestedScrollView? {
        if (binding.root is NestedScrollView) return binding.root as NestedScrollView
        return findNestedScrollView(binding.root)
    }

    private fun findNestedScrollView(view: View): NestedScrollView? {
        if (view is NestedScrollView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = findNestedScrollView(view.getChildAt(i))
                if (child != null) return child
            }
        }
        return null
    }

    private fun setupHeader() {
        val mediaRouteButton = binding.root.findViewById<MediaRouteButton>(R.id.btnCast)
        CastButtonFactory.setUpMediaRouteButton(requireContext(), mediaRouteButton)
//        binding.btnCast.setOnClickListener {
//            Toast.makeText(context, "Cast not implemented", Toast.LENGTH_SHORT).show()
//        }
        binding.btnInbox.setOnClickListener {
            Toast.makeText(context, "Inbox is empty", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotif.setOnClickListener {
            Toast.makeText(context, "No notifications", Toast.LENGTH_SHORT).show()
        }
        binding.tvMixedForUser.text = "Mixed for User"
    }

    private fun setupAdapters() {
        binding.rvGrid.adapter = gridAdapter
        binding.rvSmallGrid.adapter = smallGridAdapter
        binding.rvHorizontal1.adapter = horizAdapter1
        binding.rvHorizontal2.adapter = horizAdapter2
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.onlineMusic.collectLatest { fullList ->
                    if (fullList.isNotEmpty()) {
                        distributeData(fullList)
                        if (!isStateRestored && homeViewModel.scrollY > 0) {
                            val scrollView = getScrollView()
                            scrollView?.post { scrollView.scrollTo(0, homeViewModel.scrollY) }
                            isStateRestored = true
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.uiState.collectLatest { state ->
                    val currentId = state.currentMediaId
                    val isPlaying = state.isPlaying

                    updateBigCardState(currentId, isPlaying)
                    gridAdapter.updatePlayingState(currentId, isPlaying)
                    smallGridAdapter.updatePlayingState(currentId, isPlaying)
                    horizAdapter1.updatePlayingState(currentId, isPlaying)
                    horizAdapter2.updatePlayingState(currentId, isPlaying)
                }
            }
        }
    }

    private fun distributeData(list: List<Music>) {
        val gridItems = list.take(4)
        gridAdapter.submitList(gridItems)

        if (list.size > 4) {
            val smallGridItems = list.drop(4).take(4)
            smallGridAdapter.submitList(smallGridItems)
        }

        if (list.size > 8) {
            val bigItem = list[8]
            binding.cardBig.visibility = View.VISIBLE
            bindBigCard(bigItem)
        } else {
            binding.cardBig.visibility = View.GONE
        }

        if (list.size > 9) {
            val horiz1Items = list.drop(9).take(10)
            horizAdapter1.submitList(horiz1Items)

            if (list.size > 19) {
                val horiz2Items = list.drop(19)
                horizAdapter2.submitList(horiz2Items)
            }
        }
    }

    private fun bindBigCard(music: Music) {
        binding.tvBigTitle.text = music.title
        binding.tvBigArtist.text = music.artist
        val likes = Random.nextInt(100, 50000)
        binding.tvLikesCount.text = "$likes people just liked this track"

        binding.ivBigArt.load(music.albumArtUri) {
            crossfade(true)
            placeholder(R.drawable.ic_music_note)
            transformations(RoundedCornersTransformation(8f))
        }

        binding.btnBigPlay.setOnClickListener {
            it.bounceClick { playMusic(music, listOf(music)) }
        }
    }

    private fun updateBigCardState(currentMediaId: String?, isPlaying: Boolean) {
        val bigMusic = homeViewModel.onlineMusic.value.getOrNull(8) ?: return
        if (bigMusic.id == currentMediaId) {
            val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            binding.btnBigPlay.setImageResource(iconRes)
            binding.tvBigTitle.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.humo_primary
                )
            )
        } else {
            binding.btnBigPlay.setImageResource(R.drawable.ic_play)
            binding.tvBigTitle.setTextColor(Color.WHITE)
        }
    }

    private fun setupAuraButton() {
        binding.btnGetAura.setOnClickListener {
            triggerAuraEffect()
        }
        binding.layoutAuraResult.setOnClickListener {
            binding.layoutAuraResult.visibility = View.GONE
            binding.btnGetAura.text = "GET AURA"
            binding.btnGetAura.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.humo_primary)
            )
        }
    }

    private fun triggerAuraEffect() {
        val imageUrl =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT0OQnNJ87AscvqbDltgaMOzUn01k2WDlRf0w&s"
        val view = binding.root
        val button = binding.btnGetAura
        val overlay = binding.viewFlashOverlay

        val shakeX = ObjectAnimator.ofFloat(
            view, "translationX", 0f, 25f, -25f, 20f, -20f, 15f, -15f, 6f, -6f, 0f
        )
        val shakeY = ObjectAnimator.ofFloat(
            view, "translationY", 0f, 25f, -25f, 20f, -20f, 15f, -15f, 6f, -6f, 0f
        )
        shakeX.duration = 3200
        shakeY.duration = 3500

        val rotateBtn = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f * 3)
        val scaleXBtn = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.5f, 1f)
        val scaleYBtn = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.5f, 1f)
        rotateBtn.duration = 3500
        rotateBtn.interpolator = AccelerateDecelerateInterpolator()

        overlay.setBackgroundColor(Color.parseColor("#88D500F9"))
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f

        val flashIn = ObjectAnimator.ofFloat(overlay, "alpha", 0f, 0.8f)
        flashIn.duration = 1200
        val flashOut = ObjectAnimator.ofFloat(overlay, "alpha", 0.8f, 0f)
        flashOut.duration = 1800
        flashOut.startDelay = 100

        val glitchTexts =
            listOf("G̶E̶T̶ ̶A̶U̶R̶A̶", "∑RRØR", "AURA +9999", "SYSTEM FAIL", "AURA MODE")
        val handler = Handler(Looper.getMainLooper())
        var delay = 80L

        glitchTexts.forEach { text ->
            handler.postDelayed({
                button.text = text
                button.setTextColor(randomColor())
            }, delay)
            delay += 100
        }

        handler.postDelayed({
            button.text = "..."
            overlay.visibility = View.GONE
            binding.ivAuraResult.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_circle_gray)
                transformations(RoundedCornersTransformation(32f))
            }
            binding.tvAuraResult.text = "No money - No aura"
            binding.layoutAuraResult.alpha = 0f
            binding.layoutAuraResult.visibility = View.VISIBLE
            binding.layoutAuraResult.animate().alpha(1f).setDuration(500).start()
        }, delay + 1200)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(shakeX, shakeY, rotateBtn, scaleXBtn, scaleYBtn, flashIn, flashOut)
        animatorSet.start()
    }

    private fun randomColor(): Int {
        val colors = listOf(Color.RED, Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.WHITE)
        return colors.random()
    }

    private fun playMusic(music: Music, contextList: List<Music>) {
        val index = contextList.indexOfFirst { it.id == music.id }
        if (index != -1) {
            playerViewModel.playMusic(contextList, index)
        }
    }

    abstract class BaseHomeAdapter<VB : androidx.viewbinding.ViewBinding>(
        bindingFactory: (android.view.LayoutInflater, android.view.ViewGroup, Boolean) -> VB,
        onItemClick: (Music) -> Unit
    ) : BaseListAdapter<Music, VB>(bindingFactory, onItemClick) {

        private var currentMediaId: String? = null
        private var isPlaying: Boolean = false

        fun updatePlayingState(mediaId: String?, playing: Boolean) {
            val oldId = currentMediaId
            currentMediaId = mediaId
            isPlaying = playing

            currentList.forEachIndexed { index, music ->
                if (music.id == mediaId || music.id == oldId) {
                    notifyItemChanged(index, "PAYLOAD_PLAYBACK_STATE")
                }
            }
        }

        protected fun isItemPlaying(item: Music): Boolean = item.id == currentMediaId && isPlaying
        protected fun isItemCurrent(item: Music): Boolean = item.id == currentMediaId
    }

    class HomeGridAdapter(private val onClick: (Music) -> Unit) :
        BaseHomeAdapter<ItemHomeGridBinding>(ItemHomeGridBinding::inflate, onClick) {
        override fun createViewHolder(binding: ItemHomeGridBinding) =
            object : BaseViewHolder<Music, ItemHomeGridBinding>(binding) {
                override fun bind(item: Music) {
                    binding.tvTitle.text = item.title
                    binding.tvArtist.text = item.artist
                    binding.ivArt.load(item.albumArtUri) {
                        crossfade(true)
                        placeholder(R.drawable.ic_music_note)
                        transformations(RoundedCornersTransformation(16f))
                    }
                    updateState(item)
                }

                override fun bindPayload(item: Music, payloads: List<Any>) {
                    updateState(item)
                }

                private fun updateState(item: Music) {
                    val isCurrent = isItemCurrent(item)
                    val isPlaying = isItemPlaying(item)

                    binding.overlayPlaying.isVisible = isCurrent
                    binding.lottieWave.isVisible = isCurrent

                    if (isCurrent) {
                        binding.tvTitle.setTextColor(context.getColor(R.color.humo_primary))
                        if (isPlaying) binding.lottieWave.playAnimation() else binding.lottieWave.pauseAnimation()
                    } else {
                        binding.tvTitle.setTextColor(Color.WHITE)
                        binding.lottieWave.cancelAnimation()
                    }
                }
            }
    }

    class HomeSmallCardAdapter(private val onClick: (Music) -> Unit) :
        BaseHomeAdapter<ItemHomeSmallCardBinding>(ItemHomeSmallCardBinding::inflate, onClick) {
        override fun createViewHolder(binding: ItemHomeSmallCardBinding) =
            object : BaseViewHolder<Music, ItemHomeSmallCardBinding>(binding) {
                override fun bind(item: Music) {
                    binding.tvTitle.text = item.title
                    binding.tvArtist.text = item.artist
                    binding.ivArt.load(item.albumArtUri) {
                        crossfade(true)
                        placeholder(R.drawable.ic_music_note)
                        transformations(RoundedCornersTransformation(12f))
                    }
                    updateState(item)
                }

                override fun bindPayload(item: Music, payloads: List<Any>) {
                    updateState(item)
                }

                private fun updateState(item: Music) {
                    val isCurrent = isItemCurrent(item)
                    binding.overlayPlaying.isVisible = isCurrent
                    binding.lottieWave.isVisible = isCurrent

                    if (isCurrent && isItemPlaying(item)) {
                        binding.lottieWave.playAnimation()
                    } else {
                        binding.lottieWave.pauseAnimation()
                    }
                }
            }
    }

    class HomeHorizontalAdapter(private val onClick: (Music) -> Unit) :
        BaseHomeAdapter<ItemHomeHorizontalBinding>(ItemHomeHorizontalBinding::inflate, onClick) {
        override fun createViewHolder(binding: ItemHomeHorizontalBinding) =
            object : BaseViewHolder<Music, ItemHomeHorizontalBinding>(binding) {
                override fun bind(item: Music) {
                    binding.tvTitle.text = item.title
                    binding.tvArtist.text = item.artist
                    binding.ivArt.load(item.albumArtUri) {
                        crossfade(true)
                        placeholder(R.drawable.ic_music_note)
                        transformations(RoundedCornersTransformation(16f))
                    }
                    updateState(item)
                }

                override fun bindPayload(item: Music, payloads: List<Any>) {
                    updateState(item)
                }

                private fun updateState(item: Music) {
                    val isCurrent = isItemCurrent(item)
                    binding.overlayPlaying.isVisible = isCurrent
                    binding.lottieWave.isVisible = isCurrent

                    if (isCurrent) {
                        binding.tvTitle.setTextColor(context.getColor(R.color.humo_primary))
                        if (isItemPlaying(item)) binding.lottieWave.playAnimation() else binding.lottieWave.pauseAnimation()
                    } else {
                        binding.tvTitle.setTextColor(Color.WHITE)
                        binding.lottieWave.cancelAnimation()
                    }
                }
            }
    }
}