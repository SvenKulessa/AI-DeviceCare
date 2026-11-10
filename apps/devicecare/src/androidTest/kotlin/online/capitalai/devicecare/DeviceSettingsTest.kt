package online.capitalai.devicecare

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceSettingsTest {
    private val powerSaver = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)

    @Test fun directLaunchDoesNotOpenGeneralSettings() {
        val attempted = mutableListOf<Intent>()
        val result = openDeviceSettings(powerSaver) { attempted.add(it) }
        assertEquals(SettingsLaunchResult.DIRECT, result)
        assertEquals(listOf(Settings.ACTION_BATTERY_SAVER_SETTINGS), attempted.map { it.action })
    }

    @Test fun missingHandlerFallsBackToGeneralSettings() {
        val actions = mutableListOf<String?>()
        val result = openDeviceSettings(powerSaver) {
            actions.add(it.action)
            if (it.action == Settings.ACTION_BATTERY_SAVER_SETTINGS) throw ActivityNotFoundException()
        }
        assertEquals(SettingsLaunchResult.GENERAL_SETTINGS, result)
        assertEquals(listOf(Settings.ACTION_BATTERY_SAVER_SETTINGS, Settings.ACTION_SETTINGS), actions)
    }

    @Test fun deniedAndMissingHandlersReturnUnavailable() {
        val result = openDeviceSettings(powerSaver) {
            if (it.action == Settings.ACTION_BATTERY_SAVER_SETTINGS) throw SecurityException()
            throw ActivityNotFoundException()
        }
        assertEquals(SettingsLaunchResult.UNAVAILABLE, result)
    }

    @Test fun appInfoTargetsOnlyTheSuppliedOwnPackage() {
        val intent = ownAppSettings("online.capitalai.devicecare")
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:online.capitalai.devicecare", intent.data.toString())
    }
}
