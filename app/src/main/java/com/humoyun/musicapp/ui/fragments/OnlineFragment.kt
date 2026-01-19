package com.humoyun.musicapp.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.humoyun.musicapp.R
import com.humoyun.musicapp.ui.viewmodel.HomeViewModel
import com.humoyun.musicapp.ui.viewmodel.UserImportData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnlineFragment : BaseListFragment() {
    private val viewModel: HomeViewModel by viewModel()

    private var activeImageRequestIndex = -1
    private val selectedImages = mutableMapOf<Int, Uri>()
    private val rowImageViews = mutableListOf<ImageView>()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null && activeImageRequestIndex != -1) {
                selectedImages[activeImageRequestIndex] = uri
                if (activeImageRequestIndex < rowImageViews.size) {
                    rowImageViews[activeImageRequestIndex].setImageURI(uri)
                    rowImageViews[activeImageRequestIndex].scaleType =
                        ImageView.ScaleType.CENTER_CROP
                }
            }
        }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.onlineMusic.collectLatest { list -> updateList(list) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading && musicAdapter.itemCount == 0) {
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onLoadMore() {
        viewModel.loadMoreMusic()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeaderTitle("Online")

        binding.fabAddUrl.apply {
            visibility = View.VISIBLE
            setImageResource(android.R.drawable.ic_input_add)
            setOnClickListener { showAddMusicDialog() }
        }
    }

    private fun showAddMusicDialog() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Add Music from URL")

        val scrollView = ScrollView(context)
        val containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }
        scrollView.addView(containerLayout)

        selectedImages.clear()
        rowImageViews.clear()
        val inputRows = mutableListOf<Triple<EditText, EditText, EditText>>()

        fun addInputRow(index: Int) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 48)
            }

            val contentLayout =
                LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

            val imagePreview = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(180, 180).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    marginEnd = 24
                }
                setImageResource(android.R.drawable.ic_menu_camera)
                setBackgroundColor(android.graphics.Color.DKGRAY)
                scaleType = ImageView.ScaleType.CENTER
                setOnClickListener {
                    activeImageRequestIndex = index
                    pickImageLauncher.launch("image/*")
                }
            }
            rowImageViews.add(imagePreview)

            val textsLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameInput = EditText(context).apply { hint = "Song Name" }
            val artistInput = EditText(context).apply { hint = "Artist Name" }
            val urlInput = EditText(context).apply {
                hint = "MP3 URL"
                inputType = InputType.TYPE_TEXT_VARIATION_URI
            }

            textsLayout.addView(nameInput)
            textsLayout.addView(artistInput)
            textsLayout.addView(urlInput)
            contentLayout.addView(imagePreview)
            contentLayout.addView(textsLayout)
            rowLayout.addView(contentLayout)

            rowLayout.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                setBackgroundColor(android.graphics.Color.DKGRAY)
            })

            containerLayout.addView(rowLayout)
            inputRows.add(Triple(nameInput, artistInput, urlInput))
        }

        addInputRow(0)

        val addMoreBtn = Button(context).apply {
            text = "+ Add Another Song"
            setOnClickListener {
                addInputRow(inputRows.size)
                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
            }
        }
        containerLayout.addView(addMoreBtn)

        builder.setView(scrollView)
        builder.setPositiveButton("Add All") { _, _ ->
            val validEntries = mutableListOf<UserImportData>()
            inputRows.forEachIndexed { index, (nameEt, artistEt, urlEt) ->
                val name = nameEt.text.toString().trim()
                val artist = artistEt.text.toString().trim()
                val url = urlEt.text.toString().trim()
                if (name.isNotEmpty() && url.isNotEmpty() && isValidUrl(url)) {
                    validEntries.add(UserImportData(name, artist, url, selectedImages[index]))
                }
            }
            if (validEntries.isNotEmpty()) {
                viewModel.importMusicFromUrls(validEntries)
                scrollToTop()
                Toast.makeText(context, "Imported ${validEntries.size} songs", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun isValidUrl(url: String): Boolean {
        return url.isNotEmpty() && Patterns.WEB_URL.matcher(url).matches() && url.endsWith(
            ".mp3",
            true
        )
    }
}