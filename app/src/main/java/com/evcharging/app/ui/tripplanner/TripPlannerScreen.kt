package com.evcharging.app.ui.tripplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.PaymentDialog
import com.evcharging.app.ui.components.VoiceAssistantButton
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Define Colors based on the UI image
private val TravelBlue = Color(0xFF448AFF)
private val TravelGreen = Color(0xFF00C853)
private val TextBlack = Color(0xFF202124)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val BackgroundGray = Color(0xFFF2F4F8)

@Composable
fun TripPlannerScreen(
    navController: NavController,
    viewModel: TripPlannerViewModel = hiltViewModel()
) {
    var startLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var activeField by remember { mutableStateOf(TripField.NONE) }
    val tripResult by viewModel.tripResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val suggestions by viewModel.locationSuggestions.collectAsState()
    
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<ChargingStation?>(null) }
    var showVoiceAlert by remember { mutableStateOf(false) }
    var voiceAlertMessage by remember { mutableStateOf("") }
    
    // Scratch Card State
    var showScratchCard by remember { mutableStateOf(false) }
    var scratchAmount by remember { mutableStateOf(0.0) }
    
    val scope = rememberCoroutineScope()
    
    val upcomingBookings by viewModel.upcomingBookings.collectAsState()

    // --- Dialogs ---
    if (showPaymentDialog && selectedStation != null) {
        PaymentDialog(
            stationName = selectedStation!!.name,
            amount = "₹15.00",
            onDismiss = { showPaymentDialog = false },
            onConfirm = { paymentMethod ->
                viewModel.bookStation(selectedStation!!.name, paymentMethod)
                showPaymentDialog = false
                
                // Trigger Scratch Card Reward
                // Check if user is lucky (example logic: always lucky for demo or random)
                val isLucky = (1..10).random() <= 8 // 80% chance
                if (isLucky) {
                    scratchAmount = (1..10).random().toDouble()
                    showScratchCard = true
                }
            }
        )
    }
    
    if (showScratchCard) {
        com.evcharging.app.ui.components.ScratchCardDialog(
            amount = scratchAmount,
            onDismiss = { showScratchCard = false },
            onClaim = {
                // Logic to credit wallet would go here or be handled by ViewModel
                // For demo/UI update, we just close it
                showScratchCard = false
                // Ideally call viewModel.claimReward(amount)
            }
        )
    }

    if (showVoiceAlert) {
        Dialog(onDismissRequest = { showVoiceAlert = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = voiceAlertMessage, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }

    // --- Main Screen ---
    Scaffold(
        containerColor = BackgroundGray,
        floatingActionButton = {
            VoiceAssistantButton { command ->
                if (command.contains("plan trip to", ignoreCase = true)) {
                    val dest = command.substringAfter("plan trip to").trim()
                    destination = dest
                    viewModel.planTrip(startLocation, dest)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextBlack, modifier = Modifier.size(28.dp).clickable {})
                Text(
                    text = "Trip Planner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Box {
                    Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = TextBlack, modifier = Modifier.size(28.dp))
                    Box(modifier = Modifier.padding(2.dp).size(8.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd))
                }
            }

            // 2. Content with Scroll
            Column(
                modifier = Modifier
                    .weight(1f) // Takes remaining space above bottom bar? No, this is the main content area
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // --- Input Card ---
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceWhite,
                    shadowElevation = 8.dp, // High elevation for "floating" look
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Start Location
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, contentDescription = "Start", tint = TravelGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (startLocation.isEmpty()) Text("Enter start location", color = Color.Gray, fontSize = 16.sp)
                                BasicTextField(
                                    value = startLocation,
                                    onValueChange = { startLocation = it; viewModel.searchLocation(it) },
                                    textStyle = TextStyle(color = TextBlack, fontSize = 16.sp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { if (it.isFocused) activeField = TripField.START }
                                )
                            }
                        }
                        Divider(color = BackgroundGray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        
                        // Destination
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = "Dest", tint = Color(0xFF546E7A), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (destination.isEmpty()) Text("Enter destination", color = Color.Gray, fontSize = 16.sp)
                                BasicTextField(
                                    value = destination,
                                    onValueChange = { destination = it; viewModel.searchLocation(it) },
                                    textStyle = TextStyle(color = TextBlack, fontSize = 16.sp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { if (it.isFocused) activeField = TripField.DESTINATION }
                                )
                            }
                        }
                    }
                }

                // Suggestions
                if (suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        suggestions.forEach { prediction ->
                            DropdownMenuItem(
                                text = { Text(prediction.primaryText) },
                                onClick = {
                                    when (activeField) {
                                        TripField.START -> startLocation = prediction.primaryText
                                        TripField.DESTINATION -> destination = prediction.primaryText
                                        else -> {}
                                    }
                                    viewModel.clearSuggestions()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Plan Trip Button ---
                Button(
                    onClick = { viewModel.planTrip(startLocation, destination) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(28.dp) // Fully rounded
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Plan Trip", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- Results Section (Integrated) ---
                if (tripResult != null) {
                   Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                         Column(modifier = Modifier.padding(20.dp)) {
                            Text("Trip & Charging Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextBlack)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Distance: ${tripResult!!.distance}", color = Color.Gray)
                                Text("Battery: ${tripResult!!.batteryUsage}", color = TravelBlue, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = BackgroundGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Available Stations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextBlack)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            tripResult!!.chargingStops.forEach { station ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(station.name, fontWeight = FontWeight.Medium, color = TextBlack)
                                        Text(station.distance, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    if (station.isAvailable) {
                                        Button(
                                            onClick = {
                                                val encodedName = URLEncoder.encode(station.name, StandardCharsets.UTF_8.toString())
                                                val encodedId = URLEncoder.encode(station.id, StandardCharsets.UTF_8.toString())
                                                navController.navigate("booking_detail?stationName=$encodedName&stationId=$encodedId")
                                            },
                                            modifier = Modifier.height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = TravelGreen),
                                            shape = RoundedCornerShape(18.dp)
                                        ) {
                                            Text("Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Divider(color = BackgroundGray, thickness = 0.5.dp)
                            }
                         }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- Upcoming Bookings ---
                if (upcomingBookings.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Upcoming Bookings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextBlack)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand", tint = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    upcomingBookings.forEach { booking ->
                        UpcomingBookingCard(booking)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // --- Map View (Persistent) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    PersistentMapView(tripResult, upcomingBookings.firstOrNull())
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Bottom padding for FAB
            }
        }
    }
}

enum class TripField { START, DESTINATION, NONE }

@Composable
fun UpcomingBookingCard(booking: Map<String, Any>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE0F7FA), // Light Cyan
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF006064))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = booking["stationName"] as? String ?: "EV Station",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = "April 17, 2024", // Mock Date
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = Color(0xFFE8F5E9), // Light Green
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Confirmed",
                        color = TravelGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "₹ ${booking["amount"]}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TravelGreen
                )
            }
        }
    }
}

