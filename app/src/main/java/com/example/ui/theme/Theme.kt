package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NexoraDarkColorScheme = darkColorScheme(
    primary = NexoraPrimaryDark,
    onPrimary = NexoraOnPrimaryDark,
    primaryContainer = NexoraPrimaryContainerDark,
    onPrimaryContainer = NexoraOnPrimaryContainerDark,
    secondary = NexoraSecondaryDark,
    onSecondary = NexoraOnSecondaryDark,
    secondaryContainer = NexoraSecondaryContainerDark,
    onSecondaryContainer = NexoraOnSecondaryContainerDark,
    tertiary = NexoraTertiaryDark,
    onTertiary = NexoraOnTertiaryDark,
    background = NexoraDarkBackground,
    onBackground = NexoraTextPrimaryDark,
    surface = NexoraDarkSurface,
    onSurface = NexoraTextPrimaryDark,
    surfaceVariant = NexoraDarkSurfaceVariant,
    onSurfaceVariant = NexoraTextSecondaryDark,
    outline = NexoraOutlineDark,
    surfaceContainer = NexoraDarkSurfaceContainer,
    surfaceContainerHigh = NexoraDarkSurfaceElevated
)

private val NexoraLightColorScheme = lightColorScheme(
    primary = NexoraPrimaryLight,
    onPrimary = NexoraOnPrimaryLight,
    primaryContainer = NexoraPrimaryContainerLight,
    onPrimaryContainer = NexoraOnPrimaryContainerLight,
    secondary = NexoraSecondaryLight,
    onSecondary = NexoraOnSecondaryLight,
    secondaryContainer = NexoraSecondaryContainerLight,
    onSecondaryContainer = NexoraOnSecondaryContainerLight,
    tertiary = NexoraTertiaryLight,
    onTertiary = NexoraOnTertiaryLight,
    background = NexoraLightBackground,
    onBackground = NexoraTextPrimaryLight,
    surface = NexoraLightSurface,
    onSurface = NexoraTextPrimaryLight,
    surfaceVariant = NexoraLightSurfaceVariant,
    onSurfaceVariant = NexoraTextSecondaryLight,
    outline = NexoraOutlineLight,
    surfaceContainer = NexoraLightSurfaceContainer
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinct custom Nexora aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NexoraDarkColorScheme
        else -> NexoraLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
