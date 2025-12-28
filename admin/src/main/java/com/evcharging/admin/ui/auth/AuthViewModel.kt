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
    private val firestore: FirebaseFirestore,
    private val storage: com.google.firebase.storage.FirebaseStorage
) : ViewModel() {

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    val adminDoc = firestore.collection("admins").document(user.uid).get().await()
                    if (adminDoc.exists()) {
                        onResult(true, null)
                    } else {
                        auth.signOut()
                        onResult(false, "Access Denied: Not an Admin account.")
                    }
                } else {
                    onResult(false, "Login failed.")
                }
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                onResult(false, "Incorrect email or password.")
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                onResult(false, "No account found with this email.")
            } catch (e: Exception) {
                onResult(false, "Login failed: ${e.localizedMessage}")
            }
        }
    }

    fun signup(email: String, pass: String, name: String, phoneNumber: String, station: Station, imageUri: android.net.Uri?, videoUri: android.net.Uri?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Create Auth User
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val userId = authResult.user?.uid ?: throw Exception("User creation failed")

                // 2. Create Station Record ID
                val stationRef = firestore.collection("stations").document()
                val stationId = stationRef.id

                // 3. Upload Files
                var imageUrl = ""
                if (imageUri != null) {
                    imageUrl = uploadFile(imageUri, "station_images/$stationId/image_${System.currentTimeMillis()}")
                }

                var videoUrl = ""
                if (videoUri != null) {
                    videoUrl = uploadFile(videoUri, "station_videos/$stationId/video_${System.currentTimeMillis()}")
                }

                // 4. Save Station with URLs
                val newStation = station.copy(
                    id = stationId, 
                    adminId = userId,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl
                )
                stationRef.set(newStation).await()

                // 5. Create Admin User Record
                val adminUser = AdminUser(
                    id = userId,
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber,
                    stationId = stationId
                )
                firestore.collection("admins").document(userId).set(adminUser).await()

                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    private suspend fun uploadFile(uri: android.net.Uri, path: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
