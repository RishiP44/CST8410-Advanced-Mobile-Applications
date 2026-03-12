package com.example.lab05

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                CalculatorScreen()
            }
        }
    }

    @Composable
    fun CalculatorScreen() {
        var num1 by remember { mutableStateOf("") }
        var num2 by remember { mutableStateOf("") }
        var resultText by remember { mutableStateOf("Result will appear here") }
        var lastOp by remember { mutableStateOf("add") }

        // Emulator -> PC server (stable)
        val emulatorBaseUrl = "http://10.0.2.2:8080"

        // Phone -> PC server via ngrok (changes each time you restart ngrok)
        val phoneBaseUrl = "https://bd25-192-197-88-93.ngrok-free.app"

        // CURL uses emulatorBaseUrl (matches what the Android app is doing)
        val curlCommand =
            """curl -X POST "$emulatorBaseUrl/calc" -H "Content-Type: application/json" -d '{"a":${num1.ifBlank { "0" }},"b":${num2.ifBlank { "0" }},"op":"$lastOp"}'"""

        // QR uses phoneBaseUrl so your real phone can open it
        val qrUrl =
            "$phoneBaseUrl/calc?a=${num1.ifBlank { "0" }}&b=${num2.ifBlank { "0" }}&op=$lastOp"

        val qrBitmap = generateQrCode(qrUrl, 600, 600)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("Lab 5 - Client/Server Calculator", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = num1,
                onValueChange = { num1 = it },
                label = { Text("Number 1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = num2,
                onValueChange = { num2 = it },
                label = { Text("Number 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    lastOp = "add"
                    callServer(emulatorBaseUrl, num1, num2, "add") { resultText = it }
                }) { Text("Add") }

                Button(onClick = {
                    lastOp = "subtract"
                    callServer(emulatorBaseUrl, num1, num2, "subtract") { resultText = it }
                }) { Text("Subtract") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    lastOp = "multiply"
                    callServer(emulatorBaseUrl, num1, num2, "multiply") { resultText = it }
                }) { Text("Multiply") }

                Button(onClick = {
                    lastOp = "divide"
                    callServer(emulatorBaseUrl, num1, num2, "divide") { resultText = it }
                }) { Text("Divide") }
            }

            Text("Server Response:")
            Text(resultText)

            HorizontalDivider()

            Text("CURL Command:")
            Text(curlCommand)

            Spacer(modifier = Modifier.height(8.dp))

            Text("QR Code URL:")
            Text(qrUrl)

            Spacer(modifier = Modifier.height(8.dp))

            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }
        }
    }

    private fun callServer(
        baseUrl: String,
        num1: String,
        num2: String,
        op: String,
        onResult: (String) -> Unit
    ) {
        val a = num1.toDoubleOrNull()
        val b = num2.toDoubleOrNull()

        if (a == null || b == null) {
            onResult("Please enter valid numbers")
            return
        }

        val json = JSONObject()
            .put("a", a)
            .put("b", b)
            .put("op", op)
            .toString()

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/calc")
            .post(body)
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: "No response"
                    runOnUiThread { onResult(responseText) }
                }
            } catch (e: Exception) {
                runOnUiThread { onResult("Network error: ${e.message}") }
            }
        }.start()
    }

    private fun generateQrCode(text: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix =
                MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
