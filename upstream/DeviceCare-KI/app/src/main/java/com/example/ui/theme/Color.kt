package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemePalette(
  val displayName: String,
  val tagline: String,
) {
  TERRAKOTTA("Terrakotta Studio", "Warmes Ziegelrot & Sand"),
  KOBALT("Kobalt Editorial", "Präzises Blau & Alabaster"),
  SALBEI("Salbei & Stein", "Ruhiges Grün & Kreide"),
  OBSIDIAN("Obsidian Gold", "Edles Gold & Graphit"),
}

// 1. Terrakotta Studio
private val TerrakottaLight =
  lightColorScheme(
    primary = Color(0xFFC4512C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF476657),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC9ECD9),
    onSecondaryContainer = Color(0xFF042116),
    tertiary = Color(0xFF7D5836),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFAF6F2),
    onBackground = Color(0xFF211A17),
    surface = Color(0xFFFAF6F2),
    onSurface = Color(0xFF211A17),
    surfaceVariant = Color(0xFFF0E4DC),
    onSurfaceVariant = Color(0xFF52443D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5EFE9),
    surfaceContainer = Color(0xFFEFE7E0),
    surfaceContainerHigh = Color(0xFFE9E0D8),
    surfaceContainerHighest = Color(0xFFE3D8CF),
    outline = Color(0xFF84736B),
    outlineVariant = Color(0xFFD7C6BD),
  )

private val TerrakottaDark =
  darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF933414),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFADD0BD),
    onSecondary = Color(0xFF19372A),
    secondaryContainer = Color(0xFF304E40),
    onSecondaryContainer = Color(0xFFC9ECD9),
    tertiary = Color(0xFFF0BF94),
    onTertiary = Color(0xFF482A0D),
    tertiaryContainer = Color(0xFF624020),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF181210),
    onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF181210),
    onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF4D3F38),
    onSurfaceVariant = Color(0xFFD4C3BA),
    surfaceContainerLowest = Color(0xFF120D0B),
    surfaceContainerLow = Color(0xFF211A17),
    surfaceContainer = Color(0xFF261E1B),
    surfaceContainerHigh = Color(0xFF312824),
    surfaceContainerHighest = Color(0xFF3C322E),
    outline = Color(0xFF9D8D85),
    outlineVariant = Color(0xFF4D3F38),
  )

// 2. Kobalt Editorial
private val KobaltLight =
  lightColorScheme(
    primary = Color(0xFF1E4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE2FF),
    onPrimaryContainer = Color(0xFF001453),
    secondary = Color(0xFF495D7A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E3FF),
    onSecondaryContainer = Color(0xFF061B36),
    tertiary = Color(0xFFB5600B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC0),
    onTertiaryContainer = Color(0xFF341800),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF161B26),
    surface = Color(0xFFF6F8FC),
    onSurface = Color(0xFF161B26),
    surfaceVariant = Color(0xFFE3E8F4),
    onSurfaceVariant = Color(0xFF444B5A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F3FA),
    surfaceContainer = Color(0xFFE9EDF7),
    surfaceContainerHigh = Color(0xFFE2E7F2),
    surfaceContainerHighest = Color(0xFFDCE1ED),
    outline = Color(0xFF747B8B),
    outlineVariant = Color(0xFFC5CCD9),
  )

private val KobaltDark =
  darkColorScheme(
    primary = Color(0xFFB5C4FF),
    onPrimary = Color(0xFF002487),
    primaryContainer = Color(0xFF0A39B4),
    onPrimaryContainer = Color(0xFFDCE2FF),
    secondary = Color(0xFFB3C7E8),
    onSecondary = Color(0xFF1B2F4A),
    secondaryContainer = Color(0xFF324662),
    onSecondaryContainer = Color(0xFFD5E3FF),
    tertiary = Color(0xFFFFB878),
    onTertiary = Color(0xFF4D2600),
    tertiaryContainer = Color(0xFF703B00),
    onTertiaryContainer = Color(0xFFFFDCC0),
    background = Color(0xFF0F131D),
    onBackground = Color(0xFFE3E7F2),
    surface = Color(0xFF0F131D),
    onSurface = Color(0xFFE3E7F2),
    surfaceVariant = Color(0xFF3E4555),
    onSurfaceVariant = Color(0xFFC2C8D6),
    surfaceContainerLowest = Color(0xFF0A0E16),
    surfaceContainerLow = Color(0xFF171C28),
    surfaceContainer = Color(0xFF1C2230),
    surfaceContainerHigh = Color(0xFF272D3C),
    surfaceContainerHighest = Color(0xFF323949),
    outline = Color(0xFF8C93A3),
    outlineVariant = Color(0xFF3E4555),
  )

// 3. Salbei & Stein
private val SalbeiLight =
  lightColorScheme(
    primary = Color(0xFF286655),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7EFE0),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF53635D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E8E0),
    onSecondaryContainer = Color(0xFF111F1A),
    tertiary = Color(0xFFB85440),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD2),
    onTertiaryContainer = Color(0xFF3D0600),
    background = Color(0xFFF5F7F5),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5F7F5),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFE1E8E4),
    onSurfaceVariant = Color(0xFF414A46),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF2EE),
    surfaceContainer = Color(0xFFE8EDE9),
    surfaceContainerHigh = Color(0xFFE1E8E3),
    surfaceContainerHighest = Color(0xFFDBE2DD),
    outline = Color(0xFF707A75),
    outlineVariant = Color(0xFFC1CBC5),
  )

