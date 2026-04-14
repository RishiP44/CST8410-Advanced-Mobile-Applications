package com.example.final_project.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.final_project.data.local.SensorReadingEntity
import com.example.final_project.data.repository.SensorReadingRepository
import com.example.final_project.model.SensorReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val repository: SensorReadingRepository
) : ViewModel() {

    var localAmbientLight by mutableStateOf("--")
        private set

    var localProximity by mutableStateOf("--")
        private set

    var localPhotoBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var remoteDeviceName by mutableStateOf("--")
        private set

    var remoteDeviceUuid by mutableStateOf("--")
        private set

    var remoteAmbientLight by mutableStateOf("--")
        private set

    var remoteProximity by mutableStateOf("--")
        private set

    var remotePhotoBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var localDeviceName by mutableStateOf("My Device")
        private set

    var localDeviceUuid by mutableStateOf(UUID.randomUUID().toString())
        private set

    var isServerRunning by mutableStateOf(false)
        private set

    var serverStatusText by mutableStateOf("Server not started")
        private set

    var showQrSection by mutableStateOf(false)
        private set

    var bluetoothStatusText by mutableStateOf("Bluetooth idle")
        private set

    private val _history = MutableStateFlow<List<SensorReading>>(emptyList())
    val history: StateFlow<List<SensorReading>> = _history.asStateFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.getAllReadings().collect { entityList ->
                _history.value = entityList.map { entity ->
                    mapEntityToModel(entity)
                }
            }
        }
    }

    fun updateLocalLight(value: Float) {
        localAmbientLight = "${value} lx"
    }

    fun updateLocalProximity(value: Float) {
        localProximity = "${value} cm"
    }

    fun updateLocalPhoto(bitmap: Bitmap?) {
        localPhotoBitmap = bitmap
    }

    fun updateRemoteDeviceInfo(name: String, uuid: String) {
        remoteDeviceName = name
        remoteDeviceUuid = uuid
    }

    fun updateRemoteLight(value: Float) {
        remoteAmbientLight = "${value} lx"
    }

    fun updateRemoteProximity(value: Float) {
        remoteProximity = "${value} cm"
    }

    fun updateRemotePhoto(bitmap: Bitmap?) {
        remotePhotoBitmap = bitmap
    }

    fun updateBluetoothStatus(status: String) {
        bluetoothStatusText = status
    }

    fun updateRemoteFromBluetooth(
        deviceName: String,
        deviceUuid: String,
        ambientLight: Float,
        proximity: Float
    ) {
        remoteDeviceName = deviceName
        remoteDeviceUuid = deviceUuid
        remoteAmbientLight = "$ambientLight lx"
        remoteProximity = "$proximity cm"
        bluetoothStatusText = "Remote data received"
    }

    fun startServer() {
        isServerRunning = true
        showQrSection = true
        serverStatusText = "Server started. Share this UUID / QR to connect."

        if (remoteDeviceName == "--") {
            remoteDeviceName = "Waiting for connection..."
        }

        if (remoteDeviceUuid == "--") {
            remoteDeviceUuid = "Not connected yet"
        }
    }

    fun stopServer() {
        isServerRunning = false
        showQrSection = false
        serverStatusText = "Server not started"
        remoteDeviceName = "--"
        remoteDeviceUuid = "--"
        remoteAmbientLight = "--"
        remoteProximity = "--"
        remotePhotoBitmap = null
        bluetoothStatusText = "Bluetooth idle"
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllReadings()
            com.example.final_project.data.remote.ServerApiHelper.clearServerHistory()
        }
    }

    fun addReading(photoBase64: String = "") {
        val lightValue = localAmbientLight.replace(" lx", "").toFloatOrNull() ?: 0f
        val proximityValue = localProximity.replace(" cm", "").toFloatOrNull() ?: 0f

        val newReading = SensorReading(
            id = 0,
            timestamp = System.currentTimeMillis(),
            ambientLight = lightValue,
            proximity = proximityValue,
            photoBase64 = photoBase64,
            localDeviceName = localDeviceName,
            localDeviceUuid = localDeviceUuid,
            remoteDeviceName = remoteDeviceName,
            remoteDeviceUuid = remoteDeviceUuid,
            remoteAmbientLight = remoteAmbientLight,
            remoteProximity = remoteProximity
        )

        val entity = SensorReadingEntity(
            timestamp = newReading.timestamp,
            ambientLight = newReading.ambientLight,
            proximity = newReading.proximity,
            photoBase64 = newReading.photoBase64,
            localDeviceName = newReading.localDeviceName,
            localDeviceUuid = newReading.localDeviceUuid,
            remoteDeviceName = newReading.remoteDeviceName,
            remoteDeviceUuid = newReading.remoteDeviceUuid
        )

        viewModelScope.launch {
            repository.insertReading(entity)
            com.example.final_project.data.remote.ServerApiHelper.sendReading(newReading)
        }
    }

    fun simulateRemoteConnection() {
        remoteDeviceName = "Pixel Remote"
        remoteDeviceUuid = UUID.randomUUID().toString()
        remoteAmbientLight = "220.0 lx"
        remoteProximity = "2.0 cm"
        serverStatusText = "Remote device connected"
    }

    private fun mapEntityToModel(entity: SensorReadingEntity): SensorReading {
        return SensorReading(
            id = entity.id,
            timestamp = entity.timestamp,
            ambientLight = entity.ambientLight,
            proximity = entity.proximity,
            photoBase64 = entity.photoBase64,
            localDeviceName = entity.localDeviceName,
            localDeviceUuid = entity.localDeviceUuid,
            remoteDeviceName = entity.remoteDeviceName,
            remoteDeviceUuid = entity.remoteDeviceUuid
        )
    }
}