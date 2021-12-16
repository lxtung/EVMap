package com.humaxdigital.ev_charging_station.fragment.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceFragmentCompat
import com.humaxdigital.ev_charging_station.MapsActivity
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.storage.PreferenceDataSource

abstract class BaseSettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    protected lateinit var prefs: PreferenceDataSource

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceDataSource(requireContext())
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val navController = findNavController()
        val toolbar = requireView().findViewById(R.id.toolbar) as Toolbar
        toolbar.setupWithNavController(
            navController,
            (requireActivity() as MapsActivity).appBarConfiguration
        )
    }

    override fun onPause() {
        preferenceManager.sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }
}