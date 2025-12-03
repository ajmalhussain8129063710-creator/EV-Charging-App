package com.evcharging.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val localModels = listOf(
        "Tesla Model S", "Tesla Model 3", "Tesla Model X", "Tesla Model Y", "Tesla Cybertruck",
        "Nissan Leaf", "Nissan Ariya",
        "Chevrolet Bolt EV", "Chevrolet Bolt EUV", "Chevrolet Blazer EV", "Chevrolet Equinox EV", "Chevrolet Silverado EV",
        "Ford Mustang Mach-E", "Ford F-150 Lightning", "Ford E-Transit",
        "Hyundai Kona Electric", "Hyundai Ioniq 5", "Hyundai Ioniq 6",
        "Kia Niro EV", "Kia EV6", "Kia EV9", "Kia Soul EV",
        "Audi e-tron", "Audi Q4 e-tron", "Audi Q8 e-tron", "Audi e-tron GT",
        "Porsche Taycan", "Porsche Macan Electric",
        "Volkswagen ID.4", "Volkswagen ID.Buzz", "Volkswagen ID.7",
        "BMW i3", "BMW i4", "BMW iX", "BMW i7", "BMW i5",
        "Mercedes-Benz EQS", "Mercedes-Benz EQE", "Mercedes-Benz EQB", "Mercedes-Benz EQS SUV",
        "Rivian R1T", "Rivian R1S",
        "Lucid Air", "Lucid Gravity",
        "Polestar 2", "Polestar 3",
        "Volvo XC40 Recharge", "Volvo C40 Recharge", "Volvo EX30", "Volvo EX90",
        "Jaguar I-PACE",
        "Subaru Solterra",
        "Toyota bZ4X",
        "Lexus RZ 450e",
        "Cadillac Lyriq", "Cadillac Celestiq", "Cadillac Escalade IQ",
        "GMC Hummer EV Pickup", "GMC Hummer EV SUV", "GMC Sierra EV",
        "Fisker Ocean",
        "VinFast VF 8", "VinFast VF 9",
        "Ola S1", "Ola S1 Pro", "Ola S1 Air",
        "Ather 450X", "Ather 450S",
        "TVS iQube", "TVS X",
        "Bajaj Chetak",
        "Hero Vida V1",
        "Simple One",
        "Ultraviolette F77"
    )

    private val localColors = listOf(
        "White", "Pearl White", "Black", "Matte Black", "Silver", "Metallic Silver",
        "Gray", "Stealth Gray", "Gunmetal",
        "Red", "Crimson Red", "Cherry Red",
        "Blue", "Deep Blue", "Ocean Blue", "Electric Blue",
        "Brown", "Bronze",
        "Green", "Forest Green", "Lime Green",
        "Yellow", "Gold",
        "Orange", "Sunset Orange",
        "Purple", "Violet"
    )

    suspend fun getVehicleModels(): Result<List<String>> {
        return try {
            val snapshot = firestore.collection("config").document("vehicles").get().await()
            if (snapshot.exists()) {
                val models = snapshot.get("models") as? List<String> ?: emptyList()
                if (models.isEmpty()) {
                    // Try to seed, but return local if it fails or while waiting
                    try { seedConfig() } catch (e: Exception) { e.printStackTrace() }
                    Result.success(localModels)
                } else {
                    Result.success(models)
                }
            } else {
                try { seedConfig() } catch (e: Exception) { e.printStackTrace() }
                Result.success(localModels)
            }
        } catch (e: Exception) {
            // Fallback to local data on any error (e.g. permission denied)
            Result.success(localModels)
        }
    }

    suspend fun getVehicleColors(): Result<List<String>> {
        return try {
            val snapshot = firestore.collection("config").document("colors").get().await()
            if (snapshot.exists()) {
                val colors = snapshot.get("list") as? List<String> ?: emptyList()
                if (colors.isEmpty()) {
                    try { seedConfig() } catch (e: Exception) { e.printStackTrace() }
                    Result.success(localColors)
                } else {
                    Result.success(colors)
                }
            } else {
                try { seedConfig() } catch (e: Exception) { e.printStackTrace() }
                Result.success(localColors)
            }
        } catch (e: Exception) {
             // Fallback to local data on any error
            Result.success(localColors)
        }
    }

    private suspend fun seedConfig() {
        firestore.collection("config").document("vehicles").set(mapOf("models" to localModels)).await()
        firestore.collection("config").document("colors").set(mapOf("list" to localColors)).await()
    }
}
