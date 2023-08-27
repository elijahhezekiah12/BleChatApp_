package com.example.blechatapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.blechatapp.bluetooth.ChatServer
import com.example.blechatapp.presentation.ChatCompose
import com.example.blechatapp.presentation.DeviceScanCompose
import com.example.blechatapp.states.DeviceConnectionState
import com.example.blechatapp.ui.theme.BLEChatAppTheme
import com.example.blechatapp.viewmodels.DeviceScanViewModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : ComponentActivity() {

    private val viewModel: DeviceScanViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onStop() {
        super.onStop()
        ChatServer.stopServer()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        setContent {
            BLEChatAppTheme {


                val result = remember { mutableStateOf<Int?>(100) }
                val launcher =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        result.value = it.resultCode
                    }

                LaunchedEffect(key1 = true) {

                    Dexter.withContext(this@MainActivity)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                        )
                        .withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                launcher.launch(intent)
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: List<PermissionRequest?>?,
                                token: PermissionToken?
                            ) {

                            }
                        })
                        .check()

                }


                LaunchedEffect(key1 = result.value) {
                    if (result.value == RESULT_OK) {
                        ChatServer.startServer(application)
                        viewModel.startScan()
                        if (!ChatServer.blueTootEnable || !bluetoothAdapter.isEnabled)
                        {// Check if bluetooth is enabled on the device.
                             enablePermission()
                        }
                    }
                }

                ScaffoldWithTopBar()


            }
        }


    }

    @Preview
    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Composable
    fun ScaffoldWithTopBar() {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Bluetooth Chat App")
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.ArrowBack, "backIcon")
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                    ),
                )
            }, content = {
                Column(
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize()
                        .background(Color(0xff8d6e63)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val deviceScanningState by viewModel.viewState.observeAsState()

                        val deviceConnectionState by ChatServer.deviceConnection.observeAsState()


                        var isChatOpen by remember {
                            mutableStateOf(false)
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (deviceScanningState != null && !isChatOpen || deviceConnectionState == DeviceConnectionState.Disconnected) {
                                Column {
                                    Text(
                                        text = "Choose a device to chat with:",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DeviceScanCompose.DeviceScan(deviceScanViewState = deviceScanningState!!) {
                                        isChatOpen = true
                                    }
                                }

                            } else if (deviceScanningState != null && deviceConnectionState is DeviceConnectionState.Connected) {
                                ChatCompose.Chats((deviceConnectionState as DeviceConnectionState.Connected).device.name)
                            } else {
                                Text(text = "Nothing")
                            }




                        }


                    }
                }
            })
    }


    // Bluetooth permissions callback.
    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Granted
                Log.i("Bluetooth", "Bluetooth permission granted!")
            } else {
                // Denied
                Log.e("Bluetooth", "Bluetooth permission denied!")

            }
        }

    fun enablePermission () {
        if (application?.applicationContext?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                ActivityCompat.requestPermissions(this ,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    ), 2)
            }
            else{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            }

        }
    }



}


