package com.example.paseape.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema de colores para el modo oscuro
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    secondary = CardSurface,
    tertiary = PrimaryDark,
    background = PrimaryDark,
    surface = PrimaryDark
)

// Esquema de colores para el modo claro (Principal de PaseaPE)
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    secondary = CardSurface,
    tertiary = PrimaryDark,
    background = BackgroundOffWhite,
    surface = CardSurface
)

@Composable
fun PaseaPETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}