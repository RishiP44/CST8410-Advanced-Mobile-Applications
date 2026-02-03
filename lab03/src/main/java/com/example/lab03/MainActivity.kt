package com.example.lab03

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {

    private fun requestActivityRecognitionPermissionIfNeeded() {
        // Only needed on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    1001
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestActivityRecognitionPermissionIfNeeded()

        setContent {
            SensorScreen()
        }
    }
}

@Composable
fun SensorScreen() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // UI state (auto-refreshes UI)
    var lightValue by remember { mutableStateOf("Waiting...") }
    var accelValue by remember { mutableStateOf("Waiting...") }
    var stepValue by remember { mutableStateOf("Waiting...") }

    val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_LIGHT -> {
                        lightValue = "${event.values[0]} lx"
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        accelValue = "X: $x  Y: $y  Z: $z"
                    }

                    Sensor.TYPE_STEP_COUNTER -> {
                        stepValue = event.values[0].toString()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // Register/unregister sensors with lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                lightSensor?.let {
                    sensorManager.registerListener(
                        sensorListener,
                        it,
                        SensorManager.SENSOR_DELAY_UI
                    )
                } ?: run { lightValue = "Not available" }

                accelSensor?.let {
                    sensorManager.registerListener(
                        sensorListener,
                        it,
                        SensorManager.SENSOR_DELAY_UI
                    )
                } ?: run { accelValue = "Not available" }

                stepSensor?.let {
                    sensorManager.registerListener(
                        sensorListener,
                        it,
                        SensorManager.SENSOR_DELAY_UI
                    )
                } ?: run { stepValue = "Not available" }
            }

            if (event == Lifecycle.Event.ON_STOP) {
                sensorManager.unregisterListener(sensorListener)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        SensorRow(
            icon = R.drawable.ic_light,
            label = "Light",
            value = lightValue,
            tag = "light_text"
        )

        SensorRow(
            icon = R.drawable.ic_accelerometer,
            label = "Accelerometer",
            value = accelValue,
            tag = "accel_text"
        )

        SensorRow(
            icon = R.drawable.ic_steps,
            label = "Steps",
            value = stepValue,
            tag = "steps_text"
        )
    }
}

@Composable
fun SensorRow(
    icon: Int,
    label: String,
    value: String,
    tag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(tag)
        )
    }
}
