package com.example.final_project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.final_project.ui.screens.DetailScreen
import com.example.final_project.ui.screens.HistoryScreen
import com.example.final_project.ui.screens.MainScreen
import com.example.final_project.viewmodel.MainViewModel

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    onTakePhotoRequest: () -> Unit,
    onBluetoothServerRequest: () -> Unit,
    onBluetoothConnectRequest: () -> Unit
) {
    val navController = rememberNavController()
    val history by mainViewModel.history.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {

        composable("main") {
            MainScreen(
                navController = navController,
                localAmbientLight = mainViewModel.localAmbientLight,
                localProximity = mainViewModel.localProximity,
                localPhotoBitmap = mainViewModel.localPhotoBitmap,
                remoteDeviceName = mainViewModel.remoteDeviceName,
                remoteDeviceUuid = mainViewModel.remoteDeviceUuid,
                remoteAmbientLight = mainViewModel.remoteAmbientLight,
                remoteProximity = mainViewModel.remoteProximity,
                remotePhotoBitmap = mainViewModel.remotePhotoBitmap,
                localDeviceName = mainViewModel.localDeviceName,
                localDeviceUuid = mainViewModel.localDeviceUuid,
                serverStatusText = mainViewModel.serverStatusText,
                showQrSection = mainViewModel.showQrSection,
                bluetoothStatusText = mainViewModel.bluetoothStatusText,
                onStartServerClick = {
                    mainViewModel.startServer()
                    onBluetoothServerRequest()
                },
                onBluetoothConnectClick = {
                    onBluetoothConnectRequest()
                },
                onTakePhotoClick = {
                    onTakePhotoRequest()
                },
                onClearHistoryClick = {
                    mainViewModel.clearHistory()
                }
            )
        }

        composable("history") {
            HistoryScreen(
                navController = navController,
                readings = history
            )
        }

        composable(
            route = "detail/{readingId}",
            arguments = listOf(
                navArgument("readingId") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            val readingId = backStackEntry.arguments?.getInt("readingId") ?: -1
            val selectedReading = history.find { it.id == readingId }

            DetailScreen(
                navController = navController,
                reading = selectedReading
            )
        }
    }
}