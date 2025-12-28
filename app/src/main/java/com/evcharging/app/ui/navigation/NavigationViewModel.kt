package com.evcharging.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.PlacesRepository
import com.evcharging.app.data.PlacePrediction
import com.evcharging.app.data.StationRepository
import com.evcharging.app.data.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PedalBike

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val repository: StationRepository,
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    private val _diningAreas = MutableStateFlow<List<Place>>(emptyList())
    val diningAreas: StateFlow<List<Place>> = _diningAreas

    private val _searchResults = MutableStateFlow<List<PlacePrediction>>(emptyList())
    val searchResults: StateFlow<List<PlacePrediction>> = _searchResults

    private val _route = MutableStateFlow<RouteInfo?>(null)
    val route: StateFlow<RouteInfo?> = _route

    private val _cameraUpdate = MutableStateFlow<com.google.android.gms.maps.model.LatLng?>(null)
    val cameraUpdate: StateFlow<com.google.android.gms.maps.model.LatLng?> = _cameraUpdate

    init {
        fetchStations()
        fetchDiningAreas()
    }

    private val _filterType = MutableStateFlow<String?>(null) // "DC Fast", "AC Type 2", etc.
    val filterType: StateFlow<String?> = _filterType

    private val _filterStatus = MutableStateFlow<String?>(null) // "Available", "Operational"
    val filterStatus: StateFlow<String?> = _filterStatus

    fun setFilterType(type: String?) {
        _filterType.value = type
        applyFilters()
    }

    fun setFilterStatus(status: String?) {
        _filterStatus.value = status
        applyFilters()
    }

    private var allStations = listOf<Station>()

    private fun applyFilters() {
        var filtered = allStations
        
        _filterType.value?.let { type ->
            filtered = filtered.filter { it.chargerType.contains(type, ignoreCase = true) }
        }
        
        _filterStatus.value?.let { status ->
             if (status == "Available") {
                 filtered = filtered.filter { it.isAvailable }
             } else if (status == "Operational") {
                 filtered = filtered.filter { it.maintenanceStatus == "Operational" }
             }
        }
        
        _stations.value = filtered
    }

    private fun fetchStations() {
        viewModelScope.launch(Dispatchers.IO) {
            // Fetch mock stations first
            val result = repository.getStations()
            val currentStations = result.getOrDefault(emptyList()).toMutableList()
            
            // Try to fetch real stations from Google (India center)
            try {
                val realStations = placesRepository.searchPlaces("EV Charging Station India", com.google.android.gms.maps.model.LatLng(21.1458, 79.0882))
                
                val deferredDetails = realStations.take(5).map { prediction ->
                    async {
                        val details = placesRepository.getPlaceDetails(prediction.placeId)
                        if (details != null && details.latLng != null) {
                            Station(
                                id = details.id ?: "google_${prediction.placeId}",
                                name = details.name ?: prediction.primaryText,
                                latitude = details.latLng!!.latitude,
                                longitude = details.latLng!!.longitude,
                                isAvailable = true, // Assume available for now
                                address = details.address ?: prediction.secondaryText
                            )
                        } else null
                    }
                }

                val newStations = deferredDetails.awaitAll().filterNotNull()
                currentStations.addAll(newStations)
            } catch (e: Exception) {
                e.printStackTrace()
            }
             
            allStations = currentStations
            applyFilters() // Initial apply
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                _searchResults.value = placesRepository.searchPlaces(query)
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    fun onCameraMoved() {
        _cameraUpdate.value = null
    }

    private var startLocation: com.google.android.gms.maps.model.LatLng? = com.google.android.gms.maps.model.LatLng(21.1458, 79.0882) // Default to India
    private var destinationLocation: com.google.android.gms.maps.model.LatLng? = null

    fun onSearchResultSelected(placeId: String, isDestination: Boolean = false) {
        viewModelScope.launch {
            val place = placesRepository.getPlaceDetails(placeId)
            if (place != null && place.latLng != null) {
                if (isDestination) {
                    destinationLocation = place.latLng
                    if (startLocation != null) {
                        calculateRoute(startLocation!!, destinationLocation!!)
                    }
                     _cameraUpdate.value = place.latLng
                } else {
                    // Start Location selected
                    startLocation = place.latLng
                    val currentDining = _diningAreas.value.toMutableList()
                    currentDining.add(Place(place.name ?: "Selected Location", place.latLng!!.latitude, place.latLng!!.longitude, "Search"))
                    _diningAreas.value = currentDining
                     _cameraUpdate.value = place.latLng
                     
                     // If we already have a destination, recalculate the route from new start
                     if (destinationLocation != null) {
                         calculateRoute(startLocation!!, destinationLocation!!)
                     }
                }
            }
            _searchResults.value = emptyList()
        }
    }

    fun calculateRoute(start: com.google.android.gms.maps.model.LatLng, end: com.google.android.gms.maps.model.LatLng) {
        viewModelScope.launch {
             // Mock Route Calculation
            val distanceKm = calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude)
            
            // Speed assumption: Car ~ 50km/h, Bike ~ 20km/h
            val speedKmH = if (_selectedMode.value == TransportMode.CAR) 50 else 20
            val durationMins = ((distanceKm / speedKmH) * 60).toInt()
            
            // Calculate route geometry
            val points = simulateRoutePath(start, end)
            
            _route.value = RouteInfo(
                distance = "${String.format("%.1f", distanceKm)} km",
                duration = "$durationMins mins",
                points = points
            )
            
            // Focus on Midpoint
            val midLat = (start.latitude + end.latitude) / 2
            val midLng = (start.longitude + end.longitude) / 2
             _cameraUpdate.value = com.google.android.gms.maps.model.LatLng(midLat, midLng)
        }
    }

    fun setCurrentLocation(latLng: com.google.android.gms.maps.model.LatLng) {
        // Only update if we don't have a manual start location set (or initial default)
        if (startLocation == null || startLocation?.latitude == 21.1458) {
            startLocation = latLng
            _cameraUpdate.value = latLng // Initial center
        }
    }

    private fun simulateRoutePath(start: com.google.android.gms.maps.model.LatLng, end: com.google.android.gms.maps.model.LatLng): List<com.google.android.gms.maps.model.LatLng> {
         // Create a more complex "jagged" path to simulate city streets
         val points = mutableListOf<com.google.android.gms.maps.model.LatLng>()
         points.add(start)
         
         val latDiff = end.latitude - start.latitude
         val lngDiff = end.longitude - start.longitude
         
         // Add 5 intermediate points with some "noise" to look like turns
         for (i in 1..5) {
             val fraction = i / 6.0
             val baseLat = start.latitude + latDiff * fraction
             val baseLng = start.longitude + lngDiff * fraction
             
             // Add "turn" offset (alternating)
             val offset = if (i % 2 == 0) 0.002 else -0.002
             
             points.add(com.google.android.gms.maps.model.LatLng(baseLat + offset, baseLng + offset))
             points.add(com.google.android.gms.maps.model.LatLng(baseLat, baseLng)) // Return to "main road" line
         }

         points.add(end)
         return points
    }
    
    private val _isNavigationActive = MutableStateFlow(false)
    val isNavigationActive: StateFlow<Boolean> = _isNavigationActive

    private val _directionSteps = MutableStateFlow<List<DirectionStep>>(emptyList())
    val directionSteps: StateFlow<List<DirectionStep>> = _directionSteps

    // ... existing vars ...

    // ... existing calculateRoute ...

    fun startNavigation() {
        if (_route.value != null) {
            _isNavigationActive.value = true
            generateMockDirections()
        }
    }

    fun recenterCamera() {
        startLocation?.let {
            _cameraUpdate.value = it
        }
    }

    fun endNavigation() {
        _isNavigationActive.value = false
        _directionSteps.value = emptyList()
    }

    private fun generateMockDirections() {
        // Mock steps based on route
        val modeStr = if (_selectedMode.value == TransportMode.CAR) "Drive" else "Ride"
        
        _directionSteps.value = listOf(
            DirectionStep("$modeStr north", "200 m", androidx.compose.material.icons.Icons.Default.ArrowUpward),
            DirectionStep("Turn right onto Orchard Rd", "500 m", androidx.compose.material.icons.Icons.Default.TurnRight),
            DirectionStep("Keep left to continue on CTE", "2.5 km", androidx.compose.material.icons.Icons.Default.TurnLeft),
            DirectionStep("Take exit 8B", "300 m", androidx.compose.material.icons.Icons.Default.CallMade),
            DirectionStep("Arrive at Destination", "0 m", androidx.compose.material.icons.Icons.Default.Place)
        )
    }

    // ... existing clearRoute ...

    // ... existing functions ...
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of the earth
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private val _selectedMode = MutableStateFlow(TransportMode.CAR)
    val selectedMode: StateFlow<TransportMode> = _selectedMode

    fun setTransportMode(mode: TransportMode) {
        _selectedMode.value = mode
        // Recalculate route info if route exists
        if (_route.value != null) {
             val start = _route.value!!.points.first()
             val end = _route.value!!.points.last()
             calculateRoute(start, end)
        }
    }

    private fun fetchDiningAreas() {
        // Mock dining areas near charging stations
        val areas = listOf(
            Place("Haldiram's", 21.1458, 79.0882, "Dining"), // Nagpur
            Place("Karim's", 28.6508, 77.2334, "Dining"), // Delhi
            Place("Paradise Biryani", 17.4399, 78.4983, "Dining"), // Hyderabad
            Place("Saravana Bhavan", 13.0827, 80.2707, "Dining"), // Chennai
            Place("Leopold Cafe", 18.9233, 72.8315, "Dining") // Mumbai
        )
        _diningAreas.value = areas
    }
}

enum class TransportMode {
    CAR, BIKE
}

data class RouteInfo(
    val distance: String,
    val duration: String,
    val points: List<com.google.android.gms.maps.model.LatLng>
)

data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String
)

data class DirectionStep(
    val instruction: String,
    val distance: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
