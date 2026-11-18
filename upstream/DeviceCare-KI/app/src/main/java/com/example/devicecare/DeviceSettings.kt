package com.example.devicecare

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings

enum class SettingsLaunchResult {
  DIRECT,
  GENERAL_SETTINGS,
  UNAVAILABLE,
}

/**
 * Erstellt einen Standard-Android-Intent zur App-Info-Systemseite einer installierten App,
 * über die Nutzer Berechtigungen direkt entziehen oder verwalten können.
 */
fun appPermissionSettingsIntent(packageName: String): Intent =
  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

fun ownAppSettings(packageName: String): Intent = appPermissionSettingsIntent(packageName)

fun openDeviceSettings(primary: Intent, launch: (Intent) -> Unit): SettingsLaunchResult {
  // Launch directly because package visibility can hide an otherwise usable handler.
  if (tryLaunch(primary, launch)) return SettingsLaunchResult.DIRECT
  if (tryLaunch(Intent(Settings.ACTION_SETTINGS), launch)) {
    return SettingsLaunchResult.GENERAL_SETTINGS
  }
  return SettingsLaunchResult.UNAVAILABLE
}

private fun tryLaunch(intent: Intent, launch: (Intent) -> Unit): Boolean =
  try {
    launch(intent)
    true
  } catch (_: ActivityNotFoundException) {
    false
  } catch (_: SecurityException) {
    false
  }
