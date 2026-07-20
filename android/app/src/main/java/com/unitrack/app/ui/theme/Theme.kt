package com.unitrack.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueDim,
    onPrimaryContainer = Color.White,
    secondary = AccentViolet,
    onSecondary = Color.White,
    tertiary = StatusSuccess,
    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    error = StatusError,
    onError = Color.White,
    outline = Color(0xFF35353F)
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentBlue,
    onPrimary = Color.White,
    secondary = AccentViolet,
    background = LightBackground,
    onBackground = Color(0xFF17171C),
    surface = LightSurface,
    onSurface = Color(0xFF17171C),
    error = Color(0xFFD3453A)
)

/**
 * dynamicColor varsayılan olarak KAPALI. Sistem duvar kağıdına göre renk
 * çıkarımı (Material You), premium/marka hissi hedefleyen sabit bir palet
 * için tercih edilmiyor — bkz. Color.kt üstündeki not.
 */
@Composable
fun UniTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Sistem çubuklarını da zeminle aynı renge boyayıp kenarlıksız,
            // "tam ekran" bir premium his veriyoruz.
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
