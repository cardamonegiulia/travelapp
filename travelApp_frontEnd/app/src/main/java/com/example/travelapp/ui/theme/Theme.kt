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

// Valori grezzi usati solo per costruire i due schemi qui sotto.
private val BackgroundLightRaw = Color(0xFFF7F8FA)
private val SurfaceLightRaw = Color(0xFFFFFFFF)
private val TextPrimaryLightRaw = Color(0xFF1A1D1F)
private val TextSecondaryLightRaw = Color(0xFF6B7280)
private val DividerLightRaw = Color(0xFFE5E7EB)

private val BackgroundDarkRaw = Color(0xFF121417)
private val SurfaceDarkRaw = Color(0xFF1C1F23)
private val TextPrimaryDarkRaw = Color(0xFFF2F3F4)
private val TextSecondaryDarkRaw = Color(0xFF9CA3AF)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceLightRaw,
    primaryContainer = Color(0xFFD0E8F5),
    onPrimaryContainer = PrimaryDark,

    secondary = AccentOrange,
    onSecondary = SurfaceLightRaw,
    secondaryContainer = Color(0xFFFFE8D0),
    onSecondaryContainer = Color(0xFF7A3D00),

    background = BackgroundLightRaw,
    onBackground = TextPrimaryLightRaw,

    surface = SurfaceLightRaw,
    onSurface = TextPrimaryLightRaw,
    onSurfaceVariant = TextSecondaryLightRaw,

    error = ErrorRed,
    onError = SurfaceLightRaw,
    errorContainer = Color(0xFFFFEAEA),
    onErrorContainer = ErrorRed,

    outline = DividerLightRaw,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = BackgroundDarkRaw,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFD0E8F5),

    secondary = AccentOrangeDark,
    onSecondary = BackgroundDarkRaw,

    background = BackgroundDarkRaw,
    onBackground = TextPrimaryDarkRaw,

    surface = SurfaceDarkRaw,
    onSurface = TextPrimaryDarkRaw,
    onSurfaceVariant = TextSecondaryDarkRaw,

    error = ErrorRed,
    onError = BackgroundDarkRaw,
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