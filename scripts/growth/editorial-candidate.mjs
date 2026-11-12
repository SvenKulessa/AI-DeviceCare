/**
 * AI-DeviceCare source-bound editorial DRAFT ONLY.
 * No external workers, network, social tokens, device data or publishing adapter.
 */
import {createHash} from 'node:crypto';
import {readFile, mkdir, writeFile} from 'node:fs/promises';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

export const REPO = 'SvenKulessa/AI-DeviceCare';
export const REPO_ID = 1440512580;
export const OWNER_ID = 84307769;
export const CONTRACT = 'DEVICECARE_EDITORIAL_DRAFT@1';
const shaPattern = /^[0-9a-f]{40}$/;
const titleClean = s => String(s ?? '')
  .replace(/[\u0000-\u001f\u007f-\u009f\u202a-\u202e<>#*_[\](){}|\\~]/g, ' ')
  .replace(/\s+/g, ' ').trim().slice(0, 135);
const fingerprint = obj => createHash('sha256').update(JSON.stringify(obj)).digest('hex');

export function buildEditorialCandidate(event) {
  const p = event?.pull_request;
  if (event?.action !== 'closed' || p?.merged !== true ||
      event.repository?.id !== REPO_ID || event.repository?.full_name !== REPO ||
      event.repository?.owner?.id !== OWNER_ID ||
      p.base?.ref !== 'main' || p.base?.repo?.id !== REPO_ID ||
      p.base?.repo?.full_name !== REPO ||
      !Number.isSafeInteger(p.number) || p.number < 1 ||
      !shaPattern.test(p.merge_commit_sha ?? '')) {
    throw new Error('EDITORIAL_MERGE_EVENT_NOT_VERIFIED');
  }
  const pr = p.number, sha = p.merge_commit_sha;
  const title = titleClean(p.title) || 'Technische Repository-Änderung';
  const sourceUrl = 'https://github.com/' + REPO + '/pull/' + pr;
  const commitUrl = 'https://github.com/' + REPO + '/commit/' + sha;
  const id = 'pr-' + pr + '-' + sha.slice(0, 12);
  const cleanTitle = title.slice(0, 135);
  return Object.freeze({
    contract: CONTRACT, id, state: 'DRAFT_ONLY',
    source: Object.freeze({
      type: 'AUTHENTICATED_GITHUB_MERGE_EVENT', repository: REPO, repoId: REPO_ID,
      pr, mergeSha: sha, sourceUrls: [sourceUrl, commitUrl],
    }),
    fingerprint: fingerprint({repoId: REPO_ID, pr, mergeSha: sha}),
    title: cleanTitle,
    quality: {
      githubEvent: 'VALIDATED_STRUCTURE_ONLY',
      ciReadback: 'NOT_PROVEN', releaseEvidence: 'NOT_PROVEN',
      legalAndMediaRights: 'REVIEW_REQUIRED',
      editorialFactCheck: 'REVIEW_REQUIRED',
      accessibilityReview: 'REVIEW_REQUIRED',
    },
    deviceScoring: 'NOT_ASSESSABLE',
    publication: Object.freeze({
      approved: false, publishAllowed: false, publicationAuthority: 'NONE',
      deliveries: [], providerCalls: 0, externalSpendAllowed: false,
    }),
    branding: {product: 'AI-DeviceCare', capitalAiBrandRights: 'REVIEW_REQUIRED', mediaAsset: null},
    drafts: [{
      channel: 'FIRST_PARTY_CHANGELOG', language: 'de-DE',
      heading: 'Entwurf: AI-DeviceCare Entwicklungsstand',
      text: 'Im GitHub-Repository wurde PR #' + pr + ' zusammengeführt: ' +
        cleanTitle + '. Das bestätigt eine Codeänderung, weder die Veröffentlichung ' +
        'im Google Play Store noch eine geprüfte Schutzwirkung auf einem realen Gerät.',
      sourceUrl, approval: 'REVIEW_REQUIRED',
    }, {
      channel: 'EDITORIAL_PITCH', language: 'de-DE',
      heading: 'Unverbindlicher Themenvorschlag – mobile Sicherheit und evidenzbasierte QA',
      text: 'Möglicher Fachbeitrag über reproduzierbare Android-Builds, ' +
        'Berechtigungsgrenzen und transparente Testnachweise. ' +
        'Kein Pressebericht und keine redaktionelle Annahme bestätigt.',
      sourceUrl, approval: 'REVIEW_REQUIRED',
    }],
  });
}

function renderPreview(candidate) {
  return [
    '# DRAFT ONLY – AI-DeviceCare redaktionelle Arbeitsfassung', '',
    'Nicht öffentlich teilen ohne Quellen-, Rechte-, Fakten- und redaktionelle Prüfung.', '',
    'Quelle: ' + candidate.source.sourceUrls[0],
    'Merge-SHA: ' + candidate.source.mergeSha,
    'CI/Runtime: NOT_PROVEN; Veröffentlichung: NICHT FREIGEGEBEN.', '',
    ...candidate.drafts.flatMap(draft => ['## ' + draft.heading, '', draft.text, '']),
  ].join('\n');
}

async function main() {
  if (process.env.GITHUB_REPOSITORY !== REPO || !process.env.GITHUB_EVENT_PATH) {
    throw new Error('EDITORIAL_RUNTIME_NOT_AUTHORIZED');
  }
  const event = JSON.parse(await readFile(process.env.GITHUB_EVENT_PATH, 'utf8'));
  const candidate = buildEditorialCandidate(event);
  const target = 'artifacts/editorial-drafts/' + candidate.id;
  await mkdir(dirname(target), {recursive: true});
  await writeFile(target + '.json', JSON.stringify(candidate, null, 2) + '\n',
    {flag: 'wx', mode: 0o600});
  await writeFile(target + '.md', renderPreview(candidate), {flag: 'wx', mode: 0o600});
  process.stdout.write('EDITORIAL_DRAFT_CREATED state=DRAFT_ONLY pr=' +
    candidate.source.pr + ' external_calls=0\n');
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(() => {
    process.stderr.write('EDITORIAL_DRAFT_FAILED_CLOSED\n');
    process.exitCode = 1;
  });
}
