package com.humoyun.musicapp.ui.adapter

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import coil.load
import coil.transform.RoundedCornersTransformation
import com.humoyun.musicapp.R
import com.humoyun.musicapp.core.base.BaseListAdapter
import com.humoyun.musicapp.core.base.BaseViewHolder
import com.humoyun.musicapp.databinding.ItemCategoryMasonryBinding
import com.humoyun.musicapp.model.Category

class CategoryAdapter(
    private val onItemClick: (Category) -> Unit
) : BaseListAdapter<Category, ItemCategoryMasonryBinding>(
    bindingFactory = ItemCategoryMasonryBinding::inflate,
    onItemClick = onItemClick
) {
    override fun createViewHolder(binding: ItemCategoryMasonryBinding): BaseViewHolder<Category, ItemCategoryMasonryBinding> {
        return CategoryViewHolder(binding)
    }

    inner class CategoryViewHolder(binding: ItemCategoryMasonryBinding) :
        BaseViewHolder<Category, ItemCategoryMasonryBinding>(binding) {

        override fun bind(item: Category) {
            with(binding) {
                tvCategoryName.text = item.name

                //рандомка чтобы как в пинтересте было
                val params = ivCategory.layoutParams as ConstraintLayout.LayoutParams
                params.dimensionRatio = "1:${item.heightRatio}"
                ivCategory.layoutParams = params

                ivCategory.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_circle_gray)
                    transformations(RoundedCornersTransformation(16f))
                }
            }
        }
    }
}