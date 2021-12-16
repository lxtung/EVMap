package com.humaxdigital.ev_charging_station.ui

import androidx.appcompat.app.AppCompatDelegate
import com.humaxdigital.ev_charging_station.storage.PreferenceDataSource

fun updateNightMode(prefs: PreferenceDataSource) {
    AppCompatDelegate.setDefaultNightMode(
        when (prefs.darkmode) {
            "on" -> AppCompatDelegate.MODE_NIGHT_YES
            "off" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}