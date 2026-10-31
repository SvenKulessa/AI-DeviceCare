package online.capitalai.devicecare.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QualityPipelineTest {
    private val sha = "0123456789abcdef0123456789abcdef01234567"
    @Test fun allChecksPassOnlyWithCommitEvidence() {
        assertEquals(CheckStatus.PASS,
            QualityPipeline.assess(listOf(QualityCheck("lint", CheckStatus.PASS)), sha).status)
    }
    @Test fun noEvidenceMeansNotProven() {
        assertEquals(CheckStatus.NOT_PROVEN, QualityPipeline.assess(emptyList(), sha).status)
        assertEquals(CheckStatus.NOT_PROVEN,
            QualityPipeline.assess(listOf(QualityCheck("test", CheckStatus.PASS)), null).status)
    }
    @Test fun negativeCheckFailsClosed() {
        assertEquals(CheckStatus.FAIL,
            QualityPipeline.assess(listOf(QualityCheck("secret", CheckStatus.FAIL)), sha).status)
    }
    @Test fun duplicateCheckIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            QualityPipeline.assess(listOf(QualityCheck("same", CheckStatus.PASS),
                QualityCheck("same", CheckStatus.PASS)), sha)
        }
    }
}
