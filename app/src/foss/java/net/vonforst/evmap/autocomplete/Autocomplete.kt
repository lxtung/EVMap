package com.humaxdigital.ev_charging_station.autocomplete

import android.content.Context

fun getAutocompleteProviders(context: Context) = listOf(MapboxAutocompleteProvider(context))