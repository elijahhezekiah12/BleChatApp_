package com.example.blechatapp.bluetooth

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blechatapp.Message
import com.example.blechatapp.states.DeviceConnectionState
import com.example.blechatapp.util.MESSAGE_UUID
import com.example.blechatapp.util.SERVICE_UUID

object ChatServer {

    private const val TAG = "ChatServerTAG"

    private var app: Application? = null
    private lateinit var bluetoothManager: BluetoothManager



    private var adapter: BluetoothAdapter =BluetoothAdapter.getDefaultAdapter()

    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
    private var advertiseData: AdvertiseData = buildAdvertiseData()

    private val _messages = MutableLiveData<Message>()
    val messages = _messages as LiveData<Message>


    private var gattServer: BluetoothGattServer? = null
    private var gattServerCallback: BluetoothGattServerCallback? = null

    private var gattClient: BluetoothGatt? = null
    private var gattClientCallback: BluetoothGattCallback? = null

    private var currentDevice: BluetoothDevice? = null

    private val _deviceConnection = MutableLiveData<DeviceConnectionState>()
    val deviceConnection = _deviceConnection as LiveData<DeviceConnectionState>

    private var gatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    var blueTootEnable = false


    @RequiresApi(Build.VERSION_CODES.S)

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT])
    fun startServer(app: Application) {
        bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = bluetoothManager.adapter
        setupGattServer(app)
        startAdvertisement()

        }
    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT])
    @RequiresApi(Build.VERSION_CODES.S)
    fun stopServer(){
        stopAdvertising()
    }



    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(BLUETOOTH_CONNECT)
    fun setCurrentChatConnection(device: BluetoothDevice) {
        currentDevice = device
        _deviceConnection.postValue(DeviceConnectionState.Connected(device))
        connectToChatDevice(device)
    }


    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(BLUETOOTH_CONNECT)
    private fun connectToChatDevice(device: BluetoothDevice) {
        gattClientCallback = GattClientCallback()
        gattClient = device.connectGatt(app, false, gattClientCallback)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(BLUETOOTH_CONNECT)
    fun sendMessage(message: String): Boolean {
        messageCharacteristic?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes
            gatt?.let {
                val success = it.writeCharacteristic(messageCharacteristic)
                if (success) {
                    _messages.postValue(Message.LocalMessage(message))
                }
            }
        }
        return false
    }




    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(BLUETOOTH_CONNECT)
    fun setupGattServer(app: Application) {
        Log.i(TAG,"setupGattServer")
       // enablePermission()
        gattServerCallback = GattServerCallback()

        gattServer = bluetoothManager.openGattServer(
            app,
            gattServerCallback
        ).apply {
            addService(setupGattService())
        }
    }



    private fun setupGattService(): BluetoothGattService {
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val messageCharacteristic = BluetoothGattCharacteristic(
            MESSAGE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(messageCharacteristic)
        return service
    }




    @RequiresPermission(BLUETOOTH_ADVERTISE)
    @RequiresApi(Build.VERSION_CODES.S)
    fun startAdvertisement() {
        advertiser = adapter.bluetoothLeAdvertiser

        if (advertiseCallback == null) {
            advertiseCallback = DeviceAdvertiseCallback()
            //enablePermission()

            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }



    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(BLUETOOTH_ADVERTISE)
    private fun stopAdvertising() {
        Log.d(TAG, "Stopping Advertising with advertiser $advertiser")
        //enablePermission()
        if(advertiseCallback != null)
            advertiser?.stopAdvertising(advertiseCallback)
        advertiseCallback = null
    }


    private fun buildAdvertiseData(): AdvertiseData {
        val dataBuilder = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(true)

        return dataBuilder.build()
    }

    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTimeout(0)
            .build()
    }



    private class GattServerCallback : BluetoothGattServerCallback() {
        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            if (isSuccess && isConnected) {
               // enablePermission()
                setCurrentChatConnection(device)
            } else {
                _deviceConnection.postValue(DeviceConnectionState.Disconnected)
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            if (characteristic.uuid == MESSAGE_UUID) {
               // enablePermission()
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                val message = value?.toString(Charsets.UTF_8)
                message?.let {
                    _messages.postValue(Message.RemoteMessage(it))
                }
            }
        }
    }



   fun enablePermission () {
        if (app?.applicationContext?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    BLUETOOTH_ADVERTISE
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                blueTootEnable = false
            }
            else{
                blueTootEnable = false
            }

        }
    }



    private class GattClientCallback : BluetoothGattCallback() {

        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED

            if (isSuccess && isConnected) {

               // enablePermission()
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(discoveredGatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt = discoveredGatt
                val service = discoveredGatt.getService(SERVICE_UUID)
                if(service != null)
                    messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
            }
        }
    }


    private class DeviceAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            val errorMessage = "Advertise failed with error: $errorCode"
            Log.d(TAG, "failed $errorMessage")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "successfully started")
        }
    }





}