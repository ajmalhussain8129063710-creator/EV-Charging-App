package com.evcharging.admin.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.data.PlacesRepository
import com.evcharging.admin.data.PlacePrediction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _adminName = MutableStateFlow("")
    val adminName: StateFlow<String> = _adminName.asStateFlow()

    private val _stationName = MutableStateFlow("")
    val stationName: StateFlow<String> = _stationName.asStateFlow()

    private val _stationAddress = MutableStateFlow("")
    val stationAddress: StateFlow<String> = _stationAddress.asStateFlow()
    
    private val _stationImageUrl = MutableStateFlow("")
    val stationImageUrl: StateFlow<String> = _stationImageUrl.asStateFlow()

    private val _stationVideoUrl = MutableStateFlow("")
    val stationVideoUrl: StateFlow<String> = _stationVideoUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<PlacePrediction>>(emptyList())
    val locationSuggestions: StateFlow<List<PlacePrediction>> = _locationSuggestions.asStateFlow()

    init {
        fetchProfile()
    }

    fun searchLocation(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                _locationSuggestions.value = placesRepository.searchPlaces(query)
            } else {
                _locationSuggestions.value = emptyList()
            }
        }
    }

    fun clearSuggestions() {
        _locationSuggestions.value = emptyList()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                // Assuming admin data is in 'admins' collection or 'users'
                // Based on previous context, let's assume 'users' or a specific 'stations' collection linked to admin
                // For this implementation, I'll assume the admin manages a station stored in 'stations' where 'adminId' == userId
                // OR the admin document itself contains station info.
                
                // Let's try to find the station managed by this admin
                val stationSnapshot = firestore.collection("stations")
                    .whereEqualTo("adminId", userId)
                    .limit(1)
                    .get()
                    .await()

                if (!stationSnapshot.isEmpty) {
                    val station = stationSnapshot.documents[0]
                    _stationName.value = station.getString("name") ?: ""
                    _stationAddress.value = station.getString("address") ?: ""
                    _stationImageUrl.value = station.getString("imageUrl") ?: ""
                    _stationVideoUrl.value = station.getString("videoUrl") ?: ""
                }
                
                // Also get admin name
                val userSnapshot = firestore.collection("admins").document(userId).get().await()
                _adminName.value = userSnapshot.getString("name") ?: ""

            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateProfile(name: String, address: String, imageUri: android.net.Uri?, videoUri: android.net.Uri?, currentImageUrl: String, currentVideoUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Update Admin Name
                firestore.collection("admins").document(userId).update("name", name).await()
                _adminName.value = name

                // Upload Image if changed
                var finalImageUrl = currentImageUrl
                if (imageUri != null) {
                    finalImageUrl = uploadFile(imageUri, "station_images/$userId")
                }

                // Upload Video if changed
                var finalVideoUrl = currentVideoUrl
                if (videoUri != null) {
                    finalVideoUrl = uploadFile(videoUri, "station_videos/$userId")
                }

                // Update Station Details
                val stationSnapshot = firestore.collection("stations")
                    .whereEqualTo("adminId", userId)
                    .limit(1)
                    .get()
                    .await()

                if (!stationSnapshot.isEmpty) {
                    val stationDoc = stationSnapshot.documents[0]
                    stationDoc.reference.update(
                        mapOf(
                            "address" to address,
                            "imageUrl" to finalImageUrl,
                            "videoUrl" to finalVideoUrl
                        )
                    ).await()
                    
                    _stationAddress.value = address
                    _stationImageUrl.value = finalImageUrl
                    _stationVideoUrl.value = finalVideoUrl
                }
                
                _updateSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Update failed: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadFile(uri: android.net.Uri, path: String): String {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("$path/${java.util.UUID.randomUUID()}")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }
    
    fun resetSuccess() {
        _updateSuccess.value = false
    }
}