private val SalbeiDark =
  darkColorScheme(
    primary = Color(0xFF9BD4C3),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF104F3F),
    onPrimaryContainer = Color(0xFFB7EFE0),
    secondary = Color(0xFFBACBC4),
    onSecondary = Color(0xFF25342F),
    secondaryContainer = Color(0xFF3C4B45),
    onSecondaryContainer = Color(0xFFD6E8E0),
    tertiary = Color(0xFFFFB4A5),
    onTertiary = Color(0xFF5E1608),
    tertiaryContainer = Color(0xFF832D1B),
    onTertiaryContainer = Color(0xFFFFDAD2),
    background = Color(0xFF101614),
    onBackground = Color(0xFFDFE6E2),
    surface = Color(0xFF101614),
    onSurface = Color(0xFFDFE6E2),
    surfaceVariant = Color(0xFF3B4440),
    onSurfaceVariant = Color(0xFFBEC8C2),
    surfaceContainerLowest = Color(0xFF0B100E),
    surfaceContainerLow = Color(0xFF181F1C),
    surfaceContainer = Color(0xFF1D2522),
    surfaceContainerHigh = Color(0xFF27302C),
    surfaceContainerHighest = Color(0xFF323B37),
    outline = Color(0xFF88928C),
    outlineVariant = Color(0xFF3B4440),
  )

// 4. Obsidian Gold
private val ObsidianLight =
  lightColorScheme(
    primary = Color(0xFF9C721A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCE2AE),
    onPrimaryContainer = Color(0xFF2B1C00),
    secondary = Color(0xFF635A4A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBE0CC),
    onSecondaryContainer = Color(0xFF1F190C),
    tertiary = Color(0xFF8F4D32),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCD),
    onTertiaryContainer = Color(0xFF351003),
    background = Color(0xFFFAF8F3),
    onBackground = Color(0xFF1D1B17),
    surface = Color(0xFFFAF8F3),
    onSurface = Color(0xFF1D1B17),
    surfaceVariant = Color(0xFFECE6DA),
    onSurfaceVariant = Color(0xFF4C463B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F1EA),
    surfaceContainer = Color(0xFFEEEAE1),
    surfaceContainerHigh = Color(0xFFE8E3D9),
    surfaceContainerHighest = Color(0xFFE2DDD1),
    outline = Color(0xFF7D7669),
    outlineVariant = Color(0xFFCFC8B9),
  )

private val ObsidianDark =
  darkColorScheme(
    primary = Color(0xFFE5BD66),
    onPrimary = Color(0xFF422C00),
    primaryContainer = Color(0xFF6B4C07),
    onPrimaryContainer = Color(0xFFFCE2AE),
    secondary = Color(0xFFCFC4B0),
    onSecondary = Color(0xFF352E20),
    secondaryContainer = Color(0xFF4C4435),
    onSecondaryContainer = Color(0xFFEBE0CC),
    tertiary = Color(0xFFF3B89E),
    onTertiary = Color(0xFF53220E),
    tertiaryContainer = Color(0xFF703720),
    onTertiaryContainer = Color(0xFFFFDBCD),
    background = Color(0xFF12110E),
    onBackground = Color(0xFFE8E2D9),
    surface = Color(0xFF12110E),
    onSurface = Color(0xFFE8E2D9),
    surfaceVariant = Color(0xFF443F35),
    onSurfaceVariant = Color(0xFFC9C2B5),
    surfaceContainerLowest = Color(0xFF0D0C09),
    surfaceContainerLow = Color(0xFF1A1814),
    surfaceContainer = Color(0xFF201E19),
    surfaceContainerHigh = Color(0xFF2B2822),
    surfaceContainerHighest = Color(0xFF36322B),
    outline = Color(0xFF938C7F),
    outlineVariant = Color(0xFF443F35),
  )

fun ThemePalette.colorScheme(darkTheme: Boolean): ColorScheme =
  when (this) {
    ThemePalette.TERRAKOTTA -> if (darkTheme) TerrakottaDark else TerrakottaLight
    ThemePalette.KOBALT -> if (darkTheme) KobaltDark else KobaltLight
    ThemePalette.SALBEI -> if (darkTheme) SalbeiDark else SalbeiLight
    ThemePalette.OBSIDIAN -> if (darkTheme) ObsidianDark else ObsidianLight
  }

fun ThemePalette.previewSwatches(): List<Color> =
  when (this) {
    ThemePalette.TERRAKOTTA -> listOf(Color(0xFFC4512C), Color(0xFF476657), Color(0xFFF0E4DC))
    ThemePalette.KOBALT -> listOf(Color(0xFF1E4ED8), Color(0xFFB5600B), Color(0xFFDCE2FF))
    ThemePalette.SALBEI -> listOf(Color(0xFF286655), Color(0xFFB85440), Color(0xFFD6E8E0))
    ThemePalette.OBSIDIAN -> listOf(Color(0xFFE5BD66), Color(0xFF8F4D32), Color(0xFF201E19))
  }
