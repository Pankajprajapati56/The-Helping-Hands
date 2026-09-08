package com.helpinghands.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TealPrimary = Color(0xFF0D9488)
val TealDark = Color(0xFF0F766E)
val TealLight = Color(0xFFCCFBF1)
val GoldAmber = Color(0xFFD97706)
val AmberLight = Color(0xFFFEF3C7)
val Slate900 = Color(0xFF0F172A)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)
val SuccessGreen = Color(0xFF16A34A)
val SuccessLight = Color(0xFFDCFCE7)
val ErrorRed = Color(0xFFDC2626)
val ErrorLight = Color(0xFFFEE2E2)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealLight,
    onPrimaryContainer = Color(0xFF115E59),
    secondary = GoldAmber,
    onSecondary = Color.White,
    secondaryContainer = AmberLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Slate700,
    onTertiary = Color.White,
    tertiaryContainer = Slate100,
    onTertiaryContainer = Slate900,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Color(0xFFCBD5E1),
    error = ErrorRed,
    errorContainer = ErrorLight,
    onError = Color.White,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF003734),
    primaryContainer = TealDark,
    onPrimaryContainer = TealLight,
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = AmberLight,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D)
)

@Composable
fun HelpingHandsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
