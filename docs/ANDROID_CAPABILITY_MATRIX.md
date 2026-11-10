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

## M1 implementation evidence — 2026-11-05

A **limited consumer developer preview** now contains two on-demand collectors (see [M1-COLLECTORS.md](M1-COLLECTORS.md)):

- `AndroidBatteryCollector`: sticky `ACTION_BATTERY_CHANGED` + `BatteryManager` capacity fallback; maps level, temperature, charging status and system-reported health, explicitly not chemical cell wear or per-app battery drain.
- `AndroidPermissionCollector`: `PackageManager.GET_PERMISSIONS` for only OS-visible packages, at most 250 sampled; own-app permission grant counts; no raw installed-package names in UI/logs.
- `DeviceObservationBatch@devicecare-observation/1`: explicit availability statuses and timestamped bounded observations, no implicit numeric malware/device-risk score.
- Both collectors are invoked from a user-controlled button, off the Compose main thread. No `INTERNET`, `QUERY_ALL_PACKAGES`, `PACKAGE_USAGE_STATS`, `MANAGE_EXTERNAL_STORAGE`, VPN, AccessibilityService or background job.
- **Not a Play or field-test PASS**: Android 15 physical-device API behavior, OEM compatibility, consent UX/accessibility, APK manifest inspection and battery overhead remain to be verified. No third-party app permission changes or security score exist.

## M1 device-adaptive permission sampling — Issue #7

| Signals read after the manual scan starts | Maximum visible apps sampled |
| --- | ---: |
| All resource signals available and clear | 250 |
| Low-RAM class, power saver, moderate thermal pressure or unavailable signal | 75 |
| Active memory pressure or severe thermal status or above | 25 |

The Consumer preview reads coarse states from ActivityManager and PowerManager, including thermal status from Android API 29 onward. Unsupported, denied, failed or unknown signals select a conservative budget. The strictest applicable cap wins. The chosen cap and reasons appear with scan results under **Geräteprofil**. The same section explains the coarse resource states and whether the battery and scoped permission signals were observed, unavailable, denied or failed. The detail rows retain the original evidence values and sources. The same shared feature can be reused by Plus when it has a scan entry point.

For reduced budgets, the app asks Android for the visible set without permission metadata, then requests permission details only for the sampled entries and the app itself. The visible count is still partial and OS-filtered. If an entry disappears during the scan, the collector reports unavailable results rather than a misleading total. The default path retains the existing bulk query. The cap limits metadata scope, while actual CPU, duration and energy impact require measurements. The budget does not affect battery evidence or the risk score.

No new permissions, identifiers, persistence, background work or network access. Raw package names are only used transiently to request Android metadata and are excluded from display and logs. The profile is reread on every manual scan. Real-device and OEM resource behavior remain unverified for this slice.

After a manual scan, the Consumer preview can open Android's battery-saver screen or its own App info using public Settings intents. A missing or restricted direct screen falls back to general Settings with a navigation hint. If general Settings is also unavailable, the app displays a manual hint. This does not change settings or request additional permissions.

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
