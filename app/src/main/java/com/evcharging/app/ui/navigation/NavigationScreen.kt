package com.evcharging.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import com.evcharging.app.ui.components.VoiceAssistantButton
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.theme.*
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val singapore = LatLng(1.3521, 103.8198)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 11f)
    }

    val stations by viewModel.stations.collectAsState()
    val route by viewModel.route.collectAsState()
    val diningAreas by viewModel.diningAreas.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    // var searchQuery removed as it's now local to the search column

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val cameraUpdate by viewModel.cameraUpdate.collectAsState()

    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }
    
    // Fetch user location if permission granted
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.setCurrentLocation(LatLng(location.latitude, location.longitude))
                    }
                }
            } catch (e: SecurityException) {
                // Handle exception
            }
        }
    }

    LaunchedEffect(cameraUpdate) {
        cameraUpdate?.let { latLng ->
            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 12f),
                durationMs = 1000
            )
            viewModel.onCameraMoved()
        }
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<com.evcharging.app.data.model.Station?>(null) }

    Scaffold(
        floatingActionButton = {
            VoiceAssistantButton { command ->
                // Handle voice command
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionState.status.isGranted,
                    isTrafficEnabled = true
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    mapToolbarEnabled = true,
                    zoomControlsEnabled = true,
                    compassEnabled = true
                )
            ) {
                val isNavigationActive by viewModel.isNavigationActive.collectAsState()
                
                // Only show stations and dining areas when NOT navigating
                if (!isNavigationActive) {
                    stations.forEach { station ->
                        val markerColor = if (station.maintenanceStatus == "Maintenance") BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_CYAN
                        val snippetText = if (station.maintenanceStatus == "Maintenance") "Under Maintenance" else if (station.isAvailable) "Available" else "Occupied"
                        
                        Marker(
                            state = MarkerState(position = LatLng(station.latitude, station.longitude)),
                            title = station.name,
                            snippet = snippetText,
                            icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                            onClick = {
                                selectedStation = station
                                showBottomSheet = true
                                false
                            }
                        )
                    }
                    
                    diningAreas.forEach { place ->
                        Marker(
                            state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                            title = place.name,
                            snippet = "Dining Area",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        )
                    }
                }

                // Route Visualization
                val route by viewModel.route.collectAsState()
                if (route != null) {
                    // Polyline removed as per user request ("remove the blue line")
                    
                    // Destination Marker with Distance/Time
                    if (route!!.points.isNotEmpty()) {
                        val destination = route!!.points.last()
                        Marker(
                            state = MarkerState(position = destination),
                            title = "Destination",
                            snippet = "${route!!.distance} • ${route!!.duration}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                        // Note: To automatically show info window, we'd need a separate MarkerState we can control,
                        // but default behavior often shows it on click. For "on the direction mark", this snippet is key.
                    }
                }
            }

            val isNavigationActive by viewModel.isNavigationActive.collectAsState()
            val directionSteps by viewModel.directionSteps.collectAsState()

            // Search Bar Overlay (Hide when navigating)
            if (!isNavigationActive) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // ... existing Search Bar Content ...
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = DeepBackground.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Start Location Input
                            var startLocation by remember { mutableStateOf("") }
                            var destination by remember { mutableStateOf("") }
                            var activeField by remember { mutableStateOf<String?>(null) } // "start" or "dest"

                            androidx.compose.material3.TextField(
                                value = startLocation,
                                onValueChange = { 
                                    startLocation = it
                                    activeField = "start"
                                    viewModel.search(it)
                                },
                                placeholder = { Text("Start Location (Current)", color = TextSecondary) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Place, 
                                        contentDescription = "Start", 
                                        tint = NeonCyan
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = NeonCyan,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )
                            
                            Divider(color = GlassWhite, thickness = 0.5.dp)

                            // Destination Input
                            androidx.compose.material3.TextField(
                                value = destination,
                                onValueChange = { 
                                    destination = it
                                    activeField = "dest"
                                    viewModel.search(it)
                                },
                                placeholder = { Text("Destination Point", color = TextSecondary) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.LocationOn, 
                                        contentDescription = "Destination", 
                                        tint = NeonRed
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = NeonRed,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )

                             // Predictions List
                            if (searchResults.isNotEmpty() && activeField != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                                ) {
                                    items(searchResults.size) { index ->
                                        val result = searchResults[index]
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(result.primaryText, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                                    Text(result.secondaryText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                }
                                            },
                                            onClick = {
                                                if (activeField == "start") {
                                                    startLocation = result.primaryText
                                                } else {
                                                    destination = result.primaryText
                                                    viewModel.onSearchResultSelected(result.placeId, isDestination = true)
                                                }
                                                activeField = null
                                            }
                                        )
                                        if (index < searchResults.size - 1) {
                                            Divider(color = GlassWhite)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Navigation Active Mode - Top Direction Card
                if (directionSteps.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        GlassCard {
                            val selectedMode by viewModel.selectedMode.collectAsState()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = directionSteps[0].icon,
                                    contentDescription = "Turn",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(directionSteps[0].distance, style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                                    Text(directionSteps[0].instruction, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("via ${if (selectedMode == TransportMode.CAR) "Car" else "Bike"}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
            
            
            // Route Info Card (Show existing card or Navigation Mode card)
            // Route Info Card (Show existing card or Navigation Mode card)
            if (route != null) {
                val selectedMode by viewModel.selectedMode.collectAsState()
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp) // Above BottomBar
                        .padding(horizontal = 16.dp)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            // Mode Selector Tabs (Only show before navigation starts)
                            if (!isNavigationActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ModeTab(
                                        text = "Car",
                                        icon = androidx.compose.material.icons.Icons.Default.DirectionsCar,
                                        isSelected = selectedMode == TransportMode.CAR,
                                        onClick = { viewModel.setTransportMode(TransportMode.CAR) },
                                        color = NeonCyan
                                    )
                                    ModeTab(
                                        text = "Bike",
                                        icon = androidx.compose.material.icons.Icons.Default.PedalBike,
                                        isSelected = selectedMode == TransportMode.BIKE,
                                        onClick = { viewModel.setTransportMode(TransportMode.BIKE) },
                                        color = NeonPurple
                                    )
                                }
                                Divider(color = GlassWhite.copy(alpha = 0.3f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (isNavigationActive) {
                                         Text("On Route (${if (selectedMode == TransportMode.CAR) "Car" else "Bike"})", style = MaterialTheme.typography.labelLarge, color = NeonGreen)
                                    } else {
                                         Text("Estimated Trip", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        Icon(androidx.compose.material.icons.Icons.Default.Place, "Dist", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(route!!.distance, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Icon(androidx.compose.material.icons.Icons.Default.LocationOn, "Time", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(route!!.duration, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                if (isNavigationActive) {
                                     NeonButton(
                                        text = "End",
                                        onClick = { viewModel.endNavigation() },
                                        modifier = Modifier.height(48.dp),
                                        color = NeonRed
                                    )
                                } else {
                                    NeonButton(
                                        text = "Start",
                                        onClick = { viewModel.startNavigation() },
                                        modifier = Modifier.height(48.dp),
                                        color = NeonGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }


            
            if (!locationPermissionState.status.isGranted) {
                Text(
                    text = "Please enable location permission to see your current location",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    color = Color.White
                )
            }
            
            if (showBottomSheet && selectedStation != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = DeepBackground,
                    contentColor = TextPrimary
                ) {
                    StationDetailContent(station = selectedStation!!)
                }
            }
        }
    }
}

@Composable
fun StationDetailContent(station: com.evcharging.app.data.model.Station) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        // Header
        Text(text = station.name, style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
        Text(text = station.address, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = if (station.maintenanceStatus == "Maintenance") NeonRed else NeonGreen
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = statusColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (station.maintenanceStatus == "Maintenance") "Under Maintenance" else "Operational",
                style = MaterialTheme.typography.titleMedium,
                color = statusColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Promotions
        if (station.promotions.isNotEmpty()) {
            Text(text = "Special Offers", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
            Spacer(modifier = Modifier.height(8.dp))
            station.promotions.forEach { promo ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(text = promo["title"] as? String ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = promo["description"] as? String ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = "${promo["discountPercentage"]}% OFF", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Rewards System
        Text(text = "Loyalty Rewards", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
        Text(text = "Earn ${station.pointsPerKw} points per kW", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (station.rewards.isNotEmpty()) {
            station.rewards.forEach { reward ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = reward["title"] as? String ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "${reward["pointsCost"]} Points", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
                        }
                        NeonButton(
                            text = "Redeem",
                            onClick = { /* TODO: Implement Redeem Logic */ },
                            modifier = Modifier.height(36.dp),
                            color = NeonGreen
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dining
        if (station.dining.isNotEmpty()) {
            Text(text = "Dining Options", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
            Spacer(modifier = Modifier.height(8.dp))
            station.dining.forEach { item ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item["name"] as? String ?: "", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(text = item["description"] as? String ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Text(text = "$${item["price"]}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ModeTab(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    val tabColor = if (isSelected) color else GlassWhite.copy(alpha = 0.3f)
    val contentColor = if (isSelected) DeepBackground else TextSecondary

    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(tabColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
