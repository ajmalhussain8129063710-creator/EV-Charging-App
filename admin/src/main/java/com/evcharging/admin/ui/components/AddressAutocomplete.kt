package com.evcharging.admin.ui.components

import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    onAddressSelected: (String, Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { query ->
                onValueChange(query)
                expanded = true
                
                // Debounce search
                searchJob?.cancel()
                searchJob = scope.launch(Dispatchers.IO) {
                    delay(500) // 500ms debounce
                    if (query.length > 2) {
                        try {
                            val geocoder = Geocoder(context)
                            @Suppress("DEPRECATION")
                            val results = geocoder.getFromLocationName(query, 5)
                            withContext(Dispatchers.Main) {
                                suggestions = results ?: emptyList()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                suggestions = emptyList()
                            }
                        }
                    }
                }
            },
            label = label,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (suggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                suggestions.forEach { address ->
                    val addressLine = address.getAddressLine(0) ?: "${address.featureName}, ${address.adminArea}"
                    DropdownMenuItem(
                        text = { Text(addressLine) },
                        onClick = {
                            onValueChange(addressLine)
                            onAddressSelected(addressLine, address.latitude, address.longitude)
                            expanded = false
                            suggestions = emptyList()
                        }
                    )
                }
            }
        }
    }
}
