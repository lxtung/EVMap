package com.humaxdigital.ev_charging_station.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.car2go.maps.model.LatLng
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.humaxdigital.ev_charging_station.adapter.Equatable
import com.humaxdigital.ev_charging_station.api.availability.ChargeLocationStatus
import com.humaxdigital.ev_charging_station.api.availability.ChargepointStatus
import com.humaxdigital.ev_charging_station.api.availability.getAvailability
import com.humaxdigital.ev_charging_station.model.ChargeLocation
import com.humaxdigital.ev_charging_station.storage.AppDatabase
import com.humaxdigital.ev_charging_station.utils.distanceBetween
import kotlinx.coroutines.awaitAll

class FavoritesViewModel(application: Application, geApiKey: String) :
    AndroidViewModel(application) {
    private var db = AppDatabase.getInstance(application)

    val favorites: LiveData<List<ChargeLocation>> by lazy {
        db.chargeLocationsDao().getAllChargeLocations()
    }

    val location: MutableLiveData<LatLng> by lazy {
        MutableLiveData<LatLng>()
    }

    val availability: MediatorLiveData<Map<Long, Resource<ChargeLocationStatus>>> by lazy {
        MediatorLiveData<Map<Long, Resource<ChargeLocationStatus>>>().apply {
            addSource(favorites) { chargers ->
                if (chargers != null) {
                    viewModelScope.launch {
                        val data = hashMapOf<Long, Resource<ChargeLocationStatus>>()
                        chargers.forEach { charger ->
                            data[charger.id] = Resource.loading(null)
                        }
                        availability.value = data

                        chargers.map { charger ->
                            async {
                                data[charger.id] = getAvailability(charger)
                                availability.value = data
                            }
                        }.awaitAll()
                    }
                } else {
                    value = null
                }
            }
        }
    }

    val listData: MediatorLiveData<List<FavoritesListItem>> by lazy {
        MediatorLiveData<List<FavoritesListItem>>().apply {
            val callback = { _: Any ->
                listData.value = favorites.value?.map { charger ->
                    FavoritesListItem(
                        charger,
                        totalAvailable(charger.id),
                        charger.chargepoints.sumBy { it.count },
                        location.value.let { loc ->
                            if (loc == null) null else {
                                distanceBetween(
                                    loc.latitude,
                                    loc.longitude,
                                    charger.coordinates.lat,
                                    charger.coordinates.lng
                                )
                            }
                        })
                }?.sortedBy { it.distance }
            }
            addSource(favorites, callback)
            addSource(location, callback)
            addSource(availability, callback)
        }
    }

    data class FavoritesListItem(
        val charger: ChargeLocation,
        val available: Resource<List<ChargepointStatus>>,
        val total: Int,
        val distance: Double?
    ) : Equatable

    private fun totalAvailable(id: Long): Resource<List<ChargepointStatus>> {
        val availability = availability.value?.get(id) ?: return Resource.error(null, null)
        if (availability.status != Status.SUCCESS) {
            return Resource(availability.status, null, availability.message)
        } else {
            val values = availability.data?.status?.values ?: return Resource.error(null, null)
            return Resource.success(values.flatten())
        }
    }

    fun insertFavorite(charger: ChargeLocation) {
        viewModelScope.launch {
            db.chargeLocationsDao().insert(charger)
        }
    }

    fun deleteFavorite(charger: ChargeLocation) {
        viewModelScope.launch {
            db.chargeLocationsDao().delete(charger)
        }
    }
}