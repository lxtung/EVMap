package com.humaxdigital.ev_charging_station.adapter

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.ItemTouchHelper
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.databinding.ItemFilterProfileBinding
import com.humaxdigital.ev_charging_station.storage.FilterProfile

class FilterProfilesAdapter(
    val dragHelper: ItemTouchHelper,
    val onDelete: (FilterProfile) -> Unit,
    val onRename: (FilterProfile) -> Unit
) : DataBindingAdapter<FilterProfile>() {
    init {
        setHasStableIds(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(
        holder: ViewHolder<FilterProfile>,
        item: FilterProfile
    ) {
        super.bind(holder, item)

        val binding = holder.binding as ItemFilterProfileBinding
        binding.handle.setOnTouchListener { v, event ->
            if (event?.action == MotionEvent.ACTION_DOWN) {
                dragHelper.startDrag(holder)
            }
            false
        }
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
        binding.btnRename.setOnClickListener {
            onRename(item)
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_filter_profile
}