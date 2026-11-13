package online.capitalai.devicecare

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Local, on-demand posture evidence. Neither a Google release comparison nor a CVE
 * vulnerability verdict: public Android APIs do not expose Play Protect results,
 * Play System update date, or Google Play Services beta enrollment reliably.
 */
internal data class SystemSecuritySnapshot(
    val collectedAtMillis: Long,
    val androidRelease: String,
    val apiLevel: Int,
    val securityPatch: String?,
    val playServicesVersion: String?,
    val playStoreVersion: String?,
    val webViewVersion: String?,
    val webViewProvider: String?,
)

internal enum class PatchAgeSignal { REVIEW_RECOMMENDED, NO_AGE_WARNING, NOT_PROVEN }

internal object PatchAgePolicy {
    /**
     * Product heuristic, not Google's CVE policy or a claim about OEM backports.
     * Device-clock UTC date is used; future or malformed dates fail closed.
     */
    fun classify(patch: String?, nowMillis: Long): PatchAgeSignal {
        if (patch.isNullOrBlank() || nowMillis < 0L) return PatchAgeSignal.NOT_PROVEN
        val date = try {
            LocalDate.parse(patch)
        } catch (_: DateTimeParseException) {
            return PatchAgeSignal.NOT_PROVEN
        }
        val today = LocalDate.ofEpochDay(Math.floorDiv(nowMillis, 86_400_000L))
        val ageDays = ChronoUnit.DAYS.between(date, today)
        return when {
            ageDays < 0L -> PatchAgeSignal.NOT_PROVEN
            ageDays >= 60L -> PatchAgeSignal.REVIEW_RECOMMENDED
            else -> PatchAgeSignal.NO_AGE_WARNING
        }
    }
}

/** Bounded, user-visible values; no package inventory, logging or network requests. */
internal class AndroidSystemSecurityReader(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun read(): SystemSecuritySnapshot {
        val webView = try {
            WebView.getCurrentWebViewPackage()
        } catch (_: RuntimeException) {
            null
        } catch (_: SecurityException) {
            null
        }
        return SystemSecuritySnapshot(
            collectedAtMillis = clock(),
            androidRelease = Build.VERSION.RELEASE.take(32),
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH
                .takeIf { it.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) },
            playServicesVersion = packageVersion("com.google.android.gms"),
            playStoreVersion = packageVersion("com.android.vending"),
            webViewVersion = boundedVersion(webView?.versionName),
            // Used transiently only to open this provider's system App-info screen.
            webViewProvider = webView?.packageName?.takeIf {
                it.matches(Regex("[A-Za-z0-9_.]{1,128}"))
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun packageVersion(packageName: String): String? = try {
        boundedVersion(context.packageManager.getPackageInfo(packageName, 0).versionName)
    } catch (_: PackageManager.NameNotFoundException) {
        null // absent or hidden by OEM visibility rules: never report outdated
    } catch (_: SecurityException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    private fun boundedVersion(value: String?): String? =
        value?.takeIf { it.length in 1..80 && it.none { c -> Character.isISOControl(c) } }
}

/**
 * Explicit provenance and uncertainty. Installed != latest; a beta channel cannot
 * be deduced from the version string. No automatic security or malware scores.
 */
internal fun presentSystemSecurity(snapshot: SystemSecuritySnapshot): List<String> {
    val patch = snapshot.securityPatch ?: "nicht verfügbar"
    val patchAssessment = when (PatchAgePolicy.classify(snapshot.securityPatch, snapshot.collectedAtMillis)) {
        PatchAgeSignal.REVIEW_RECOMMENDED ->
            "Prüfung empfohlen (mindestens 60 Tage alt laut Geräteuhr; keine CVE-Aussage)"
        PatchAgeSignal.NO_AGE_WARNING -> "kein Alterswarnsignal; Aktualität nicht unabhängig bestätigt"
        PatchAgeSignal.NOT_PROVEN -> "NOT_PROVEN (Datum oder Geräteuhr nicht plausibel)"
    }
    fun installed(v: String?) = if (v == null) "NOT_PROVEN (nicht auslesbar)" else
        v + " [VERIFIED: lokal installiert; neueste Release nicht geprüft]"
    return listOf(
        "Android: " + snapshot.androidRelease + " (API " + snapshot.apiLevel + ") [VERIFIED: System-API]",
        "Sicherheitspatch: " + patch + " — " + patchAssessment,
        "Google Play-Dienste: " + installed(snapshot.playServicesVersion),
        "Google Play Store: " + installed(snapshot.playStoreVersion),
        "Aktiver Android WebView-Provider: " + installed(snapshot.webViewVersion),
        "Google Play-Systempatch: NOT_PROVEN (kein öffentlicher verlässlicher Lesezugriff)",
        "Play Protect: NOT_PROVEN (Scan-/Zertifizierungsstatus manuell prüfen)",
        "Play-Dienste-Beta: NOT_PROVEN (Mitgliedschaft nur im Play Store kontrollieren)",
        "ZTE-/OEM-OTA: NOT_PROVEN (nur über Hersteller-Systemupdate prüfen)",
    )
}
