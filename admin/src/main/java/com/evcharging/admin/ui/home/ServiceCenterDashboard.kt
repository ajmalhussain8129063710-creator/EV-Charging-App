package com.evcharging.admin.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun ServiceCenterDashboard(
    navController: androidx.navigation.NavController,
    viewModel: ServiceCenterDashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val pendingCount by viewModel.pendingRequestsCount.collectAsState()
    val activeServicesCount by viewModel.activeServicesCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Service Center Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ServiceStatCard(
            title = "Active Services", 
            value = "$activeServicesCount", 
            icon = Icons.Default.Build,
            onClick = { navController.navigate(com.evcharging.admin.ui.navigation.AdminScreen.ServiceAnalytics.route) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ServiceStatCard("Pending Requests", "$pendingCount", Icons.Default.PendingActions)
        Spacer(modifier = Modifier.height(16.dp))
        ServiceStatCard("Technicians Available", "3", Icons.Default.Person) // Mock data for now
    }
}

@Composable
fun ServiceStatCard(
    title: String, 
    value: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = value, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
