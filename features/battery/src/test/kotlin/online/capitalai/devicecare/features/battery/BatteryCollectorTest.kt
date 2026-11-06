package online.capitalai.devicecare.features.battery

import online.capitalai.devicecare.core.evidence.ObservationStatus
import org.junit.Assert.*
import org.junit.Test

class BatteryCollectorTest {
    @Test fun validBroadcastIsMappedWithoutRiskAssumptions() {
        val batch = BatteryEvidenceMapper.map(1000, BatteryReading(46, 100, null, 319, 2, 2))
        assertEquals("devicecare-observation/1", batch.contractVersion)
        assertEquals("46", batch.observations.first { it.id == "battery_percent" }.value)
        assertEquals("31.9", batch.observations.first { it.id == "battery_temperature" }.value)
        assertEquals("CHARGING", batch.observations.first { it.id == "battery_charging" }.value)
        assertEquals("GOOD", batch.observations.first { it.id == "battery_health_reported" }.value)
    }

    @Test fun unknownBatteryAndAbsentSensorsNeverBecomeHealthyMeasurements() {
        val batch = BatteryEvidenceMapper.map(10, BatteryReading(-1, 100, -2147483648, null, 1, 1))
        assertTrue(batch.observations.all { it.status == ObservationStatus.UNSUPPORTED && it.value == null })
    }

    @Test fun fallbackCapacityIsAllowedOnlyWhenSane() {
        val batch = BatteryEvidenceMapper.map(10, BatteryReading(null, null, 85, -1000, null, null))
        assertEquals("85", batch.observations.first { it.id == "battery_percent" }.value)
        assertEquals(ObservationStatus.UNSUPPORTED, batch.observations.first { it.id == "battery_temperature" }.status)
    }

    @Test fun missingBroadcastIsNotFaked() {
        val batch = BatteryEvidenceMapper.missing(0, ObservationStatus.UNSUPPORTED, "BROADCAST_UNAVAILABLE")
        assertTrue(batch.observations.all { it.value == null && it.status == ObservationStatus.UNSUPPORTED })
    }
}
