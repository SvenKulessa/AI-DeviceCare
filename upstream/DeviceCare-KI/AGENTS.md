# AGENTS.md — DeviceCare-KI repository execution policy

Version: MIGRATION-P0-1 | 2026-11-18. Scope: DeviceCare-KI only.

## Repository authority
This is the sole repository-wide operating policy. The prior
[AI-DeviceCare AGENTS.md](https://github.com/SvenKulessa/AI-DeviceCare/blob/main/AGENTS.md)
is a reference for approved patterns, not an independent authorization here.
Preserve the single existing Gradle root `settings.gradle.kts` and `:app`,
applicationId, Firebase/Cloud and UI features. Do not bulk-overwrite them with
the former multi-module project. Proven features may be ported as isolated slices.

## Execution and evidence
Work on feature/fix branches and use pull requests. Confirm current main and
open PRs, map Android capability and privacy boundaries, implement the smallest
compatible change, run applicable unit/negative tests, Android lint, build and
security/license scans. Record precise commit SHA, run ID, exit status, APK
hash and signing certificate. No merge while CI or device acceptance is missing.
Do not invent test results. Supply rollback instructions. Only the owner may
approve Google Play publishing and production release signing.

## Safety and consent
No root, ADB, AccessibilityService automation, broad hidden package access,
stealth diagnostics, third-party cache clearing or unapproved permissions.
Device findings must distinguish local evidence from unproven external
freshness, CVEs, Play Protect, Play System patch, OEM OTA and malware verdicts.
No automated changes to Android/Google settings. Sensitive scans require
user initiation, minimal collection, bounded resource budgets and redaction.

Optional Firebase Auth, Firestore and Firebase AI are separate opt-in cloud
data paths: never upload local security evidence automatically. Keep Firebase
configuration and signing keys out of the repository. Never replace or expose
an existing production signing key. Debug APKs are signed for internal testing
and are not production-ready or suitable for Google Play distribution.

## Merge / release gates
Require green Gradle unit tests, lint, signed-debug APK validation, Trivy
vulnerability/secret/license scan and manifest review. Before any public Play
release, independently verify consent, Firebase data rules, package signature
continuity, accessibility, real-device Android 15, privacy policy, Data Safety
and release provenance. Do not create a release key on CI for a production build.


## Cross-repository execution — owner direction 2026-11-18

Adopt the execution pattern of [CAPITAL-AI Root AGENTS.md](https://github.com/SvenKulessa/Capital-AI/blob/main/AGENTS.md), contract `SOLO_MAINTAINER_FLOW@2`, **as repository-local practice**. This root file remains the only authorizing instruction here. No foreign workflow, rule, check, provider credential, deploy authorization or finance-specific regulation is imported.

- **One-pass convergence:** inspect current main, this root policy, open PRs/issues and the actual affected checks once; reuse existing evidence and prioritize a single smallest complete change. Prefer repairing an existing scoped PR to opening a competing PR. Classify old PRs as MERGE_READY, REPAIR, SUPERSEDED, BLOCKED or NOT_PROVEN against exact SHAs; close only verified duplicates/superseded work with a link.
- **Execution-first:** analyze → implement → relevant positive/negative tests → repair bounded failures → review actual CI/rulesets/reviews → merge when legitimately authorized and fully green → verify new main. Do not create empty milestone PRs or redundant evidence-only branches. Never claim a blocked gate is green.
- **Merge immediately when eligible:** this owner instruction delegates safe repository-local *code PR* merging within active protections, using the merge method actually permitted by that repository. Check current base/head SHA, mergeability, all relevant CI/security/license checks, review threads and requested reviewers; use an expected-head-SHA guard. For critical auth, secrets, data, signing, release/publishing, paid actions and missing human approvals: stop the particular protected action and report BLOCKED instead of weakening gates. No standalone approval loop for routine code when checks and existing authorization suffice.
- **Domain orchestration:** select one accountable primary domain among PRODUCT, MARKET, PLATFORM, TRUST, GROWTH. Invoke fit-for-purpose specialists (Android/mobile, accessibility, AI/data, app security, compliance, release) from the advisory [CAPITAL-AI skill registry](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/registry.json) and this repository's actual local tools. Specialists exchange evidence only, not new permissions or independent policies.
- **Backlog hygiene:** reconcile issues to code/PRs on each scoped run; deduplicate and link cause to fix. Close confirmed finished/superseded issues only after code and checks prove resolution. Keep release and deployment approval boundaries intact.

This addendum does not change branch protection, required checks, action permissions or production/release gates.
