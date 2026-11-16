package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val HindSiliguriFontFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri, FontWeight.SemiBold),
)

@Immutable
data class ChotoTextStyles(
    val display: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
)

// Bangla-first typography: line-height >= 1.6x, letterSpacing = 0.sp everywhere
val DefaultChotoTextStyles = ChotoTextStyles(
    display = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp,
    ),
    h1 = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    h2 = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    body = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
    ),
    bodyStrong = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
    ),
    caption = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    button = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
)

val LocalChotoTextStyles = staticCompositionLocalOf { DefaultChotoTextStyles }

val ChotoMaterialTypography = Typography(
    displayMedium = DefaultChotoTextStyles.display,
    headlineSmall = DefaultChotoTextStyles.h1,
    titleMedium = DefaultChotoTextStyles.h2,
    bodyLarge = DefaultChotoTextStyles.body,
    bodyMedium = DefaultChotoTextStyles.body,
    labelLarge = DefaultChotoTextStyles.button,
    bodySmall = DefaultChotoTextStyles.caption,
    labelSmall = DefaultChotoTextStyles.caption,
)
