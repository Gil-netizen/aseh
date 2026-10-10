# ExecPlan 0007: Operational workspaces and dated service occurrences

**Status:** Active; implementation is present, the ADR-0003 workspace-persistence correction is in progress, and final exact-commit verification is pending\
**Owner:** Implementation: Codex; product acceptance and release: Gil\
**Started:** 2026-10-07\
**Last updated:** 2026-10-07\
**Issue/PR:** [PR #11](https://github.com/Gil-netizen/aseh/pull/11); working branch `codex/shabbat-rehearsal-alpha7`\
**Related plans:** [ExecPlan 0006](0006-functional-shabbat-rehearsal.md)\
**Related ADRs:** [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0006](../adr/0006-manual-app-composition.md), [ADR-0007](../adr/0007-foreground-location-and-local-calendar.md); [ADR-0008](../adr/0008-local-calendar-context-engine.md) remains Proposed

This plan records the alpha.8 delta from the alpha.7 functional Shabbat rehearsal. It is intentionally separate from ExecPlan 0006 because the change turns two labeled demonstration surfaces into durable user behavior: editable local organizational records and service-occurrence-specific rehearsal state. Keep `Progress`, `Decisions`, `Surprises and discoveries`, and `Outcomes` current so a contributor can distinguish implemented behavior, verified behavior, and required correction without chat history.

## Outcome

The alpha.8 development build lets a user maintain useful Household and Qahal records on the device, find an exact local record through global search, and open it in its owning workspace. The service selected in Pray supplies the actual civil date and calendar profile for a supported Shabbat-morning rehearsal, and preparation, assignments, and conductor progress remain isolated from every other scheduled occurrence. Unsupported service selections remain inspectable but cannot masquerade as the Shabbat rehearsal or mutate its state. Practice guides remain readable and saveable before setup, while occurrence-owned progress becomes writable only after a supported occurrence exists.

This outcome extends the operational mechanics of the recommended Shabbat-morning demonstrator. It does not add approved prayer text, Torah passages, a Rambam corpus, reviewed Hebrew, or connected AI.

## Success criteria

- [x] Self, Household, and Qahal workspace names can be edited with validation and survive navigation.
- [x] Household users can create, edit, and delete responsibilities, shared-calendar items, preparation kits, and nested kit tasks.
- [x] Qahal users can create, edit, and delete local decisions, inventory items, volunteer rotations and slots, and financial-control checklists and controls.
- [x] Deletes require a specific confirmation, invalid names, dates, ranges, and quantities do not save, and editing an older form does not overwrite newer lifecycle state or nested records.
- [x] Global search indexes workspace names and user records locally, applies all query terms, and deep-links to the owning context, record, and editor surface without a network request.
- [x] A supported scheduled Shabbat-morning service displays its name, civil date, day kind, time, and calendar profile and supplies the rehearsal's date and region.
- [x] Practice steps, preflight, role assignments, reading plans, and conductor completion are keyed to a complete occurrence identity rather than only a civil date.
- [x] Changing occurrence, date, or Israel/diaspora profile preserves the other occurrence's state; stale callbacks and unsupported selections cannot write through to the active rehearsal.
- [x] Unsupported weekday, afternoon, or festival selections show their metadata and an explicit unavailable-content state instead of substituting the synthetic Shabbat conductor.
- [x] Practice catalog, details, exact sources, and saved-card actions remain available before service setup; occurrence-owned step controls explain why they are disabled.
- [ ] User-created workspace records live in the ADR-0003 operational Room store. A one-time import preserves valid alpha.7/early-alpha.8 SharedPreferences state, quarantines unreadable legacy payloads, removes migrated keys only after a durable Room commit, and preserves an explicit clear-data recovery path.
- [ ] The final source revision passes unit, migration, lint, policy, API 26, API 37 at 200% text, exact APK inspection, install, and cold-launch checks, with any platform-specific failure stated precisely.
- [ ] Gil reviews the concrete alpha.8 APK and decides whether to merge or publish. This plan does not authorize an AI agent to approve, merge to protected `main`, or perform the owner's final publication action.

## Canonical requirements

- [Repository instructions](../../AGENTS.md) require an ExecPlan for this multi-module work, protected-main review, appropriate verification, preservation of user changes, and explicit reporting of assumptions and gaps.
- [PRODUCT.md](../PRODUCT.md) defines offline-first Self, Household, and Qahal workspaces, global search and context switching, explicit correctable context, date/profile-aware prayer assembly, local community records without universal claims, and the Shabbat-morning demonstrator.
- The attached product/build specification calls for actionable household and qahal coordination, service planning, role and reading assignments, source explanations, offline operation, and a complete Shabbat-morning beit-knesset demonstrator. It does not override repository policy or decide unresolved corpus rights.
- [ADR-0003](../adr/0003-database-and-data-class-separation.md) requires non-sensitive user-created organizational records, schedules, checklists, and review dates to live in the operational Room database. Preferences are for small settings. Feature and domain modules receive repositories and domain models rather than database handles.
- [ADR-0006](../adr/0006-manual-app-composition.md) keeps persistence adapters in the application-owned composition root and passes narrow state and callbacks into features.
- [ADR-0007](../adr/0007-foreground-location-and-local-calendar.md) keeps GPS and manual place input distinct from explicit calendar-profile choices. Coordinates cannot silently choose Israel or diaspora.
- [ADR-0008](../adr/0008-local-calendar-context-engine.md) describes the proposed local calendar boundary, but it is not accepted product policy. Alpha.8 may expose deterministic development-schedule metadata without claiming that the proposal settles readings, additions, or liturgical rules.
- [PRIVACY_MODEL.md](../PRIVACY_MODEL.md) and [THREAT_MODEL.md](../THREAT_MODEL.md) require local-first minimal data, deliberate export, complete deletion, and no credentials or sensitive case material in operational storage, indexes, logs, fixtures, or evidence.
- [ACCESSIBILITY.md](../ACCESSIBILITY.md) requires Hebrew RTL, English LTR, mixed-script safety, TalkBack semantics, clear errors and disabled states, visible focus, and reflow through 200% text.
- [RELEASE.md](../RELEASE.md) distinguishes a development-review APK from a public release and reserves final publication for the owner after required CI, signing, device, accessibility, source, and artifact evidence.
- [OPEN_QUESTIONS.md](../OPEN_QUESTIONS.md) leaves the intended Rambam source and liturgical rights questions unresolved. Alpha.8 must continue to fail closed for unavailable sacred content.

The initial alpha.8 implementation stored a complete editable workspace snapshot in SharedPreferences. That was acceptable only while the surface was a labeled synthetic fixture, as recorded in ExecPlan 0006. Once alpha.8 made those records user-created organizational data, it conflicted with ADR-0003. The correction is mandatory: current workspace state belongs in the operational Room database. This plan records the correction; it does not create an exception or new persistence policy.

## Scope and non-goals

**In scope**

- Rename the active Self, Household, or Qahal workspace.
- Add validated CRUD for the bounded Household and Qahal record types listed in the success criteria, including nested records.
- Preserve lifecycle fields and child lists when an editor submits against a newer in-memory snapshot.
- Persist workspace state through the operational Room boundary, with validated legacy import, last-good recovery data, unreadable-state quarantine, and explicit deletion.
- Add a non-sensitive in-memory search index over local workspace state and exact record deep links into Build.
- Represent a scheduled service with a stable occurrence ID plus explicit civil date and isolate all rehearsal-owned state by that identity.
- Migrate the operational database without destructive fallback and preserve legacy date-scoped rehearsal records under an explicit legacy identity.
- Make unsupported service coverage fail closed and keep Practice browsing useful before setup.
- Produce a separately installable `0.1.0-alpha.8` development-review APK after final validation.

**Out of scope**

- Approved liturgical text, translation, transliteration, Torah passages, calendar-linked weekly readings, or a distributable Rambam corpus.
- Human editorial approval, source-rights decisions, or any claim that synthetic content is a valid service or universal community rule.
- Personal-practice creation and editing, full household/qahal charter authoring, care cases, private decision evidence, sensitive participant records, accounts, synchronization, or multi-device collaboration.
- Conductors for weekday, afternoon, festival, or arbitrary installed services. Alpha.8 supports one exact synthetic Shabbat-morning rehearsal shape.
- Connected AI, provider credentials, paid services, or a production content pack.
- A reviewed Hebrew translation or completion of the full release accessibility matrix.
- Public release signing, signed Git tags, an AAB release, merge to protected `main`, or publication.

## Current state and context

ExecPlan 0006 made all five destinations interactive, but it deliberately described two gaps. Workspace dashboards replayed lifecycle commands over a labeled synthetic fixture rather than supporting general record creation, and dated service entries were planning metadata disconnected from the fixed Shabbat conductor. Alpha.8 addresses those gaps while retaining the existing GPS-first Now screen, synthetic Practice and Study content, source-bound fake provider, role views, reading assignments, packet preview/share/print, and local-data deletion.

The current implementation spans these boundaries:

- `domain/workspace/LocalWorkspaceApi.kt` defines typed workspace models, validation, lifecycle transitions, record-specific put/delete/rename commands, and latest-snapshot merge behavior.
- `feature/workspace/WorkspaceEditors.kt`, `WorkspaceEditCommands.kt`, and `WorkspaceDashboard.kt` provide validated Compose editors, save/cancel/delete flows, nested record editing, and record focus/highlight.
- `core/database/WorkspaceStateStore.kt` defines the narrow operational-store contract and the singleton `workspace_snapshot_state` Room row for current, backup, and retained legacy payloads. `OperationalDatabase.kt` adds the v4-to-v5 migration and exports the schema through the normal Room path.
- `app/WorkspaceReviewStateRepository.kt` maps the strict versioned workspace codec to `WorkspaceStateStore`. The initial alpha.8 working tree wrote current and backup JSON directly to SharedPreferences; the correction now present in the working tree leaves those preferences only as an idempotent one-time import source. Verification of that correction remains pending.
- `app/WorkspaceSearchIndex.kt` derives non-sensitive search documents from the latest local snapshot. `GlobalNavigationTools.kt` and `AsehApp.kt` carry a stable record target into the owning workspace.
- `core/model/ExperienceModels.kt` defines `ServiceInstanceKey(occurrenceId, serviceDate)` and state projections that hide progress belonging to another occurrence.
- `core/database` stores shared state separately from occurrence-owned service records. The service-occurrence schema adds an occurrence table and keys role assignments, reading assignments/plans, and rehearsal markers by `service_instance_id`.
- `domain/servicecatalog` marks the exact development Shabbat-morning occurrence that can drive the rehearsal. `AsehApp.kt` resolves the selected schedule item, activates supported state, rejects stale writes, and supplies the selected civil date and calendar region to assembly.
- `feature/prayer`, `feature/practice`, and `feature/build` display supported/unsupported occurrence state and gate only the mutations that require a supported rehearsal.

Source currently declares version code `8`, base version name `0.1.0-alpha.8`, and development package `io.github.gilnetizen.aseh.dev.alpha8.debug` with label `ASEH alpha 8 review`. It intentionally installs separately from alpha.7, so earlier package data does not migrate across Android application IDs.

## Design and interfaces

### Workspace commands and stale-editor behavior

Editors create record-level commands rather than replacing the whole workspace object. `LocalWorkspaceApi.apply` validates the command against the repository's latest snapshot. For an existing record, editable fields come from the submitted form while lifecycle status, status timestamps, source references, dissent fields, and nested children that the form does not own remain from the latest stored record. A parent delete removes its nested children only after a confirmation names that effect. Stable locally generated IDs distinguish adds from edits.

The UI must show field-specific validation and retain the editor after a rejected save. A completed command closes the editor only after the repository reports that the persisted latest snapshot contains the intended change. This prevents a stale Compose callback from presenting an uncommitted change as saved.

### Workspace persistence boundary

Workspace names, responsibilities, calendar items, preparation kits/tasks, decisions, inventory, rotations/slots, and financial controls are non-sensitive operational user records under ADR-0003. The final alpha.8 persistence path is therefore:

1. the application repository encodes and decodes the workspace envelope with the existing strict, allowlisted, versioned codec;
2. the operational Room database owns the current envelope, last-good envelope, and any raw unreadable legacy payload needed for explicit recovery;
3. app-layer mapping converts the decoded payload to domain `WorkspaceSnapshot` values and runs `LocalWorkspaceApi.validate` before exposing or updating state;
4. each mutation writes the previous validated current envelope as backup and the new validated envelope as current in one Room transaction;
5. the one-time importer reads the former `aseh_workspace_review_state` SharedPreferences keys, validates a current or backup snapshot or replays the legacy command log, commits the result or quarantine record to Room, and removes migrated preferences only after that commit succeeds; and
6. an unreadable current/backup/import payload is retained, user writes remain blocked, and explicit local-data deletion clears the operational rows and legacy keys before restoring the labeled fixture baseline.

The operational store contains no credentials, copied corpus passages, sensitive case narratives, aid requests, accusations, or private decision evidence. Search is derived in memory from the approved non-sensitive workspace record fields and is not a second durable plaintext index. Feature modules never receive Room DAOs, database paths, or Android persistence objects.

### Service-occurrence identity and compatibility

`ServiceInstanceKey` contains both the stable schedule occurrence ID and civil service date. The date is presentation and calendar context; the occurrence ID distinguishes services and opinion profiles that can share a date. The active profile points to one occurrence, while Room retains data for other occurrences.

The operational schema change first introduces occurrence-owned rows and migrates older date-only state to `legacy-date:<civil-date>`. It does not pretend that legacy data belonged to a known service plan. The workspace-persistence correction then advances the operational database to schema version 5. Every shipped schema has an exported Room snapshot and an explicit forward migration; destructive fallback remains disabled.

All occurrence-owned writes take the key captured by the visible screen and verify it against the supported selected occurrence. A stale callback is rejected after the selection changes. Global saved cards, bookmarks, workspace records, charter/dossier state, and small interface preferences remain shared across occurrences.

### Supported and unsupported service selection

The selected service card is always inspectable. Only an occurrence whose installed development definition, day kind, service shape, and opinion profile match the supported Shabbat-morning rehearsal produces a `ServiceInstanceKey` for conductor mutations. Weekday, afternoon, festival, missing, or otherwise unsupported entries display their metadata and an unavailable explanation. They do not receive a fallback conductor, packet, assignment form, or progress write.

The selected service remains in the visible schedule list even when it falls outside the chronological preview window. Coordinates can calculate the selected date's local solar values, but the user-selected calendar region remains explicit and controls the Israel/diaspora profile.

### Search and deep links

The local search builder emits one document per workspace and record, including meaningful non-sensitive names, summaries, scope, role, assignment, and date fields. Normalized query terms use AND semantics: every term must occur in the indexed text. Results carry workspace kind, workspace ID, record ID, parent ID where needed, and a display label. Opening a result switches the active context, navigates to Build workspace work, scrolls the owning section into view, and focuses/highlights the exact record. No search result grants editorial status or exposes a private evidence field.

### Practice before service setup

Practice-card browsing, details, exact synthetic source links, and saved-card state are global and remain useful without service setup. Checklist progress belongs to a service occurrence. Before a supported occurrence is active, toggles are disabled with an explanatory notice rather than silently writing date-less progress. Once a supported occurrence is selected, progress resumes from that occurrence's Room rows.

### Accessibility, language, privacy, and trust boundaries

Editors use labeled controls, semantic headings, keyboard-reachable actions, text errors, confirmation dialogs, and scroll/focus behavior that remains usable at 200% font scale. Hebrew locale continues to exercise RTL structure with explicit English fallback copy; it is not represented as reviewed Hebrew. Mixed-script values and stable IDs remain readable in their natural direction.

All added functionality is offline and local. No account, network provider, analytics, or credential path is introduced. The development catalog remains synthetic and pre-approval. Local qahal decisions state only local operating scope; creating or editing one does not upgrade its evidence, civil, denominational, or editorial status.

## Milestones

### 1. Typed workspace maintenance

**Result:** Household and Qahal records can be added, edited, deleted, validated, and nested without overwriting lifecycle state or child records changed after an editor opened.

**Work:**

- Add record-specific commands and validation to `LocalWorkspaceApi`.
- Add form-state builders and Compose editors with save, cancel, and confirmed delete.
- Re-read the latest repository snapshot when applying a command and preserve fields outside the editor's ownership.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :domain:workspace:test :feature:workspace:testDebugUnitTest
Expected: command, validation, stale-editor merge, nested-record, and editor helper tests pass.
```

### 2. ADR-0003 workspace persistence correction

**Result:** The operational Room database, rather than SharedPreferences, owns user-created workspace state and its recovery material; valid legacy review state imports once without silent loss.

**Work:**

- Add operational Room entities/DAO operations for current, last-good, and quarantined legacy workspace payloads and advance the database to schema version 5.
- Preserve the strict app-layer workspace codec and validation boundary.
- Import valid SharedPreferences snapshots or the legacy command log exactly once, remove migrated keys only after a successful Room commit, and preserve unreadable raw state with writes blocked until explicit clear.
- Include workspace rows in the existing confirmed local-data deletion flow.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :core:database:testDebugUnitTest :app:testDevDebugUnitTest :app:compileDevDebugAndroidTestKotlin
Expected: schema and migration tests pass; current/backup recovery, legacy import, quarantine, write blocking, deletion, and process recreation are covered.
```

### 3. Local search and exact workspace navigation

**Result:** A distinctive local record is findable offline and opens at the exact owning workspace record.

**Work:**

- Build an in-memory non-sensitive search index from the latest workspace snapshot.
- Apply normalized all-term matching and merge local results with installed development-catalog results.
- Carry context, parent, and record IDs through global navigation and scroll/focus the destination after composition.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :app:testDevDebugUnitTest :app:compileDevDebugAndroidTestKotlin
Expected: indexing, AND matching, stable result identity, deep-link routing, and exact-record Compose navigation tests pass.
```

### 4. Occurrence-specific rehearsal state

**Result:** A supported dated service controls Prayer and owns isolated rehearsal progress; unsupported service selections fail closed.

**Work:**

- Add `ServiceInstanceKey` and full-key projections.
- Key Room service rows and markers by occurrence ID, retain the civil date, and migrate legacy date-only data explicitly.
- Resolve the supported schedule occurrence in the composition root and guard every occurrence-owned callback.
- Show supported/unsupported metadata and keep the selected service visible outside the schedule preview limit.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :core:model:test :core:database:testDebugUnitTest :domain:servicecatalog:test :app:testDevDebugUnitTest :feature:prayer:testDebugUnitTest
Expected: occurrence validation, date/profile isolation, legacy migration, stale-write rejection, unsupported-selection behavior, and service-list retention tests pass.
```

### 5. Practice availability and integrated UI

**Result:** Practice content remains browseable before setup, occurrence-owned controls are safely gated, and system Back unwinds nested Practice, Prayer, and Workspace views before leaving their destination.

**Work:**

- Separate global saved-card behavior from occurrence-owned step progress.
- Add disabled-state explanation and semantics.
- Preserve nested-view Back behavior, large-text scroll access, and exact search-result focus.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :feature:practice:testDebugUnitTest :feature:prayer:testDebugUnitTest :feature:workspace:testDebugUnitTest :app:compileDevDebugAndroidTestKotlin
Expected: feature tests and the end-to-end instrumentation source compile.
```

### 6. Integrated alpha.8 candidate

**Result:** The exact source commit produces an installable, separately identified alpha.8 APK whose new workspace and occurrence flows pass on API 26 and API 37.

**Work:**

- Stop shared-file edits, export the final Room schema, and run all local gates from the settled tree.
- Verify API 26 and API 37 at 200% text, including workspace CRUD/restart/search, occurrence switching/isolation, unsupported selection, nested Back, deletion, and existing alpha.7 flows.
- Inspect the exact CI APK's package, version, label, digest, and signer; install and cold-launch the same bytes before giving the reviewer a verified direct link.

**Verification:**

```text
.\gradlew.bat --no-daemon --no-parallel --dependency-verification strict checkNoDynamicVersions lint assembleDevDebug assembleStagingRelease assembleProdRelease :app:assembleDevDebugAndroidTest :core:database:assembleDebugAndroidTest :core:content-android:assembleDebugAndroidTest
python -m unittest scripts/test_verify_android_policy.py
python scripts/verify_android_policy.py
Expected: every applicable gate exits zero and the APK reports io.github.gilnetizen.aseh.dev.alpha8.debug, versionCode 8, versionName 0.1.0-alpha.8-dev-debug, and label ASEH alpha 8 review.
```

## Progress

- [x] 2026-10-07 — Audited the attached specification and ExecPlan 0006 gaps against the alpha.8 request.
- [x] 2026-10-07 — Added workspace rename and bounded Household/Qahal CRUD with validation, nested records, confirmation, and latest-snapshot merging.
- [x] 2026-10-07 — Added local workspace indexing and exact Build deep links with context switching, scrolling, focus, and highlight.
- [x] 2026-10-07 — Added `ServiceInstanceKey`, occurrence-keyed repository operations, service-state projection, stale-write guards, and explicit unsupported selection UI.
- [x] 2026-10-07 — Added the operational database v3-to-v4 service-occurrence migration and schema evidence, including an explicit legacy date-only identity.
- [x] 2026-10-07 — Kept Practice browsing/source/save behavior available before setup while gating step progress to a supported occurrence.
- [x] 2026-10-07 — Updated the review package identity to version code 8, `0.1.0-alpha.8`, and `io.github.gilnetizen.aseh.dev.alpha8.debug`.
- [x] 2026-10-07 — Local pre-correction validation recorded successful targeted tests, lint/assembly, API 26 app `48/48` plus database `15/15`, and API 37 app `49/49` at 200% text plus database `15/15`.
- [x] 2026-10-07 — Architecture review found that full editable workspace snapshots in SharedPreferences conflict with ADR-0003 once the records are user-created rather than fixture-only.
- [x] 2026-10-07 — Added the v5 operational Room workspace store, v4-to-v5 migration, one-time SharedPreferences import, retained unreadable input, migrated-key removal, and deletion wiring to the working tree.
- [ ] 2026-10-07 — Complete review and targeted verification of the Room workspace store, wrapper/fixture compatibility checks, import/quarantine recovery, deletion integration, and exported schema.
- [ ] 2026-10-07 — Rerun targeted and full gates after the persistence correction, including exact final navigation instrumentation and Linux CI.
- [ ] 2026-10-07 — Inspect, install, cold-launch, and stage the exact CI-built alpha.8 APK at a mobile-accessible link.
- [ ] 2026-10-07 — Obtain Gil's review of the concrete artifact and PR; retain manual TalkBack and Android print/save-to-PDF review as explicit evidence if not completed.

## Decisions

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| 2026-10-07 | Use record-specific workspace commands applied to the latest snapshot. | Editors own only their fields; lifecycle transitions and nested changes made while a form is open must survive. | Implementation detail under [ADR-0003](../adr/0003-database-and-data-class-separation.md) |
| 2026-10-07 | Treat editable workspace records as operational user data and move their persisted current/backup/quarantine payloads into Room. | Alpha.8 changes the surface from a synthetic fixture rehearsal to user-created organizational records; SharedPreferences is no longer an allowed owner. | [ADR-0003](../adr/0003-database-and-data-class-separation.md) |
| 2026-10-07 | Retain a strict versioned codec at the app repository boundary and use SharedPreferences only as a one-time legacy input. | Exact decoding, validation, recovery, and quarantine preserve early review data while Room owns the final operational lifecycle. | [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0006](../adr/0006-manual-app-composition.md) |
| 2026-10-07 | Identify rehearsal state by occurrence ID plus civil date. | Date alone collides when multiple services or Israel/diaspora opinion profiles share a day. | Implementation detail under [ADR-0003](../adr/0003-database-and-data-class-separation.md) |
| 2026-10-07 | Preserve v3 date-scoped state under `legacy-date:<date>` rather than assigning it to a current schedule entry. | Migration must not invent provenance or silently bind old progress to a service/profile the user did not select. | [ADR-0003](../adr/0003-database-and-data-class-separation.md) |
| 2026-10-07 | Allow only the exact installed synthetic Shabbat-morning shape to enter the rehearsal. | Other schedule entries lack conductable reviewed content; a fallback conductor would misrepresent context. | [PRODUCT.md](../PRODUCT.md), [METHOD.md](../METHOD.md) |
| 2026-10-07 | Keep saved Practice cards global but scope step progress to a supported occurrence. | Reading and saving a guide is useful before setup, while completion contributes to a particular service's readiness. | Product-level behavior within this alpha delta |
| 2026-10-07 | Rebuild workspace search in memory from approved non-sensitive fields. | Search remains offline and current without creating another durable plaintext index or including sensitive narratives. | [ADR-0003](../adr/0003-database-and-data-class-separation.md) |

## Surprises and discoveries

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| 2026-10-07 | ExecPlan 0006 intentionally described workspace dashboards as fixture-backed lifecycle demonstrations. Alpha.8 creation/editing changes their persistence classification. | ExecPlan 0006 residual `Workspace gap`; alpha.8 `WorkspaceEditCommands` and editors | Added the mandatory Room persistence milestone rather than documenting a preference-store exception. |
| 2026-10-07 | A civil date cannot uniquely identify a service occurrence or opinion profile. | Service catalog can emit multiple entries and separate Israel/diaspora profiles for a date. | Added stable occurrence ID to the key and every occurrence-owned table/operation. |
| 2026-10-07 | Selecting an unsupported service previously risked showing or mutating the fixed Shabbat rehearsal. | Composition-root selection and feature gating audit | Unsupported entries now retain metadata but fail closed before conductor, packet, assignments, or progress writes. |
| 2026-10-07 | The schedule preview limit can hide a persisted selected occurrence. | `visibleScheduledServices` regression case | Always append the selected entry before de-duplication. |
| 2026-10-07 | The aggregate Windows JVM `test` task can fail two AndroidX DataStore host tests during atomic replacement even when targeted suites and connected equivalents pass. | `FileStorage.kt:114` in `LegacyExperiencePreferencesTest.cleanupRemovesOnlyMigratedOperationalKeys` and `LocalUserDataDeletionTest.deleteAllClearsSavedPlaceExperienceAndInterfacePreferences` | Keep the exact failures visible; Linux CI is the deciding host check and the Room correction requires fresh targeted/deletion coverage. |
| 2026-10-07 | Large-text exact-record navigation requires scrolling the outer Build list until the workspace content is composed before focusing the nested record. | API 37 200% instrumentation | Keep the outer pre-scroll and exact-record instrumentation in final regression coverage. |

## Verification and acceptance

Local evidence recorded before the mandatory workspace-persistence correction is useful regression evidence, not final-source acceptance:

| Gate | Recorded result | What it establishes | Remaining limitation |
|---|---|---|---|
| Targeted model/database/service/workspace/app tests | Passed | Command merging, occurrence projections, scheduling, persistence operations, and search behavior had unit coverage on the pre-correction tree. | Must rerun after Room v5 lands. |
| Broad build without aggregate JVM tests | 999 tasks passed for dependency policy, lint, all three app flavors, and Android-test APK assembly | Changed modules and flavor isolation compiled and linted. | Exact final source and CI remain pending. |
| Android/repository policy | Policy unit tests `16/16`; source, merged-manifest, resolved-dependency, secret/history, file-size, shell syntax, and whitespace checks passed in local review | No observed policy, dependency, manifest, secret, or repository artifact violation in the reviewed pre-correction tree. | Stage and scan the final source again. |
| API 26 connected tests | App `48/48`; operational database `15/15` | Workspace, occurrence, migration, navigation, and deletion flows passed on the minimum API before the Room v5 correction. | Final exact-commit run is required. |
| API 37 connected tests at 200% text | App `49/49`; operational database `15/15` | The integrated UI and database flows passed on the target API with expanded text before the Room v5 correction. | Final exact-commit run and manual accessibility review are required. |
| Local APK inspection | Package `io.github.gilnetizen.aseh.dev.alpha8.debug`; code `8`; name `0.1.0-alpha.8-dev-debug`; label `ASEH alpha 8 review` | The local pre-correction candidate had the intended identity. | Final CI bytes will have a separate digest and likely signer. |

Final acceptance requires:

- schema-version-5 migration tests from every shipped operational schema, including service occurrence and workspace legacy import;
- current/backup recovery, malformed/version-unknown quarantine, write blocking, explicit clear, migrated-key removal, and process-restart evidence;
- targeted unit tests and the broad Gradle/policy gates on the settled final tree;
- full API 26 and API 37 app and database suites, with API 37 at 200% font scale;
- airplane-mode workspace CRUD/search and supported/unsupported occurrence switching;
- Hebrew-locale RTL fallback and mixed-script form/search checks without claiming reviewed Hebrew;
- TalkBack names, roles, states, focus order, validation announcements, confirmation dialogs, and disabled Practice-step explanation;
- exact CI APK package/version/label, SHA-256, signer, source commit, install, cold launch, and a verified mobile-accessible direct download; and
- human review of development disclosures, packet share/print, and the remaining release-policy gaps.

## Rollout and recovery

Alpha.8 remains a development-review build under the separate package `io.github.gilnetizen.aseh.dev.alpha8.debug`. It does not update alpha.7 or the production package and does not import their app-private data. Its debug signer is temporary; a differently signed rebuild with the same package can require uninstall and therefore erase that package's local state.

Within alpha.8, the operational database uses explicit forward migrations and no destructive fallback. Legacy date-only service progress is retained under a truthful legacy occurrence ID. The workspace correction imports the former alpha.8 preference payload only after strict decoding and domain validation. A successful Room transaction precedes removal of legacy keys. An unreadable payload is quarantined rather than discarded; mutations remain blocked until the user explicitly clears local data. The current/last-good pair supports recovery from one corrupt envelope without rewriting a valid older snapshot.

Selecting another service never deletes an occurrence. It changes the active projection while retaining previous occurrence rows. Unsupported entries do not become active rehearsal owners. Confirmed local-data deletion clears operational Room state, workspace recovery/quarantine rows, small service/interface preferences, and saved place context through their owning repositories. Files already shared, printed, or saved outside ASEH remain outside that deletion boundary.

If final review finds a regression, withdraw the review link and issue a newly versioned or clearly rebuilt development candidate. A public release would require protected-main review, project signing, a signed tag, complete release artifacts, approved content, and an owner-performed publication action under `RELEASE.md`; this debug APK is not a release rollback mechanism.

## Residual risks and handoff

- **Persistence correction:** The SharedPreferences-to-Room workspace correction and schema version 5 are present in the working tree but must pass migration/recovery/deletion and stored-version/fixture-compatibility tests before the branch is ready to merge. The plan cannot accept a preference-store exception to ADR-0003.
- **Content blockers:** `RIGHTS-001`, `RIGHTS-004`, and `LITURGY-001` still prevent distribution of the intended Rambam-based prayer content. The conductor proves operational mechanics while sacred text remains explicitly unavailable.
- **Editorial blockers:** Synthetic cards, assembly decisions, qahal records, and Hebrew fallback copy are not human-reviewed or Approved content.
- **Service coverage:** Only one synthetic Shabbat-morning shape is conductable. Weekday, afternoon, festival, weekly-reading, and arbitrary installed-service conductors remain unavailable.
- **Workspace coverage:** Personal-practice creation, full charter authoring, care programs, education programs, document libraries, private evidence, and collaboration are outside this slice.
- **Calendar scope:** The service date and explicit Israel/diaspora profile drive the current narrow flow. ADR-0008 remains Proposed, and alpha.8 does not establish all calendar additions, readings, sunset transitions, or opinion rules.
- **Language and accessibility:** Hebrew remains an RTL English fallback, and manual Hebrew-reader, TalkBack, contrast, keyboard/switch, phone/tablet, and print/save-to-PDF acceptance are incomplete unless separately recorded.
- **Provider scope:** Ask ASEH remains a deterministic synthetic development provider. No real model, BYOK consent, or complete high-consequence language matrix is present.
- **Host-test issue:** Two aggregate Windows DataStore tests have failed in AndroidX atomic file replacement. Record the exact final Linux CI result and do not convert that host-specific observation into an unqualified pass.
- **Artifact and publication:** Local APK identity is not the final CI artifact. Exact final bytes require CI inspection and device installation. Protected-main merge, signed release artifacts, and public publication remain human actions.

Handoff must include this plan, ExecPlan 0006, the alpha.8 review guide, final schema snapshots and migration evidence, exact commands and results, APK provenance, direct-link verification, and an explicit list of anything skipped or failed.

## Outcomes

The alpha.8 product delta is substantially implemented: bounded Household and Qahal records are editable, local search opens exact records, and service selection now controls occurrence-specific rehearsal context with explicit unsupported states. Practice remains useful before setup, and occurrence-owned writes fail closed when the visible selection cannot support the rehearsal.

This plan remains Active because architecture review found and required correction of the workspace persistence boundary. Final closure requires the version-5 operational Room store and legacy import/quarantine path, rerun exact-source verification and CI, inspection and installation of the exact APK, and Gil's review. Closure will describe a durable offline operational demonstrator with synthetic content and explicit missing sacred text, not a complete or approved Shabbat service or public ASEH release.
