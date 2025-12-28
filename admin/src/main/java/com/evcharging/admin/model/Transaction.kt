package com.evcharging.admin.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val bookingId: String = "",
    val userId: String = "",
    val stationId: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.BOOKING, // BOOKING, REFUND, TOPUP
    val status: TransactionStatus = TransactionStatus.PENDING, // PENDING, COMPLETED, FAILED
    val timestamp: Timestamp = Timestamp.now()
)

enum class TransactionType {
    BOOKING, REFUND, TOPUP
}

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED, REFUNDED, IN_PROGRESS
}
