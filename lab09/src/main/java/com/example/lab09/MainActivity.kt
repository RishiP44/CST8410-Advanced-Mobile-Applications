package com.example.lab09

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lab09.ui.theme.MyLabsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyLabsTheme {
                ChatApp(viewModel)
            }
        }
    }
}

@Composable
fun ChatApp(viewModel: MessageViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            ChatScreen(
                viewModel = viewModel,
                onMessageClick = { messageId ->
                    navController.navigate("details/$messageId")
                }
            )
        }

        composable(
            route = "details/{messageId}",
            arguments = listOf(navArgument("messageId") { type = NavType.IntType })
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getInt("messageId") ?: 0

            DetailsScreen(
                messageId = messageId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MessageViewModel,
    onMessageClick: (Int) -> Unit
) {
    val messages by viewModel.allMessages.collectAsState()
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lab 9 Chat App") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Enter message") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.padding(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        viewModel.insertMessage(messageText, "receive")
                        messageText = ""
                    }
                ) {
                    Text("Receive")
                }

                Button(
                    onClick = {
                        viewModel.insertMessage(messageText, "send")
                        messageText = ""
                    }
                ) {
                    Text("Send")
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                items(messages) { message ->
                    MessageRow(
                        message = message,
                        onClick = { onMessageClick(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageRow(
    message: ChatMessage,
    onClick: () -> Unit
) {
    val isSend = message.type == "send"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        horizontalArrangement = if (isSend) Arrangement.End else Arrangement.Start
    ) {
        if (!isSend) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Receive Avatar",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.text)
                Spacer(modifier = Modifier.padding(2.dp))
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (isSend) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Send Avatar",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    messageId: Int,
    viewModel: MessageViewModel,
    onBack: () -> Unit
) {
    var message by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(messageId) {
        message = viewModel.getMessageById(messageId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Details") }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            message?.let { currentMessage ->
                Column {
                    Text(
                        text = "Message: ${currentMessage.text}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.padding(6.dp))

                    Text(text = "Type: ${currentMessage.type}")
                    Text(text = "Time: ${currentMessage.time}")

                    Spacer(modifier = Modifier.padding(12.dp))

                    Button(
                        onClick = {
                            viewModel.deleteMessage(currentMessage)
                            onBack()
                        }
                    ) {
                        Text("Remove")
                    }
                }
            } ?: Text("Message not found")
        }
    }
}