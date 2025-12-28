package com.evcharging.app.ui.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.AuthRepository
import com.evcharging.app.data.ServiceCenter
import com.evcharging.app.data.ServiceCenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ServiceCenterViewModel @Inject constructor(
    private val serviceCenterRepository: ServiceCenterRepository,
    private val authRepository: AuthRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _serviceCenters = MutableStateFlow<List<ServiceCenter>>(emptyList())
    val serviceCenters: StateFlow<List<ServiceCenter>> = _serviceCenters.asStateFlow()

    private val _nearbyCenters = MutableStateFlow<List<ServiceCenter>>(emptyList())
    val nearbyCenters: StateFlow<List<ServiceCenter>> = _nearbyCenters.asStateFlow()
    
    private val _userCarBrand = MutableStateFlow<String?>(null)
    val userCarBrand: StateFlow<String?> = _userCarBrand.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val result = authRepository.getUserProfile(userId)
                if (result.isSuccess) {
                    val profile = result.getOrDefault(emptyMap())
                    val carModel = profile["carModel"] as? String ?: "Generic"
                    
                    // Simple extraction logic: First word is brand
                    val brand = carModel.split(" ").firstOrNull() ?: "Generic"
                    _userCarBrand.value = brand

                    _serviceCenters.value = serviceCenterRepository.getServiceCenters(brand)
                    _nearbyCenters.value = serviceCenterRepository.getNearbyServiceCenters()
                }
            } else {
                 // Fallback if not logged in
                 _serviceCenters.value = serviceCenterRepository.getServiceCenters("Generic")
                 _nearbyCenters.value = serviceCenterRepository.getNearbyServiceCenters()
            }
        }
    }

    private val _services = MutableStateFlow<List<com.evcharging.app.model.ServiceItem>>(emptyList())
    val services: StateFlow<List<com.evcharging.app.model.ServiceItem>> = _services.asStateFlow()

    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    fun fetchServices(stationId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("stations")
                    .document(stationId)
                    .collection("services")
                    .get()
                    .await()
                val list = snapshot.toObjects(com.evcharging.app.model.ServiceItem::class.java)
                _services.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun bookService(service: com.evcharging.app.model.ServiceItem, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                onResult(false)
                return@launch
            }
            
            // Fetch user name for the request
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userName = userDoc.getString("name") ?: "Unknown User"

            val request = com.evcharging.app.model.ServiceRequest(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                stationId = service.stationId,
                serviceId = service.id,
                serviceName = service.name,
                price = service.price,
                status = "Pending",
                timestamp = com.google.firebase.Timestamp.now()
            )

            try {
                firestore.collection("service_requests")
                    .document(request.id)
                    .set(request)
                    .await()
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }
}
