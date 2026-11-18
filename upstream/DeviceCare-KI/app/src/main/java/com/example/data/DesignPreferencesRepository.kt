package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.CornerStyle
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePalette
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.designDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "devicecare_ui_prefs")

/**
 * Speichert ausschließlich UI-Design-Einstellungen und lokale QA-Haken des Nutzers.
 * NIEMALS Geräte-, Akku- oder Paket-Beobachtungen persistieren (M1-Datenschutzregel).
 */
data class DesignPreferences(
  val palette: ThemePalette = ThemePalette.TERRAKOTTA,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val cornerStyle: CornerStyle = CornerStyle.BALANCED,
  val compactDensity: Boolean = false,
  val hapticPreview: Boolean = true,
  val localVerifiedTaskIds: Set<String> = emptySet(),
  val bookmarkedTaskIds: Set<String> = setOf("ms-m1", "rem-p1-budget", "qa-4-a11y"),
)

class DesignPreferencesRepository(private val context: Context) {
  private object Keys {
    val PALETTE = stringPreferencesKey("palette")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val CORNER_STYLE = stringPreferencesKey("corner_style")
    val COMPACT_DENSITY = booleanPreferencesKey("compact_density")
    val HAPTIC_PREVIEW = booleanPreferencesKey("haptic_preview")
    val VERIFIED_TASKS = stringSetPreferencesKey("verified_task_ids")
    val BOOKMARKED_TASKS = stringSetPreferencesKey("bookmarked_task_ids")
  }

  val preferencesFlow: Flow<DesignPreferences> =
    context.designDataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { prefs ->
      val palette =
        prefs[Keys.PALETTE]?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() }
          ?: ThemePalette.TERRAKOTTA
      val themeMode =
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
          ?: ThemeMode.SYSTEM
      val cornerStyle =
        prefs[Keys.CORNER_STYLE]?.let { runCatching { CornerStyle.valueOf(it) }.getOrNull() }
          ?: CornerStyle.BALANCED

      DesignPreferences(
        palette = palette,
        themeMode = themeMode,
        cornerStyle = cornerStyle,
        compactDensity = prefs[Keys.COMPACT_DENSITY] ?: false,
        hapticPreview = prefs[Keys.HAPTIC_PREVIEW] ?: true,
        localVerifiedTaskIds = prefs[Keys.VERIFIED_TASKS] ?: emptySet(),
        bookmarkedTaskIds =
          prefs[Keys.BOOKMARKED_TASKS] ?: setOf("ms-m1", "rem-p1-budget", "qa-4-a11y"),
      )
    }

  suspend fun setPalette(palette: ThemePalette) {
    context.designDataStore.edit { it[Keys.PALETTE] = palette.name }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.designDataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setCornerStyle(style: CornerStyle) {
    context.designDataStore.edit { it[Keys.CORNER_STYLE] = style.name }
  }

  suspend fun setCompactDensity(enabled: Boolean) {
    context.designDataStore.edit { it[Keys.COMPACT_DENSITY] = enabled }
  }

  suspend fun setHapticPreview(enabled: Boolean) {
    context.designDataStore.edit { it[Keys.HAPTIC_PREVIEW] = enabled }
  }

  suspend fun toggleLocalTaskVerified(taskId: String) {
    context.designDataStore.edit { prefs ->
      val current = prefs[Keys.VERIFIED_TASKS] ?: emptySet()
      prefs[Keys.VERIFIED_TASKS] =
        if (taskId in current) current - taskId else current + taskId
    }
  }

  suspend fun toggleBookmark(taskId: String) {
    context.designDataStore.edit { prefs ->
      val current =
        prefs[Keys.BOOKMARKED_TASKS] ?: setOf("ms-m1", "rem-p1-budget", "qa-4-a11y")
      prefs[Keys.BOOKMARKED_TASKS] =
        if (taskId in current) current - taskId else current + taskId
    }
  }

  suspend fun resetDefaults() {
    context.designDataStore.edit { prefs ->
      prefs[Keys.PALETTE] = ThemePalette.TERRAKOTTA.name
      prefs[Keys.THEME_MODE] = ThemeMode.SYSTEM.name
      prefs[Keys.CORNER_STYLE] = CornerStyle.BALANCED.name
      prefs[Keys.COMPACT_DENSITY] = false
      prefs[Keys.HAPTIC_PREVIEW] = true
    }
  }
}
