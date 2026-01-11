package com.evcharging.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentResultManager @Inject constructor() {

    private val _paymentResult = MutableSharedFlow<PaymentResult>()
    val paymentResult: SharedFlow<PaymentResult> = _paymentResult.asSharedFlow()

    suspend fun onPaymentSuccess(razorpayPaymentId: String) {
        _paymentResult.emit(PaymentResult.Success(razorpayPaymentId))
    }

    suspend fun onPaymentError(code: Int, response: String?) {
        _paymentResult.emit(PaymentResult.Error(code, response ?: "Unknown error"))
    }
}

sealed class PaymentResult {
    data class Success(val paymentId: String) : PaymentResult()
    data class Error(val code: Int, val message: String) : PaymentResult()
}
