package com.example.paseape.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * La app está diseñada con superficies claras (blancas) en todas sus pantallas,
 * por eso se usa siempre el esquema claro. Si se respetara el modo oscuro del
 * sistema, los textos de los campos tomarían colores claros sobre fondos
 * blancos y se verían lavados o casi invisibles.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    secondary = CardSurface,
    onSecondary = PrimaryDark,
    tertiary = PrimaryDark,
    onTertiary = Color.White,
    background = BackgroundOffWhite,
    onBackground = PrimaryDark,
    surface = CardSurface,
    onSurface = PrimaryDark,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFF9A9A9A),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun PaseaPETheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = LightColorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
