package com.evcharging.app.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val carModel: String = "",
    val carColor: String = "",
    val car3dModelUrl: String = "", // URL to the uploaded 3D model/image
    val phoneNumber: String = "",
    val walletBalance: Double = 0.0,
    val points: Int = 0,
    val vehicleType: String = "Car"
)
