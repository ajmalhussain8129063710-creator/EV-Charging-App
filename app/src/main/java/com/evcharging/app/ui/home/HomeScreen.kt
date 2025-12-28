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
import androidx.compose.material.icons.filled.Search
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
import com.evcharging.app.ui.components.Car3DViewer
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.components.VoiceAssistantButton
import com.evcharging.app.ui.theme.GlassSurface
import com.evcharging.app.ui.theme.GlassWhite
import com.evcharging.app.ui.theme.NeonCyan
import com.evcharging.app.ui.theme.NeonGreen
import com.evcharging.app.ui.theme.NeonRed
    
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val carModel by viewModel.carModel.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    // AI Theme Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant

    var showMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                .background(MaterialTheme.colorScheme.background)
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Driver", // Could be user name
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Profile", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("profile")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("settings")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Help & Support", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("support")
                                }
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = { Text("Charging History", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    showHistoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Loyalty Points", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    showPointsDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Vehicle Search & 3D Visualization
                var searchQuery by remember { mutableStateOf("") }
                val car3dUrl by viewModel.car3dModelUrl.collectAsState()
                val selectedColor by viewModel.selectedCarColor.collectAsState()

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        // Auto-search or wait for submit? doing auto for demo
                        if (it.length > 2) viewModel.searchAndSelectCar(it)
                    },
                    placeholder = { Text("Search EV Model (e.g. Tesla)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Box(
                    modifier = Modifier
                        .size(300.dp) // Car View Area
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Car3DViewer(
                        modelUrl = car3dUrl,
                        carColor = selectedColor,
                        onColorChange = viewModel::updateCarColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Text(
                    text = "Touch car to customize & view details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

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
                                        Text(booking["stationName"] as String, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                        Text(dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(status, color = if(status == "Charging") NeonGreen else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("$${booking["amount"]}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.titleMedium)
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
                            title = { Text("Booking Management", color = MaterialTheme.colorScheme.onSurface) },
                            text = { 
                                Column {
                                    Text("Station: ${(upcomingBookings.find { it["id"] == bookingToCancel })?.get("stationName")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Would you like to cancel this booking? A refund will be initiated.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
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
            title = { Text("Charging History", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                if (history.isEmpty()) {
                    Text("No recent charging history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(history.size) { index ->
                            val transaction = history[index]
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text("Station ID: ${transaction.stationId}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text("Amount: $${transaction.amount}", color = MaterialTheme.colorScheme.primary)
                                Text("Date: ${transaction.timestamp.toDate()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Divider(color = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showPointsDialog) {
        val points by viewModel.userPoints.collectAsState()
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Loyalty Points", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("$points Points", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Earn points with every charge!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPointsDialog = false }) {
                    Text("Awesome!", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
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
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
       color = MaterialTheme.colorScheme.primary // Use theme primary
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
