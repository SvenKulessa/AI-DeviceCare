package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.devicecare.AndroidSystemSecurityReader
import com.example.devicecare.SettingsLaunchResult
import com.example.devicecare.openDeviceSettings
import com.example.devicecare.ownAppSettings
import com.example.devicecare.presentSystemSecurity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entirely transient screen state. Data collection begins only on explicit user action. */
@Composable
fun SystemSecurityFeatureScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }
  var evidence by remember { mutableStateOf(emptyList<String>()) }
  var webViewProvider by remember { mutableStateOf<String?>(null) }
  var message by remember { mutableStateOf<String?>(null) }

  fun open(intent: Intent, fallback: String): String? =
    when (openDeviceSettings(intent) { context.startActivity(it) }) {
      SettingsLaunchResult.DIRECT -> null
      SettingsLaunchResult.GENERAL_SETTINGS -> "Zielseite nicht verfügbar. $fallback"
      SettingsLaunchResult.UNAVAILABLE -> "Einstellungen nicht verfügbar. $fallback"
    }

  SystemSecurityWorkflowScreen(
    busy = busy,
    evidenceLines = evidence,
    settingsMessage = message,
    onBack = { if (!busy) onBack() },
    onInspect = {
      if (!busy) {
        busy = true
        message = null
        scope.launch {
          try {
            val snapshot = withContext(Dispatchers.IO) {
              AndroidSystemSecurityReader(context.applicationContext).read()
            }
            webViewProvider = snapshot.webViewProvider
            evidence = presentSystemSecurity(snapshot)
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (_: RuntimeException) {
            evidence = listOf("Geräteabfrage fehlgeschlagen: NOT_PROVEN. Keine Daten gespeichert.")
          } finally {
            busy = false
          }
        }
      }
    },
    onOpenSecuritySettings = {
      message = open(Intent(Settings.ACTION_SECURITY_SETTINGS),
        "Öffne Sicherheit und prüfe Google Play-Systemupdate und Hersteller-OTA manuell.")
    },
    onOpenServicesSettings = {
      message = open(ownAppSettings("com.google.android.gms"), "Apps → Google Play-Dienste → App-Info.")
    },
    onOpenWebViewSettings = {
      message = open(ownAppSettings(webViewProvider ?: "com.google.android.webview"),
        "Apps → aktiver WebView-Provider.")
    },
    onOpenPlayStore = {
      val intent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
      message = if (intent == null) "Play Store nicht verfügbar. Play Protect manuell öffnen."
        else open(intent, "Play Store → Profil → Play Protect.")
    },
    onOpenGoogleReleases = {
      message = open(Intent(Intent.ACTION_VIEW,
        Uri.parse("https://support.google.com/product-documentation/answer/14343500?hl=de")),
        "Offizielle Google-System-Release-Notes im Browser suchen.")
    },
  )
}
