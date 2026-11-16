package online.capitalai.devicecare

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic fixtures: no claim about OEM OTA, Play Protect or current CVEs. */
class SystemSecurityAuditTest {
    private val today = Instant.parse("2026-11-13T12:00:00Z").toEpochMilli()

    @Test fun augustPatchIsReviewRecommendedButNotACveVerdict() {
        assertEquals(PatchAgeSignal.REVIEW_RECOMMENDED,
            PatchAgePolicy.classify("2026-08-05", today))
        val lines = presentSystemSecurity(snapshot("2026-08-05"))
        assertTrue(lines.any { it.contains("Prüfung empfohlen") })
        assertTrue(lines.any { it.contains("keine CVE-Aussage") })
        assertFalse(lines.any { it.contains("infiziert", ignoreCase = true) })
    }

    @Test fun recentPatchIsNotClaimedToBeLatest() {
        assertEquals(PatchAgeSignal.NO_AGE_WARNING,
            PatchAgePolicy.classify("2026-11-05", today))
        val lines = presentSystemSecurity(snapshot("2026-11-05"))
        assertTrue(lines.any { it.contains("Aktualität nicht unabhängig bestätigt") })
    }

    @Test fun malformedFutureAndMissingPatchesFailClosed() {
        for (patch in listOf<String?>(null, "", "2026-02-30", "bad", "2026-12-01")) {
            assertEquals(PatchAgeSignal.NOT_PROVEN, PatchAgePolicy.classify(patch, today))
        }
        assertEquals(PatchAgeSignal.NOT_PROVEN,
            PatchAgePolicy.classify("2026-11-05", -1L))
    }

    @Test fun unavailableGooglePackagesAreNotLabeledOutdated() {
        val lines = presentSystemSecurity(snapshot(null))
        assertTrue(lines.any { it.contains("Play-Dienste: NOT_PROVEN") })
        assertTrue(lines.any { it.contains("WebView-Provider: NOT_PROVEN") })
        assertTrue(lines.any { it.contains("Play Protect: NOT_PROVEN") })
        assertTrue(lines.any { it.contains("Beta: NOT_PROVEN") })
        assertTrue(lines.any { it.contains("ZTE-/OEM-OTA: NOT_PROVEN") })
    }

    @Test fun observedVersionIsNotEvidenceOfCurrentReleaseOrStableChannel() {
        val sample = snapshot("2026-11-05").copy(
            playServicesVersion = "26.44.12",
            playStoreVersion = "54.0",
            webViewVersion = "158.0",
        )
        val lines = presentSystemSecurity(sample)
        assertTrue(lines.any { it.contains("26.44.12 [VERIFIED: lokal installiert") })
        assertTrue(lines.any { it.contains("Play-Dienste-Beta: NOT_PROVEN") })
        assertFalse(lines.any { it.contains("Beta: VERIFIED") })
    }

    private fun snapshot(patch: String?) = SystemSecuritySnapshot(
        collectedAtMillis = today,
        androidRelease = "15",
        apiLevel = 35,
        securityPatch = patch,
        playServicesVersion = null,
        playStoreVersion = null,
        webViewVersion = null,
        webViewProvider = null,
    )
}
