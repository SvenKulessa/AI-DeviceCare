import test from 'node:test';
import assert from 'node:assert/strict';
import {REPO, REPO_ID, OWNER_ID, CONTRACT, buildEditorialCandidate} from './editorial-candidate.mjs';

const valid = () => ({
  action: 'closed',
  repository: {id: REPO_ID, full_name: REPO, owner: {id: OWNER_ID}},
  pull_request: {
    merged: true, number: 4,
    title: 'Document Android-15 battery and permission QA',
    merge_commit_sha: 'a'.repeat(40),
    base: {ref: 'main', repo: {id: REPO_ID, full_name: REPO}},
  },
});
test('valid merged event produces only evidence-bound, unapproved drafts', () => {
  const result = buildEditorialCandidate(valid());
  assert.equal(result.contract, CONTRACT);
  assert.equal(result.state, 'DRAFT_ONLY');
  assert.equal(result.source.sourceUrls[0], 'https://github.com/' + REPO + '/pull/4');
  assert.equal(result.source.mergeSha, 'a'.repeat(40));
  assert.equal(result.publication.approved, false);
  assert.equal(result.publication.publishAllowed, false);
  assert.equal(result.publication.providerCalls, 0);
  assert.equal(result.publication.externalSpendAllowed, false);
  assert.equal(result.deviceScoring, 'NOT_ASSESSABLE');
  assert.equal(result.quality.ciReadback, 'NOT_PROVEN');
  assert.ok(result.drafts.every(d => d.approval === 'REVIEW_REQUIRED'));
});
test('unmerged, cross-repository and wrong-base events fail closed', () => {
  for (const modify of [
    e => {e.pull_request.merged=false;},
    e => {e.action='opened';},
    e => {e.pull_request.base.ref='staging';},
    e => {e.repository.id=123;},
    e => {e.repository.owner.id=123;},
    e => {e.repository.full_name='attacker/fork';},
    e => {e.pull_request.base.repo.full_name='attacker/fork';},
    e => {e.pull_request.number=-1;},
    e => {e.pull_request.merge_commit_sha='bad-sha';},
  ]) {
    const event = valid();
    modify(event);
    assert.throws(() => buildEditorialCandidate(event), /EDITORIAL_MERGE_EVENT_NOT_VERIFIED/);
  }
});
test('PR title cannot inject HTML or markup into static drafts', () => {
  const event = valid();
  event.pull_request.title = '<script>alert(1)</script> **breakout** \n # Hidden';
  const candidate = buildEditorialCandidate(event);
  assert.ok(!candidate.title.includes('<') && !candidate.title.includes('>'));
  assert.ok(!candidate.title.includes('*') && !candidate.title.includes('#'));
  assert.ok(!candidate.drafts[0].text.includes('<script>'));
  assert.equal(candidate.publication.approved, false);
});
test('candidate id and fingerprint are deterministic without secrets or usernames', () => {
  const a=buildEditorialCandidate(valid()), b=buildEditorialCandidate(valid());
  assert.equal(a.fingerprint,b.fingerprint);
  assert.deepEqual(a,b);
  assert.equal(a.publication.publicationAuthority,'NONE');
  assert.equal(JSON.stringify(a).includes('password'),false);
  assert.equal(JSON.stringify(a).includes('token'),false);
});
test('empty title remains a factual PR code update, not a deployment claim', () => {
  const event=valid();event.pull_request.title='';
  const out=buildEditorialCandidate(event);
  assert.match(out.drafts[0].text,/Codeänderung/);
  assert.match(out.drafts[0].text,/weder die Veröffentlichung/);
  assert.equal(out.quality.releaseEvidence,'NOT_PROVEN');
});
