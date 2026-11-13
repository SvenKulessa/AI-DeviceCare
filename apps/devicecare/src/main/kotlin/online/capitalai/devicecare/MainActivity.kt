package online.capitalai.devicecare

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.capitalai.devicecare.core.ui.DeviceCareHome
import online.capitalai.devicecare.core.ui.SystemSecurityWorkflowScreen
import online.capitalai.devicecare.features.battery.AndroidBatteryCollector
import online.capitalai.devicecare.features.permissions.AndroidPermissionCollector
import online.capitalai.devicecare.features.permissions.AndroidPermissionScanProfile
import online.capitalai.devicecare.features.permissions.PermissionScanBudget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The preview uses a light Material surface in both system themes.
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent {
            var busy by remember { mutableStateOf(false) }
            var inspection by remember { mutableStateOf(InspectionDisplay()) }
            var settingsMessage by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            var systemSecurityOpen by remember { mutableStateOf(false) }
            var systemSecurityBusy by remember { mutableStateOf(false) }
            var systemSecurityLines by remember { mutableStateOf(emptyList<String>()) }
            var webViewProvider by remember { mutableStateOf<String?>(null) }
            if (systemSecurityOpen) {
                SystemSecurityWorkflowScreen(
                    busy = systemSecurityBusy,
                    evidenceLines = systemSecurityLines,
                    settingsMessage = settingsMessage,
                    onBack = {
                        if (!systemSecurityBusy) {
                            settingsMessage = null
                            systemSecurityLines = emptyList()
                            webViewProvider = null
                            systemSecurityOpen = false
                        }
                    },
                    onInspect = {
                        if (!systemSecurityBusy) {
                            systemSecurityBusy = true
                            settingsMessage = null
                            scope.launch {
                                try {
                                    val snapshot = withContext(Dispatchers.IO) {
                                        AndroidSystemSecurityReader(applicationContext).read()
                                    }
                                    webViewProvider = snapshot.webViewProvider
                                    systemSecurityLines = presentSystemSecurity(snapshot)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: RuntimeException) {
                                    systemSecurityLines = listOf(
                                        "Geräteabfrage fehlgeschlagen: NOT_PROVEN. Keine Daten gespeichert."
                                    )
                                } finally {
                                    systemSecurityBusy = false
                                }
                            }
                        }
                    },
                    onOpenSecuritySettings = {
                        settingsMessage = openSettings(
                            Intent(Settings.ACTION_SECURITY_SETTINGS),
                            "Öffne Sicherheit → Google Play-Systemupdate beziehungsweise Systemupdate manuell.",
                        )
                    },
                    onOpenServicesSettings = {
                        settingsMessage = openSettings(
                            ownAppSettings("com.google.android.gms"),
                            "Apps → Google Play-Dienste → App-Info.",
                        )
                    },
                    onOpenWebViewSettings = {
                        settingsMessage = openSettings(
                            ownAppSettings(webViewProvider ?: "com.google.android.webview"),
                            "Apps → Android System WebView oder aktiven WebView-Provider.",
                        )
                    },
                    onOpenPlayStore = {
                        val launch = packageManager.getLaunchIntentForPackage("com.android.vending")
                        settingsMessage = if (launch == null) {
                            "Play Store nicht verfügbar. Prüfe Play Protect und Betaprogramm manuell."
                        } else {
                            openSettings(launch, "Play Store → Profil → Play Protect.") ?:
                                "Im Play Store Profil → Play Protect beziehungsweise Google Play-Dienste öffnen."
                        }
                    },
                    onOpenGoogleReleases = {
                        settingsMessage = openSettings(
                            Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://support.google.com/product-documentation/answer/14343500?hl=de"
                            )),
                            "Offizielle Google-System-Release-Notes im Browser suchen.",
                        )
                    },
                )
            } else {
            DeviceCareHome(
                appName = "AI-DeviceCare",
                enterprise = false,
                onOpenSystemSecurity = {
                    settingsMessage = null
                    systemSecurityOpen = true
                },
                inspectionInProgress = busy,
                inspectionLines = inspection.resultLines,
                inspectionProfileLines = inspection.profileLines,
                settingsMessage = settingsMessage,
                onOpenBatterySettings = {
                    settingsMessage = openSettings(
                        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                        "Suche in den Einstellungen nach Energiesparmodus.",
                    )
                },
                onOpenAppSettings = {
                    settingsMessage = openSettings(
                        ownAppSettings(packageName),
                        "Öffne in den Einstellungen Apps → AI-DeviceCare.",
                    )
                },
                onInspect = {
                    if (!busy) {
                        busy = true
                        settingsMessage = null
                        scope.launch {
                            val startElapsed = SystemClock.elapsedRealtime()
                            val startCpu = Process.getElapsedCpuTime()
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    val scanProfile = AndroidPermissionScanProfile(applicationContext)
                                    val profile = scanProfile.read()
                                    val budget = PermissionScanBudget.forProfile(profile)
                                    val battery = AndroidBatteryCollector(applicationContext).collect()
                                    var deferred = false
                                    val permissions = AndroidPermissionCollector(applicationContext).collect(
                                        sampleLimit = budget.packageLimit,
                                        shouldDeferOptionalReads = {
                                            scanProfile.hasHighPressure().also { deferred = it }
                                        },
                                        checkCancellation = { coroutineContext.ensureActive() },
                                    )
                                    presentInspection(profile, budget, battery, permissions, deferred)
                                }
                                val elapsedMs = (SystemClock.elapsedRealtime() - startElapsed).coerceAtLeast(0)
                                val cpuMs = (Process.getElapsedCpuTime() - startCpu).coerceAtLeast(0)
                                inspection = result.copy(resultLines = result.resultLines +
                                    "Messaufwand: ${elapsedMs} ms Laufzeit; ${cpuMs} ms Prozess-CPU (nur informativ, keine Akkuenergie-Messung).")
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: RuntimeException) {
                                inspection = InspectionDisplay(
                                    resultLines = listOf("Lokale Prüfung nicht verfügbar. Keine Daten gespeichert."),
                                )
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
            )
            }
        }
    }

    private fun openSettings(intent: Intent, fallbackHint: String): String? =
        when (openDeviceSettings(intent) { startActivity(it) }) {
            SettingsLaunchResult.DIRECT -> null
            SettingsLaunchResult.GENERAL_SETTINGS -> "Die Zielseite ist nicht verfügbar. $fallbackHint"
            SettingsLaunchResult.UNAVAILABLE -> "Android-Einstellungen konnten nicht geöffnet werden. $fallbackHint"
        }
}
