package com.evcharging.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.evcharging.app.ui.profile.ProfileViewModel
import com.evcharging.app.ui.components.GlassCard
import com.evcharging.app.ui.components.NeonButton
import com.evcharging.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val carModels by viewModel.carModels.collectAsState()
    val carColors by viewModel.carColors.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var carColor by remember { mutableStateOf("") }
    
    // Initialize state when profile loads
    LaunchedEffect(userProfile) {
        if (userProfile.isNotEmpty()) {
            name = userProfile["name"] as? String ?: ""
            carModel = userProfile["carModel"] as? String ?: ""
            carColor = userProfile["carColor"] as? String ?: ""
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            scope.launch { 
                snackbarHostState.showSnackbar("Profile Updated Successfully")
                viewModel.resetUpdateSuccess()
            }
        }
    }
    
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            if (isLoading && userProfile.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    GlassCard {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Edit Your Details", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = { 
                                         // Hide keyboard
                                         defaultKeyboardAction(androidx.compose.ui.text.input.ImeAction.Done)
                                    }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Car Model Dropdown
                            var expandedModel by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedModel,
                                onExpandedChange = { expandedModel = !expandedModel },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = carModel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vehicle Model", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedModel,
                                    onDismissRequest = { expandedModel = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    carModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model, color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                carModel = model
                                                expandedModel = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Car Color Dropdown
                            var expandedColor by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedColor,
                                onExpandedChange = { expandedColor = !expandedColor },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = carColor,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vehicle Color", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedColor,
                                    onDismissRequest = { expandedColor = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    carColors.forEach { color ->
                                        DropdownMenuItem(
                                            text = { Text(color, color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                carColor = color
                                                expandedColor = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    NeonButton(
                        text = if (isLoading) "Saving..." else "Save Changes",
                        onClick = { viewModel.updateUserProfile(name, carModel, carColor) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
