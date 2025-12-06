package com.evcharging.admin.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.model.SupportTicket // Need to duplicate model in admin or share
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
class AdminSupportViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _tickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val tickets: StateFlow<List<SupportTicket>> = _tickets.asStateFlow()

    init {
        fetchTickets()
    }

    fun fetchTickets() {
        viewModelScope.launch {
            try {
                val adminId = auth.currentUser?.uid ?: return@launch
                val snapshot = firestore.collection("support_tickets")
                    .whereEqualTo("adminId", adminId)
                    .get()
                    .await()
                
                val ticketList = snapshot.toObjects(SupportTicket::class.java)
                _tickets.value = ticketList
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
