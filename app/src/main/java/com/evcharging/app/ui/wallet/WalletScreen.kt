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

    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var amountToAdd by remember { mutableStateOf("") }
    
    // Razorpay Integration
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.paymentEvent.collect { event ->
            when (event) {
                is WalletPaymentEvent.StartPayment -> {
                    val activity = context.findActivity()
                    if (activity != null) {
                        val apiKey = com.evcharging.app.BuildConfig.RAZORPAY_API_KEY
                        // Debug logging
                        android.util.Log.d("RazorpayDebug", "Using API Key: $apiKey")

                        if (apiKey.isNullOrEmpty() || apiKey.contains("REPLACE_ME") || apiKey.contains("YOUR_KEY")) {
                             android.widget.Toast.makeText(context, "Invalid Razorpay Key! Please check local.properties", android.widget.Toast.LENGTH_LONG).show()
                             return@collect
                        }

                        val checkout = com.razorpay.Checkout()
                        checkout.setKeyID(apiKey)
                        try {
                            val options = org.json.JSONObject()
                            options.put("name", "EV Charging App")
                            options.put("description", "Wallet Recharge")
                            options.put("currency", "INR")
                            options.put("amount", event.amountInPaise)
                            options.put("prefill.email", "user@example.com")
                            options.put("prefill.contact", "9876543210")
                            checkout.open(activity, options)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.util.Log.e("RazorpayDebug", "Error starting payment: ${e.message}")
                            android.widget.Toast.makeText(context, "Error starting payment: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                         android.widget.Toast.makeText(context, "Unable to find Activity context", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (showAddMoneyDialog) {
        AlertDialog(
            onDismissRequest = { showAddMoneyDialog = false },
            title = { Text("Add Money to Wallet", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Enter Amount (₹)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountToAdd,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountToAdd = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                             focusedBorderColor = MaterialTheme.colorScheme.primary,
                             unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                             focusedTextColor = MaterialTheme.colorScheme.onSurface,
                             unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                com.evcharging.app.ui.components.NeonButton(
                    text = "Add Money",
                    onClick = {
                        val amount = amountToAdd.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.initiateRecharge(amount)
                            showAddMoneyDialog = false
                            amountToAdd = ""
                        }
                    },
                    color = MaterialTheme.colorScheme.primary
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddMoneyDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Wallet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
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
                Text("Current Balance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${String.format("%.2f", balance)}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Spacer(modifier = Modifier.height(16.dp))
                com.evcharging.app.ui.components.NeonButton(
                    text = "+ Add Money",
                    onClick = { showAddMoneyDialog = true },
                    modifier = Modifier.width(150.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        
        var selectedTransaction by remember { mutableStateOf<com.evcharging.app.data.model.Transaction?>(null) }
    
        if (transactions.isEmpty()) {
            Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions.size) { index ->
                    val transaction = transactions[index]
                    val isCredit = transaction.type == "REFUND" || transaction.type == "TOPUP"
                    val sign = if (isCredit) "+" else "-"
                    val amountColor = if (isCredit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error // NeonGreen/Red
                    val title = if (isCredit) "Credit Amount" else "Debit Amount"
                    
                    val subtitle = when(transaction.type) {
                        "BOOKING" -> "Paid via ${transaction.paymentMethod}"
                        "REFUND" -> "Refund Processed"
                        "TOPUP" -> "Wallet Top Up"
                        else -> "Transaction"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTransaction = transaction },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // CardBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(transaction.timestamp.toDate().toString().take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$sign₹${transaction.amount}", style = MaterialTheme.typography.bodyLarge, color = amountColor, fontWeight = FontWeight.Bold)
                                Text(if(transaction.status == "COMPLETED") "Confirmed" else transaction.status, style = MaterialTheme.typography.labelSmall, color = if(transaction.status == "COMPLETED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
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
                title = { Text("Transaction Details", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        DetailRow("Transaction ID", selectedTransaction!!.id)
                        DetailRow("Date", selectedTransaction!!.timestamp.toDate().toString())
                        DetailRow("Type", selectedTransaction!!.type)
                        DetailRow("Status", selectedTransaction!!.status)
                        DetailRow("Amount", "₹${selectedTransaction!!.amount}")
                        DetailRow("Payment Method", selectedTransaction!!.paymentMethod)

                        if (selectedTransaction!!.stationId.isNotEmpty()) {
                            DetailRow("Station ID", selectedTransaction!!.stationId)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTransaction = null }) {
                        Text("Close", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
