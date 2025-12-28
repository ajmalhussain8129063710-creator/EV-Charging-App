package com.evcharging.admin.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    navController: NavController,
    viewModel: AdminSettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            Text("Common Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            ListItem(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text("Manage app notifications") },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = { Text("Toggle dark theme") },
                trailingContent = { 
                    Switch(
                        checked = isDarkTheme, 
                        onCheckedChange = { viewModel.toggleTheme(it) }
                    ) 
                }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Language") },
                supportingContent = { Text("English") }
            )
        }
    }
}
