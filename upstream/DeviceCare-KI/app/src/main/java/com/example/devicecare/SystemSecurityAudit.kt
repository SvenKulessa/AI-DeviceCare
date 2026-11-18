package com.example.devicecare

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** On-demand local evidence; not a malware verdict or independent Google/OEM update lookup. */
internal const val SYSTEM_UPDATE_EVIDENCE_VERSION = "devicecare-system-update/1"

internal data class SystemSecuritySnapshot(
  val collectedAtMillis: Long,
  val androidRelease: String,
  val apiLevel: Int,
  val securityPatch: String?,
  val playServicesVersion: String?,
  val playStoreVersion: String?,
  val webViewVersion: String?,
  val webViewProvider: String?,
) {
  val contractVersion: String = SYSTEM_UPDATE_EVIDENCE_VERSION
}

internal enum class PatchAgeSignal { REVIEW_RECOMMENDED, NO_AGE_WARNING, NOT_PROVEN }

internal object PatchAgePolicy {
  /** Heuristic only, not CVE/OEM knowledge. Avoid java.time: minSdk is 24. */
  fun classify(patch: String?, nowMillis: Long): PatchAgeSignal {
    if (patch.isNullOrBlank() || nowMillis < 0L || !patch.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) {
      return PatchAgeSignal.NOT_PROVEN
    }
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
      isLenient = false
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val position = ParsePosition(0)
    val date = formatter.parse(patch, position)
      ?.takeIf { position.index == patch.length }
      ?: return PatchAgeSignal.NOT_PROVEN
    val dayMs = 86_400_000L
    val days = Math.floorDiv(nowMillis, dayMs) - Math.floorDiv(date.time, dayMs)
    return when {
      days < 0L -> PatchAgeSignal.NOT_PROVEN
      days >= 60L -> PatchAgeSignal.REVIEW_RECOMMENDED
      else -> PatchAgeSignal.NO_AGE_WARNING
    }
  }
}

/** Exact package visibility only; no general inventory, network, background work, or upload. */
internal class AndroidSystemSecurityReader(
  private val context: Context,
  private val clock: () -> Long = System::currentTimeMillis,
) {
  fun read(): SystemSecuritySnapshot {
    // getCurrentWebViewPackage() requires API 26; do not crash devices on API 24/25.
    val webView = if (Build.VERSION.SDK_INT >= 26) {
      try { WebView.getCurrentWebViewPackage() } catch (_: RuntimeException) { null }
    } else null
    return SystemSecuritySnapshot(
      collectedAtMillis = clock(),
      androidRelease = Build.VERSION.RELEASE.take(32),
      apiLevel = Build.VERSION.SDK_INT,
      securityPatch = Build.VERSION.SECURITY_PATCH
        .takeIf { it.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) },
      playServicesVersion = packageVersion("com.google.android.gms"),
      playStoreVersion = packageVersion("com.android.vending"),
      webViewVersion = bounded(webView?.versionName),
      webViewProvider = webView?.packageName?.takeIf { it.matches(Regex("[A-Za-z0-9_.]{1,128}")) },
    )
  }

  @Suppress("DEPRECATION")
  private fun packageVersion(name: String): String? =
    try { bounded(context.packageManager.getPackageInfo(name, 0).versionName) }
    catch (_: PackageManager.NameNotFoundException) { null }
    catch (_: SecurityException) { null }
    catch (_: RuntimeException) { null }

  private fun bounded(value: String?): String? =
    value?.takeIf { it.length in 1..80 && it.none(Character::isISOControl) }
}

internal fun presentSystemSecurity(snapshot: SystemSecuritySnapshot): List<String> {
  val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("UTC")
  }.format(Date(snapshot.collectedAtMillis))
  val patchAssessment = when (PatchAgePolicy.classify(snapshot.securityPatch, snapshot.collectedAtMillis)) {
    PatchAgeSignal.REVIEW_RECOMMENDED ->
      "Prüfung empfohlen (mindestens 60 Tage alt laut Geräteuhr; keine CVE-Aussage)"
    PatchAgeSignal.NO_AGE_WARNING ->
      "kein Alterswarnsignal; Aktualität nicht unabhängig bestätigt"
    PatchAgeSignal.NOT_PROVEN ->
      "NOT_PROVEN (Datum oder Geräteuhr nicht plausibel)"
  }
  fun installed(value: String?) = if (value == null) "NOT_PROVEN (nicht auslesbar)"
    else "$value [VERIFIED: lokal installiert; neueste Release nicht geprüft]"
  return listOf(
    "Evidence: ${snapshot.contractVersion} · UTC $utc",
    "Android: ${snapshot.androidRelease} (API ${snapshot.apiLevel}) [VERIFIED: System-API]",
    "Sicherheitspatch: ${snapshot.securityPatch ?: "nicht verfügbar"} — $patchAssessment",
    "Google Play-Dienste: ${installed(snapshot.playServicesVersion)}",
    "Google Play Store: ${installed(snapshot.playStoreVersion)}",
    "Aktiver Android WebView-Provider: ${installed(snapshot.webViewVersion)}",
    "Google Play-Systempatch: NOT_PROVEN (kein öffentlicher verlässlicher Lesezugriff)",
    "Play Protect: NOT_PROVEN (Scan-/Zertifizierungsstatus manuell prüfen)",
    "Play-Dienste-Beta: NOT_PROVEN (Mitgliedschaft nur im Play Store kontrollieren)",
    "ZTE-/OEM-OTA: NOT_PROVEN (nur über Hersteller-Systemupdate prüfen)",
  )
}
