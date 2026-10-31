package online.capitalai.devicecare.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * P0 status view. Deliberately does not imply that scans have been performed.
 * The business variant is internal-only until distinct functionality is proven.
 */
@Composable
fun DeviceCareHome(appName: String, enterprise: Boolean) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(appName, style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (enterprise) "Business prototype - internal only"
                    else "Device health & privacy - P0 developer preview",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                Text("Quality pipeline", style = MaterialTheme.typography.titleLarge)
                Text("Build evidence: NOT_PROVEN until matched to this artifact.")
                Text("Device scoring pipeline", style = MaterialTheme.typography.titleLarge)
                Text("Device security score: NOT_ASSESSABLE - no device signals collected.")
                Text("Android permissions", style = MaterialTheme.typography.titleLarge)
                Text("No sensitive permissions requested in this P0 build.")
                Text("Your data stays on this device. No cloud, analytics or VPN is active.")
                if (enterprise) {
                    Text("Fleet enrollment and organization features: NOT IMPLEMENTED.")
                }
            }
        }
    }
}
