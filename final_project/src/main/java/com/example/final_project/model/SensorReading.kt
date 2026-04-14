package com.example.final_project.model

data class SensorReading(
    val id: Int = 0,
    val timestamp: Long,
    val ambientLight: Float,
    val proximity: Float,
    val photoBase64: String,
    val localDeviceName: String,
    val localDeviceUuid: String,
    val remoteDeviceName: String,
    val remoteDeviceUuid: String,
    val remoteAmbientLight: String = "",
    val remoteProximity: String = ""
)