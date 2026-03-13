package com.example.lab07

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.lab07.ui.theme.MyLabsTheme
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.UUID

enum class ScreenState {
    Start, Connecting, Connected
}

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
                Lab07Screen(
                    bluetoothManager = bluetoothManager,
                    advertiser = advertiser,
                    bleScanner = bleScanner,
                    isBluetoothReady = (bluetoothAdapter != null && advertiser != null && bleScanner != null)
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun Lab07Screen(
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

    var chatInput by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<String>()) }
    var showDisconnectedDialog by remember { mutableStateOf(false) }
    var lastClientSentMessage by remember { mutableStateOf("") }

    val incomingMessageUuid = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
    val outgoingMessageUuid = UUID.fromString("0000180B-0000-1000-8000-00805F9B34FB")
    val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    var screenState by remember { mutableStateOf(ScreenState.Start) }

    // ---------- Permissions ----------
    val btPermissions = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            else -> {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
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
        message = if (granted) {
            "Bluetooth permissions granted. Try again."
        } else {
            "Bluetooth permissions denied."
        }
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
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

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
                    screenState = ScreenState.Connected
                    chatMessages = emptyList()
                    vibrateOnce()
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false
                    isClientScanning = false
                    screenState = ScreenState.Start
                    connectedDeviceName = ""
                    connectedUuid = null
                    scannedServerUuid = null
                    showDisconnectedDialog = true
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) return

                val service = connectedUuid?.let { gatt.getService(it) } ?: return
                val outgoingCharacteristic = service.getCharacteristic(outgoingMessageUuid) ?: return

                gatt.setCharacteristicNotification(outgoingCharacteristic, true)

                val descriptor = outgoingCharacteristic.getDescriptor(cccdUuid) ?: return
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == outgoingMessageUuid) {
                    val receivedText = characteristic.value?.toString(Charsets.UTF_8).orEmpty()
                    if (receivedText.isNotBlank()) {
                        if (receivedText != lastClientSentMessage) {
                            chatMessages = chatMessages + "Server sent this message: $receivedText"
                        } else {
                            lastClientSentMessage = ""
                        }
                    }
                }
            }
        }
    }

    val gattServerCallback = remember {
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDeviceName = device.name ?: device.address
                    connectedUuid = serverUuid
                    isConnected = true
                    screenState = ScreenState.Connected
                    chatMessages = emptyList()

                    advertiser?.stopAdvertising(advertiseCallback)
                    vibrateOnce()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false
                    screenState = ScreenState.Start
                    connectedDeviceName = ""
                    connectedUuid = null
                    showDisconnectedDialog = true
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (characteristic.uuid == incomingMessageUuid) {
                    val receivedText = value.toString(Charsets.UTF_8)

                    chatMessages = chatMessages + "Client sent this message: $receivedText"

                    val service = serverUuid?.let { gattServer?.getService(it) }
                    val outgoingCharacteristic = service?.getCharacteristic(outgoingMessageUuid)

                    outgoingCharacteristic?.value = receivedText.toByteArray(Charsets.UTF_8)

                    if (outgoingCharacteristic != null) {
                        gattServer?.notifyCharacteristicChanged(device, outgoingCharacteristic, false)
                    }

                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            value
                        )
                    }
                } else {
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            offset,
                            null
                        )
                    }
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (descriptor.uuid == cccdUuid) {
                    descriptor.value = value

                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            value
                        )
                    }
                } else {
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            offset,
                            null
                        )
                    }
                }
            }
        }
    }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val target = scannedServerUuid ?: return
                val device = result.device ?: return

                val advertisedUuids = result.scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()
                if (!advertisedUuids.contains(target)) return

                try {
                    bleScanner?.stopScan(this)
                } catch (_: Exception) {
                }

                isClientScanning = false
                message = "trying to connect to $target"
                screenState = ScreenState.Connecting

                gattClient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    @Suppress("DEPRECATION")
                    device.connectGatt(context, false, gattClientCallback)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                isClientScanning = false
                message = "BLE scan failed: $errorCode"
            }
        }
    }

    fun stopServerNow() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (_: Exception) {
        }
        try {
            gattServer?.close()
        } catch (_: Exception) {
        }

        gattServer = null
        isServerRunning = false
        serverUuid = null
        showQrScreen = false
        isConnected = false
        connectedDeviceName = ""
        connectedUuid = null
        scannedServerUuid = null
        screenState = ScreenState.Start
        chatMessages = emptyList()
        chatInput = ""
        message = ""
        lastClientSentMessage = ""
    }

    fun stopClientNow() {
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }

        try {
            gattClient?.disconnect()
        } catch (_: Exception) {
        }

        try {
            gattClient?.close()
        } catch (_: Exception) {
        }

        gattClient = null
        scannedServerUuid = null
        isClientScanning = false
        isConnected = false
        connectedDeviceName = ""
        connectedUuid = null
        screenState = ScreenState.Start
        chatMessages = emptyList()
        chatInput = ""
        message = ""
        lastClientSentMessage = ""
    }

    fun launchQrScanner() {
        val moduleInstallClient = ModuleInstall.getClient(context)
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(qrScanner)
            .build()

        moduleInstallClient.areModulesAvailable(qrScanner)
            .addOnSuccessListener { availability ->
                if (availability.areModulesAvailable()) {
                    qrScanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val raw = barcode.rawValue?.trim().orEmpty()

                            try {
                                val uuid = UUID.fromString(raw)
                                scannedServerUuid = uuid
                                isConnected = false
                                connectedUuid = null
                                connectedDeviceName = ""
                                screenState = ScreenState.Connecting
                                chatMessages = emptyList()
                                chatInput = ""
                                message = "trying to connect to $uuid"

                                val settings = ScanSettings.Builder()
                                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                    .build()

                                isClientScanning = true
                                bleScanner?.startScan(null, settings, scanCallback)
                            } catch (_: Exception) {
                                scannedServerUuid = null
                                message = "QR did not contain a valid UUID."
                            }
                        }
                        .addOnCanceledListener {
                            message = "Scan cancelled."
                        }
                        .addOnFailureListener { e ->
                            val details = buildString {
                                append("Scan failed")
                                append("\nType: ${e::class.java.simpleName}")
                                append("\nMessage: ${e.message ?: "no message"}")
                                if (e is MlKitException) {
                                    append("\nCode: ${e.errorCode}")
                                }
                            }
                            message = details
                            Log.e("QR_SCAN", details, e)
                        }
                } else {
                    message = "Installing scanner module. Tap Start Client again."
                    moduleInstallClient.installModules(moduleInstallRequest)
                        .addOnSuccessListener {
                            message = "Scanner module installed. Tap Start Client again."
                        }
                        .addOnFailureListener { e ->
                            message = "Scanner module install failed: ${e.message}"
                            Log.e("QR_SCAN", "Module install failed", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                message = "Module availability check failed: ${e.message}"
                Log.e("QR_SCAN", "Availability check failed", e)
            }
    }

    // ---------- QR URL ----------
    val qrUrl = remember(serverUuid) {
        if (serverUuid != null) {
            "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=${serverUuid.toString()}"
        } else {
            ""
        }
    }

    // ---------- Display message ----------
    val displayMessage = when {
        !isBluetoothReady -> "Bluetooth LE not available on this device."
        isConnected && connectedUuid != null ->
            "Connected\nDevice: $connectedDeviceName\nUUID: $connectedUuid"

        isClientScanning -> "Scanning for server..."
        screenState == ScreenState.Connecting && scannedServerUuid != null ->
            "Trying to connect to $scannedServerUuid"

        message.isNotBlank() -> message
        isServerRunning && serverUuid != null -> "Advertising UUID:\n$serverUuid"
        else -> "Welcome to Lab 7 Bluetooth Server/Client."
    }

    // ---------- UI ----------
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = {
                        if (!isBluetoothReady) return@Button

                        if (!hasBtPermissions()) {
                            btPermissionLauncher.launch(btPermissions)
                            return@Button
                        }

                        if (!isServerRunning) {
                            isConnected = false
                            connectedUuid = null
                            connectedDeviceName = ""
                            stopClientNow()
                            screenState = ScreenState.Start
                            chatMessages = emptyList()
                            chatInput = ""

                            serverUuid = UUID.randomUUID()

                            val mgr = bluetoothManager
                            if (mgr == null) {
                                message = "BluetoothManager null."
                                return@Button
                            }

                            gattServer?.close()
                            gattServer = mgr.openGattServer(context, gattServerCallback)

                            val service = BluetoothGattService(
                                serverUuid,
                                BluetoothGattService.SERVICE_TYPE_PRIMARY
                            )

                            val incomingCharacteristic = BluetoothGattCharacteristic(
                                incomingMessageUuid,
                                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
                                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
                            )

                            val outgoingCharacteristic = BluetoothGattCharacteristic(
                                outgoingMessageUuid,
                                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                                BluetoothGattCharacteristic.PERMISSION_READ
                            )

                            val notificationDescriptor = BluetoothGattDescriptor(
                                cccdUuid,
                                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
                            )

                            outgoingCharacteristic.addDescriptor(notificationDescriptor)

                            service.addCharacteristic(incomingCharacteristic)
                            service.addCharacteristic(outgoingCharacteristic)

                            gattServer?.addService(service)

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
                            message = ""
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

                Button(
                    onClick = {
                        if (isConnected && !isServerRunning) {
                            stopClientNow()
                            return@Button
                        }

                        if (isServerRunning && serverUuid != null) {
                            showQrScreen = !showQrScreen
                            return@Button
                        }

                        if (!hasBtPermissions()) {
                            btPermissionLauncher.launch(btPermissions)
                            return@Button
                        }

                        launchQrScanner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    Text(
                        when {
                            isConnected && !isServerRunning -> "Disconnect"
                            isServerRunning -> if (showQrScreen) "Back" else "Show QR"
                            else -> "Start Client"
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
                Text(
                    "Scan this QR code on the client:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                AsyncImage(
                    model = qrUrl,
                    contentDescription = "Server UUID QR Code",
                    modifier = Modifier.size(300.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("UUID:\n$serverUuid", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(displayMessage, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                if (screenState == ScreenState.Connected) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        label = { Text("Enter message") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val textToSend = chatInput.trim()
                            if (textToSend.isBlank()) return@Button

                            if (isServerRunning) {
                                val service = serverUuid?.let { gattServer?.getService(it) }
                                val outgoingCharacteristic = service?.getCharacteristic(outgoingMessageUuid)

                                outgoingCharacteristic?.value = textToSend.toByteArray(Charsets.UTF_8)

                                val connectedDevices =
                                    bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT_SERVER)

                                connectedDevices?.forEach { device ->
                                    if (outgoingCharacteristic != null) {
                                        gattServer?.notifyCharacteristicChanged(
                                            device,
                                            outgoingCharacteristic,
                                            false
                                        )
                                    }
                                }

                                chatMessages = chatMessages + "Server sent this message: $textToSend"
                            } else {
                                val service = connectedUuid?.let { gattClient?.getService(it) }
                                val incomingCharacteristic = service?.getCharacteristic(incomingMessageUuid)

                                incomingCharacteristic?.value = textToSend.toByteArray(Charsets.UTF_8)

                                if (incomingCharacteristic != null) {
                                    gattClient?.writeCharacteristic(incomingCharacteristic)
                                    chatMessages = chatMessages + "Client sent this message: $textToSend"
                                    lastClientSentMessage = textToSend
                                }
                            }

                            chatInput = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    chatMessages.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    if (showDisconnectedDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectedDialog = false
                        screenState = ScreenState.Start
                        connectedDeviceName = ""
                        connectedUuid = null
                        scannedServerUuid = null
                        chatMessages = emptyList()
                        chatInput = ""
                        message = ""
                        lastClientSentMessage = ""
                    }
                ) {
                    Text("Ok")
                }
            },
            title = { Text("Connection Terminated") },
            text = { Text("The connection has ended.") }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                advertiser?.stopAdvertising(advertiseCallback)
            } catch (_: Exception) {
            }
            try {
                bleScanner?.stopScan(scanCallback)
            } catch (_: Exception) {
            }
            try {
                gattServer?.close()
            } catch (_: Exception) {
            }
            try {
                gattClient?.close()
            } catch (_: Exception) {
            }
        }
    }
}