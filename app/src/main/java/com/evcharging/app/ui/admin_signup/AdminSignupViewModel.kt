package com.evcharging.app.ui.admin_signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.model.AdminUser
import com.evcharging.app.data.model.Station
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminSignupViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: com.google.firebase.storage.FirebaseStorage
) : ViewModel() {

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

                // 6. Important: Sign out the new admin user so the main user session can resume (or prompt re-login)
                // However, usually we might want to keep them logged in? 
                // Issue: If they are logged in as Admin in User App, the User App might break if it expects User-specific data.
                // Best practice for this "Cross-Signup": Sign them up, then sign them out immediately so they can log in to the actual Admin App.
                auth.signOut()

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
