# ASEH / עֲשֵׂה

**Make Jewish life. Make Jewish community.**

ASEH is an offline-first Android project for contextual prayer, practical Jewish life, source study, household practice, and community formation. It is organized around exact sources, visible reasoning, local choice, and the separation of historical texts from contemporary policy.

## Current status

The repository contract is approved and the Android application is under active development. The current `codex/shabbat-rehearsal-alpha7` review branch contains an offline functional rehearsal with device-location context, actionable preparation guides, a planning-only dated service chooser, readiness and role views, a focused synthetic conductor, source search and bookmarks, Self/Household/Qahal development dashboards, local service/governance records, a signed synthetic content-pack path, and preview-before-share/print packet export.

The development catalog, workspace fixture, and signed development pack are synthetic and visibly non-normative. The workspace dashboards demonstrate due work and lifecycle persistence; general user-created workspace CRUD is not yet implemented. Dated planning entries do not reconfigure the conductor. No distribution-approved siddur, Torah-reading corpus, or reviewed Hebrew interface is bundled. Staging and production therefore remain fail-closed while the open rights, liturgy, and editorial decisions are resolved. See the [Alpha 7 review guide](docs/releases/0.1.0-alpha.7-review.md) for the exact implemented walkthrough and limitations.

## Alpha target

The first public-alpha target remains a complete Shabbat-morning beit-knesset experience for a small emerging qahal. Alpha 7 exercises the product mechanics with synthetic data: preparation, local calendar/service planning, deterministic assembly traces, roles and reading handoffs, accessibility profiles, print output, a disputed-practice workflow, local search, and a disclosed deterministic source-bound provider. Reviewed liturgy and corpus content remain separate release blockers.

## Canonical documents

- Product and scope: `docs/PRODUCT.md`
- Method and source layers: `docs/METHOD.md`
- Editorial process: `docs/EDITORIAL_POLICY.md`
- Corpus and licensing: `docs/SOURCE_POLICY.md` and `docs/DATA_LICENSES.md`
- Privacy and safety: `docs/PRIVACY_MODEL.md` and `docs/THREAT_MODEL.md`
- Accessibility: `docs/ACCESSIBILITY.md`
- Release process: `docs/RELEASE.md`
- Deliberately open questions: `docs/OPEN_QUESTIONS.md`
- Specification ownership map: `docs/TRACEABILITY.md`

## Licensing

Application code, repository automation, and project documentation are licensed under Apache-2.0 under the root `LICENSE` and `NOTICE`. Original distributable ASEH editorial content under `content/` is licensed under CC BY-SA 4.0 unless a file or manifest states otherwise. Imported sources and editions retain their own recorded licenses and may be distributed only when the source manifest and build-time rights gate permit it.
