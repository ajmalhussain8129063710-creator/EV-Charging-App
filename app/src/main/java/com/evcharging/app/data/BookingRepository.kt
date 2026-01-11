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
            
            // 0. Check Slot Availability (Prevent Double Booking)
            // Query for overlapping bookings: 
            // Existing Start < Requested End AND Existing End > Requested Start
            // AND status is NOT Cancelled.
            // Using a simple check for now. For production, composite indexes are needed.
            val existingBookings = firestore.collection("bookings")
                .whereEqualTo("stationName", stationName)
                .whereNotEqualTo("status", "Cancelled")
                .get()
                .await()

            val requestedEndTime = bookingDate + (1000 * 60 * 60) // Assuming 1 hour slot for simplification or passed duration
            
            val isSlotTaken = existingBookings.documents.any { doc ->
                val start = doc.getLong("bookingDate") ?: 0L
                val end = doc.getLong("endTime") ?: (start + 3600000) // Default 1 hr if not set
                // Overlap formula: (StartA <= EndB) and (EndA >= StartB)
                // Here: (bookingDate < end) and (requestedEndTime > start)
                bookingDate < end && requestedEndTime > start
            }

            if (isSlotTaken) {
                throw Exception("Slot already booked. Please choose another time.")
            }

            val amountDouble = amount.toDoubleOrNull() ?: 0.0
            
            // 1. Deduct Balance if Wallet
            if (paymentMethod == "Wallet") {
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
                "endTime" to requestedEndTime, // Save end time for checks
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
                "status" to "PENDING", 
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
            val updates = mapOf(
                "status" to "Charging",
                "startTime" to System.currentTimeMillis()
            )
            firestore.collection("bookings").document(bookingId).update(updates).await()
            
            // Move Transaction to IN_PROGRESS
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
            val bookingRef = firestore.collection("bookings").document(bookingId)
            val bookingDoc = bookingRef.get().await()
            
            if (bookingDoc.getString("status") == "Completed") {
                 return Result.success(true) // Already completed
            }

            val startTime = bookingDoc.getLong("startTime") ?: 0L
            val amount = bookingDoc.getString("amount")?.toDoubleOrNull() ?: 0.0
            val userId = bookingDoc.getString("userId") ?: ""
            val paymentMethod = bookingDoc.getString("paymentMethod") ?: "Wallet"

            // Logic: User pays for 30 mins (Total Amount). 
            // Rate = Amount / 30 min.
            // Refund = Amount - (ElapsedMin * Rate)
            
            var refundAmount = 0.0
            
            if (startTime > 0 && amount > 0) {
                val endTime = System.currentTimeMillis()
                val elapsedMillis = endTime - startTime
                val elapsedMinutes = elapsedMillis / 60000.0
                
                // Pricing Configuration: 200 INR per 60 mins -> 3.3333 INR/min
                val pricePerMinute = 200.0 / 60.0 

                // Calculate Actual Usage Amount
                val actualUsageAmount = elapsedMinutes * pricePerMinute
                
                // Calculate Refund: Booked - Used
                if (actualUsageAmount < amount) {
                    val rawRefund = amount - actualUsageAmount
                    // Round to 2 decimal places using standard financial rounding
                    refundAmount = kotlin.math.round(rawRefund * 100) / 100.0
                }
            }

            if (refundAmount > 0.5) { // Minimum threshold 50 paise
                 // 1. Credit Wallet
                 // We only refund to wallet if they paid by Wallet or just credit Wallet as generic refund? 
                 // User said "withdrawn from user wallet should be credit". Assuming generic Wallet credit.
                 
                 firestore.runTransaction { transaction ->
                    val userRef = firestore.collection("users").document(userId)
                    val snapshot = transaction.get(userRef)
                    val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0
                    transaction.update(userRef, "walletBalance", currentBalance + refundAmount)
                 }.await()

                 // 2. Create Refund Transaction
                 val transactionId = java.util.UUID.randomUUID().toString()
                 val refundTransaction = hashMapOf(
                    "id" to transactionId,
                    "bookingId" to bookingId,
                    "userId" to userId,
                    "amount" to refundAmount,
                    "type" to "REFUND",
                    "status" to "COMPLETED",
                    "paymentMethod" to "Wallet",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "rrn" to "REF-${System.currentTimeMillis()}"
                 )
                 firestore.collection("transactions").document(transactionId).set(refundTransaction).await()
            }

            // 3. Mark Booking Completed
            val updates = hashMapOf<String, Any>(
                "status" to "Completed",
                "endTime" to System.currentTimeMillis()
            )
            if (refundAmount > 0) {
                updates["refundedAmount"] = refundAmount
            }
            bookingRef.update(updates).await()
            
            // 4. Update Original Transaction Status
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("bookingId", bookingId)
                .whereEqualTo("type", "BOOKING")
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
            
            val transactions = transactionsSnapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val amountDouble = doc.getDouble("amount") ?: doc.getString("amount")?.toDoubleOrNull() ?: 0.0
                    val type = doc.getString("type") ?: "UNKNOWN"
                    val status = doc.getString("status") ?: "UNKNOWN"
                    val bookingId = doc.getString("bookingId") ?: ""
                    val stationId = doc.getString("stationId") ?: ""
                    val paymentMethod = doc.getString("paymentMethod") ?: "Wallet"
                    val rrn = doc.getString("rrn") ?: ""
                    val timestamp = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now()

                    com.evcharging.app.data.model.Transaction(
                        id = id,
                        bookingId = bookingId,
                        userId = userId,
                        stationId = stationId,
                        amount = amountDouble,
                        type = type,
                        status = status,
                        rrn = rrn,
                        paymentMethod = paymentMethod,
                        timestamp = timestamp
                    )
                } catch (e: Exception) {
                    android.util.Log.e("WalletDebug", "Error mapping transaction doc ${doc.id}", e)
                    null
                }
            }
            android.util.Log.d("WalletDebug", "Fetched ${transactions.size} explicit transactions for user $userId")

            // 2. Fetch Bookings (for historical data)
            val bookingsSnapshot = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            android.util.Log.d("WalletDebug", "Fetched ${bookingsSnapshot.size()} bookings for user $userId")
                
            val bookingTransactions = bookingsSnapshot.documents.mapNotNull { doc ->
                try {
                    val timestampLong = doc.getLong("timestamp") ?: 0L
                    val amountStr = doc.getString("amount") ?: "0.0"
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val bookingId = doc.id
                    val paymentMethod = doc.getString("paymentMethod") ?: "Wallet" // Default to Wallet
                    val statusStr = doc.getString("status") ?: "Unknown"
                    val status = if (statusStr.contains("Confirm", ignoreCase = true)) "Confirmed" else statusStr
                    
                    // Check if this booking already has a corresponding explicit transaction
                    // We check if any transaction has THIS bookingId
                    if (transactions.any { it.bookingId == bookingId && it.type == "BOOKING" }) {
                        null 
                    } else {
                        com.evcharging.app.data.model.Transaction(
                            id = bookingId,
                            bookingId = bookingId,
                            userId = userId,
                            stationId = doc.getString("stationName") ?: "Unknown",
                            amount = amount,
                            type = "BOOKING",
                            status = status,
                            rrn = "BKG-${timestampLong}",
                            paymentMethod = paymentMethod,
                            timestamp = com.google.firebase.Timestamp(java.util.Date(timestampLong))
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WalletDebug", "Error mapping booking to transaction", e)
                    null
                }
            }

            // 3. Merge and Sort
            val allHistory = (transactions + bookingTransactions)
                .sortedByDescending { it.timestamp.seconds }
            
            android.util.Log.d("WalletDebug", "Returning ${allHistory.size} total history items")

            Result.success(allHistory)
        } catch (e: Exception) {
            android.util.Log.e("WalletDebug", "getChargingHistory failed", e)
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

    suspend fun checkNoShows(stationName: String) {
        try {
            val currentTime = System.currentTimeMillis()
            // Grace period 10 mins (600000ms)
            // If bookingDate + 10 mins < currentTime AND status == "Confirmed", then it's a no-show.
            
            val snapshot = firestore.collection("bookings")
                .whereEqualTo("stationName", stationName)
                .whereEqualTo("status", "Confirmed")
                .get()
                .await()
                
            snapshot.documents.forEach { doc ->
                val bookingDate = doc.getLong("bookingDate") ?: 0L
                if ((bookingDate + 600000) < currentTime) {
                    // It's a no-show! Cancel it.
                    val bookingId = doc.id
                    firestore.collection("bookings").document(bookingId).update("status", "Cancelled_NoShow")
                    
                    // Notify nearby users logic (Stub)
                    notifyNearbyUsers(stationName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyNearbyUsers(stationName: String) {
        // Logic to find users within 25km radius and send FCM notification.
        // Requires geospatial query on "users" collection (if location stored) or "active users".
        android.util.Log.d("Notification", "Sending notification: Spot available at $stationName due to no-show")
    }

    suspend fun getUserBookings(): Result<List<Map<String, Any>>> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val snapshot = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereIn("status", listOf("Confirmed", "Charging"))
                .get()
                .await()
            
            val bookings = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    data.plus("id" to doc.id)
                } else {
                    null
                }
            }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
            
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

