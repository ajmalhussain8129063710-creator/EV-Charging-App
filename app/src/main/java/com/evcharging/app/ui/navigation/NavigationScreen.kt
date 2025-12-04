package com.evcharging.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.evcharging.app.data.StationRepository
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
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
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
    val diningAreas by viewModel.diningAreas.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val cameraUpdate by viewModel.cameraUpdate.collectAsState()

    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }

    LaunchedEffect(cameraUpdate) {
        cameraUpdate?.let { latLng ->
            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 15f),
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
                // Handle voice command (e.g., zoom to station)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = locationPermissionState.status.isGranted),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true)
            ) {
                stations.forEach { station ->
                    val markerColor = if (station.maintenanceStatus == "Maintenance") BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_BLUE
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

            // Search Bar Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                androidx.compose.material3.TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.search(it)
                    },
                    placeholder = { Text("Search places...") },
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    singleLine = true
                )
                
                if (searchResults.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        items(searchResults.size) { index ->
                            val result = searchResults[index]
                            androidx.compose.material3.DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(result.primaryText, style = MaterialTheme.typography.bodyLarge)
                                        Text(result.secondaryText, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    viewModel.onSearchResultSelected(result.placeId)
                                    searchQuery = ""
                                }
                            )
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (showBottomSheet && selectedStation != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
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
        Text(text = station.name, style = MaterialTheme.typography.headlineSmall)
        Text(text = station.address, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = if (station.maintenanceStatus == "Maintenance") Color.Red else Color.Green
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
            Text(text = "Special Offers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            station.promotions.forEach { promo ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = promo["title"] as? String ?: "", style = MaterialTheme.typography.titleSmall, androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(text = promo["description"] as? String ?: "", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${promo["discountPercentage"]}% OFF", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Rewards System
        Text(text = "Loyalty Rewards", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Text(text = "Earn ${station.pointsPerKw} points per kW", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (station.rewards.isNotEmpty()) {
            station.rewards.forEach { reward ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = reward["title"] as? String ?: "", style = MaterialTheme.typography.titleSmall, androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(text = "${reward["pointsCost"]} Points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Button(
                            onClick = { /* TODO: Implement Redeem Logic */ },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Redeem", style = MaterialTheme.typography.labelSmall)
                        }
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
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item["name"] as? String ?: "", style = MaterialTheme.typography.titleSmall)
                            Text(text = item["description"] as? String ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(text = "$${item["price"]}", style = MaterialTheme.typography.titleMedium, androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
