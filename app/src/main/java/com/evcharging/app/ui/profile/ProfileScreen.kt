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
                title = { Text("Edit Profile", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBackground,
                    scrolledContainerColor = DeepBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1525), DeepBackground),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            if (isLoading && userProfile.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
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
                            .background(GlassSurface, androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, NeonCyan, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(60.dp),
                            tint = NeonCyan
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    GlassCard {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Edit Your Details", style = MaterialTheme.typography.headlineSmall, color = NeonPurple)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassWhite,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = NeonCyan
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
                                    label = { Text("Vehicle Model", color = TextSecondary) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = GlassWhite,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = NeonCyan
                                    ),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedModel,
                                    onDismissRequest = { expandedModel = false },
                                    modifier = Modifier.background(CardBackground)
                                ) {
                                    carModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model, color = TextPrimary) },
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
                                    label = { Text("Vehicle Color", color = TextSecondary) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedColor) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = GlassWhite,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = NeonCyan
                                    ),
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedColor,
                                    onDismissRequest = { expandedColor = false },
                                    modifier = Modifier.background(CardBackground)
                                ) {
                                    carColors.forEach { color ->
                                        DropdownMenuItem(
                                            text = { Text(color, color = TextPrimary) },
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
                        color = NeonCyan
                    )
                }
            }
        }
    }
}
