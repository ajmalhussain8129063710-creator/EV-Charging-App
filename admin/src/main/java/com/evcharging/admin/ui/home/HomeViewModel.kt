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
}
