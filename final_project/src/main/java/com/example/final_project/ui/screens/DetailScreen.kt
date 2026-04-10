package com.example.final_project.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.final_project.model.SensorReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavHostController,
    reading: SensorReading?
) {
    val formattedTime = if (reading != null) {
        SimpleDateFormat(
            "yyyy-MM-dd hh:mm a",
            Locale.getDefault()
        ).format(Date(reading.timestamp))
    } else {
        "Unknown"
    }

    val decodedBitmap = remember(reading?.photoBase64) {
        try {
            if (!reading?.photoBase64.isNullOrBlank()) {
                val imageBytes = Base64.decode(reading?.photoBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Details") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (reading != null) {
                        Text("Time: $formattedTime")
                        Text("Ambient Light: ${reading.ambientLight} lx")
                        Text("Proximity: ${reading.proximity} cm")
                        Text("Local Device Name: ${reading.localDeviceName}")
                        Text("Local Device UUID: ${reading.localDeviceUuid}")
                        Text("Remote Device Name: ${reading.remoteDeviceName}")
                        Text("Remote Device UUID: ${reading.remoteDeviceUuid}")

                        if (decodedBitmap != null) {
                            Text("Saved Photo:")
                            Image(
                                bitmap = decodedBitmap.asImageBitmap(),
                                contentDescription = "Saved reading photo",
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(180.dp)
                            )
                        } else {
                            Text("Photo: No photo saved")
                        }
                    } else {
                        Text("No reading found")
                    }
                }
            }

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}