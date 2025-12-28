package com.evcharging.app.model

import com.google.firebase.Timestamp

data class ServiceRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val stationId: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val price: Double = 0.0,
    val status: String = "Pending", // Pending, Approved, Completed, Cancelled
    val timestamp: Timestamp = Timestamp.now()
)
