package com.evcharging.app.data

import javax.inject.Inject
import javax.inject.Singleton

data class ServiceCenter(
    val id: String,
    val name: String,
    val address: String,
    val brand: String,
    val distance: String,
    val latitude: Double,
    val longitude: Double
)

@Singleton
class ServiceCenterRepository @Inject constructor() {

    fun getServiceCenters(brand: String): List<ServiceCenter> {
        // Mock Data
        val allCenters = listOf(
            ServiceCenter("1", "Tesla Service Center - Downtown", "123 Tech Blvd", "Tesla", "5.2 km", 1.3000, 103.8000),
            ServiceCenter("2", "Tesla Service Center - West", "456 Innovation Dr", "Tesla", "12.5 km", 1.3500, 103.7500),
            ServiceCenter("3", "BMW Service - Premium Auto", "789 Luxury Ln", "BMW", "3.8 km", 1.3100, 103.8200),
            ServiceCenter("4", "Nissan EV Care", "101 Leaf Rd", "Nissan", "8.0 km", 1.3200, 103.8500),
            ServiceCenter("5", "General EV Service", "202 Electric Ave", "Generic", "2.1 km", 1.3050, 103.8100)
        )

        // Filter by brand, or return generic/all if brand not found
        val brandCenters = allCenters.filter { it.brand.equals(brand, ignoreCase = true) }
        
        return if (brandCenters.isNotEmpty()) {
            brandCenters
        } else {
            // Return generic centers if specific brand not found
            allCenters.filter { it.brand == "Generic" }
        }
    }
    
    fun getNearbyServiceCenters(): List<ServiceCenter> {
        return listOf(
             ServiceCenter("5", "General EV Service", "202 Electric Ave", "Generic", "2.1 km", 1.3050, 103.8100),
             ServiceCenter("3", "BMW Service - Premium Auto", "789 Luxury Ln", "BMW", "3.8 km", 1.3100, 103.8200)
        )
    }
}
