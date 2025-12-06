package com.evcharging.admin.model

data class AdminUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val stationId: String = "", // Link to the station they manage
    val phoneNumber: String = ""
)
