package online.capitalai.devicecare.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * App-local on-demand inspection. No risk score is inferred from raw device
 * observations; an M1 preview does not claim malware/phone-health diagnosis.
 */
@Composable
fun DeviceCareHome(
    appName: String,
    enterprise: Boolean,
    onInspect: (() -> Unit)? = null,
    inspectionInProgress: Boolean = false,
    inspectionLines: List<String> = emptyList(),
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(appName, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (enterprise) "Business prototype - internal only"
                    else "Akku & App-Berechtigungen - M1 Entwickler-Vorschau",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                Text("Quality pipeline", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                Text("Build evidence: NOT_PROVEN until matched to this artifact.")
                Text("Device scoring pipeline", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                Text("Risk score: NOT_ASSESSABLE - no calibrated model or complete device evidence.")
                if (!enterprise && onInspect != null) {
                    Text("Lokale Prüfung auf ausdrücklichen Start: Akku und nur die von Android sichtbaren Apps. Keine vollständige App-Inventur. Keine automatischen Änderungen.")
                    Button(onClick = onInspect, enabled = !inspectionInProgress, modifier = Modifier.heightIn(min = 48.dp).testTag("inspection_start_button")) {
                        Text(if (inspectionInProgress) "Prüfung läuft" else "Lokale Prüfung starten")
                    }
                    Text("Keine Speicherung, kein Upload, kein Hintergrundscan und keine Spezialberechtigungen.")
                    Text("Momentaufnahme; Messwerte können nach 60 Sekunden veraltet sein.")
                    Column(modifier = Modifier.testTag("inspection_results").semantics { liveRegion = LiveRegionMode.Polite }) {
                        inspectionLines.forEach { line -> Text(line, style = MaterialTheme.typography.bodyMedium) }
                    }
                } else if (enterprise) {
                    Text("Fleet enrollment and organization features: NOT IMPLEMENTED.")
                }
                Text("Android permissions", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                Text("No sensitive Android permissions requested in this M1 preview.")
                Text("Cloud, analytics, VPN, malware scans and paid billing are not active.")
            }
        }
    }
}
