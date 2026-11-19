# AI-DeviceCare Consumer M1 – prüfbare Data-Safety-Matrix

Stand: 2026-11-15. **NICHT als Play-Console-Abgabe freigegeben.** Der Nachweis gilt als Architektur-/Source-Review für Main 1b4fe12e und PR #19 9cc88639 plus PR #17 8d0c46a0. Aktuelle Build- und Laufzeitevidenz des gemeinsamen Integrations-Head sind noch NOT_PROVEN.

Google Play nennt nur lokal verarbeitete und nicht vom Gerät übermittelte Daten **nicht** als „collected“. Das ist kein pauschaler Freibrief: SDK-Datenflüsse, Supportprozesse, externe Systeme und alle Play-Varianten müssen für das endgültige AAB erfasst werden.

| Kategorie / konkrete Daten | Zugriff / Zweck | Speicherung | App-eigene Übermittlung | Vorläufige Play-Angabe | Nachweis / Vorbehalt |
|---|---|---|---|---|---|
| Akku-Ladestand, Status, Temperatur, System-Health | Einmalig manuell, Batteriezustand erläutern | Nur UI-Speicher | Kein Upload im M1-Quellcode | Nicht gesammelt / nicht geteilt, **vorläufig** | `features/battery`, M1-Collectors; finaler AAB-/Netzwerktest offen |
| Android-sichtbare Paketnamen und Berechtigungsmetadaten | Vorübergehender PackageManager-Zugriff | Aggregierte Zahlen im UI; rohe Namen nicht persistent | Kein Upload im M1-Quellcode | Nicht gesammelt / nicht geteilt, **vorläufig** | `features/permissions`; Play betrachtet installierte Apps als sensible Daten |
| Eigene App-Berechtigungen | Eigene Permissions gezielt erklären | Nur UI-Speicher | Kein Upload im M1-Quellcode | Nicht gesammelt / nicht geteilt, **vorläufig** | `AndroidPermissionCollector` |
| RAM-/Thermik-/Geräteprofil | Auswahl 250/75/25 sichtbare Apps | Nur UI-Speicher | Kein Upload im M1-Quellcode | Nicht gesammelt / nicht geteilt, **vorläufig** | `AndroidPermissionScanProfile`; keine individuellen Sensoridentitäten |
| Android-Release, API-Level und Patchdatum | Manuell ausgelöster lokaler Sicherheitscheck | Nur UI-Speicher | Kein Upload im PR-#19-Code | Nicht gesammelt / nicht geteilt, **vorläufig** | PR #19 `SystemSecurityAudit.kt`, keine CVE-Auswertung |
| Google Play-Dienste/Store-Version und WebView-Provider | Nur gezielte lokale Versionsabfrage / Settings-Hand-off | Nur UI-Speicher; Paketname nur transient | Kein Upload im PR-#19-Code | Nicht gesammelt / nicht geteilt, **vorläufig** | PR #19 `<queries>` ausschließlich GMS und Play Store |
| Diagnosezeiten / CPU-Millisekunden | Messaufwand erklären, keine Energiebilanz | Nur UI-Speicher | Kein Upload im M1-Quellcode | Nicht gesammelt / nicht geteilt, **vorläufig** | `MainActivity.kt` |
| Konten, Benutzerkennung, E-Mail, Auth | In der M1-App nicht implementiert | Kein App-Account-Speicher | Keine App-Account-API | Nicht gesammelt / nicht geteilt, **vorläufig** | Vollständiger Dependency-/AAB-Check offen |
| Standort, Kontakte, Kamera, Mikrofon, Dateizugriff | Nicht Teil von M1 | Nicht vorgesehen | Nicht vorgesehen | Nicht gesammelt / nicht geteilt, **vorläufig** | Merge-Manifest und SDK-Gate erneut erforderlich |
| Cloud, KI, Ad SDK, Analytics, Crash Reporting | Nicht Teil von M1 | Nicht vorgesehen | Keine App-Cloud/API | Nicht gesammelt / nicht geteilt, **vorläufig** | SBOM, Manifest, transitive SDKs, dynamischer Test ausstehend |
| Supportanfrage außerhalb der App | Separater Kommunikationskanal, **noch nicht festgelegt** | **NOT_PROVEN** | **NOT_PROVEN** | **BLOCKED** bis Kontaktverfahren definiert | Betreiber, Zweck, Frist, Empfänger vor Play-Upload dokumentieren |
| Manuell geöffnete Google-/Android-Ziel-App | User-initiated Intent, kein Diagnosedaten-Payload | Drittanbieter verwalten eigene Daten | Browser/Play Store können selbst kommunizieren | Kein durch AI-DeviceCare verursachter Upload von Diagnosedaten nach Source-Review | Externe Policies getrennt prüfen |
| Rechtliche Person / Datenschutzkontakt | Veröffentlichungs-Pflichtangabe | **NICHT VORHANDEN** | – | **BLOCKED** | Owner-Nachweis, ladungsfähige Anschrift, Kontakt und Legal Review |
| Öffentliche Privacy-URL | Play-Console-Eintrag | **NICHT NACHGEWIESEN** | – | **BLOCKED** | Aktives HTTPS, weltweit, ohne Anmeldung, HTML, identische DE/EN-Inhalte |

## Vorläufige Antworten – strikt releasegebunden
- **App sammelt Nutzerdaten:** vorläufig NEIN, ausschließlich für nachgewiesene lokalen M1-Diagnose; erst nach exaktem signed-AAB-/SDK-/Netzwerktest bestätigen.
- **App teilt Nutzerdaten mit Dritten:** vorläufig NEIN, derselbe Vorbehalt. Externe von Nutzern gestartete Apps separat bewerten.
- **Datenlöschung:** Keine persistente M1-Diagnosehistorie oder App-Konten; Angaben für Supportkommunikation noch offen.
- **Datenverschlüsselung bei Übertragung:** Für die dokumentierte M1-Diagnose kein app-eigener Upload. Keine positive Sicherheitsbehauptung für zukünftige Netzwerkfunktionen.
- **SDKs:** AndroidX/Kotlin-Stack anhand PR-#19-APK vorgeprüft; finaler vollständiger Software-/SDK-Bestand am konsolidierten AAB erneut zu belegen.

## Reproduktions- / Go-No-Go-Check
1. Exakten finalen Head-/Tree-SHA und AAB-SHA-256 erfassen.
2. AAB/Universal APK per bundletool und `apkanalyzer`/`aapt2` dekodieren; Manifest aller Varianten, Queries, exportierte Komponenten und Permissions prüfen.
3. Vollständigen Gradle dependency graph/SBOM sowie transitive SDK-Zwecke und -Policies dokumentieren.
4. Nach Freigabe in isolierter Testumgebung Netzwerkfluss (einschließlich Start, Scan, Abbruch, Settings-Intent) überprüfen; kein ADB/Root auf physischem Nutzergerät.
5. Supportprozess, Legal Identity und öffentliches Privacy-Hosting verifizieren.
6. Erst dann reale Antworten in Play Console anhand derselben finalen Build-ID freigeben.

Quellen:
- https://support.google.com/googleplay/android-developer/answer/10787469
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://developer.android.com/training/package-visibility
