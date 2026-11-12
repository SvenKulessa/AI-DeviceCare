package online.capitalai.devicecare.features.permissions

import java.util.concurrent.CancellationException
import online.capitalai.devicecare.core.evidence.ObservationStatus
import org.junit.Assert.*
import org.junit.Test

class PermissionCollectorTest {
    @Test fun cancellationStopsBeforeReadingAnotherSampledPackage() {
        val reads = mutableListOf<String>()
        val cancellation = CancellationException()
        val thrown = assertThrows(CancellationException::class.java) {
            readSampledPermissionRequests(
                sampled = listOf("first", "second"),
                checkCancellation = { if (reads.isNotEmpty()) throw cancellation },
            ) { entry ->
                reads.add(entry)
                1
            }
        }

        assertSame(cancellation, thrown)
        assertEquals(listOf("first"), reads)
    }

    @Test fun risingPressureDefersRemainingPackageReads() {
        val reads = mutableListOf<Int>()
        val checkpoints = mutableListOf<Int>()
        val sample = readSampledPermissionRequests(
            sampled = (1..75).toList(),
            checkCancellation = {},
            shouldDeferOptionalReads = {
                checkpoints.add(reads.size)
                reads.size == 50
            },
        ) { entry ->
            reads.add(entry)
            2
        }

        assertEquals(listOf(25, 50), checkpoints)
        assertEquals((1..50).toList(), reads)
        assertEquals(PermissionSample(50, 100), sample)
    }

    @Test fun clearCheckpointsKeepThePlannedSample() {
        var checks = 0
        val sample = readSampledPermissionRequests(
            sampled = (1..75).toList(),
            checkCancellation = {},
            shouldDeferOptionalReads = {
                checks++
                false
            },
        ) { 2 }

        assertEquals(2, checks)
        assertEquals(PermissionSample(75, 150), sample)
    }

    @Test fun scopedCountsExposeTheirScopeWithoutInventingOtherApps() {
        val batch = PermissionEvidenceMapper.map(50L, PermissionReading(
            visiblePackages = 16, sampledPackages = 16, requestedInSample = 52,
            ownRequested = 2, ownGranted = 1, truncatedByLimit = false,
        ))
        assertEquals("ANDROID_VISIBLE_APPS", batch.scope)
        assertEquals("16", batch.observations.first { it.id == "visible_packages" }.value)
        assertEquals("ANDROID_VISIBILITY_FILTERED",
            batch.observations.first { it.id == "visible_packages" }.limitationCode)
    }

    @Test fun limitedSamplingIsExplicit() {
        val batch = PermissionEvidenceMapper.map(10L, PermissionReading(
            visiblePackages = 300, sampledPackages = 250, requestedInSample = 1200,
            ownRequested = 0, ownGranted = 0, truncatedByLimit = true,
        ))
        assertEquals("true", batch.observations.first { it.id == "sample_truncated" }.value)
        assertEquals("250", batch.observations.first { it.id == "sampled_packages" }.value)
    }

    @Test fun missingPermissionFlagsAreNotAssumedGrantedOrDenied() {
        val batch = PermissionEvidenceMapper.map(10L, PermissionReading(
            visiblePackages = 2, sampledPackages = 2, requestedInSample = 4,
            ownRequested = 3, ownGranted = null, truncatedByLimit = false,
        ))
        val granted = batch.observations.first { it.id == "own_granted_permissions" }
        assertEquals(ObservationStatus.UNSUPPORTED, granted.status)
        assertNull(granted.value)
    }

    @Test fun inconsistentAggregatesCannotBeSignedAsEvidence() {
        assertThrows(IllegalArgumentException::class.java) {
            PermissionEvidenceMapper.map(10L, PermissionReading(
                visiblePackages = 2, sampledPackages = 3, requestedInSample = 0,
                ownRequested = 1, ownGranted = 2, truncatedByLimit = false,
            ))
        }
    }

    @Test fun denialProducesOnlyMissingEvidence() {
        val batch = PermissionEvidenceMapper.missing(10L, ObservationStatus.DENIED, "ACCESS_DENIED")
        assertTrue(batch.observations.all { it.status == ObservationStatus.DENIED && it.value == null })
    }
}
