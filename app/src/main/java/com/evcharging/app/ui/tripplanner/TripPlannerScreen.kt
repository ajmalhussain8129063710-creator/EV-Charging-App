package com.evcharging.app.ui.tripplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.evcharging.app.ui.components.PaymentDialog
import com.evcharging.app.ui.components.VoiceAssistantButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

@Composable
fun TripPlannerScreen(
    navController: androidx.navigation.NavController,
    viewModel: TripPlannerViewModel = hiltViewModel()
) {
    var startLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    val tripResult by viewModel.tripResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<ChargingStation?>(null) }
    var showVoiceAlert by remember { mutableStateOf(false) }
    var voiceAlertMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (showPaymentDialog && selectedStation != null) {
            PaymentDialog(
                stationName = selectedStation!!.name,
                amount = "$15.00",
                onDismiss = { showPaymentDialog = false },
                onConfirm = { paymentMethod ->
                    viewModel.bookStation(selectedStation!!.name, paymentMethod)
                    showPaymentDialog = false
                }
            )
        }

        // Voice Alert Simulation Popup
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

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Trip Planner", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                
                // Upcoming Bookings Section
                val upcomingBookings by viewModel.upcomingBookings.collectAsState()
                var bookingToCancel by remember { mutableStateOf<String?>(null) } // Booking ID

                if (upcomingBookings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Upcoming Bookings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    upcomingBookings.forEach { booking ->
                        com.evcharging.app.ui.components.GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                                bookingToCancel = booking["id"] as String
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(booking["stationName"] as String, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("Payment: ${booking["paymentMethod"]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Confirmed", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
                                    Text("$${booking["amount"]}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    
                    if (bookingToCancel != null) {
                        AlertDialog(
                            onDismissRequest = { bookingToCancel = null },
                            title = { Text("Cancel Booking?") },
                            text = { Text("Are you sure you want to cancel this booking? A refund will be processed.") },
                            confirmButton = {
                                Button(
                                    onClick = { 
                                        viewModel.cancelBooking(bookingToCancel!!)
                                        bookingToCancel = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Cancel Booking")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { bookingToCancel = null }) {
                                    Text("Keep")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                val suggestions by viewModel.locationSuggestions.collectAsState()
                var activeField by remember { mutableStateOf<String?>(null) }

                // Start Location Field
                Column {
                    OutlinedTextField(
                        value = startLocation,
                        onValueChange = { 
                            startLocation = it
                            activeField = "start"
                            viewModel.searchLocation(it)
                        },
                        label = { Text("Start Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (activeField == "start" && suggestions.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            items(suggestions.size) { index ->
                                val prediction = suggestions[index]
                                DropdownMenuItem(
                                    text = { Text(prediction.primaryText, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = { 
                                        startLocation = prediction.primaryText
                                        viewModel.clearSuggestions()
                                        activeField = null
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Destination Field
                Column {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { 
                            destination = it
                            activeField = "dest"
                            viewModel.searchLocation(it)
                        },
                        label = { Text("Destination") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                         keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = {
                                viewModel.planTrip(startLocation, destination)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (activeField == "dest" && suggestions.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            items(suggestions.size) { index ->
                                val prediction = suggestions[index]
                                DropdownMenuItem(
                                    text = { Text(prediction.primaryText, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = { 
                                        destination = prediction.primaryText
                                        viewModel.clearSuggestions()
                                        activeField = null
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.planTrip(startLocation, destination)
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Plan Trip")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (tripResult != null) {
                    Button(
                        onClick = {
                            scope.launch {
                                val nextStation = tripResult?.chargingStops?.firstOrNull()?.name ?: "Unknown Station"
                                voiceAlertMessage = "Voice Assistant: Next charging station is $nextStation in 15km. Battery at ${tripResult?.batteryUsage}."
                                showVoiceAlert = true
                                delay(4000)
                                showVoiceAlert = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Trip (Voice Alert)")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                tripResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Trip Details", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Distance: ${result.distance}")
                            Text("Est. Battery Usage: ${result.batteryUsage}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Suggested Charging Stops:", style = MaterialTheme.typography.titleMedium)
                            
                            result.chargingStops.forEach { station ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("• ${station.name} (${station.distance})")
                                        if (station.isBooked) {
                                            Text("Booked", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    if (station.isAvailable && !station.isBooked) {
                                        Button(
                                            onClick = {
                                                // Navigate to Booking Detail Screen
                                                navController.navigate("booking_detail/${station.name}")
                                            },
                                            modifier = Modifier.height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Book", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            if (result.steps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Route Details:", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                result.steps.forEachIndexed { index, step ->
                                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
    }
}

data class TripResult(
    val distance: String,
    val batteryUsage: String,
    val chargingStops: List<ChargingStation>,
    val steps: List<String> = emptyList()
)

data class ChargingStation(
    val name: String,
    val distance: String,
    val isAvailable: Boolean,
    val isBooked: Boolean = false
)
