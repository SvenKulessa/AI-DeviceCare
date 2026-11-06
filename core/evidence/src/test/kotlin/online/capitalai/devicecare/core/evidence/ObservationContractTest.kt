package online.capitalai.devicecare.core.evidence

import org.junit.Assert.*
import org.junit.Test

class ObservationContractTest {
    @Test fun measuredSignalIsVersionedAndExpires() {
        val signal = measuredObservation("battery_percent", "42", "%", 1_000L, "NONE")
        assertEquals(ObservationContract.VERSION, signal.contractVersion)
        assertTrue(signal.isFresh(1_000L))
        assertFalse(signal.isFresh(61_001L))
    }

    @Test fun missingSensorCannotCarryAFakedValue() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceObservation("battery_percent", ObservationStatus.UNSUPPORTED, 20, null, "0", "%", "OEM_UNAVAILABLE")
        }
    }

    @Test fun duplicateObservationsFailClosed() {
        val signal = measuredObservation("battery_percent", "45", "%", 10, "NONE")
        assertThrows(IllegalArgumentException::class.java) {
            DeviceObservationBatch("battery", 10, "LOCAL_BATTERY", listOf(signal, signal))
        }
    }

    @Test fun invalidCollectorScopeCannotBeUsedAsProof() {
        val signal = unavailableObservation("battery_percent", ObservationStatus.ERROR, 10, "READ_FAILED")
        assertThrows(IllegalArgumentException::class.java) {
            DeviceObservationBatch("battery", 10, "ALL_DEVICE_CONTENTS", listOf(signal))
        }
    }

    @Test fun unavailableSignalsAreNotFresh() {
        val denied = unavailableObservation("apps_visible", ObservationStatus.DENIED, 10, "ACCESS_DENIED")
        assertFalse(denied.isFresh(10))
        assertNull(denied.value)
    }

    @Test fun rawPackageNamesAndDeviceIdsAreNotValidObservations() {
        for (value in listOf("com.example.private", "serial12345678", "user@example.com")) {
            assertThrows(IllegalArgumentException::class.java) {
                measuredObservation("visible_packages", value, "apps", 100, "ANDROID_VISIBILITY_FILTERED")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            measuredObservation("raw_package_id", "123", null, 100, "NONE")
        }
    }

    @Test fun batteryPercentAndTemperatureArePhysicallyBounded() {
        for (value in listOf("-1", "101", "0042", "NaN")) {
            assertThrows(IllegalArgumentException::class.java) {
                measuredObservation("battery_percent", value, "%", 100, "NONE")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            measuredObservation("battery_temperature", "99.9", "C", 100, "NONE")
        }
    }

    @Test fun observationTtlAndCollectorScopeCannotExpand() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceObservation("battery_percent", ObservationStatus.OBSERVED, 100,
                100 + ObservationContract.MAX_AGE_MILLIS + 1, "50", "%", "NONE")
        }
        val signal = measuredObservation("battery_percent", "50", "%", 100, "NONE")
        assertThrows(IllegalArgumentException::class.java) {
            DeviceObservationBatch("permissions", 100, "ANDROID_VISIBLE_APPS", listOf(signal))
        }
    }

    @Test fun unobservedSignalsCannotCarryAnyData() {
        for (status in listOf(ObservationStatus.DENIED, ObservationStatus.ERROR, ObservationStatus.UNSUPPORTED)) {
            assertThrows(IllegalArgumentException::class.java) {
                DeviceObservation("battery_percent", status, 100, null, "0", null, "READ_FAILED")
            }
        }
    }

    @Test fun samplerLimitIsValidatedAtEvidenceBoundary() {
        assertThrows(IllegalArgumentException::class.java) {
            measuredObservation("sampled_packages", "251", "apps", 100, "ANDROID_VISIBILITY_FILTERED")
        }
        val valid = measuredObservation("sampled_packages", "250", "apps", 100, "ANDROID_VISIBILITY_FILTERED")
        assertEquals("250", valid.value)
    }

}
