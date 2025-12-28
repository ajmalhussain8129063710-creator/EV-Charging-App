package com.evcharging.admin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ServiceCenterDashboardViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _pendingRequestsCount = MutableStateFlow(0)
    val pendingRequestsCount: StateFlow<Int> = _pendingRequestsCount.asStateFlow()

    private val _activeServicesCount = MutableStateFlow(0)
    val activeServicesCount: StateFlow<Int> = _activeServicesCount.asStateFlow()

    init {
        listenForData()
    }

    private fun listenForData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val adminDoc = firestore.collection("admins").document(userId).get().await()
            val stationId = adminDoc.getString("stationId")

            if (stationId != null) {
                // Listen for Pending Requests
                firestore.collection("service_requests")
                    .whereEqualTo("stationId", stationId)
                    .whereEqualTo("status", "Pending")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            _pendingRequestsCount.value = snapshot.size()
                        }
                    }

                // Listen for Active Services
                firestore.collection("stations")
                    .document(stationId)
                    .collection("services")
                    .whereEqualTo("enabled", true)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            _activeServicesCount.value = snapshot.size()
                        }
                    }
            }
        }
    }
}
