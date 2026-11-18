package com.example.model

enum class GovernanceStatus(val label: String) {
  VERIFIED("VERIFIED"),
  IN_PROGRESS("IN_PROGRESS"),
  NOT_PROVEN("NOT_PROVEN"),
  BLOCKED("BLOCKED"),
  REVIEW_REQUIRED("REVIEW_REQUIRED"),
}

enum class PlanCategory(val label: String) {
  ALL("Alle"),
  MILESTONES("Meilensteine M0–M5"),
  REMEDIATION("P0–P2 Aufgaben"),
  ANDROID15_QA("Android 15 QA"),
  PERMISSIONS("Berechtigungen"),
}

data class RoadmapTask(
  val id: String,
  val code: String,
  val title: String,
  val subtitle: String,
  val category: PlanCategory,
  val status: GovernanceStatus,
  val owner: String,
  val blockingRelease: Boolean,
  val sourceDoc: String,
  val description: String,
  val acceptanceCriteria: List<String>,
  val nonNegotiableBoundary: String,
  val canMarkVerifiedLocally: Boolean = true,
)

data class SafeguardRule(
  val number: Int,
  val title: String,
  val summary: String,
)

data class GradleModuleSpec(
  val path: String,
  val role: String,
  val state: GovernanceStatus,
)

data class CommercialTierPlan(
  val product: String,
  val tierName: String,
  val targetPriceMonthly: String,
  val milestone: String,
  val scopeSummary: String,
  val statusNote: String,
)

