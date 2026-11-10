# Google Play policy matrix — AI-DeviceCare (P0)

**Status:** Design review only. Not legal advice. Play approval not guaranteed.

## Restricted / sensitive permissions
| Permission / feature | Intended use | Justification required | Status |
|----------------------|--------------|------------------------|--------|
| QUERY_ALL_PACKAGES | Antivirus core only (if approved) | Declaration + scoped alternative preferred | Not used in P0 |
| MANAGE_EXTERNAL_STORAGE | Storage management / cleaner | Only if core function; user consent | Not used in P0 |
| AccessibilityService | Never for automation / settings bypass | Explicitly prohibited | Forbidden |
| VPN / VpnService | DNS filtering / optional remote VPN | Single coordinated service | Future (M3) |
| UsageStatsManager | Optional battery / usage hints | User must enable special access | Future |

## Principles
- Prefer least privilege and scoped queries.
- Every new dangerous/special permission needs dated declaration, consent UX and threat-model update.
- No Accessibility abuse, no silent system changes.

Cross-reference: `docs/ANDROID_CAPABILITY_MATRIX.md` and `AGENTS.md`.
