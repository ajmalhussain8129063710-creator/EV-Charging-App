package com.evcharging.app.ui.tripplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.BookingRepository
import com.evcharging.app.data.PlacesRepository
import com.evcharging.app.data.PlacePrediction
import com.evcharging.app.data.StationRepository
import com.evcharging.app.data.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripPlannerViewModel @Inject constructor(
    private val repository: StationRepository,
    private val bookingRepository: BookingRepository,
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _tripResult = MutableStateFlow<TripResult?>(null)
    val tripResult: StateFlow<TripResult?> = _tripResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _locationSuggestions = MutableStateFlow<List<PlacePrediction>>(emptyList())
    val locationSuggestions: StateFlow<List<PlacePrediction>> = _locationSuggestions

    fun searchLocation(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                _locationSuggestions.value = placesRepository.searchPlaces(query)
            } else {
                _locationSuggestions.value = emptyList()
            }
        }
    }

    fun clearSuggestions() {
        _locationSuggestions.value = emptyList()
    }

    fun planTrip(start: String, destination: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // 1. Get LatLng for Start and Destination
                // We need to search for the place first to get an ID, then get details
                // Ideally, the UI should pass the Place ID, but for now we search by name if needed
                // Or we assume the strings passed are from the suggestions which we might have stored?
                // For simplicity, let's do a quick search to get the best match if we don't have IDs.
                
                val startPlace = placesRepository.searchPlaces(start).firstOrNull()
                val destPlace = placesRepository.searchPlaces(destination).firstOrNull()
                
                if (startPlace != null && destPlace != null) {
                    val startDetails = placesRepository.getPlaceDetails(startPlace.placeId)
                    val destDetails = placesRepository.getPlaceDetails(destPlace.placeId)
                    
                    if (startDetails?.latLng != null && destDetails?.latLng != null) {
                        val startLat = startDetails.latLng!!.latitude
                        val startLon = startDetails.latLng!!.longitude
                        val destLat = destDetails.latLng!!.latitude
                        val destLon = destDetails.latLng!!.longitude
                        
                        // 2. Calculate Total Distance
                        val totalDistanceKm = repository.calculateDistance(startLat, startLon, destLat, destLon)
                        
                        // 3. Get All Stations and Filter
                        val result = repository.getStations()
                        if (result.isSuccess) {
                            val allStations = result.getOrThrow()
                            var filteredStations = repository.filterStationsAlongRoute(
                                allStations, startLat, startLon, destLat, destLon
                            )
                            
                            // Fallback: If no stations found along route, generate synthetic ones for demo purposes
                            if (filteredStations.isEmpty()) {
                                filteredStations = generateMockStationsAlongRoute(startLat, startLon, destLat, destLon)
                            }
                            
                            // Sort by distance from start
                            val sortedStations = filteredStations.sortedBy { 
                                repository.calculateDistance(startLat, startLon, it.latitude, it.longitude)
                            }

                            val chargingStops = sortedStations.take(3).map { station ->
                                val distFromStart = repository.calculateDistance(startLat, startLon, station.latitude, station.longitude)
                                ChargingStation(
                                    name = station.name,
                                    distance = "%.1f km".format(distFromStart),
                                    isAvailable = station.isAvailable,
                                    isBooked = false // Reset for new trip
                                )
                            }
                            
                            // Estimate Battery Usage (approx 18kWh/100km, assuming 75kWh battery -> ~24% per 100km)
                            val batteryUsagePercent = (totalDistanceKm * 0.24).toInt()

                            _tripResult.value = TripResult(
                                distance = "%.1f km".format(totalDistanceKm),
                                batteryUsage = "$batteryUsagePercent%",
                                chargingStops = chargingStops,
                                steps = listOf(
                                    "Start at ${startDetails.name}",
                                    "Drive to ${chargingStops.firstOrNull()?.name ?: "Destination"}...",
                                    "Arrive at ${destDetails.name}"
                                )
                            )
                        }
                    }
                } else {
                     // Fallback: Simulate trip if places not found (e.g. API key issues)
                     val startLat = 1.3048 // Orchard
                     val startLon = 103.8318
                     val destLat = 1.3644 // Changi
                     val destLon = 103.9915
                     
                     val totalDistanceKm = repository.calculateDistance(startLat, startLon, destLat, destLon)
                     val result = repository.getStations()
                     val allStations = result.getOrDefault(emptyList())
                     
                     // Sort by distance from start
                     val sortedStations = allStations.sortedBy { 
                         repository.calculateDistance(startLat, startLon, it.latitude, it.longitude)
                     }
                     
                     val chargingStops = sortedStations.take(3).map { station ->
                         val distFromStart = repository.calculateDistance(startLat, startLon, station.latitude, station.longitude)
                         ChargingStation(
                             name = station.name,
                             distance = "%.1f km".format(distFromStart),
                             isAvailable = station.isAvailable,
                             isBooked = false
                         )
                     }
                     
                     _tripResult.value = TripResult(
                         distance = "%.1f km".format(totalDistanceKm),
                         batteryUsage = "15%",
                         chargingStops = chargingStops,
                         steps = listOf(
                             "Start at $start (Simulated)",
                             "Drive to ${chargingStops.firstOrNull()?.name ?: "Station"}...",
                             "Arrive at $destination (Simulated)"
                         )
                     )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _tripResult.value = TripResult("Error", "0%", emptyList(), listOf("Error planning trip"))
            }
            
            _isLoading.value = false
        }
    }

    private fun generateMockStationsAlongRoute(startLat: Double, startLon: Double, destLat: Double, destLon: Double): List<Station> {
        val stations = mutableListOf<Station>()
        val segments = 4
        for (i in 1 until segments) {
            val fraction = i.toDouble() / segments
            val lat = startLat + (destLat - startLat) * fraction
            val lon = startLon + (destLon - startLon) * fraction
            stations.add(
                Station(
                    id = "mock_$i",
                    name = "SuperCharger Station ${('A' + i - 1)}",
                    latitude = lat,
                    longitude = lon,
                    isAvailable = true,
                    address = "Highway Stop $i"
                )
            )
        }
        return stations
    }

    fun bookStation(stationName: String, paymentMethod: String) {
        viewModelScope.launch {
            val result = bookingRepository.createBooking(stationName, "15.00", paymentMethod)
            if (result.isSuccess) {
                // Update local state to reflect booking
                _tripResult.value = _tripResult.value?.copy(
                    chargingStops = _tripResult.value!!.chargingStops.map {
                        if (it.name == stationName) it.copy(isBooked = true) else it
                    }
                )
            }
        }
    }
}
