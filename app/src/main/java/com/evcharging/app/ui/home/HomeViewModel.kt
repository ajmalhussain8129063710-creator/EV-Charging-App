package com.evcharging.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val stationRepository: com.evcharging.app.data.StationRepository,
    private val bookingRepository: com.evcharging.app.data.BookingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userProfile = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userProfile: StateFlow<Map<String, Any>> = _userProfile

    private val _nearbyStations = MutableStateFlow<List<com.evcharging.app.data.model.Station>>(emptyList())
    val nearbyStations: StateFlow<List<com.evcharging.app.data.model.Station>> = _nearbyStations

    private val _chargingHistory = MutableStateFlow<List<com.evcharging.app.data.model.Transaction>>(emptyList())
    val chargingHistory: StateFlow<List<com.evcharging.app.data.model.Transaction>> = _chargingHistory

    private val _userPoints = MutableStateFlow<Int>(0)
    val userPoints: StateFlow<Int> = _userPoints

    private val _upcomingBookings = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val upcomingBookings: StateFlow<List<Map<String, Any>>> = _upcomingBookings

    init {
        fetchUserProfile()
        fetchNearbyStations()
        fetchHistoryAndPoints()
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            val result = bookingRepository.getUserBookings()
            _upcomingBookings.value = result.getOrDefault(emptyList())
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            val result = bookingRepository.cancelBooking(bookingId)
            if (result.isSuccess) {
                loadBookings() // Refresh
            }
        }
    }

    fun startCharging(bookingId: String) {
        viewModelScope.launch {
            val result = bookingRepository.startCharging(bookingId)
            if (result.isSuccess) {
                loadBookings()
            }
        }
    }

    fun refreshData() {
        loadBookings()
        fetchHistoryAndPoints()
        fetchNearbyStations()
        fetchUserProfile()
    }

    private fun fetchHistoryAndPoints() {
        viewModelScope.launch {
            val historyResult = bookingRepository.getChargingHistory()
            if (historyResult.isSuccess) {
                _chargingHistory.value = historyResult.getOrDefault(emptyList())
            }
            
            val pointsResult = bookingRepository.getUserPoints()
            if (pointsResult.isSuccess) {
                _userPoints.value = pointsResult.getOrDefault(0)
            }
        }
    }

    private fun fetchNearbyStations() {
        viewModelScope.launch {
            // Mock user location for now (e.g., Nagpur - India Center)
            val userLat = 21.1458
            val userLng = 79.0882
            
            val result = stationRepository.getStationsNear(userLat, userLng, 5.0)
            if (result.isSuccess) {
                _nearbyStations.value = result.getOrDefault(emptyList())
            }
        }
    }

    private fun fetchUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                val result = repository.getUserProfile(userId)
                if (result.isSuccess) {
                    _userProfile.value = result.getOrDefault(emptyMap())
                }
            }
        }
    }

    val carModel: StateFlow<String?> = _userProfile.map { it["carModel"] as? String }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)
    
    // 3D Car State
    private val _selectedCarColor = MutableStateFlow<androidx.compose.ui.graphics.Color>(androidx.compose.ui.graphics.Color.Red)
    val selectedCarColor: StateFlow<androidx.compose.ui.graphics.Color> = _selectedCarColor

    // Using highly reliable CDN model (Astronaut) to ensure loading works
    private val _car3dModelUrl = MutableStateFlow<String>("https://modelviewer.dev/shared-assets/models/Astronaut.glb") 
    val car3dModelUrl: StateFlow<String> = _car3dModelUrl

    fun updateCarColor(color: androidx.compose.ui.graphics.Color) {
        _selectedCarColor.value = color
    }

    fun searchAndSelectCar(modelName: String) {
        // Mock logic
        if (modelName.contains("Tesla", ignoreCase = true)) {
             _car3dModelUrl.value = "https://modelviewer.dev/shared-assets/models/RobotExpressive.glb" // Alternative model
        } else {
             _car3dModelUrl.value = "https://modelviewer.dev/shared-assets/models/Astronaut.glb"
        }
    }
}
