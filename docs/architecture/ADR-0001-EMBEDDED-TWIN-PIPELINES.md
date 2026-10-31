# ADR-0001 — Two independent embedded pipelines in one Android Core

- **Status:** PROPOSED / EXTERNAL_ADVISORY_PENDING
- **Date:** 2026-10-31
- **Owner repository:** `SvenKulessa/AI-DeviceCare`
- **Current scope:** P0 design and build boundary; no Android runtime or backend provisioned
- **Authority:** only root `AGENTS.md`; this ADR adds no Required Check, merge delegation or cost approval.
- **Related:** [P0 product masterplan #1](https://github.com/SvenKulessa/AI-DeviceCare/issues/1), [architecture](../ARCHITECTURE.md), [governance as code](../GOVERNANCE-AS-CODE.md), Draft PR #2.

## Core design hypothesis to be externally evaluated

Adopt one repository and **one Gradle project root**, with two distinct app modules `:apps:devicecare` (consumer) and `:apps:devicecare-plus` (enterprise). Both share feature and core modules. The two independent pipelines are created as first-class interfaces **from P0, not bolted on after M1**. They are not two hosted microservices, two competing Scoring Dispatchers or duplicate CI suites.

```mermaid
flowchart LR
    D[Android Public API Collectors] --> E[(EvidenceEnvelope@1)]
    E --> QA[Core Quality Projection @1]
    E --> DQ[Device Evidence / DQ Gate]
    QA -. contract tests + exact source SHA .-> CI[GitHub CI QA Checks]
    DQ --> REG[Device Model Registry @1]
    REG --> SC[One Device Scoring Dispatcher]
    SC --> F[Canonical Device Assessment @1]
    QA --> R[Read-only quality status]
    F --> UI[User-facing findings + guided actions]
    R --> UI
```

**Separation of authorities:** QA measures engineering evidence (tests, static permission checks, SBOM/license, code provenance, build SHA/digest, own-app energy overhead). Device Scoring evaluates only device-security/battery/posture signals for which the Android app has lawful public-API access and user consent. QA may test the score functions but not choose risk weights, rewrite device findings or grant automatic remediation; Device Scoring cannot produce a CI or security compliance PASS. Both may read the same immutable evidence identity; no shared mutable policy/score authority.

### Pipeline A — `CORE_QUALITY@1`

- **Build/CI executable lane:** Android build, unit/contract/negative tests, Android lint, restricted manifest gates, supply-chain scans, Kotlin ABI/contracts, accessibility where verified, measured CPU/network/battery budgets.
- **Embedded read-only core representation:** quality contract structures and run/diagnostic status (no full repository scan on phones).
- **Outputs:** `QualityReport@1`, exact Git SHA, artifact digest, profile, per-check result `PASS|FAIL|NOT_PROVEN|NOT_APPLICABLE`, scope, tested environment, observedAt.
- **Fail-closed:** missing artifact/build/security evidence is `NOT_PROVEN`, not `PASS`. QA evidence authorizes neither Play production nor third-party rights.

### Pipeline B — `DEVICE_SCORE@1`

- **On device:** `EvidenceEnvelope@1` -> freshness/permissions/availability gate -> versioned `DeviceModelRegistry` -> exactly one `DeviceScoringDispatcher` -> domain evaluators (permission, battery, posture; malware and network are later milestones) -> `CanonicalDeviceAssessment@1`.
- **Outputs:** factor evidence IDs, model/version, confidence, measured coverage, status `ASSESSED|NOT_ASSESSABLE`, limitations, risk factors, recommendation category `direct|guided|unsupported`.
- **Fail-closed:** absent Android signal, outdated observations, unsupported OEM API or denied permission => `NOT_ASSESSABLE` for the affected factor, never fabricated zero risk. Do not reuse financial numerical formulas, providers, rankings or historical price data.

### Suggested module placement (subject to build/test validation)

```text
AI-DeviceCare/                    # one Gradle project root / repository root
  settings.gradle.kts             # one and only settings root, future P0.2
  apps/
    devicecare/                   # consumer Android app
    devicecare-plus/              # business client, kept unpublished until distinct
  core/
    evidence/                     # immutable source contracts and provenance
    quality/                      # read-only QA status/contracts
    scoring/                      # registry, dispatcher, deterministic evaluator API
    logging/                      # capped, PII-redacted local events
    governance/                   # version/status contracts; root AGENTS owns policy
    billing/                      # entitlement interface, per-platform adapters
    ui/                           # common design system
  features/
    battery/ permissions/ diagnostics/ cleaner/
    malware/ network/ fleet/      # gated milestones; do not ship placeholders as value
  .github/workflows/             # actual CI quality execution outside the device
```

## Reference reuse/provenance (read source; do not copy blindly)

| Source repository | Concrete files | Reuse | Do not copy |
|---|---|---|---|
| [Finance](https://github.com/SvenKulessa/Finance) | `src/platform/Quality/README.md`, `docs/architecture/QUALITY_CENTER_CORE_ORCHESTRATION.md` | Read-only quality projection, exact-head evidence, fail-closed check status | Finance domain authorities and Chapter-12 numeric formulas |
| Finance | `docs/adr/ADR-0087-single-scoring-architecture-uai-model-registry.md`, `src/platform/Scoring/ScoringDispatcher.ts` | One registry/dispatcher, versioned result and provenance, no invented score | Asset classes, factors, provider-rights, investment score weights |
| [Capital-AI](https://github.com/SvenKulessa/Capital-AI) | `AGENTS.md`, `.agents/skills/registry.json` | Solo-maintainer source of truth, task-based skills and evidence | Render workspace, NATS, finance legal requirements, merge delegation |
| Capital-AI | `docs/security/CADS-GROWTH-ENGINE-QUALITY-20261009.md`, `packages/benchmark-core/growth-quality.mjs` | Versioned technical checks, negative fixtures, artifact SHA and distinction from publication approval | CADS commerce/Marketplace product or any implied licensing right |
| [Capital-AI-Mobile](https://github.com/SvenKulessa/Capital-AI-Mobile) | `.github/workflows/mobile-quality.yml`, `docs/RELEASE_SIGNING.md`, `scripts/quality.sh` | Gradle/lint/scan/commit digest/signing procedure, explicit release blockers | Signing key identity, private market scoring, WebView architecture as default |

References are working patterns, not external module imports. Every port must record exact source commit/file, license/provenance check, Kotlin reimplementation scope, and new positive/negative test fixtures.

## Backend, payments and brand boundaries

1. **Supabase:** one future standalone `AI-DeviceCare` project, not a shared Capital-AI database/Auth project. Remain offline-first in P0. When account sync/entitlements or B2B tenancy is needed, provision independently after price and security approval. DeviceCare and DeviceCare+ can share this project only with verified row-level tenant isolation, separate app identifiers and service-role secrets server side. Additional projects incur per-project compute costs (see [Supabase billing](https://supabase.com/docs/guides/platform/billing-on-supabase)).
2. **Billing:** same legal entity may consider a shared Stripe account for eligible website/B2B contracts, with separate Product/Price IDs, tenant metadata, webhook secrets/routes, idempotency and branded receipts. Google Play Billing is the default for Play-distributed digital in-app subscriptions unless a specific allowed EEA program applies; server-side `EntitlementLedger@1` validates provider-specific purchases and refunds ([Google Play payments](https://support.google.com/googleplay/android-developer/answer/10281818?hl=de), [backend verification](https://developer.android.com/google/play/billing/backend)).
3. **Branding:** P0 brand decision + provenance contract; M1 consumer icon, tested accessibility, store images, text and rights; M2/3 scope-specific security claims and blueprints; M4 distinct Enterprise+ identity/Fleet UI. Capital-AI gold and Universe style remain a source reference, not automatic license for arbitrary third-party redistribution; check [Capital-AI brand provenance](https://github.com/SvenKulessa/Capital-AI/blob/main/docs/licenses/Capital-AI-BRANDING.md). Two similar Play applications risk the [repetitive content policy](https://support.google.com/googleplay/android-developer/answer/9899034?hl=de).
4. **Seven target prices (not active):** Consumer Free 0 EUR, Starter 3.99, Pro 6.49, Elite 14.79. Business+ Enterprise 29, Business Pro 59, Business Enterprise 129. Assumed monthly, still to verify tax/fees, real features, device limits, data/vendor costs.
5. **Operating model:** no NATS, Kafka, separate CADS hosted runtime, extra paid provider or unnecessary Supabase project solely for core QA/scoring. Later scale by data and feature volume only when measurements justify it.

## Timeboxed scenario, not delivery promise

| Period | Work / branding decision | Public release implications |
|---|---|---|
| **Nov 2026 / P0** | One Gradle project, two app modules, immutable evidence contract and both pipeline interfaces, local fixtures, CI tests, root governance, initial shared/design tokens, brand provenance/license inventory | Internal builds only |
| **Nov–Dec 2026 / M1** | Consumer Battery/Permission/diagnostic/cleaner vertical slices, positive + negative tests, battery overhead, Free/Starter Play Billing, privacy and brand assets | First Consumer Play submission **only if all app/policy gates pass** |
| **Dec 2026–Jan 2027 / M2** | Validated Pro malware/static scan, false-positive/negative evidence, no zero-day claims | Optional independent Pro release |
| **Jan 2027+ / M3** | Elite DNS filter, optional VPN within single `VpnService`, leak/battery/license testing | Only upon proof and provider/policy rights |
| **M4 / later** | Differentiated Business+ app with authorized organization/fleet management and tenant isolation | Separate Play listing only after material standalone value |

## External advisory to resolve before architecture promotion

- A native Kotlin/Compose vs B KMP shared core vs C TS/WebView bridge, weighed by API access, runtime/battery cost, security and solo maintenance. Native Kotlin is the **working hypothesis**, not externally certified.
- Exact `EvidenceEnvelope@1`, `QualityReport@1`, `CanonicalDeviceAssessment@1` schemas and unsigned/invalid/stale fixture behavior.
- What QA must reside in CI vs app runtime; how to measure application battery overhead reproducibly.
- Android compileSdk/AGP/Gradle/Java versions at P0 implementation, Play policy and permission declarations.
- COGS/Billing reconciliation and whether business-app runtime and own project are financially justified.
- External branding/IP/title and logos rights.

**Status today:** `PROPOSED`. No Core Kotlin code/build, dedicated Supabase project, Stripe product, billing enrollment, real APK or Play review is claimed. No production resources created; no merge.
