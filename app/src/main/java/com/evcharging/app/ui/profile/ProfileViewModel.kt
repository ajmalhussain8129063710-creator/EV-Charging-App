package com.evcharging.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.AuthRepository
import com.evcharging.app.data.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userProfile: StateFlow<Map<String, Any>> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Dropdown Data
    private val _carModels = MutableStateFlow<List<String>>(emptyList())
    val carModels: StateFlow<List<String>> = _carModels.asStateFlow()

    private val _carColors = MutableStateFlow<List<String>>(emptyList())
    val carColors: StateFlow<List<String>> = _carColors.asStateFlow()

    init {
        fetchUserProfile()
        loadDropdownData()
    }

    private fun loadDropdownData() {
        viewModelScope.launch {
            _carModels.value = configRepository.getVehicleModels("Car").getOrDefault(emptyList())
            _carColors.value = configRepository.getVehicleColors().getOrDefault(emptyList())
        }
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val result = authRepository.getUserProfile(userId)
                if (result.isSuccess) {
                    _userProfile.value = result.getOrDefault(emptyMap())
                } else {
                    _errorMessage.value = "Failed to load profile"
                }
            }
            _isLoading.value = false
        }
    }

    fun updateUserProfile(name: String, carModel: String, carColor: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _updateSuccess.value = false
            _errorMessage.value = null
            
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val result = authRepository.updateUserProfile(userId, name, carModel, carColor)
                if (result.isSuccess) {
                    _updateSuccess.value = true
                    fetchUserProfile() // Refresh data
                } else {
                    _errorMessage.value = "Failed to update profile: ${result.exceptionOrNull()?.message}"
                }
            }
            _isLoading.value = false
        }
    }
    
    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }
}
