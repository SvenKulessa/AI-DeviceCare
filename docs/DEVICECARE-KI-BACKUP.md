# DeviceCare-KI -> AI-DeviceCare: backup and isolated test

Owner: SvenKulessa | Source: `SvenKulessa/DeviceCare-KI` | Destination: `SvenKulessa/AI-DeviceCare`.

## Direction and schedule

The destination's default `main` remains a separate legacy Android project, with its existing module layout, Ruleset 27343655 and test workflows. Do **not** combine the unrelated Gradle roots or overwrite `main`.

`.github/workflows/devicecare-ki-mirror.yml` runs at 00:17, 08:17 and 16:17 UTC (GitHub scheduling may be delayed), and via manual `workflow_dispatch` after merging into the destination's default branch. It fetches the source's exact `main` Git commit, never executes upstream code in the write-credential job, and **fast-forwards only** `refs/heads/backup/devicecare-ki-main`. Each changed source SHA is also recorded as an append-only tag `refs/tags/backup/devicecare-ki/<40-hex-source-SHA>`. The workflow never force-pushes, deletes backup refs or modifies the legacy `main`.

If source history is rewritten or diverges, the workflow fails closed. The existing backup ref and snapshots are left intact. A green mirror push is not evidence of a green build.

## Independent test job

After each **changed** upstream SHA, a separate, read-only GitHub-hosted runner checks out that exact upstream SHA, runs Trivy (vulnerability/secret/misconfiguration/license), JDK 21 + Gradle 9.3.1 on Android 36.1, `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug`; verifies the ephemeral **debug-only** APK signature; uploads the APK and SHA-256/cert/evidence for 14 days. No release key, Firebase secret, Play publishing or shared provider credential is used. The source repository is public; this is not a substitute for an offline/private backup.

Note: Android test failures must not delete or roll back a verified source snapshot. The test job exposes failure separately.

## Recovery / proof

```sh
git clone https://github.com/SvenKulessa/AI-DeviceCare.git
cd AI-DeviceCare
git fetch origin refs/heads/backup/devicecare-ki-main
git show --no-patch --format=fuller FETCH_HEAD
git ls-remote --tags origin 'refs/tags/backup/devicecare-ki/*'
# Restore a specific exact source commit into a *new* local recovery branch:
git fetch origin 'refs/tags/backup/devicecare-ki/<FULL_SOURCE_SHA>'
git switch -c recovery/devicecare-ki FETCH_HEAD
```

Use the SHA in the workflow summary, branch ref and tag as proof. Check the isolated test job and artifact before considering the source production-ready. Immutable retention is not guaranteed by tags alone unless destination tag rulesets protect them; this workflow itself never overwrites an existing snapshot.

## Relationship to merge automation

Code PRs are merged only after real per-repo checks, GitHub Ruleset and owner authorization. Synchronization is **not** an automatic merge into the legacy `main` and not a production release. Do not run untrusted upstream scripts in the credential-bearing mirror job.

## Initial verification

Run the workflow manually once after the PR merges, or inspect its first scheduled run. Record the upstream SHA, mirror branch SHA, append-only tag and full Android test outcome. Until that happens: workflow committed != execution VERIFIED.
