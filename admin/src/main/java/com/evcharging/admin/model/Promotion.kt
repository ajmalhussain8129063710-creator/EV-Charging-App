package com.evcharging.admin.model

data class Promotion(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val discountPercentage: Double = 0.0,
    val expiryDate: Long = 0,
    val stationId: String = ""
)
