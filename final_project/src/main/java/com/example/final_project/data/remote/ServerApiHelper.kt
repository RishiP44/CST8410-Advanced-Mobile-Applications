package com.example.final_project.data.remote

import com.example.final_project.model.SensorReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ServerApiHelper {

    // Change this later to your real server URL
    private const val BASE_URL = "https://a0ac-192-197-88-93.ngrok-free.app"

    suspend fun sendReading(reading: SensorReading): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/readings")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val json = JSONObject().apply {
                put("timestamp", reading.timestamp)
                put("ambientLight", reading.ambientLight)
                put("proximity", reading.proximity)
                put("photoBase64", reading.photoBase64)
                put("localDeviceName", reading.localDeviceName)
                put("localDeviceUuid", reading.localDeviceUuid)
                put("remoteDeviceName", reading.remoteDeviceName)
                put("remoteDeviceUuid", reading.remoteDeviceUuid)
            }

            val writer = BufferedWriter(OutputStreamWriter(connection.outputStream))
            writer.write(json.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearServerHistory(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/readings/clear")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
}