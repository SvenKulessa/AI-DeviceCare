---
name: revenue-engineering
description: "Play Billing, tier entitlements, pricing economics and release accuracy"
---

# revenue-engineering — AI-DeviceCare Fachskill

**Fachlicher Bereich:** GROWTH. **Autorität:** ausschließlich Repository-Root `AGENTS.md`.
**Adaptierte Vorlage:** Capital-AI `.agents/skills/revenue-engineering/SKILL.md` (keine Übernahme der Capital-AI-spezifischen Finanz- oder Deploymentrechte).

## Verfahren
1. Free/Starter/Pro/Elite und drei Business-Entitlements versionieren.
2. Play Billing, Rückerstattung, Offline-Grenzen und serverseitige Entitlement-Prüfung testen.
3. Store-Texte nur für ausgelieferte, nachweisbare Funktionen erstellen.
4. **Execution-first bei autorisierten Implementierungs- und Reparaturaufträgen:** Ursache reproduzieren, den kleinsten sicheren reversiblen Fix auf einem Feature-Branch selbst implementieren, betroffene Positiv-/Negativtests und bestehende CI ausführen, reproduzierte Fehler korrigieren und den PR mit Evidence und Rollback bis zur regelkonformen Mergeentscheidung abschließen; kein unnötiger Analyse-Handoff. Ausdrückliche Read-only-, No-Merge- und No-Release-Aufträge bleiben verbindlich. CAPITAL-AI-Merge-/Deploymentdelegation wird nicht übernommen; Android-Sicherheit (kein Root/ADB), Permissions, Datenschutz, Lizenzen, Play-Policies und persönliche Owner-Releasefreigabe bleiben unberührt.

## Eingaben und Nachweise
Anforderung, betroffene Android-Module, aktueller Commit, Geräte-/API-Version und tatsächliche Tool-Rechte. Ausgabe: reproduzierbare Schritte, positive/negative Tests, Lizenz-/Permission-Evidence und Status `VERIFIED`, `NOT_PROVEN`, `BLOCKED` oder `REVIEW_REQUIRED`.

## Sicherheitsgrenze
Keine freigeschalteten, nicht gelieferten Leistungen und kein Vertrauen in Client-seitige Kaufbehauptungen.
Skill-Dateien routen Beratung und Engineering-Wissen; sie sind **keine Tools, keine Berechtigungen und keine zusätzlichen Required Checks oder Approval-Gates**. Sie gelten nur im bestehenden Projekt und Arbeitsauftrag.
