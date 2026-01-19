package com.humoyun.musicapp

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.humoyun.musicapp.databinding.ActivityMainBinding
import com.humoyun.musicapp.databinding.LayoutBottomSheetPlayerBinding
import com.humoyun.musicapp.ui.fragments.ThemeBottomSheet
import com.humoyun.musicapp.ui.manager.CastManager
import com.humoyun.musicapp.ui.manager.PlayerSheetManager
import com.humoyun.musicapp.ui.manager.ThemeManager
import com.humoyun.musicapp.ui.viewmodel.SharedPlayerViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val themeManager: ThemeManager by inject()
    private val playerViewModel: SharedPlayerViewModel by viewModel()
    private lateinit var playerSheetManager: PlayerSheetManager

    private val musicServiceConnection: MusicServiceConnection by inject()
    private var castManager: CastManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        themeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupNavigation()

        val playerBinding = LayoutBottomSheetPlayerBinding.bind(binding.playerSheet.root)

        playerSheetManager = PlayerSheetManager(
            activity = this,
            binding = playerBinding,
            viewModel = playerViewModel,
            onSheetStateChanged = { isExpanded ->
                binding.bottomNav?.let { nav ->
                    if (ViewCompat.isLaidOut(nav) && nav.height > 0) {
                        val translation = if (isExpanded) nav.height.toFloat() else 0f
                        nav.translationY = translation
                    } else {
                        nav.doOnLayout { view ->
                            val translation = if (isExpanded) view.height.toFloat() else 0f
                            view.translationY = translation
                        }
                    }
                }
            },
            onSlide = { slideOffset ->
                binding.bottomNav?.post {
                    val fraction = slideOffset.coerceIn(0f, 1f)
                    val navHeight = binding.bottomNav?.height?.toFloat() ?: 0f
                    binding.bottomNav?.translationY = navHeight * fraction
                }
            }
        )

        binding.btnTheme?.setOnClickListener {
            val bottomSheet = ThemeBottomSheet()
            bottomSheet.show(supportFragmentManager, "theme_sheet")
        }

        playerSheetManager.setup(savedInstanceState)
        setupBackPress()

        castManager = CastManager(this, playerViewModel, musicServiceConnection)
        // Execute on main thread to ensure lazy loading of Cast Context
        binding.root.post {
            castManager?.init()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::playerSheetManager.isInitialized) {
            playerSheetManager.saveState(outState)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val header = binding.root.findViewById<android.view.View>(R.id.headerContainer)
            header?.updatePadding(top = systemBars.top)
            binding.bottomNav?.updatePadding(bottom = 0)
            insets
        }
    }

    private fun setupNavigation() {
        val navView: NavigationBarView? =
            binding.bottomNav ?: binding.root.findViewById(R.id.navigation_rail)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navView?.setupWithNavController(navController)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!playerSheetManager.handleBackPress()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}