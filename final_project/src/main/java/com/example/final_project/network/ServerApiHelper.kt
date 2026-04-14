package com.example.final_project.network

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ServerApiHelper {

    // Replace this with your laptop/ngrok server URL
    private const val BASE_URL = "https://0352-192-197-88-93.ngrok-free.app"

    fun clearServerHistory(): Boolean {
        return try {
            val url = URL("$BASE_URL/clear-history")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val body = "{}"
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(body)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}