package com.evcharging.app.ui.charging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.theme.DeepBackground
import com.evcharging.app.ui.theme.NeonCyan
import com.evcharging.app.ui.theme.NeonGreen
import com.evcharging.app.ui.theme.TextPrimary
import com.evcharging.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

import androidx.hilt.navigation.compose.hiltViewModel
import com.evcharging.app.ui.booking.BookingViewModel

@Composable
fun ChargingScreen(
    navController: NavController,
    bookingId: String,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var progress by remember { mutableStateOf(0f) }
    var kwhCharged by remember { mutableStateOf(0.0) }
    val targetKwh = 20.0
    
    // Simulate charging
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(1000)
            progress += 0.05f
            kwhCharged = targetKwh * progress
        }
    }

    Scaffold(
        containerColor = DeepBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Charging in Progress", style = MaterialTheme.typography.headlineMedium, color = NeonGreen)
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animation
            val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://lottie.host/5a6a4f2c-5c3a-4b0a-9b0a-5c3a4b0a9b0a/placeholder.json")) // Reuse placeholder or specific charging one
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(250.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
    // Stats
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.displayLarge, color = NeonCyan)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Energy", color = TextSecondary)
                    Text(String.format("%.1f kWh", kwhCharged), color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Time Left", color = TextSecondary)
                    // Calculate remaining seconds based on 20 mins (1200 seconds) total
                    val totalSeconds = 1200
                    val remainingSeconds = (totalSeconds * (1f - progress)).toInt()
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    Text(String.format("%02d:%02d", minutes, seconds), color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (progress >= 1f) {
                NeonButton(
                    text = "Finish Charging",
                    onClick = { 
                        viewModel.completeBooking(bookingId)
                        navController.navigate("home") {
                             popUpTo("home") { inclusive = true }
                        }
                    },
                    color = NeonGreen,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                NeonButton(
                    text = "Stop Charging",
                    onClick = { navController.popBackStack() },
                    color = com.evcharging.app.ui.theme.NeonRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
