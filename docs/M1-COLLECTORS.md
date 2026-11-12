# M1 preview — one-shot BatteryManager & visible-permission collectors

Status: **CODE IMPLEMENTED / DEVICE RUNTIME NOT YET VERIFIED**. Android public API only, no elevated privileges, no Google Play release. Root `AGENTS.md` is the only repository policy authority. Scope: consumer AI-DeviceCare; AI-DeviceCare+ stays an internal-only preview.

## Actual end-to-end flow

1. User opens consumer app and reads **limited visibility and local-only disclaimer**.
2. User selects **Lokale Prüfung starten**. No collector or background job runs beforehand.
3. Coroutine runs both collectors on `Dispatchers.IO`; each returns a versioned `DeviceObservationBatch` with bounded signal values and explicit availability.
4. In-memory UI displays measurement/status and scope, **never raw installed-package identifiers**, never a malware verdict or risk score.
5. When the UI is destroyed, no database, persistence, analytics, network transfer or job remains.

### BatteryCollector — `:features:battery`
- Reads sticky `Intent.ACTION_BATTERY_CHANGED` and `BatteryManager.BATTERY_PROPERTY_CAPACITY` once.
- Produces `battery_percent`, `battery_temperature` (tenths of a degree Celsius normalized to C), `battery_charging`, and `battery_health_reported`.
- System health flags are **not** battery wear/capacity percentage. Never attribute per-app battery drain.
- Invalid level/scale/capacity, unsupported OEM flags, no sticky broadcast or errors map to explicit `UNSUPPORTED|ERROR|DENIED`, never to "0%" or "healthy".
- Sampling uses no scheduled worker, foreground service, wakelock or battery optimization bypass.

### AndroidPermissionCollector — `:features:permissions`
- Uses `PackageManager.GET_PERMISSIONS`, `getInstalledPackages` and `getPackageInfo(context.packageName)`, subject to **Android package-visibility filtering**.
- Exposes aggregate counts only: visible packages, up to 250 sampled packages, their requested-permission count, this app's requested and confirmed granted-permission counts, sampling-truncated marker.
- **Not** an inventory of all installed/system apps. Does **not** infer another package's granted permissions or assign a privacy/malware threat verdict. No `QUERY_ALL_PACKAGES` or `<queries>` added.
- The raw list of package names never leaves the collector and is not written to logs, evidence reports, database, or network.
- Cancellation is checked between package metadata calls and before publishing aggregates. A synchronous Android call already in progress can finish. A cancelled scan returns no new batch.
- Inaccessible/unknown own-permission flags remain `UNSUPPORTED`, not a granted/denied guess.

### Canonical versioned observation schema

`ObservationContract.VERSION = devicecare-observation/1`. Each `DeviceObservationBatch` has a fixed `collectorId`, `collectedAtMillis`, `scope`, explicit `observations` and contract version; each `DeviceObservation` has `id`, `status`, timestamp, optional expiry, bounded value, optional unit and fixed limitation code. Constructors reject invalid IDs, missing expiry for observations, duplicated IDs, invalid scope, and fabricated values for unsupported/denied/error status. A displayed reading becomes stale after 60 seconds; the UI does not claim it has refreshed automatically.

**Quality boundary:** `core:quality` continues to evaluate CI/build evidence only. **Device risk scoring boundary:** `core:scoring` is unchanged; the raw battery/permission observations are **not mapped** to the test-only `RiskLevel` or dispatched as a real risk score. `NOT_ASSESSABLE` remains the only truthful customer-facing risk score until validated calibration, risk criteria and real-device negative cases exist.

## Privacy and threat model

