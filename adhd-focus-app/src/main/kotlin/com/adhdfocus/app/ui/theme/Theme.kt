package com.adhdfocus.app.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = ErrorLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight
)

@Composable
fun AdhdfocusAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled dynamic colors to ensure WCAG compliance
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * AdhdfocusAppThemeWithTheme applies theme based on Theme enum value.
 * Ensures WCAG 2.1 AA compliance by disabling dynamic colors.
 *
 * @param theme Theme to apply (LIGHT or DARK)
 * @param dynamicColor Whether to use dynamic colors (disabled for WCAG compliance)
 * @param content Composable content
 */
@Composable
fun AdhdfocusAppThemeWithTheme(
    theme: com.adhdfocus.app.data.model.Theme,
    dynamicColor: Boolean = false,  // Disabled for WCAG compliance
    content: @Composable () -> Unit
) {
    val isDarkTheme = theme == com.adhdfocus.app.data.model.Theme.DARK
    AdhdfocusAppTheme(
        darkTheme = isDarkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
