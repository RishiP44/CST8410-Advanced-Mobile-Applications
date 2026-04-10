package com.example.final_project.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorReadingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: SensorReadingEntity)

    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<SensorReadingEntity>>

    @Query("DELETE FROM sensor_readings")
    suspend fun clearAllReadings()
}