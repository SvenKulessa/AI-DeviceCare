package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Hell"),
  DARK("Dunkel"),
}

enum class CornerStyle(val label: String) {
  GEOMETRIC("Geometrisch"),
  BALANCED("Ausgewogen"),
  ORGANIC("Organisch"),
}

fun CornerStyle.toShapes(): Shapes =
  when (this) {
    CornerStyle.GEOMETRIC ->
      Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
      )
    CornerStyle.BALANCED ->
      Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
      )
    CornerStyle.ORGANIC ->
      Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(30.dp),
        extraLarge = RoundedCornerShape(36.dp),
      )
  }

@Composable
fun FormwerkTheme(
  palette: ThemePalette = ThemePalette.TERRAKOTTA,
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  cornerStyle: CornerStyle = CornerStyle.BALANCED,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val colorScheme = palette.colorScheme(isDark)
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        WindowCompat.getInsetsController(window, view).apply {
          isAppearanceLightStatusBars = !isDark
          isAppearanceLightNavigationBars = !isDark
        }
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = FormwerkTypography,
    shapes = cornerStyle.toShapes(),
    content = content,
  )
}
