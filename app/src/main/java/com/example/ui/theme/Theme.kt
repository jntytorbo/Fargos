package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalVaultPalette = staticCompositionLocalOf<VaultThemePalette> { VaultThemePalette.Dark }
val LocalAccentColor = staticCompositionLocalOf { Color(0xFF8B5CF6) }

fun parseHexColor(hex: String, fallback: Color = Color(0xFF8B5CF6)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (clean.length == 8) {
            Color(colorInt)
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun GVJVaultTheme(
    paletteName: String = "Dark",
    accentColorHex: String = "#8B5CF6",
    content: @Composable () -> Unit
) {
    val palette = VaultThemePalette.fromName(paletteName)
    val accent = parseHexColor(accentColorHex)

    val colorScheme = if (palette is VaultThemePalette.Light) {
        lightColorScheme(
            primary = accent,
            background = palette.bg,
            surface = palette.surface,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.cardBg,
            outline = palette.border
        )
    } else {
        darkColorScheme(
            primary = accent,
            background = palette.bg,
            surface = palette.surface,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.cardBg,
            outline = palette.border
        )
    }

    CompositionLocalProvider(
        LocalVaultPalette provides palette,
        LocalAccentColor provides accent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
