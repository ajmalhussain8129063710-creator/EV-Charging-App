package com.evcharging.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.app.ui.components.VoiceAssistantButton

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val carModel by viewModel.carModel.collectAsState()
    val carColorName by viewModel.carColor.collectAsState()
    
    val carColor = when (carColorName?.lowercase()) {
        "white" -> Color.White
        "black" -> Color.Black
        "silver" -> Color(0xFFC0C0C0)
        "gray" -> Color.Gray
        "red" -> Color.Red
        "blue" -> Color.Blue
        "brown" -> Color(0xFFA52A2A)
        "green" -> Color.Green
        "yellow" -> Color.Yellow
        "orange" -> Color(0xFFFFA500)
        else -> Color.White // Default to white if unknown
    }

    Scaffold(
        floatingActionButton = {
            VoiceAssistantButton { command ->
                handleVoiceCommand(command, navController)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome Back!", 
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your Vehicle: ${carModel ?: "Loading..."}",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // 3D Rotation Logic
            var carRotationY by remember { mutableStateOf(0f) }
            val density = androidx.compose.ui.platform.LocalDensity.current.density

            androidx.compose.animation.AnimatedVisibility(
                visible = carModel != null,
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -1000 }) + androidx.compose.animation.fadeIn()
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                carRotationY += dragAmount * 0.5f // Sensitivity
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.evcharging.app.util.CarResourceHelper.getCarImageResource(carModel)),
                        contentDescription = "User Car 3D View",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .graphicsLayer {
                                rotationY = carRotationY
                                cameraDistance = 12f * density
                            },
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(carColor, androidx.compose.ui.graphics.BlendMode.Modulate)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Swipe to rotate", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

private fun handleVoiceCommand(command: String, navController: NavController) {
    val lowerCommand = command.lowercase()
    when {
        lowerCommand.contains("plan trip to") -> {
            val destination = command.substringAfter("plan trip to").trim()
            // In a real app, we'd pass the destination as an argument
            navController.navigate("tripplanner") 
        }
        lowerCommand.contains("trip planner") -> {
            navController.navigate("tripplanner")
        }
        lowerCommand.contains("map") || lowerCommand.contains("navigation") -> {
            navController.navigate("navigation")
        }
        lowerCommand.contains("home") -> {
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }
}
