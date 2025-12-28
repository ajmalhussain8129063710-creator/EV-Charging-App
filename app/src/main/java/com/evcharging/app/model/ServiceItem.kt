package com.evcharging.app.model

data class ServiceItem(
    val id: String = "",
    val stationId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val enabled: Boolean = true
)
