package com.humoyun.musicapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.humoyun.musicapp.R
import com.humoyun.musicapp.databinding.BottomSheetThemeBinding
import com.humoyun.musicapp.databinding.ItemThemeOptionBinding
import com.humoyun.musicapp.ui.manager.AppTheme
import com.humoyun.musicapp.ui.manager.ThemeManager
import org.koin.android.ext.android.inject

class ThemeBottomSheet : BottomSheetDialogFragment() {

    private val themeManager: ThemeManager by inject()
    private lateinit var binding: BottomSheetThemeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvThemes.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvThemes.adapter =
            ThemeAdapter(AppTheme.entries, themeManager.getCurrentTheme()) { selectedTheme ->
                themeManager.setTheme(selectedTheme, requireActivity())
                dismiss()
            }
    }

    inner class ThemeAdapter(
        private val themes: List<AppTheme>,
        private val currentTheme: AppTheme,
        private val onClick: (AppTheme) -> Unit
    ) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
            return ThemeViewHolder(
                ItemThemeOptionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
            holder.bind(themes[position])
        }

        override fun getItemCount() = themes.size

        inner class ThemeViewHolder(private val binding: ItemThemeOptionBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(theme: AppTheme) {
                binding.tvThemeName.text = theme.title

                val colorRes = when (theme) {
                    AppTheme.BLUE -> R.color.theme_blue_primary
                    AppTheme.RED -> R.color.theme_red_primary
                    AppTheme.GREEN -> R.color.theme_green_primary
                    AppTheme.PURPLE -> R.color.theme_purple_primary
                    AppTheme.AMOLED -> R.color.white
                }

                val context = binding.root.context
                val color = ContextCompat.getColor(context, colorRes)
                binding.viewPrimary.setBackgroundColor(color)

                if (theme == currentTheme) {
                    binding.ivCheck.visibility = View.VISIBLE
                    val typedValue = TypedValue()
                    context.theme.resolveAttribute(R.attr.humoPrimary, typedValue, true)
                    binding.cardColor.strokeColor = typedValue.data
                    binding.cardColor.strokeWidth = 6
                } else {
                    binding.ivCheck.visibility = View.GONE
                    binding.cardColor.strokeColor = Color.parseColor("#33FFFFFF")
                    binding.cardColor.strokeWidth = 2
                }

                binding.root.setOnClickListener { onClick(theme) }
            }
        }
    }
}