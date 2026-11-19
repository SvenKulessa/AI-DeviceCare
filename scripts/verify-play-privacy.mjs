import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join, resolve } from "node:path";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const read = (path) => readFileSync(join(root, path), "utf8");

export function validatePrivacy({ screen, home, activity, de, en, release = false }) {
  const errors = [];
  const items = [...screen.matchAll(/PrivacySection\("([^"]+)", "([^"]+)",\s*\n\s*"([^"]*)"\)/g)];
  if (items.length !== 20) errors.push("POLICY_SECTIONS_NOT_20");
  const expectedTags = ["controller", "scope", "access", "security", "storage", "network",
    "permission", "legal", "support", "updates"];
  for (const [idx, doc] of [[0, de], [10, en]]) {
    const slice = items.slice(idx, idx + 10);
    if (slice.length !== 10 || slice.some((m, n) => m[1] !== expectedTags[n])) {
      errors.push(idx ? "EN_SECTION_ORDER" : "DE_SECTION_ORDER");
    }
    for (const match of slice) {
      if (!doc.includes("## " + match[2] + "\n\n" + match[3])) {
        errors.push("POLICY_DRIFT_" + (idx ? "EN" : "DE") + "_" + match[1]);
      }
    }
  }
  if (!home.includes('testTag("privacy_policy_open")') ||
      !home.includes("onClick = onOpenPrivacyPolicy")) {
    errors.push("NO_HOME_POLICY_ENTRY");
  }
  if (!activity.includes("PrivacyPolicyScreen(onBack = { privacyOpen = false })") ||
      !activity.includes("onOpenPrivacyPolicy = { privacyOpen = true }")) {
    errors.push("NO_PRIVACY_NAVIGATION");
  }
  if (!screen.includes('testTag("privacy_policy_screen")') ||
      !screen.includes('testTag("privacy_policy_back")')) {
    errors.push("POLICY_SCREEN_NOT_ACCESSIBLE");
  }
  if (release) {
    const blocking = [/\[VOR FREIGABE EINTRAGEN\]/i, /\[TO BE COMPLETED BEFORE RELEASE\]/i,
      /\bINTERNER ENTWURF\b/i, /\bINTERNAL DRAFT\b/i, /\bNOT_PROVEN\b/i,
      /interner M1-entwurf/i, /not approved for play release/i];
    if (blocking.some((pattern) => pattern.test(screen + de + en))) {
      errors.push("POLICY_RELEASE_METADATA_INCOMPLETE");
    }
    // The published HTTPS URL and operator verification are not yet configured.
    // Never infer a privacy URL from the product's domain.
    const releaseRecord = join(root, "docs", "PLAY-M1-PRIVACY-RELEASE-APPROVAL.json");
    let record;
    try {
      record = JSON.parse(readFileSync(releaseRecord, "utf8"));
    } catch {
      errors.push("PUBLIC_PRIVACY_URL_NOT_APPROVED");
    }
    if (record) {
      const u = String(record.publicUrl || "");
      if (!/^https:\/\/[^/\s]+\.[^/\s]+\/\S+$/i.test(u) ||
          record.ownerApproved !== true ||
          typeof record.reviewedAt !== "string" ||
          !record.reviewedAt) errors.push("PUBLIC_PRIVACY_URL_NOT_APPROVED");
    }
  }
  return [...new Set(errors)];
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const errors = validatePrivacy({
    screen: read("core/ui/src/main/kotlin/online/capitalai/devicecare/core/ui/PrivacyPolicyScreen.kt"),
    home: read("core/ui/src/main/kotlin/online/capitalai/devicecare/core/ui/DeviceCareHome.kt"),
    activity: read("apps/devicecare/src/main/kotlin/online/capitalai/devicecare/MainActivity.kt"),
    de: read("docs/PLAY-M1-PRIVACY-DE.md"),
    en: read("docs/PLAY-M1-PRIVACY-EN.md"),
    release: process.argv.includes("--release"),
  });
  if (errors.length) {
    console.error("PLAY_PRIVACY_GATE_BLOCKED:", errors.join(", "));
    process.exitCode = 1;
  } else console.log("PLAY_PRIVACY_DRAFT_CONSISTENT: VERIFIED (not Play approval)");
}