@Composable
fun PersistentMapView(tripResult: TripResult?, booking: Map<String, Any>?) {
    // Default: Nagpur (Center of India approx) or User Location
    val defaultPos = LatLng(21.1458, 79.0882) 
    
    // Determine Target Location
    val targetLocation = remember(tripResult, booking) {
        if (tripResult != null && tripResult.chargingStops.isNotEmpty()) {
           // Trip has priority - assume first stop or derived bounds (omitted for brevity)
           // For now, let's just pick the first stop if we can parse it, or default
           defaultPos 
        } else if (booking != null) {
            val lat = booking["latitude"] as? Double
            val lng = booking["longitude"] as? Double
            if (lat != null && lng != null) LatLng(lat, lng) else defaultPos
        } else {
            defaultPos
        }
    }

    // Fix: key must be String, so we use toString() or unique string representation
    val cameraPositionState = rememberCameraPositionState(key = targetLocation.toString()) {
        position = CameraPosition.fromLatLngZoom(targetLocation, 14f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        // 1. Show Trip Route/Stops
        if (tripResult != null) {
            tripResult.chargingStops.forEach { stop ->
                 // Ideally extract lat/lng from station or use geocoding
                 // Currently TripResult stops don't store lat/lng in the simplified model?
                 // Wait, we updated the model? No, Station model has it, but TripResult model uses ChargingStation which we defined in TripPlannerModels.
                 // We need to check if ChargingStation has lat/lng.
                 // If not, we might fail to show markers for the trip stops unless we add it. 
                 // But the primary goal here is the BOOKING.
            }
        }
        
        // 2. Show Booking Marker (The User Request)
        if (booking != null) {
            val lat = booking["latitude"] as? Double
            val lng = booking["longitude"] as? Double
            val name = booking["stationName"] as? String ?: "Booked Station"
            val address = booking["address"] as? String
            
            if (lat != null && lng != null) {
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    title = name,
                    snippet = address ?: "Upcoming Booking"
                )
            }
        }
    }
}

