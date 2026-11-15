# Privacy policy – AI-DeviceCare Consumer M1

> **INTERNAL DRAFT / RELEASE BLOCKED.** The legal publisher, postal address, privacy contact, legal-basis review, support-data retention and public HTTPS URL must be completed before any Google Play upload. Text matches the in-app view, subject to final legal review.

- Product: AI-DeviceCare (Consumer M1)
- Package: `online.capitalai.devicecare`
- Version: 2026-11-15
- Evidence baseline: Main `1b4fe12e12fc412e1a473a9c8070980a60080bc0`, PR #19 `9cc886394914c6020db6a4c5fb58a617c9e34846`, PR #17 `8d0c46a045974c7afe5bdafdd2ba4016d92ca20b`
- Public privacy URL: **NOT_PROVEN – verify before release**

## 1. Controller and contact

AI-DeviceCare (package online.capitalai.devicecare). Legal publisher, postal address and privacy contact: [TO BE COMPLETED BEFORE RELEASE]. This is an internal draft and is not approved for public use.

## 2. Scope

This notice covers only the Consumer M1 app. It does not cover the separate AI-DeviceCare+ business app or future cloud, VPN, malware detection or subscription features.

## 3. On-device access and purpose

Only after a user starts a check, the app processes battery readings (level, charging state, temperature and Android-reported health status), coarse memory/thermal signals, Android-visible app and permission metadata, and permissions of AI-DeviceCare itself. Depending on device pressure, sampling is limited to at most 250, 75 or 25 visible apps. Android may hide other apps; this is not a complete installed-app inventory.

## 4. Optional system and Google security check

A separate user-initiated check reads Android version, API level, reported security patch date, and available version information for Google Play services, Play Store and the active WebView provider. The two Google packages have narrow visibility declarations only. There is no live CVE lookup, malware scan or claim of verifying Play Protect status.

## 5. Retention and deletion

Results are held in memory for display only. This M1 preview has no account, persistent diagnostic history, diagnostic upload or planned storage of inspected package names. In-memory results disappear when the app process ends. Android app backup is disabled. Operating-system diagnostic records are outside the app's control.

## 6. Transfers, sharing and recipients

These M1 checks use no cloud analysis, analytics, advertising SDKs or app-managed network upload. The inspected debug package declares no INTERNET permission. If a user explicitly opens Android Settings, Play Store or an official Google help webpage, the external apps and websites apply their own privacy terms; AI-DeviceCare does not pass diagnostic results to them.

## 7. Permissions and security

This scope requests no dangerous Android runtime permissions, all-files access, VPN, AccessibilityService or unrestricted package visibility. Raw package names are used transiently on-device for Android metadata queries and are not shown in the results. Unavailable readings are marked as unknown or unsupported.

## 8. Legal basis and rights

The final GDPR legal basis assessment must be confirmed by the controller before publication. Where applicable, individuals have statutory rights to access, rectification, erasure, restriction and complaints to a data protection authority. The app stores no accounts or server-side diagnostic records; separate rules are required for any support correspondence.

## 9. Support correspondence

Information users voluntarily send outside the app for support is not part of the on-device M1 diagnostic process. Before external release, the contact method, controller, purposes, recipients and retention/deletion period for support requests must be confirmed.

## 10. Changes to this policy

Version: 15 November 2026 – internal draft. Changes in shipped features, SDKs, transfers or retention must update this notice, in-app text and Play Data safety declarations together. A public HTTPS privacy policy URL must be verified before Play upload.


---

**Release gate:** Public hosting with valid TLS, global availability and no login, plus reconciliation with the final signed AAB and Play Data safety form, remain unverified.
