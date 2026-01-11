package com.evcharging.app.ui.tripplanner

data class TripResult(
    val distance: String,
    val batteryUsage: String,
    val chargingStops: List<ChargingStation>,
    val steps: List<String> = emptyList()
)

data class ChargingStation(
    val id: String,
    val name: String,
    val distance: String,
    val isAvailable: Boolean,
    val isBooked: Boolean = false
)
