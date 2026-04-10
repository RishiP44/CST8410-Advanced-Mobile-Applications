package com.example.final_project.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorHelper(
    context: Context,
    private val onLightChanged: (Float) -> Unit,
    private val onProximityChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val lightSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val proximitySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    fun startListening() {
        lightSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        proximitySensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                val lightValue = event.values.firstOrNull() ?: 0f
                onLightChanged(lightValue)
            }

            Sensor.TYPE_PROXIMITY -> {
                val proximityValue = event.values.firstOrNull() ?: 0f
                onProximityChanged(proximityValue)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this project right now
    }
}