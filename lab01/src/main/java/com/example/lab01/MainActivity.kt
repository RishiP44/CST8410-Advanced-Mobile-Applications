package com.example.lab01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.lab01.ui.theme.MyLabsTheme

class MainActivity : ComponentActivity() {

    // Step 1: Two String variables with First and Last name
    private val FirstName: String = "Rishi"
    private val LastName: String = "Patel"

    // Step 2: return a String using $
    private fun printName(): String {
        return "Your first name is: $FirstName and your last name is: $LastName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyLabsTheme {
                // Step 3: Change containerColor to Color.Yellow
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Yellow
                ) { innerPadding ->
                    // Step 4: Use Greeting() and call printName()
                    Greeting(
                        name = printName(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyLabsTheme {
        Greeting(name = "Preview")
    }
}
