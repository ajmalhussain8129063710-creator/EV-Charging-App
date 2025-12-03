package com.evcharging.app.util

import com.evcharging.app.R

object CarResourceHelper {

    fun getCarImageResource(modelName: String?): Int {
        if (modelName == null) return R.drawable.car_front

        val lowerName = modelName.lowercase()
        return when {
            // Trucks
            lowerName.contains("cybertruck") || 
            lowerName.contains("f-150") || 
            lowerName.contains("rivian r1t") || 
            lowerName.contains("hummer") || 
            lowerName.contains("sierra") -> R.drawable.car_front // TODO: Replace with car_truck_front when available

            // SUVs / Crossovers
            lowerName.contains("model x") || 
            lowerName.contains("model y") || 
            lowerName.contains("mach-e") || 
            lowerName.contains("ioniq 5") || 
            lowerName.contains("ev6") || 
            lowerName.contains("id.4") || 
            lowerName.contains("eqs suv") || 
            lowerName.contains("rivian r1s") -> R.drawable.car_front // TODO: Replace with car_suv_front when available

            // Sedans / Coupes (Default)
            else -> R.drawable.car_front
        }
    }
}
