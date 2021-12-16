package com.humaxdigital.ev_charging_station.autocomplete

import android.content.Context
import com.humaxdigital.ev_charging_station.storage.PreferenceDataSource

fun getAutocompleteProviders(context: Context) =
    if (PreferenceDataSource(context).searchProvider == "google") {
        listOf(GooglePlacesAutocompleteProvider(context), MapboxAutocompleteProvider(context))
    } else {
        listOf(MapboxAutocompleteProvider(context), GooglePlacesAutocompleteProvider(context))
    }