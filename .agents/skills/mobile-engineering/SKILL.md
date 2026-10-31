---
name: mobile-engineering
description: "Android API, min/target SDK, Permissions, Play Signing and release"
---

# mobile-engineering — AI-DeviceCare Fachskill

**Fachlicher Bereich:** MOBILE. **Autorität:** ausschließlich Repository-Root `AGENTS.md`.
**Adaptierte Vorlage:** Capital-AI `.agents/skills/mobile-engineering/SKILL.md` (keine Übernahme der Capital-AI-spezifischen Finanz- oder Deploymentrechte).

## Verfahren
1. Android-Public-APIs und OEM-/API-Level-Unterschiede pro Fähigkeit nachweisen.
2. Lifecycle, Berechtigungen, App-Signing, Upgrade/Recovery und Play-Store-Angaben prüfen.
3. Reproduzierbare Gradle-/Geräte- und Rollback-Evidence liefern.

## Eingaben und Nachweise
Anforderung, betroffene Android-Module, aktueller Commit, Geräte-/API-Version und tatsächliche Tool-Rechte. Ausgabe: reproduzierbare Schritte, positive/negative Tests, Lizenz-/Permission-Evidence und Status `VERIFIED`, `NOT_PROVEN`, `BLOCKED` oder `REVIEW_REQUIRED`.

## Sicherheitsgrenze
Keine Root-/ADB-Umgehung; keine unbewiesenen Android-Gerätefähigkeiten.
Skill-Dateien routen Beratung und Engineering-Wissen; sie sind **keine Tools, keine Berechtigungen und keine zusätzlichen Required Checks oder Approval-Gates**. Sie gelten nur im bestehenden Projekt und Arbeitsauftrag.
