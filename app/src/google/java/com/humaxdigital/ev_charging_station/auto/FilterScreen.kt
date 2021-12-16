package com.humaxdigital.ev_charging_station.auto

import android.graphics.Bitmap
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LiveData
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.model.FILTERS_CUSTOM
import com.humaxdigital.ev_charging_station.model.FILTERS_DISABLED
import com.humaxdigital.ev_charging_station.model.FILTERS_FAVORITES
import com.humaxdigital.ev_charging_station.storage.AppDatabase
import com.humaxdigital.ev_charging_station.storage.FilterProfile
import com.humaxdigital.ev_charging_station.storage.PreferenceDataSource
import kotlin.math.roundToInt

class FilterScreen(ctx: CarContext) : Screen(ctx) {
    private val prefs = PreferenceDataSource(ctx)
    private val db = AppDatabase.getInstance(ctx)
    val filterProfiles: LiveData<List<FilterProfile>> by lazy {
        db.filterProfileDao().getProfiles(prefs.dataSource)
    }
    private val maxRows = 6
    private val checkIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_check)).build()
    private val emptyIcon: CarIcon

    init {
        val size = (ctx.resources.displayMetrics.density * 24).roundToInt()
        emptyIcon = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        ).asCarIcon()
    }

    init {
        filterProfiles.observe(this) {
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val filterStatus =
            prefs.filterStatus.takeUnless { it == FILTERS_CUSTOM || it == FILTERS_FAVORITES }
                ?: FILTERS_DISABLED
        return ListTemplate.Builder().apply {
            filterProfiles.value?.let {
                setSingleList(buildFilterProfilesList(it.take(maxRows), filterStatus))
            } ?: setLoading(true)
            setTitle(carContext.getString(R.string.menu_filter))
            setHeaderAction(Action.BACK)
        }.build()
    }

    private fun buildFilterProfilesList(
        profiles: List<FilterProfile>,
        filterStatus: Long
    ): ItemList {
        return ItemList.Builder().apply {
            addItem(Row.Builder().apply {
                setTitle(carContext.getString(R.string.no_filters))
                if (FILTERS_DISABLED == filterStatus) {
                    setImage(checkIcon)
                } else {
                    setImage(emptyIcon)
                }
                setOnClickListener {
                    prefs.filterStatus = FILTERS_DISABLED
                    screenManager.pop()
                }
            }.build())
            profiles.forEach {
                addItem(Row.Builder().apply {
                    val name =
                        it.name.ifEmpty { carContext.getString(R.string.unnamed_filter_profile) }
                    setTitle(name)
                    if (it.id == filterStatus) {
                        setImage(checkIcon)
                    } else {
                        setImage(emptyIcon)
                    }
                    setOnClickListener {
                        prefs.filterStatus = it.id
                        screenManager.pop()
                    }
                }.build())
            }
            setNoItemsMessage(carContext.getString(R.string.filterprofiles_empty_state))
        }.build()
    }
}