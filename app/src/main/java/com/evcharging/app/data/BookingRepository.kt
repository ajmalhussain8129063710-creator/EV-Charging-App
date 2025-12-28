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

    suspend fun createBooking(stationName: String, amount: String, paymentMethod: String, bookingDate: Long): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val amountDouble = amount.toDoubleOrNull() ?: 0.0
            
            // 1. Deduct Balance if Wallet
            if (paymentMethod == "Wallet") {
                // We need to deduct balance. 
                // Since BookingRepository doesn't have AuthRepository injected directly (it has Auth but AuthRepository is a wrapper),
                // we should duplicate the simple deduction logic or inject AuthRepository. 
                // For now, I'll implement the deduction logic directly here using Firestore transaction to ensure atomicity with booking creation if possible, 
                // but since they are different collections, we can just run a transaction for deduction first or part of the same batch.
                // Let's use a runTransaction for deduction first.
                
                firestore.runTransaction { transaction ->
                    val userRef = firestore.collection("users").document(userId)
                    val snapshot = transaction.get(userRef)
                    val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0
                    if (currentBalance >= amountDouble) {
                        transaction.update(userRef, "walletBalance", currentBalance - amountDouble)
                    } else {
                        throw Exception("Insufficient balance")
                    }
                }.await()
            }

            val booking = hashMapOf(
                "userId" to userId,
                "stationName" to stationName,
                "amount" to amount, 
                "paymentMethod" to paymentMethod,
                "bookingDate" to bookingDate,
                "timestamp" to System.currentTimeMillis(),
                "status" to "Confirmed"
            )
            val docRef = firestore.collection("bookings").add(booking).await()
            
            // Create corresponding Transaction record for Wallet History
            val transactionId = java.util.UUID.randomUUID().toString()
            val transactionRecord = hashMapOf(
                "id" to transactionId,
                "bookingId" to docRef.id,
                "userId" to userId,
                "stationId" to stationName, 
                "amount" to amountDouble, 
                "type" to "BOOKING",
                "status" to "PENDING", // Initial state is Pending until Charging Starts
                "paymentMethod" to paymentMethod,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "rrn" to "BKG-${System.currentTimeMillis()}"
            )
            firestore.collection("transactions").document(transactionId).set(transactionRecord).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTransaction(transaction: com.evcharging.app.data.model.Transaction): Result<Boolean> {
        return try {
            val docRef = firestore.collection("transactions").document()
            val transactionWithId = transaction.copy(id = docRef.id)
            docRef.set(transactionWithId).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startCharging(bookingId: String): Result<Boolean> {
        return try {
            firestore.collection("bookings").document(bookingId).update("status", "Charging").await()
            
            // Move Transaction to IN_PROGRESS (Confirmed/Active for Admin)
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("bookingId", bookingId)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                val docId = snapshot.documents[0].id
                firestore.collection("transactions").document(docId).update("status", "IN_PROGRESS").await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeBooking(bookingId: String): Result<Boolean> {
        return try {
            firestore.collection("bookings").document(bookingId).update("status", "Completed").await()
            
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("bookingId", bookingId)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                val docId = snapshot.documents[0].id
                firestore.collection("transactions").document(docId).update("status", "COMPLETED").await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChargingHistory(): Result<List<com.evcharging.app.data.model.Transaction>> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            
            // 1. Fetch Transactions
            val transactionsSnapshot = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val transactions = transactionsSnapshot.toObjects(com.evcharging.app.data.model.Transaction::class.java)

            // 2. Fetch Bookings (for historical data)
            val bookingsSnapshot = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            val bookingTransactions = bookingsSnapshot.documents.mapNotNull { doc ->
                try {
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val amountStr = doc.getString("amount") ?: "0.0"
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val bookingId = doc.id
                    val paymentMethod = doc.getString("paymentMethod") ?: "Wallet" // Default to Wallet
                    val statusStr = doc.getString("status") ?: "Unknown"
                    
                    // Check if this booking already has a corresponding transaction
                    if (transactions.any { it.bookingId == bookingId }) {
                        null 
                    } else {
                        val txStatus = when (statusStr) {
                            "Cancelled", "Failed" -> com.evcharging.app.data.model.TransactionStatus.FAILED
                            "Confirmed" -> com.evcharging.app.data.model.TransactionStatus.PENDING
                            "Charging" -> com.evcharging.app.data.model.TransactionStatus.IN_PROGRESS
                            "Completed" -> com.evcharging.app.data.model.TransactionStatus.COMPLETED
                            else -> com.evcharging.app.data.model.TransactionStatus.PENDING
                        }

                        com.evcharging.app.data.model.Transaction(
                            id = bookingId,
                            bookingId = bookingId,
                            userId = userId,
                            stationId = doc.getString("stationName") ?: "Unknown",
                            amount = amount,
                            type = com.evcharging.app.data.model.TransactionType.BOOKING,
                            status = txStatus,
                            rrn = "BKG-${timestamp}",
                            paymentMethod = paymentMethod,
                            timestamp = com.google.firebase.Timestamp(java.util.Date(timestamp))
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }

            // 3. Merge and Sort
            val allHistory = (transactions + bookingTransactions)
                .sortedByDescending { it.timestamp.seconds }

            Result.success(allHistory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Boolean> {
        return try {
            val bookingDoc = firestore.collection("bookings").document(bookingId).get().await()
            val userId = bookingDoc.getString("userId") ?: return Result.failure(Exception("User not found"))
            val amount = bookingDoc.getString("amount")?.toDoubleOrNull() ?: 0.0
            val paymentMethod = bookingDoc.getString("paymentMethod") ?: "Card"

            // 1. Update Booking Status
            firestore.collection("bookings").document(bookingId).update("status", "Cancelled").await()

            // 2. Refund Wallet if applicable
            // 2. Refund Processing
            if (amount > 0) {
                // Refund to Wallet Balance only if paid via Wallet
                if (paymentMethod == "Wallet") {
                     firestore.runTransaction { transaction ->
                        val userRef = firestore.collection("users").document(userId)
                        val snapshot = transaction.get(userRef)
                        val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0
                        transaction.update(userRef, "walletBalance", currentBalance + amount)
                    }.await()
                }

                // 3. Create Refund Transaction Record (For History)
                val transactionId = java.util.UUID.randomUUID().toString()
                val refundTransaction = hashMapOf(
                    "id" to transactionId,
                    "bookingId" to bookingId,
                    "userId" to userId,
                    "amount" to amount,
                    "type" to "REFUND",
                    "status" to "COMPLETED", // Refund transaction itself is complete
                    "paymentMethod" to paymentMethod, // Refunded to original source
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "rrn" to "REF-${System.currentTimeMillis()}"
                )
                firestore.collection("transactions").document(transactionId).set(refundTransaction).await()
            }

            // 4. Update original transaction status to refunded
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("bookingId", bookingId)
                .whereEqualTo("type", "BOOKING")
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                val docId = snapshot.documents[0].id
                firestore.collection("transactions").document(docId).update("status", "REFUNDED").await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPoints(): Result<Int> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val snapshot = firestore.collection("users").document(userId).get().await()
            val points = snapshot.getLong("points")?.toInt() ?: 0
            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserBookings(): Result<List<Map<String, Any>>> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val snapshot = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereIn("status", listOf("Confirmed", "Charging"))
                .get()
                .await()
            
            val bookings = snapshot.documents.map { doc ->
                doc.data!!.plus("id" to doc.id)
            }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
            
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

