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
import androidx.compose.ui.graphics.Color

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

// Elite AI Light Scheme
private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = FriendlyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA), // Light Blue Container
    onPrimaryContainer = FriendlyTextPrimary,
    secondary = FriendlySecondary,
    onSecondary = Color.White,
    tertiary = NeonGreen, // Keep Neon for status like "Active"
    background = LightBackground,
    onBackground = FriendlyTextPrimary,
    surface = LightSurface,
    onSurface = FriendlyTextPrimary,
    surfaceVariant = LightGlassSurface, // Mapping Glass Surface here for reusability if needed
    onSurfaceVariant = FriendlyTextSecondary,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun EVChargingAppTheme(
    darkTheme: Boolean = true, // Default to Dark
    dynamicColor: Boolean = false, // Disable dynamic color
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EliteColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
