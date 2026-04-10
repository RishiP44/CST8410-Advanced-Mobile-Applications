package com.example.final_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val ambientLight: Float,
    val proximity: Float,
    val photoBase64: String,
    val localDeviceName: String,
    val localDeviceUuid: String,
    val remoteDeviceName: String,
    val remoteDeviceUuid: String
)