object DeviceCareRoadmapData {
  val tasks: List<RoadmapTask> =
    listOf(
      // --- Meilensteine M0-M5 ---
      RoadmapTask(
        id = "ms-m0",
        code = "M0",
        title = "Fundament, Governance & Manifest-Minima",
        subtitle = "Ein Gradle-Root, beide App-IDs, Governance-Skript & P0-CI",
        category = PlanCategory.MILESTONES,
        status = GovernanceStatus.VERIFIED,
        owner = "PLATFORM / TRUST",
        blockingRelease = false,
        sourceDoc = "docs/ARCHITECTURE.md · AGENTS.md",
        description =
          "Etabliert das zentrale Gradle-Mono-Projekt mit strikter `AGENTS.md`-Autorität, verbotenen M0-Berechtigungen und deterministischen Kern-Pipelines (`core:quality`, `core:scoring`). Nur für interne Debug-Artefakte.",
        acceptanceCriteria =
          listOf(
            "Nur ein Root `settings.gradle.kts` mit `:apps:devicecare` und `:apps:devicecare-plus`",
            "Keine verbotenen Manifest-Berechtigungen (kein INTERNET, QUERY_ALL_PACKAGES, VPN, Accessibility)",
            "`validate-governance.mjs` und P0-Unit-Tests laufen deterministisch",
          ),
        nonNegotiableBoundary = "Interne Foundation — autorisiert keinen Google Play Release.",
      ),
      RoadmapTask(
        id = "ms-m1",
        code = "M1",
        title = "Consumer-Vorschau (Akku, Berechtigungen & Diagnostik)",
        subtitle = "On-Demand-Kollektoren, adaptives 250/75/25-Budget & System-Intents",
        category = PlanCategory.MILESTONES,
        status = GovernanceStatus.IN_PROGRESS,
        owner = "MOBILE / PRODUCT",
        blockingRelease = true,
        sourceDoc = "docs/M1-COLLECTORS.md · docs/M1-ANDROID15-QA.md",
        description =
          "Erster Consumer-Vertikalschnitt: Liest auf expliziten Nutzerklick den Akkustand und OS-sichtbare Paket-Aggregate (`devicecare-observation/1`). Reale Android-15-Gerätetests, TalkBack-Abnahme und Play Billing stehen noch aus.",
        acceptanceCriteria =
          listOf(
            "Manueller Start auf Dispatchers.IO; kein Scan beim App-Öffnen",
            "Adaptives Budget 250 / 75 / 25 anhand RAM-, Energiespar- & Thermiksignalen",
            "Keine Rohpaketnamen in UI, Logs oder Persistenz; 60s Ablaufzeit (TTL)",
            "Physischer Android-15-Test und OEM-Vergleich noch durchzuführen",
          ),
        nonNegotiableBoundary = "Kein impliziter Risiko-Score aus M1-Beobachtungen (`NOT_ASSESSABLE`).",
      ),
      RoadmapTask(
        id = "ms-m2",
        code = "M2",
        title = "Pro-Schutz: Validierter Statik- & Malware-Scan",
        subtitle = "Signierte Regel-Updates & statische FPR-Nachweise (upperFpr95 <= 1%)",
        category = PlanCategory.MILESTONES,
        status = GovernanceStatus.BLOCKED,
        owner = "TRUST / AI",
        blockingRelease = true,
        sourceDoc = "docs/QA-SYNTHETIC-MALWARE-FIXTURES.md · StatisticalValidation.kt",
        description =
          "Geplanter Pro-Meilenstein für statische APK-/Regel-Analysen. Derzeit durch fehlende kalibrierte FP/FN-Studien, rechtmäßige Signatur-Quellen und Threat-Model-Freigaben blockiert.",
        acceptanceCriteria =
          listOf(
            "FPR <= 0,5% und 95% Clopper-Pearson Obergrenze <= 1,0%",
            "Signierte, lizenzgeprüfte Regel-Supply-Chain ohne GPL-Konflikte",
            "Keine Zero-Day-Versprechen und keine stille App-Löschung",
          ),
        nonNegotiableBoundary = "SYN-001 bis SYN-009 sind reine Test-Fixtures und KEIN Malware-Detektor.",
      ),
      RoadmapTask(
        id = "ms-m3",
        code = "M3",
        title = "Elite-Datenschutz: Einzelner VpnService & DNS-Filter",
        subtitle = "Koordinierte Netzwerk-/Firewall-Architektur mit Akku- & Leak-Tests",
        category = PlanCategory.MILESTONES,
        status = GovernanceStatus.BLOCKED,
        owner = "TRUST / MOBILE",
        blockingRelease = true,
        sourceDoc = "docs/ARCHITECTURE.md · AGENTS.md",
        description =
          "Android erlaubt nur einen aktiven `VpnService` pro Profil. Erfordert vollständige Google-Play-VPN-Deklaration, DNS-Leak-Tests, klares Akku-Budget und explizite Nutzer-Einwilligung.",
        acceptanceCriteria =
          listOf(
            "Ein koordinierter `VpnService` für DNS-Filterung und optionales Remote-VPN",
            "Nachgewiesene Leak-Tests und gemessener Akku-Overhead",
            "Dokumentierte Google Play VPN-Richtlinien-Freigabe",
          ),
        nonNegotiableBoundary = "In M0/M1 strikt verboten (`BIND_VPN_SERVICE` gesperrt).",
      ),
      RoadmapTask(
        id = "ms-m4",
        code = "M4",
        title = "AI-DeviceCare+ Business & Fleet-Posture",
        subtitle = "Mandanten-Isolation, Rollenrechte & autorisiertes Enrollment",
        category = PlanCategory.MILESTONES,
        status = GovernanceStatus.BLOCKED,
        owner = "PLATFORM / REVENUE",
        blockingRelease = true,
        sourceDoc = "docs/ARCHITECTURE.md · apps/devicecare-plus",
        description =
          "Eigenständige Business-App mit organisatorischem Enrollment, Row-Level-Tenant-Isolation in eigenem Supabase-Projekt, Audit-Export und rollenbasierten Richtlinien. Aktuell nur interner Prototyp.",
        acceptanceCriteria =
          listOf(
            "Eigenständiger funktionaler Mehrwert gegen Play Repetitive-Content-Richtlinie",
            "Serverseitige Mandantentrennung & RBAC vor Aktivierung",
            "Unabhängige Play-Listing-Prüfung für `online.capitalai.devicecare.plus`",
          ),
        nonNegotiableBoundary = "Nicht veröffentlichen, bevor B2B-Funktionen real differenziert sind.",
      ),
      RoadmapTask(
        id = "ms-m5",
        code = "M5",
        title = "Business-Expansion: Per-Seat-Verträge & SSO",
        subtitle = "Enterprise-Export, SLA-Support & Stripe/Play-Entitlement-Abgleich",
        category = PlanCategory.MILESTONES,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "REVENUE / PLATFORM",
        blockingRelease = false,
        sourceDoc = "docs/architecture/ADR-0001-EMBEDDED-TWIN-PIPELINES.md",
        description =
          "Ausbau der B2B-Plattform für Enterprise-Verträge, serverseitiges `EntitlementLedger@1`, Steuer-/COGS-Prüfung und optionales SSO.",
        acceptanceCriteria =
          listOf(
            "Validierte COGS- und Steuer-Kalkulation je Seat/Gerät",
            "Serverseitige Entitlement-Prüfung ohne automatische Übertragung zwischen App-IDs",
          ),
        nonNegotiableBoundary = "Monatliche Zielpreise sind unbestätigte Annahmen.",
      ),

      // --- P0 / P1 / P2 Aufgaben & Behebungen ---
      RoadmapTask(
        id = "rem-p0-apk",
        code = "P0-1",
        title = "Paketierte APK-Manifest-Prüfung (apkanalyzer & aapt2)",
        subtitle = "Verifikation des finalen APK-Manifests & SHA-256 Commit-Bindung",
        category = PlanCategory.REMEDIATION,
        status = GovernanceStatus.VERIFIED,
        owner = "PLATFORM",
        blockingRelease = false,
        sourceDoc = "docs/M1-ANDROID15-QA.md · verify-apk-policy.mjs",
        description =
          "Ersetzt reine Quell-Manifest-Greps durch echte `apkanalyzer`- und `aapt2`-Prüfungen der gebauten Debug-APKs. Blockiert Wildcard-Rechte, unerwartete Services, Backup=true und altes Target-SDK.",
        acceptanceCriteria =
          listOf(
            "Nur exakt eine Launcher-Activity und `allowBackup=false`",
            "Keine Netzwerk- oder gefährlichen Plattformrechte im gemergten Manifest",
            "Artifact SHA-256 an Commit-Hash gebunden",
          ),
        nonNegotiableBoundary = "Debug-APK-Prüfung ersetzt keine Release-AAB-Abnahme.",
      ),
      RoadmapTask(
        id = "rem-p0-contract",
        code = "P0-2",
        title = "ObservationContract@1 Allowlist & 60s-TTL",
        subtitle = "Harte Eingrenzung gegen PII, Paketnamen & Exception-Texte",
        category = PlanCategory.REMEDIATION,
        status = GovernanceStatus.VERIFIED,
        owner = "TRUST",
        blockingRelease = false,
        sourceDoc = "ObservationContract.kt · docs/M1-COLLECTORS.md",
        description =
          "Erzwingt fest definierte Signal-IDs (`battery_*`, `visible_packages`, `sampled_*`, `own_*`), Wertebereiche und maximale Gültigkeit von 60 Sekunden. Lehnt freie Texte und Roh-IDs mit `IllegalArgumentException` ab.",
        acceptanceCriteria =
          listOf(
            "Keine Paketnamen, E-Mails oder Android-IDs zulässig",
            "Unverfügbare Signale tragen `value = null` und festen `limitationCode`",
          ),
        nonNegotiableBoundary = "M1-Beobachtungen dürfen niemals in `RiskLevel` umgemünzt werden.",
      ),
      RoadmapTask(
        id = "rem-p1-budget",
        code = "P1-1",
        title = "Issue #7: Geräte-adaptives Berechtigungs-Budget",
        subtitle = "Dynamische Kappung (250 / 75 / 25) & Druck-Check alle 25 Pakete",
        category = PlanCategory.REMEDIATION,
        status = GovernanceStatus.IN_PROGRESS,
        owner = "MOBILE",
        blockingRelease = true,
        sourceDoc = "PermissionScanBudget.kt · AndroidPermissionScanProfile.kt",
        description =
          "Liest beim manuellen Scan grobe Speicher-, Low-RAM-, Energiespar- und Thermiksignale. Reduziert bei Bedarf das Stichprobenlimit auf 75 bzw. 25 Apps und bricht optionale Abfragen bei akutem Druck ab.",
        acceptanceCriteria =
          listOf(
            "250 Pakete bei klaren Signalen, 75 bei Low-RAM/Sparmodus/moderater Wärme/Unbekannt, 25 bei Speicherdruck/hoher Hitze",
            "Alle 25 Einträge erneute Prüfung auf hohen Druck und Abbruch-Signal",
            "Geräteprofil erklärt Budget und Gründe transparent in der UI",
          ),
        nonNegotiableBoundary = "Echtes OEM-Thermikverhalten noch auf physischen Geräten unbestätigt.",
      ),
      RoadmapTask(
        id = "rem-p1-emu",
        code = "P1-2",
        title = "Android 15 (API 35) Emulator Compose & Accessibility QA",
        subtitle = "Semantik-Tests, TalkBack-Überschriften & expliziter Scan-Button",
        category = PlanCategory.REMEDIATION,
        status = GovernanceStatus.IN_PROGRESS,
        owner = "PRODUCT / MOBILE",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md · M1InspectionAccessibilityTest.kt",
        description =
          "Übt auf API-35-Emulator das manuelle Starten der Prüfung, Überschriften-Semantik, 48dp-Touch-Ziele und Live-Regionen. Ersetzt keine manuelle TalkBack-Prüfung auf echter Hardware.",
        acceptanceCriteria =
          listOf(
            "Kein automatischer Scan beim Öffnen der App",
            "Alle interaktiven Elemente >= 48dp mit klaren Content-Descriptions",
            "LiveRegion-Ankündigung für Ergebnis- und Einstellungs-Meldungen",
          ),
        nonNegotiableBoundary = "Emulator-PASS beweist keine echte OEM-Kompatibilität.",
      ),
      RoadmapTask(
        id = "rem-p1-cpu",
        code = "P1-3",
        title = "Transparenter Messaufwand (Laufzeit & Prozess-CPU ms)",
        subtitle = "Informative Anzeige pro Scan ohne Energie-Hochrechnung",
        category = PlanCategory.REMEDIATION,
        status = GovernanceStatus.VERIFIED,
        owner = "PLATFORM",
        blockingRelease = false,
        sourceDoc = "MainActivity.kt · docs/M1-ANDROID15-QA.md",
        description =
          "Misst pro manuellem Scan die monoton verstrichene Laufzeit (`SystemClock.elapsedRealtime()`) sowie die Prozess-CPU-Zeit (`Process.getElapsedCpuTime()`). Keine Persistenz, keine mAh-Schätzung.",
        acceptanceCriteria =
          listOf(
            "Anzeige enthält expliziten Hinweis: nur informativ, keine Akkuenergie-Messung",
            "Kein Background-Worker, kein WakeLock",
          ),
        nonNegotiableBoundary = "CPU-Zeit darf niemals als mAh- oder Akku-Ersparnis dargestellt werden.",
      ),
      RoadmapTask(
        id = "rem-p2-aab",
        code = "P2-1",
        title = "Release-AAB-Build-Validierung & Play-Policy-Gate",
        subtitle = "versionCode/versionName-Prüfung, Upload-Keystore & Datenschutz-Erklärung",
        category = PlanCategory.REMEDIATION,
        status = GovernanceStatus.BLOCKED,
        owner = "PLATFORM / REVENUE",
        blockingRelease = true,
        sourceDoc = "docs/RELEASE-BUILD-VALIDATION.md",
        description =
          "Validiert `versionCode` (1..2100000000) und SemVer `versionName`, erstellt unsigniertes Release-Bundle zur Prüfung. Produktiv-Signatur und Play Console Freigabe erfordern separates Owner-Review.",
        acceptanceCriteria =
          listOf(
            "Erfolgreicher `bundleRelease` Build mit geprüfter Manifest-Version",
            "Vollständige Play Data Safety & Berechtigungs-Deklaration durch Owner",
          ),
        nonNegotiableBoundary = "Kein stilles Hochladen oder Veröffentlichen in Google Play.",
      ),

      // --- 8 Physische Android 15 QA Tests (M1-ANDROID15-QA.md) ---
      RoadmapTask(
        id = "qa-1-no-auto",
        code = "QA-01",
        title = "Kein ADB/Root & kein Auto-Scan beim Start",
        subtitle = "Installation via regulärem Debug-Build; Prüfung startet erst nach Tipp",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §1",
        description =
          "Auf physischem Android-15-Gerät ohne ADB/Root prüfen, dass beim Öffnen der App keinerlei Akku- oder Paketabfrage automatisch startet.",
        acceptanceCriteria =
          listOf(
            "Ergebnisbereich bleibt vor dem Klick auf 'Lokale Prüfung starten' leer",
            "Kein Hintergrunddienst aktiv",
          ),
        nonNegotiableBoundary = "Nur Gerätemodell-Familie, API-Level und SHA dokumentieren — keine Paketlisten!",
      ),
      RoadmapTask(
        id = "qa-2-battery",
        code = "QA-02",
        title = "Physischer Akku-, Lade- & OEM-Sensor-Abgleich",
        subtitle = "Prozentwert, Laden/Entladen & UNSUPPORTED-Verhalten bei fehlenden Sensoren",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §2",
        description =
          "Vergleiche angezeigten Akkustand und Ladezustand mit der Systemanzeige beim Ein-/Ausstecken. Prüfe, dass `GOOD` als Systemstatus und nicht als Zell-Restkapazität erklärt wird.",
        acceptanceCriteria =
          listOf(
            "Übereinstimmung mit System-Akkuanzeige",
            "Fehlende Temperatur/Health liefert explizit `UNSUPPORTED`, niemals 0 oder erfunden",
          ),
        nonNegotiableBoundary = "Kein Zellverschleiß- oder App-Verbrauchsversprechen.",
      ),
      RoadmapTask(
        id = "qa-3-packages",
        code = "QA-03",
        title = "Physische App-Sichtbarkeit & sample_truncated",
        subtitle = "Vergleich gegen OS-Kontext ohne Anzeige roher Paket-IDs",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §3",
        description =
          "Verifiziere auf echtem Gerät, dass ausschließlich aggregierte Zähler angezeigt werden, `sample_truncated` bei Überschreitung des Budgets greift und keine Paket-IDs sichtbar sind.",
        acceptanceCriteria =
          listOf(
            "Keine Rohpaketnamen sichtbar oder geteilt",
            "Sichtbarkeits-Hinweis weist klar auf OS-Filterung hin",
          ),
        nonNegotiableBoundary = "Niemals Screenshots mit privaten App-Listen hochladen.",
      ),
      RoadmapTask(
        id = "qa-4-a11y",
        code = "QA-04",
        title = "Android 15 TalkBack, 200% Schrift & 320dp Layout",
        subtitle = "Vollständige manuelle Barrierefreiheits- und Umbruch-Prüfung",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §4",
        description =
          "Aktiviere TalkBack manuell und prüfe Überschriften-Navigation, Fokusreihenfolge, Live-Regionen, 200% Schrift-/Anzeigegröße, Querformat, Hell/Dunkel und 320dp Breite.",
        acceptanceCriteria =
          listOf(
            "Kein Text-Clipping bei 200% Schriftgröße",
            "Saubere TalkBack-Ansage bei Scan-Ergebnis und Einstellungs-Fehlern",
          ),
        nonNegotiableBoundary = "Ohne eigene AccessibilityService-Berechtigung testen.",
      ),
      RoadmapTask(
        id = "qa-5-process-death",
        code = "QA-05",
        title = "Abbruch, Rotation & Process Death",
        subtitle = "Keine Persistenz von Scan-Werten nach Beenden oder Drehen",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §5",
        description =
          "Starte Scan, drehe das Gerät, wechsle in den Hintergrund und erzwinge Beenden. Nach Neustart dürfen keine alten Scan-Werte oder hängende Diagnosen bestehen bleiben.",
        acceptanceCriteria =
          listOf(
            "Nach Force-Close erscheinen keine gespeicherten Messwerte",
            "Keine verwaisten Services",
          ),
        nonNegotiableBoundary = "Scan-Ergebnisse bleiben strikt im Arbeitsspeicher.",
      ),
      RoadmapTask(
        id = "qa-6-power",
        code = "QA-06",
        title = "10x Scan CPU- & Thermik-Messung",
        subtitle = "Wiederholbare Messreihe (>= 30s Abstand) & echter Akkuvergleich",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §6",
        description =
          "Führe 10 einzelne Scans im Abstand von mindestens 30 Sekunden durch und vergleiche Laufzeit/CPU-ms sowie den realen Geräte-Akkuverbrauch gegen eine Baseline.",
        acceptanceCriteria =
          listOf(
            "Reproduzierbare Laufzeit- und CPU-Werte dokumentiert",
            "Keine auffällige Erwärmung oder Hintergrundaktivität",
          ),
        nonNegotiableBoundary = "Keine Ableitung von mAh-Werten aus `Process.getElapsedCpuTime()`.",
      ),
      RoadmapTask(
        id = "qa-7-permissions-audit",
        code = "QA-07",
        title = "Android-Einstellungen Berechtigungs-Audit",
        subtitle = "Verifikation unter Apps → AI-DeviceCare → Berechtigungen",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §7",
        description =
          "Prüfe direkt in den Android-Systemeinstellungen, dass keine Standort-, Kontakt-, Datei-, Netzwerk-, VPN-, Usage- oder Paket-Sonderrechte angefordert werden.",
        acceptanceCriteria =
          listOf(
            "0 gefährliche oder spezielle Berechtigungen erteilt",
            "App nimmt keine eigenständigen Systemänderungen vor",
          ),
        nonNegotiableBoundary = "Least-Privilege-Prinzip bleibt verbindlich.",
      ),
      RoadmapTask(
        id = "qa-8-error-paths",
        code = "QA-08",
        title = "Fehlerpfade & verweigerte OEM-Signale",
        subtitle = "Expliziter UNSUPPORTED/DENIED/ERROR-Status statt Scheinwert",
        category = PlanCategory.ANDROID15_QA,
        status = GovernanceStatus.NOT_PROVEN,
        owner = "OWNER QA",
        blockingRelease = true,
        sourceDoc = "docs/M1-ANDROID15-QA.md §8",
        description =
          "Prüfe auf abweichenden OEM-Geräten und bei fehlenden Signalen, dass Fehlerpfade sauber als nicht verfügbar gemeldet werden und niemals einen gesunden Risiko-Score vorgaukeln.",
        acceptanceCriteria =
          listOf(
            "Explizite Limitation-Codes statt 0% oder GOOD-Fallback",
            "Risiko-Score bleibt `NOT_ASSESSABLE`",
          ),
        nonNegotiableBoundary = "Fail-Closed in jedem Fehlerfall.",
      ),

      // --- Berechtigungs- & Play-Policy-Matrix ---
      RoadmapTask(
        id = "perm-query-all",
        code = "PERM-01",
        title = "QUERY_ALL_PACKAGES (Gesperrt in M0/M1)",
        subtitle = "Ersetzt durch OS-gefilterte Sichtbarkeit & 250er Stichprobe",
        category = PlanCategory.PERMISSIONS,
        status = GovernanceStatus.BLOCKED,
        owner = "TRUST / MOBILE",
        blockingRelease = false,
        sourceDoc = "docs/ANDROID_CAPABILITY_MATRIX.md · project-policy.json",
        description =
          "Darf nur bei nachgewiesener Kern-Antivirus-/Sicherheitsfunktion mit vollständiger Google-Play-Erklärung und Genehmigung beantragt werden. In M0/M1 ausdrücklich verboten.",
        acceptanceCriteria =
          listOf(
            "Play-Policy-Nachweis für Core Security Role erforderlich",
            "Bis dahin nur scoped `getInstalledPackages` ohne `<queries>`",
          ),
        nonNegotiableBoundary = "Verboten im M0/M1 Manifest (`forbiddenManifestPermissionNames`).",
        canMarkVerifiedLocally = false,
      ),
      RoadmapTask(
        id = "perm-all-files",
        code = "PERM-02",
        title = "MANAGE_EXTERNAL_STORAGE (Gesperrt in M0/M1)",
        subtitle = "Keine pauschalen Cleaner-Versprechen ohne Play-Freigabe",
        category = PlanCategory.PERMISSIONS,
        status = GovernanceStatus.BLOCKED,
        owner = "TRUST / PRODUCT",
        blockingRelease = false,
        sourceDoc = "docs/ANDROID_CAPABILITY_MATRIX.md · project-policy.json",
        description =
          "Vollzugriff auf alle Dateien ist für M0/M1 gesperrt. Spätere Speicherbereinigung nutzt nur nutzerautorisierte, sichere Storage-APIs oder gezielte Dokumenten-Picker.",
        acceptanceCriteria =
          listOf(
            "Play All-Files-Deklaration & Bereinigungstest vor Einsatz",
            "Keine Behauptung, private Caches beliebiger Dritt-Apps löschen zu können",
          ),
        nonNegotiableBoundary = "Verboten im M0/M1 Manifest.",
        canMarkVerifiedLocally = false,
      ),
      RoadmapTask(
        id = "perm-usage-stats",
        code = "PERM-03",
        title = "PACKAGE_USAGE_STATS (Gesperrt in M0/M1)",
        subtitle = "Erfordert expliziten Spezialzugriff-Dialog & klare Einwilligung",
        category = PlanCategory.PERMISSIONS,
        status = GovernanceStatus.BLOCKED,
        owner = "TRUST / PLATFORM",
        blockingRelease = false,
        sourceDoc = "docs/ANDROID_CAPABILITY_MATRIX.md · project-policy.json",
        description =
          "Nutzungsstatistiken erlauben keinen exakten mAh-Verbrauch pro App. Einsatz nur nach ausdrücklicher Nutzer-Navigation in den System-Spezialzugriff.",
        acceptanceCriteria =
          listOf(
            "Expliziter Einwilligungspfad vor Abfrage",
            "Nie als exakte Akku-Verbrauchsmessung pro App bewerben",
          ),
        nonNegotiableBoundary = "Verboten im M0/M1 Manifest.",
        canMarkVerifiedLocally = false,
      ),
      RoadmapTask(
        id = "perm-vpn-a11y",
        code = "PERM-04",
        title = "VpnService & AccessibilityService (Gesperrt)",
        subtitle = "VPN erst in M3; Accessibility-Automatisierung dauerhaft untersagt",
        category = PlanCategory.PERMISSIONS,
        status = GovernanceStatus.BLOCKED,
        owner = "TRUST",
        blockingRelease = false,
        sourceDoc = "AGENTS.md §7-8 · project-policy.json",
        description =
          "AccessibilityService darf NIEMALS verwendet werden, um Berechtigungsdialoge, Einstellungen oder Cache-Löschungen zu automatisieren. `VpnService` bleibt bis M3 blockiert.",
        acceptanceCriteria =
          listOf(
            "Kein `BIND_ACCESSIBILITY_SERVICE` für UI-Automation",
            "Einzelner `VpnService` erst nach Threat-Model, Leak-Tests & Play-Review in M3",
          ),
        nonNegotiableBoundary = "Striktes Verbot von Accessibility-Shortcuts.",
        canMarkVerifiedLocally = false,
      ),
    )

