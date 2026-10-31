# Android capability and Play policy matrix — P0 baseline

**Status:** P0 design review only. Android 15 focus; verify variations on devices/API levels when implemented. This is not legal advice or confirmation of Google Play approval.

| User feature | What Android public APIs can provide | Permission / consent | Limitations / safe remediation | Initial milestone |
| --- | --- | --- | --- | --- |
| Basic device posture | Android API level, patch version, available storage, charging state | No sensitive permission for basic metadata | OEM-specific fields may be absent | M1 |
| Battery analysis | Level/status/temp/health and optional current/cycle measurements, when supported | Typically no dangerous permission for BATTERY_CHANGED/BatteryManager basics | No comprehensive per-app mAh, thermal sensors may vary; label unavailable data | M1 |
| Battery optimization | Guided power-save and app battery settings | User acts in OS settings | Other apps' power modes cannot be broadly set silently; usage stats are approximate and require separate special access | M1 |
| Installed apps | PackageManager visible metadata | scoped <queries> or justified QUERY_ALL_PACKAGES | Android package visibility is restricted; Play acceptance required for broad inventory | M1 |
| Permission audit | PackageManager declared/granted state for visible packages where exposed | scoped visibility | Not all privileged internal accesses or user activity are exposed | M1 |
| App/system remediation | ACTION_APPLICATION_DETAILS_SETTINGS or appropriate settings intents | user confirms in Settings | Cannot generally toggle other apps' runtime permissions, special access or system switches | M1 |
| Cleaner | Own cache; user-selected external files via SAF/MediaStore and OS storage management | SAF consent; special permissions only if clearly essential and Play-approved | Cannot promise removal of every internal third-party cache; require dry run, preview and verification | M1 |
| Malware analysis | User-selected files and accessible APK metadata/static features, signed rules | user file selection / permitted package visibility | No kernel-level process access; heuristics may falsely flag benign apps; no universal zero-day claims | M2 |
| Tracker blocking | DNS/domain filters and per-app VPNService routing where feasible | VpnService dialog and disclosures | Can miss inline ads/DoH/endpoint-sharing, and an Android profile only has one active VPN | M3 |
| Remote VPN | VPNService + licensed compatible remote tunnel | explicit VPN consent | User must choose between conflicting VPN services; bandwidth and privacy obligations | M3 |
| Fleet policy | DevicePolicyManager for authorized enrolled managed profiles/devices | admin/org enrollment and role-bound management | Consumer installation alone cannot grant MDM privileges | M4 |
| AI explanations | Local deterministic score + optional ML triage | cloud opt-in and data minimization | AI cannot authorize privileged changes or substantiate unverified malware verdicts | M1+ |
| Diagnostic logs | Local structured app-own events + permitted telemetry | explicit remote export consent | No unrestricted system logcat, application secrets or third-party private data | M1 |

## Restricted / special permissions approval checklist
- **QUERY_ALL_PACKAGES:** only for a demonstrable core security/antivirus role, declaration and Play policy approval. Default to scoped package queries.
- **MANAGE_EXTERNAL_STORAGE:** do not add for M0. Consider only when eligible core AV or file-management use and Play permission declaration/approval; cache-clear intent behavior/version support must be tested and user-authorized.
- **PACKAGE_USAGE_STATS:** user navigates to explicit special-access page and consents if usage-derived battery insights are essential; never claim accurate per-app battery drain from usage statistics.
- **VpnService:** only in M3 after clear UX, disclosures, valid declarations, leak tests and battery budget.
- **AccessibilityService:** no shortcut to manipulate Android permission dialogs or system settings. Do not use it for app cleaner automation.
- **REQUEST_INSTALL_PACKAGES, device admin, overlay and notification access:** prohibited by default in M0/M1 until a narrowly scoped use case, threat model, Play review and explicit user consent are documented.

## Evidence and quality acceptance
1. Every collector implements **supported / unsupported / denied / transient error** outcomes rather than hiding failure.
2. Every remediation advertises **direct API / system-guided / unavailable** before the user attempts it.
3. Include falsifiable per-device QA matrices (Android 12–16 where practical, varying OEMs) and track UX for missing sensors.
4. Sensitive action and log data have local retention limits, export/redaction, granular consent and deletion.
5. Never advertise Play approval, malware detection completeness, universal cache cleaning or battery savings without measured evidence.

## Authoritative review entrypoints
- Android package visibility: https://developer.android.com/training/package-visibility
- Google Play package visibility: https://support.google.com/googleplay/android-developer/answer/10158779
- Google Play all-files: https://support.google.com/googleplay/android-developer/answer/10467955
- Android BatteryManager: https://developer.android.com/reference/android/os/BatteryManager
- Android UsageStatsManager: https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Android VPN: https://developer.android.com/develop/connectivity/vpn
- Google Play VPN policy: https://support.google.com/googleplay/android-developer/answer/12564964
- Android storage manager: https://developer.android.com/reference/android/os/storage/StorageManager
- Google Play repetitive content: https://support.google.com/googleplay/android-developer/answer/9899034
