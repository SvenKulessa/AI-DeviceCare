# AI-DeviceCare Skill Registry

Only repository-root [AGENTS.md](../../AGENTS.md) authorizes work. The local registry [registry.json](registry.json) describes eight relevant engineering and advisory skill areas, adapted from [Capital-AI's skill system](https://github.com/SvenKulessa/Capital-AI/tree/main/.agents/skills).

| Skill | Route by topic |
| --- | --- |
| mobile-engineering | Android SDK/Compose/Play release and signing |
| trust-engineering | Threat model, permissions, malware claims, privacy and licenses |
| platform-engineering | Single-root Gradle layout and GitHub Actions |
| platform-observability | Device diagnostics, logging and battery budgets |
| incident-response | Safe fault triage and recovery |
| accessibility-engineering | TalkBack, Compose semantics, device UX |
| ai-engineering | Explainable recommendations and model risks |
| revenue-engineering | Play Billing and paid tier entitlements |

**Routing is advisory, not permission-bearing.** Select by task topic, not chat name. Multiple skills may apply without handoff. A matching skill neither installs a plugin nor grants Android/Play/GitHub access. Do not import Capital-AI's NATS, Render, finance licensing, MARKET role or Docker Required Checks into this Android repository.

## Execution-first statt Analyse-Handoff

Bei ausdrücklich beauftragten Feature- oder Reparaturarbeiten die passenden Skills anwenden, die Ursache reproduzieren, den kleinsten sicheren Fix im autorisierten Feature-Branch implementieren, Positiv-/Negativtests und bestehende CI ausführen, nachgewiesene Fehler eigenständig korrigieren und den Scope-PR mit Commit-, Test-, Rechte- und Rollback-Evidence abschließen. Kopierbare Folgeprompts verlangen konkrete Reparatur und Validierung statt pauschal „keine Änderungen, Mutationen, Merges oder Deployments“ anzuhängen. Ausdrücklich lesende Aufgaben bleiben lesend; No-Merge-/No-Release-Grenzen, Android-Permissions, Play-Policy, Datenschutz und die persönliche Owner-Releasefreigabe aus Root-`AGENTS.md` gelten unverändert. Die delegierte CAPITAL-AI-Merge-/Deploymentregel gilt **nicht** für dieses Repository. Skills schaffen keine neuen Freigaben oder Checks.

Governance-as-Code validator: `node scripts/validate-governance.mjs`. Test suite: `node --test scripts/validate-governance.test.mjs`. They check structure, authority, skill integrity and restricted Android manifest entries; they are not substitutes for real device tests, Play approval, malware detection testing or build verification.
