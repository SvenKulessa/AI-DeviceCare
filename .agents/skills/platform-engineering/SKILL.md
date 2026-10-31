---
name: platform-engineering
description: "Gradle mono-project, GitHub CI, reproducible builds and supply chain"
---

# platform-engineering — AI-DeviceCare Fachskill

**Fachlicher Bereich:** PLATFORM. **Autorität:** ausschließlich Repository-Root `AGENTS.md`.
**Adaptierte Vorlage:** Capital-AI `.agents/skills/platform-engineering/SKILL.md` (keine Übernahme der Capital-AI-spezifischen Finanz- oder Deploymentrechte).

## Verfahren
1. Ein Root-Gradle-Multiprojekt für zwei Apps und gemeinsame Module erhalten.
2. Toolchain-Versionen, SHA-Pins, Build- und Dependency-Evidence prüfen.
3. CI, Artefakt-Digest, sichere Signierung, Rollback und Release-Zustände dokumentieren.

## Eingaben und Nachweise
Anforderung, betroffene Android-Module, aktueller Commit, Geräte-/API-Version und tatsächliche Tool-Rechte. Ausgabe: reproduzierbare Schritte, positive/negative Tests, Lizenz-/Permission-Evidence und Status `VERIFIED`, `NOT_PROVEN`, `BLOCKED` oder `REVIEW_REQUIRED`.

## Sicherheitsgrenze
Keine Secrets im Repository; keine Direktänderung an main oder Store ohne Autorisierung.
Skill-Dateien routen Beratung und Engineering-Wissen; sie sind **keine Tools, keine Berechtigungen und keine zusätzlichen Required Checks oder Approval-Gates**. Sie gelten nur im bestehenden Projekt und Arbeitsauftrag.
