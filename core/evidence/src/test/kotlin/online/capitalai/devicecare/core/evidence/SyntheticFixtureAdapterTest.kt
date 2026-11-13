package online.capitalai.devicecare.core.evidence

import org.junit.Assert.*
import org.junit.Test

/**
 * TEST-ONLY reference oracle for Issue #13.
 * No Android permissions, I/O, device discovery, production scores, remediation
 * or malware detection. This code is compiled only with the unit-test source set.
 */
class SyntheticFixtureAdapterTest {
    private enum class Gate {
        NO_MALWARE_EVIDENCE, REVIEW_ONLY, SIMULATION_REVIEW, NOT_ASSESSABLE
    }

    private data class Fixture(
        val id: String,
        val expected: Gate,
        val permissions: List<String> = emptyList(),
        val events: Map<String, Int> = emptyMap(),
        val coverage: String = "full_test_fixture",
        val evidenceFresh: Boolean = true,
        val schema: String = SCHEMA,
    )

    companion object {
        private const val SCHEMA = "devicecare-synthetic-qa-fixtures/1"
        private val allowedPermissions = setOf(
            "android.permission.CAMERA",
            "android.permission.READ_SMS",
            "android.permission.POST_NOTIFICATIONS",
        )
        private val allowedEvents = setOf(
            "synthetic_abnormal_access_count",
            "synthetic_rename_count",
            "synthetic_encryption_hint",
            "synthetic_benign_backup_count",
            "synthetic_normal_background_count",
        )

        /**
         * This is a synthetic test gate, NOT a classifier or a claim that a
         * real device is healthy. Unknown or stale input must fail closed.
         */
        private fun assessFixture(f: Fixture): Gate {
            if (f.schema != SCHEMA || !f.evidenceFresh ||
                f.coverage !in setOf("full_test_fixture", "known_subset")
            ) return Gate.NOT_ASSESSABLE
            if (f.permissions.any { it !in allowedPermissions } ||
                f.events.any { (kind, count) -> kind !in allowedEvents || count !in 0..250 }
            ) return Gate.NOT_ASSESSABLE

            if (f.coverage == "known_subset") {
                return if (f.permissions.isNotEmpty()) Gate.REVIEW_ONLY else Gate.NOT_ASSESSABLE
            }

            if ((f.events["synthetic_rename_count"] ?: 0) > 0 &&
                (f.events["synthetic_encryption_hint"] ?: 0) > 0
            ) return Gate.SIMULATION_REVIEW

            // A permission is never sufficient evidence of malware.
            if (f.permissions.isNotEmpty()) return Gate.REVIEW_ONLY

            // This label is specific to a fully specified artificial test case.
            if (f.events.isEmpty() ||
                (f.events.keys == setOf("synthetic_benign_backup_count"))
            ) return Gate.NO_MALWARE_EVIDENCE

            return Gate.REVIEW_ONLY
        }

        private val referenceCases = listOf(
            Fixture("SYN-001", Gate.REVIEW_ONLY,
                permissions = listOf("android.permission.CAMERA"), coverage = "known_subset"),
            Fixture("SYN-002", Gate.NO_MALWARE_EVIDENCE),
            Fixture("SYN-003", Gate.REVIEW_ONLY,
                permissions = listOf("android.permission.READ_SMS"),
                events = mapOf("synthetic_abnormal_access_count" to 3)),
            Fixture("SYN-004", Gate.SIMULATION_REVIEW,
                events = mapOf("synthetic_rename_count" to 25, "synthetic_encryption_hint" to 1)),
            Fixture("SYN-005", Gate.NO_MALWARE_EVIDENCE,
                events = mapOf("synthetic_benign_backup_count" to 50)),
            Fixture("SYN-006", Gate.NOT_ASSESSABLE,
                permissions = listOf("android.permission.CAMERA"), evidenceFresh = false),
            Fixture("SYN-007", Gate.NOT_ASSESSABLE, coverage = "unknown"),
            Fixture("SYN-008", Gate.NOT_ASSESSABLE, coverage = "private_space_unavailable"),
            Fixture("SYN-009", Gate.REVIEW_ONLY,
                permissions = listOf("android.permission.POST_NOTIFICATIONS"),
                events = mapOf("synthetic_normal_background_count" to 7)),
        )
    }

    @Test fun allNineVersionedReferenceFixturesMatchExpectedSafetyGates() {
        assertEquals(9, referenceCases.size)
        assertEquals((1..9).map { "SYN-" + it.toString().padStart(3, '0') },
            referenceCases.map { it.id })
        referenceCases.forEach { fixture ->
            assertEquals(fixture.id, fixture.expected, assessFixture(fixture))
            assertEquals(SCHEMA, fixture.schema)
        }
    }

