import test from 'node:test';
import assert from 'node:assert/strict';
import { validateManifest } from './verify-apk-policy.mjs';

const xml = `<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/>
  <application android:allowBackup="false" android:supportsRtl="true">
    <activity android:name="online.capitalai.devicecare.MainActivity" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>
    </activity>
  </application>
</manifest>`;
const validate = (value = xml, permissions = '') =>
  validateManifest(value, 'online.capitalai.devicecare', permissions);

test('one exported launcher, backup disabled and no merged permissions', () =>
  assert.deepEqual(validate(), []));

test('reject inherited INTERNET permission from transitive library', () => {
  const mutated = xml.replace('<application', '<uses-permission android:name="android.permission.INTERNET"/>\n<application');
  assert.ok(validate(mutated).includes('APK_HAS_PERMISSIONS'));
});
test('reject permission present in aapt2 even if XML printer misses it', () =>
  assert.ok(validate(xml, "uses-permission: name='android.permission.QUERY_ALL_PACKAGES'")
    .includes('APK_HAS_PERMISSIONS')));

test('reject exported transitive service and provider', () => {
  const bad = xml.replace('</application>', '<service android:name=".Service" android:exported="true"/><provider android:name=".Provider"/></application>');
  assert.ok(validate(bad).includes('UNEXPECTED_SERVICE'));
  assert.ok(validate(bad).includes('UNEXPECTED_PROVIDER'));
});
test('reject backup or cleartext regressions', () => {
  assert.ok(validate(xml.replace('android:allowBackup="false"', 'android:allowBackup="true"'))
    .includes('BACKUP_MUST_BE_DISABLED'));
  assert.ok(validate(xml.replace('android:allowBackup="false"', 'android:allowBackup="false" android:usesCleartextTraffic="true"'))
    .includes('CLEARTEXT_TRAFFIC_ALLOWED'));
});
test('reject additional activities and nonexported launcher', () => {
  assert.ok(validate(xml.replace('</application>', '<activity android:name=".Secret"/></application>'))
    .includes('ACTIVITY_COUNT_INVALID'));
  assert.ok(validate(xml.replace('android:exported="true"', 'android:exported="false"'))
    .includes('LAUNCHER_EXPORT_REQUIRED'));
});
test('reject unusable/incomplete output instead of passing', () => {
  assert.deepEqual(validate(''), ['UNREADABLE_MERGED_MANIFEST']);
  assert.ok(validate(xml.replace('android.intent.action.MAIN','not.main'))
    .includes('LAUNCHER_INTENT_MISSING'));
});

test('accept only the AndroidX package-private signature permission and audited components', () => {
  const appId = 'online.capitalai.devicecare';
  const marker = appId + '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION';
  const safe = xml
    .replace('<application',
      '<permission android:name="' + marker + '" android:protectionLevel="signature"/>\\n' +
      '<uses-permission android:name="' + marker + '"/>\\n<application')
    .replace('</application>',
      '<receiver android:name="androidx.profileinstaller.ProfileInstallReceiver" ' +
      'android:exported="true" android:permission="android.permission.DUMP"/>\\n' +
      '<provider android:name="androidx.startup.InitializationProvider" ' +
      'android:exported="false" android:authorities="' + appId + '.androidx-startup"/>\\n' +
      '</application>');
  const aapt = "package: " + appId + "\\npermission: " + marker +
    "\\nuses-permission: name='" + marker + "'";
  assert.deepEqual(validate(safe, aapt), []);
  // Android's compiled binary XML renders signature (protection bit 2) numerically.
  assert.deepEqual(validate(safe.replace('android:protectionLevel="signature"', 'android:protectionLevel="0x00000002"'), aapt), []);
  assert.ok(validate(safe.replace('android:protectionLevel="signature"', 'android:protectionLevel="0x00000000"'), aapt).includes('APK_DECLARES_PERMISSION'));

  const unsafe = safe.replace('android:exported="false" android:authorities=', 'android:exported="true" android:authorities=');
  assert.ok(validate(unsafe, aapt).includes('UNEXPECTED_PROVIDER'));
});
