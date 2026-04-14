package com.example.final_project

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.final_project.bluetooth.BluetoothHelper
import com.example.final_project.data.local.AppDatabase
import com.example.final_project.data.repository.SensorReadingRepository
import com.example.final_project.navigation.AppNavigation
import com.example.final_project.sensors.SensorHelper
import com.example.final_project.theme.MyLabsTheme
import com.example.final_project.viewmodel.MainViewModel
import com.example.final_project.viewmodel.MainViewModelFactory
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var sensorHelper: SensorHelper
    private lateinit var bluetoothHelper: BluetoothHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestBluetoothPermissionsIfNeeded()

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.sensorReadingDao()
        val repository = SensorReadingRepository(dao)
        val factory = MainViewModelFactory(repository)
        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        sensorHelper = SensorHelper(
            context = this,
            onLightChanged = { value ->
                mainViewModel.updateLocalLight(value)
            },
            onProximityChanged = { value ->
                mainViewModel.updateLocalProximity(value)
            }
        )

        bluetoothHelper = BluetoothHelper(
            context = this,
            provideLocalData = {
                BluetoothHelper.LocalSensorPayload(
                    deviceName = mainViewModel.localDeviceName,
                    deviceUuid = mainViewModel.localDeviceUuid,
                    ambientLight = mainViewModel.localAmbientLight.replace(" lx", "").toFloatOrNull() ?: 0f,
                    proximity = mainViewModel.localProximity.replace(" cm", "").toFloatOrNull() ?: 0f
                )
            },
            onRemoteDataReceived = { deviceName, deviceUuid, light, proximity ->
                runOnUiThread {
                    mainViewModel.updateRemoteFromBluetooth(
                        deviceName = deviceName,
                        deviceUuid = deviceUuid,
                        ambientLight = light,
                        proximity = proximity
                    )
                }
            },
            onStatusChanged = { status ->
                runOnUiThread {
                    mainViewModel.updateBluetoothStatus(status)
                }
            }
        )

        setContent {
            MyLabsTheme {
                val photoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicturePreview()
                ) { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        mainViewModel.updateLocalPhoto(bitmap)
                        mainViewModel.addReading(
                            photoBase64 = bitmapToBase64(bitmap)
                        )

                        val lightValue = mainViewModel.localAmbientLight.replace(" lx", "").toFloatOrNull() ?: 0f
                        val proximityValue = mainViewModel.localProximity.replace(" cm", "").toFloatOrNull() ?: 0f

                        bluetoothHelper.sendSensorData(
                            localDeviceName = mainViewModel.localDeviceName,
                            localDeviceUuid = mainViewModel.localDeviceUuid,
                            ambientLight = lightValue,
                            proximity = proximityValue
                        )
                    }
                }

                AppNavigation(
                    mainViewModel = mainViewModel,
                    onTakePhotoRequest = {
                        photoLauncher.launch(null)
                    },
                    onBluetoothServerRequest = {
                        bluetoothHelper.startServer()
                    },
                    onBluetoothConnectRequest = {
                        bluetoothHelper.connectToBondedDevice()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorHelper.startListening()
    }

    override fun onPause() {
        super.onPause()
        sensorHelper.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHelper.closeConnections()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = mutableListOf<String>()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
            }
        }
    }
}
