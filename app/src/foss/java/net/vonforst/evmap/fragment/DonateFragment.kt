package com.humaxdigital.ev_charging_station.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.humaxdigital.ev_charging_station.MapsActivity
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.databinding.FragmentDonateBinding

class DonateFragment : Fragment() {
    private lateinit var binding: FragmentDonateBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDonateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        binding.btnDonate.setOnClickListener {
            (activity as? MapsActivity)?.openUrl(getString(R.string.paypal_link))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.toolbar.setupWithNavController(
            findNavController(),
            (requireActivity() as MapsActivity).appBarConfiguration
        )
    }
}