package com.humaxdigital.ev_charging_station.auto

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.car.app.model.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.ui.Gauge
import kotlin.math.min
import kotlin.math.roundToInt

class VehicleDataScreen(ctx: CarContext) : Screen(ctx), LifecycleObserver {
    private val hardwareMan = ctx.getCarService(CarContext.HARDWARE_SERVICE) as CarHardwareManager
    private var model: Model? = null
    private var energyLevel: EnergyLevel? = null
    private var speed: Speed? = null
    private var gauge = Gauge((ctx.resources.displayMetrics.density * 128).roundToInt(), ctx)
    private val maxSpeed = 160f / 3.6f // m/s, speed gauge will show max if speed is higher

    private val permissions = listOf(
        "com.google.android.gms.permission.CAR_FUEL",
        "com.google.android.gms.permission.CAR_SPEED"
    )

    init {
        lifecycle.addObserver(this)
    }

    override fun onGetTemplate(): Template {
        if (!permissionsGranted()) {
            Handler(Looper.getMainLooper()).post {
                screenManager.pushForResult(
                    PermissionScreen(
                        carContext,
                        R.string.auto_vehicle_data_permission_needed,
                        permissions
                    )
                ) {
                    setupListeners()
                }
            }
        }

        val energyLevel = energyLevel
        val model = model
        val speed = speed

        return GridTemplate.Builder().apply {
            setTitle(
                if (model != null && model.manufacturer.value != null && model.name.value != null) {
                    "${model.manufacturer.value} ${model.name.value}"
                } else {
                    carContext.getString(R.string.auto_vehicle_data)
                }
            )
            setHeaderAction(Action.BACK)
            if (!permissionsGranted()) {
                setLoading(true)
            } else {
                setSingleList(
                    ItemList.Builder().apply {
                        addItem(GridItem.Builder().apply {
                            setTitle(carContext.getString(R.string.auto_charging_level))
                            if (energyLevel == null) {
                                setLoading(true)
                            } else if (energyLevel.batteryPercent.value != null && energyLevel.fuelPercent.value != null) {
                                // both battery and fuel (Plug-in hybrid)
                                setText(
                                    "\uD83D\uDD0C %.0f %% ⛽ %.0f %%".format(
                                        energyLevel.batteryPercent.value,
                                        energyLevel.fuelPercent.value
                                    )
                                )
                                setImage(
                                    gauge.draw(
                                        energyLevel.batteryPercent.value,
                                        energyLevel.fuelPercent.value
                                    ).asCarIcon()
                                )
                            } else if (energyLevel.batteryPercent.value != null) {
                                // BEV
                                setText("%.0f %%".format(energyLevel.batteryPercent.value))
                                setImage(gauge.draw(energyLevel.batteryPercent.value).asCarIcon())
                            } else if (energyLevel.fuelPercent.value != null) {
                                // ICE
                                setText("⛽ %.0f %%".format(energyLevel.fuelPercent.value))
                                setImage(gauge.draw(energyLevel.fuelPercent.value).asCarIcon())
                            } else {
                                setText(carContext.getString(R.string.auto_no_data))
                                setImage(gauge.draw(0f).asCarIcon())
                            }
                        }.build())
                        addItem(GridItem.Builder().apply {
                            setTitle(carContext.getString(R.string.auto_range))
                            if (energyLevel == null) {
                                setLoading(true)
                            } else if (energyLevel.rangeRemainingMeters.value != null) {
                                setText(
                                    formatCarUnitDistance(
                                        energyLevel.rangeRemainingMeters.value,
                                        energyLevel.distanceDisplayUnit.value
                                    )
                                )
                                setImage(
                                    CarIcon.Builder(
                                        IconCompat.createWithResource(
                                            carContext,
                                            R.drawable.ic_car
                                        )
                                    ).build()
                                )
                            } else {
                                setText(carContext.getString(R.string.auto_no_data))
                                setImage(
                                    CarIcon.Builder(
                                        IconCompat.createWithResource(
                                            carContext,
                                            R.drawable.ic_car
                                        )
                                    ).build()
                                )
                            }
                        }.build())
                        addItem(GridItem.Builder().apply {
                            setTitle(carContext.getString(R.string.auto_speed))
                            if (speed == null) {
                                setLoading(true)
                            } else {
                                val rawSpeed = speed.rawSpeedMetersPerSecond.value
                                val displaySpeed = speed.displaySpeedMetersPerSecond.value
                                if (rawSpeed != null) {
                                    setText(
                                        formatCarUnitSpeed(
                                            rawSpeed,
                                            speed.speedDisplayUnit.value
                                        )
                                    )
                                    setImage(
                                        gauge.draw(min(rawSpeed / maxSpeed * 100, 100f)).asCarIcon()
                                    )
                                } else if (displaySpeed != null) {
                                    setText(
                                        formatCarUnitSpeed(
                                            speed.displaySpeedMetersPerSecond.value,
                                            speed.speedDisplayUnit.value
                                        )
                                    )
                                    setImage(
                                        gauge.draw(min(displaySpeed / maxSpeed * 100, 100f))
                                            .asCarIcon()
                                    )
                                } else {
                                    setText(carContext.getString(R.string.auto_no_data))
                                    setImage(gauge.draw(0f).asCarIcon())
                                }
                            }
                        }.build())
                    }.build()
                )
            }
        }.build()
    }

    private fun onEnergyLevelUpdated(energyLevel: EnergyLevel) {
        this.energyLevel = energyLevel
        invalidate()
    }

    private fun onSpeedUpdated(speed: Speed) {
        this.speed = speed
        invalidate()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun setupListeners() {
        if (!permissionsGranted()) return

        println("Setting up energy level listener")

        val exec = ContextCompat.getMainExecutor(carContext)
        hardwareMan.carInfo.addEnergyLevelListener(exec, ::onEnergyLevelUpdated)
        hardwareMan.carInfo.addSpeedListener(exec, ::onSpeedUpdated)

        hardwareMan.carInfo.fetchModel(exec) {
            this.model = it
            invalidate()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun removeListeners() {
        println("Removing energy level listener")
        hardwareMan.carInfo.removeEnergyLevelListener(::onEnergyLevelUpdated)
        hardwareMan.carInfo.removeSpeedListener(::onSpeedUpdated)
    }

    private fun permissionsGranted(): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(
                carContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
}