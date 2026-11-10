package online.capitalai.devicecare

import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.capitalai.devicecare.core.ui.DeviceCareHome
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
            val scope = rememberCoroutineScope()
            DeviceCareHome(
                appName = "AI-DeviceCare",
                enterprise = false,
                inspectionInProgress = busy,
                inspectionLines = inspection.resultLines,
                inspectionProfileLines = inspection.profileLines,
                onInspect = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            val startElapsed = SystemClock.elapsedRealtime()
                            val startCpu = Process.getElapsedCpuTime()
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    val profile = AndroidPermissionScanProfile(applicationContext).read()
                                    val budget = PermissionScanBudget.forProfile(profile)
                                    val battery = AndroidBatteryCollector(applicationContext).collect()
                                    val permissions = AndroidPermissionCollector(applicationContext).collect(budget.packageLimit)
                                    presentInspection(profile, budget, battery, permissions)
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
