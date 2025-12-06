package com.evcharging.app.ui.home

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.components.VoiceAssistantButton
import com.evcharging.app.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    isDarkTheme: Boolean = true,
    onThemeChange: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val carModel by viewModel.carModel.collectAsState()
// ... existing code ...
    
    
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    // AI Theme Colors
    val neonBlue = NeonCyan
    val darkBackground = DeepBackground
    val cardBackground = CardBackground // Use this for dialogs, Glass for UI

    var showMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = darkBackground,
        floatingActionButton = {
            VoiceAssistantButton { command ->
                handleVoiceCommand(command, navController)
            }
        }
    ) { innerPadding ->
        // Main Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1525), DeepBackground),
                        startY = 0f,
                        endY = 1500f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Good Morning,",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Text(
                            text = "Driver", // Could be user name
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Profile Icon Placeholder with Menu
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(GlassSurface, CircleShape)
                            .border(1.dp, GlassWhite, CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(CardBackground)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Dark Mode", color = Color.White, modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = isDarkTheme,
                                            onCheckedChange = { onThemeChange() },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = NeonCyan,
                                                checkedTrackColor = DeepBackground,
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color.White
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    // Switch handles click
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Profile", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("profile")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    android.widget.Toast.makeText(navController.context, "Settings Clicked", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Help & Support", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("support")
                                }
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = { Text("Charging History", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    showHistoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Loyalty Points", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    showPointsDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // AI Core Visualization
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Glow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(neonBlue.copy(alpha = 0.2f), Color.Transparent),
                                    radius = 250f
                                )
                            )
                    )
                    
                    // Lottie Animation (AI Orb)
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.Url("https://lottie.host/5a6a4f2c-5c3a-4b0a-9b0a-5c3a4b0a9b0a/placeholder.json")
                    )
                    
                    if (composition == null) {
                         Box(
                            modifier = Modifier
                                .size(100.dp)
                                .border(2.dp, neonBlue, CircleShape)
                                .background(neonBlue.copy(alpha = 0.1f), CircleShape)
                        )
                    } else {
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Text(
                    text = "AI System Online",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Vehicle: ${carModel ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Active Bookings Section
                val upcomingBookings by viewModel.upcomingBookings.collectAsState()
                var bookingToCancel by remember { mutableStateOf<String?>(null) } // Booking ID

                if (upcomingBookings.isNotEmpty()) {
                    Text(
                        text = "Active Booking",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    upcomingBookings.forEach { booking ->
                        val status = booking["status"] as? String ?: "Confirmed"
                        val bookingDate = booking["bookingDate"] as? Long ?: 0L
                        val dateString = if (bookingDate > 0) java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(bookingDate)) else "Scheduled"

                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                                // View Details or Manage
                                if (status == "Charging") {
                                    navController.navigate("charging/${booking["id"]}")
                                } else {
                                    bookingToCancel = booking["id"] as String // Open manage dialog
                                }
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(booking["stationName"] as String, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(dateString, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(status, color = if(status == "Charging") NeonGreen else NeonCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("$${booking["amount"]}", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (status == "Confirmed") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { 
                                                viewModel.startCharging(booking["id"] as String)
                                                navController.navigate("charging/${booking["id"]}")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Start Charge")
                                        }
                                        OutlinedButton(
                                            onClick = { bookingToCancel = booking["id"] as String },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                                            border = BorderStroke(1.dp, NeonRed),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                } else if (status == "Charging") {
                                     Button(
                                        onClick = { navController.navigate("charging/${booking["id"]}") },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Text("View Charging Session")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (bookingToCancel != null) {
                        AlertDialog(
                            onDismissRequest = { bookingToCancel = null },
                            title = { Text("Booking Management", color = TextPrimary) },
                            text = { 
                                Column {
                                    Text("Station: ${(upcomingBookings.find { it["id"] == bookingToCancel })?.get("stationName")}", color = TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Would you like to cancel this booking? A refund will be initiated.", color = TextSecondary)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { 
                                        viewModel.cancelBooking(bookingToCancel!!)
                                        bookingToCancel = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                                ) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { bookingToCancel = null }) {
                                    Text("No", color = NeonCyan)
                                }
                            },
                            containerColor = DeepBackground,
                            titleContentColor = TextPrimary,
                            textContentColor = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Status Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusCard(
                        title = "Battery",
                        value = "85%",
                        icon = Icons.Default.BatteryChargingFull,
                        color = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatusCard(
                        title = "Range",
                        value = "320 km",
                        icon = Icons.Default.Speed,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionButton(
                        text = "Navigate",
                        icon = Icons.Default.Navigation,
                        onClick = { navController.navigate("navigation") },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "Plan Trip",
                        icon = Icons.Default.Map,
                        onClick = { navController.navigate("tripplanner") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showHistoryDialog) {
        val history by viewModel.chargingHistory.collectAsState()
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Charging History", color = Color.White) },
            text = {
                if (history.isEmpty()) {
                    Text("No recent charging history.", color = TextSecondary)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(history.size) { index ->
                            val transaction = history[index]
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text("Station ID: ${transaction.stationId}", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Amount: $${transaction.amount}", color = neonBlue)
                                Text("Date: ${transaction.timestamp.toDate()}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Divider(color = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = neonBlue)
                }
            },
            containerColor = cardBackground
        )
    }

    if (showPointsDialog) {
        val points by viewModel.userPoints.collectAsState()
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Loyalty Points", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("$points Points", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Earn points with every charge!", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPointsDialog = false }) {
                    Text("Awesome!", color = neonBlue)
                }
            },
            containerColor = cardBackground
        )
    }
}

@Composable
fun StatusCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeonButton(
       text = text,
       onClick = onClick,
       modifier = modifier,
       color = NeonCyan // Or passing color if needed
    )
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
