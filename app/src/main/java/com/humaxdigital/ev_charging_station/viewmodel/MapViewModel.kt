package com.humaxdigital.ev_charging_station.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.*
import com.car2go.maps.AnyMap
import com.car2go.maps.model.LatLng
import com.car2go.maps.model.LatLngBounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import com.humaxdigital.ev_charging_station.api.ChargepointApi
import com.humaxdigital.ev_charging_station.api.availability.ChargeLocationStatus
import com.humaxdigital.ev_charging_station.api.availability.getAvailability
import com.humaxdigital.ev_charging_station.api.createApi
import com.humaxdigital.ev_charging_station.api.goingelectric.GEChargepoint
import com.humaxdigital.ev_charging_station.api.goingelectric.GEReferenceData
import com.humaxdigital.ev_charging_station.api.goingelectric.GoingElectricApiWrapper
import com.humaxdigital.ev_charging_station.api.openchargemap.OCMConnection
import com.humaxdigital.ev_charging_station.api.openchargemap.OCMReferenceData
import com.humaxdigital.ev_charging_station.api.openchargemap.OpenChargeMapApiWrapper
import com.humaxdigital.ev_charging_station.api.stringProvider
import com.humaxdigital.ev_charging_station.autocomplete.PlaceWithBounds
import com.humaxdigital.ev_charging_station.model.*
import com.humaxdigital.ev_charging_station.storage.AppDatabase
import com.humaxdigital.ev_charging_station.storage.FilterProfile
import com.humaxdigital.ev_charging_station.storage.PreferenceDataSource
import com.humaxdigital.ev_charging_station.ui.cluster
import com.humaxdigital.ev_charging_station.utils.distanceBetween
import java.io.IOException

@Parcelize
data class MapPosition(val bounds: LatLngBounds, val zoom: Float) : Parcelable

internal fun getClusterDistance(zoom: Float): Int? {
    return when (zoom) {
        in 0.0..7.0 -> 100
        in 7.0..11.5 -> 75
        in 11.5..12.5 -> 60
        in 12.5..13.0 -> 45
        else -> null
    }
}

class MapViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {
    val apiType: Class<ChargepointApi<ReferenceData>>
        get() = api.javaClass
    val apiName: String
        get() = api.getName()

    private var db = AppDatabase.getInstance(application)
    private var prefs = PreferenceDataSource(application)
    private var api: ChargepointApi<ReferenceData> = createApi(prefs.dataSource, application)

    val bottomSheetState: MutableLiveData<Int> by lazy {
        state.getLiveData("bottomSheetState")
    }

