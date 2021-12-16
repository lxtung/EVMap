package com.humaxdigital.ev_charging_station.fragment.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.humaxdigital.ev_charging_station.R


class SettingsFragment : BaseSettingsFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {

    }
}