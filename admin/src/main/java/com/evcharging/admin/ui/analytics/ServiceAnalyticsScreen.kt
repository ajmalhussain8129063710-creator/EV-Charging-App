package com.evcharging.admin.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.admin.model.ServiceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceAnalyticsScreen(
    navController: NavController,
    viewModel: ServiceAnalyticsViewModel = hiltViewModel()
) {
    val services by viewModel.services.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val chartData by viewModel.revenueChartData.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Analytics") },
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
            // Filter Dropdown
            Box {
                OutlinedTextField(
                    value = selectedService?.name ?: "All Services",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter Analysis") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDropdown = true }
                )
                // Overlay for click capture if TextField consumes it
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDropdown = true }
                )
                
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Services") },
                        onClick = {
                            viewModel.selectService(null)
                            showDropdown = false
                        }
                    )
                    services.forEach { service ->
                        DropdownMenuItem(
                            text = { Text(service.name) },
                            onClick = {
                                viewModel.selectService(service)
                                showDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Performance Overview",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    if (chartData.isEmpty()) {
                        Text(
                            "No data available to analyze.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SimpleBarChart(data = chartData)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Text Summary
            Text(
                text = if (selectedService == null) 
                    "Comparing revenue across all active services." 
                else 
                    "Analyzing metrics for ${selectedService!!.name}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SimpleBarChart(data: List<AnalyticsDataPoint>) {
    if (data.isEmpty()) return
    val maxValue = data.maxOf { it.value }
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / (data.size * 2f)
        val spacing = size.width / (data.size * 2f)
        val heightMultiplier = if (maxValue > 0) size.height / maxValue else 0f

        data.forEachIndexed { index, point ->
            val x = index * (barWidth + spacing) + spacing / 2
            val barHeight = point.value * heightMultiplier
            
            // Draw Bar
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
            
            // Draw Label (Simplified, just a circle placeholder for text if too complex)
            // In a real app we use drawContext.canvas.nativeCanvas.drawText
        }
        
        // Axis Line
        drawLine(
            color = Color.Gray,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f
        )
    }
    
    // Legend/Labels below
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        data.forEach { 
             Text(
                 text = it.label.take(3), 
                 style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurface
             )
        }
    }
}
