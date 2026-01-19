package com.humoyun.musicapp.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.FragmentOfflineContainerBinding
import com.humoyun.musicapp.ui.viewmodel.LocalViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LibraryFragment : Fragment(R.layout.fragment_offline_container) {

    private lateinit var binding: FragmentOfflineContainerBinding
    private val viewModel: LocalViewModel by activityViewModel()

    private val permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) viewModel.loadLibraryData()
        else Toast.makeText(context, "Permission needed for Library", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentOfflineContainerBinding.bind(view)

        val adapter = LibraryPagerAdapter(requireActivity())
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 5

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Favorites"
                1 -> "Tracks"
                2 -> "Online"
                3 -> "Albums"
                4 -> "Artists"
                5 -> "Folders"
                else -> ""
            }
        }.attach()

        // Check Permissions & Load Data
        if (ContextCompat.checkSelfPermission(
                requireContext(), permissionName
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadLibraryData()
        } else {
            requestPermissionLauncher.launch(permissionName)
        }
    }

    class LibraryPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 6

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FavoritesFragment()
                1 -> LocalTracksFragment()
                2 -> OnlineFragment()
                3 -> GenericGridFragment.newInstance("ALBUMS")
                4 -> GenericGridFragment.newInstance("ARTISTS")
                5 -> GenericGridFragment.newInstance("FOLDERS")
                else -> OnlineFragment()
            }
        }
    }

}