  val safeguards: List<SafeguardRule> =
    listOf(
      SafeguardRule(1, "Kein Root, kein ADB, keine Bypässe", "Nur offizielle Android-APIs und dokumentierte Systemdialoge."),
      SafeguardRule(2, "Einwilligung vor jeder Aktion", "Keine stillen Systemänderungen; Preview, Risiko & Consent zuerst."),
      SafeguardRule(3, "Privacy by Default", "On-Device-First, minimale Daten, Redaktion und keine Rohpaket-Speicherung."),
      SafeguardRule(4, "Beweise vor Urteil", "Jeder Befund erfasst Signal, API, Zeitstempel, Unsicherheit & Limitation."),
      SafeguardRule(5, "Sichere Anti-Malware-Aussagen", "Keine Zero-Day-Versprechen; FP/FN-Tests und signierte Regeln Pflicht."),
      SafeguardRule(6, "Akku-bewusstes Verhalten", "On-Demand / WorkManager mit Budgets; eigener CPU-Aufwand messbar."),
      SafeguardRule(7, "Einzelne VPN-Integration", "Maximal ein koordinierter VpnService für DNS, Firewall & VPN ab M3."),
      SafeguardRule(8, "Kein Accessibility-Missbrauch", "Keine automatisierte Bedienung fremder Dialoge oder App-Cleaner."),
      SafeguardRule(9, "Lizenzen als Release-Gate", "Prüfung aller Abhängigkeiten und Regel-Datensätze auf kommerzielle Rechte."),
      SafeguardRule(10, "Kein stiller Produktiv-Release", "Play-Upload, Secrets und produktive Änderungen nur nach Owner-Review."),
    )

