package com.evcharging.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun createBooking(stationName: String, amount: String, paymentMethod: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val booking = hashMapOf(
                "userId" to userId,
                "stationName" to stationName,
                "amount" to amount,
                "paymentMethod" to paymentMethod,
                "timestamp" to System.currentTimeMillis(),
                "status" to "Confirmed"
            )
            firestore.collection("bookings").add(booking).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
