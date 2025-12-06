package com.evcharging.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun signUp(
        emailOrPhone: String,
        pass: String,
        name: String,
        carModel: String,
        carColor: String,
        phoneNumber: String,
        vehicleType: String
    ): Result<Boolean> {
        return try {
            // Determine if input is email or phone
            var finalEmail = emailOrPhone
            var finalPhoneNumber = phoneNumber
            
            if (!emailOrPhone.contains("@")) {
                // Assume it's a phone number if no @
                finalPhoneNumber = emailOrPhone
                finalEmail = "${emailOrPhone.replace("[^0-9]".toRegex(), "")}@evapp.com" // Dummy email
            }

            val authResult = auth.createUserWithEmailAndPassword(finalEmail, pass).await()
            val userId = authResult.user?.uid ?: throw Exception("Firebase Authentication failed: User ID is null")
            
            // Auto-assign 3D Model URL
            val car3dModelUrl = com.evcharging.app.util.CarResourceHelper.get3dModelUrl(carModel, carColor)

            val user = com.evcharging.app.data.model.User(
                id = userId,
                name = name,
                email = finalEmail,
                carModel = carModel,
                carColor = carColor,
                car3dModelUrl = car3dModelUrl,
                phoneNumber = finalPhoneNumber,
                walletBalance = 1000.0,
                vehicleType = vehicleType
            )

            firestore.collection("users").document(userId).set(user).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Sign Up Failed: ${e.message}", e))
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun logout() {
        auth.signOut()
    }

    suspend fun getUserProfile(userId: String): Result<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            if (snapshot.exists()) {
                Result.success(snapshot.data ?: emptyMap())
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updatePoints(userId: String, newPoints: Int): Result<Boolean> {
        return try {
            firestore.collection("users").document(userId).update("points", newPoints).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWalletBalance(userId: String): Result<Double> {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val balance = snapshot.getDouble("walletBalance") ?: 0.0
            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deductWalletBalance(userId: String, amount: Double): Result<Boolean> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0
                if (currentBalance >= amount) {
                    transaction.update(userRef, "walletBalance", currentBalance - amount)
                } else {
                    throw Exception("Insufficient balance")
                }
            }.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWalletBalance(userId: String, amount: Double): Result<Boolean> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getDouble("walletBalance") ?: 0.0
                transaction.update(userRef, "walletBalance", currentBalance + amount)
            }.await()
            
            // Record Transaction
            val transactionId = java.util.UUID.randomUUID().toString()
            val transaction = hashMapOf(
                "id" to transactionId,
                "userId" to userId,
                "amount" to amount,
                "type" to "TOPUP",
                "status" to "COMPLETED",
                "timestamp" to com.google.firebase.Timestamp.now(),
                "rrn" to "TOPUP-${System.currentTimeMillis()}" 
            )
            firestore.collection("transactions").document(transactionId).set(transaction).await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updateUserProfile(userId: String, name: String, carModel: String, carColor: String): Result<Boolean> {
        return try {
            val updates = mapOf(
                "name" to name,
                "carModel" to carModel,
                "carColor" to carColor,
                // Update 3D model URL if car details change
                "car3dModelUrl" to com.evcharging.app.util.CarResourceHelper.get3dModelUrl(carModel, carColor)
            )
            firestore.collection("users").document(userId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
