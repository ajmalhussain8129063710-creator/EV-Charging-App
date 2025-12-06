package com.evcharging.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.AuthRepository
import com.evcharging.app.data.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _vehicleModels = MutableStateFlow<List<String>>(emptyList())
    val vehicleModels: StateFlow<List<String>> = _vehicleModels

    private val _vehicleColors = MutableStateFlow<List<String>>(emptyList())
    val vehicleColors: StateFlow<List<String>> = _vehicleColors

    private val _vehicleType = MutableStateFlow("Car")
    val vehicleType: StateFlow<String> = _vehicleType

    init {
        fetchConfig("Car")
    }

    fun setVehicleType(type: String) {
        _vehicleType.value = type
        fetchConfig(type)
    }

    private fun fetchConfig(type: String) {
        viewModelScope.launch {
            val modelsResult = configRepository.getVehicleModels(type)
            if (modelsResult.isSuccess) {
                _vehicleModels.value = modelsResult.getOrDefault(emptyList())
            }
            val colorsResult = configRepository.getVehicleColors()
            if (colorsResult.isSuccess) {
                _vehicleColors.value = colorsResult.getOrDefault(emptyList())
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun signUp(emailOrPhone: String, pass: String, name: String, carModel: String, carColor: String, phoneNumber: String, vehicleType: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signUp(emailOrPhone, pass, name, carModel, carColor, phoneNumber, vehicleType)
            if (result.isSuccess) {
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
