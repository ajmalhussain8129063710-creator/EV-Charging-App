package com.evcharging.admin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.model.AdminUser
import com.evcharging.admin.model.Station
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _station = MutableStateFlow<Station?>(null)
    val station: StateFlow<Station?> = _station.asStateFlow()

    val stationType: StateFlow<String> = _station.map { it?.type ?: "Charging Station" }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "Charging Station")

    private val _adminUser = MutableStateFlow<AdminUser?>(null)
    val adminUser: StateFlow<AdminUser?> = _adminUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookings = MutableStateFlow<List<com.evcharging.admin.model.Booking>>(emptyList())
    val bookings: StateFlow<List<com.evcharging.admin.model.Booking>> = _bookings.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    // Fetch Admin User
                    val adminSnapshot = firestore.collection("admins").document(userId).get().await()
                    val admin = adminSnapshot.toObject(AdminUser::class.java)
                    _adminUser.value = admin

                    // Fetch Station
                    if (admin?.stationId != null) {
                        val stationSnapshot = firestore.collection("stations").document(admin.stationId).get().await()
                        val stationData = stationSnapshot.toObject(Station::class.java)
                        _station.value = stationData

                        // Fetch Bookings
                        // Filter by station name (Assuming stationName is unique or ID matches, simplified for now matching stationName)
                        if (stationData != null) {
                            val bookingsSnapshot = firestore.collection("bookings")
                                .whereEqualTo("stationName", stationData.name) // ideally stationId
                                .whereIn("status", listOf("Confirmed", "Charging"))
                                .get()
                                .await()
                            
                            val bookingList = bookingsSnapshot.documents.mapNotNull { doc ->
                                doc.toObject(com.evcharging.admin.model.Booking::class.java)?.copy(id = doc.id)
                            }.sortedByDescending { it.timestamp }
                            
                            _bookings.value = bookingList
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refresh() {
        fetchData()
    }

    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            try {
                val currentStation = _station.value ?: return@launch
                firestore.collection("stations").document(currentStation.id)
                    .update("status", newStatus).await()
                _station.value = currentStation.copy(status = newStatus)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startCharging(bookingId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // 1. Update Booking Status
                firestore.collection("bookings").document(bookingId).update("status", "Charging").await()

                // 2. Update Transaction Status (Pending -> In Progress)
                val snapshot = firestore.collection("transactions")
                    .whereEqualTo("bookingId", bookingId)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    val docId = snapshot.documents[0].id
                    firestore.collection("transactions").document(docId).update("status", "IN_PROGRESS").await()
                }

                // Refresh Data
                fetchData()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun stopCharging(bookingId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // 1. Update Booking Status
                firestore.collection("bookings").document(bookingId).update("status", "Completed").await()

                // 2. Update Transaction Status
                val snapshot = firestore.collection("transactions")
                    .whereEqualTo("bookingId", bookingId)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    val docId = snapshot.documents[0].id
                    firestore.collection("transactions").document(docId).update("status", "COMPLETED").await()
                }

                // Refresh Data
                fetchData()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
