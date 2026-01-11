package com.evcharging.app.ui.booking

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext

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
    pricePerKwh: String = "₹0.25/kWh",
    stationId: String = "",
    viewModel: BookingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var selectedHours by remember { mutableStateOf(1) }
    var selectedMinutes by remember { mutableStateOf(0) }
    var paymentMethod by remember { mutableStateOf("Card") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    
    var showLowBalanceDialog by remember { mutableStateOf(false) }

    val walletBalance by viewModel.walletBalance.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()
    val diningList by viewModel.diningList.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pricing: ₹200 per 60 Mins (approx ₹3.33/min)
    val totalMinutes = (selectedHours * 60) + selectedMinutes
    val pricePerMinute = 200.0 / 60.0
    val subtotal = totalMinutes * pricePerMinute
    val discount = 0.0 
    val total = subtotal - discount

    if (showLowBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showLowBalanceDialog = false },
            title = { Text("Insufficient Balance", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("You need ₹${String.format("%.2f", total)} but have ₹${String.format("%.2f", walletBalance)}. Please add money to your wallet.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                NeonButton(
                    text = "Go to Wallet",
                    onClick = {
                        showLowBalanceDialog = false
                        navController.navigate("wallet")
                    },
                    color = MaterialTheme.colorScheme.primary
                )
            },
            dismissButton = {
                TextButton(onClick = { showLowBalanceDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Allow dates from today onwards
                    val today = Calendar.getInstance().apply {
                         set(Calendar.HOUR_OF_DAY, 0)
                         set(Calendar.MINUTE, 0)
                         set(Calendar.SECOND, 0)
                         set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    return utcTimeMillis >= today
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year >= Calendar.getInstance().get(Calendar.YEAR)
                }
            }
        )
        
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
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.primary) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                headlineContentColor = MaterialTheme.colorScheme.onBackground,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                yearContentColor = MaterialTheme.colorScheme.onBackground,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = MaterialTheme.colorScheme.onBackground,
                selectedYearContainerColor = MaterialTheme.colorScheme.primaryContainer,
                dayContentColor = MaterialTheme.colorScheme.onBackground,
                disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking?", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("Are you sure you want to cancel? The amount will be refunded to your original payment method.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                NeonButton(
                    text = "Yes, Cancel",
                    onClick = {
                        viewModel.cancelBooking()
                        showCancelDialog = false
                    },
                    color = MaterialTheme.colorScheme.error
                )
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep it", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
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
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        // Outer ripple effect (simulated with border)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(scale)
                                .border(2.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Payment Successful!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Redirecting to Home...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                title = { Text("Booking Details", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background),
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
                         Text(text = stationName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                         Spacer(modifier = Modifier.height(8.dp))
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                             Spacer(modifier = Modifier.width(8.dp))
                             Text(text = stationAddress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         }
                         Spacer(modifier = Modifier.height(8.dp))
                         Text(text = "$pricePerKwh", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
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
                             Text("Select Date", color = MaterialTheme.colorScheme.onBackground)
                             Text(
                                 text = if (selectedDate != null) java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate!!)) else "Choose Date",
                                 color = if (selectedDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                 fontWeight = FontWeight.Bold
                             )
                         }
                         
                         Divider(color = MaterialTheme.colorScheme.outlineVariant)
                         
                         // Time Slots Logic
                         val allSlots = listOf("09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM", "09:00 PM")
                         val availableSlots = remember(selectedDate) {
                             if (selectedDate == null) {
                                 allSlots
                             } else {
                                 val now = Calendar.getInstance()
                                 val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate!! }
                                 
                                 val isToday = now.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                                               now.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
                                 
                                 if (isToday) {
                                     allSlots.filter { slot ->
                                         try {
                                             val slotFormat = java.text.SimpleDateFormat("hh:00 a", Locale.getDefault())
                                             val slotDate = slotFormat.parse(slot)
                                             val slotCal = Calendar.getInstance().apply {
                                                 time = slotDate!!
                                                 set(Calendar.YEAR, now.get(Calendar.YEAR))
                                                 set(Calendar.MONTH, now.get(Calendar.MONTH))
                                                 set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                                             }
                                             slotCal.after(now)
                                         } catch (e: Exception) {
                                             true
                                         }
                                     }
                                 } else {
                                     allSlots
                                 }
                             }
                         }

                         Text("Select Time Slot", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(vertical = 8.dp))
                         
                         if (selectedDate != null && availableSlots.isEmpty()) {
                             Text("No slots available for this date.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                         } else {
                             LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                 items(availableSlots) { slot ->
                                     val isSelected = selectedSlot == slot
                                     Box(
                                         modifier = Modifier
                                             .clip(RoundedCornerShape(8.dp))
                                             .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                             .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                             .clickable { selectedSlot = slot }
                                             .padding(horizontal = 16.dp, vertical = 8.dp)
                                     ) {
                                         Text(
                                             text = slot, 
                                             color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                             fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                         )
                                     }
                                 }
                             }
                         }
                     }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Duration Selection
                SectionHeader("Select Duration")
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Hours Row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hours", color = MaterialTheme.colorScheme.onBackground)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (selectedHours > 0) selectedHours-- }, // Allow 0 hours if minutes > 0
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                
                                Text(
                                    text = "$selectedHours Hr",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                IconButton(
                                    onClick = { if (selectedHours < 12) selectedHours++ },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        // Minutes Row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Minutes", color = MaterialTheme.colorScheme.onBackground)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (selectedMinutes > 0) selectedMinutes -= 15 },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                
                                Text(
                                    text = "$selectedMinutes Min",
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                IconButton(
                                    onClick = { if (selectedMinutes < 45) selectedMinutes += 15 },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onSurface)
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
                                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("₹${item.price}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                     Text("No dining options available.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Payment Method Info
                SectionHeader("Payment")
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp), 
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pay via Wallet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Balance: ₹${String.format("%.2f", walletBalance)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (walletBalance < total) {
                             Text("Low Balance", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                     }
                }

                 Spacer(modifier = Modifier.height(24.dp))

                // Price Breakdown
                GlassCard {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Text("Charge ($selectedHours h $selectedMinutes m)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                             Text("₹${String.format("%.2f", subtotal)}", color = MaterialTheme.colorScheme.onBackground)
                        }
                        if (discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                 Text("Wallet Discount", color = MaterialTheme.colorScheme.tertiary)
                                 Text("-₹${String.format("%.2f", discount)}", color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Text("Total to Pay", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                             Text("₹${String.format("%.2f", total)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
                                if (walletBalance >= total) {
                                    viewModel.processBooking(
                                        stationId = stationId,
                                        stationName = stationName,
                                        amount = total,
                                        paymentMethod = "Wallet",
                                        date = selectedDate!!
                                    )
                                } else {
                                    showLowBalanceDialog = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
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
