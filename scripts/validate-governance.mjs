#!/usr/bin/env node
// AI-DeviceCare governance-as-code validator. Read-only, no third-party dependencies.
import { readFileSync, readdirSync, existsSync, lstatSync } from 'node:fs';
import { resolve, join, relative, basename, sep, isAbsolute } from 'node:path';
import { fileURLToPath } from 'node:url';

const defaultRoot = fileURLToPath(new URL('../', import.meta.url));

function safeRelative(value) {
  return typeof value === 'string' && value.length > 0 && !isAbsolute(value)
    && value !== '..' && !value.startsWith('../') && !value.includes('\\')
    && value.split('/').every(part => part !== '..' && part !== '');
}
function readJson(root, path, errors) {
  try { return JSON.parse(readFileSync(join(root, path), 'utf8')); }
  catch (e) { errors.push(`cannot read ${path}: ${e.message}`); return null; }
}
function pathsBelow(root) {
  const paths = [], ignored = new Set(['.git', '.gradle', 'node_modules', 'build', '.idea']);
  function walk(folder) {
    for (const dirent of readdirSync(folder, { withFileTypes: true })) {
      if (dirent.isSymbolicLink()) continue;
      const abs = join(folder, dirent.name);
      if (dirent.isDirectory()) {
        if (!ignored.has(dirent.name)) walk(abs);
      } else if (dirent.isFile()) paths.push(relative(root, abs).split(sep).join('/'));
    }
  }
  walk(root); return paths;
}
function validateGovernance(root = defaultRoot) {
  const errors = [];
  root = resolve(root);
  const policy = readJson(root, 'governance/project-policy.json', errors);
  const registry = readJson(root, '.agents/skills/registry.json', errors);
  if (!policy || !registry) return { ok: false, errors };
  if (policy.schemaVersion !== 'AI_DEVICECARE_GOVERNANCE@1' || policy.authority !== 'AGENTS.md'
      || policy.structure?.projectRoot !== '.' || policy.structure?.gradleSettings !== 'settings.gradle.kts') {
    errors.push('invalid governance schema, root or authority');
  }
  if (!existsSync(join(root,'AGENTS.md'))) errors.push('root AGENTS.md missing');
  if (registry.schemaVersion !== 'AI_DEVICECARE_SKILLS@1' || registry.authority !== 'AGENTS.md'
      || registry.projectRoot !== '.') errors.push('invalid skill registry schema or authority');
  const apps = Object.values(policy.structure?.appModules ?? {});
  if (apps.length !== 2 || new Set(apps).size !== 2
      || !apps.every(p => safeRelative(p) && p.startsWith('apps/'))) {
    errors.push('expected exactly two distinct app modules under apps/');
  }
  const shared = policy.structure?.sharedModuleRoots;
  if (!Array.isArray(shared) || shared.length !== 2
      || !shared.every(p => safeRelative(p) && ['core','features'].includes(p))) errors.push('invalid shared module roots');
  const records = registry.skills;
  if (!Array.isArray(records) || records.length < 1) errors.push('skill records missing');
  else {
    const ids = records.map(x => x.id), declared = registry.skillIds;
    if (new Set(ids).size !== ids.length || JSON.stringify(ids) !== JSON.stringify(declared))
      errors.push('duplicate or out-of-sync skill IDs');
    for (const rec of records) {
      if (typeof rec.id !== 'string' || !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(rec.id)
          || rec.path !== `.agents/skills/${rec.id}/SKILL.md` || !safeRelative(rec.path)) {
        errors.push('unsafe skill record'); continue;
      }
      let text;
      try { text = readFileSync(join(root, rec.path), 'utf8'); }
      catch { errors.push(`missing skill: ${rec.id}`); continue; }
      if (!text.startsWith('---\n') || !text.includes(`\nname: ${rec.id}\n`)
          || !text.includes('\n---\n') || !text.includes('AGENTS.md'))
        errors.push(`invalid skill frontmatter/authority: ${rec.id}`);
    }
  }
  const deny = policy.permissionPolicy?.forbiddenManifestPermissionNames;
  if (!Array.isArray(deny) || deny.length < 6 || deny.some(s => typeof s !== 'string' || !s.startsWith('android.permission.')))
    errors.push('invalid restricted permission policy');
  const scanned = pathsBelow(root);
  for (const path of scanned) {
    if (path !== 'AGENTS.md' && basename(path) === 'AGENTS.md') errors.push(`nested AGENTS.md authority: ${path}`);
    if (path !== 'settings.gradle.kts' && (basename(path) === 'settings.gradle.kts' || basename(path) === 'settings.gradle'))
      errors.push(`nested Gradle settings (multiple projects): ${path}`);
    if (basename(path) === 'AndroidManifest.xml' && Array.isArray(deny)) {
      const manifest = readFileSync(join(root,path),'utf8');
      for (const permission of deny) {
        if (manifest.includes(permission)) errors.push(`restricted permission needs reviewed policy change: ${path}: ${permission}`);
      }
    }
  }
  return { ok: errors.length === 0, errors, scannedFileCount: scanned.length,
    skillCount: records?.length ?? 0, appModuleCount: apps.length };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const result = validateGovernance(process.argv[2] ? resolve(process.argv[2]) : defaultRoot);
  process.stdout.write(JSON.stringify(result, null, 2) + '\n');
  if (!result.ok) process.exitCode = 1;
}
export { validateGovernance };
