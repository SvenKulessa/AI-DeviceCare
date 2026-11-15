# Issue #20: Consumer M1 – gemeinsam prüfbarer Scope aus PR #17 und PR #19

Status **INTEGRATION PREVIEW / NOT_PROVEN**. Kein Merge, keine AAB-Signatur, kein Play-Upload. Basis: AGENTS.md (P0-FDN-1) und Issue #20. Diese Branch ist eine explizite technische Integration, **kein stiller Ersatz oder Merge von PR #17 oder #19**.

## Exakte Eingangs-SHAs

| Ursprung | SHA | Übernommener Umfang |
|---|---|---|
| Main | `1b4fe12e12fc412e1a473a9c8070980a60080bc0` | M1 BatteryManager, gefilterte Permission-Aggregate, adaptives 250/75/25-Profil |
| PR #19 | `9cc886394914c6020db6a4c5fb58a617c9e34846` | Basis dieses Integrationsbranches, gesonderter Android-/Google-Update-Metadatencheck, begrenzte Paket-Queries |
| PR #17 | `8d0c46a045974c7afe5bdafdd2ba4016d92ca20b` | Manueller Release-AAB-Workflow und validierte Gradle-Versionseigenschaften |
| Issue #20 Privacy Slice | Feature-Head wird nach PR-Erstellung erneut geprüft | Offline DE/EN Policy-View, DE/EN Rechtstexte, Data Safety, UI-Regression |

Die gemeinsame Gradle-Datei erhält **beide** Änderungen:
- PR #17: Validierung und Übernahme von `versionCode` (1..2100000000) und `versionName` (SemVer-artige Form), bei fehlenden Inputs Preview-Defaults 1 / 0.1.0-m1-preview.
- PR #19: Consumer `testImplementation("junit:junit:4.13.2")` für den Sicherheitscheck.

Release-AAB-Workflow aus PR #17 ist weiterhin **nur manuell** auslösbar. Signatur-Secrets dürfen nur im genehmigten Workflow verwendet werden; dieser Branch löst keinen Dispatch aus. Interne PR-#17/#19-Debug-APKs und frühere Testläufe sind **keine** Evidenz für das kombinierte AAB.

## Feature-Claims für Listing und Policy
**Consumer M1:**
1. Einmalig manuell gestartete Akkuwerte, ohne Versprechen auf per-App-mAh oder Einsparungen.
2. Begrenzte, OS-gefilterte Permission-/App-Metadaten in aggregierter Form.
3. Ressourcenadaptives 250/75/25-Profil.
4. Nutzergesteuerte Android-Settings-Navigation.
5. Separater, nur auf Nutzerklick ausgeführter Android-/Google-Versions- und Patch-Hinweis; keine Aussage zur Aktualität, CVE-Abdeckung oder Play-Protect-Scan.
6. Vor dem Scan zugängliche, offline dargestellte Datenschutzinformationen auf Deutsch/Englisch.

**NICHT im Release-Scope:** Malware-/Ransomware-Erkennung, volle System-App-Analyse, automatische Drittanbieter-Cache-Löschung, VPN/DNS, KI-Cloud, Billing, Accounts, Business/MDM. Keine entsprechenden Store-Claims.

## Datenschutz-Abnahme
- `core/ui/.../PrivacyPolicyScreen.kt`: Datenschutz-Einstieg ohne Konto, Offline-Anzeige vor Scan, systemsprachlich DE/EN, keine Browser-/Cloud-Abhängigkeit.
- `docs/PLAY-M1-PRIVACY-DE.md`, `docs/PLAY-M1-PRIVACY-EN.md`: identische 10 Themen mit Pflichtfeld-Platzhaltern.
- `docs/PLAY-M1-DATA-SAFETY-MATRIX.md`: Trennung zwischen On-device-Verarbeitung und Datenübermittlung mit konkreten Nachweisen.
- `PrivacyPolicyUiTest`: Öffnen/Schließen ohne Collector; von Emulator-CI neu am **exakten Integration-Head** zu prüfen.

**BLOCKER:** Rechtlicher Herausgeber, Adresse, Datenschutzkontakt, ggf. zuständige Datenschutzbehörde, abschließende DSGVO-Rechtsgrundlage, Support-Handling und öffentliche HTTPS-URL fehlen. Der In-App-Text ist ein interner Entwurf, nicht veröffentlichungsfähig.

## Gate-Liste (muss neu auf exaktem Head grün sein)
1. GitHub CI, Governance, Android P0, Emulator API 26/32/33/35/36, Lint, Negativ- und Regressionstests.
2. Gradle `bundleRelease` **unsigniert mit Testversionen** und negatives `-PversionCode=0`, `-PversionName=bad name` reproduzierbar; keine produktiven Signierungs-Secrets.
3. Finales AAB-/APK-Merged-Manifest, transitive SDK-/Lizenzprüfung, SBOM, 16-KB-Kompatibilität, Netzwerktest.
4. Neuer echter Android-15-OEM-Test: Lesbarkeit, TalkBack, Rotation, Scan-/Zurücknavigation, Prozess-Neustart, Keine-Autostart-Prüfung, CPU/Batterie.
5. Privacy-URL samt 200/HTML/TLS/weltweiter Erreichbarkeit und Gleichheit der Policies; Play-Konsole read-only abgleichen.
6. Erst nach ausdrücklicher Owner-Freigabe: Integrations-/Mergeentscheidung, signierter AAB-Build, Play Internal Testing.

## Empfohlener Merge-/Rollback-Pfad
**Keine Bulk-Merges.** PR #17 gegen aktuellen Main einzeln mit neuer CI-Evidence konsolidieren; PR #19 anschließend gegen aktualisierten Main synchronisieren und separat beurteilen; den Datenschutz-Slice danach klein und testbar übernehmen. Bei fehlender Evidence bleibt jede Stufe BLOCKED. Rollback ausschließlich über Revert der einzelnen relevanten Commits, ohne Datenmigration.
