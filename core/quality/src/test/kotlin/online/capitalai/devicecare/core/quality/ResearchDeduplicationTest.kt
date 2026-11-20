package online.capitalai.devicecare.core.quality

import org.junit.Assert.*
import org.junit.Test

class ResearchDeduplicationTest {
    private val d = ResearchDeduplication
    private fun p(doi: String? = null, arxiv: String? = null, date: String = "2026-11-01",
                  material: Boolean = false) =
        ResearchDeduplication.Publication("Synthetic research fixture", doi, arxiv, date, material)
    @Test fun newDoi() {
        assertEquals(ResearchDeduplication.Outcome.NEW_PUBLICATION,
            d.classify(p(doi="10.1234/abc"), emptyList()).outcome)
    }
    @Test fun doiNormalizationAndDuplicate() {
        assertEquals(ResearchDeduplication.Outcome.DUPLICATE,
            d.classify(p(doi="https://doi.org/10.1234/ABC"), listOf(p(doi="doi:10.1234/abc"))).outcome)
    }
    @Test fun arxivVersionsAreSamePublication() {
        assertEquals(ResearchDeduplication.Outcome.DUPLICATE,
            d.classify(p(arxiv="arXiv:2607.23272v2"), listOf(p(arxiv="2607.23272v1"))).outcome)
    }
    @Test fun materialVersionRevision() {
        assertEquals(ResearchDeduplication.Outcome.MATERIAL_REVISION,
            d.classify(p(arxiv="2607.23272v2",material=true),listOf(p(arxiv="2607.23272v1"))).outcome)
    }
    @Test fun materialDateRevision() {
        assertEquals(ResearchDeduplication.Outcome.MATERIAL_REVISION,
            d.classify(p(doi="10.1234/abc",date="2026-11-03",material=true),
                listOf(p(doi="10.1234/abc"))).outcome)
    }
    @Test fun nonMaterialNewVersionNotNewPublication() {
        assertEquals(ResearchDeduplication.Outcome.DUPLICATE,
            d.classify(p(arxiv="2607.23272v3"),listOf(p(arxiv="2607.23272v1"))).outcome)
    }
    @Test fun staleRevisionNotReannounced() {
        assertEquals(ResearchDeduplication.Outcome.DUPLICATE,
            d.classify(p(arxiv="2607.23272v1",material=true),
                listOf(p(arxiv="2607.23272v2",date="2026-11-03"))).outcome)
    }
    @Test fun crossIdentifierMatching() {
        assertEquals(ResearchDeduplication.Outcome.DUPLICATE,
            d.classify(p(doi="10.1234/abc",arxiv="2607.23272v2"),
                listOf(p(arxiv="2607.23272v1"))).outcome)
    }
    @Test fun rejectsMissingIdentityAndInvalidDates() {
        assertThrows(IllegalArgumentException::class.java) { d.identity(p()) }
        assertThrows(IllegalArgumentException::class.java) { d.identity(p(doi="bad")) }
        assertThrows(IllegalArgumentException::class.java) { d.identity(p(arxiv="not-an-id")) }
        assertThrows(IllegalArgumentException::class.java) { d.identity(p(doi="10.1234/a",date="2026-02-30")) }
        assertThrows(IllegalArgumentException::class.java) { d.identity(p(arxiv="2607.23272v999999999999999999999")) }
    }
    @Test fun permutationAndReplayDeterministic() {
        val prior=listOf(p(doi="10.1234/a"),p(arxiv="2607.23272v1"))
        val candidate=p(arxiv="2607.23272v2",date="2026-11-03",material=true)
        assertEquals(d.classify(candidate,prior),d.classify(candidate,prior.reversed()))
        assertEquals(d.classify(candidate,prior),d.classify(candidate,prior))
    }
}