  val gradleModules: List<GradleModuleSpec> =
    listOf(
      GradleModuleSpec(":apps:devicecare", "Consumer Android-App (distinct applicationId)", GovernanceStatus.IN_PROGRESS),
      GradleModuleSpec(":apps:devicecare-plus", "Business-Client (Internal Prototype Only)", GovernanceStatus.BLOCKED),
      GradleModuleSpec(":core:evidence", "Unveränderliche Verträge, ObservationContract@1 & QA-Fixtures", GovernanceStatus.VERIFIED),
      GradleModuleSpec(":core:quality", "Deterministische CI-Evidenz & Clopper-Pearson Statistik", GovernanceStatus.VERIFIED),
      GradleModuleSpec(":core:scoring", "Single Dispatcher & Model Registry (Fail-Closed)", GovernanceStatus.VERIFIED),
      GradleModuleSpec(":core:ui", "Gemeinsames Compose Design-System & Barrierefreiheit", GovernanceStatus.IN_PROGRESS),
      GradleModuleSpec(":features:battery", "On-Demand BatteryManager & Broadcast Collector", GovernanceStatus.VERIFIED),
      GradleModuleSpec(":features:permissions", "OS-sichtbare Paket-Stichprobe & 250/75/25 Budget", GovernanceStatus.IN_PROGRESS),
      GradleModuleSpec(":features:malware / network / fleet", "Gesperrt für M2 / M3 / M4 (keine Platzhalter ausliefern)", GovernanceStatus.BLOCKED),
    )

