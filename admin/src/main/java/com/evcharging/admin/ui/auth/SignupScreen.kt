package com.evcharging.admin.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.admin.model.Station
import com.evcharging.admin.ui.navigation.AdminScreen
import com.evcharging.admin.ui.theme.GradientEnd
import com.evcharging.admin.ui.theme.GradientStart
import com.evcharging.admin.ui.theme.GradientEnd
import com.evcharging.admin.ui.theme.GradientStart
import com.evcharging.admin.ui.components.AddressAutocomplete
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Admin Details
    var adminName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    // Station Details
    var selectedType by remember { mutableStateOf("Charging Station") }

    // Station Details
    var stationName by remember { mutableStateOf("") }
    var stationLocation by remember { mutableStateOf("") }
    var stationImageUri by remember { mutableStateOf<Uri?>(null) }
    var stationVideoUri by remember { mutableStateOf<Uri?>(null) }
    var stationDescription by remember { mutableStateOf("") }
    var stationLatitude by remember { mutableStateOf("") }
    var stationLongitude by remember { mutableStateOf("") }
    var pricePerKw by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Section 1: Admin Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Admin Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = adminName, onValueChange = { adminName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    

                    OutlinedTextField(
                        value = phoneNumber, onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Station Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Station Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    val stationNameLabel = when (selectedType) {
                        "Dining" -> "Restaurant Name"
                        "Service Center" -> "Service Center Name"
                        else -> "Station Name"
                    }
                    OutlinedTextField(
                        value = stationName, onValueChange = { stationName = it },
                        label = { Text(stationNameLabel) },
                        leadingIcon = { Icon(Icons.Default.EvStation, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Facility Type Dropdown
                    var expanded by remember { mutableStateOf(false) }

                    val types = listOf("Charging Station", "Dining", "Service Center")

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Facility Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    AddressAutocomplete(
                        value = stationLocation,
                        onValueChange = { stationLocation = it },
                        onAddressSelected = { address, lat, lng ->
                            stationLocation = address
                            stationLatitude = lat.toString()
                            stationLongitude = lng.toString()
                        },
                        label = { Text("Location (Search Address)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
    
                    // Image Picker
                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri -> stationImageUri = uri }

                    val imageLabel = if (selectedType == "Charging Station") "Station Image" else "Gallery Image"
                    
                    OutlinedTextField(
                        value = stationImageUri?.lastPathSegment ?: "No file selected",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(imageLabel) },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { 
                                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Select Image")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { 
                             imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = false, // Read only, click handles it
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Video Picker
                    val videoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri -> stationVideoUri = uri }

                    val videoLabel = if (selectedType == "Charging Station") "Station Video (Optional)" else "Gallery Video (Optional)"
                    
                    OutlinedTextField(
                        value = stationVideoUri?.lastPathSegment ?: "No file selected",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(videoLabel) },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                        trailingIcon = {
                             IconButton(onClick = { 
                                videoPickerLauncher.launch("video/*")
                            }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Select Video")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { 
                            videoPickerLauncher.launch("video/*")
                        },
                        enabled = false,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stationDescription, onValueChange = { stationDescription = it },
                        label = { Text("Description") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (selectedType == "Charging Station") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pricePerKw, onValueChange = { pricePerKw = it },
                            label = { Text("Price per kW") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    // Trigger signup
                                     isLoading = true
                                    val station = Station(
                                        name = stationName,
                                        address = stationLocation,
                                        imageUrl = "", 
                                        videoUrl = "", 
                                        description = stationDescription,
                                        latitude = stationLatitude.toDoubleOrNull() ?: 0.0,
                                        longitude = stationLongitude.toDoubleOrNull() ?: 0.0,
                                        pricePerKw = pricePerKw.toDoubleOrNull() ?: 0.0,
                                        type = selectedType
                                    )
                                    viewModel.signup(email, password, adminName, phoneNumber, station, stationImageUri, stationVideoUri) { success, error ->
                                        isLoading = false
                                        if (success) {
                                            navController.navigate(AdminScreen.Home.route) {
                                                popUpTo(AdminScreen.Login.route) { inclusive = true }
                                            }
                                        } else {
                                            errorMessage = error
                                        }
                                    }
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    val station = Station(
                        name = stationName,
                        address = stationLocation,
                        imageUrl = "", // Will be updated in ViewModel
                        videoUrl = "", // Will be updated in ViewModel
                        description = stationDescription,
                        latitude = stationLatitude.toDoubleOrNull() ?: 0.0,
                        longitude = stationLongitude.toDoubleOrNull() ?: 0.0,
                        pricePerKw = pricePerKw.toDoubleOrNull() ?: 0.0,

                        type = selectedType
                    )
                    viewModel.signup(email, password, adminName, phoneNumber, station, stationImageUri, stationVideoUri) { success, error ->
                        isLoading = false
                        if (success) {
                            navController.navigate(AdminScreen.Home.route) {
                                popUpTo(AdminScreen.Login.route) { inclusive = true }
                            }
                        } else {
                            errorMessage = error
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd)),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("REGISTER & CREATE STATION", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
