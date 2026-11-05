package online.capitalai.devicecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.capitalai.devicecare.core.evidence.DeviceObservationBatch
import online.capitalai.devicecare.core.evidence.ObservationStatus
import online.capitalai.devicecare.core.ui.DeviceCareHome
import online.capitalai.devicecare.features.battery.AndroidBatteryCollector
import online.capitalai.devicecare.features.permissions.AndroidPermissionCollector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var busy by remember { mutableStateOf(false) }
            var lines by remember { mutableStateOf(emptyList<String>()) }
            val scope = rememberCoroutineScope()
            DeviceCareHome(
                appName = "AI-DeviceCare",
                enterprise = false,
                inspectionInProgress = busy,
                inspectionLines = lines,
                onInspect = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    val battery = AndroidBatteryCollector(applicationContext).collect()
                                    val permissions = AndroidPermissionCollector(applicationContext).collect()
                                    battery to permissions
                                }
                                lines = showBatch(result.first) + showBatch(result.second)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: RuntimeException) {
                                lines = listOf("Lokale Prüfung nicht verfügbar. Keine Daten gespeichert.")
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

/** Never display/record raw installed-package names, identifiers or exceptions. */
private fun showBatch(batch: DeviceObservationBatch): List<String> =
    listOf("Quelle: ${batch.collectorId} · ${batch.scope} · ${batch.contractVersion}") +
        batch.observations.map { observation ->
            val display = if (observation.status == ObservationStatus.OBSERVED)
                "${observation.value}${observation.unit?.let { " $it" } ?: ""}"
            else "${observation.status} (${observation.limitationCode})"
            "${observation.id}: $display"
        }
