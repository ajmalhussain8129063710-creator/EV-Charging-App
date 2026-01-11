package com.evcharging.app.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.evcharging.app.ui.components.VoiceAssistantButton
import com.evcharging.app.R

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    
    // Restoration of AI Mode Toggle
    var isAiMode by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
            viewModel.resetState()
        }
    }

    Scaffold(
        floatingActionButton = {
            VoiceAssistantButton { /* Handle AI commands on login if needed */ }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // --- 1. Wave Background (Shared) ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Wave 1
                val path = Path().apply {
                    moveTo(0f, height * 0.4f)
                    cubicTo(
                        width * 0.2f, height * 0.35f,
                        width * 0.8f, height * 0.5f,
                        width, height * 0.45f
                    )
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = if (isAiMode) listOf( // Darker/Mysterious for AI Mode
                            Color(0xFFE3F2FD).copy(alpha = 0.6f),
                            Color(0xFFBBDEFB).copy(alpha = 0.8f)
                        ) else listOf( // Fresh Cyan/Green for Standard
                            Color(0xFFE0F7FA).copy(alpha = 0.6f),
                            Color(0xFFE0F2F1).copy(alpha = 0.8f)
                        )
                    )
                )
                
                // Wave 2
                 val path2 = Path().apply {
                    moveTo(0f, height * 0.5f)
                    cubicTo(
                        width * 0.3f, height * 0.55f,
                        width * 0.7f, height * 0.4f,
                        width, height * 0.48f
                    )
                     lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                 drawPath(
                    path = path2,
                    brush = Brush.linearGradient(
                        colors = if (isAiMode) listOf(
                             Color(0xFF42A5F5).copy(alpha = 0.3f), 
                             Color(0xFF1E88E5).copy(alpha = 0.3f)
                        ) else listOf(
                             Color(0xFF80DEEA).copy(alpha = 0.3f), 
                             Color(0xFFA5D6A7).copy(alpha = 0.3f) 
                        ),
                        start = Offset(0f, height * 0.5f),
                        end = Offset(width, height)
                    )
                )
            }

            // --- 2. Main Content Area ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isAiMode) {
                    // AI Login UI
                    AiLoginContent(
                        email = emailOrPhone,
                        onEmailChange = { emailOrPhone = it },
                        password = password,
                        onPasswordChange = { password = it },
                        onLogin = { viewModel.login(emailOrPhone, password) },
                        authState = authState,
                        onSignupClick = { navController.navigate("signup") }
                    )
                } else {
                    // Standard Login UI
                    StandardLoginContent(
                        email = emailOrPhone,
                        onEmailChange = { emailOrPhone = it },
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = it },
                        rememberMe = rememberMe,
                        onRememberMeChange = { rememberMe = it },
                        onLogin = { viewModel.login(emailOrPhone, password) },
                        authState = authState,
                        onSignupClick = { navController.navigate("signup") }
                    )
                }
            }

            // --- 3. Mode Toggle (Top Right) ---
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { isAiMode = !isAiMode }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isAiMode) "AI HOST" else "STANDARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAiMode) Color(0xFF1E88E5) else Color(0xFF009688), // Blue for AI, Teal for Standard
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAiMode,
                            onCheckedChange = { isAiMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1E88E5), // Blue
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF26A69A).copy(alpha = 0.5f), // Teal
                                uncheckedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StandardLoginContent(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean, onPasswordVisibilityChange: (Boolean) -> Unit,
    rememberMe: Boolean, onRememberMeChange: (Boolean) -> Unit,
    onLogin: () -> Unit,
    authState: AuthState,
    onSignupClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF4DD0E1), Color(0xFF81C784))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF263238)
        )

        Text(
            text = "Sign in to continue charging smarter",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF78909C),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email or Mobile Number", color = Color(0xFF90A4AE)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4DD0E1),
                        unfocusedBorderColor = Color(0xFFCFD8DC),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password", color = Color(0xFF90A4AE)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color(0xFF90A4AE))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4DD0E1),
                        unfocusedBorderColor = Color(0xFFCFD8DC),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF26C6DA), Color(0xFF66BB6A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("LOG IN", fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = onRememberMeChange,
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF26C6DA))
                        )
                        Text("Remember me", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78909C))
                    }
                    Text("Forgot password?", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78909C), modifier = Modifier.clickable {})
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", color = Color(0xFF78909C))
            TextButton(onClick = onSignupClick) {
                Text("Sign Up", color = Color(0xFF26C6DA), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        
        if (authState is AuthState.Error) {
             Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun AiLoginContent(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    authState: AuthState,
    onSignupClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E88E5).copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Color(0xFF64B5F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "AI Host",
                tint = Color(0xFF1565C0), // Darker Blue
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "System Host Online",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF1565C0),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Identify yourself to proceed.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF546E7A)
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("Identity (Email/Phone)") },
            modifier = Modifier.fillMaxWidth(0.9f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.8f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Color(0xFF42A5F5),
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Access Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.9f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.8f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Color(0xFF42A5F5),
                unfocusedBorderColor = Color.Transparent
            ),
             shape = RoundedCornerShape(24.dp),
             keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onLogin() })
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
             if (authState is AuthState.Loading) {
                Text("VERIFYING...", color = Color.White)
            } else {
                Text("ACCESS SYSTEM", color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onSignupClick) {
            Text("Create New Identity", color = Color(0xFF1976D2))
        }
        
        if (authState is AuthState.Error) {
             Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
