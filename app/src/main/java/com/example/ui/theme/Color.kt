package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ChotoColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val action: Color,
    val actionText: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
)

val LightChotoColors = ChotoColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFF6F6F7),
    surface2 = Color(0xFFEFEFF1),
    textPrimary = Color(0xFF111114),
    textSecondary = Color(0xFF6B6B75),
    textTertiary = Color(0xFF9A9AA3),
    border = Color(0xFFE6E6EA),
    action = Color(0xFF111114),
    actionText = Color(0xFFFFFFFF),
    accent = Color(0xFF1F7A55),
    accentSoft = Color(0xFFE8F4EF),
    danger = Color(0xFFC4392F),
    dangerSoft = Color(0xFFFBEDEC),
)

val DarkChotoColors = ChotoColors(
    bg = Color(0xFF0B0B0D),
    surface = Color(0xFF161619),
    surface2 = Color(0xFF1F1F24),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF9E9EA8),
    textTertiary = Color(0xFF6E6E78),
    border = Color(0xFF2A2A31),
    action = Color(0xFFF5F5F7),
    actionText = Color(0xFF111114),
    accent = Color(0xFF3DD68C),
    accentSoft = Color(0xFF122D21),
    danger = Color(0xFFE5534B),
    dangerSoft = Color(0xFF2C1616),
)

val LocalChotoColors = staticCompositionLocalOf { LightChotoColors }
