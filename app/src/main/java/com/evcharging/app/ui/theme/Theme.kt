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

// Elite Deep Space (Dark Mode)
private val DeepSpaceScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepSpaceBlack,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = ElectricPurple,
    onSecondary = Color.White,
    tertiary = VividGreen,
    background = DeepSpaceBlack,
    onBackground = Color.White,
    surface = DeepSurface,
    onSurface = Color.White,
    surfaceVariant = DarkGlass, // For Glass Cards
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    error = VividOrange,
    outline = ElectricCyan.copy(alpha = 0.5f)
)

// Bright Future (Light Mode) - High Energy & Clean
private val BrightFutureScheme = androidx.compose.material3.lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricCyan.copy(alpha = 0.2f),
    onPrimaryContainer = InkBlack,
    secondary = ElectricPurple,
    onSecondary = Color.White,
    tertiary = VividGreen,
    background = BrightBackground,
    onBackground = InkBlack,
    surface = BrightSurface,
    onSurface = InkBlack,
    surfaceVariant = BrightSurfaceVariant,
    onSurfaceVariant = InkGrey,
    error = VividOrange,
    outline = GlassBorderLight
)

@Composable
fun EVChargingAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Default to System Setting
    dynamicColor: Boolean = false, // Disable dynamic color
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DeepSpaceScheme else BrightFutureScheme

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
