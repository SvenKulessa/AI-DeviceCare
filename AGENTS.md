# AGENTS.md — AI-DeviceCare repository operating rules

Version: P0-FDN-1 | Last reviewed: 2026-10-31
Scope: entire repository, both Android applications and shared libraries.

## Mission
Build two differentiated, privacy-first Android products using a shared modular Kotlin/Jetpack Compose codebase:
- **AI-DeviceCare**: consumer device health, battery, permissions, secure cleanup, and incremental malware/network protection.
- **AI-DeviceCare+**: distinct business product for authorized organizations, tenant-isolated fleet posture, policies, and reporting.
The commercial objective is staged Google Play publication. A feature is only sellable when implemented, tested, supportable and compliant.

## Repository authority, project structure and skills

- This root `AGENTS.md` is the **only repository-wide authorizing policy**. Machine-readable `governance/project-policy.json`, documentation, skills and workflows validate or explain this policy but do not create independent approvals or grant tools.
- Adopt evidence-first, least-privilege and compact PR sequencing ideas from [Capital-AI `SOLO_MAINTAINER_FLOW@1`](https://github.com/SvenKulessa/Capital-AI/blob/main/AGENTS.md) **as patterns only**. Its finance regulations, production deploys, required Docker checks, Render workspace and merge delegation do not apply to AI-DeviceCare.
- Maintain **one repository root = one Gradle project root** (`AI-DeviceCare/`). Exactly one root `settings.gradle.kts`; consumer `:apps:devicecare` and business `:apps:devicecare-plus` are modules, not independent nested projects or repositories; shared `core/*` and `features/*` are reusable modules.
- Follow `.agents/skills/registry.json` to choose task-specific skills, possibly several for cross-cutting scope. These are advisory operating playbooks and do not add a human approval, branch check, plugin, Android permission, SDK entitlement or merge authority.
- Run `node scripts/validate-governance.mjs` and `node --test scripts/validate-governance.test.mjs` when working with governance, modules or manifests. The read-only PR workflow is not automatically a branch-protection Required Check. Current build/test scope must be reported accurately.

## Non-negotiable safeguards
1. **No root, no ADB, no bypasses.** Use supported Android APIs and documented OS permission flows. Never claim the ability to clear all third-party private caches, read every system setting, or change arbitrary apps' permissions.
2. **Consent before action.** Sensitive permissions are off by default; every destructive or privilege-bearing action has a clear preview, risk explanation, explicit user consent and post-action validation. No stealth device modification.
3. **Privacy by default.** On-device-first analysis, minimum access and data, no sensitive data to analytics/LLMs by default; optional cloud uploads require explicit granular consent. Apply redaction and deletion/retention controls to diagnostic logs.
4. **Evidence before verdict.** Findings record signal, collection method, timestamp, Android API/capability, confidence, uncertainty, and remediation. An unusual permission or sensor event alone is not proof of spyware, ransomware, or compromise.
5. **Safe anti-malware.** No blanket claims of zero-day detection. Document false-positive/false-negative tests. Prefer signed, validated rule updates; no silent deletion or app uninstall.
6. **Battery-aware behavior.** WorkManager and event-driven sampling; configurable budgets for scanning, telemetry and VPN. Record own CPU, wakeup and battery overhead. Battery sensor and per-app consumption availability varies by OEM/API; label estimates.
7. **Single VPN integration.** Android user/profile permits one active VPN service at a time. VPN, DNS filtering and firewall features must share a coordinated, policy-compliant networking architecture.
8. **No Accessibility abuse.** Never use AccessibilityService to bypass Android permission prompts, drive system settings autonomously or automate unrelated interfaces. Justify every Play-restricted permission via a reviewed matrix.
9. **Licensing is a gate.** Scan direct/transitive dependencies, code and signature/blocklist datasets for commercial redistribution rights. GPL/copyleft integration requires approved design/legal review.
10. **No production mutation without a separate review.** Never silently publish to Play, change backend secrets, edit customer data or enable destructive system actions.

## Workflow / PR rules
- Confirm current `main`, relevant open PRs, issue scope and file ownership before editing.
- Work on `feature/*`, `p0/*`, or `fix/*` branches. Do not push directly to `main`.
- Keep PRs focused, link the corresponding roadmap issue and provide a rollback/revert plan.
- CI gates: successful Gradle build, unit tests, Android lint, dependency/license and secret scans; add instrumentation and real-device tests at milestone boundaries. Do not claim tests passed if they were not executed.
- Verify manifest/Play policies for every new dangerous, special or restricted permission. Provide a dated permission declaration and tested user-consent path.
- Require threat-model update for sensitive features (VPN, malware, diagnostics, telemetry, AI/cloud, enterprise enrollment).
- Do not merge with unresolved blockers, red CI, missing evidence or unapproved license/permission assumptions.
- Release approval explicitly belongs to repository owner. Distinguish code merged, AAB built, Play internal testing and production published.

## Modularity & compatibility
- Separate application modules and IDs; shared cross-app code belongs in reusable `core` or `feature` modules.
- Implement complete vertical slices behind gates; inactive unfinished features must not impair the earliest releasable tier.
- Modules must expose interfaces, injectable collectors, testable policies and versioned evidence contracts.
- Preserve database and configuration compatibility; destructive migrations need tested downgrade/export plan.
- App roles and subscriptions are server-verified. Never assume consumer billing entitlements automatically transfer between two distinct Play packages.

## Definition of done per feature
- User value and Android capability matrix updated.
- Scoped permissions and privacy impact documented.
- Clear UX for allow/deny/cancel/unsupported cases, TalkBack and accessibility reviewed.
- Tests and security/license checks pass on current head; real-device QA when relevant.
- Diagnostic logs are capped, redacted and deletable; no secrets.
- Performance, battery and network overhead measured where applicable.
- Documentation and release notes accurately describe available functionality.

## P0 development sequencing
1. Governance/architecture and capability matrix (this branch).
2. Native Kotlin/Compose project skeleton for each app and shared modules.
3. CI and reproducible build verification.
4. Battery, permission, diagnostic and basic cleaner minimum vertical slice.
5. Internal testing, Play policy assessment, billing verification and staged public release.