| Risk | Mitigation | Still unverified |
| --- | --- | --- |
| Installed-app information reveals sensitive user interests | Store/process aggregated counts only; do not include names, exact app list or packages in logs/exports | Real device Android 12–16 app-visibility behavior |
| False confidence due to Android visibility filtering | Show explicit visibility limitation and `sample_truncated` | OEM-specific coverage |
| Overstated battery-health rating | Distinguish platform health status from cell wear/remaining capacity; no battery-performance claim | OEM support for health/status/temp |
| App drains battery itself | On-demand single pass, `Dispatchers.IO`, hard 250-package aggregation cap; no polls | Real-device CPU/battery profiling |
| Raw data exfiltration | No Internet permission, remote APIs, analytics, persistence or logging of observations | Full produced APK manifest and dynamic network trace |
| Misleading security verdict | No Device Scoring integration and no numeric real score | Calibrated score model, consent-based remediation |
| UI cancellation / rotation | Compose scope tied to current composition; no long-running background service | Instrumentation / rotate / process death |

Privacy classification: device battery state = local device diagnostic data; package visibility and permission metadata = potentially sensitive installed-app information (raw names intentionally discarded). No cloud or diagnostic log collection in this feature. For future export, implement separate explicit consent, redaction, capped retention, deletion and tenant isolation before enabling it.

## Tests and evidence

- `:core:evidence:testDebugUnitTest`: version, expiry, impossible values, duplicate IDs, invalid scope.
- `:features:battery:testDebugUnitTest`: valid measurement, missing/invalid sensor, capacity fallback and unavailable sticky broadcast.
- `:features:permissions:testDebugUnitTest`: filtered scope, truncated sampling, missing flags, invalid aggregates, denied access.
- Existing `:core:quality:testDebugUnitTest` and `:core:scoring:testDebugUnitTest` remain independent regression suites.
- Consumer + Business debug assemble, both lint tasks, collector lint tasks and no-risky-manifest check in `.github/workflows/android-p0.yml`.
- **Unit tests cover pure mappers, NOT live Android services**. Android 15 API-35 emulator Compose instrumentation now exercises the explicit button and on-demand flow (CI status must be verified separately). OEM and real battery-energy reports remain separate M1 release blockers.
- Built APK manifest policy now uses Android SDK `apkanalyzer` and `aapt2`, not only source manifest grep. It rejects any merged permission, unexpected service/receiver/provider/activity alias, enabled backup or cleartext, extra launch activities, mismatched IDs and old target SDK. Review `evidence/apk-manifest-policy.json` and CI result for exact-head confirmation.
- Evidence strings are allowlisted by signal ID, value type/unit, bounds, 60-second expiry and collector scope. Regressions attempting to include package IDs, arbitrary device identifiers or longer TTL are negative tested.
- Consumer UI exposes elapsed monotonic scan time and process CPU time **informationally only**; neither measures battery power or proves optimization.

### Local reproduction (JDK 17, Android SDK platform 36, Gradle 9.6.0)
```sh
node scripts/validate-governance.mjs
node --test scripts/validate-governance.test.mjs
gradle --no-daemon :core:evidence:testDebugUnitTest :core:quality:testDebugUnitTest :core:scoring:testDebugUnitTest :features:battery:testDebugUnitTest :features:permissions:testDebugUnitTest
gradle --no-daemon :apps:devicecare:assembleDebug :apps:devicecare-plus:assembleDebug :apps:devicecare:lintDebug :apps:devicecare-plus:lintDebug :features:battery:lintDebug :features:permissions:lintDebug
```

## Known required follow-ups
- Android 15 emulator CI now covers basic Compose semantics and explicit scan start; physical Android 15/OEM still needed for sensors, TalkBack, rotation, process death and layout at large text sizes.
- Actual packaged **debug APK** manifest is now inspected automatically. Release AAB/signed artifact must be independently verified in a later release work package.
- True per-app permission auditing requires Play-eligible package visibility and user/privacy justification; do not silently broaden.
- Battery optimizations are currently **guidance-only**, not executed system modifications.
- Backend, Supabase, Stripe, user accounts, AI malware engine and VPN are deliberately out of this package.
- No claim of public Store readiness; separate Play Billing and commercial/privacy review required.

- [Detailed QA/remaining manual test ledger](M1-ANDROID15-QA.md) tracks each gate, evidence and issue #5.

## References
- https://developer.android.com/reference/android/os/BatteryManager
- https://developer.android.com/training/package-visibility
- https://developer.android.com/reference/android/content/pm/PackageManager
- https://developer.android.com/guide/topics/permissions/overview
