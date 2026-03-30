package com.example.lab08

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lab08.ui.theme.MyLabsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link
        val data: Uri? = intent?.data

        var startDestination = "login"

        if (data != null) {
            val host = data.host

            if (host == "profile") {
                startDestination = "profile"

                // Read query parameters
                UserRepository.phone = data.getQueryParameter("phone") ?: ""
                UserRepository.email = data.getQueryParameter("email") ?: ""
                UserRepository.address = data.getQueryParameter("address") ?: ""
            } else if (host == "login") {
                startDestination = "login"
            }
        }

        setContent {
            MyLabsTheme {
                AppNavigation(startDestination)
            }
        }
    }
}