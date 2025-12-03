package com.evcharging.app.ui.components

import android.Manifest
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.evcharging.app.util.VoiceRecognitionManager
import com.evcharging.app.util.VoiceState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceAssistantButton(
    onCommandDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val voiceManager = remember { VoiceRecognitionManager(context) }
    val voiceState by voiceManager.voiceState.collectAsState()
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    LaunchedEffect(voiceState) {
        if (voiceState is VoiceState.Result) {
            onCommandDetected((voiceState as VoiceState.Result).text)
        }
    }

    FloatingActionButton(
        onClick = {
            if (permissionState.status.isGranted) {
                if (voiceState is VoiceState.Listening) {
                    voiceManager.stopListening()
                } else {
                    voiceManager.startListening()
                }
            } else {
                permissionState.launchPermissionRequest()
            }
        },
        containerColor = if (voiceState is VoiceState.Listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = if (voiceState is VoiceState.Listening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = "Voice Assistant",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
