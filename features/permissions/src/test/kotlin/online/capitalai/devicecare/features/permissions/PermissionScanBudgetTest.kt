package online.capitalai.devicecare.features.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionScanBudgetTest {
    private val clear = PermissionScanProfile(
        apiLevel = 35, observedAtMillis = 100,
        lowRam = ResourceSignal.CLEAR, memoryPressure = ResourceSignal.CLEAR,
        powerSaver = ResourceSignal.CLEAR, thermal = ResourceSignal.CLEAR,
    )

    @Test fun knownClearSignalsAllowStandardScopeAcrossModernApis() {
        for (api in listOf(29, 33, 35, 36)) {
            assertEquals(PermissionScanBudget(250, emptyList()), PermissionScanBudget.forProfile(clear.copy(apiLevel = api)))
        }
    }

    @Test fun missingOrDeniedSignalsReduceScope() {
        val legacy = clear.copy(apiLevel = 26, thermal = ResourceSignal.UNSUPPORTED)
        assertEquals(PermissionScanBudget(75, listOf(PermissionScanReason.SIGNAL_UNAVAILABLE)),
            PermissionScanBudget.forProfile(legacy))
        for (signal in listOf(ResourceSignal.DENIED, ResourceSignal.ERROR, ResourceSignal.UNKNOWN)) {
            assertEquals(75, PermissionScanBudget.forProfile(clear.copy(memoryPressure = signal)).packageLimit)
        }
    }

    @Test fun lowRamPowerSaverAndModerateHeatAreIndependentlyBounded() {
        val cases = listOf(
            clear.copy(lowRam = ResourceSignal.ACTIVE) to PermissionScanReason.LOW_RAM,
            clear.copy(powerSaver = ResourceSignal.ACTIVE) to PermissionScanReason.POWER_SAVER,
            clear.copy(thermal = ResourceSignal.ACTIVE) to PermissionScanReason.MODERATE_THERMAL,
        )
        for ((profile, reason) in cases) {
            assertEquals(PermissionScanBudget(75, listOf(reason)), PermissionScanBudget.forProfile(profile))
        }
    }

    @Test fun memoryPressureOrSevereHeatTakesPrecedenceOverOtherSignals() {
        val saver = clear.copy(powerSaver = ResourceSignal.ACTIVE)
        assertEquals(PermissionScanBudget(25, listOf(PermissionScanReason.MEMORY_PRESSURE, PermissionScanReason.POWER_SAVER)),
            PermissionScanBudget.forProfile(saver.copy(memoryPressure = ResourceSignal.ACTIVE)))
        assertEquals(PermissionScanBudget(25, listOf(PermissionScanReason.THERMAL_PRESSURE, PermissionScanReason.POWER_SAVER)),
            PermissionScanBudget.forProfile(saver.copy(thermal = ResourceSignal.HIGH)))
    }
}
