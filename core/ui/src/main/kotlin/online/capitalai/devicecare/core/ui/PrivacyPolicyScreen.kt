package online.capitalai.devicecare.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Bundled offline privacy text. Keep in sync with docs/PLAY-M1-PRIVACY-{DE,EN}.md. */
private data class PrivacySection(val tag: String, val title: String, val body: String)

private fun privacySections(isGerman: Boolean): List<PrivacySection> =
    if (isGerman) listOf(
        PrivacySection("controller", "1. Verantwortlicher und Kontakt",
            "AI-DeviceCare (Paket online.capitalai.devicecare). Rechtlicher Herausgeber, ladungsfähige Anschrift und Datenschutzkontakt: [VOR FREIGABE EINTRAGEN]. Dies ist eine interne Vorabfassung, nicht zur öffentlichen Verwendung freigegeben."),
        PrivacySection("scope", "2. Geltungsbereich",
            "Diese Erklärung gilt nur für die Consumer-M1-App. Sie beschreibt nicht die getrennte Business-App AI-DeviceCare+, künftige Cloud-, VPN-, Malware- oder Abofunktionen."),
        PrivacySection("access", "3. Lokaler Datenzugriff und Zweck",
            "Nur nach manuellem Start verarbeitet die App Akkuwerte (Ladestand, Ladezustand, Temperatur, vom System gemeldeter Gesundheitsstatus), grobe Speicher-/Thermiksignale, Android-sichtbare App- und Berechtigungsmetadaten sowie die Berechtigungen von AI-DeviceCare selbst. Die Zahl der geprüften sichtbaren Apps ist je nach Gerätebelastung auf maximal 250, 75 oder 25 begrenzt. Android kann weitere Apps verbergen; es entsteht keine vollständige App-Liste."),
        PrivacySection("security", "4. Optionaler System- und Google-Sicherheitscheck",
            "Eine getrennt manuell gestartete Prüfung liest Android-Version, API-Level, angezeigtes Sicherheitspatchdatum sowie verfügbare Versionsangaben von Google Play-Diensten, Play Store und aktivem WebView-Provider. Für die beiden Google-Pakete bestehen nur eng begrenzte Sichtbarkeitseinträge. Es erfolgen keine Live-Abfrage nach CVEs, kein Malware-Scan und keine Aussage über den tatsächlichen Play-Protect-Status."),
        PrivacySection("storage", "5. Speicherung und Löschung",
            "Prüfergebnisse werden für die Anzeige nur im Arbeitsspeicher gehalten. Es gibt in dieser M1-Vorschau kein Benutzerkonto, keine persistente Diagnosehistorie, keinen Diagnoseexport und keine planmäßige Speicherung der untersuchten Paketnamen. Beim Beenden des Prozesses gehen diese flüchtigen Ergebnisse verloren. Die App deaktiviert Android-App-Backups. Betriebssysteminterne Diagnoseaufzeichnungen bleiben außerhalb dieser App."),
        PrivacySection("network", "6. Übermittlung, Weitergabe und Empfänger",
            "Die beschriebenen M1-Prüfungen nutzen keine Cloudanalyse, Analytics, Werbe-SDKs oder einen eigenen Netzwerk-Upload. Das geprüfte Debug-Paket deklariert keine INTERNET-Berechtigung. Öffnet die Person ausdrücklich Android-Einstellungen, den Play Store oder eine offizielle Google-Hilfeseite, gelten für diese externen Anwendungen und Websites deren eigene Datenschutzregeln; AI-DeviceCare übermittelt dabei keine Diagnoseergebnisse."),
        PrivacySection("permission", "7. Berechtigungen und Sicherheit",
            "Es werden für diesen Umfang keine gefährlichen Android-Laufzeitberechtigungen, kein Vollzugriff auf Dateien, kein VPN, kein AccessibilityService und keine vollständige Paketabfrage angefordert. Roh-Paketnamen werden nur vorübergehend lokal zur Android-Metadatenabfrage verwendet und nicht in die Ergebnisanzeige übernommen. Nicht unterstützte Messungen werden als unbekannt oder nicht verfügbar markiert."),
        PrivacySection("legal", "8. Rechtsgrundlage und Rechte",
            "Der endgültige DSGVO-Rechtsgrundlagenabgleich ist vor Veröffentlichung durch den Verantwortlichen vorzunehmen. Betroffene haben im jeweils geltenden gesetzlichen Umfang Rechte auf Auskunft, Berichtigung, Löschung, Einschränkung und Beschwerde bei einer Datenschutzaufsicht. Da die App keine Konten oder serverseitige Diagnosedaten speichert, können Lösch- oder Auskunftsanfragen zu solchen Beständen entfallen; für Supportkontakte gelten gesondert festzulegende Regeln."),
        PrivacySection("support", "9. Supportanfragen und Aufbewahrung",
            "Support- und Kontaktinformationen, die Nutzer selbst außerhalb der App übermitteln, sind nicht Teil der lokalen M1-Diagnose. Kontaktweg, verantwortliche Stelle, Zwecke, Empfänger und Löschfristen für Supportvorgänge müssen vor einer externen Freigabe verbindlich ergänzt werden."),
        PrivacySection("updates", "10. Änderungen dieser Erklärung",
            "Fassung: 15.11.2026 – interner Entwurf. Bei Änderungen des tatsächlichen App-Scope, der SDKs, Datenflüsse oder Speicherfristen sind die Erklärung, In-App-Texte und die Data-Safety-Angaben gemeinsam zu aktualisieren. Die öffentliche HTTPS-Datenschutzadresse ist vor einem Play-Upload zu verifizieren.")
    ) else listOf(
        PrivacySection("controller", "1. Controller and contact",
            "AI-DeviceCare (package online.capitalai.devicecare). Legal publisher, postal address and privacy contact: [TO BE COMPLETED BEFORE RELEASE]. This is an internal draft and is not approved for public use."),
        PrivacySection("scope", "2. Scope",
            "This notice covers only the Consumer M1 app. It does not cover the separate AI-DeviceCare+ business app or future cloud, VPN, malware detection or subscription features."),
        PrivacySection("access", "3. On-device access and purpose",
            "Only after a user starts a check, the app processes battery readings (level, charging state, temperature and Android-reported health status), coarse memory/thermal signals, Android-visible app and permission metadata, and permissions of AI-DeviceCare itself. Depending on device pressure, sampling is limited to at most 250, 75 or 25 visible apps. Android may hide other apps; this is not a complete installed-app inventory."),
        PrivacySection("security", "4. Optional system and Google security check",
            "A separate user-initiated check reads Android version, API level, reported security patch date, and available version information for Google Play services, Play Store and the active WebView provider. The two Google packages have narrow visibility declarations only. There is no live CVE lookup, malware scan or claim of verifying Play Protect status."),
        PrivacySection("storage", "5. Retention and deletion",
            "Results are held in memory for display only. This M1 preview has no account, persistent diagnostic history, diagnostic upload or planned storage of inspected package names. In-memory results disappear when the app process ends. Android app backup is disabled. Operating-system diagnostic records are outside the app's control."),
        PrivacySection("network", "6. Transfers, sharing and recipients",
            "These M1 checks use no cloud analysis, analytics, advertising SDKs or app-managed network upload. The inspected debug package declares no INTERNET permission. If a user explicitly opens Android Settings, Play Store or an official Google help webpage, the external apps and websites apply their own privacy terms; AI-DeviceCare does not pass diagnostic results to them."),
        PrivacySection("permission", "7. Permissions and security",
            "This scope requests no dangerous Android runtime permissions, all-files access, VPN, AccessibilityService or unrestricted package visibility. Raw package names are used transiently on-device for Android metadata queries and are not shown in the results. Unavailable readings are marked as unknown or unsupported."),
        PrivacySection("legal", "8. Legal basis and rights",
            "The final GDPR legal basis assessment must be confirmed by the controller before publication. Where applicable, individuals have statutory rights to access, rectification, erasure, restriction and complaints to a data protection authority. The app stores no accounts or server-side diagnostic records; separate rules are required for any support correspondence."),
        PrivacySection("support", "9. Support correspondence",
            "Information users voluntarily send outside the app for support is not part of the on-device M1 diagnostic process. Before external release, the contact method, controller, purposes, recipients and retention/deletion period for support requests must be confirmed."),
        PrivacySection("updates", "10. Changes to this policy",
            "Version: 15 November 2026 – internal draft. Changes in shipped features, SDKs, transfers or retention must update this notice, in-app text and Play Data safety declarations together. A public HTTPS privacy policy URL must be verified before Play upload.")
    )

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val german = Locale.getDefault().language.equals("de", ignoreCase = true)
    val sections = privacySections(german)
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding()
                    .testTag("privacy_policy_screen")
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("privacy_policy_back")
                ) { Text(if (german) "Zurück zur Übersicht" else "Back to overview") }
                Text(
                    if (german) "Datenschutzerklärung" else "Privacy policy",
                    modifier = Modifier.semantics { heading() }.testTag("privacy_policy_title"),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(if (german) "Interner M1-Entwurf · keine Play-Freigabe" else
                    "Internal M1 draft · not approved for Play release")
                sections.forEach { section ->
                    Text(
                        section.title,
                        modifier = Modifier.semantics { heading() }
                            .testTag("privacy_policy_${section.tag}"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(section.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
