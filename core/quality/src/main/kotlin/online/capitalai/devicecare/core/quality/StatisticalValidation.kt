package online.capitalai.devicecare.core.quality

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.ln1p

/** Pure offline statistical validation. Synthetic fixtures are not malware-detection evidence. */
object StatisticalValidation {
    enum class Reference { BENIGN, MALWARE, UNKNOWN }
    enum class Decision { NO_WARNING, REVIEW_ONLY, MALWARE_WARNING, NOT_ASSESSABLE }
    data class Case(val reference: Reference, val decision: Decision)
    data class Counts(val tp: Long, val fp: Long, val tn: Long, val fn: Long,
                      val excluded: Long = 0, val unknown: Long = 0, val notAssessable: Long = 0) {
        init {
            require(listOf(tp, fp, tn, fn, excluded, unknown, notAssessable).all { it >= 0 })
            require(unknown <= excluded && notAssessable <= excluded)
            require(Math.addExact(unknown, notAssessable) == excluded)
            total() // overflow check
        }
        fun total(): Long = Math.addExact(Math.addExact(Math.addExact(tp, fp), Math.addExact(tn, fn)), excluded)
        fun negatives(): Long = Math.addExact(fp, tn)
        fun positives(): Long = Math.addExact(tp, fn)
    }
    enum class Threshold { REVIEW, MALWARE }
    fun count(cases: List<Case>, threshold: Threshold): Counts {
        var tp=0L; var fp=0L; var tn=0L; var fn=0L; var unknown=0L; var unavailable=0L
        for (case in cases) {
            if (case.reference == Reference.UNKNOWN) { unknown++; continue }
            if (case.decision == Decision.NOT_ASSESSABLE) { unavailable++; continue }
            val warning = case.decision == Decision.MALWARE_WARNING ||
                (threshold == Threshold.REVIEW && case.decision == Decision.REVIEW_ONLY)
            when {
                case.reference == Reference.MALWARE && warning -> tp++
                case.reference == Reference.MALWARE -> fn++
                warning -> fp++
                else -> tn++
            }
        }
        return Counts(tp,fp,tn,fn,Math.addExact(unknown,unavailable),unknown,unavailable)
    }
    private fun ratio(n: Long, d: Long): Double? = if (d == 0L) null else n.toDouble()/d
    fun fpr(c: Counts): Double? = ratio(c.fp,c.negatives())
    fun fnr(c: Counts): Double? = ratio(c.fn,c.positives())
    fun precision(c: Counts): Double? = ratio(c.tp,Math.addExact(c.tp,c.fp))
    fun recall(c: Counts): Double? = ratio(c.tp,c.positives())
    fun coverage(c: Counts): Double? = ratio(Math.addExact(Math.addExact(c.tp,c.fp),Math.addExact(c.tn,c.fn)),c.total())

    // Lanczos log-gamma, for positive arguments only.
    private val coefficients = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028,
        771.32342877765313, -176.61502916214059, 12.507343278686905,
        -0.13857109526572012, 9.984369578019572e-6, 1.5056327351493116e-7)
    private fun logGamma(z: Double): Double {
        require(z >= 1.0 && z.isFinite())
        var sum=coefficients[0]
        for (i in 1 until coefficients.size) sum+=coefficients[i]/(z+i-1.0)
        val t=z+6.5
        return 0.9189385332046727+(z-0.5)*ln(t)-t+ln(sum)
    }
    private fun betaFraction(a: Double,b: Double,x: Double): Double {
        val tiny=1e-300
        var c=1.0
        var d=1.0-(a+b)*x/(a+1.0)
        if (abs(d)<tiny) d=tiny
        d=1.0/d
        var h=d
        for (m in 1..10000) {
            val mf=m.toDouble()
            var aa=mf*(b-mf)*x/((a+2*mf-1)*(a+2*mf))
            d=1.0+aa*d; if(abs(d)<tiny)d=tiny
            c=1.0+aa/c; if(abs(c)<tiny)c=tiny
            d=1.0/d; h*=d*c
            aa=-(a+mf)*(a+b+mf)*x/((a+2*mf)*(a+2*mf+1))
            d=1.0+aa*d; if(abs(d)<tiny)d=tiny
            c=1.0+aa/c; if(abs(c)<tiny)c=tiny
            d=1.0/d
            val delta=d*c
            h*=delta
            if(abs(delta-1.0)<3e-14) return h
        }
        throw ArithmeticException("Incomplete beta did not converge")
    }
    private fun regularizedBeta(x: Double,a: Double,b: Double): Double {
        if(x<=0.0)return 0.0
        if(x>=1.0)return 1.0
        val front=exp(logGamma(a+b)-logGamma(a)-logGamma(b)+a*ln(x)+b*ln1p(-x))
        val value=if(x<(a+1.0)/(a+b+2.0))
            front*betaFraction(a,b,x)/a
        else 1.0-front*betaFraction(b,a,1.0-x)/b
        if(!value.isFinite())throw ArithmeticException("Invalid beta result")
        return value.coerceIn(0.0,1.0)
    }
    /** Exact one-sided 95% Clopper-Pearson upper bound; null if there are no negatives. */
    fun upperFpr95(c: Counts): Double? {
        val n=c.negatives()
        if(n==0L)return null
        if(c.fp==n)return 1.0
        if(c.fp==0L)return -kotlin.math.expm1(ln(0.05)/n.toDouble())
        val a=c.fp.toDouble()+1.0
        val b=c.tn.toDouble()
        var low=c.fp.toDouble()/n
        var high=1.0
        repeat(90) {
            val mid=low+(high-low)/2
            if(regularizedBeta(mid,a,b)<0.95)low=mid else high=mid
        }
        return low+(high-low)/2
    }
    fun reviewFprTarget(c: Counts): Boolean = fpr(c)?.let { it<=0.01 } ?: false
    fun malwareFprTargets(c: Counts): Boolean =
        (fpr(c)?.let { it<=0.005 } ?: false) &&
        (upperFpr95(c)?.let { it<=0.01 } ?: false)
}
