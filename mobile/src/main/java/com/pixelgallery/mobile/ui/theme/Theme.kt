package com.pixelgallery.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PixelDarkColorScheme = darkColorScheme(
    primary = PixelTealPrimary,
    onPrimary = PixelTealOnPrimary,
    primaryContainer = PixelTealPrimaryContainer,
    onPrimaryContainer = PixelTealOnPrimaryContainer,
    secondary = PixelBlueSecondary,
    onSecondary = PixelBlueOnSecondary,
    secondaryContainer = PixelBlueSecondaryContainer,
    onSecondaryContainer = PixelBlueOnSecondaryContainer,
    background = PixelDarkBackground,
    onBackground = PixelTextPrimary,
    surface = PixelDarkSurface,
    onSurface = PixelTextPrimary,
    surfaceVariant = PixelDarkSurfaceVariant,
    onSurfaceVariant = PixelTextSecondary,
    outline = PixelOutline,
    error = PixelAccentError
)

@Composable
fun PixelGalleryTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = PixelDarkBackground.toArgb()
            window.navigationBarColor = PixelDarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = PixelDarkColorScheme,
        typography = Typography,
        content = content
    )
}
