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
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GeoPurple,
    onPrimary = Color.White,
    secondary = GeoLightPurple,
    onSecondary = GeoDarkPurple,
    tertiary = GeoInputBg,
    background = Color(0xFF141318), // Cozy dark slate to coordinate with geometry theme
    surface = Color(0xFF1D1B22),
    onBackground = GeoCreamBg,
    onSurface = GeoCreamBg,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeoPurple,
    onPrimary = Color.White,
    secondary = GeoLightPurple,
    onSecondary = GeoDarkPurple,
    background = GeoCreamBg,
    surface = GeoSurface,
    onBackground = GeoTextDark,
    onSurface = GeoTextDark,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set to false to prioritize Geometric Balance colors by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
