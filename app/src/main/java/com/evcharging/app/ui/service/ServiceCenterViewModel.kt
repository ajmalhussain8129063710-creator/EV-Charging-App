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
}
