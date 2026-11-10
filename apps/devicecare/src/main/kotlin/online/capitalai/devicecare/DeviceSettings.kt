package online.capitalai.devicecare

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal enum class SettingsLaunchResult { DIRECT, GENERAL_SETTINGS, UNAVAILABLE }

internal fun ownAppSettings(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

internal fun openDeviceSettings(primary: Intent, launch: (Intent) -> Unit): SettingsLaunchResult {
    // Launch directly because package visibility can hide an otherwise usable handler.
    if (tryLaunch(primary, launch)) return SettingsLaunchResult.DIRECT
    if (tryLaunch(Intent(Settings.ACTION_SETTINGS), launch)) return SettingsLaunchResult.GENERAL_SETTINGS
    return SettingsLaunchResult.UNAVAILABLE
}

private fun tryLaunch(intent: Intent, launch: (Intent) -> Unit): Boolean = try {
    launch(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
