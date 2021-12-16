package com.humaxdigital.ev_charging_station.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import com.humaxdigital.ev_charging_station.fragment.DataSourceSelectDialog

class DataSourceSelectDialogPreference(ctx: Context, attrs: AttributeSet) :
    ListPreference(ctx, attrs) {
    override fun onClick() {
        val dialog = DataSourceSelectDialog.getInstance(true)
        dialog.okListener = { selected ->
            value = selected
        }
        dialog.show((context as AppCompatActivity).supportFragmentManager, null)
    }
}