# Release bundle build validation

Roadmap: #1. This build fix does not authorize signing, Play upload or publication.

The manually dispatched bundle workflow uses the same pinned JDK 17 and Gradle
9.6.0 setup as Android P0, and installs only platform 36 / build-tools 36.0.0.
There is no Gradle wrapper in this repository. Version values are passed through
environment variables, validated by Gradle before signing secrets are accessed,
and applied to the consumer app. The business app keeps its separate version.

`versionCode` must be an integer in 1..2100000000. `versionName` must have
major.minor.patch form, optionally followed by a `-` or `+` suffix. Without these
properties the preview defaults remain code 1 / name `0.1.0-m1-preview`.
The operator must still check that the code exceeds the latest uploaded Play
version; a local build cannot establish the state of the Play Console.

## Local verification without production credentials

```sh
gradle --no-daemon :apps:devicecare:tasks -PversionCode=0
# Must fail with the versionCode validation message.
gradle --no-daemon :apps:devicecare:tasks '-PversionName=bad name'
# Must fail with the versionName validation message.
gradle --no-daemon :apps:devicecare:assembleDebug :apps:devicecare:bundleRelease \
  :apps:devicecare:lintDebug -PversionName=1.2.3-qa -PversionCode=42
apkanalyzer manifest version-code apps/devicecare/build/outputs/apk/debug/devicecare-debug.apk
apkanalyzer manifest version-name apps/devicecare/build/outputs/apk/debug/devicecare-debug.apk
# Must report 42 and 1.2.3-qa. The release bundle is unsigned without signing properties.
```

The workflow keeps the keystore in the runner temporary directory with restrictive
permissions and removes it in an always-run step. Only the manual signed workflow
uses repository signing secrets; no secret or signing material is used by these
local checks. Signed-bundle validation and distribution remain separate gates.

Rollback: revert the release workflow, consumer Gradle properties and this document
as one focused change. No database, runtime permission or customer-data migration.
