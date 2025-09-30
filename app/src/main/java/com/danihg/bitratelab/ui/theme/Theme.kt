package com.danihg.bitratelab.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Speedometer-inspired dark theme (primary theme)
private val SpeedometerColorScheme = darkColorScheme(
    // Primary colors - Warm Orange for buttons and active elements
    primary = WarmOrange,
    onPrimary = OnPrimary,
    primaryContainer = VioletBlue,
    onPrimaryContainer = YellowOrange,

    // Secondary colors - Yellow Orange for secondary actions
    secondary = YellowOrange,
    onSecondary = DarkBlue,
    secondaryContainer = VioletBlue,
    onSecondaryContainer = YellowOrange,

    // Tertiary - Violet Blue for accents
    tertiary = VioletBlue,
    onTertiary = OnSurface,

    // Background and Surface - Dark cool tones
    background = Background,        // Dark Blue
    onBackground = OnBackground,    // Light text
    surface = Surface,              // Medium Blue for cards
    onSurface = OnSurface,          // Light text
    surfaceVariant = VioletBlue,
    onSurfaceVariant = OnSurface,

    // Error colors - Red Orange
    error = Error,                  // Red Orange
    onError = OnPrimary,
    errorContainer = RedOrange,
    onErrorContainer = OnPrimary,

    // Outline and other colors
    outline = VioletBlue,
    outlineVariant = MediumBlue
)

// Keep a light scheme as fallback (though we'll force dark theme)
private val LightColorScheme = lightColorScheme(
    primary = WarmOrange,
    secondary = YellowOrange,
    tertiary = VioletBlue,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimary,
    onSecondary = DarkBlue,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = Error,
    errorContainer = RedOrange,
    onError = OnPrimary
)

@Composable
fun BitrateLabTheme(
    darkTheme: Boolean = true,  // Always use dark theme for speedometer look
    // Disable dynamic color to always use our custom speedometer palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use the Speedometer color scheme
    val colorScheme = SpeedometerColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}