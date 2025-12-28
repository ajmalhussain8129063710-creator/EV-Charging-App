package com.evcharging.admin.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.model.ServiceItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services: StateFlow<List<ServiceItem>> = _services.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentStationId: String? = null

    init {
        loadStationAndServices()
    }

    private fun loadStationAndServices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminDoc = firestore.collection("admins").document(userId).get().await()
                currentStationId = adminDoc.getString("stationId")
                
                if (currentStationId != null) {
                    fetchServices()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchServices() {
        if (currentStationId == null) return
        try {
            val snapshot = firestore.collection("stations")
                .document(currentStationId!!)
                .collection("services")
                .get()
                .await()
            val list = snapshot.toObjects(ServiceItem::class.java)
            _services.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addService(name: String, description: String, price: Double) {
        if (currentStationId == null) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = UUID.randomUUID().toString()
                val service = ServiceItem(
                    id = id,
                    stationId = currentStationId!!,
                    name = name,
                    description = description,
                    price = price,
                    enabled = true
                )
                
                firestore.collection("stations")
                    .document(currentStationId!!)
                    .collection("services")
                    .document(id)
                    .set(service)
                    .await()
                    
                fetchServices()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
