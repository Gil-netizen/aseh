# ExecPlan 0008: Weekday individual prayer first

**Status:** Active\
**Owner:** Implementation: Codex; editorial and product acceptance: Gil\
**Started:** 2026-10-08\
**Last updated:** 2026-10-11\
**Issue/PR:** working branch `codex/shabbat-rehearsal-alpha7`\
**Related ADRs:** [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0006](../adr/0006-manual-app-composition.md), [ADR-0007](../adr/0007-foreground-location-and-local-calendar.md)

This is the living implementation record for the correction from a community-rehearsal-first alpha to the individual-prayer-first product required by the specification. Keep the distinction between implemented behavior, source limitations, human review, and future work explicit.

## Outcome

The development APK opens Pray as a useful offline Hebrew-first weekday reader with separate Shacharit, Mincha, and Arvit services. A person can accept the local-time suggestion or choose a service, start or resume that service's dated progress, move through ordered prayer sections, inspect its exact source status, and print or share the assembled local text without creating a community workspace or assigning communal roles. Community rehearsal remains available in the codebase but no longer blocks the personal prayer path.

## Success criteria

- Pray leads to separate weekday individual Shacharit, Mincha, and Arvit services instead of the Shabbat team-rehearsal setup.
- The reader contains complete readable ordinary-weekday selections assembled from pinned Hebrew sources. The pinned Rambam Order of Prayer revisions are the research anchor, and separately identified Yemenite Baladi pages supply text where that witness abbreviates familiar passages.
- The alpha labels the result as a `Drafted` development composite and never presents it as a human-approved, canonical, or pure Nusach HaRambam edition.
- A local-time suggestion is only a convenience; the person can always select another service. Location is optional, obtained through a deliberate foreground device request when used, and prayer remains available without it.
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

- A dedicated three-service individual-prayer model and screen, independent of community roles and readiness gates.
- A development-flavor research draft built from pinned Hebrew Wikisource Rambam revisions and pinned Hebrew Wikisource Yemenite Baladi weekday pages.
- Local progress, source inspection, and structured-text share/print.
- Optional one-shot device location from the prayer surface, with manual coordinates kept as an advanced fallback.
- A directly downloadable alpha APK.

**Out of scope**

- Claiming that this composite resolves `LITURGY-001`, is a settled Nusach HaRambam, or has received human rights or editorial approval.
- Rosh Hodesh, fast-day, Shabbat, festival, household, and community prayer flows in this increment.
- Automatic seasonal, calendar-dependent, or local-practice substitutions beyond visibly preserving alternatives for later review.
- Generalizing the existing Shabbat rehearsal assembler or content-pack schema before the personal reader is usable.

## Current state and context

The baseline before this plan was a large community rehearsal surface that deliberately supplied no prayer text. `FlavorContentCatalog.kt` contained one synthetic Shabbat service and marked liturgical fields unavailable, while `AsehApp.kt` preferred a future Shabbat-morning occurrence and blocked weekday schedule entries. The reusable pieces were the existing `ServiceSegment` text/provenance model, Room-backed occurrence progress, share/print adapters, local calendar/location context, and source navigation.

The new path uses `IndividualPrayerService` and `IndividualPrayerScreen` directly. It does not wrap personal prayer in `DemonstratorCatalog`, because that type requires a community choice, preflight, roles, and reading assignments. The dev flavor supplies the draft; staging and production continue to fail closed until human review permits distribution there.

## Design and interfaces

`IndividualPrayerService` holds an ordered list of existing `ServiceSegment` records and exact `SourceUnit` records. Each segment carries Hebrew text, movement/voice/accessibility cues, an explanation trace, and edition-level provenance. The source text is displayed RTL and primary. Translation and transliteration are absent until separately licensed and reviewed.

`IndividualPrayerScreen` is a focused sequential reader with an explicit three-service chooser. It starts from the first incomplete section for the selected service, keeps navigation buttons visible, and puts cues and source detail behind secondary controls so prayer text remains primary. A local-time function suggests Shacharit, Mincha, or Arvit without making a religious timing determination. The packet helper emits the same ordered text and attribution for deliberate Android share/print actions.

`AsehApp` gives each personal service a stable `ServiceInstanceKey` based on service ID and local civil date, then uses the existing repository callback to persist section completion. Each service therefore keeps separate progress. These personal keys are independent of the Shabbat scheduler and do not require GPS, community profile, a Qahal, roles, a quorum, readings, or preflight. Date and device-derived place remain visible context when available.

