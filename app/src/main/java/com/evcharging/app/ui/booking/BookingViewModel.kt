package com.evcharging.app.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.StationRepository
import com.evcharging.app.data.AuthRepository
import com.evcharging.app.data.BookingRepository
import com.evcharging.app.data.model.Dining
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _diningList = MutableStateFlow<List<Dining>>(emptyList())
    val diningList: StateFlow<List<Dining>> = _diningList.asStateFlow()

    private val _walletBalance = MutableStateFlow(0.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()

    init {
        fetchWalletBalance()
    }

    private fun fetchWalletBalance() {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val result = authRepository.getWalletBalance(userId)
                if (result.isSuccess) {
                    _walletBalance.value = result.getOrDefault(0.0)
                }
            }
        }
    }

    fun fetchDining(stationId: String) {
        if (stationId.isBlank()) return
        viewModelScope.launch {
            val result = stationRepository.getStationDining(stationId)
            if (result.isSuccess) {
                _diningList.value = result.getOrDefault(emptyList())
            }
        }
    }

    fun processBooking(
        stationId: String,
        stationName: String,
        amount: Double,
        paymentMethod: String,
        date: Long
    ) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _bookingState.value = BookingState.Error("User not logged in")
                return@launch
            }

            // 1. Handle Wallet Payment
            if (paymentMethod == "Wallet") {
                if (_walletBalance.value < amount) {
                    _bookingState.value = BookingState.Error("Insufficient wallet balance")
                    return@launch
                }
                val deductResult = authRepository.deductWalletBalance(userId, amount)
                if (deductResult.isFailure) {
                    _bookingState.value = BookingState.Error("Wallet transaction failed")
                    return@launch
                }
                _walletBalance.value -= amount // Optimistic update
            }

            // 2. Create Booking
            val bookingResult = bookingRepository.createBooking(stationName, amount.toString(), paymentMethod, date)
            if (bookingResult.isFailure) {
                _bookingState.value = BookingState.Error("Booking failed: ${bookingResult.exceptionOrNull()?.message}")
                return@launch
            }
            val bookingId = bookingResult.getOrNull() ?: ""
            currentBookingId = bookingId

            // 3D. Create Transaction
            val rrn = "TXN-${System.currentTimeMillis()}"
            val transaction = com.evcharging.app.data.model.Transaction(
                bookingId = bookingId,
                userId = userId,
                stationId = stationId,
                amount = amount,
                type = com.evcharging.app.data.model.TransactionType.BOOKING,
                status = com.evcharging.app.data.model.TransactionStatus.COMPLETED, // Mark as COMPLETED since money is deducted
                rrn = rrn,
                timestamp = com.google.firebase.Timestamp.now()
            )
            bookingRepository.createTransaction(transaction)

            _bookingState.value = BookingState.Success
        }
    }
    
    var currentBookingId: String = ""
        private set
    fun startCharging(bookingId: String) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            val result = bookingRepository.startCharging(bookingId)
            if (result.isSuccess) {
                _bookingState.value = BookingState.Charging(bookingId)
            } else {
                _bookingState.value = BookingState.Error("Failed to start charging: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun completeBooking(bookingId: String) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            val result = bookingRepository.completeBooking(bookingId)
            if (result.isSuccess) {
                _bookingState.value = BookingState.Completed
            } else {
                _bookingState.value = BookingState.Error("Failed to complete charging: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun cancelBooking() {
        if (currentBookingId.isBlank()) return
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            val result = bookingRepository.
            cancelBooking(currentBookingId)
            if (result.isSuccess) {
                _bookingState.value = BookingState.Cancelled
            } else {
                _bookingState.value = BookingState.Error("Failed to cancel booking: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}

sealed class BookingState {
    object Idle : BookingState()
    object Loading : BookingState()
    object Success : BookingState()
    data class Charging(val bookingId: String) : BookingState()
    object Completed : BookingState()
    object Cancelled : BookingState()
    data class Error(val message: String) : BookingState()
}
