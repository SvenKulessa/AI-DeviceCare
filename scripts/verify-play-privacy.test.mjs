import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { validatePrivacy } from "./verify-play-privacy.mjs";

const read = (path) => readFileSync(join(process.cwd(), path), "utf8");
const fixtures = () => ({
  screen: read("core/ui/src/main/kotlin/online/capitalai/devicecare/core/ui/PrivacyPolicyScreen.kt"),
  home: read("core/ui/src/main/kotlin/online/capitalai/devicecare/core/ui/DeviceCareHome.kt"),
  activity: read("apps/devicecare/src/main/kotlin/online/capitalai/devicecare/MainActivity.kt"),
  de: read("docs/PLAY-M1-PRIVACY-DE.md"),
  en: read("docs/PLAY-M1-PRIVACY-EN.md"),
});

test("DE and EN privacy pages match the exact on-device copy and remain accessible", () => {
  assert.deepEqual(validatePrivacy(fixtures()), []);
});
test("negative: changing a section of the policy without updating the app fails closed", () => {
  const f = fixtures();
  f.en = f.en.replace("## 6. Transfers, sharing and recipients", "## 6. Missing");
  assert.ok(validatePrivacy(f).includes("POLICY_DRIFT_EN_network"));
});
test("negative: a missing main-screen privacy entry fails closed", () => {
  const f = fixtures();
  f.home = f.home.replace('testTag("privacy_policy_open")', 'testTag("other")');
  assert.ok(validatePrivacy(f).includes("NO_HOME_POLICY_ENTRY"));
});
test("negative: an incomplete legal contact and unverified public URL block release", () => {
  assert.ok(validatePrivacy({ ...fixtures(), release: true })
    .includes("POLICY_RELEASE_METADATA_INCOMPLETE"));
});
