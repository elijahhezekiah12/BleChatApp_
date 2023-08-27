package com.example.blechatapp.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.blechatapp.bluetooth.ChatServer
import com.example.blechatapp.states.DeviceScanViewState
import com.example.blechatapp.util.SERVICE_UUID


private const val TAG = "DeviceScanViewModel"

private const val SCAN_PERIOD = 20000L


class DeviceScanViewModel(app: Application) : AndroidViewModel(app) {



    private val _viewState = MutableLiveData<DeviceScanViewState>()
    val viewState = _viewState as LiveData<DeviceScanViewState>

    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    private var scanner: BluetoothLeScanner? = null

    private var scanCallback: DeviceScanCallback? = null
    private lateinit var scanFilters: List<ScanFilter>
    private lateinit var scanSettings: ScanSettings


    private lateinit var bluetoothManager: BluetoothManager



    private var adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT])
    private fun stopScanning() {

        scanner?.stopScan(scanCallback)
        scanCallback = null
        _viewState.value = DeviceScanViewState.ScanResults(scanResults)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT])
    fun startScan() {
        scanFilters = buildScanFilters()
        scanSettings = buildScanSettings()
        if (!adapter.isMultipleAdvertisementSupported) {
            _viewState.value = DeviceScanViewState.AdvertisementNotSupported
            return
        }

        if (scanCallback == null) {
            scanner = adapter.bluetoothLeScanner
            _viewState.value = DeviceScanViewState.ActiveScan
            Handler().postDelayed({ stopScanning() }, SCAN_PERIOD)

            scanCallback = DeviceScanCallback()


            scanner?.startScan(scanFilters, scanSettings, scanCallback)
        }
    }

    private fun buildScanFilters(): List<ScanFilter> {
        val builder = ScanFilter.Builder()
        builder.setServiceUuid(ParcelUuid(SERVICE_UUID))
        val filter = builder.build()
        return listOf(filter)
    }


    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }


    private inner class DeviceScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            for (item in results) {
                item.device?.let { device ->
                    scanResults[device.address] = device
                }
            }
            Log.i(TAG, scanResults.toString())
            _viewState.value = DeviceScanViewState.ScanResults(scanResults)
        }

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            super.onScanResult(callbackType, result)
            result.device?.let { device ->
                scanResults[device.address] = device
            }
            Log.i(TAG, scanResults.toString())
            _viewState.value = DeviceScanViewState.ScanResults(scanResults)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = "Scan failed with error: $errorCode"
            _viewState.value = DeviceScanViewState.Error(errorMessage)
        }
    }


}