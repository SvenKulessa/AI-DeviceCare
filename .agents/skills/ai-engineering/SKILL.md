---
name: ai-engineering
description: "Explainable battery/risk recommendations and on-device or opt-in model usage"
---

# ai-engineering — AI-DeviceCare Fachskill

**Fachlicher Bereich:** PRODUCT. **Autorität:** ausschließlich Repository-Root `AGENTS.md`.
**Adaptierte Vorlage:** Capital-AI `.agents/skills/ai-engineering/SKILL.md` (keine Übernahme der Capital-AI-spezifischen Finanz- oder Deploymentrechte).

## Verfahren
1. Deterministische Evidenzbasis, Modellgrenzen und Datenklassen feststellen.
2. Prompt-Injection, Halluzinationen, Fehlklassifikation, Modell- und Datensatzrechte testen.
3. Modelle dürfen Maßnahmen erklären, aber keine privilegierten Aktionen autonom autorisieren.

4. **Execution-first bei autorisierten Implementierungs- und Reparaturaufträgen:** Ursache reproduzieren, den kleinsten sicheren reversiblen Fix auf einem Feature-Branch selbst implementieren, betroffene Positiv-/Negativtests und bestehende CI ausführen, reproduzierte Fehler korrigieren und den PR mit Evidence und Rollback bis zur regelkonformen Mergeentscheidung abschließen; kein unnötiger Analyse-Handoff. Ausdrückliche Read-only-, No-Merge- und No-Release-Aufträge bleiben verbindlich. CAPITAL-AI-Merge-/Deploymentdelegation wird nicht übernommen; Android-Sicherheit (kein Root/ADB), Permissions, Datenschutz, Lizenzen, Play-Policies und persönliche Owner-Releasefreigabe bleiben unberührt.

## Eingaben und Nachweise
Anforderung, betroffene Android-Module, aktueller Commit, Geräte-/API-Version und tatsächliche Tool-Rechte. Ausgabe: reproduzierbare Schritte, positive/negative Tests, Lizenz-/Permission-Evidence und Status `VERIFIED`, `NOT_PROVEN`, `BLOCKED` oder `REVIEW_REQUIRED`.

## Sicherheitsgrenze
Keine ungeprüften Malware- oder Akkuersparnis-Versprechen; Cloud nur per granularer Zustimmung.
Skill-Dateien routen Beratung und Engineering-Wissen; sie sind **keine Tools, keine Berechtigungen und keine zusätzlichen Required Checks oder Approval-Gates**. Sie gelten nur im bestehenden Projekt und Arbeitsauftrag.
