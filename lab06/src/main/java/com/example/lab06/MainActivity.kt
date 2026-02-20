package com.example.lab06

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.lab06.ui.theme.MyLabsTheme
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var bleScanner: BluetoothLeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        bleScanner = bluetoothAdapter?.bluetoothLeScanner

        enableEdgeToEdge()

        setContent {
            MyLabsTheme {
                Lab06Screen(
                    bluetoothManager = bluetoothManager,
                    advertiser = advertiser,
                    bleScanner = bleScanner,
                    isBluetoothReady = (bluetoothAdapter != null && advertiser != null && bleScanner != null)
                )
            }
        }
    }
}

@SuppressLint("MissingPermission") // runtime permissions are requested in-app for Android 12+
@Composable
fun Lab06Screen(
    bluetoothManager: BluetoothManager?,
    advertiser: BluetoothLeAdvertiser?,
    bleScanner: BluetoothLeScanner?,
    isBluetoothReady: Boolean
) {
    val context = LocalContext.current

    // ---------- UI + State ----------
    var message by remember { mutableStateOf("") }

    var isServerRunning by remember { mutableStateOf(false) }
    var showQrScreen by remember { mutableStateOf(false) }
    var serverUuid by remember { mutableStateOf<UUID?>(null) }

    var scannedServerUuid by remember { mutableStateOf<UUID?>(null) }
    var isClientScanning by remember { mutableStateOf(false) }

    var isConnected by remember { mutableStateOf(false) }
    var connectedDeviceName by remember { mutableStateOf("") }
    var connectedUuid by remember { mutableStateOf<UUID?>(null) }

    // ---------- Permissions ----------
    val btPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else emptyArray()
    }

    fun hasBtPermissions(): Boolean {
        if (btPermissions.isEmpty()) return true
        return btPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        val granted = results.values.all { it }
        message = if (granted) "Bluetooth permissions granted. Try again." else "Bluetooth permissions denied."
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        message = if (granted) "Camera granted. Tap Start Client again." else "Camera denied."
    }

    // ---------- Vibration ----------
    fun vibrateOnce() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(android.os.Vibrator::class.java)
                @Suppress("DEPRECATION")
                v.vibrate(150)
            }
        } catch (_: Exception) {
        }
    }

    // ---------- QR Scanner ----------
    val qrScanner = remember {
        val options = GmsBarcodeScannerOptions.Builder().build()
        GmsBarcodeScanning.getClient(context, options)
    }

    // ---------- BLE callbacks & objects ----------
    var gattServer by remember { mutableStateOf<BluetoothGattServer?>(null) }
    var gattClient by remember { mutableStateOf<BluetoothGatt?>(null) }

    val advertiseCallback = remember {
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d("BLE", "Advertising started")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e("BLE", "Advertising failed: $errorCode")
                message = "Advertising failed: $errorCode"
                isServerRunning = false
                serverUuid = null
                showQrScreen = false
            }
        }
    }

    val gattClientCallback = remember {
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDeviceName = gatt.device.name ?: gatt.device.address
                    connectedUuid = scannedServerUuid
                    isConnected = true
                    isClientScanning = false
                    vibrateOnce()
                }
            }
        }
    }

    val gattServerCallback = remember {
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDeviceName = device.name ?: device.address
                    connectedUuid = serverUuid
                    isConnected = true

                    // Requirement: Server stops advertising when connected
                    advertiser?.stopAdvertising(advertiseCallback)

                    // Requirement: vibrate on connection
                    vibrateOnce()
                }
            }
        }
    }

    // Scan callback must be one stable instance
    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val target = scannedServerUuid ?: return
                val device = result.device ?: return

                // Stop scanning once matched
                try {
                    bleScanner?.stopScan(this)
                } catch (_: Exception) { }

                isClientScanning = false
                message = "trying to connect to $target"

                // Connect GATT
                gattClient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    @Suppress("DEPRECATION")
                    device.connectGatt(context, false, gattClientCallback)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                isClientScanning = false
                message = "Scan failed: $errorCode"
            }
        }
    }

    fun stopServerNow() {
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
        isServerRunning = false
        serverUuid = null
        showQrScreen = false
    }

    fun stopClientNow() {
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: Exception) { }

        isClientScanning = false
        gattClient?.close()
        gattClient = null
        scannedServerUuid = null
    }

    // ---------- QR URL ----------
    val qrUrl = remember(serverUuid) {
        if (serverUuid != null) {
            "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=$serverUuid"
        } else ""
    }

    // ---------- Display message ----------
    val displayMessage = when {
        !isBluetoothReady -> "Bluetooth LE not available on this device."
        isConnected && connectedUuid != null ->
            "connected\nDevice: $connectedDeviceName\nUUID: $connectedUuid"
        isClientScanning -> "Scanning for server..."
        scannedServerUuid != null -> "trying to connect to $scannedServerUuid"
        message.isNotBlank() -> message
        isServerRunning && serverUuid != null -> "Advertising UUID:\n$serverUuid"
        else -> "Welcome to Lab 6 Bluetooth Server/Client."
    }

    // ---------- UI ----------
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar {

                // Left button: Start/Stop Server
                Button(
                    onClick = {
                        if (!isBluetoothReady) return@Button

                        if (!hasBtPermissions()) {
                            btPermissionLauncher.launch(btPermissions)
                            return@Button
                        }

                        if (!isServerRunning) {
                            // Start server: UUID + GATT service + advertise
                            isConnected = false
                            connectedUuid = null
                            connectedDeviceName = ""
                            stopClientNow()

                            serverUuid = UUID.randomUUID()

                            val mgr = bluetoothManager
                            if (mgr == null) {
                                message = "BluetoothManager null."
                                return@Button
                            }

                            // Create GATT server with service UUID
                            gattServer?.close()
                            gattServer = mgr.openGattServer(context, gattServerCallback)

                            val service = BluetoothGattService(
                                serverUuid,
                                BluetoothGattService.SERVICE_TYPE_PRIMARY
                            )

                            // Minimal characteristic so service is valid
                            val characteristic = BluetoothGattCharacteristic(
                                UUID.randomUUID(),
                                BluetoothGattCharacteristic.PROPERTY_READ,
                                BluetoothGattCharacteristic.PERMISSION_READ
                            )
                            service.addCharacteristic(characteristic)
                            gattServer?.addService(service)

                            // Advertise UUID
                            val settings = AdvertiseSettings.Builder()
                                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                                .setConnectable(true)
                                .build()

                            val data = AdvertiseData.Builder()
                                .addServiceUuid(ParcelUuid(serverUuid))
                                .setIncludeDeviceName(true)
                                .build()

                            advertiser?.startAdvertising(settings, data, advertiseCallback)

                            isServerRunning = true
                            showQrScreen = false

                        } else {
                            stopServerNow()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    Text(if (isServerRunning) "Stop Server" else "Start Server")
                }

                // Right button: Show QR/Back when server running, otherwise Start Client
                Button(
                    onClick = {
                        // Server mode: show QR
                        if (isServerRunning && serverUuid != null) {
                            showQrScreen = !showQrScreen
                            return@Button
                        }

                        // Client mode: scan QR then BLE scan -> connect
                        if (!hasCameraPermission()) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            return@Button
                        }

                        if (!hasBtPermissions()) {
                            btPermissionLauncher.launch(btPermissions)
                            return@Button
                        }

                        qrScanner.startScan()
                            .addOnSuccessListener { barcode ->
                                val raw = barcode.rawValue?.trim().orEmpty()
                                try {
                                    val uuid = UUID.fromString(raw)
                                    scannedServerUuid = uuid
                                    isConnected = false
                                    connectedUuid = null
                                    connectedDeviceName = ""
                                    message = "trying to connect to $uuid"

                                    // BLE scan filter by UUID
                                    val filter = ScanFilter.Builder()
                                        .setServiceUuid(ParcelUuid(uuid))
                                        .build()

                                    val settings = ScanSettings.Builder()
                                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                        .build()

                                    isClientScanning = true
                                    bleScanner?.startScan(listOf(filter), settings, scanCallback)

                                } catch (_: Exception) {
                                    scannedServerUuid = null
                                    message = "QR did not contain a valid UUID."
                                }
                            }
                            .addOnFailureListener {
                                message = "Scan cancelled or failed."
                            }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    Text(
                        if (isServerRunning) {
                            if (showQrScreen) "Back" else "Show QR"
                        } else {
                            "Start Client"
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (showQrScreen && serverUuid != null) {
                Text("Scan this QR code on the client:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                AsyncImage(
                    model = qrUrl,
                    contentDescription = "Server UUID QR Code"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("UUID:\n$serverUuid", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(displayMessage, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            try { advertiser?.stopAdvertising(advertiseCallback) } catch (_: Exception) { }
            try { bleScanner?.stopScan(scanCallback) } catch (_: Exception) { }
            try { gattServer?.close() } catch (_: Exception) { }
            try { gattClient?.close() } catch (_: Exception) { }
        }
    }
}