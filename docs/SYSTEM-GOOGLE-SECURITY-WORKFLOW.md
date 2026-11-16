# System- & Google-Sicherheitscheck — consumer M1
Issue #18 · 2026-11-13 · Feature-Branch `feature/system-google-security-check-p0`

## Nutzer-Workflow (Android 8.0+, Schwerpunkt Android 15)

1. In der Consumer-App **System- & Google-Sicherheitscheck öffnen**.
2. Bildschirm startet ohne Datenerfassung. Nach explizitem Tippen auf **Versionsprüfung starten** werden eine einzelne lokale Momentaufnahme und ein quellencodierter Befund angezeigt.
3. Android `Build.VERSION.RELEASE` / `SDK_INT` / `SECURITY_PATCH` stammen vom Android-System. Google Play-Dienste und Play Store werden nur über die exakt im Manifest sichtbaren Pakete abgefragt. Die aktive WebView-Version stammt von `WebView.getCurrentWebViewPackage()` (API 26).
4. Ein 60-Tage-Altersindikator ist eine konservative **lokale Produktheuristik**, keine Google-Frist, kein CVE-Mapping und keine OEM-Updateverfügbarkeit. Ungültige und zukünftige Patchdaten sind `NOT_PROVEN`. Eine installierte Paketversion bedeutet nicht „neueste Release“.
5. Die Oberfläche bietet ausschließlich **manuelle** Weiterleitungen zu Sicherheitseinstellungen, Play-Dienste-App-Info, WebView-App-Info, Play Store/Play Protect und offiziellen Google-System-Release-Notes. Intents können von OEMs fehlen; der vorhandene sichere Einstellungen-Fallback zeigt dann einen manuellen Hinweis.
6. Der Nutzer überprüft Play Protect (Scan + Play-Zertifizierung), die Google-Play-Systemupdate-Abfrage, den Play-Dienste-Beta-Status und den OEM-OTA separat. Der Workflow kann diese Ergebnisse **nicht** per öffentlicher Android-API eigenständig nachweisen.
7. Der Befund existiert nur im aktuellen UI-Speicher und wird bei Zurücknavigation verworfen. Keine Speicherung, Hintergrundjobs, WorkManager, Netzwerkabfrage, Uploads oder Sensor-Scan.

## Grenzen, Bedrohungsmodell und Status

- Threat: Veraltete Firmware, irreführende „aktuell“-Aussagen, manipulierte Gerätezeit, OEM-Patch-Backports und getrennte Play-Systemmodule. Mitigation: Verifizierungsstatus plus Herkunft pro Zeile; kein aggregierter Malware-/Sicherheits-Score.
- Threat: Unnötige App-Inventur und personenbezogene Gerätekennungen. Mitigation: nur `<queries>` für `com.google.android.gms` und `com.android.vending`. Die WebView-Paketkennung dient nur transient dem App-Info-Intent. Keine Telemetrie/Logs.
- Threat: Nutzer verliert Google-Zugang oder Wallet-Daten beim Zurücksetzen. Mitigation: keine Deinstallation, kein „Updates entfernen“, kein Löschen von Play-Dienste-Daten, keine Beta-Abmeldung im Code, keine Systemeinstellungsmutation.
- Threat: App behauptet kritische CVEs ohne Treiber-/OEM-Kenntnis. Mitigation: Link zu offiziellen Release-Notes; CVE-Zuordnung bleibt `NOT_PROVEN` bis OEM- und Google-Nachweise vorliegen.
- Kein `INTERNET`, `QUERY_ALL_PACKAGES`, Root, ADB, AccessibilityService, Geräteadministrator oder neue gefährliche Berechtigung. Kein Backend, neues kostenpflichtiges SDK oder Lizenz-Datensatz.

| Signal | Quelle | Lokaler Evidence-Status | Limitation |
| --- | --- | --- | --- |
| Android-Version und API | `Build.VERSION` | VERIFIED nach Nutzerstart | Keine aktuelle OEM-OTA-Aussage |
| Android-Sicherheitspatch | `Build.VERSION.SECURITY_PATCH` | VERIFIED falls plausibel | Geräteuhr/Backports/CVE-Abdeckung unbekannt |
| Google Play-Dienste | `PackageManager` exaktes Paket | VERIFIED falls sichtbar | Beta-Kanal, verfügbare Releases unbekannt |
| Google Play Store | `PackageManager` exaktes Paket | VERIFIED falls sichtbar | Play Protect nicht auslesbar |
| Aktiver WebView-Provider | `WebView.getCurrentWebViewPackage()` | VERIFIED falls sichtbar | Release-/Patchvergleich extern |
| Play Protect / Zertifizierung | manueller Play-Store-Workflow | NOT_PROVEN | Keine öffentliche Scan-Resultat-API |
| Google Play-Systempatch | manuelle Sicherheitseinstellungen | NOT_PROVEN | Keine verlässliche öffentliche API |
| Hersteller-OTA | OEM-Systemaktualisierung | NOT_PROVEN | Hersteller-/Region-/Rolloutstatus extern |
| Google Play-Dienste Beta | manuelle Store-Prüfung | NOT_PROVEN | Versionstring beweist keinen Release-Kanal |

## Prüfbefehle & Release-Gates

```sh
gradle --no-daemon :apps:devicecare:testDebugUnitTest
gradle --no-daemon :apps:devicecare:assembleDebug :apps:devicecare:lintDebug
node scripts/validate-governance.mjs
node --test scripts/validate-governance.test.mjs scripts/verify-apk-policy.test.mjs
node scripts/verify-apk-policy.mjs
gradle --no-daemon :apps:devicecare:connectedDebugAndroidTest
```

- Unit fixtures: Patch älter als 60 Tage, kein „latest“-Urteil, ungültig/fehlend/zukünftig, fehlende Google-Pakete, keine abgeleitete Beta-/Play-Protect-Freigabe.
- UI: separater Bildschirm, kein automatischer Collector-Aufruf, expliziter Scan, TalkBack-/Touch-Ziele, Zurücknavigation.
- Physisches Z2459 mit Android 15: versionsbasierte Geräteauslesung und Google-/OEM-Oberflächen inklusive OEM-Fallback; manuell nachzuweisen. Tests am Emulator sind **kein** ZTE-OTA- oder Play-Protect-Nachweis.
- Keine produktive Freigabe oder Merge ohne vollständige grüne CI am exakten Head, Manifest-/Lizenzprüfung und Android-15-Gerätenachweis.

## Quellen / Referenzen

- Android `Build.VERSION.SECURITY_PATCH`: https://developer.android.com/reference/android/os/Build.VERSION
- Android `WebView.getCurrentWebViewPackage()`: https://developer.android.com/reference/android/webkit/WebView
- Gezielt sichtbare Pakete: https://developer.android.com/training/package-visibility/declaring
- Android-Bulletins: https://source.android.com/docs/security/bulletin
- Google System Release Notes: https://support.google.com/product-documentation/answer/14343500

Rollback: Feature-Commit(s) revertieren, keine Schemaänderung und keine Datenmigration.
