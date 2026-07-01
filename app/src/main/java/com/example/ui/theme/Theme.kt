package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ThemePrimary,
    secondary = ThemeSecondary,
    tertiary = ThemeTertiary,
    background = ThemeBackground,
    surface = ThemeSurface,
    surfaceVariant = ThemeSurfaceVariant,
    onBackground = ThemeOnBackground,
    onSurface = ThemeOnBackground,
    onSurfaceVariant = ThemeOnSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ThemePrimary,
    secondary = ThemeSecondary,
    tertiary = ThemeTertiary,
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4E7EB),
    onBackground = Color(0xFF1A1C23),
    onSurface = Color(0xFF1A1C23),
    onSurfaceVariant = Color(0xFF4A4D55)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to enforce our premium brand identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        // Not using dynamic colors, sticking to brand colors
        if (darkTheme) DarkColorScheme else LightColorScheme
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
