package online.capitalai.devicecare.core.quality

import online.capitalai.devicecare.core.quality.StatisticalValidation as S
import org.junit.Assert.*
import org.junit.Test

class StatisticalValidationTest {
    private fun c(tp: Long=0,fp: Long=0,tn: Long=0,fn: Long=0) = S.Counts(tp,fp,tn,fn)
    private fun close(expected: Double, actual: Double?, tolerance: Double=1e-8) {
        assertNotNull(actual)
        assertEquals(expected,actual!!,tolerance)
    }
    @Test fun r01ConfusionMetrics() {
        val a=c(9,1,99,1)
        close(.01,S.fpr(a)); close(.1,S.fnr(a))
        close(.9,S.precision(a)); close(.9,S.recall(a)); close(1.0,S.coverage(a))
    }
    @Test fun r02AllZeroNotProven() {
        val a=c()
        assertNull(S.fpr(a));assertNull(S.fnr(a));assertNull(S.precision(a))
        assertNull(S.recall(a));assertNull(S.coverage(a));assertNull(S.upperFpr95(a))
        assertFalse(S.reviewFprTarget(a));assertFalse(S.malwareFprTargets(a))
    }
    @Test fun r03ZeroFalsePositives299() {
        close(.009969146792899286,S.upperFpr95(c(fp=0,tn=299)))
        assertTrue(S.malwareFprTargets(c(fp=0,tn=299)))
    }
    @Test fun r04ZeroFalsePositives100NotEnough() {
        close(.02951304960703993,S.upperFpr95(c(tn=100)))
        assertFalse(S.malwareFprTargets(c(tn=100)))
    }
    @Test fun r05ReviewThresholdInclusive() {
        assertTrue(S.reviewFprTarget(c(fp=1,tn=99)))
        assertFalse(S.reviewFprTarget(c(fp=2,tn=98)))
    }
    @Test fun r06MalwarePointThresholdInclusiveButConfidenceSeparate() {
        val a=c(fp=1,tn=199)
        close(.005,S.fpr(a))
        assertFalse(S.malwareFprTargets(a))
        assertTrue(S.upperFpr95(a)!!>.01)
    }
    @Test fun r07NoNegativeDenominator() {
        assertNull(S.fpr(c(tp=1)));assertNull(S.upperFpr95(c(tp=1)))
    }
    @Test fun r08NoPositiveDenominator() {
        assertNull(S.fnr(c(tn=1)));assertNull(S.recall(c(tn=1)))
    }
    @Test fun r09NoPredictedPositiveDenominator() {
        assertNull(S.precision(c(tn=1,fn=1)))
    }
    @Test fun r10RejectInvalidCounts() {
        assertThrows(IllegalArgumentException::class.java) { c(fp=-1) }
        assertThrows(IllegalArgumentException::class.java) { S.Counts(0,0,0,0,1,0,0) }
    }
    @Test fun r11UnknownLabelExcludedEvenWhenWarning() {
        val result=S.count(listOf(S.Case(S.Reference.UNKNOWN,S.Decision.MALWARE_WARNING)),S.Threshold.MALWARE)
        assertEquals(1L,result.unknown);assertEquals(1L,result.excluded)
        assertEquals(0L,result.negatives());assertNull(S.fpr(result))
        close(0.0,S.coverage(result))
    }
    @Test fun r12NotAssessableIsNotTrueNegative() {
        val result=S.count(listOf(S.Case(S.Reference.BENIGN,S.Decision.NOT_ASSESSABLE)),S.Threshold.REVIEW)
        assertEquals(1L,result.notAssessable);assertEquals(0L,result.tn)
        assertNull(S.fpr(result));close(0.0,S.coverage(result))
    }
    @Test fun r13SameFixturesProduceSameResults() {
        val fixtures=listOf(S.Case(S.Reference.BENIGN,S.Decision.REVIEW_ONLY),
            S.Case(S.Reference.MALWARE,S.Decision.MALWARE_WARNING),
            S.Case(S.Reference.BENIGN,S.Decision.NO_WARNING))
        val a=S.count(fixtures,S.Threshold.REVIEW)
        assertEquals(a,S.count(fixtures,S.Threshold.REVIEW))
        assertEquals(S.upperFpr95(a),S.upperFpr95(S.count(fixtures,S.Threshold.REVIEW)))
    }
    @Test fun r14PermutationAndThresholdHierarchy() {
        val fixtures=listOf(S.Case(S.Reference.BENIGN,S.Decision.REVIEW_ONLY),
            S.Case(S.Reference.MALWARE,S.Decision.MALWARE_WARNING),
            S.Case(S.Reference.BENIGN,S.Decision.NO_WARNING))
        assertEquals(S.count(fixtures,S.Threshold.REVIEW),S.count(fixtures.reversed(),S.Threshold.REVIEW))
        val review=S.count(fixtures,S.Threshold.REVIEW)
        val malware=S.count(fixtures,S.Threshold.MALWARE)
        assertTrue(review.fp>=malware.fp);assertEquals(review.tp,malware.tp)
    }
    @Test fun r15OverflowRejected() {
        assertThrows(ArithmeticException::class.java) { c(tp=Long.MAX_VALUE,tn=1) }
        assertThrows(ArithmeticException::class.java) {
            S.Counts(0,0,0,0,Long.MAX_VALUE,Long.MAX_VALUE,1)
        }
    }
    @Test fun exactOneSidedOracleAdditionalPoints() {
        val points=listOf(
            Triple(0L,298L,.010002432437677733),
            Triple(1L,100L,.04655981145353893),
            Triple(1L,200L,.023498470434950702),
            Triple(2L,1000L,.006282284546723426),
            Triple(5L,2000L,.005249275286933494),
            Triple(10L,2000L,.008466348205518519))
        for((fp,n,expected) in points)close(expected,S.upperFpr95(c(fp=fp,tn=n-fp)))
    }
    @Test fun allFalsePositivesUpperBoundOne() {
        close(1.0,S.upperFpr95(c(fp=10)))
    }
}
