# License audit — AI-DeviceCare (P0)

**Status:** Process defined. Full scan required before dependencies are added.

## Policy
- Scan direct and transitive dependencies, code and any signature/rule packs for commercial redistribution rights.
- GPL/copyleft integration requires explicit design and legal review.
- Prefer permissive licenses (Apache-2.0, MIT, BSD) for core libraries.
- Document license of every new dependency in PRs.

## Current
No third-party libraries beyond Android/Gradle/Compose baseline have been fully audited yet. Run license check as part of CI once the dependency graph stabilizes.

Cross-reference: `AGENTS.md` licensing gate and governance validation scripts.
