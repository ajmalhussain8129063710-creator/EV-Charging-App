package com.evcharging.app.data

import com.evcharging.app.data.model.Station
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val localStations = listOf(
        Station(id = "1", name = "Orchard Central Charger", address = "181 Orchard Rd", latitude = 1.3007, longitude = 103.8397, status = "Available", isAvailable = true, chargerType = "DC Fast, AC Type 2"),
        Station(id = "2", name = "VivoCity EV Point", address = "1 HarbourFront Walk", latitude = 1.2642, longitude = 103.8223, status = "Available", isAvailable = true),
        Station(id = "3", name = "Marina Bay Sands Charging", address = "10 Bayfront Ave", latitude = 1.2834, longitude = 103.8607, status = "Busy", isAvailable = false),
        Station(id = "4", name = "Jurong Point Station", address = "1 Jurong West Central 2", latitude = 1.3403, longitude = 103.7060, status = "Available", isAvailable = true),
        Station(id = "5", name = "Changi Airport T3", address = "65 Airport Blvd", latitude = 1.3554, longitude = 103.9864, status = "Available", isAvailable = true),
        Station(id = "6", name = "Suntec City Mall", address = "3 Temasek Blvd", latitude = 1.2935, longitude = 103.8572, status = "Available", isAvailable = true),
        Station(id = "7", name = "ION Orchard", address = "2 Orchard Turn", latitude = 1.3040, longitude = 103.8319, status = "Available", isAvailable = true),
        Station(id = "8", name = "Great World City", address = "1 Kim Seng Promenade", latitude = 1.2933, longitude = 103.8322, status = "Available", isAvailable = true),
        Station(id = "9", name = "Plaza Singapura", address = "68 Orchard Rd", latitude = 1.3010, longitude = 103.8454, status = "Busy", isAvailable = false),
        Station(id = "10", name = "Paragon Shopping Centre", address = "290 Orchard Rd", latitude = 1.3038, longitude = 103.8358, status = "Available", isAvailable = true),
        Station(id = "11", name = "Bugis Junction", address = "200 Victoria St", latitude = 1.3005, longitude = 103.8560, status = "Available", isAvailable = true),
        Station(id = "12", name = "Raffles City Shopping Centre", address = "252 North Bridge Rd", latitude = 1.2940, longitude = 103.8534, status = "Available", isAvailable = true),
        Station(id = "13", name = "The Star Vista", address = "1 Vista Exchange Green", latitude = 1.3068, longitude = 103.7884, status = "Available", isAvailable = true),
        Station(id = "14", name = "Westgate", address = "3 Gateway Dr", latitude = 1.3337, longitude = 103.7423, status = "Available", isAvailable = true),
        Station(id = "15", name = "JEM", address = "50 Jurong Gateway Rd", latitude = 1.3330, longitude = 103.7436, status = "Busy", isAvailable = false),
        Station(id = "16", name = "Tampines Mall", address = "4 Tampines Central 5", latitude = 1.3526, longitude = 103.9447, status = "Available", isAvailable = true),
        Station(id = "17", name = "Century Square", address = "2 Tampines Central 5", latitude = 1.3516, longitude = 103.9442, status = "Available", isAvailable = true),
        Station(id = "18", name = "NEX", address = "23 Serangoon Central", latitude = 1.3506, longitude = 103.8722, status = "Available", isAvailable = true),
        Station(id = "19", name = "Waterway Point", address = "83 Punggol Central", latitude = 1.4067, longitude = 103.9022, status = "Available", isAvailable = true),
        Station(id = "20", name = "Northpoint City", address = "930 Yishun Ave 2", latitude = 1.4295, longitude = 103.8362, status = "Available", isAvailable = true)
    )

    suspend fun getStations(): Result<List<Station>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("stations").get().await()
            val stations = try {
                snapshot.toObjects(Station::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
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
            try {
                firestore.collection("stations").document(station.id).set(station).await()
            } catch (e: Exception) {
                // Squelch errors during seeding to prevent crash
                e.printStackTrace()
            }
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
        bufferKm: Double = 5.0
    ): List<Station> {
        // This is CPU bound, if list is huge, consider moving to Default dispatcher
        // But caller will typically be in suspend function.
        return stations.filter { station ->
            distanceFromLineSegment(
                station.latitude, station.longitude,
                startLat, startLon,
                endLat, endLon,
                bufferKm
            )
        }
    }

    private fun distanceFromLineSegment(
        lat: Double, lon: Double,
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double,
        bufferKm: Double
    ): Boolean {
        val distStartToStation = calculateDistance(startLat, startLon, lat, lon)
        val distStationToEnd = calculateDistance(lat, lon, endLat, endLon)
        val distStartToEnd = calculateDistance(startLat, startLon, endLat, endLon)
        
        return (distStartToStation + distStationToEnd) <= (distStartToEnd + bufferKm)
    }

    suspend fun getStationsNear(lat: Double, lng: Double, radiusKm: Double = 5.0, chargerType: String? = null): Result<List<Station>> = withContext(Dispatchers.IO) {
        try {
            // Fetch all stations and filter locally (for simplicity without geospatial query)
            val snapshot = firestore.collection("stations").get().await()
            val allStations = try {
                snapshot.toObjects(Station::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            
            val nearbyStations = allStations.filter { station ->
                val distance = calculateDistance(lat, lng, station.latitude, station.longitude)
                val withinRadius = distance <= radiusKm
                
                val typeMatch = if (chargerType != null) {
                    // Check if station's chargerType contains the requested type (e.g. "Fast" in "Fast Charger, DC")
                    // Safety: Access nullable field directly
                    station.chargerType?.contains(chargerType, ignoreCase = true) == true
                } else {
                    true
                }

                withinRadius && typeMatch
            }
            
            Result.success(nearbyStations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStationDining(stationId: String): Result<List<com.evcharging.app.data.model.Dining>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("stations").document(stationId)
                .collection("dining").get().await()
            val diningList = snapshot.toObjects(com.evcharging.app.data.model.Dining::class.java)
            Result.success(diningList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
