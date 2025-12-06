package com.evcharging.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSupportViewModel @Inject constructor(
    private val repository: SupportRepository
) : ViewModel() {

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _sendSuccess = MutableStateFlow(false)
    val sendSuccess: StateFlow<Boolean> = _sendSuccess.asStateFlow()

    fun submitTicket(message: String) {
        viewModelScope.launch {
            _isSending.value = true
            val result = repository.createTicket(message)
            if (result.isSuccess) {
                _sendSuccess.value = true
            }
            _isSending.value = false
        }
    }
    
    fun resetSuccess() {
        _sendSuccess.value = false
    }
}
