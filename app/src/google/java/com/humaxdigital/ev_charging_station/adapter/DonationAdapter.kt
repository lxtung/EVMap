package com.humaxdigital.ev_charging_station.adapter

import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.viewmodel.DonationItem

class DonationAdapter() : DataBindingAdapter<DonationItem>() {
    override fun getItemViewType(position: Int): Int = R.layout.item_donation
}