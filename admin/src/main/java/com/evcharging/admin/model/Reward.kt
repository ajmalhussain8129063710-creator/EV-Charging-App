package com.evcharging.admin.model

data class Reward(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val pointsCost: Int = 0,
    val type: String = "Discount", // "Discount", "Item"
    val value: Double = 0.0, // e.g., 10.0 for $10 off
    val stationId: String = ""
)
