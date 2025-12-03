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

    private val _cameraUpdate = MutableStateFlow<com.google.android.gms.maps.model.LatLng?>(null)
    val cameraUpdate: StateFlow<com.google.android.gms.maps.model.LatLng?> = _cameraUpdate

    init {
        fetchStations()
        fetchDiningAreas()
    }

    private fun fetchStations() {
        viewModelScope.launch {
            // Fetch mock stations first
            val result = repository.getStations()
            val currentStations = result.getOrDefault(emptyList()).toMutableList()
            _stations.value = currentStations

            // Try to fetch real stations from Google (Singapore center)
            try {
                val realStations = placesRepository.searchPlaces("EV Charging Station", com.google.android.gms.maps.model.LatLng(1.3521, 103.8198))
                realStations.take(5).forEach { prediction ->
                    val details = placesRepository.getPlaceDetails(prediction.placeId)
                    if (details != null && details.latLng != null) {
                        currentStations.add(
                            Station(
                                id = details.id ?: "google_${prediction.placeId}",
                                name = details.name ?: prediction.primaryText,
                                latitude = details.latLng!!.latitude,
                                longitude = details.latLng!!.longitude,
                                isAvailable = true, // Assume available for now
                                address = details.address ?: prediction.secondaryText
                            )
                        )
                    }
                }
                _stations.value = currentStations
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun onSearchResultSelected(placeId: String) {
        viewModelScope.launch {
            val place = placesRepository.getPlaceDetails(placeId)
            if (place != null && place.latLng != null) {
                // Add as a temporary dining area or point of interest to show on map
                val currentDining = _diningAreas.value.toMutableList()
                currentDining.add(Place(place.name ?: "Selected Location", place.latLng!!.latitude, place.latLng!!.longitude, "Search"))
                _diningAreas.value = currentDining
                
                // Trigger camera update
                _cameraUpdate.value = place.latLng
            }
            _searchResults.value = emptyList()
        }
    }

    fun onCameraMoved() {
        _cameraUpdate.value = null
    }

    private fun fetchDiningAreas() {
        // Mock dining areas near charging stations
        val areas = listOf(
            Place("Food Republic", 1.3007, 103.8397, "Dining"), // Near Orchard Central
            Place("VivoCity Food Court", 1.2642, 103.8223, "Dining"), // Near VivoCity
            Place("Rasapura Masters", 1.2834, 103.8607, "Dining"), // Near MBS
            Place("Kopitiam", 1.3403, 103.7060, "Dining"), // Near Jurong Point
            Place("Changi Eats", 1.3554, 103.9864, "Dining") // Near Changi
        )
        _diningAreas.value = areas
    }
}

data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String
)
