package com.evcharging.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Elite AI Dark Scheme
private val EliteColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepBackground,
    primaryContainer = NeonBlue,
    onPrimaryContainer = TextPrimary,
    secondary = NeonPurple,
    onSecondary = TextPrimary,
    tertiary = NeonGreen,
    background = DeepBackground,
    onBackground = TextPrimary,
    surface = SurfaceBlack, // Or use Glass in components
    onSurface = TextPrimary,
    error = NeonRed,
    onError = TextPrimary
)

@Composable
fun EVChargingAppTheme(
    darkTheme: Boolean = true, // Force Dark Mode for Elite Look
    dynamicColor: Boolean = false, // Disable dynamic color to maintain Brand Identity
    content: @Composable () -> Unit
) {
    val colorScheme = EliteColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepBackground.toArgb()
            window.navigationBarColor = DeepBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
