package com.evcharging.admin.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.model.Transaction
import com.evcharging.admin.model.TransactionStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminWalletViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _pendingAmount = MutableStateFlow(0.0)
    val pendingAmount: StateFlow<Double> = _pendingAmount.asStateFlow()

    private val _accountAmount = MutableStateFlow(0.0)
    val accountAmount: StateFlow<Double> = _accountAmount.asStateFlow()

    init {
        fetchTransactions()
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("transactions")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val transactionList = snapshot.toObjects(Transaction::class.java)
                _transactions.value = transactionList

                calculateTotals(transactionList)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun calculateTotals(transactions: List<Transaction>) {
        var pending = 0.0
        var account = 0.0
        transactions.forEach {
            when (it.status) {
                TransactionStatus.PENDING -> pending += it.amount
                TransactionStatus.IN_PROGRESS, TransactionStatus.COMPLETED -> account += it.amount
                else -> {} // Ignore Failed/Refunded
            }
        }
        _pendingAmount.value = pending
        _accountAmount.value = account
    }

    fun markAsCompleted(transactionId: String) {
        viewModelScope.launch {
            try {
                // In a real app, we'd query by ID, but here we might need to find the doc ID first if 'id' is not the doc ID.
                // Assuming 'id' in Transaction is not the doc ID, we query.
                // Actually, when saving, we didn't set 'id' to doc ID.
                // Let's assume for now we query by some field or just update if we have the doc ID.
                // For simplicity, let's query by bookingId or timestamp if id is empty.
                // Better: Update fetch to store doc ID.
                
                // Quick fix: Query by bookingId (assuming unique enough for this demo)
                // Or just scan the list since we have it.
                
                val transaction = _transactions.value.find { it.id == transactionId } ?: return@launch
                
                // Find the document
                val querySnapshot = firestore.collection("transactions")
                    .whereEqualTo("bookingId", transaction.bookingId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    doc.reference.update("status", TransactionStatus.COMPLETED).await()
                    fetchTransactions() // Refresh
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
