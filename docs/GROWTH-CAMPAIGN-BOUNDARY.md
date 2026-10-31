# Campaign integration boundaries (post-P0 proposal, not implemented)

**Intent:** Convert verified, owner-approved AI-DeviceCare milestone/evidence changes into useful, clearly labeled technical editorial material. Not a promotional autoposter or a shadow news publisher.

## Source flow

`Accepted PR/Milestone SHA -> signed GitHub webhook with replay protection -> bounded durable event inbox -> validated factual claim ledger -> editorial draft -> CADS-style content/brand quality checks -> human review -> platform-specific publication adapter -> consent-based analytics`

## Event delivery and streaming
- Prefer GitHub Actions or an existing authenticated webhook listener to start. **Do not deploy NATS/Kafka for P0**, and do not turn Capital-AI's intentionally disabled NATS market data infrastructure on.
- If sustained asynchronous workloads justify it, select a durable queue with idempotency keys, deduplication, DLQ, retry/backoff and event schemas; keep the subscriber decoupled from CI and billing.
- Never expose repository or customer secrets in social content or LLM prompts.
- Reuse Capital-AI CADS contract/provenance patterns **only after license and owner-rights review**, not the CADS Marketplace product or paid hosted runtime.
- Any autonomous publishing must be expressly scoped to owned or licensed channels with adequate moderation and editorial review. No impersonation, undisclosed sponsored content, community-rule bypass or bulk forum submissions.

## Editorial channels (research candidates; not connected)
- First-party technical changelog/blog on Capital-AI/AI-DeviceCare websites.
- GitHub Discussions/Releases with actual changelog and reproducible evidence.
- Independent editorial publications (for example mobile privacy, Android engineering, IT security) **via article pitches and editorial acceptance**, not unsolicited automated publication.
- Community developer channels where rules explicitly permit relevant, disclosed posts. Check forum policies and authentic first-party accounts before any integration.

## Commercial approach
- Product differentiation: early free battery/privacy audit, transparent permission limits, evidence-first device scoring, guided remediation.
- Brand sequence: P0 rights and dual-brand positioning; M1 store assets and actual tested feature claims; M2 validated security detection reports; M3 privacy protection; M4 business fleet case studies.
- Metrics: confirmed editorial acceptance, transparent referrals, consent-based installs, M1 activation, legitimate paid conversion. Do not invent reach, endorsements, journalist affiliation or published articles.

**Status:** DESIGN ONLY. No webhook, event stream, CADS license, journalist relationship, campaign or social post created.
