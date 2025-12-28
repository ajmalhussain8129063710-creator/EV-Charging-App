package com.evcharging.app.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import com.evcharging.app.ui.components.GlassCard


@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val balance by viewModel.balance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    // Refresh on entry
    LaunchedEffect(Unit) {
        viewModel.fetchWalletData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Wallet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Current Balance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("$${String.format("%.2f", balance)}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        var selectedTransaction by remember { mutableStateOf<com.evcharging.app.data.model.Transaction?>(null) }
    
        if (transactions.isEmpty()) {
            Text("No transactions yet.", color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions.size) { index ->
                    val transaction = transactions[index]
                    val isCredit = transaction.type.name == "REFUND" || transaction.type.name == "TOPUP"
                    val sign = if (isCredit) "+" else "-"
                    val amountColor = if (isCredit) com.evcharging.app.ui.theme.NeonGreen else com.evcharging.app.ui.theme.NeonRed
                    val title = if (isCredit) "Credit Amount" else "Debit Amount"
                    
                    val subtitle = when(transaction.type.name) {
                        "BOOKING" -> "Paid via ${transaction.paymentMethod}"
                        "REFUND" -> "Refund Processed"
                        "TOPUP" -> "Wallet Top Up"
                        else -> "Transaction"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTransaction = transaction },
                        colors = CardDefaults.cardColors(containerColor = com.evcharging.app.ui.theme.CardBackground)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = com.evcharging.app.ui.theme.TextPrimary)
                                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = com.evcharging.app.ui.theme.TextSecondary)
                                Text(transaction.timestamp.toDate().toString().take(16), style = MaterialTheme.typography.bodySmall, color = com.evcharging.app.ui.theme.TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$sign$${transaction.amount}", style = MaterialTheme.typography.bodyLarge, color = amountColor, fontWeight = FontWeight.Bold)
                                Text(if(transaction.status.name == "COMPLETED") "Confirmed" else transaction.status.name, style = MaterialTheme.typography.labelSmall, color = if(transaction.status.name == "COMPLETED") com.evcharging.app.ui.theme.NeonCyan else com.evcharging.app.ui.theme.NeonRed)
                            }
                        }
                    }
                }
            }
        }

        // Transaction Details Dialog
        if (selectedTransaction != null) {
            AlertDialog(
                onDismissRequest = { selectedTransaction = null },
                title = { Text("Transaction Details", color = com.evcharging.app.ui.theme.TextPrimary) },
                text = {
                    Column {
                        DetailRow("Transaction ID", selectedTransaction!!.id)
                        DetailRow("Date", selectedTransaction!!.timestamp.toDate().toString())
                        DetailRow("Type", selectedTransaction!!.type.name)
                        DetailRow("Status", selectedTransaction!!.status.name)
                        DetailRow("Amount", "$${selectedTransaction!!.amount}")
                        DetailRow("Payment Method", selectedTransaction!!.paymentMethod)

                        if (selectedTransaction!!.stationId.isNotEmpty()) {
                            DetailRow("Station ID", selectedTransaction!!.stationId)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTransaction = null }) {
                        Text("Close", color = com.evcharging.app.ui.theme.NeonCyan)
                    }
                },
                containerColor = com.evcharging.app.ui.theme.DeepBackground,
                titleContentColor = com.evcharging.app.ui.theme.TextPrimary,
                textContentColor = com.evcharging.app.ui.theme.TextSecondary
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = com.evcharging.app.ui.theme.TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = com.evcharging.app.ui.theme.TextPrimary)
    }
}
