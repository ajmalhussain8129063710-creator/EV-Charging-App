package com.evcharging.admin.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.admin.model.Transaction
import com.evcharging.admin.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWalletScreen(
    navController: NavController,
    viewModel: AdminWalletViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val pendingAmount by viewModel.pendingAmount.collectAsState()
    val accountAmount by viewModel.accountAmount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Wallet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WalletCard(
                    title = "Total Credit (In)",
                    amount = accountAmount, // Using accountAmount as Credit for now
                    color = Color(0xFF66BB6A), // Green
                    modifier = Modifier.weight(1f)
                )
                WalletCard(
                    title = "Total Debit (Out)",
                    amount = pendingAmount, // Using pendingAmount as Debit for now (or derive correctly)
                    color = Color(0xFFEF5350), // Red
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Admin Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Ledger Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Source", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("DR", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF5350))
                Text("CR", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFF66BB6A))
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(transactions) { transaction ->
                    LedgerRow(transaction)
                }
            }
        }
    }
}

@Composable
fun LedgerRow(transaction: Transaction) {
    val isCredit = transaction.type == com.evcharging.admin.model.TransactionType.TOPUP || transaction.type == com.evcharging.admin.model.TransactionType.BOOKING
    val isDebit = transaction.type == com.evcharging.admin.model.TransactionType.REFUND
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Text(
                text = SimpleDateFormat("MMM dd\nHH:mm", Locale.getDefault()).format(transaction.timestamp.toDate()),
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            // Source
            val sourceText = when(transaction.type) {
                com.evcharging.admin.model.TransactionType.TOPUP -> "User Recharge"
                com.evcharging.admin.model.TransactionType.BOOKING -> "Booking"
                com.evcharging.admin.model.TransactionType.REFUND -> "Refund"
                else -> "Other"
            }
            Text(
                text = sourceText,
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            // DR
            Text(
                text = if (isDebit) "₹${String.format("%.0f", transaction.amount)}" else "-",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isDebit) Color(0xFFEF5350) else Color.Gray.copy(alpha = 0.5f)
            )
            
            // CR
            Text(
                text = if (isCredit) "₹${String.format("%.0f", transaction.amount)}" else "-",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) Color(0xFF66BB6A) else Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun WalletCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text("₹${String.format("%.2f", amount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
