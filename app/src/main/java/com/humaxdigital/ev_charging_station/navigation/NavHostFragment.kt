package com.humaxdigital.ev_charging_station.navigation

import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class NavHostFragment : NavHostFragment() {
    override fun onCreateNavController(navController: NavController) {
        super.onCreateNavController(navController)
        navController.navigatorProvider.addNavigator(
            CustomNavigator(
                requireContext()
            )
        )
    }
}