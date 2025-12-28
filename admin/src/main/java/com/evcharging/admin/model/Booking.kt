package com.evcharging.admin.model

import com.google.firebase.Timestamp

data class Booking(
    val id: String = "",
    val userId: String = "",
    val stationName: String = "",
    val amount: String = "0.0",
    val paymentMethod: String = "",
    val bookingDate: Long = 0,
    val status: String = "",
    val timestamp: Long = 0
)
