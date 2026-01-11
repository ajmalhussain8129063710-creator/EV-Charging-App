package com.evcharging.app.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanQRScreen(
    navController: NavController
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var isScanning by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    // Mock Scanning Logic
    LaunchedEffect(isScanning) {
        if (isScanning && cameraPermissionState.status.isGranted) {
            delay(3000) // Simulate scanning time
            isScanning = false
            scanResult = "STATION_001" // Mock Result
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (cameraPermissionState.status.isGranted) {
                if (scanResult == null) {
                    // Scanning UI
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(200.dp),
                            color = Color(0xFF00C853),
                            strokeWidth = 2.dp
                        )
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scanning",
                            modifier = Modifier.size(100.dp),
                            tint = Color(0xFF00C853)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Align QR code within frame",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                } else {
                    // Success UI
                    Text(
                        "Station Found!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Station ID: $scanResult", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("booking_detail?stationName=Mock Station&stationId=$scanResult") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("Start Charging", color = Color.White)
                    }
                }
            } else {
                Text("Camera permission is required to scan QR codes.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
