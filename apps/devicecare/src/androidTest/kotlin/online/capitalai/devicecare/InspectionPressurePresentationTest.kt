package online.capitalai.devicecare

import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import online.capitalai.devicecare.core.evidence.ObservationStatus
import online.capitalai.devicecare.core.ui.DeviceCareHome
import online.capitalai.devicecare.features.battery.BatteryEvidenceMapper
import online.capitalai.devicecare.features.permissions.PermissionEvidenceMapper
import online.capitalai.devicecare.features.permissions.PermissionReading
import online.capitalai.devicecare.features.permissions.PermissionScanBudget
import online.capitalai.devicecare.features.permissions.PermissionScanProfile
import online.capitalai.devicecare.features.permissions.ResourceSignal
import org.junit.Rule
import org.junit.Test

class InspectionPressurePresentationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun deferredSampleExplainsPressureAndShowsActualCount() {
        showFixture(sampledPackages = 50, optionalReadsDeferred = true)
        pressureNotice().assertDoesNotExist()

        startInspection()

        pressureNotice().performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("sampled_packages: 50 apps").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("sample_truncated: true").performScrollTo().assertIsDisplayed()
    }

    @Test fun plannedSampleCapDoesNotShowPressureNotice() {
        showFixture(sampledPackages = 75, optionalReadsDeferred = false)

        startInspection()

        rule.onNodeWithText("sampled_packages: 75 apps").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("sample_truncated: true").performScrollTo().assertIsDisplayed()
        pressureNotice().assertDoesNotExist()
    }

    private fun startInspection() {
        rule.onNodeWithTag("inspection_start_button").performScrollTo().performClick()
    }

    private fun pressureNotice() = rule.onNodeWithText(
        "Die Stichprobe wurde wegen Speicherdruck oder hoher thermischer Belastung während der Prüfung verkürzt.",
    )

    private fun showFixture(sampledPackages: Int, optionalReadsDeferred: Boolean) {
        val now = System.currentTimeMillis()
        val profile = PermissionScanProfile(
            apiLevel = Build.VERSION.SDK_INT,
            observedAtMillis = now,
            lowRam = ResourceSignal.CLEAR,
            memoryPressure = ResourceSignal.CLEAR,
            powerSaver = ResourceSignal.ACTIVE,
            thermal = ResourceSignal.CLEAR,
        )
        val inspection = presentInspection(
            profile = profile,
            budget = PermissionScanBudget.forProfile(profile),
            battery = BatteryEvidenceMapper.missing(now, ObservationStatus.UNSUPPORTED, "OEM_UNAVAILABLE"),
            permissions = PermissionEvidenceMapper.map(now, PermissionReading(
                visiblePackages = 100,
                sampledPackages = sampledPackages,
                requestedInSample = sampledPackages * 2,
                ownRequested = 0,
                ownGranted = 0,
                truncatedByLimit = true,
            )),
            optionalReadsDeferred = optionalReadsDeferred,
        )
        rule.runOnUiThread {
            rule.activity.setContent {
                var started by remember { mutableStateOf(false) }
                DeviceCareHome(
                    appName = "AI-DeviceCare",
                    enterprise = false,
                    onInspect = { started = true },
                    inspectionLines = if (started) inspection.resultLines else emptyList(),
                    inspectionProfileLines = if (started) inspection.profileLines else emptyList(),
                )
            }
        }
    }
}
