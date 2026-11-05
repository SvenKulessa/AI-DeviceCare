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
}
