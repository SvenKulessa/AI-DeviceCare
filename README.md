# AI-DeviceCare

**Status:** P0 foundation underway — not yet a working Android application.

A privacy-first Android security and device-health platform, designed as two differentiated applications supported by one modular Kotlin/Jetpack Compose codebase.

- **AI-DeviceCare** (consumer): safer permission analysis, battery insights, device diagnostics, storage review, guided remediation, and progressively validated malware/network safety features.
- **AI-DeviceCare+** (business): planned organizational device posture, role-based management, consented device enrollment and multi-device reporting. A separate app is only released when it delivers demonstrably distinct business value.

## Planned release sequence

| Stage | Scope | Store readiness |
| --- | --- | --- |
| P0 / M0 | Governance, capability matrix, native Android skeleton, CI | Internal only |
| M1 | Free + Starter: app/permission posture, battery, diagnostics, safe cleaner, logging | First candidate public app |
| M2 | Pro: validated malware inspection, signatures and reports | Feature release |
| M3 | Elite: DNS/ad/tracker filtering, optional licensed VPN | Feature release |
| M4 | AI-DeviceCare+: true business fleet management and organizational dashboard | Separate business launch |
| M5 | Business enhancements: enterprise controls, reporting, SSO | Incremental release |

## Product principles

- **No root and no ADB.** Public Android APIs only; supported manual settings flows for OS-controlled actions.
- **No false guarantees.** Android cannot universally clear all internal caches of all apps or reveal all other applications' hidden system activity.
- **Permission, data and user-consent minimization.** Sensitive permissions gated by policy and separate user consent.
- **Explainable evidence, not fear-based detections.** No malware verdict without testable evidence, risk confidence and false-positive handling.
- **Battery-aware by design.** Diagnostics must not become the cause of device drain.
- **Safe fixes.** Preview changes, require permission, log and verify after action.

## Documentation

- [Repository & agent rules](AGENTS.md)
- [Planned modular architecture and milestones](docs/ARCHITECTURE.md)
- [Android capability and Play permission matrix](docs/ANDROID_CAPABILITY_MATRIX.md)
- [Product master roadmap — GitHub Issue #1](https://github.com/SvenKulessa/AI-DeviceCare/issues/1)

## Contributing / development state

For P0, work in feature branches and never bypass review gates. Build, tests, restricted Android permissions and Play distribution are **not yet configured**. The next step is to create an Android Gradle/Kotlin/Jetpack Compose skeleton for the consumer app plus future business app and shared modules; after that set up reproducible CI, app signing and automated tests.

No production deployment, APK, AAB or accepted Play Store listing is currently claimed.
