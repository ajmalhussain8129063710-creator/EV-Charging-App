package com.evcharging.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evcharging.app.data.model.Station
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.components.VoiceAssistantButton
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val indiaCenter = LatLng(21.1458, 79.0882) // Nagpur, center of India
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(indiaCenter, 5f)
    }

    val stations by viewModel.stations.collectAsState()
    val route by viewModel.route.collectAsState()
    val diningAreas by viewModel.diningAreas.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraUpdate by viewModel.cameraUpdate.collectAsState()
    val isNavigationActive by viewModel.isNavigationActive.collectAsState()
    val directionSteps by viewModel.directionSteps.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<Station?>(null) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var isTrafficEnabled by remember { mutableStateOf(false) }

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
                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                durationMs = 1000
            )
            viewModel.onCameraMoved()
        }
    }

    Scaffold(
        floatingActionButton = {
            VoiceAssistantButton { command ->
                viewModel.search(command)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            // --- MAP LAYER ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionState.status.isGranted,
                    isTrafficEnabled = isTrafficEnabled,
                    mapType = mapType
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true
                )
            ) {
                // Only show stations and dining areas when NOT navigating
                if (!isNavigationActive) {
                    // Dining Areas
                    diningAreas.forEach { place ->
                        Marker(
                            state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                            title = place.name,
                            snippet = "Dining Area",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        )
                    }

                    // Stations
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
                }

                // Route Visualization
                if (route != null && route!!.points.isNotEmpty()) {
                    Polyline(
                        points = route!!.points,
                        color = Color(0xFF00E5FF), // Neon Cyan Highlighting
                        width = 16f,
                        geodesic = true
                    )

                    val startPoint = route!!.points.first()
                    Marker(
                        state = MarkerState(position = startPoint),
                        title = "Start",
                        snippet = "Your Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )

                    val destination = route!!.points.last()
                    Marker(
                        state = MarkerState(position = destination),
                        title = "Destination",
                        snippet = "${route!!.distance} â€¢ ${route!!.duration}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            // --- UI OVERLAYS ---

            // 1. Map Control Buttons (Side Bar)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 16.dp)
            ) {
                // Map Type Toggle
                GlassCard(
                    modifier = Modifier
                        .size(50.dp)
                        .clickable {
                            mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL
                        },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (mapType == MapType.NORMAL) Icons.Filled.Satellite else Icons.Filled.Map,
                            contentDescription = "Map Type",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Traffic Toggle
                GlassCard(
                    modifier = Modifier
                        .size(50.dp)
                        .clickable { isTrafficEnabled = !isTrafficEnabled },
                    shape = CircleShape,
                    containerColor = if (isTrafficEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Traffic,
                            contentDescription = "Traffic",
                            tint = if (isTrafficEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // My Location Button
                GlassCard(
                    modifier = Modifier
                        .size(50.dp)
                        .clickable { viewModel.recenterCamera() },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "My Location",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // 2. Search Bar / Navigation Info
            if (!isNavigationActive) {
                // Search Bar Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Start Location Input
                            var startLocation by remember { mutableStateOf("") }
                            var destination by remember { mutableStateOf("") }
                            var activeField by remember { mutableStateOf<String?>(null) } // "start" or "dest"

                            TextField(
                                value = startLocation,
                                onValueChange = {
                                    startLocation = it
                                    activeField = "start"
                                    viewModel.search(it)
                                },
                                placeholder = { Text("Start Location (Current)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Place,
                                        contentDescription = "Start",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                singleLine = true
                            )

                            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                            // Destination Input
                            TextField(
                                value = destination,
                                onValueChange = {
                                    destination = it
                                    activeField = "dest"
                                    viewModel.search(it)
                                },
                                placeholder = { Text("Destination Point", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Destination",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.error,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                singleLine = true
                            )

                            if (searchResults.isNotEmpty() && activeField != null) {
                                // List of predictions
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .padding(top = 8.dp)
                                ) {
                                    items(searchResults.size) { index ->
                                        val result = searchResults[index]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (activeField == "start") {
                                                        startLocation = result.primaryText
                                                        viewModel.onSearchResultSelected(result.placeId, isDestination = false)
                                                    } else {
                                                        destination = result.primaryText
                                                        viewModel.onSearchResultSelected(result.placeId, isDestination = true)
                                                    }
                                                    activeField = null
                                                }
                                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(result.primaryText, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                                                Text(result.secondaryText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filter Chips Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        text = "Fast Charger",
                                        isSelected = filterType == "Fast",
                                        onClick = { viewModel.setFilterType(if (filterType == "Fast") null else "Fast") },
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                item {
                                    FilterChip(
                                        text = "Available",
                                        isSelected = filterStatus == "Available",
                                        onClick = { viewModel.setFilterStatus(if (filterStatus == "Available") null else "Available") },
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                item {
                                    FilterChip(
                                        text = "Under Maintenance",
                                        isSelected = filterStatus == "Operational",
                                        onClick = { viewModel.setFilterStatus(if (filterStatus == "Operational") null else "Operational") },
                                        color = MaterialTheme.colorScheme.secondary
                                    )
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = directionSteps[0].icon,
                                    contentDescription = "Turn",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(directionSteps[0].distance, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(directionSteps[0].instruction, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                    Text("via ${if (selectedMode == TransportMode.CAR) "Car" else "Bike"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Route Info Card (Bottom)
            if (route != null) {
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
                                        icon = Icons.Filled.DirectionsCar,
                                        isSelected = selectedMode == TransportMode.CAR,
                                        onClick = { viewModel.setTransportMode(TransportMode.CAR) },
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    ModeTab(
                                        text = "Bike",
                                        icon = Icons.Filled.PedalBike,
                                        isSelected = selectedMode == TransportMode.BIKE,
                                        onClick = { viewModel.setTransportMode(TransportMode.BIKE) },
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (isNavigationActive) {
                                        Text("On Route (${if (selectedMode == TransportMode.CAR) "Car" else "Bike"})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                                    } else {
                                        Text("Estimated Trip", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        Icon(Icons.Filled.Place, "Dist", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(route!!.distance, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Icon(Icons.Filled.LocationOn, "Time", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(route!!.duration, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isNavigationActive) {
                                    NeonButton(
                                        text = "End",
                                        onClick = { viewModel.endNavigation() },
                                        modifier = Modifier.height(48.dp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    NeonButton(
                                        text = "Start",
                                        onClick = { viewModel.startNavigation() },
                                        modifier = Modifier.height(48.dp),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Missing Permission Warning
            if (!locationPermissionState.status.isGranted) {
                Text(
                    text = "Please enable location permission to see your current location",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    color = Color.White
                )
            }

            // 5. Station Detail Sheet
            if (showBottomSheet && selectedStation != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    StationDetailContent(station = selectedStation!!)
                }
            }
        }
    }
}


@Composable
fun StationDetailContent(station: Station) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(text = station.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = station.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = if (station.maintenanceStatus == "Maintenance") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
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
            Text(text = "Special Offers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            station.promotions.forEach { promo ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(text = promo["title"] as? String ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(text = promo["description"] as? String ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${promo["discountPercentage"]}% OFF", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Rewards System
        Text(text = "Loyalty Rewards", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Text(text = "Earn ${station.pointsPerKw} points per kW", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        if (station.rewards.isNotEmpty()) {
            station.rewards.forEach { reward ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = reward["title"] as? String ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text(text = "${reward["pointsCost"]} Points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        }
                        NeonButton(
                            text = "Redeem",
                            onClick = { /* TODO: Implement Redeem Logic */ },
                            modifier = Modifier.height(36.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dining
        if (station.dining.isNotEmpty()) {
            Text(text = "Dining Options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            station.dining.forEach { item ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item["name"] as? String ?: "", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                            Text(text = item["description"] as? String ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = "$${item["price"]}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    val tabColor = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tabColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    val backgroundColor = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent
    val borderColor = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (isSelected) color else MaterialTheme.colorScheme.onBackground)
    }
}
