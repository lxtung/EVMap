package com.humaxdigital.ev_charging_station.storage

import androidx.room.*
import com.car2go.maps.model.LatLng
import com.car2go.maps.model.LatLngBounds
import com.humaxdigital.ev_charging_station.autocomplete.AutocompletePlace
import com.humaxdigital.ev_charging_station.autocomplete.AutocompletePlaceType
import com.humaxdigital.ev_charging_station.autocomplete.PlaceWithBounds
import com.humaxdigital.ev_charging_station.utils.distanceBetween
import java.time.Instant

@Entity(primaryKeys = ["id", "dataSource"])
data class RecentAutocompletePlace(
    val id: String,
    val dataSource: String,
    var timestamp: Instant,
    val primaryText: String,
    val secondaryText: String,
    val latLng: LatLng,
    val viewport: LatLngBounds?,
    val types: List<AutocompletePlaceType>
) {
    constructor(
        place: AutocompletePlace,
        details: PlaceWithBounds,
        dataSource: String,
        timestamp: Instant
    ) : this(
        place.id, dataSource, timestamp, place.primaryText.toString(),
        place.secondaryText.toString(), details.latLng, details.viewport, place.types
    )

    fun asAutocompletePlace(currentLocation: LatLng?): AutocompletePlace {
        return AutocompletePlace(
            primaryText,
            secondaryText,
            id,
            currentLocation?.let {
                distanceBetween(
                    latLng.latitude, latLng.longitude,
                    it.latitude, it.longitude
                )
            },
            types + AutocompletePlaceType.RECENT
        )
    }

    fun asPlaceWithBounds(): PlaceWithBounds {
        return PlaceWithBounds(latLng, viewport)
    }
}

@Dao
abstract class RecentAutocompletePlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(vararg places: RecentAutocompletePlace)

    @Query("DELETE FROM recentautocompleteplace")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM recentautocompleteplace WHERE dataSource = :dataSource AND primaryText LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    abstract fun search(
        query: String,
        dataSource: String,
        limit: Int? = null
    ): List<RecentAutocompletePlace>

    @Query("SELECT * FROM recentautocompleteplace WHERE dataSource = :dataSource ORDER BY timestamp DESC LIMIT :limit")
    abstract fun getAll(dataSource: String, limit: Int? = null): List<RecentAutocompletePlace>
}