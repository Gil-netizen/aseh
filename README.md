# ASEH / עֲשֵׂה

**Make Jewish life. Make Jewish community.**

ASEH is an offline-first Android project for contextual prayer, practical Jewish life, source study, household practice, and community formation. It is organized around exact sources, visible reasoning, local choice, and the separation of historical texts from contemporary policy.

## Current status

The repository contract is approved and the Android application is under active development. The current `codex/shabbat-rehearsal-alpha7` review branch is being packaged as **ASEH alpha 8 review**. It contains an offline functional rehearsal with device-location context, browsable preparation guides with occurrence-scoped progress, an occurrence-driven service chooser, readiness and role views, a focused synthetic conductor, source and user-record search, Self/Household/Qahal workspaces with initial local editing, local service/governance records, a signed synthetic content-pack path, and preview-before-share/print packet export.

Alpha 8 adds practical local workspace use: a reviewer can rename a workspace; create, edit, and delete household responsibilities, calendar items, preparation kits, and nested tasks; maintain qahal decisions, volunteer rotations and slots, inventory, and financial-control checklists; see due work in Now; and open the exact local record from global search. Destructive record removal requires confirmation, corrupt local state is preserved rather than overwritten, and concurrent lifecycle changes are not reverted by an older open editor. Selecting a dated service now controls the service context shown in Pray. Progress is isolated by exact occurrence, including same-date calendar profiles. Only a dated Shabbat-morning occurrence matches the installed rehearsal; other selections remain visible as schedule metadata and fail closed instead of silently running or modifying the wrong conductor. Practice guides remain readable before setup, while their step controls stay disabled until that supported occurrence is active.

The development catalog, remaining seeded workspace records, and signed development pack are synthetic and visibly non-normative. Personal-practice creation, charters, education and care programs, document libraries, and collaboration remain future workspace work. No distribution-approved siddur, Torah-reading corpus, real Rambam corpus, or reviewed Hebrew interface is bundled. The development Ask ASEH provider remains deterministic and synthetic, and staging and production remain fail-closed while the open rights, liturgy, and editorial decisions are resolved. See the [Alpha 8 review guide](docs/releases/0.1.0-alpha.8-review.md) for the exact walkthrough, artifact placeholders, and limitations.

## Alpha target

The first public-alpha target remains a complete Shabbat-morning beit-knesset experience for a small emerging qahal. Alpha 8 exercises the product mechanics with synthetic data: preparation, local calendar/service planning, occurrence-bound service context, deterministic assembly traces, roles and reading handoffs, editable local workspace records, accessibility profiles, print output, a disputed-practice workflow, local search, and a disclosed deterministic source-bound provider. Reviewed liturgy and corpus content remain separate release blockers.

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
