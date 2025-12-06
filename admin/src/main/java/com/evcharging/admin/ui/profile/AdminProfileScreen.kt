package com.evcharging.admin.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.admin.ui.components.AddressAutocomplete
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(
    navController: NavController,
    viewModel: AdminProfileViewModel = hiltViewModel()
) {
    val adminName by viewModel.adminName.collectAsState()
    val stationName by viewModel.stationName.collectAsState()
    val stationAddress by viewModel.stationAddress.collectAsState()
    val stationImageUrl by viewModel.stationImageUrl.collectAsState()
    val stationVideoUrl by viewModel.stationVideoUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var imageInput by remember { mutableStateOf("") }
    var videoInput by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Initialize inputs when data is loaded
    LaunchedEffect(adminName, stationAddress, stationImageUrl, stationVideoUrl) {
        if (nameInput.isEmpty()) nameInput = adminName
        if (addressInput.isEmpty()) addressInput = stationAddress
        if (imageInput.isEmpty()) imageInput = stationImageUrl
        if (videoInput.isEmpty()) videoInput = stationVideoUrl
    }

    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("Profile Updated Successfully")
                delay(1000)
                viewModel.resetSuccess()
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Admin Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Station Location", style = MaterialTheme.typography.titleMedium)
                
                // Custom Search with Google Places
                val suggestions by viewModel.locationSuggestions.collectAsState()
                var expanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { 
                            addressInput = it
                            viewModel.searchLocation(it)
                            expanded = true
                        },
                        label = { Text("Search Location") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )
                    
                    if (expanded && suggestions.isNotEmpty()) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            suggestions.forEach { prediction ->
                                DropdownMenuItem(
                                    text = { Text(prediction.primaryText) },
                                    onClick = {
                                        addressInput = prediction.primaryText
                                        viewModel.clearSuggestions()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text("Selected Address") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true, // Make it read-only so they use the search
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )

                // Image Upload
                Text("Station Image", style = MaterialTheme.typography.titleMedium)
                var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
                val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    selectedImageUri = uri
                }

                if (selectedImageUri != null) {
                    Text("Image Selected", color = MaterialTheme.colorScheme.primary)
                } else if (imageInput.isNotEmpty()) {
                    Text("Current Image: $imageInput", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }

                Button(onClick = { imageLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedImageUri != null) "Change Image" else "Select Image")
                }

                // Video Upload
                Text("Station Video", style = MaterialTheme.typography.titleMedium)
                var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
                val videoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    selectedVideoUri = uri
                }

                if (selectedVideoUri != null) {
                    Text("Video Selected", color = MaterialTheme.colorScheme.primary)
                } else if (videoInput.isNotEmpty()) {
                    Text("Current Video: $videoInput", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }

                Button(onClick = { videoLauncher.launch("video/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedVideoUri != null) "Change Video" else "Select Video")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = nameInput,
                            address = addressInput,
                            imageUri = selectedImageUri,
                            videoUri = selectedVideoUri,
                            currentImageUrl = imageInput,
                            currentVideoUrl = videoInput
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}
