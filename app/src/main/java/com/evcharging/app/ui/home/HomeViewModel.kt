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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userProfile = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userProfile: StateFlow<Map<String, Any>> = _userProfile

    init {
        fetchUserProfile()
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
    val carColor: StateFlow<String?> = _userProfile.map { it["carColor"] as? String }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)
}
