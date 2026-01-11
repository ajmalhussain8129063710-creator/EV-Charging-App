package com.evcharging.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.components.SearchableDropdown


@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var carColor by remember { mutableStateOf("") }
    
    val authState by viewModel.authState.collectAsState()
    val carModels by viewModel.vehicleModels.collectAsState()
    val carColors by viewModel.vehicleColors.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate("login") {
                popUpTo("signup") { inclusive = true }
            }
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background),
                    startY = 0f,
                    endY = 1000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Join the Future",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("User Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email or Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var phoneNumber by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Alternative Phone Number (Optional)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Vehicle Type Selection
                    Text("Select Vehicle Type", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val selectedType by viewModel.vehicleType.collectAsState()
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedType == "Car",
                                onClick = { 
                                    viewModel.setVehicleType("Car")
                                    carModel = ""
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text("Car", color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedType == "Bike",
                                onClick = { 
                                    viewModel.setVehicleType("Bike")
                                    carModel = ""
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text("Bike", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SearchableDropdown(
                        label = "Vehicle Model",
                        items = carModels,
                        selectedItem = carModel,
                        onItemSelected = { carModel = it },
                        onValueChange = { carModel = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SearchableDropdown(
                        label = "Vehicle Color",
                        items = carColors,
                        selectedItem = carColor,
                        onItemSelected = { carColor = it },
                        onValueChange = { carColor = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    NeonButton(
                        text = if (authState is AuthState.Loading) "Signing Up..." else "CREATE ACCOUNT",
                        onClick = { 
                            val type = viewModel.vehicleType.value
                            viewModel.signUp(email, password, name, carModel, carColor, phoneNumber, type) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { navController.navigate("login") }) {
                        Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary)
                    }
                    
                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
