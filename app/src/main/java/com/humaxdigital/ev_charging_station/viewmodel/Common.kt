package com.humaxdigital.ev_charging_station.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import com.humaxdigital.ev_charging_station.api.ChargepointApi
import com.humaxdigital.ev_charging_station.api.StringProvider
import com.humaxdigital.ev_charging_station.api.goingelectric.GoingElectricApiWrapper
import com.humaxdigital.ev_charging_station.api.openchargemap.OpenChargeMapApiWrapper
import com.humaxdigital.ev_charging_station.model.*
import com.humaxdigital.ev_charging_station.storage.*
import kotlin.reflect.cast

fun ChargepointApi<ReferenceData>.getReferenceData(
    scope: CoroutineScope,
    ctx: Context
): LiveData<out ReferenceData> {
    val db = AppDatabase.getInstance(ctx)
    val prefs = PreferenceDataSource(ctx)
    return when (this) {
        is GoingElectricApiWrapper -> {
            GEReferenceDataRepository(
                this,
                scope,
                db.geReferenceDataDao(),
                prefs
            ).getReferenceData()
        }
        is OpenChargeMapApiWrapper -> {
            OCMReferenceDataRepository(
                this,
                scope,
                db.ocmReferenceDataDao(),
                prefs
            ).getReferenceData()
        }
        else -> {
            throw RuntimeException("no reference data implemented")
        }
    }
}

fun filtersWithValue(
    filters: LiveData<List<Filter<FilterValue>>>,
    filterValues: LiveData<List<FilterValue>>
): MediatorLiveData<FilterValues> =
    MediatorLiveData<FilterValues>().apply {
        listOf(filters, filterValues).forEach {
            addSource(it) {
                val f = filters.value ?: return@addSource
                val values = filterValues.value ?: return@addSource
                value = f.map { filter ->
                    val value =
                        values.find { it.key == filter.key } ?: filter.defaultValue()
                    FilterWithValue(filter, filter.valueClass.cast(value))
                }
            }
        }
    }

fun ChargepointApi<ReferenceData>.getFilters(
    referenceData: LiveData<out ReferenceData>,
    stringProvider: StringProvider
) = MediatorLiveData<List<Filter<FilterValue>>>().apply {
    addSource(referenceData) { data ->
        value = getFilters(data, stringProvider)
    }
}

fun FilterValueDao.getFilterValues(filterStatus: LiveData<Long>, dataSource: String) =
    MediatorLiveData<List<FilterValue>>().apply {
        var source: LiveData<List<FilterValue>>? = null
        addSource(filterStatus) { status ->
            source?.let { removeSource(it) }
            source = getFilterValues(status, dataSource)
            addSource(source!!) { result ->
                value = result
            }
        }
    }