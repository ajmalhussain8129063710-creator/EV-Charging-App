package com.evcharging.admin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.model.Promotion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.evcharging.admin.model.Reward

@HiltViewModel
class PromotionsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())
    val promotions: StateFlow<List<Promotion>> = _promotions.asStateFlow()

    private val _rewards = MutableStateFlow<List<Reward>>(emptyList())
    val rewards: StateFlow<List<Reward>> = _rewards.asStateFlow()

    private val _pointsPerKw = MutableStateFlow(0.001)
    val pointsPerKw: StateFlow<Double> = _pointsPerKw.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminSnapshot = firestore.collection("admins").document(userId).get().await()
                val stationId = adminSnapshot.getString("stationId") ?: return@launch

                // Fetch Station for Rate
                val stationSnapshot = firestore.collection("stations").document(stationId).get().await()
                _pointsPerKw.value = stationSnapshot.getDouble("pointsPerKw") ?: 0.001

                // Fetch Promotions
                val promoSnapshot = firestore.collection("stations").document(stationId)
                    .collection("promotions").get().await()
                _promotions.value = promoSnapshot.toObjects(Promotion::class.java)

                // Fetch Rewards
                val rewardSnapshot = firestore.collection("stations").document(stationId)
                    .collection("rewards").get().await()
                _rewards.value = rewardSnapshot.toObjects(Reward::class.java)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEarningRate(rate: Double) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminSnapshot = firestore.collection("admins").document(userId).get().await()
                val stationId = adminSnapshot.getString("stationId") ?: return@launch

                firestore.collection("stations").document(stationId)
                    .update("pointsPerKw", rate).await()
                _pointsPerKw.value = rate
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addPromotion(title: String, description: String, discount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminSnapshot = firestore.collection("admins").document(userId).get().await()
                val stationId = adminSnapshot.getString("stationId") ?: return@launch

                val ref = firestore.collection("stations").document(stationId).collection("promotions").document()
                val newItem = Promotion(
                    id = ref.id,
                    title = title,
                    description = description,
                    discountPercentage = discount,
                    expiryDate = System.currentTimeMillis() + 86400000 * 7, // 7 days default
                    stationId = stationId
                )
                ref.set(newItem).await()
                fetchData() // Refresh all
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addReward(title: String, description: String, cost: Int, value: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminSnapshot = firestore.collection("admins").document(userId).get().await()
                val stationId = adminSnapshot.getString("stationId") ?: return@launch

                val ref = firestore.collection("stations").document(stationId).collection("rewards").document()
                val newItem = Reward(
                    id = ref.id,
                    title = title,
                    description = description,
                    pointsCost = cost,
                    value = value,
                    stationId = stationId
                )
                ref.set(newItem).await()
                fetchData() // Refresh all
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
