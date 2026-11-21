package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LkLightColorScheme = lightColorScheme(
    primary = LkEmeraldPrimary,
    onPrimary = LkOnEmeraldPrimary,
    primaryContainer = LkEmeraldContainer,
    onPrimaryContainer = LkOnEmeraldContainer,
    secondary = LkSlateSecondary,
    onSecondary = LkOnSlateSecondary,
    secondaryContainer = LkSlateContainer,
    onSecondaryContainer = LkOnSlateContainer,
    tertiary = LkIndigoTertiary,
    onTertiary = LkOnIndigoTertiary,
    tertiaryContainer = LkIndigoContainer,
    onTertiaryContainer = LkOnIndigoContainer,
    background = LkLightBackground,
    onBackground = LkLightOnBackground,
    surface = LkLightSurface,
    onSurface = LkLightOnSurface,
    surfaceVariant = LkLightSurfaceVariant,
    onSurfaceVariant = LkLightOnSurfaceVariant,
    outline = LkLightOutline,
    outlineVariant = LkLightOutlineVariant,
    error = LkErrorRed,
    errorContainer = LkErrorContainerLight,
    onErrorContainer = LkOnErrorContainerLight
)

private val LkDarkColorScheme = darkColorScheme(
    primary = LkMintDarkPrimary,
    onPrimary = LkOnMintDarkPrimary,
    primaryContainer = LkMintDarkContainer,
    onPrimaryContainer = LkOnMintDarkContainer,
    secondary = LkDarkSecondary,
    onSecondary = LkOnDarkSecondary,
    secondaryContainer = LkDarkSecondaryContainer,
    onSecondaryContainer = LkOnDarkSecondaryContainer,
    tertiary = LkDarkTertiary,
    onTertiary = LkOnDarkTertiary,
    tertiaryContainer = LkDarkTertiaryContainer,
    onTertiaryContainer = LkOnDarkTertiaryContainer,
    background = LkDarkBackground,
    onBackground = LkDarkOnBackground,
    surface = LkDarkSurface,
    onSurface = LkDarkOnSurface,
    surfaceVariant = LkDarkSurfaceVariant,
    onSurfaceVariant = LkDarkOnSurfaceVariant,
    outline = LkDarkOutline,
    outlineVariant = LkDarkOutlineVariant
)

val LkStoreShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LkStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LkDarkColorScheme
        else -> LkLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LkStoreTypography,
        shapes = LkStoreShapes,
        content = content
    )
}
