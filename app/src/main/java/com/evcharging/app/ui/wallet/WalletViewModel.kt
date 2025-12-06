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
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    init {
        fetchWalletData()
    }

    fun fetchWalletData() {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                // Fetch Balance
                val balanceResult = authRepository.getWalletBalance(userId)
                if (balanceResult.isSuccess) {
                    _balance.value = balanceResult.getOrDefault(0.0)
                }

                // Fetch Transactions (Using existing getChargingHistory or creating new one)
                // Assuming getChargingHistory returns Transactions, or we need a specific getTransactions method
                // Let's use getChargingHistory for now as it returns Transaction model
                val historyResult = bookingRepository.getChargingHistory()
                if (historyResult.isSuccess) {
                    _transactions.value = historyResult.getOrDefault(emptyList())
                }
            }
        }
    }
}
