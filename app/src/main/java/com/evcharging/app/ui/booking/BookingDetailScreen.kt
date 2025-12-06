package com.evcharging.app.ui.booking

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.components.SectionHeader
import com.evcharging.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingDetailScreen(
    navController: NavController,
    stationName: String,
    stationAddress: String = "123 EV Street, Green City",
    pricePerKwh: String = "$0.25/kWh",
    stationId: String = "",
    viewModel: BookingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf("Card") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    
    val walletBalance by viewModel.walletBalance.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()
    val diningList by viewModel.diningList.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val basePrice = pricePerKwh.replace("$", "").replace("/kWh", "").toDoubleOrNull() ?: 0.25
    val estimatedKwh = 20.0
    val subtotal = basePrice * estimatedKwh
    val discount = if (paymentMethod == "Wallet") subtotal * 0.10 else 0.0
    val total = subtotal - discount

    LaunchedEffect(stationId) {
        viewModel.fetchDining(stationId)
    }

    LaunchedEffect(bookingState) {
        when (bookingState) {
            is BookingState.Success -> {
                snackbarHostState.showSnackbar("Booking Confirmed! Ready to charge.")
            }
            is BookingState.Charging -> {
                val bookingId = (bookingState as BookingState.Charging).bookingId
                navController.navigate("charging/$bookingId")
            }
            is BookingState.Completed -> {
                snackbarHostState.showSnackbar("Charging Completed!")
                delay(1000)
                navController.popBackStack()
            }
            is BookingState.Error -> {
                snackbarHostState.showSnackbar((bookingState as BookingState.Error).message)
            }
            is BookingState.Cancelled -> {
                snackbarHostState.showSnackbar("Booking Cancelled. Refund initiated.")
            }
            else -> {}
        }
    }

    // Modern Date Picker Dialog (Auto-select)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        
        // Auto-confirm when date is selected (only if it changes)
        LaunchedEffect(datePickerState.selectedDateMillis) {
            if (datePickerState.selectedDateMillis != null && datePickerState.selectedDateMillis != selectedDate) {
                // Add a small delay for visual feedback
                delay(300)
                selectedDate = datePickerState.selectedDateMillis
                showDatePicker = false
            }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {}, // Hidden as requested
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = NeonCyan) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = CardBackground,
                titleContentColor = NeonCyan,
                headlineContentColor = TextPrimary,
                weekdayContentColor = TextSecondary,
                subheadContentColor = TextSecondary,
                yearContentColor = TextPrimary,
                currentYearContentColor = NeonCyan,
                selectedYearContentColor = TextPrimary,
                selectedYearContainerColor = NeonCyan,
                dayContentColor = TextPrimary,
                disabledDayContentColor = TextSecondary,
                selectedDayContentColor = DeepBackground,
                selectedDayContainerColor = NeonCyan,
                todayContentColor = NeonCyan,
                todayDateBorderColor = NeonCyan
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking?", color = TextPrimary) },
            text = { Text("Are you sure you want to cancel? The amount will be refunded to your original payment method.", color = TextSecondary) },
            confirmButton = {
                NeonButton(
                    text = "Yes, Cancel",
                    onClick = {
                        viewModel.cancelBooking()
                        showCancelDialog = false
                    },
                    color = NeonRed
                )
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep it", color = NeonCyan)
                }
            },
            containerColor = CardBackground
        )
    }

    // Success Animation Dialog
    if (bookingState is BookingState.Success) {
        Dialog(onDismissRequest = {}) {
            GlassCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Charging Icon
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .scale(scale),
                            tint = NeonGreen
                        )
                        // Outer ripple effect (simulated with border)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(scale)
                                .border(2.dp, NeonGreen.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Payment Successful!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Redirecting to Home...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
        
        // Auto-redirect after delay
        LaunchedEffect(Unit) {
            delay(3000) // Show animation for 3 seconds
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBackground,
                    scrolledContainerColor = DeepBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1525), DeepBackground),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                 // Station Details Card
                 GlassCard {
                    Column(horizontalAlignment = Alignment.Start) {
                         Text(text = stationName, style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                         Spacer(modifier = Modifier.height(8.dp))
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                             Spacer(modifier = Modifier.width(8.dp))
                             Text(text = stationAddress, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                         }
                         Spacer(modifier = Modifier.height(8.dp))
                         Text(text = "$pricePerKwh", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                    }
                 }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Date & Time Selection
                SectionHeader("Schedule Booking")
                GlassCard {
                     Column {
                         // Date Selection
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clickable { showDatePicker = true }
                                 .padding(vertical = 8.dp),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text("Select Date", color = TextPrimary)
                             Text(
                                 text = if (selectedDate != null) java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate!!)) else "Choose Date",
                                 color = if (selectedDate != null) NeonCyan else TextSecondary,
                                 fontWeight = FontWeight.Bold
                             )
                         }
                         
                         Divider(color = GlassWhite)
                         
                         // Time Slots
                         Text("Select Time Slot", color = TextPrimary, modifier = Modifier.padding(vertical = 8.dp))
                         LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             val slots = listOf("09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM", "03:00 PM")
                             items(slots) { slot ->
                                 val isSelected = selectedSlot == slot
                                 Box(
                                     modifier = Modifier
                                         .clip(RoundedCornerShape(8.dp))
                                         .background(if (isSelected) NeonCyan else GlassSurface)
                                         .border(1.dp, if (isSelected) NeonCyan else GlassWhite, RoundedCornerShape(8.dp))
                                         .clickable { selectedSlot = slot }
                                         .padding(horizontal = 16.dp, vertical = 8.dp)
                                 ) {
                                     Text(
                                         text = slot, 
                                         color = if (isSelected) DeepBackground else TextPrimary,
                                         fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                     )
                                 }
                             }
                         }
                     }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Dining Options
                SectionHeader("Pre-order Dining")
                if (diningList.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(diningList) { item ->
                            GlassCard(modifier = Modifier.width(160.dp)) {
                                Column {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("$${item.price}", color = NeonGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                     Text("No dining options available.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Payment Method
                SectionHeader("Payment Method")
                GlassCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { paymentMethod = "Wallet" }) {
                            RadioButton(selected = paymentMethod == "Wallet", onClick = { paymentMethod = "Wallet" }, colors = RadioButtonDefaults.colors(selectedColor = NeonCyan, unselectedColor = TextSecondary))
                            Text("Wallet (Balance: $$walletBalance)", color = TextPrimary)
                             if (paymentMethod == "Wallet") {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("10% OFF", color = NeonGreen, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { paymentMethod = "Card" }) {
                            RadioButton(selected = paymentMethod == "Card", onClick = { paymentMethod = "Card" }, colors = RadioButtonDefaults.colors(selectedColor = NeonCyan, unselectedColor = TextSecondary))
                            Text("Credit/Debit Card", color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { paymentMethod = "Cash" }) {
                            RadioButton(selected = paymentMethod == "Cash", onClick = { paymentMethod = "Cash" }, colors = RadioButtonDefaults.colors(selectedColor = NeonCyan, unselectedColor = TextSecondary))
                            Text("Pay at Station", color = TextPrimary)
                        }
                    }
                }

                 Spacer(modifier = Modifier.height(24.dp))

                // Price Breakdown
                GlassCard {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Text("Est. Charge (20kWh)", color = TextSecondary)
                             Text("$${String.format("%.2f", subtotal)}", color = TextPrimary)
                        }
                        if (discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                 Text("Wallet Discount", color = NeonGreen)
                                 Text("-$${String.format("%.2f", discount)}", color = NeonGreen)
                            }
                        }
                        Divider(color = GlassWhite, modifier = Modifier.padding(vertical = 8.dp))
                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Text("Total to Pay", fontWeight = FontWeight.Bold, color = TextPrimary)
                             Text("$${String.format("%.2f", total)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                if (bookingState !is BookingState.Success && bookingState !is BookingState.Cancelled) {
                     NeonButton(
                        text = if (bookingState is BookingState.Loading) "Processing..." else "Confirm & Pay",
                        onClick = {
                            if (selectedDate == null) {
                                scope.launch { snackbarHostState.showSnackbar("Please select a date") }
                            } else if (selectedSlot == null) {
                                scope.launch { snackbarHostState.showSnackbar("Please select a time slot") }
                            } else {
                                viewModel.processBooking(
                                    stationId = stationId,
                                    stationName = stationName,
                                    amount = total,
                                    paymentMethod = paymentMethod,
                                    date = selectedDate!!
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonCyan
                    )
                } else if (bookingState is BookingState.Cancelled) {
                     NeonButton(
                        text = "Booking Cancelled",
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
