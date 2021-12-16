package com.humaxdigital.ev_charging_station.fragment.preference

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mikepenz.aboutlibraries.LibsBuilder
import com.humaxdigital.ev_charging_station.BuildConfig
import com.humaxdigital.ev_charging_station.MapsActivity
import com.humaxdigital.ev_charging_station.R


class AboutFragment : PreferenceFragmentCompat() {
    override fun onResume() {
        super.onResume()
        val toolbar = requireView().findViewById(R.id.toolbar) as Toolbar

        toolbar.setupWithNavController(
            findNavController(),
            (requireActivity() as MapsActivity).appBarConfiguration
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about, rootKey)

        findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            "github_link" -> {
                (activity as? MapsActivity)?.openUrl(getString(R.string.github_link))
                true
            }
            "privacy" -> {
                (activity as? MapsActivity)?.openUrl(getString(R.string.privacy_link))
                true
            }
            "faq" -> {
                (activity as? MapsActivity)?.openUrl(getString(R.string.faq_link))
                true
            }
            "oss_licenses" -> {
                LibsBuilder()
                    .withLicenseShown(true)
                    .withAboutVersionShown(false)
                    .withAboutIconShown(false)
                    .withActivityTitle(getString(R.string.oss_licenses))
                    .withExcludedLibraries()
                    .start(requireActivity())
                true
            }
            "donate" -> {
                findNavController().navigate(R.id.action_about_to_donateFragment)
                true
            }
            "github_sponsors" -> {
                findNavController().navigate(R.id.action_about_to_github_sponsors)
                true
            }
            "twitter" -> {
                (activity as? MapsActivity)?.openUrl(getString(R.string.twitter_url))
                true
            }
            "goingelectric" -> {
                (activity as? MapsActivity)?.openUrl(getString(R.string.goingelectric_forum_url))
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

}