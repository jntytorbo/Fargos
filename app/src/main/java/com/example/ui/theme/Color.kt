package com.example.ui.theme

import androidx.compose.ui.graphics.Color

sealed class VaultThemePalette(
    val name: String,
    val bg: Color,
    val surface: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val skeletonBg: Color
) {
    object Dark : VaultThemePalette(
        name = "Dark",
        bg = Color(0xFF1E1E22),
        surface = Color(0xFF28282D),
        cardBg = Color(0xFF2A2A30),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFD1D5DB),
        textMuted = Color(0xFF9CA3AF),
        border = Color(0x2EFFFFFF),
        skeletonBg = Color(0xFF38383F)
    )

    object Amoled : VaultThemePalette(
        name = "Amoled",
        bg = Color(0xFF000000),
        surface = Color(0xFF0D0D11),
        cardBg = Color(0xFF141418),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFCCCCCC),
        textMuted = Color(0xFF888888),
        border = Color(0x33FFFFFF),
        skeletonBg = Color(0xFF1F1F24)
    )

    object Blue : VaultThemePalette(
        name = "Blue",
        bg = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        cardBg = Color(0xFF1E293B),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFCBD7E6),
        textMuted = Color(0xFF7E95AD),
        border = Color(0x26FFFFFF),
        skeletonBg = Color(0xFF334155)
    )

    object Grey : VaultThemePalette(
        name = "Grey",
        bg = Color(0xFF2B2E33),
        surface = Color(0xFF35393E),
        cardBg = Color(0xFF3B4046),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFE1E4E8),
        textMuted = Color(0xFFAAB2BC),
        border = Color(0x26FFFFFF),
        skeletonBg = Color(0xFF494F56)
    )

    object Light : VaultThemePalette(
        name = "Light",
        bg = Color(0xFFF1F5F9),
        surface = Color(0xFFFFFFFF),
        cardBg = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        border = Color(0x1F0F172A),
        skeletonBg = Color(0xFFE2E8F0)
    )

    companion object {
        fun fromName(name: String): VaultThemePalette {
            return when (name.lowercase()) {
                "amoled" -> Amoled
                "blue" -> Blue
                "grey" -> Grey
                "light" -> Light
                else -> Dark
            }
        }
    }
}
