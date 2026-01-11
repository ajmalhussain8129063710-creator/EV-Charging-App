package com.evcharging.app.data.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val bookingId: String = "",
    val userId: String = "",
    val stationId: String = "",
    val amount: Double = 0.0,
    val type: String = "BOOKING", // BOOKING, REFUND, TOPUP
    val status: String = "PENDING", // PENDING, COMPLETED, FAILED, Confirmed, Charging
    val rrn: String = "", 
    val paymentMethod: String = "Wallet",
    val timestamp: Timestamp = Timestamp.now()
)
