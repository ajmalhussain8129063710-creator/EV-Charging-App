package com.evcharging.admin.ui.analytics

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
import java.util.Calendar
import javax.inject.Inject

data class AnalyticsDataPoint(
    val label: String, // e.g., "Mon", "Tue" or Service Name
    val value: Float
)

@HiltViewModel
class ServiceAnalyticsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services: StateFlow<List<ServiceItem>> = _services.asStateFlow()

    private val _selectedService = MutableStateFlow<ServiceItem?>(null) // Null means "All"
    val selectedService: StateFlow<ServiceItem?> = _selectedService.asStateFlow()

    private val _revenueChartData = MutableStateFlow<List<AnalyticsDataPoint>>(emptyList())
    val revenueChartData: StateFlow<List<AnalyticsDataPoint>> = _revenueChartData.asStateFlow()
    
    // Raw request data
    private var allRequests: List<Map<String, Any>> = emptyList()

    private var stationId: String? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val adminDoc = firestore.collection("admins").document(userId).get().await()
                stationId = adminDoc.getString("stationId")

                if (stationId != null) {
                    // Fetch Services
                    val servicesSnapshot = firestore.collection("stations")
                        .document(stationId!!)
                        .collection("services")
                        .get()
                        .await()
                    _services.value = servicesSnapshot.toObjects(ServiceItem::class.java)

                    // Fetch Requests (Mocking real data for now if empty, but fetching real path)
                    val requestsSnapshot = firestore.collection("service_requests")
                        .whereEqualTo("stationId", stationId)
                        .get()
                        .await()
                    
                    allRequests = requestsSnapshot.documents.map { it.data!! }
                    
                    updateCharts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectService(service: ServiceItem?) {
        _selectedService.value = service
        updateCharts()
    }

    private fun updateCharts() {
        // Filter requests locally
        val filteredRequests = if (_selectedService.value == null) {
            allRequests
        } else {
            allRequests.filter { it["serviceId"] == _selectedService.value!!.id }
        }

        // Example Chart: Revenue by Day (Last 7 days)
        // Or if "All" is selected, Revenue by Service
        
        if (_selectedService.value == null) {
            // Aggregate: Revenue by Service
            val revenueByService = filteredRequests.groupBy { it["serviceName"] as? String ?: "Unknown" }
                .mapValues { entry -> 
                    entry.value.sumOf { (it["price"] as? Number)?.toDouble() ?: 0.0 }.toFloat()
                }
            
            _revenueChartData.value = revenueByService.map { AnalyticsDataPoint(it.key, it.value) }
        } else {
            // Specific Service: Revenue over generic time (mock timestamps for demo or use real)
            // Ideally group by date. For simplicity/demo:
            val count = filteredRequests.size.toFloat()
            val revenue = filteredRequests.sumOf { (it["price"] as? Number)?.toDouble() ?: 0.0 }.toFloat()
            
            _revenueChartData.value = listOf(
                AnalyticsDataPoint("Total Requests", count),
                AnalyticsDataPoint("Total Revenue ($)", revenue)
            )
        }
    }
}
