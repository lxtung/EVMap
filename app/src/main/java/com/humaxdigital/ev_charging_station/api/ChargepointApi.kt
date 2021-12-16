package com.humaxdigital.ev_charging_station.api

import android.content.Context
import com.car2go.maps.model.LatLng
import com.car2go.maps.model.LatLngBounds
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.api.goingelectric.GoingElectricApiWrapper
import com.humaxdigital.ev_charging_station.api.openchargemap.OpenChargeMapApiWrapper
import com.humaxdigital.ev_charging_station.model.*
import com.humaxdigital.ev_charging_station.viewmodel.Resource

interface ChargepointApi<out T : ReferenceData> {
    suspend fun getChargepoints(
        referenceData: ReferenceData,
        bounds: LatLngBounds,
        zoom: Float,
        filters: FilterValues?
    ): Resource<List<ChargepointListItem>>

    suspend fun getChargepointsRadius(
        referenceData: ReferenceData,
        location: LatLng,
        radius: Int,
        zoom: Float,
        filters: FilterValues?
    ): Resource<List<ChargepointListItem>>

    suspend fun getChargepointDetail(
        referenceData: ReferenceData,
        id: Long
    ): Resource<ChargeLocation>

    suspend fun getReferenceData(): Resource<T>

    fun getFilters(referenceData: ReferenceData, sp: StringProvider): List<Filter<FilterValue>>

    fun getName(): String
}

interface StringProvider {
    fun getString(id: Int): String
}

fun Context.stringProvider() = object : StringProvider {
    override fun getString(id: Int): String {
        return this@stringProvider.getString(id)
    }
}

fun createApi(type: String, ctx: Context): ChargepointApi<ReferenceData> {
    return when (type) {
        "openchargemap" -> {
            OpenChargeMapApiWrapper(
                ctx.getString(
                    R.string.openchargemap_key
                )
            )
        }
        "goingelectric" -> {
            GoingElectricApiWrapper(
                ctx.getString(
                    R.string.goingelectric_key
                )
            )
        }
        else -> throw IllegalArgumentException()
    }
}