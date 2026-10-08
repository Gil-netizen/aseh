# ExecPlan 0008: Weekday individual prayer first

**Status:** Active\
**Owner:** Implementation: Codex; editorial and product acceptance: Gil\
**Started:** 2026-10-08\
**Last updated:** 2026-10-08\
**Issue/PR:** working branch `codex/shabbat-rehearsal-alpha7`\
**Related ADRs:** [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0006](../adr/0006-manual-app-composition.md), [ADR-0007](../adr/0007-foreground-location-and-local-calendar.md)

This is the living implementation record for the correction from a community-rehearsal-first alpha to the individual-prayer-first product required by the specification. Keep the distinction between implemented behavior, source limitations, human review, and future work explicit.

## Outcome

The development APK opens Pray as a useful offline Hebrew-first weekday Shacharit reader. A person can start or resume, move through ordered prayer sections, mark progress, inspect why a section is present and its exact source status, and print or share the assembled local text without creating a community workspace or assigning communal roles. Community rehearsal remains available in the codebase but no longer blocks the personal prayer path.

## Success criteria

- Pray leads to weekday individual Shacharit instead of the Shabbat team-rehearsal setup.
- The reader contains substantial actual Hebrew source text, including the complete three biblical paragraphs of Shema and the Rambam/Wikisource wording that is explicit in the pinned source revisions.
- Wikisource abbreviations such as `וכו׳` remain visible and are disclosed as unresolved lacunae; the alpha never presents the research draft as an approved or complete canonical Nusach HaRambam.
- Start/resume, previous/next, completion, source details, accessibility cues, and print/share work offline.
- Progress is keyed to the local date and persists through the existing Room repository without a schema migration.
- The changed modules compile and the dev debug APK is produced. Verification stays focused on the new path.

## Canonical requirements

- `AGENTS.md` requires this ExecPlan for multi-module work and a review branch rather than direct protected-main changes.
- [PRODUCT.md](../PRODUCT.md) and the attached full build specification put weekday individual prayer before weekday communal and Shabbat community conductors.
- [METHOD.md](../METHOD.md), [EDITORIAL_POLICY.md](../EDITORIAL_POLICY.md), and [OPEN_QUESTIONS.md](../OPEN_QUESTIONS.md) prohibit silently calling a hybrid reconstruction “Rambam’s nosah”; `LITURGY-001` remains open.
- [SOURCE_POLICY.md](../SOURCE_POLICY.md) and [DATA_LICENSES.md](../DATA_LICENSES.md) allow only exact edition identities with pinned provenance, rights metadata, checksums, and attribution. Mechon Mamre and Mifal Mishneh Torah remain excluded.
- [ACCESSIBILITY.md](../ACCESSIBILITY.md) requires Hebrew RTL, English LTR, logical TalkBack order, button alternatives, and large-text reflow.
- [PRIVACY_MODEL.md](../PRIVACY_MODEL.md) requires local processing and deliberate export.

## Scope and non-goals

**In scope**

- A dedicated individual-prayer model and screen, independent of community roles and readiness gates.
- A development-flavor research draft built from pinned Hebrew Wikisource Rambam revisions and exact allowlisted Miqra According to the Masorah API responses.
- Local progress, source inspection, and structured-text share/print.
- A directly downloadable alpha APK.

**Out of scope**

- Claiming that this draft resolves `LITURGY-001`, that abbreviated passages are complete, or that AI has granted human editorial approval.
- Mincha, Arvit, Rosh Hodesh, fast-day, Shabbat, festival, household, and community flows in this first correction.
- Generalizing the existing Shabbat rehearsal assembler or content-pack schema before the personal reader is usable.

## Current state and context

`PrayerScreen.kt` is a large community rehearsal surface that deliberately supplies no prayer text. `FlavorContentCatalog.kt` contains one synthetic Shabbat service and marks liturgical fields unavailable. `AsehApp.kt` prefers a future Shabbat-morning occurrence and blocks weekday schedule entries. The reusable pieces are the existing `ServiceSegment` text/provenance model, Room-backed occurrence progress, share/print adapters, local calendar/location context, and source navigation.

The new path uses `IndividualPrayerService` and `IndividualPrayerScreen` directly. It does not wrap personal prayer in `DemonstratorCatalog`, because that type requires a community choice, preflight, roles, and reading assignments. The dev flavor supplies the draft; staging and production continue to fail closed until human review permits distribution there.

## Design and interfaces

`IndividualPrayerService` holds an ordered list of existing `ServiceSegment` records and exact `SourceUnit` records. Each segment carries Hebrew text, movement/voice/accessibility cues, an explanation trace, and edition-level provenance. The source text is displayed RTL and primary. Translation and transliteration are absent until separately licensed and reviewed.

