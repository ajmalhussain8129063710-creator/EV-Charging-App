package com.evcharging.app.data

import com.evcharging.app.data.model.Station
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val localStations = listOf(
        Station("1", "Orchard Central Charger", 1.3007, 103.8397, true, "181 Orchard Rd"),
        Station("2", "VivoCity EV Point", 1.2642, 103.8223, true, "1 HarbourFront Walk"),
        Station("3", "Marina Bay Sands Charging", 1.2834, 103.8607, false, "10 Bayfront Ave"),
        Station("4", "Jurong Point Station", 1.3403, 103.7060, true, "1 Jurong West Central 2"),
        Station("5", "Changi Airport T3", 1.3554, 103.9864, true, "65 Airport Blvd"),
        Station("6", "Suntec City Mall", 1.2935, 103.8572, true, "3 Temasek Blvd"),
        Station("7", "ION Orchard", 1.3040, 103.8319, true, "2 Orchard Turn"),
        Station("8", "Great World City", 1.2933, 103.8322, true, "1 Kim Seng Promenade"),
        Station("9", "Plaza Singapura", 1.3010, 103.8454, false, "68 Orchard Rd"),
        Station("10", "Paragon Shopping Centre", 1.3038, 103.8358, true, "290 Orchard Rd"),
        Station("11", "Bugis Junction", 1.3005, 103.8560, true, "200 Victoria St"),
        Station("12", "Raffles City Shopping Centre", 1.2940, 103.8534, true, "252 North Bridge Rd"),
        Station("13", "The Star Vista", 1.3068, 103.7884, true, "1 Vista Exchange Green"),
        Station("14", "Westgate", 1.3337, 103.7423, true, "3 Gateway Dr"),
        Station("15", "JEM", 1.3330, 103.7436, false, "50 Jurong Gateway Rd"),
        Station("16", "Tampines Mall", 1.3526, 103.9447, true, "4 Tampines Central 5"),
        Station("17", "Century Square", 1.3516, 103.9442, true, "2 Tampines Central 5"),
        Station("18", "NEX", 1.3506, 103.8722, true, "23 Serangoon Central"),
        Station("19", "Waterway Point", 1.4067, 103.9022, true, "83 Punggol Central"),
        Station("20", "Northpoint City", 1.4295, 103.8362, true, "930 Yishun Ave 2")
    )

    suspend fun getStations(): Result<List<Station>> {
        return try {
            val snapshot = firestore.collection("stations").get().await()
            val stations = snapshot.toObjects(Station::class.java)
            if (stations.isEmpty()) {
                try { seedStations() } catch (e: Exception) { e.printStackTrace() }
                Result.success(localStations)
            } else {
                Result.success(stations)
            }
        } catch (e: Exception) {
            // Fallback to local data on error
            Result.success(localStations)
        }
    }

    private suspend fun seedStations() {
        localStations.forEach { station ->
            firestore.collection("stations").document(station.id).set(station).await()
        }
    }

    // --- Helper Functions for Distance & Filtering ---

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    fun filterStationsAlongRoute(
        stations: List<Station>,
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        bufferKm: Double = 50.0 // Increased buffer to 50km for better detection with sparse data
    ): List<Station> {
        return stations.filter { station ->
            val dist = distanceFromLineSegment(
                station.latitude, station.longitude,
                startLat, startLon,
                endLat, endLon,
                bufferKm
            )
            dist
        }
    }

    private fun distanceFromLineSegment(
        lat: Double, lon: Double,
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double,
        bufferKm: Double
    ): Boolean {
        // Simple "is between" check:
        // Distance(Start, Station) + Distance(Station, End) approx equals Distance(Start, End)
        
        val distStartToStation = calculateDistance(startLat, startLon, lat, lon)
        val distStationToEnd = calculateDistance(lat, lon, endLat, endLon)
        val distStartToEnd = calculateDistance(startLat, startLon, endLat, endLon)
        
        // If the station is "along the way", the sum of distances should be close to the total distance.
        // Allow for a detour factor (e.g., bufferKm)
        return (distStartToStation + distStationToEnd) <= (distStartToEnd + bufferKm)
    }
}
