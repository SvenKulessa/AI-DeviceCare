---
name: trust-engineering
description: "AppSec, threat model, Play policy, privacy and dependency rights"
---

# trust-engineering — AI-DeviceCare Fachskill

**Fachlicher Bereich:** TRUST. **Autorität:** ausschließlich Repository-Root `AGENTS.md`.
**Adaptierte Vorlage:** Capital-AI `.agents/skills/trust-engineering/SKILL.md` (keine Übernahme der Capital-AI-spezifischen Finanz- oder Deploymentrechte).

## Verfahren
1. Trust Boundaries, PII, Berechtigungen, SDKs, App-Signing und Threat Model nachweisen.
2. Dependency-/Lizenz-/SBOM- und Datenquellenrechte gesondert prüfen.
3. Security Evidence, Fehlalarme und unverifizierte Scan-Claims explizit trennen.
4. **Execution-first bei autorisierten Implementierungs- und Reparaturaufträgen:** Ursache reproduzieren, den kleinsten sicheren reversiblen Fix auf einem Feature-Branch selbst implementieren, betroffene Positiv-/Negativtests und bestehende CI ausführen, reproduzierte Fehler korrigieren und den PR mit Evidence und Rollback bis zur regelkonformen Mergeentscheidung abschließen; kein unnötiger Analyse-Handoff. Ausdrückliche Read-only-, No-Merge- und No-Release-Aufträge bleiben verbindlich. CAPITAL-AI-Merge-/Deploymentdelegation wird nicht übernommen; Android-Sicherheit (kein Root/ADB), Permissions, Datenschutz, Lizenzen, Play-Policies und persönliche Owner-Releasefreigabe bleiben unberührt.

## Eingaben und Nachweise
Anforderung, betroffene Android-Module, aktueller Commit, Geräte-/API-Version und tatsächliche Tool-Rechte. Ausgabe: reproduzierbare Schritte, positive/negative Tests, Lizenz-/Permission-Evidence und Status `VERIFIED`, `NOT_PROVEN`, `BLOCKED` oder `REVIEW_REQUIRED`.

## Sicherheitsgrenze
Kein unbelegter Spyware-/Malware-Fund; kein Permission- oder Datenzugriff ohne Freigabe.
Skill-Dateien routen Beratung und Engineering-Wissen; sie sind **keine Tools, keine Berechtigungen und keine zusätzlichen Required Checks oder Approval-Gates**. Sie gelten nur im bestehenden Projekt und Arbeitsauftrag.
