package com.example.padibot.theme

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

// Fresh Agricultural Light Palette (AI Studio & Blog Spec)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),            // Emerald 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),   // Emerald 100
    onPrimaryContainer = Color(0xFF064E3B), // Emerald 900
    secondary = Color(0xFF10B981),          // Emerald 500
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5), // Emerald 50
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFFD97706),           // Amber 600
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),         // Slate 50 (Fresh Light BG)
    onBackground = Color(0xFF0F172A),       // Slate 900
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),     // Slate 100
    onSurfaceVariant = Color(0xFF334155),   // Slate 700
    outline = Color(0xFFCBD5E1),            // Slate 300
    outlineVariant = Color(0xFFE2E8F0),     // Slate 200
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF047857),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun PadiBotTheme(
    darkTheme: Boolean = false, // Default to Versi Light (Terang)
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
