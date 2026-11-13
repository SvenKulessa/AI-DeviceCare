package online.capitalai.devicecare.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * An independent, user-initiated and offline-first security update workflow.
 * Manual handoffs never claim that another app was actually scanned or updated.
 */
@Composable
fun SystemSecurityWorkflowScreen(
    busy: Boolean,
    evidenceLines: List<String>,
    settingsMessage: String?,
    onBack: () -> Unit,
    onInspect: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onOpenServicesSettings: () -> Unit,
    onOpenWebViewSettings: () -> Unit,
    onOpenPlayStore: () -> Unit,
    onOpenGoogleReleases: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding()
                    .testTag("system_security_workflow")
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)
                    .testTag("system_security_back")) { Text("Zurück zur Übersicht") }
                Text("System- & Google-Sicherheitscheck",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium)
                Text("Eigenständiger Android-Workflow. Nur lokale Geräte-Metadaten, " +
                    "keine Speicherung, kein Upload, kein Hintergrundscan und keine " +
                    "automatischen Änderungen.")
                Button(onClick = onInspect, enabled = !busy, modifier = Modifier
                    .heightIn(min = 48.dp).testTag("system_security_start")) {
                    Text(if (busy) "Prüfung läuft" else "Versionsprüfung starten")
                }
                Text("Die App vergleicht keine Google-Releases live. Eine installierte " +
                    "Versionsnummer ist kein Nachweis für den neuesten Sicherheitsstand.")
                Column(modifier = Modifier.testTag("system_security_results")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (evidenceLines.isEmpty()) {
                        Text("Noch keine Prüfung ausgeführt (NOT_PROVEN).")
                    } else {
                        Text("Lokale Evidence", modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleMedium)
                        evidenceLines.forEach { Text(it) }
                    }
                }
                Text("Manuelle Schritte", modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge)
                Text("1. Android-/Google-Play-Systemupdate: Öffne Sicherheit und " +
                    "suche nach Google Play-Systemupdate sowie Hersteller-Systemupdate. " +
                    "Prüfe die Ergebnisse getrennt.")
                Button(onClick = onOpenSecuritySettings,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Sicherheitseinstellungen öffnen")
                }
                Text("2. Google Play-Dienste: Prüfe die App-Version und im Play Store " +
                    "die Beta-Mitgliedschaft. Ein Beta-Ausstieg erfolgt nur manuell. " +
                    "Keine Updates entfernen und keine Daten löschen.")
                Button(onClick = onOpenServicesSettings,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Play-Dienste App-Info öffnen")
                }
                Text("3. WebView: Prüfe die angezeigte Provider-Version und " +
                    "installiere Updates ausschließlich über den Play Store.")
                Button(onClick = onOpenWebViewSettings,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("WebView App-Info öffnen")
                }
                Text("4. Play Protect: Im Play Store Profil → Play Protect → Scannen " +
                    "öffnen. Kontrolliere zusätzlich die Geräte-Zertifizierung unter " +
                    "Play Store → Einstellungen → Info. Die App kann den Scanstatus " +
                    "nicht über öffentliche APIs bestätigen.")
                Button(onClick = onOpenPlayStore,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Play Store für Play Protect öffnen")
                }
                Text("5. Offizielle Google-Release-Notes und Android-Sicherheitsbulletins " +
                    "manuell vergleichen. Geräte- und OEM-Verfügbarkeit kann abweichen.")
                Button(onClick = onOpenGoogleReleases,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Google-Release-Notes öffnen")
                }
                if (settingsMessage != null) {
                    Text(settingsMessage,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Text("Alle Schritte sind lesend oder öffnen Android-/Google-Oberflächen. " +
                    "Kein Root, ADB, Reset, APK-Sideloading oder Fremdfirmware.")
            }
        }
    }
}
