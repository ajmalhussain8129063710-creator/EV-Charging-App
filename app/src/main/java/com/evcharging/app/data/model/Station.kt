package com.evcharging.app.data.model

data class Station(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isAvailable: Boolean = true,
    val address: String = "",
    // New Admin Fields
    val imageUrl: String = "",
    val videoUrl: String = "",
    val authorityName: String = "",
    val chargerType: String = "Normal", // "Fast", "Normal", "Both"
    val amenities: List<String> = emptyList(), // "Dining", "Snacks", etc.
    val maintenanceStatus: String = "Operational", // "Maintenance", "Operational"
    val lastUpdated: Long = System.currentTimeMillis(),
    val dining: List<Map<String, Any>> = emptyList(),
    val promotions: List<Map<String, Any>> = emptyList(),
    val pointsPerKw: Double = 0.001,
    val rewards: List<Map<String, Any>> = emptyList()
)