  val tierPlans: List<CommercialTierPlan> =
    listOf(
      CommercialTierPlan(
        product = "AI-DeviceCare (Consumer)",
        tierName = "Free",
        targetPriceMonthly = "0,00 €",
        milestone = "M1",
        scopeSummary = "Lokaler Akku-/Systemstatus, sichtbare App-Stichprobe, geführte Einstellungen.",
        statusNote = "M1 Vorschau (Billing inaktiv)",
      ),
      CommercialTierPlan(
        product = "AI-DeviceCare (Consumer)",
        tierName = "Starter",
        targetPriceMonthly = "3,99 €*",
        milestone = "M1",
        scopeSummary = "Erweiterte lokale Diagnostik, sichere nutzerautorisierte Bereinigung & Export.",
        statusNote = "Unbestätigte Annahme · Play Review offen",
      ),
      CommercialTierPlan(
        product = "AI-DeviceCare (Consumer)",
        tierName = "Pro",
        targetPriceMonthly = "6,49 €*",
        milestone = "M2",
        scopeSummary = "Validierter Statik-/Malware-Scan mit signierten Regeln & FP/FN-Berichten.",
        statusNote = "Blockiert bis M2-Validierung",
      ),
      CommercialTierPlan(
        product = "AI-DeviceCare (Consumer)",
        tierName = "Elite",
        targetPriceMonthly = "14,79 €*",
        milestone = "M3",
        scopeSummary = "Integrierter DNS-/Datenschutzfilter & optionales VPN über einzelnen VpnService.",
        statusNote = "Blockiert bis M3-Leak- & Play-Gate",
      ),
      CommercialTierPlan(
        product = "AI-DeviceCare+ (Business)",
        tierName = "Enterprise / Pro / Biz",
        targetPriceMonthly = "29 – 129 €*",
        milestone = "M4–M5",
        scopeSummary = "Mandantenfähige Flotten-Posture, RBAC-Richtlinien, Audit-Export & Per-Seat-SSO.",
        statusNote = "Interner Prototyp · Nicht für Store-Release",
      ),
    )
}
