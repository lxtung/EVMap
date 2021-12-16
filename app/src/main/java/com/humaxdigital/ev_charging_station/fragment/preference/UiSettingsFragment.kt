package com.humaxdigital.ev_charging_station.fragment.preference

import android.content.SharedPreferences
import android.os.Bundle
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.ui.updateNightMode

class UiSettingsFragment : BaseSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_ui, rootKey)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "language" -> {
                activity?.let {
                    it.finish();
                    it.startActivity(it.intent);
                }
            }
            "darkmode" -> {
                updateNightMode(prefs)
            }
        }
    }
}