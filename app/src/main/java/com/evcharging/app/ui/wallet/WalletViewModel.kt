package com.evcharging.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.AuthRepository
import com.evcharging.app.data.BookingRepository
import com.evcharging.app.data.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository,
    private val paymentResultManager: com.evcharging.app.data.PaymentResultManager
) : ViewModel() {

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions
    
    private val _paymentEvent = kotlinx.coroutines.flow.MutableSharedFlow<WalletPaymentEvent>()
    val paymentEvent = _paymentEvent

    private var pendingRechargeAmount: Double = 0.0

    init {
        fetchWalletData()
        observePaymentResults()
    }

    private fun observePaymentResults() {
        viewModelScope.launch {
            paymentResultManager.paymentResult.collect { result ->
                when (result) {
                    is com.evcharging.app.data.PaymentResult.Success -> {
                       if (pendingRechargeAmount > 0) {
                           addBalance(pendingRechargeAmount)
                           pendingRechargeAmount = 0.0
                       }
                    }
                    is com.evcharging.app.data.PaymentResult.Error -> {
                        // Handle error (show toast/snackbar via event if needed)
                        pendingRechargeAmount = 0.0
                    }
                }
            }
        }
    }

    fun fetchWalletData() {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            
            // Fetch Balance
            val balanceResult = authRepository.getWalletBalance(userId)
            if (balanceResult.isSuccess) {
                _balance.value = balanceResult.getOrDefault(0.0)
            }

            val historyResult = bookingRepository.getChargingHistory()
            if (historyResult.isSuccess) {
                _transactions.value = historyResult.getOrDefault(emptyList())
            }
        }
    }
    
    fun initiateRecharge(amount: Double) {
        if (amount <= 0) return
        pendingRechargeAmount = amount
        viewModelScope.launch {
            _paymentEvent.emit(WalletPaymentEvent.StartPayment(amount * 100)) // Paise
        }
    }

    private fun addBalance(amount: Double) {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val result = authRepository.addWalletBalance(userId, amount)
            if (result.isSuccess) {
                fetchWalletData() // Refresh
            }
        }
    }
}

sealed class WalletPaymentEvent {
    data class StartPayment(val amountInPaise: Double) : WalletPaymentEvent()
}
