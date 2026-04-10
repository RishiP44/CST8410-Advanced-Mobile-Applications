package com.example.final_project.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.final_project.utils.QrCodeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,

    localAmbientLight: String,
    localProximity: String,
    localPhotoBitmap: Bitmap?,

    remoteDeviceName: String,
    remoteDeviceUuid: String,
    remoteAmbientLight: String,
    remoteProximity: String,
    remotePhotoBitmap: Bitmap?,

    localDeviceName: String,
    localDeviceUuid: String,
    serverStatusText: String,
    showQrSection: Boolean,
    bluetoothStatusText: String,

    onStartServerClick: () -> Unit,
    onBluetoothConnectClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onClearHistoryClick: () -> Unit
) {
    val qrBitmap = remember(localDeviceUuid, showQrSection) {
        if (showQrSection) {
            QrCodeHelper.generateQrCode(localDeviceUuid)
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Sensor Recorder") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LOCAL DEVICE")
                    Text("Name: $localDeviceName")
                    Text("UUID: $localDeviceUuid")
                    Text("Light: $localAmbientLight")
                    Text("Proximity: $localProximity")

                    localPhotoBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Local Photo",
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SERVER STATUS")
                    Text(serverStatusText)

                    if (showQrSection) {
                        Text("Share this UUID / QR for pairing")
                        Text("Bluetooth UUID: $localDeviceUuid")

                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .size(220.dp)
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BLUETOOTH STATUS")
                    Text(bluetoothStatusText)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("REMOTE DEVICE")
                    Text("Name: $remoteDeviceName")
                    Text("UUID: $remoteDeviceUuid")
                    Text("Light: $remoteAmbientLight")
                    Text("Proximity: $remoteProximity")

                    remotePhotoBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Remote Photo",
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Button(
                    onClick = onStartServerClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Bluetooth Server + Show QR")
                }

                Button(
                    onClick = onBluetoothConnectClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to Paired Device")
                }

                Button(
                    onClick = onTakePhotoClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take Photo & Save")
                }

                Button(
                    onClick = { navController.navigate("history") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View History")
                }

                Button(
                    onClick = onClearHistoryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear History")
                }
            }
        }
    }
}