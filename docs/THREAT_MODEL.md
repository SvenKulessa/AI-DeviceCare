# Threat model — AI-DeviceCare (P0 baseline)

**Status:** Initial. Update for each sensitive feature (VPN, malware, diagnostics, telemetry, enterprise).

## Assets
- User device integrity and privacy
- Local evidence and diagnostic logs (redacted, deletable)
- App reputation / trust

## Threats considered
- Over-collection of sensitive data (IMEI, package lists, etc.) — mitigated by policy and collectors.
- False-positive security claims — mitigated by evidence contracts and "unknown/unsupported" states.
- Battery drain from own scanning — mitigated by budgets and event-driven design.
- Unauthorized remote actions — no cloud mutation without explicit consent and review.
- Play policy violations (Accessibility, broad permissions) — explicitly forbidden or gated.

## Out of scope (current)
- Kernel-level access, root exploitation, or zero-day completeness claims.

Review and expand when implementing collectors (Issue #5/#7) or network features.
