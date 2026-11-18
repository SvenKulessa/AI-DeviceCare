# AI-DeviceCare → DeviceCare-KI integration readback (2026-11-18)

## Sources and baseline
- Source: `SvenKulessa/AI-DeviceCare` main `177b4e1dbaccb4b2789ebcd8c2ffc13f73cc5e2b`.
- Target: `SvenKulessa/DeviceCare-KI` main `52169108a4c3a42386fbbd7223fe0a7a46fddd22`.
- Target stays `com.aistudio.formwerk.vktmrx`, single `:app` Gradle module.
- This is a selective **source port**, not a Git-history merge or copying the
  old multi-module application over Firebase Auth/Firestore/AI, UI or preferences.

## Ported and preserved
- On-demand System & Google security workflow: Android release, patch age
  heuristic, local GMS/Store package versions and active WebView provider.
  Explicit launch, transient UI-only evidence, scoped package visibility and
  handoff to settings/Play Store/release notes; **no automatic device mutation**.
- Old SystemSecurityWorkflowScreen UI and fixture tests adapted to `com.example`.
- API 24/25 compatibility: no `java.time` on-device and API-26 WebView guarded.
- Existing UI, battery/permissions, scoring, quality, Firebase and AI preserved.
- Added a PR-gated Android CI workflow: Trivy HIGH/CRITICAL, unit tests, lint,
  assembleDebug, APK signature verify, SHA-256 and downloadable 14-day artifact.
- CI creates an **ephemeral** debug keystore compatible with the existing
  debug signingConfig; this is a signed *internal* APK, **not** a release key.
  Installs from separate CI runs cannot reliably update one another because
  signatures differ. Never replace an installed Play build with this artifact.

## Deliberately NOT bulk-copied
- `:apps:devicecare-plus` — distinct unimplemented business product, not a
  missing consumer integration. Requires separate package, test and approval.
- Old `core/*` and `features/*` — functions already exist under target
  `app/src/main/java/com/example`; duplicating introduces conflicting contracts.
- Old Gradle/AGP9.6 CI — target uses AGP 9.1.1 and Gradle 9.3.1. Robolectric SDK 36\n  requires JDK 21, while Android app bytecode remains Java 11.
- Old Play release AAB workflow — no proof that the historical release key,
  Play app identity and target Firebase project are bound to this new app.
- No live patch lookup, Play Protect inference, background surveillance,
  Cloud Sync of system evidence or automatic device remediation.

## Release and integration blockers
1. Record GitHub Actions run/job conclusions and signed APK signer SHA-256 for
   the **exact** target feature-branch commit. A workflow file alone is NOT
   build evidence.
2. Firebase Auth/Firestore/AI require owner-verified configuration and
   Firestore/App Check rules; missing `default_web_client_id` now fails closed
   at sign-in rather than preventing an isolated Android compilation.
3. For a **release-signed** APK/AAB use the existing owner-controlled keystore
   only; verify alias, package, certificate continuity and secrets outside git.
4. Verify Android 15 Z2459 TalkBack, large font, Play System update and battery
   overhead on actual hardware; emulator is not OTA or Play Protect evidence.

## Rollback
Revert the migration PR commit(s). No database schema, live credentials,
Play settings or customer records are migrated.
