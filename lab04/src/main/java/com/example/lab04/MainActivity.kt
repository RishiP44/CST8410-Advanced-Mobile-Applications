package com.example.lab04

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Lab04Screen()
            }
        }
    }
}

@Composable
fun Lab04Screen() {

    // REQUIRED: each TextField has its own remember { mutableStateOf() }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Dialog visibility
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load saved values on app launch (or when screen first appears)
    LaunchedEffect(Unit) {
        val prefs = getEncryptedPrefs(context)
        firstName = prefs.getString(KEY_FIRST, "") ?: ""
        lastName = prefs.getString(KEY_LAST, "") ?: ""
        address = prefs.getString(KEY_ADDRESS, "") ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("User Profile", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Profile")
        }
    }

    // Permission dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Permission to collect profile data") },
            text = {
                Text(
                    "We would like to collect your first name, last name, and address.\n\n" +
                            "Why: We use customer location information to decide where to build our next store.\n\n" +
                            "This information is NOT required to use our services. You may decline."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Agree -> save encrypted
                        val prefs = getEncryptedPrefs(context)
                        prefs.edit()
                            .putString(KEY_FIRST, firstName)
                            .putString(KEY_LAST, lastName)
                            .putString(KEY_ADDRESS, address)
                            .apply()

                        showDialog = false
                    }
                ) {
                    Text("Agree")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Decline -> clear encrypted prefs + clear UI fields
                        val prefs = getEncryptedPrefs(context)
                        prefs.edit().clear().apply()

                        firstName = ""
                        lastName = ""
                        address = ""

                        showDialog = false
                    }
                ) {
                    Text("Decline")
                }
            }
        )
    }
}

/** Creates/returns the encrypted SharedPreferences for Lab 4 */
private fun getEncryptedPrefs(context: Context) =
    EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

private const val PREFS_NAME = "lab04_profile_prefs"
private const val KEY_FIRST = "first_name"
private const val KEY_LAST = "last_name"
private const val KEY_ADDRESS = "address"
