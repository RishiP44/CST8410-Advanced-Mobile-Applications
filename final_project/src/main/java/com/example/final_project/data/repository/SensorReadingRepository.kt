package com.example.final_project.data.repository

import com.example.final_project.data.local.SensorReadingDao
import com.example.final_project.data.local.SensorReadingEntity
import kotlinx.coroutines.flow.Flow

class SensorReadingRepository(
    private val sensorReadingDao: SensorReadingDao
) {
    fun getAllReadings(): Flow<List<SensorReadingEntity>> {
        return sensorReadingDao.getAllReadings()
    }

    suspend fun insertReading(reading: SensorReadingEntity) {
        sensorReadingDao.insertReading(reading)
    }

    suspend fun clearAllReadings() {
        sensorReadingDao.clearAllReadings()
    }
}