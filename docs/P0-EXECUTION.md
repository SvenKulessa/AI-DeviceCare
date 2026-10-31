# AI-DeviceCare — P0.2 first working vertical slice

- **Scope:** one Gradle root with 2 app modules; 4 core libraries; 2 isolated pure Kotlin pipelines; P0 informational Compose UI.
- **Authority:** root `AGENTS.md`; `docs/architecture/ADR-0001-EMBEDDED-TWIN-PIPELINES.md` remains proposed; no production risk model authorized.
- **Build toolchain:** Android Gradle Plugin 9.4.0, Gradle 9.6.0, JDK 17, compileSdk 37, targetSdk 36, minSdk 26, Kotlin/Compose compiler plugin 2.4.10, Compose BOM 2026.09.00.
- **One project folder:** `settings.gradle.kts` only at repository root. Two application IDs `online.capitalai.devicecare` and `online.capitalai.devicecare.plus`. Both are distinct installable applications, but DeviceCare+ is *internal prototype only* and not approved for store release.
- **Core 1:** `:core:quality` pure deterministic quality-check summary bound to SHA, no PASS if absent or failed evidence. The actual CI runner executes tests, build and lint; the Android app does not run source analysis.
- **Core 2:** `:core:scoring` evidence envelope, versioned static model registry, single deterministic dispatcher, no numeric score with denied/missing/stale/duplicate signal. Synthetic fixture risk mapping is a **P0 engineering test of the contract, not a released device/security risk model**.
- **UI:** informational P0 welcome panel with explicit NOT_PROVEN and NOT_ASSESSABLE, without data collection or elevated permissions.
- **Privacy:** no INTERNET, QUERY_ALL_PACKAGES, storage-all-files, usage stats, overlay, accessibility or VPN permissions.
- **Tests:** isolated JUnit cases in both pipelines for happy path, missing or bad inputs, stale and duplicate signals.
- **CI:** `.github/workflows/android-p0.yml` builds both debug APKs, executes Kotlin unit tests and lint, prints artifact SHA-256, uploads **internal only**. `.github/workflows/governance-validation.yml` independently checks governance and dangerous permission baseline.
- **Limitations:** no Android 15 physical-device test, cloud/Supabase, subscriptions, device collector, malware, cleaner, VPN, signing key or production. No store release.
- **Follow-up:** M1 implement Android permission/device battery collector through public API with opt-in and `:features:battery`, typed evidence adapter, and measured performance. Incorporate user-confirmed branding and billing only after privacy/license/Play review.
- **Local build path:** where full JDK 17, Android SDK 37, Gradle 9.6 are available, run `gradle :core:quality:testDebugUnitTest :core:scoring:testDebugUnitTest :apps:devicecare:assembleDebug :apps:devicecare-plus:assembleDebug`.
