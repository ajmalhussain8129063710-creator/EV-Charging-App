package com.evcharging.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val carModels = listOf(
        // Tesla
        "Tesla Model S", "Tesla Model 3", "Tesla Model X", "Tesla Model Y", "Tesla Cybertruck", "Tesla Roadster",
        // Nissan
        "Nissan Leaf", "Nissan Ariya", "Nissan Sakur",
        // Chevrolet
        "Chevrolet Bolt EV", "Chevrolet Bolt EUV", "Chevrolet Blazer EV", "Chevrolet Equinox EV", "Chevrolet Silverado EV",
        // Ford
        "Ford Mustang Mach-E", "Ford F-150 Lightning", "Ford E-Transit", "Ford Explorer EV",
        // Hyundai
        "Hyundai Kona Electric", "Hyundai Ioniq 5", "Hyundai Ioniq 6", "Hyundai Ioniq 7",
        // Kia
        "Kia Niro EV", "Kia EV6", "Kia EV9", "Kia Soul EV", "Kia EV5",
        // Audi
        "Audi e-tron", "Audi Q4 e-tron", "Audi Q8 e-tron", "Audi e-tron GT", "Audi Q6 e-tron",
        // Porsche
        "Porsche Taycan", "Porsche Macan Electric", "Porsche 718 Electric",
        // Volkswagen
        "Volkswagen ID.3", "Volkswagen ID.4", "Volkswagen ID.5", "Volkswagen ID.Buzz", "Volkswagen ID.7", "Volkswagen ID.2all",
        // BMW
        "BMW i3", "BMW i4", "BMW iX", "BMW i7", "BMW i5", "BMW iX1", "BMW iX3",
        // Mercedes-Benz
        "Mercedes-Benz EQA", "Mercedes-Benz EQB", "Mercedes-Benz EQC", "Mercedes-Benz EQE", "Mercedes-Benz EQS", "Mercedes-Benz EQS SUV", "Mercedes-Benz EQG",
        // Rivian
        "Rivian R1T", "Rivian R1S", "Rivian R2", "Rivian R3",
        // Lucid
        "Lucid Air", "Lucid Gravity",
        // Polestar
        "Polestar 2", "Polestar 3", "Polestar 4", "Polestar 5",
        // Volvo
        "Volvo XC40 Recharge", "Volvo C40 Recharge", "Volvo EX30", "Volvo EX90", "Volvo EM90",
        // Jaguar
        "Jaguar I-PACE",
        // Subaru
        "Subaru Solterra",
        // Toyota
        "Toyota bZ4X", "Toyota bZ3",
        // Lexus
        "Lexus RZ 450e", "Lexus UX 300e",
        // Cadillac
        "Cadillac Lyriq", "Cadillac Celestiq", "Cadillac Escalade IQ", "Cadillac Optiq",
        // GMC
        "GMC Hummer EV Pickup", "GMC Hummer EV SUV", "GMC Sierra EV",
        // Fisker
        "Fisker Ocean", "Fisker Pear", "Fisker Alaska",
        // VinFast
        "VinFast VF 6", "VinFast VF 7", "VinFast VF 8", "VinFast VF 9",
        // BYD
        "BYD Atto 3", "BYD Seal", "BYD Dolphin", "BYD Han", "BYD Tang",
        // MG
        "MG ZS EV", "MG4 EV", "MG5 EV", "MG Marvel R",
        // Tata
        "Tata Nexon EV", "Tata Tiago EV", "Tata Tigor EV", "Tata Punch EV", "Tata Harrier EV", "Tata Curvv EV",
        // Mahindra
        "Mahindra XUV400", "Mahindra BE.05", "Mahindra BE.07"
    )

    private val bikeModels = listOf(
        // Ola
        "Ola S1", "Ola S1 Pro", "Ola S1 Air", "Ola S1 X", "Ola S1 X+",
        // Ather
        "Ather 450X", "Ather 450S", "Ather Rizta", "Ather 450 Apex",
        // TVS
        "TVS iQube", "TVS X",
        // Bajaj
        "Bajaj Chetak",
        // Hero
        "Hero Vida V1 Pro", "Hero Vida V1 Plus",
        // Simple Energy
        "Simple One", "Simple Dot One",
        // Ultraviolette
        "Ultraviolette F77", "Ultraviolette F99",
        // Revolt
        "Revolt RV400", "Revolt RV400 BRZ",
        // Tork
        "Tork Kratos R",
        // Oben
        "Oben Rorr",
        // Matter
        "Matter Aera",
        // River
        "River Indie",
        // Ampere
        "Ampere Primus", "Ampere Magnus EX", "Ampere Zeal EX",
        // Okinawa
        "Okinawa Okhi-90", "Okinawa Praise Pro", "Okinawa Ridge+",
        // Hero Electric
        "Hero Electric Optima", "Hero Electric Photon", "Hero Electric NYX",
        // Pure EV
        "Pure EV EPluto 7G", "Pure EV eTryst 350", "Pure EV ecoDryft",
        // Komaki
        "Komaki Ranger", "Komaki Venice", "Komaki SE",
        // Hop
        "Hop Oxo", "Hop Leo",
        // Gogoro
        "Gogoro 2 Series", "Gogoro SuperSport",
        // Yulu
        "Yulu Wynn"
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

    suspend fun getVehicleModels(type: String = "Car"): Result<List<String>> {
        // For now, returning local lists directly based on type to ensure speed and reliability
        // In a real app, these could be fetched from separate Firestore documents
        return if (type.equals("Bike", ignoreCase = true)) {
            Result.success(bikeModels)
        } else {
            Result.success(carModels)
        }
    }

    suspend fun getVehicleColors(): Result<List<String>> {
        return Result.success(localColors)
    }

    private suspend fun seedConfig() {
        // Optional: Update Firestore if needed, but we are using local lists for now
    }
}
