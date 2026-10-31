# AI-DeviceCare governance-as-code — P0 adaptation

## Sources reviewed (2026-10-31)
Source repository: [SvenKulessa/Capital-AI](https://github.com/SvenKulessa/Capital-AI).
- [AGENTS.md](https://github.com/SvenKulessa/Capital-AI/blob/main/AGENTS.md) — `SOLO_MAINTAINER_FLOW@1`; sole repository-wide authorizing policy; evidence-first, scoped change, PR sequencing, no duplicate administrative gates.
- [.agents/skills/README.md](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/README.md) and [registry.json](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/registry.json) — declarative skill inventory and deterministic validation.
- [mobile-engineering](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/mobile-engineering/SKILL.md) — Android lifecycle, permissions, signing, artifacts and real-device evidence.
- [trust-engineering](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/trust-engineering/SKILL.md) — privacy, supply chain, SBOM and trustworthy findings.
- [platform-observability](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/platform-observability/SKILL.md) — privacy-aware logs, costs, SLOs; adapted to Android battery impact.
- [incident-response](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/incident-response/SKILL.md) — evidence-preserving triage and rollback.
- [accessibility-engineering](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/accessibility-engineering/SKILL.md), [ai-engineering](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/ai-engineering/SKILL.md) and [revenue-engineering](https://github.com/SvenKulessa/Capital-AI/blob/main/.agents/skills/revenue-engineering/SKILL.md).
- [Domain release governance](https://github.com/SvenKulessa/Capital-AI/blob/main/docs/governance/DOMAIN-RELEASE-GOVERNANCE.md) and [main protection](https://github.com/SvenKulessa/Capital-AI/blob/main/docs/security/MAIN-PROTECTION.md) explicitly say SUPERSEDED/NON-AUTHORIZING, and are **not** copied.

## Adopt / adapt / reject
**Adopt:** single root authority, small reversible PRs with evidence; machine-readable registry; repeatable validation with negative tests; trust boundaries, status vocabulary, least privilege and fail-closed decisions.

**Adapt:** Android rather than finance domain; Play Billing instead of Stripe entitlements; Kotlin/Compose modules instead of React/Node; Android signing + AAB evidence instead of Docker images; on-device-first logs, battery-efficient scanning and Play-restricted permission controls.

**Reject:** Capital-AI's specific GitHub Required Checks (`Docker Security Gate`/`Domain Governance`), Render workspace, financial/NATS license restrictions, Supabase defaults, automatic merge delegation and domain-specific agent handoff/ownership. Those are not rights or rules for AI-DeviceCare.

## Single project folder, two app variants
The **Git repository root itself is the sole Android project directory**: `AI-DeviceCare/`. It will contain exactly **one** `settings.gradle.kts`, **not** a second Gradle project under `apps/`. This avoids an unnecessary nested `AI-DeviceCare/AI-DeviceCare/` directory. Inside that one project, `:apps:devicecare` and `:apps:devicecare-plus` are separate application modules; `:core:*` and `:features:*` are shared Gradle library modules. Each app has its own Android `applicationId` and release/store entitlements. Enterprise-only features are isolated. No business APK is published until it has substantive distinct value.

```text
AI-DeviceCare/               # single repository AND project root
  AGENTS.md                   # only policy authority
  settings.gradle.kts         # to be implemented in P0 step 2
  apps/
    devicecare/               # future consumer Android module
    devicecare-plus/          # future business Android module
  core/                       # shared infrastructure and UI modules
  features/                   # Android feature modules
  .agents/skills/             # skill definitions and registry
  governance/                 # declarative checks; non-authorizing policy data
  scripts/                    # policy validation and tests
  .github/workflows/          # GitHub automated verification
  docs/                       # architecture, capability matrix and evidence
```

## Policy enforcement and limits
- `governance/project-policy.json` is a machine-readable encoding of the authorized `AGENTS.md`, not a second authority.
- `scripts/validate-governance.mjs` checks root authority, unique skills/frontmatter, exactly two proposed app paths, no nested Gradle settings or competing `AGENTS.md` and M0-denied manifest permissions.
- `scripts/validate-governance.test.mjs` exercises valid and negative cases with isolated fixtures.
- `.github/workflows/governance-validation.yml` runs read-only validation on PRs, with no secrets or external business connectors. It is **not configured as a Required Check** or a substitute for later Gradle/Android security CI.
- This P0 governance validator neither builds an Android APK nor asserts Google Play acceptance.
- When permission-intensive modules are implemented, their scoped Android/Play policy update, consent, threat model and tests must be proposed in the same implementation PR.

## Evidence status
Source files and skill descriptions reviewed against Capital-AI current `main` at time of adoption. AI-DeviceCare production features and Gradle build remain `NOT_PROVEN` until implemented and executed. GitHub Actions status must be read back separately; authored workflow source does not establish a successful run.