The pinned raw inputs live under `content/sources/`. The Rambam research anchor is `rambam-wikisource/manifest.json`, covering Order of Prayer revisions `2998697`, `2870245`, and `2870237`. The completion witness is `baladi-wikisource/manifest.json`, covering weekday Shacharit revision `3081633`, Mincha revision `1076941`, and Arvit revision `2947373`. The bundled draft attributes Hebrew Wikisource contributors, links CC BY-SA 4.0, records revision IDs and SHA-256 checksums, identifies runtime selection and normalization, and labels every source `Drafted`. The Baladi pages are collaborative, unreviewed composites rather than a named historical edition; the app identifies that relationship instead of attributing completed text solely to Rambam. No human rights or editorial approval is recorded.

## Milestones

### 1. Personal reader and persistence

**Result:** Pray selects and starts/resumes one of three sequential weekday individual services without communal setup.

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
- [x] 2026-10-10 — Pinned the Baladi Shacharit, Mincha, and Arvit page revisions and recorded exact source, rendered/display, and wikitext checksums in `content/sources/baladi-wikisource/manifest.json`.
- [x] 2026-10-10 — Added three service choices, local-time suggestion, isolated per-service daily progress, and optional foreground device location from Pray.
- [x] 2026-10-11 — Updated the rights inventory and this plan to identify the Rambam anchor, the Baladi completion witness, CC BY-SA 4.0 attribution, `Drafted` status, and absence of human approval.
- [x] 2026-10-11 — Ran the focused model test and dev assembly, verified package/version metadata, and exercised the personal-first home card, all three service readers, GPS retry, preview, and save on the emulator.
- [ ] 2026-10-11 — Direct GitHub release publication remains.

## Decisions

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| 2026-10-08 | Add a parallel personal-prayer path instead of extending `DemonstratorCatalog`. | The current catalog structurally requires community concepts that must not block an individual. | ADR-0006 |
| 2026-10-08 | Ship pinned source text as a visibly unreviewed development research draft. | It provides a real testable prayer surface while preserving the unresolved canonical-anchor and human-review state. | none |
| 2026-10-08 | Preserve every source abbreviation instead of silently filling it. | The selected Rambam witness abbreviates familiar passages and the project may not invent a canonical reconstruction. | none |
| 2026-10-10 | Complete the development reader from separately identified pinned Baladi pages while retaining Rambam as its research anchor. | A usable continuous reader requires the abbreviated passages, while source policy requires the completion witness to remain visible and forbids calling the composite a settled Nusach HaRambam. This supersedes exposing every Rambam abbreviation as the runtime text. | none |
| 2026-10-10 | Keep the Baladi composite at `Drafted` and in the dev flavor. | The source pages are collaborative and unreviewed, and no human has approved the rights record, textual selection, sequence, or canonical claim. | none |

## Surprises and discoveries

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| 2026-10-08 | The existing prayer UI can render text but the installed catalog deliberately marks all liturgy unavailable. | `PrayerScreen.kt` and `FlavorContentCatalog.kt` | Reuse models and persistence, replace the entry experience. |
| 2026-10-08 | Hebrew Wikisource explicitly labels the Rambam Order of Prayer pages “not reviewed” and contains many `וכו׳` abbreviations. | pinned revisions 2998697, 2870245, 2870237 | Label the alpha as a research draft and expose gaps. |
| 2026-10-08 | The Wikisource pages state CC BY-SA 4.0 directly. | page footer and license link | Record attribution, license, revisions, changes, and checksums. |
| 2026-10-10 | No source found combined complete weekday text, a named exact Rambam witness, machine-readable input, and resolved redistribution review. | source audit recorded in `content/sources/README.md` and the two Wikisource manifests | Use the pinned Baladi pages only as a transparent development completion witness; retain `LITURGY-001` and the human review gate. |
| 2026-10-10 | The three Baladi pages are collaborative page revisions rather than a named historical edition. | revisions 3081633, 1076941, 2947373 | Identify each page and revision exactly; do not market the runtime composite as an approved edition. |

## Verification and acceptance

Verification is deliberately narrow: compile the touched modules, run only focused model/feature tests if present, assemble the dev APK once, inspect package/version/digest, and check the direct release URL. Human phone review determines usability and textual correctness; automated checks do not grant editorial approval.

## Rollout and recovery

The research reader is dev-flavor-only and uses a separate alpha application ID. Staging and production return no personal prayer catalog. A forward fix can replace the dev content or UI without migrating user records; clearing the alpha app removes its local progress. The previous community rehearsal remains as a fallback code path. Publishing any artifact that contains this content still requires the release owner to reconcile the open rights gate in `DATA_LICENSES.md`; a successful build does not grant distribution approval.

## Residual risks and handoff

The alpha supplies continuous ordinary-weekday selections, but their completeness, ordering, omissions, and fidelity have not received human textual review. The exact canonical anchor, treatment of witnesses, translation, calendar and seasonal rules, and local practice variants remain follow-up work. A later public production pack must satisfy the full rights and editorial gates even if the development APK is useful for product testing.

## Outcomes

The three-service implementation, source records, focused verification, and APK assembly are complete. Direct APK publication remains pending.
