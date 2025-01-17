package com.humaxdigital.ev_charging_station.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.humaxdigital.ev_charging_station.MapsActivity
import com.humaxdigital.ev_charging_station.R
import com.humaxdigital.ev_charging_station.adapter.ChargepriceAdapter
import com.humaxdigital.ev_charging_station.adapter.CheckableChargepriceCarAdapter
import com.humaxdigital.ev_charging_station.adapter.CheckableConnectorAdapter
import com.humaxdigital.ev_charging_station.api.chargeprice.ChargepriceCar
import com.humaxdigital.ev_charging_station.api.equivalentPlugTypes
import com.humaxdigital.ev_charging_station.databinding.FragmentChargepriceBinding
import com.humaxdigital.ev_charging_station.model.Chargepoint
import com.humaxdigital.ev_charging_station.viewmodel.ChargepriceViewModel
import com.humaxdigital.ev_charging_station.viewmodel.Status
import com.humaxdigital.ev_charging_station.viewmodel.viewModelFactory
import java.text.NumberFormat

class ChargepriceFragment : DialogFragment() {
    private lateinit var binding: FragmentChargepriceBinding
    private var connectionErrorSnackbar: Snackbar? = null

    private val vm: ChargepriceViewModel by viewModels(factoryProducer = {
        viewModelFactory {
            ChargepriceViewModel(
                requireActivity().application,
                getString(R.string.chargeprice_key)
            )
        }
    })

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.ChargepriceDialogAnimation
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_chargeprice, container, false
        )
        binding.lifecycleOwner = this
        binding.vm = vm

        binding.toolbar.inflateMenu(R.menu.chargeprice)
        binding.toolbar.setTitle(R.string.chargeprice_title)

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // dialog with 95% screen height
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentArgs: ChargepriceFragmentArgs by navArgs()
        val charger = fragmentArgs.charger
        val dataSource = fragmentArgs.dataSource
        vm.charger.value = charger
        vm.dataSource.value = dataSource
        if (vm.chargepoint.value == null) {
            vm.chargepoint.value = charger.chargepointsMerged.get(0)
        }

        val vehicleAdapter = CheckableChargepriceCarAdapter()
        binding.vehicleSelection.adapter = vehicleAdapter
        val vehicleObserver: Observer<ChargepriceCar> = Observer {
            vehicleAdapter.setCheckedItem(it)
        }
        vm.vehicle.observe(viewLifecycleOwner, vehicleObserver)
        vehicleAdapter.onCheckedItemChangedListener = {
            vm.vehicle.removeObserver(vehicleObserver)
            vm.vehicle.value = it
            vm.vehicle.observe(viewLifecycleOwner, vehicleObserver)
        }

        val chargepriceAdapter = ChargepriceAdapter().apply {
            onClickListener = {
                (requireActivity() as MapsActivity).openUrl(it.url)
            }
        }
        binding.chargePricesList.apply {
            adapter = chargepriceAdapter
            layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(
                DividerItemDecoration(
                    context, LinearLayoutManager.VERTICAL
                )
            )
        }
        vm.chargepriceMetaForChargepoint.observe(viewLifecycleOwner) {
            chargepriceAdapter.meta = it?.data
        }
        vm.myTariffs.observe(viewLifecycleOwner) {
            chargepriceAdapter.myTariffs = it
        }
        vm.myTariffsAll.observe(viewLifecycleOwner) {
            chargepriceAdapter.myTariffsAll = it
        }

        val connectorsAdapter = CheckableConnectorAdapter()

        val observer: Observer<Chargepoint> = Observer {
            connectorsAdapter.setCheckedItem(it)
        }
        vm.chargepoint.observe(viewLifecycleOwner, observer)
        connectorsAdapter.onCheckedItemChangedListener = {
            vm.chargepoint.removeObserver(observer)
            vm.chargepoint.value = it
            vm.chargepoint.observe(viewLifecycleOwner, observer)
        }

        vm.vehicleCompatibleConnectors.observe(viewLifecycleOwner) { plugs ->
            connectorsAdapter.enabledConnectors =
                plugs?.flatMap { plug -> equivalentPlugTypes(plug) }
        }

        binding.connectorsList.apply {
            adapter = connectorsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.imgChargepriceLogo.setOnClickListener {
            (requireActivity() as MapsActivity).openUrl("https://www.chargeprice.app/?poi_id=${charger.id}&poi_source=${dataSource}")
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_chargeprice_to_settingsFragment)
        }

        binding.batteryRange.setLabelFormatter { value: Float ->
            val fmt = NumberFormat.getNumberInstance()
            fmt.maximumFractionDigits = 0
            fmt.format(value.toDouble()) + "%"
        }
        binding.batteryRange.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> vm.batteryRangeSliderDragging.value = true
                MotionEvent.ACTION_UP -> vm.batteryRangeSliderDragging.value = false
            }
            false
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_close -> {
                    dismiss()
                    true
                }
                R.id.menu_help -> {
                    (activity as? MapsActivity)?.openUrl(getString(R.string.chargeprice_faq_link))
                    true
                }
                else -> false
            }
        }

        vm.chargePricesForChargepoint.observe(viewLifecycleOwner) { res ->
            when (res?.status) {
                Status.ERROR -> {
                    if (vm.vehicle.value == null) return@observe
                    connectionErrorSnackbar?.dismiss()
                    connectionErrorSnackbar = Snackbar
                        .make(
                            view,
                            R.string.chargeprice_connection_error,
                            Snackbar.LENGTH_INDEFINITE
                        )
                        .setAction(R.string.retry) {
                            connectionErrorSnackbar?.dismiss()
                            vm.loadPrices()
                        }
                    connectionErrorSnackbar!!.show()
                }
                Status.SUCCESS, null -> {
                    connectionErrorSnackbar?.dismiss()
                }
                Status.LOADING -> {
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.toolbar.setupWithNavController(
            findNavController(),
            (requireActivity() as MapsActivity).appBarConfiguration
        )
    }

}