package com.evcharging.admin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.model.AdminUser
import com.evcharging.admin.model.Station
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun signup(email: String, pass: String, name: String, station: Station, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Create Auth User
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val userId = authResult.user?.uid ?: throw Exception("User creation failed")

                // 2. Create Station Record
                val stationRef = firestore.collection("stations").document()
                val stationId = stationRef.id
                val newStation = station.copy(id = stationId, adminId = userId)
                stationRef.set(newStation).await()

                // 3. Create Admin User Record
                val adminUser = AdminUser(
                    id = userId,
                    name = name,
                    email = email,
                    stationId = stationId
                )
                firestore.collection("admins").document(userId).set(adminUser).await()

                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}