    @Test fun permissionsAloneNeverProduceAnAutomaticProtectionDecision() {
        val emptyFixture = referenceCases.first { it.id == "SYN-002" }
        for (permission in allowedPermissions) {
            val permissionOnly = emptyFixture.copy(permissions = listOf(permission))
            assertEquals(permission, Gate.REVIEW_ONLY, assessFixture(permissionOnly))
        }
    }

    @Test fun artificialRenameAndEncryptionHintsRemainSimulationOnly() {
        val ransomwareLike = referenceCases.first { it.id == "SYN-004" }
        assertEquals(Gate.SIMULATION_REVIEW, assessFixture(ransomwareLike))
        val partial = ransomwareLike.copy(coverage = "known_subset")
        assertEquals(Gate.NOT_ASSESSABLE, assessFixture(partial))
        assertEquals(Gate.REVIEW_ONLY, assessFixture(partial.copy(
            permissions = listOf("android.permission.CAMERA"))))
        assertEquals(Gate.REVIEW_ONLY, assessFixture(ransomwareLike.copy(
            events = mapOf("synthetic_rename_count" to 25))))
    }

    @Test fun legitimateBackupBurstCannotProduceMalwareVerdict() {
        assertEquals(Gate.NO_MALWARE_EVIDENCE,
            assessFixture(referenceCases.first { it.id == "SYN-005" }))
    }

    @Test fun staleUnobservableIncompleteAndUnknownSignalsFailClosed() {
        val fixture = referenceCases.first { it.id == "SYN-002" }
        assertEquals(Gate.NOT_ASSESSABLE, assessFixture(fixture.copy(evidenceFresh = false)))
        assertEquals(Gate.NOT_ASSESSABLE, assessFixture(fixture.copy(coverage = "known_subset")))
        assertEquals(Gate.NOT_ASSESSABLE, assessFixture(fixture.copy(coverage = "unknown")))
        assertEquals(Gate.NOT_ASSESSABLE,
            assessFixture(fixture.copy(coverage = "private_space_unavailable")))
        assertEquals(Gate.NOT_ASSESSABLE, assessFixture(fixture.copy(schema = "unknown/2")))
        assertEquals(Gate.NOT_ASSESSABLE,
            assessFixture(fixture.copy(events = mapOf("unexpected_runtime_event" to 1))))
        assertEquals(Gate.NOT_ASSESSABLE,
            assessFixture(fixture.copy(events = mapOf("synthetic_rename_count" to -1))))
        assertEquals(Gate.NOT_ASSESSABLE,
            assessFixture(fixture.copy(events = mapOf("synthetic_rename_count" to 251))))
        assertEquals(Gate.NOT_ASSESSABLE,
            assessFixture(fixture.copy(permissions = listOf("com.example.sensitive.app"))))
    }

    @Test fun fixtureEventsAndRawIdentifiersCannotEnterTheRealM1Contract() {
        val now = 1_000L
        for (id in listOf("raw_package_id", "synthetic_rename_count",
            "synthetic_encryption_hint", "synthetic_abnormal_access_count")) {
            assertThrows(IllegalArgumentException::class.java) {
                measuredObservation(id, "1", "count", now, "NONE")
            }
        }
        for (pii in listOf("com.example.sensitive.app", "user@example.com", "android-id-123")) {
            assertThrows(IllegalArgumentException::class.java) {
                measuredObservation("visible_packages", pii, "apps", now, "NONE")
            }
        }
        val unknown = unavailableObservation(
            "sampled_packages", ObservationStatus.UNSUPPORTED, now, "FLAGS_UNAVAILABLE")
        assertThrows(IllegalArgumentException::class.java) {
            DeviceObservationBatch("malware", now, "LOCAL_MALWARE", listOf(unknown))
        }
    }

    @Test fun expiredDeniedOrUnsupportedRealM1EvidenceNeverAppearsFresh() {
        val now = 1_000L
        val observed = measuredObservation(
            "sampled_packages", "2", "apps", now, "ANDROID_VISIBILITY_FILTERED")
        assertFalse(observed.isFresh(now + ObservationContract.MAX_AGE_MILLIS + 1))
        for (status in listOf(
            ObservationStatus.DENIED, ObservationStatus.UNSUPPORTED, ObservationStatus.ERROR
        )) {
            val missing = unavailableObservation(
                "sampled_packages", status, now, "READ_FAILED")
            assertFalse(missing.isFresh(now))
            assertNull(missing.value)
        }
    }
}
