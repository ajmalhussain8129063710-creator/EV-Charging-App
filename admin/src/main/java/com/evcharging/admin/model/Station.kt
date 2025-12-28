package com.evcharging.admin.model

data class Station(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val videoUrl: String = "",
    val description: String = "",
    val amenities: List<String> = emptyList(),
    val chargerTypes: List<String> = emptyList(),
    val pricePerKw: Double = 0.0,
    val status: String = "Available", // Available, Busy, Offline
    val adminId: String = "",
    val pointsPerKw: Double = 0.001, // Default earning rate
    val type: String = "Charging Station"
)
