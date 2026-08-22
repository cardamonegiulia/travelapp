package com.example.travelapp.ui.theme
import androidx.compose.ui.graphics.Color
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceLight,
    primaryContainer = Color(0xFFD0E8F5),
    onPrimaryContainer = PrimaryDark,

    secondary = AccentOrange,
    onSecondary = SurfaceLight,
    secondaryContainer = Color(0xFFFFE8D0),
    onSecondaryContainer = Color(0xFF7A3D00),

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorRed,
    onError = SurfaceLight,
    errorContainer = Color(0xFFFFEAEA),
    onErrorContainer = ErrorRed,

    outline = DividerColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFD0E8F5),

    secondary = AccentOrangeDark,
    onSecondary = BackgroundDark,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = BackgroundDark,
    errorContainer = Color(0xFF4A1515),
    onErrorContainer = Color(0xFFFFB4B4),

    outline = Color(0xFF374151),
)

@Composable
fun TravelAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TravelAppTypography,
        content = content
    )
}