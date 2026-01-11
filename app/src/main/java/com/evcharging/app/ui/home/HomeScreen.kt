package com.evcharging.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    voiceViewModel: com.evcharging.app.ui.components.VoiceAssistantViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    val upcomingBookings by viewModel.upcomingBookings.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // UI Colors from Screenshot
    val GreenAccent = Color(0xFF00C853)
    val LightGreenBg = Color(0xFFE8F5E9)
    val TextDark = Color(0xFF1E1E1E)
    val TextGrey = Color(0xFF757575)
    val SurfaceOffWhite = Color(0xFFF9F9F9)

    var showMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SurfaceOffWhite,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("admin_signup") },
                containerColor = GreenAccent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Host Station", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Header: Menu - Driver - Notification
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextDark, modifier = Modifier.size(28.dp))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Profile", color = TextDark) },
                                    onClick = { showMenu = false; navController.navigate("profile") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", color = TextDark) },
                                    onClick = { showMenu = false; navController.navigate("settings") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Help & Support", color = TextDark) },
                                    onClick = { showMenu = false; navController.navigate("support") }
                                )
                                Divider(color = Color.LightGray)
                                DropdownMenuItem(
                                    text = { Text("Charging History", color = TextDark) },
                                    onClick = { showMenu = false; showHistoryDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Loyalty Points", color = TextDark) },
                                    onClick = { showMenu = false; showPointsDialog = true }
                                )
                            }
                        }
                        
                        Text(
                            text = "Driver",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        
                        Box {
                            IconButton(onClick = { showNotificationDialog = true }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextDark, modifier = Modifier.size(28.dp))
                            }
                            // Notification Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(10.dp)
                                    .background(Color.Red, CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                // 2. Search Bar
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(2.dp, CircleShape),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            // Using BasicTextField or Text for placeholder look since standard TextField has padding issues in small height
                            Text(
                                text = if(searchQuery.isEmpty()) "Search EV Model or Station..." else searchQuery,
                                color = if(searchQuery.isEmpty()) TextGrey else TextDark,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // 3. Filter Chips (Nearby, Available, Fast Charger)
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            FilterChipItem(text = "Nearby", icon = Icons.Outlined.LocationOn, selected = true, color = TextDark)
                        }
                        item {
                            FilterChipItem(text = "Available", selected = false, color = GreenAccent) // Text color green suggested by "Available" context
                        }
                        item {
                            val isFastFilterEnabled by viewModel.isFastChargerFilterEnabled.collectAsState()
                            FilterChipItem(
                                text = "Fast Charger", 
                                selected = isFastFilterEnabled, 
                                color = TextDark,
                                onClick = { viewModel.toggleFastChargerFilter() }
                            )
                        }
                    }
                }

                // 4. Scan & Start Charging Hero Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = GreenAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scan & Start Charging",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap to scan QR on charger\nor connect automatically",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGrey,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { navController.navigate("scan_qr") },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                    shape = RoundedCornerShape(50), // Fully rounded
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Text("Scan Now", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            
                            // Placeholder for Illustration
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(LightGreenBg, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "QR", tint = GreenAccent, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                // 5. Active Sessions Header
                item {
                    Text(
                        text = "Active Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                // 6. Active Sessions List
                if (upcomingBookings.isNotEmpty()) {
                    items(upcomingBookings.size) { index ->
                        val booking = upcomingBookings[index]
                        val status = booking["status"] as? String ?: "Confirmed"
                        val isLive = status == "Charging"
                        
                        SessionCard(
                            booking = booking,
                            isLive = isLive,
                            onAction = { 
                                if (isLive) navController.navigate("charging/${booking["id"]}")
                                else viewModel.startCharging(booking["id"] as String)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                     item {
                        Text("No active sessions", color = TextGrey)
                     }
                }
                
                // Extra padding for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // --- Dialogs ---

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Mock Admin Update
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = GreenAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Station Update", fontWeight = FontWeight.Bold)
                            Text("Fast Charger at AJU Station is now online and available for booking!", style = MaterialTheme.typography.bodySmall)
                            Text("2 mins ago", color = TextGrey, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Close", color = GreenAccent)
                }
            },
            containerColor = Color.White,
            titleContentColor = TextDark,
            textContentColor = TextDark
        )
    }

    if (showHistoryDialog) {
        val history by viewModel.chargingHistory.collectAsState()
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Charging History", fontWeight = FontWeight.Bold) },
            text = {
                if (history.isEmpty()) {
                    Text("No recent charging history.", color = TextGrey)
                } else {
                    LazyColumn {
                        items(history.size) { index ->
                            val transaction = history[index]
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text("Station ID: ${transaction.stationId}", color = TextDark, fontWeight = FontWeight.Bold)
                                Text("Amount: $${transaction.amount}", color = GreenAccent)
                                Text("Date: ${transaction.timestamp.toDate()}", color = TextGrey, style = MaterialTheme.typography.bodySmall)
                                Divider(color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = GreenAccent)
                }
            },
            containerColor = Color.White,
            titleContentColor = TextDark,
            textContentColor = TextDark
        )
    }

    if (showPointsDialog) {
        val points by viewModel.userPoints.collectAsState()
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Loyalty Points", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material.icons.Icons.Default.Star?.let {
                        Icon(it, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("$points Points", style = MaterialTheme.typography.displayMedium, color = TextDark, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Earn points with every charge!", color = TextGrey)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPointsDialog = false }) {
                    Text("Awesome!", color = GreenAccent)
                }
            },
            containerColor = Color.White,
            titleContentColor = TextDark,
            textContentColor = TextDark
        )
    }
}

@Composable
fun FilterChipItem(text: String, icon: ImageVector? = null, selected: Boolean, color: Color, onClick: () -> Unit = {}) {
    Surface(
        color = if (selected) Color(0xFFE0E0E0) else Color.White, // Grey selected background or white
        shape = RoundedCornerShape(50),
        border = if(!selected) BorderStroke(1.dp, Color(0xFFEEEEEE)) else null,
        modifier = Modifier.height(36.dp).clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if(text == "Available") Color(0xFF00C853) else Color(0xFF424242),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SessionCard(booking: Map<String, Any>, isLive: Boolean, onAction: () -> Unit) {
    val GreenAccent = Color(0xFF00C853)
    val TextDark = Color(0xFF1E1E1E)
    val TextGrey = Color(0xFF757575)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            Surface(color = GreenAccent, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(end = 8.dp)) {
                                Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = booking["stationName"] as? String ?: "EV Station",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Connector: CCS2", color = TextGrey, style = MaterialTheme.typography.bodySmall)
                    Text("Power: 22 kW", color = TextGrey, style = MaterialTheme.typography.bodySmall)
                }
                
                if (isLive) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", color = GreenAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("65%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                } else {
                     Text(
                        text = "Apr 16, 2024", // Mock Date
                        color = TextGrey, 
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = 0.65f, // Mock progress
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = GreenAccent,
                trackColor = Color(0xFFF1F8E9)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹ ${booking["amount"]}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                if (isLive) {
                    Text("18 min left", color = TextGrey, style = MaterialTheme.typography.bodyMedium)
                } else {
                     // Booking confirmed, waiting to start
                     Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                     ) {
                         Text("Start Charging", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                     }
                }
            }
            
            if (isLive) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View Session", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
