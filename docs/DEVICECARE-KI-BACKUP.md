# DeviceCare-KI -> AI-DeviceCare: scheduled backup and independent Android tests

Owner: SvenKulessa | Canonical source: `SvenKulessa/DeviceCare-KI` | Backup/test destination: `SvenKulessa/AI-DeviceCare`

## Backup isolation and frequency

The legacy `AI-DeviceCare/main` stays independent with its existing modular Android project, Android test gates and Ruleset 27343655. Never overwrite it or attempt to combine the two incompatible Gradle project roots.

Workflow: `.github/workflows/devicecare-ki-mirror.yml`. Cron `17 */8 * * *` runs at 00:17, 08:17 and 16:17 UTC (GitHub may delay scheduled jobs) and allows manual `workflow_dispatch`. The merge of this workflow into legacy `main` triggers a one-time bootstrap via `push` on the workflow file, so it does not need to wait for the next cron. It pulls the exact current `DeviceCare-KI/main` Git commit SHA and stages a **lossless snapshot of all source files** under a neutral directory `upstream/DeviceCare-KI/` on a separately maintained destination branch:

- `refs/heads/backup/devicecare-ki-main`: latest synthetic snapshot commit, fast-forward updates only.
- `refs/tags/backup/devicecare-ki/<SOURCE_SHA>`: per-source-SHA recovery tag pointing to the synthetic snapshot commit; the script never overwrites a tag.
- `.mirror/SOURCE_SHA`: original upstream commit SHA (do not confuse with backup snapshot commit SHA).
- `.mirror/SOURCE_TREE`: original upstream tree SHA, independently verifiable against the nested source tree.

**Why the prefix?** GitHub's repository-scoped `GITHUB_TOKEN` cannot safely import another repository's active `.github/workflows` as workflow updates without broader permissions. Files remain complete under the inert prefix, including original upstream workflows, and are not interpreted as active destination GitHub Actions.

The script runs no upstream code in the credential-bearing mirror job. Existing source backups are never force-pushed/deleted. If upstream history rewinds/diverges from the previously recorded source SHA, abort without destroying older snapshots. The backup stores **full source snapshots, not upstream Git commit history**; the original source SHA and tree SHA remain recorded. These recovery tags are non-overwritten by this workflow, but are not protected against administrators deleting them unless destination tag rulesets are added separately.

## Independent Android tests

Only on changed source SHA, a separate read-only job checks out that exact **original** source SHA with `persist-credentials: false`, and runs Trivy vulnerability/secret/misconfiguration/license scan, JDK 21, Gradle 9.3.1, Android 36.1, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`. It creates an ephemeral internal **debug** signing key, verifies APK signature, records SHA-256/source provenance and uploads the debug APK and evidence for 14 days. No release key, cloud provider secret, Google Play publishing or automatic product release.

A mirror success does **not** imply a test success. If a test fails, backup snapshots persist unchanged and test failure remains visible.

## Restore a snapshot locally (never writes legacy main)

```bash
git clone https://github.com/SvenKulessa/AI-DeviceCare.git
cd AI-DeviceCare
git fetch origin refs/heads/backup/devicecare-ki-main
git switch -c review-devicecare-backup FETCH_HEAD

# Exact upstream SHA and tree SHA recorded in the snapshot
cat .mirror/SOURCE_SHA
cat .mirror/SOURCE_TREE
git rev-parse HEAD:upstream/DeviceCare-KI
# The last two tree SHAs must match.

# Restore all upstream files without mixing the two project roots
mkdir -p ../DeviceCare-KI-restored
git archive --format=tar HEAD:upstream/DeviceCare-KI | tar -x -C ../DeviceCare-KI-restored
```

For a historical snapshot: `git fetch origin 'refs/tags/backup/devicecare-ki/<FULL_SOURCE_SHA>'`, then inspect `FETCH_HEAD` in a new review branch. The per-SHA tags point to the backup snapshot commits, *not directly to upstream commits*.

## Validation and state

When the workflow is merged into the destination default branch, inspect the automatically triggered initial `push` run; if the trigger does not execute, run `workflow_dispatch` manually. Record source SHA, matching nested tree SHA, backup branch SHA, tag, green/failed test results and APK evidence. Until the first real run, status is **NOT_PROVEN**; committing the workflow alone is not evidence that 8-hour synchronization executes.

Code PR merges remain subject to their own repository rulesets and CI. Backup synchronization is never a merge into legacy `main` or a Google Play release.