    val mapPosition: MutableLiveData<MapPosition> by lazy {
        state.getLiveData("mapPosition")
    }
    val filterStatus: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>().apply {
            value = prefs.filterStatus
            observeForever {
                prefs.filterStatus = it
                if (it != FILTERS_DISABLED) prefs.lastFilterProfile = it
            }
        }
    }
    private val filterValues: LiveData<List<FilterValue>> =
        db.filterValueDao().getFilterValues(filterStatus, prefs.dataSource)
    private val referenceData = api.getReferenceData(viewModelScope, application)
    private val filters = api.getFilters(referenceData, application.stringProvider())

    private val filtersWithValue: LiveData<FilterValues> by lazy {
        filtersWithValue(filters, filterValues)
    }

    val filterProfiles: LiveData<List<FilterProfile>> by lazy {
        db.filterProfileDao().getProfiles(prefs.dataSource)
    }

    val chargeCardMap: LiveData<Map<Long, ChargeCard>> by lazy {
        MediatorLiveData<Map<Long, ChargeCard>>().apply {
            value = null
            addSource(referenceData) { data ->
                value = if (data is GEReferenceData) {
                    data.chargecards.map {
                        it.id to it.convert()
                    }.toMap()
                } else {
                    null
                }
            }
        }
    }

    val filtersCount: LiveData<Int> by lazy {
        MediatorLiveData<Int>().apply {
            value = 0
            addSource(filtersWithValue) { filtersWithValue ->
                value = filtersWithValue.count {
                    !it.value.hasSameValueAs(it.filter.defaultValue())
                }
            }
        }
    }
    val chargepoints: MediatorLiveData<Resource<List<ChargepointListItem>>> by lazy {
        MediatorLiveData<Resource<List<ChargepointListItem>>>()
            .apply {
                value = Resource.loading(emptyList())
                listOf(mapPosition, filtersWithValue, referenceData).forEach {
                    addSource(it) {
                        reloadChargepoints()
                    }
                }
            }
    }
    val filteredConnectors: MutableLiveData<Set<String>> by lazy {
        MutableLiveData<Set<String>>()
    }
    val filteredMinPower: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    val filteredChargeCards: MutableLiveData<Set<Long>> by lazy {
        MutableLiveData<Set<Long>>()
    }

    val chargerSparse: MutableLiveData<ChargeLocation> by lazy {
        state.getLiveData("chargerSparse")
    }
    val chargerDetails: MediatorLiveData<Resource<ChargeLocation>> by lazy {
        MediatorLiveData<Resource<ChargeLocation>>().apply {
            listOf(chargerSparse, referenceData).forEach {
                addSource(it) { _ ->
                    val charger = chargerSparse.value
                    val refData = referenceData.value
                    if (charger != null && refData != null) {
                        loadChargerDetails(charger, refData)
                    } else {
                        value = null
                    }
                }
            }
        }
    }
    val charger: MediatorLiveData<Resource<ChargeLocation>> by lazy {
        MediatorLiveData<Resource<ChargeLocation>>().apply {
            addSource(chargerDetails) {
                value = when (it?.status) {
                    null -> null
                    Status.SUCCESS -> Resource.success(it.data)
                    Status.LOADING -> Resource.loading(chargerSparse.value)
                    Status.ERROR -> Resource.error(it.message, chargerSparse.value)
                }
            }
        }
    }
    val chargerDistance: MediatorLiveData<Double> by lazy {
        MediatorLiveData<Double>().apply {
            val callback = { _: Any? ->
                val loc = location.value
                val charger = chargerSparse.value
                value = if (loc != null && charger != null) {
                    distanceBetween(
                        loc.latitude,
                        loc.longitude,
                        charger.coordinates.lat,
                        charger.coordinates.lng
                    )
                } else null
            }
            addSource(chargerSparse, callback)
            addSource(location, callback)
        }
    }
    val location: MutableLiveData<LatLng> by lazy {
        MutableLiveData<LatLng>()
    }
    val availability: MediatorLiveData<Resource<ChargeLocationStatus>> by lazy {
        MediatorLiveData<Resource<ChargeLocationStatus>>().apply {
            addSource(chargerSparse) { charger ->
                if (charger != null) {
                    viewModelScope.launch {
                        loadAvailability(charger)
                    }
                } else {
                    value = null
                }
            }
        }
    }
    val filteredAvailability: MediatorLiveData<Resource<ChargeLocationStatus>> by lazy {
        MediatorLiveData<Resource<ChargeLocationStatus>>().apply {
            val callback = { _: Any? ->
                val av = availability.value
                val filters = filtersWithValue.value
                if (av?.status == Status.SUCCESS && filters != null) {
                    value = Resource.success(
                        av.data!!.applyFilters(
                            filteredConnectors.value,
                            filteredMinPower.value
                        )
                    )
                } else {
                    value = av
                }
            }
            addSource(availability, callback)
            addSource(filteredConnectors, callback)
            addSource(filteredMinPower, callback)
        }
    }
    val myLocationEnabled: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val layersMenuOpen: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().apply {
            value = false
        }
    }

    val favorites: LiveData<List<ChargeLocation>> by lazy {
        db.chargeLocationsDao().getAllChargeLocations()
    }

    val searchResult: MutableLiveData<PlaceWithBounds> by lazy {
        state.getLiveData("searchResult")
    }

    val mapType: MutableLiveData<AnyMap.Type> by lazy {
        MutableLiveData<AnyMap.Type>().apply {
            value = prefs.mapType
            observeForever {
                prefs.mapType = it
            }
        }
    }

    val mapTrafficEnabled: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().apply {
            value = prefs.mapTrafficEnabled
            observeForever {
                prefs.mapTrafficEnabled = it
            }
        }
    }

    fun reloadPrefs() {
        filterStatus.value = prefs.filterStatus
    }

    fun toggleFilters() {
        if (filterStatus.value == FILTERS_DISABLED) {
            filterStatus.value = prefs.lastFilterProfile
        } else {
            filterStatus.value = FILTERS_DISABLED
        }
    }

    suspend fun copyFiltersToCustom() {
        if (filterStatus.value == FILTERS_CUSTOM) return

        db.filterValueDao().deleteFilterValuesForProfile(FILTERS_CUSTOM, prefs.dataSource)
        filterValues.value?.map {
            it.profile = FILTERS_CUSTOM
            it
        }?.let {
            db.filterValueDao().insert(*it.toTypedArray())
        }
    }

    fun setMapType(type: AnyMap.Type) {
        mapType.value = type
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

    fun reloadChargepoints() {
        val pos = mapPosition.value ?: return
        val filters = filtersWithValue.value ?: return
        val referenceData = referenceData.value ?: return
        chargepointLoader(Triple(pos, filters, referenceData))
    }

    private var chargepointLoader =
        throttleLatest(
            500L,
            viewModelScope
        ) { data: Triple<MapPosition, FilterValues, ReferenceData> ->
            chargepoints.value = Resource.loading(chargepoints.value?.data)

            val mapPosition = data.first
            val filters = data.second
            val api = api
            val refData = data.third

            if (filterStatus.value == FILTERS_FAVORITES) {
                // load favorites from local DB
                val b = mapPosition.bounds
                var chargers = db.chargeLocationsDao().getChargeLocationsInBoundsAsync(
                    b.southwest.latitude,
                    b.northeast.latitude,
                    b.southwest.longitude,
                    b.northeast.longitude
                ) as List<ChargepointListItem>

                val clusterDistance = getClusterDistance(mapPosition.zoom)
                clusterDistance?.let {
                    chargers = cluster(chargers, mapPosition.zoom, clusterDistance)
                }
                filteredConnectors.value = null
                filteredMinPower.value = null
                filteredChargeCards.value = null
                chargepoints.value = Resource.success(chargers)
                return@throttleLatest
            }

            if (api is GoingElectricApiWrapper) {
                val chargeCardsVal = filters.getMultipleChoiceValue("chargecards")!!
                filteredChargeCards.value =
                    if (chargeCardsVal.all) null else chargeCardsVal.values.map { it.toLong() }
                        .toSet()

                val connectorsVal = filters.getMultipleChoiceValue("connectors")!!
                filteredConnectors.value =
                    if (connectorsVal.all) null else connectorsVal.values.map {
                        GEChargepoint.convertTypeFromGE(it)
                    }.toSet()
                filteredMinPower.value = filters.getSliderValue("minPower")
            } else if (api is OpenChargeMapApiWrapper) {
                val connectorsVal = filters.getMultipleChoiceValue("connectors")!!
                filteredConnectors.value =
                    if (connectorsVal.all) null else connectorsVal.values.map {
                        OCMConnection.convertConnectionTypeFromOCM(
                            it.toLong(),
                            refData as OCMReferenceData
                        )
                    }.toSet()
                filteredMinPower.value = filters.getSliderValue("minPower")
            } else {
                filteredConnectors.value = null
                filteredMinPower.value = null
                filteredChargeCards.value = null
            }

            var result = api.getChargepoints(refData, mapPosition.bounds, mapPosition.zoom, filters)
            if (result.status == Status.ERROR && result.data == null) {
                // keep old results if new data could not be loaded
                result = Resource.error(result.message, chargepoints.value?.data)
            }

            chargepoints.value = result
        }

    private suspend fun loadAvailability(charger: ChargeLocation) {
        availability.value = Resource.loading(null)
        availability.value = getAvailability(charger)
    }

    private var chargerLoadingTask: Job? = null

    private fun loadChargerDetails(charger: ChargeLocation, referenceData: ReferenceData) {
        chargerDetails.value = Resource.loading(null)
        chargerLoadingTask?.cancel()
        chargerLoadingTask = viewModelScope.launch {
            try {
                chargerDetails.value = api.getChargepointDetail(referenceData, charger.id)
            } catch (e: IOException) {
                chargerDetails.value = Resource.error(e.message, null)
                e.printStackTrace()
            }
        }
    }

    fun loadChargerById(chargerId: Long) {
        chargerDetails.value = Resource.loading(null)
        chargerSparse.value = null
        referenceData.observeForever(object : Observer<ReferenceData> {
            override fun onChanged(refData: ReferenceData) {
                referenceData.removeObserver(this)
                viewModelScope.launch {
                    val response = api.getChargepointDetail(refData, chargerId)
                    chargerDetails.value = response
                    if (response.status == Status.SUCCESS) {
                        chargerSparse.value = response.data
                    } else {
                        chargerSparse.value = null
                    }
                }
            }
        })
    }
}