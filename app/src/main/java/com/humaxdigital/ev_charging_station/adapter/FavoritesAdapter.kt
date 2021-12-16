package com.humaxdigital.ev_charging_station.adapter

import android.annotation.SuppressLint
import android.view.animation.AccelerateInterpolator
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.databinding.ItemFavoriteBinding
import com.humaxdigital.ev_charging_station.viewmodel.FavoritesViewModel

class FavoritesAdapter(val onDelete: (FavoritesViewModel.FavoritesListItem) -> Unit) :
    DataBindingAdapter<FavoritesViewModel.FavoritesListItem>() {
    init {
        setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_favorite

    override fun getItemId(position: Int): Long = getItem(position).charger.id

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(
        holder: ViewHolder<FavoritesViewModel.FavoritesListItem>,
        item: FavoritesViewModel.FavoritesListItem
    ) {
        super.bind(holder, item)

        val binding = holder.binding as ItemFavoriteBinding
        binding.foreground.translationX = 0f
        binding.btnDelete.setOnClickListener {
            binding.foreground.animate()
                .translationX(binding.foreground.width.toFloat())
                .setDuration(250)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    onDelete(item)
                }
                .start()
        }
    }
}