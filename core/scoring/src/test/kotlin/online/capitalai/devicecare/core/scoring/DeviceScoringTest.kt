package online.capitalai.devicecare.core.scoring

import online.capitalai.devicecare.core.evidence.Availability
import online.capitalai.devicecare.core.evidence.DeviceEvidence
import online.capitalai.devicecare.core.evidence.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceScoringTest {
    private val model = DeviceModel("fixture-v1", "1.0.0", setOf("permission", "battery"))
    private val dispatcher = DeviceScoringDispatcher(StaticDeviceModelRegistry(mapOf(model.modelId to model)))
    private fun sample(id: String, availability: Availability = Availability.OBSERVED,
        risk: RiskLevel? = RiskLevel.NONE, expires: Long? = 100L) =
        DeviceEvidence(id, "fixture", availability, 1L, expires, risk)

    @Test fun completeVerifiedFixtureCanBeAssessed() {
        val result = dispatcher.assess(model.modelId,
            listOf(sample("battery", risk = RiskLevel.MEDIUM), sample("permission")), 20L)
        assertEquals(AssessmentStatus.ASSESSED, result.status)
        assertEquals(83, result.score)
        assertEquals(1.0, result.coverage, 0.001)
    }
    @Test fun incompleteDataNeverBecomesGoodScore() {
        val result = dispatcher.assess(model.modelId, listOf(sample("battery")), 20L)
        assertEquals(AssessmentStatus.NOT_ASSESSABLE, result.status)
        assertNull(result.score)
        assertEquals(listOf("permission"), result.missingSignals)
    }
    @Test fun deniedOrStaleSignalsCannotScore() {
        for (list in listOf(
            listOf(sample("battery"), sample("permission", Availability.DENIED)),
            listOf(sample("battery", expires = 2L), sample("permission")),
            listOf(sample("battery"), sample("battery"), sample("permission"))
        )) {
            val result = dispatcher.assess(model.modelId, list, 20L)
            assertEquals(AssessmentStatus.NOT_ASSESSABLE, result.status)
            assertNull(result.score)
        }
    }
    @Test fun unknownModelFailsClosed() {
        val result = dispatcher.assess("unregistered", listOf(sample("permission")), 20L)
        assertEquals(AssessmentStatus.NOT_ASSESSABLE, result.status)
        assertNull(result.score)
    }
}