`IndividualPrayerScreen` is a focused sequential reader. It starts from the first incomplete section, keeps navigation buttons visible, and puts cues and source detail behind secondary controls so prayer text remains primary. The packet helper emits the same ordered text and attribution for deliberate Android share/print actions.

`AsehApp` gives the personal service a stable `ServiceInstanceKey` based on service ID and local civil date, then uses the existing repository callback to persist section completion. This personal key is independent of the Shabbat scheduler and does not require GPS, community profile, a Qahal, roles, a quorum, readings, or preflight. Date and device-derived place remain visible context when available.

The pinned raw inputs live under `content/sources/`. The bundled draft attributes Hebrew Wikisource contributors, links CC BY-SA 4.0, records revision IDs and retrieval checksums, identifies formatting changes, and labels source pages as not reviewed. The full Shema paragraphs come from exact Sefaria API responses for `Miqra according to the Masorah`; HTML presentation markup, page-only notes, and cantillation marks are removed while Hebrew letters, vocalization, and verse punctuation are retained.

## Milestones

### 1. Personal reader and persistence

**Result:** Pray starts/resumes a sequential weekday individual service without communal setup.

**Verification:**

```text
./gradlew :core:model:test :feature:prayer:test :app:compileDevDebugKotlin
Expected: exit 0.
```

### 2. Pinned research content and attribution

**Result:** Real Hebrew text, exact source identities, visible draft status, and export attribution are present offline.

**Verification:**

```text
./gradlew :app:assembleDevDebug
Expected: exit 0 and app-dev-debug.apk.
```

### 3. Review APK

**Result:** A versioned APK is uploaded as a direct GitHub prerelease asset with a phone-downloadable URL.

**Verification:**

```text
Inspect APK package/version and verify the release asset returns HTTP 200.
```

## Progress

- [x] 2026-10-08 — Re-read the specification and confirmed weekday individual prayer is the mandated first Tefillah increment.
- [x] 2026-10-08 — Audited the existing code and isolated reusable persistence, text, provenance, export, and context mechanics from the communal rehearsal shell.
- [x] 2026-10-08 — Retrieved and checksummed pinned Hebrew Wikisource revisions and exact allowlisted Shema API responses.
- [x] 2026-10-08 — Implement the personal model, dev catalog, reader, application wiring, and flavor fail-closed stubs.
- [ ] 2026-10-08 — Focused compilation and APK assembly pass; direct GitHub release publication remains.

## Decisions

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| 2026-10-08 | Add a parallel personal-prayer path instead of extending `DemonstratorCatalog`. | The current catalog structurally requires community concepts that must not block an individual. | ADR-0006 |
| 2026-10-08 | Ship pinned source text as a visibly unreviewed development research draft. | It provides a real testable prayer surface while preserving the unresolved canonical-anchor and human-review state. | none |
| 2026-10-08 | Preserve every source abbreviation instead of silently filling it. | The selected Rambam witness abbreviates familiar passages and the project may not invent a canonical reconstruction. | none |

## Surprises and discoveries

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| 2026-10-08 | The existing prayer UI can render text but the installed catalog deliberately marks all liturgy unavailable. | `PrayerScreen.kt` and `FlavorContentCatalog.kt` | Reuse models and persistence, replace the entry experience. |
| 2026-10-08 | Hebrew Wikisource explicitly labels the Rambam Order of Prayer pages “not reviewed” and contains many `וכו׳` abbreviations. | pinned revisions 2998697, 2870245, 2870237 | Label the alpha as a research draft and expose gaps. |
| 2026-10-08 | The Wikisource pages state CC BY-SA 4.0 directly. | page footer and license link | Record attribution, license, revisions, changes, and checksums. |

## Verification and acceptance

Verification is deliberately narrow: compile the touched modules, run only focused model/feature tests if present, assemble the dev APK once, inspect package/version/digest, and check the direct release URL. Human phone review determines usability and textual correctness; automated checks do not grant editorial approval.

## Rollout and recovery

The research reader is dev-flavor-only and uses a separate alpha application ID. Staging and production return no personal prayer catalog. A forward fix can replace the dev content or UI without migrating user records; clearing the alpha app removes its local progress. The previous community rehearsal remains as a fallback code path.

## Residual risks and handoff

The draft is not a complete siddur because the selected Rambam source shortens familiar prayers. The exact anchor edition, treatment of witnesses, completed lacunae, translation, seasonal rules, and human textual review remain follow-up work. A later public production pack must satisfy the full rights and editorial gates even if the development APK is useful for product testing.

## Outcomes

Pending implementation and APK publication.
