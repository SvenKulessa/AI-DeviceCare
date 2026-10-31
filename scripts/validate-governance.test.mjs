import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, copyFileSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateGovernance } from './validate-governance.mjs';

const repoRoot = fileURLToPath(new URL('../', import.meta.url));
const registry = JSON.parse(readFileSync(join(repoRoot, '.agents/skills/registry.json'), 'utf8'));
function createFixture() {
  const root = mkdtempSync(join(tmpdir(), 'devicecare-policy-'));
  for (const path of ['AGENTS.md', 'governance/project-policy.json', '.agents/skills/registry.json',
                        ...registry.skills.map(x => x.path)]) {
    mkdirSync(dirname(join(root, path)), { recursive: true });
    copyFileSync(join(repoRoot, path), join(root, path));
  }
  return root;
}
function withFixture(run) {
  const root = createFixture();
  try { run(root); } finally { rmSync(root, { recursive: true, force: true }); }
}

test('P0 governance is valid with one project root and all registered skills', () =>
  withFixture(root => {
    const result = validateGovernance(root);
    assert.equal(result.ok, true, result.errors.join('; '));
    assert.equal(result.appModuleCount, 2);
    assert.equal(result.skillCount, registry.skills.length);
  }));

test('nested Gradle settings are rejected to enforce a single project', () =>
  withFixture(root => {
    mkdirSync(join(root,'apps/devicecare'), { recursive: true });
    writeFileSync(join(root,'apps/devicecare/settings.gradle.kts'), '// invalid nested project');
    assert.match(validateGovernance(root).errors.join(' '), /nested Gradle settings/);
  }));

test('a second AGENTS.md cannot create competing governance authority', () =>
  withFixture(root => {
    mkdirSync(join(root,'features/battery'), { recursive: true });
    writeFileSync(join(root,'features/battery/AGENTS.md'), '# invalid authority');
    assert.match(validateGovernance(root).errors.join(' '), /nested AGENTS.md/);
  }));

test('restricted Android permissions are blocked by the M0 baseline', () =>
  withFixture(root => {
    mkdirSync(join(root,'apps/devicecare/src/main'), { recursive: true });
    writeFileSync(join(root,'apps/devicecare/src/main/AndroidManifest.xml'),
      '<manifest><uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/></manifest>');
    assert.match(validateGovernance(root).errors.join(' '), /QUERY_ALL_PACKAGES/);
  }));

test('missing skill is a registry failure', () =>
  withFixture(root => {
    rmSync(join(root, registry.skills[0].path));
    assert.match(validateGovernance(root).errors.join(' '), /missing skill:/);
  }));

test('tampering with schema/authority fails closed', () =>
  withFixture(root => {
    const file=join(root,'governance/project-policy.json');
    const policy=JSON.parse(readFileSync(file,'utf8'));
    policy.authority='another-policy.md';
    writeFileSync(file, JSON.stringify(policy));
    assert.match(validateGovernance(root).errors.join(' '), /invalid governance schema/);
  }));
