# Product Requirements — AI-DeviceCare (P0 baseline)

**Status:** Planning / foundation only. No production claims.

Source of truth: [Roadmap Issue #1](https://github.com/SvenKulessa/AI-DeviceCare/issues/1) and root `AGENTS.md`.

## Product goals
- Privacy-first Android device health, permission analysis, battery insights and guided remediation.
- Two differentiated products from one modular codebase:
  - AI-DeviceCare (consumer)
  - AI-DeviceCare+ (business / organizational)
- No root, no ADB, public Android APIs only.
- Evidence before any security verdict. No false guarantees.

## Non-negotiable constraints
- Consent before any sensitive or destructive action.
- On-device first. No silent data upload.
- Battery-aware design. Diagnostics must not become a drain source.
- Clear unsupported / unknown states. Never invent healthy/secure scores from missing data.

## Milestone alignment
See README and Issue #1 for P0–M5 sequencing. This document tracks requirements; implementation status is tracked in issues and PRs.

## Open
Refine with real-device QA evidence (Issue #5, #7) and capability matrix.
