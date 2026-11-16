package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalReducedMotion = staticCompositionLocalOf { false }

private val ChotoLightMaterialScheme = lightColorScheme(
    primary = LightChotoColors.action,
    onPrimary = LightChotoColors.actionText,
    secondary = LightChotoColors.accent,
    onSecondary = LightChotoColors.actionText,
    secondaryContainer = LightChotoColors.accentSoft,
    onSecondaryContainer = LightChotoColors.accent,
    background = LightChotoColors.bg,
    onBackground = LightChotoColors.textPrimary,
    surface = LightChotoColors.surface,
    onSurface = LightChotoColors.textPrimary,
    surfaceVariant = LightChotoColors.surface2,
    onSurfaceVariant = LightChotoColors.textSecondary,
    outline = LightChotoColors.border,
    error = LightChotoColors.danger,
    onError = LightChotoColors.actionText,
    errorContainer = LightChotoColors.dangerSoft,
    onErrorContainer = LightChotoColors.danger,
)

private val ChotoDarkMaterialScheme = darkColorScheme(
    primary = DarkChotoColors.action,
    onPrimary = DarkChotoColors.actionText,
    secondary = DarkChotoColors.accent,
    onSecondary = DarkChotoColors.actionText,
    secondaryContainer = DarkChotoColors.accentSoft,
    onSecondaryContainer = DarkChotoColors.accent,
    background = DarkChotoColors.bg,
    onBackground = DarkChotoColors.textPrimary,
    surface = DarkChotoColors.surface,
    onSurface = DarkChotoColors.textPrimary,
    surfaceVariant = DarkChotoColors.surface2,
    onSurfaceVariant = DarkChotoColors.textSecondary,
    outline = DarkChotoColors.border,
    error = DarkChotoColors.danger,
    onError = DarkChotoColors.actionText,
    errorContainer = DarkChotoColors.dangerSoft,
    onErrorContainer = DarkChotoColors.danger,
)

private val ChotoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun ChotoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val chotoColors = if (darkTheme) DarkChotoColors else LightChotoColors
    val materialScheme = if (darkTheme) ChotoDarkMaterialScheme else ChotoLightMaterialScheme

    CompositionLocalProvider(
        LocalChotoColors provides chotoColors,
        LocalChotoTextStyles provides DefaultChotoTextStyles,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = ChotoMaterialTypography,
            shapes = ChotoShapes,
            content = content,
        )
    }
}

object ChotoThemeTokens {
    val colors: ChotoColors
        @Composable get() = LocalChotoColors.current

    val typography: ChotoTextStyles
        @Composable get() = LocalChotoTextStyles.current

    val reducedMotion: Boolean
        @Composable get() = LocalReducedMotion.current
}

fun isReducedMotionEnabled(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f,
        )
        scale == 0f
    } catch (_: Exception) {
        false
    }
}
