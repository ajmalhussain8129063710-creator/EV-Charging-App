package com.evcharging.admin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.model.AdminUser
import com.evcharging.admin.model.Dining
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
class DiningViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _diningList = MutableStateFlow<List<Dining>>(emptyList())
    val diningList: StateFlow<List<Dining>> = _diningList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchDining()
    }

    private fun fetchDining() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminSnapshot = firestore.collection("admins").document(userId).get().await()
                val stationId = adminSnapshot.getString("stationId") ?: return@launch

                val snapshot = firestore.collection("stations").document(stationId)
                    .collection("dining").get().await()
                
                val items = snapshot.toObjects(Dining::class.java)
                _diningList.value = items
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addDiningItem(name: String, description: String, price: Double, imageUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminSnapshot = firestore.collection("admins").document(userId).get().await()
                val stationId = adminSnapshot.getString("stationId") ?: return@launch

                val ref = firestore.collection("stations").document(stationId).collection("dining").document()
                val newItem = Dining(
                    id = ref.id,
                    name = name,
                    description = description,
                    price = price,
                    imageUrl = imageUrl,
                    stationId = stationId
                )
                ref.set(newItem).await()
                fetchDining()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
