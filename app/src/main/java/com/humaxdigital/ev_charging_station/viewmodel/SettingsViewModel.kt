package com.humaxdigital.ev_charging_station.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.humaxdigital.ev_charging_station.api.chargeprice.ChargepriceApi
import com.humaxdigital.ev_charging_station.api.chargeprice.ChargepriceCar
import com.humaxdigital.ev_charging_station.api.chargeprice.ChargepriceTariff
import com.humaxdigital.ev_charging_station.storage.AppDatabase
import java.io.IOException

class SettingsViewModel(application: Application, chargepriceApiKey: String) :
    AndroidViewModel(application) {
    private var api = ChargepriceApi.create(chargepriceApiKey)
    private var db = AppDatabase.getInstance(application)

    val vehicles: MutableLiveData<Resource<List<ChargepriceCar>>> by lazy {
        MutableLiveData<Resource<List<ChargepriceCar>>>().apply {
            value = Resource.loading(null)
            loadVehicles()
        }
    }

    val tariffs: MutableLiveData<Resource<List<ChargepriceTariff>>> by lazy {
        MutableLiveData<Resource<List<ChargepriceTariff>>>().apply {
            value = Resource.loading(null)
            loadTariffs()
        }
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            try {
                val result = api.getVehicles()
                vehicles.value = Resource.success(result)
            } catch (e: IOException) {
                vehicles.value = Resource.error(e.message, null)
            }
        }
    }

    private fun loadTariffs() {
        viewModelScope.launch {
            try {
                val result = api.getTariffs()
                tariffs.value = Resource.success(result)
            } catch (e: IOException) {
                tariffs.value = Resource.error(e.message, null)
            }
        }
    }

    fun deleteRecentSearchResults() {
        viewModelScope.launch {
            db.recentAutocompletePlaceDao().deleteAll()
        }
    }
}