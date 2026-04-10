package com.example.final_project.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import kotlin.concurrent.thread

class BluetoothHelper(
    private val context: Context,
    private val onRemoteDataReceived: (deviceName: String, deviceUuid: String, light: Float, proximity: Float) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {

    companion object {
        const val APP_NAME = "FinalProjectBluetooth"
        val APP_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        if (bluetoothAdapter == null) {
            onStatusChanged("Bluetooth not supported")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            onStatusChanged("Bluetooth is off")
            return
        }

        if (!hasBluetoothConnectPermission()) {
            onStatusChanged("Bluetooth permission missing")
            return
        }

        thread {
            try {
                onStatusChanged("Starting Bluetooth server...")
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                val socket = serverSocket?.accept()
                clientSocket = socket

                val remoteName = socket?.remoteDevice?.name ?: "Unknown Device"
                onStatusChanged("Connected to $remoteName")

                socket?.let { listenForIncomingData(it) }
            } catch (e: Exception) {
                onStatusChanged("Server error: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToBondedDevice() {
        if (bluetoothAdapter == null) {
            onStatusChanged("Bluetooth not supported")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            onStatusChanged("Bluetooth is off")
            return
        }

        if (!hasBluetoothConnectPermission()) {
            onStatusChanged("Bluetooth permission missing")
            return
        }

        val bondedDevices = bluetoothAdapter.bondedDevices
        if (bondedDevices.isEmpty()) {
            onStatusChanged("No bonded devices found")
            return
        }

        val targetDevice: BluetoothDevice = bondedDevices.first()

        thread {
            try {
                onStatusChanged("Connecting to ${targetDevice.name}...")
                val socket = targetDevice.createRfcommSocketToServiceRecord(APP_UUID)
                bluetoothAdapter.cancelDiscovery()
                socket.connect()
                clientSocket = socket

                onStatusChanged("Connected to ${targetDevice.name}")
                listenForIncomingData(socket)
            } catch (e: Exception) {
                onStatusChanged("Client error: ${e.message}")
            }
        }
    }

    fun sendSensorData(
        localDeviceName: String,
        localDeviceUuid: String,
        ambientLight: Float,
        proximity: Float
    ) {
        val socket = clientSocket ?: return

        thread {
            try {
                val json = JSONObject().apply {
                    put("deviceName", localDeviceName)
                    put("deviceUuid", localDeviceUuid)
                    put("ambientLight", ambientLight)
                    put("proximity", proximity)
                }

                val writer = OutputStreamWriter(socket.outputStream)
                writer.write(json.toString() + "\n")
                writer.flush()

                onStatusChanged("Sensor data sent")
            } catch (e: Exception) {
                onStatusChanged("Send failed: ${e.message}")
            }
        }
    }

    private fun listenForIncomingData(socket: BluetoothSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))

            while (true) {
                val line = reader.readLine() ?: break
                val json = JSONObject(line)

                val deviceName = json.optString("deviceName", "Unknown Device")
                val deviceUuid = json.optString("deviceUuid", "Unknown UUID")
                val light = json.optDouble("ambientLight", 0.0).toFloat()
                val proximity = json.optDouble("proximity", 0.0).toFloat()

                onRemoteDataReceived(deviceName, deviceUuid, light, proximity)
            }
        } catch (e: Exception) {
            onStatusChanged("Receive failed: ${e.message}")
        }
    }

    fun closeConnections() {
        try {
            clientSocket?.close()
        } catch (_: Exception) {
        }

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
    }
}