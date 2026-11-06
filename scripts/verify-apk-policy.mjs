#!/usr/bin/env node
/**
 * P0.3 M1 APK attestation. Inspect the INSTALLED/PACKAGED manifest, not source
 * XML: transitive libraries may merge extra components and permissions.
 * Requires SDK cmdline-tools apkanalyzer and build-tools aapt2 in CI.
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = fileURLToPath(new URL('../', import.meta.url));
const apps = [
  { path: 'apps/devicecare/build/outputs/apk/debug/devicecare-debug.apk', packageName: 'online.capitalai.devicecare' },
  { path: 'apps/devicecare-plus/build/outputs/apk/debug/devicecare-plus-debug.apk', packageName: 'online.capitalai.devicecare.plus' },
];

/** Pure validation: test fixtures cover forbidden merged permissions/components. */
export function validateManifest(manifest, packageName, reportedPermissions = '') {
  const errors = [];
  if (typeof manifest !== 'string' || !manifest.includes('<manifest')) return ['UNREADABLE_MERGED_MANIFEST'];
  if (!/^[a-z][a-z0-9_.]+$/.test(packageName)) errors.push('INVALID_APPLICATION_ID');
  // AndroidX Core may generate a package-local SIGNATURE permission used to
  // guard non-exported dynamic receivers. It is NOT a user-granted permission.
  // All platform/dangerous/network/usage/VPN permissions remain forbidden.
  const marker = packageName + '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION';
  const nameOf = tag => tag.match(/android:name\\s*=\\s*["']([^"']+)["']/)?.[1] ?? null;
  const exportedOf = tag => tag.match(/android:exported\\s*=\\s*["']([^"']+)["']/)?.[1] ?? null;
  const permTags = [...manifest.matchAll(/<uses-permission(?:-sdk-[0-9]+)?\\b[^>]*>/g)].map(m => m[0]);
  if (permTags.some(t => nameOf(t) !== marker)) errors.push('APK_HAS_PERMISSIONS');
  const declared = [...manifest.matchAll(/<permission(?:-group|-tree)?\\b[^>]*>/g)].map(m => m[0]);
  if (declared.some(t => !t.startsWith('<permission ') ||
      nameOf(t) !== marker ||
      !/android:protectionLevel\\s*=\\s*["']signature["']/.test(t)) ||
      declared.filter(t => nameOf(t) === marker).length > 1)
    errors.push('APK_DECLARES_PERMISSION');
  const packaged = reportedPermissions.trim().split(/\\r?\\n/).map(x => x.trim());
  for (const line of packaged) {
    if (line.startsWith('uses-permission:')) {
      if (line.match(/name=['"]([^'"]+)['"]/)?.[1] !== marker) errors.push('APK_HAS_PERMISSIONS');
    } else if (line.startsWith('permission:')) {
      if (line.slice('permission:'.length).trim() !== marker) errors.push('APK_DECLARES_PERMISSION');
    }
  }
  const recv = [...manifest.matchAll(/<receiver\\b[^>]*>/g)].map(m => m[0]);
  if (recv.some(t =>
      nameOf(t) !== 'androidx.profileinstaller.ProfileInstallReceiver' ||
      exportedOf(t) !== 'true' ||
      !/android:permission\\s*=\\s*["']android.permission.DUMP["']/.test(t)))
    errors.push('UNEXPECTED_RECEIVER');
  const providers = [...manifest.matchAll(/<provider\\b[^>]*>/g)].map(m => m[0]);
  if (providers.some(t =>
      nameOf(t) !== 'androidx.startup.InitializationProvider' ||
      exportedOf(t) !== 'false' ||
      !t.includes(packageName + '.androidx-startup')))
    errors.push('UNEXPECTED_PROVIDER');
  for (const type of ['service', 'activity-alias', 'instrumentation']) {
    if (new RegExp('<' + type + '\\\\b').test(manifest))
      errors.push('UNEXPECTED_' + type.toUpperCase().replace('-', '_'));
  }
  const applications = [...manifest.matchAll(/<application\\b[^>]*>/g)];
  if (applications.length !== 1) errors.push('APPLICATION_COUNT_INVALID');
  else {
    if (!/android:allowBackup\\s*=\\s*["']false["']/.test(applications[0][0])) errors.push('BACKUP_MUST_BE_DISABLED');
    if (/android:usesCleartextTraffic\\s*=\\s*["']true["']/.test(applications[0][0])) errors.push('CLEARTEXT_TRAFFIC_ALLOWED');
  }
  const activities = [...manifest.matchAll(/<activity\\b[^>]*>/g)];
  if (activities.length !== 1) errors.push('ACTIVITY_COUNT_INVALID');
  else {
    const tag = activities[0][0];
    if (!/android:name\\s*=\\s*["'][^"']*MainActivity["']/.test(tag)) errors.push('LAUNCHER_ACTIVITY_INVALID');
    if (exportedOf(tag) !== 'true') errors.push('LAUNCHER_EXPORT_REQUIRED');
  }
  if (!manifest.includes('android.intent.action.MAIN') || !manifest.includes('android.intent.category.LAUNCHER'))
    errors.push('LAUNCHER_INTENT_MISSING');
  if (manifest.includes('android.permission.INTERNET') || manifest.includes('android.permission.QUERY_ALL_PACKAGES'))
    errors.push('NETWORK_OR_PACKAGE_VISIBILITY_PERMISSION');
  return errors;
}

function checkedOutput(binary, args) {
  if (!binary) throw Error('SDK_TOOL_NOT_AVAILABLE');
  const result = spawnSync(binary, args, { encoding: 'utf8', timeout: 45_000, maxBuffer: 3_000_000 });
  if (result.error || result.status !== 0) throw Error('SDK_TOOL_FAILED: ' + args.slice(0, 2).join(' '));
  return result.stdout;
}

export function verifyApk(apkPath, expectedPackage, tools) {
  const actualPackage = checkedOutput(tools.apkanalyzer, ['manifest', 'application-id', apkPath]).trim();
  const xml = checkedOutput(tools.apkanalyzer, ['manifest', 'print', apkPath]);
  const permissionDump = checkedOutput(tools.aapt2, ['dump', 'permissions', apkPath]);
  const errors = validateManifest(xml, actualPackage, permissionDump);
  if (actualPackage !== expectedPackage) errors.push('PACKAGE_ID_MISMATCH');
  const targetSdk = Number(checkedOutput(tools.apkanalyzer, ['manifest', 'target-sdk', apkPath]).trim());
  if (!Number.isSafeInteger(targetSdk) || targetSdk < 35) errors.push('TARGET_SDK_TOO_OLD');
  const hash = createHash('sha256').update(readFileSync(apkPath)).digest('hex');
  return {
    applicationId: actualPackage, targetSdk, sha256: hash,
    permissions: permissionDump.trim(), // empty expected; never includes private device content
    status: errors.length ? 'FAIL' : 'PASS', failures: errors,
  };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
    if (!sdk) throw Error('ANDROID_SDK_NOT_CONFIGURED');
    const tools = {
      apkanalyzer: join(sdk, 'cmdline-tools/latest/bin/apkanalyzer'),
      aapt2: join(sdk, 'build-tools/36.0.0/aapt2'),
    };
    const results = apps.map(app => verifyApk(join(projectRoot, app.path), app.packageName, tools));
    const report = {
      schemaVersion: 'devicecare-apk-policy/1', sourceSha: process.env.GITHUB_SHA || 'LOCAL_UNBOUND',
      artifactScope: 'M1_INTERNAL_DEBUG_ONLY', results,
    };
    const out = join(projectRoot, 'evidence/apk-manifest-policy.json');
    mkdirSync(dirname(out), { recursive: true });
    writeFileSync(out, JSON.stringify(report, null, 2) + '\n');
    process.stdout.write(JSON.stringify(report, null, 2) + '\n');
    if (results.some(r => r.status !== 'PASS')) process.exitCode = 1;
  } catch (error) {
    process.stderr.write('APK_MANIFEST_NOT_PROVEN: ' + error.message + '\n');
    process.exitCode = 1;
  }
}
