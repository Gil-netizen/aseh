# ExecPlan 0006: Functional Shabbat rehearsal

**Status:** Implementation, settled-tree verification, and CI artifact staging complete; exact CI-byte and manual review in progress\
**Owner:** Implementation: Codex; product acceptance and release: Gil\
**Started:** 2026-10-06\
**Last updated:** 2026-10-07\
**Issue/PR:** [PR #11](https://github.com/Gil-netizen/aseh/pull/11); working branch `codex/shabbat-rehearsal-alpha7`, based on `50a65a5`\
**Related ADRs:** [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0006](../adr/0006-manual-app-composition.md), [ADR-0007](../adr/0007-foreground-location-and-local-calendar.md)

This file is the living implementation record for the alpha.7 functional-rehearsal slice. Keep `Progress`, `Decisions`, `Surprises and discoveries`, and `Outcomes` current. A contributor must be able to distinguish implemented behavior, verified behavior, and intended behavior without relying on chat history.

## Outcome

The `devDebug` application provides a coherent, offline, phone-testable rehearsal for a small emerging qahal. A user can establish date and place context, prepare from actionable synthetic guides, configure service and access settings, assign required roles and eight Torah-reading handoffs, inspect deterministic include/omit/reorder decisions, run an operational service conductor, inspect exact synthetic source records, record a local choice and draft governance artifacts, ask the deterministic development provider a source-bound question, and review a structured packet before sharing or printing it. The development build also exposes distinct Self, Household, and Qahal workspace dashboards with due-soon work and lifecycle actions, plus a signed synthetic content pack that exercises compilation, installation, activation, and full-text search.

This is an operational rehearsal of product mechanics. It is not yet a complete Shabbat-morning service: no sacred prayer text, translation, transliteration, Torah passage, or Rambam-based liturgical reconstruction is bundled. The UI shows those fields as unavailable rather than filling them with invented or distribution-blocked text.

## Success criteria

- [x] Practice, Pray, Study, and Build expose connected tasks rather than static destination placeholders in the development flavor.
- [x] The workflow remains local-first and does not need an account, provider key, or network request.
- [x] Date, calendar region, place availability, preflight, required roles, eight reading assignments, device mode, and accessibility profile feed a typed deterministic assembly context.
- [x] The service exposes include, omit, and reorder reasons, readiness blockers and warnings, and an explicit development-preview override that does not mark missing work complete.
- [x] Progress, selections, assignments, bookmarks, local adoptions, and rehearsal state use local persistence and have repository tests.
- [x] Every bundled development content record is synthetic and visibly non-normative; staging and production receive no development catalog.
- [x] The source-bound Study flow uses a disclosed deterministic fake provider, exact installed-text checks, unsupported-claim rejection, and a case-preparation route for detected high-consequence questions.
- [x] Packet export requires a preview before share or print and presents structured text as the accessible equivalent of visual print output.
- [x] Self, Household, and Qahal development workspace dashboards expose actionable due work, lifecycle transitions, persisted command replay, and a context-filtered Now summary.
- [x] The development flavor installs and activates a deterministically compiled, Ed25519-signed synthetic `.asehpack`; API 37 verifies and searches it, while API 26 fails closed with an explicit unsupported message.
- [x] The settled app instrumentation suite passes on API 26 (`43/43`) and API 37 (`44/44`) with the API 37 emulator at 200% font scale.
- [x] The settled working tree passes clean dependency/unit checks (354 tasks), lint (428 tasks), all three flavor and test-APK assemblies (790 tasks), Android policy, resolved-dependency/merged-manifest policy, staged secret/size checks, and cached-diff whitespace validation.
- [ ] Final phone and tablet evidence covers English LTR, Hebrew-locale RTL fallback, mixed script, 200% text, TalkBack, GPS permission/recovery, process restart, packet share, and Android print/PDF output. Automated semantics, workflow, and 200% layout evidence exists; manual TalkBack and Android print/save-to-PDF review remains outstanding.
- [x] A newly built CI APK has the current package identity, recorded checksum and signer, and an authenticated mobile review location.
- [ ] Install and cold-launch the exact CI-built APK bytes on API 26 and API 37; the locally verified APK has the same source and identity but a different ephemeral debug signature.
- [ ] Gil reviews the concrete build and decides whether it should proceed to a pull request or review distribution. This plan does not authorize merge or publication.

## Canonical requirements

- [Repository instructions](../../AGENTS.md) require a current ExecPlan, protected-main pull requests, verification evidence, and human approval.
- [PRODUCT.md](../PRODUCT.md) defines the five destinations, offline core, public-alpha Shabbat demonstrator, source explanations, role views, reading assignments, and separate local adoption.
- [METHOD.md](../METHOD.md) requires five-layer separation, visible disagreement and unknowns, deterministic traces, immutable citations, and case preparation rather than direct high-consequence answers.
- [EDITORIAL_POLICY.md](../EDITORIAL_POLICY.md) limits generated work to pre-approval states and requires named human review before stable content.
- [SOURCE_POLICY.md](../SOURCE_POLICY.md) requires exact editions, provenance, rights metadata, and fail-closed corpus behavior.
- [DATA_LICENSES.md](../DATA_LICENSES.md) records pilot source identities as conditional and keeps the narrow Rambam pilot out of distributable packs pending human rights review.
- [PRIVACY_MODEL.md](../PRIVACY_MODEL.md) and [THREAT_MODEL.md](../THREAT_MODEL.md) require local-first storage, deliberate export, data minimization, and no secret leakage.
- [ACCESSIBILITY.md](../ACCESSIBILITY.md) requires Hebrew RTL, English LTR, mixed-script safety, TalkBack, 200% text, equal-status movement alternatives, and an accessible packet representation.
- [RELEASE.md](../RELEASE.md) distinguishes development builds from public releases and requires signed artifacts, provenance, installation, accessibility, security, and human-publication evidence before deployment.
- [OPEN_QUESTIONS.md](../OPEN_QUESTIONS.md) keeps `RIGHTS-001`, `RIGHTS-004`, `LITURGY-001`, `CONTENT-006`, and `RELEASE-001` open.
- The attached product/build specification defines the intended end-to-end demonstrator and explicitly calls for a fake local provider before any external adapter. It does not override repository policy or resolve open rights and editorial decisions.

If these requirements conflict, implementation stops and records the conflict. This plan cannot select a canonical liturgical edition, approve content, infer a license, or define a new communal rule.

## Scope and non-goals

**In scope**

- Preserve the GPS-first, locally calculated Hebrew-date and solar-time Now experience from ExecPlan 0005 and add direct entry into the rehearsal workflow.
- Add shared models for practice, service, sources, reading plans, workspace governance, disputed-practice records, assembly context, readiness, and packet generation.
- Persist non-sensitive operational rehearsal records in Room, keep only small interface/service preferences in DataStore, and provide a deliberate local-data deletion path across both stores.
- Bundle an original synthetic catalog only in `dev`; return no catalog from staging or production.
- Implement actionable Practice cards, preflight, role-specific Prayer views, a focused conductor, Build configuration, a source library, deterministic Ask ASEH behavior, and packet review/export.
- Represent missing liturgical and Torah-reading content explicitly, including the open rights and anchor decision where applicable.
- Prepare a review-only `0.1.0-alpha.7` development APK and concrete user walkthrough after final verification.

**Out of scope**

- Shipping a siddur, Torah-reading corpus, Rambam translation, reconstruction, normalization, transliteration, or religious ruling.
- Claiming that the synthetic running order is a valid service or that a local rehearsal choice has universal force.
- Resolving electricity and lighting beyond a synthetic unresolved dossier and separately recorded local adoption.
- Connected AI, BYOK setup, remote retrieval, accounts, telemetry, synchronization, LAN conductor sessions, or paid services.
- Production or staging content packs, production content-signing keys, a release AAB, production signing, a signed Git tag, or a public GitHub prerelease.
- Full Hebrew translation. Hebrew locale currently uses a clearly marked English fallback for unfinished screens.
- Merging to protected `main`, approving this work, or performing the owner's publication action.

## Current state and context

The branch began from `50a65a5`, which supplied the useful offline Now experience: foreground one-shot device location with confirmation, manual fallback, local Hebrew date, and sunrise/solar-noon/sunset calculations. Earlier review APKs exposed little else. User testing correctly identified that location and solar events alone did not exercise the product thesis.

The current working tree adds a multi-module rehearsal:

- `core/model` holds the development domain model, typed `ServiceAssemblyContext`, composition decisions, readiness evaluation, packet rendering, governance records, and reading-plan types.
- `core/database` provides `ExperienceStateRepository` over the operational Room database plus a narrow DataStore preference layer. Room owns progress, assignments, reading preparation, workspace, local choices, governance drafts, bookmarks, and rehearsal records; DataStore retains the selected role, calendar/access settings, and device mode.
- `app/.../WorkspaceReviewStateRepository.kt` replays typed lifecycle commands over a versioned Self/Household/Qahal fixture. This separate development review surface demonstrates workspace behavior without representing fixture records as user-created data.
- `app/src/dev/.../FlavorContentCatalog.kt` provides original synthetic practice, service, source, choice, dossier, and template fixtures. `app/src/staging` and `app/src/prod` expose no catalog.
- `feature/practice` renders actionable guides and step progress, local saving, exact synthetic source navigation, reasoning, uncertainty, and review metadata.
- `feature/prayer` renders readiness, preflight, role dashboards, assembled running order, focused conductor, reading handoffs, access alternatives, exact source links, explicit unavailable liturgical fields, and packet export.
- `feature/study` renders local source search/bookmarks and the deterministic fake-provider Ask ASEH boundary. In `dev`, it also exposes a deterministically compiled and signed synthetic pack through the real pack verifier, installer, activation repository, and FTS search path.
- `feature/build` provides separate Self, Household, and Qahal development dashboards with due-soon work and lifecycle actions, plus an explicit service-setup mode for calendar region, access preferences, role and reading plans, output mode, local teaching placement, a draft qahal charter, an unresolved lighting dossier with separate local adoption, packet export, and local-data deletion.
- `core/ui/PacketExportPreviewDialog.kt` provides a reusable disclosure and exact payload preview before share or print.
- `app/.../PacketPrintDocumentAdapter.kt` uses Android's print framework to print or save the structured packet as PDF.

Current source declares `versionCode 7`, base `versionName 0.1.0-alpha.7`, minimum Android API 26, target API 37, and a distinct dev suffix intended to resolve to `io.github.gilnetizen.aseh.dev.alpha7.debug`. The development label is `ASEH alpha 7 review`.

An earlier APK at `app/build/outputs/apk/dev/debug/app-dev-debug.apk` was rejected because its manifest reported the former package `io.github.gilnetizen.aseh.dev.debug`. The CI candidate built after the settled-tree gates reports `io.github.gilnetizen.aseh.dev.alpha7.debug`, version code `7`, version name `0.1.0-alpha.7-dev-debug`, and the recorded digest, size, signer, source revision, and authenticated draft-release URL.

## Design and interfaces

### End-to-end flow

Now supplies explicit date and place context and links to the next preparation task, Prayer, or Build. Practice stores completed synthetic preparation steps. Build supplies the remaining context and assignments. `serviceAssemblyContext(...)` combines those inputs, `assembleService(...)` evaluates each segment policy and local ordering choice, and `evaluateServiceReadiness(...)` returns stable blockers and warnings. Prayer either starts/resumes the focused conductor, directs the user to missing setup, or permits a clearly disclosed development preview whose reason remains recorded.

The assembler records each inclusion, omission, and teaching-placement decision with facts, explanation, and exact synthetic source references. Catalog order is stable. A local teaching-placement choice can move only the known optional teaching segment; an unavailable segment or anchor fails visibly rather than selecting another order. Access-profile selection chooses the equal-status movement alternative without removing the standard cue from inspection.

The dated weekday, Shabbat, and festival catalog is a planning surface only. Because no approved conductable liturgical content is installed, choosing one of those dated entries does not replace the fixed synthetic Shabbat-morning rehearsal in the conductor. The UI states this separation; occurrence-specific conductor assembly remains future work after the necessary content and policy decisions exist.

### Content and editorial boundary

The development catalog contains original operational prose, not sacred or historical source text. Each record names its synthetic edition, provenance, license, a `ConclusionStatus`, and a separate `EditorialReviewState`. Development fixtures may be `Unresolved`, `Editorial proposal`, or another METHOD conclusion while remaining only `Drafted`; missing provenance fails closed to `Unresolved` and `Proposed`. No fixture claims `Human reviewed` or `Approved`. Service segments that would ordinarily carry liturgical content instead carry typed unavailable fields and a provenance block naming `LITURGY-001` and the missing distribution approval. Reading slots define seven aliyot and maftir as operational assignments while declaring the passages unavailable. A user may record a manual local passage plan only with an explicit unverified override reason; the app does not turn that entry into installed evidence.

The qahal charter remains a local draft until required metadata is supplied and the user deliberately records adoption. The lighting dossier remains unresolved even when a community records a local option. Adoption never upgrades evidence or editorial status.

### Persistence, privacy, and deletion

The operational Room database is the local source of truth for non-sensitive rehearsal records. Small service preferences remain in DataStore, and a one-time idempotent migration imports the earlier development DataStore aggregate before removing only the migrated operational keys. The synthetic workspace dashboard uses a separate versioned SharedPreferences command log so accepted fixture actions survive recreation without mutating the fixture; corrupt or incompatible logs reset to the labeled fixture. Room schema changes use explicit migrations without destructive fallback. Device coordinates remain in the separate place repository. No account or remote store is introduced. Build exposes one confirmation-gated action that clears workspace commands, calendar/access settings, assignments, choices, governance records, progress, saved practices, bookmarks, and saved place context across the local stores. Shared, printed, or externally saved packets cannot be recalled and the UI says so.

### Study provider trust boundary

`InstalledCorpusRetriever`, `AskAsehProvider`, and `StudyClaimVerifier` are separate interfaces. The development implementation is `dev.fake.installed-corpus.v1`, disclosed as a fake, deterministic, offline, keyless provider. Exact excerpts must cite one retrieved source and match the installed `SourceUnit.body` exactly. Fabricated IDs or altered excerpts become visible `Not established` claims and lose citations. Prompt-like text in a question or source remains data.

The current local classifier routes recognized English high-consequence terms to an eight-section case-preparation response. It does not provide an operative answer. This is useful development coverage, not the release-complete classifier required by `RELEASE.md`; Hebrew, mixed-script, euphemism, multi-turn, quotation/import, ambiguity, and urgent-danger matrices remain required before a public provider release.

The signed development pack is likewise test data rather than approved corpus material. Its exact SHA-256 is `3ECFBE962044F7DE6D84528E0489C90EC33DAFBC1E8BDA59620452F2E98AD8DF`, and its manifest key ID is `21fe31dfa154a261626bf854046fd2271b7bed4b6abe45aa58877ef47f9721b9`. The corresponding RFC 8032 fixture private key exists only in `core/content` test sources; no development signing key or synthetic pack is shipped by staging or production. API 37 verifies the Ed25519 signature and searches English, Hebrew, and locator fields. API 26 reports that this verifier is unsupported and does not activate or search the pack.

### Export and accessibility boundary

Share and print begin with the same exact structured packet and require a disclosure/preview step before the Android chooser or print service opens. The structured text is the accessible equivalent for the visual PDF. The PDF adapter is not claimed to create a tagged PDF. Final review must verify reading order, selectable text, large type, TalkBack access to the preview, and actual Android print/save behavior.

### Compatibility and rollback

The alpha.7 development package is intentionally separate from the production ID and, after rebuild, from prior development review packages. This avoids an unexplained debug-signature update failure but also means previous prototype data does not migrate automatically. Debug signing is not a durable update identity. Public releases continue to require the project-owned signing identity and the release procedure in `RELEASE.md`.

## Milestones

### 1. Shared state and development catalog

**Result:** All functional destinations consume one typed state and one flavor-scoped synthetic catalog.

**Work:**

- Add `core:model` and the Room-backed experience repository with a narrow DataStore preference boundary.
- Add development-only content with stable IDs, provenance, review state, and exact cross-links.
- Keep staging and production catalog-free.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :core:model:test :core:database:testDebugUnitTest :app:testDevDebugUnitTest
Expected: all model, persistence, flavor-isolation, and packet tests pass.
```

### 2. Preparation, configuration, and persistence

**Result:** A user can complete practice steps, exercise lifecycle actions in a labeled synthetic Self/Household/Qahal workspace, configure the separately persisted service rehearsal, record access and calendar context, assign roles and eight readings, and recover that state after restart.

**Work:**

- Implement Practice detail and progress.
- Implement Build workspace-dashboard, service-setup, role, reading, access, output, charter, dossier, and deletion flows.
- Wire navigation and repository mutations in `AsehApp`.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :feature:practice:testDebugUnitTest :feature:build:testDebugUnitTest :app:compileDevDebugAndroidTestKotlin
Expected: focused tests pass and the end-to-end instrumentation workflow compiles.
```

### 3. Deterministic assembly and conductor

**Result:** Prayer shows readiness, explainable composition, role-specific cues, missing content, reading handoffs, and a resumable one-segment-at-a-time conductor.

**Work:**

- Assemble from typed context and local choice.
- Preserve every blocker during development override.
- Expose movement, voice, equal access alternatives, source trace, and unavailable liturgical fields.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :core:model:test :feature:prayer:testDebugUnitTest
Expected: context rule changes, blocker/override behavior, order changes, source traces, and conductor models pass.
```

### 4. Source-bound Study boundary

**Result:** Local search and Ask ASEH preserve the eight-section display, disclose the fake provider, verify exact claims, reject unsupported output, and use case preparation for detected high-consequence input. The development flavor also exercises a signed synthetic pack through the real install, activation, and FTS path where platform crypto support exists.

**Work:**

- Separate retrieval, provider, and verifier interfaces.
- Add deterministic fake-provider, prompt-injection, fabricated-citation, unsupported-claim, and high-consequence tests.
- Bundle only the public verification material and deterministic synthetic pack in `dev`; show a fail-closed unsupported state when Ed25519 is unavailable.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :feature:study:testDebugUnitTest :feature:study:lintDebug
Expected: unit and adversarial tests pass; Android lint reports no error.
```

### 5. Packet preview, share, print, and deletion

**Result:** A user reviews one structured packet before deliberate external export and can delete all retained rehearsal state.

**Work:**

- Generate the packet from assembled order and persisted assignments.
- Add accessible preview/disclosure, Android share, Android print/PDF, and deletion confirmation.

**Verification:**

```text
.\gradlew.bat --no-configuration-cache :feature:build:testDebugUnitTest :feature:prayer:testDebugUnitTest :app:assembleDevDebug
Expected: packet and preview tests pass and a fresh devDebug APK is produced.
```

Manual evidence must additionally show cancel, share, print/save, and deletion behavior on a device. Compilation alone does not complete this milestone.

### 6. Integrated alpha.7 review candidate

**Result:** A newly built artifact with the current package identity is installable and the complete workflow is reviewable on representative devices.

**Work:**

- Run the final clean gates after all shared-file edits stop.
- Verify the APK manifest, digest, and package label.
- Exercise API 26 and API 37 phone/tablet, offline, RTL/LTR, 200% text, TalkBack, print, restart, deletion, and uninstall scenarios.
- Stage the exact reviewed APK at a mobile-accessible location only after evidence passes.

**Verification:**

```text
.\gradlew.bat --no-daemon --no-parallel --dependency-verification strict clean checkNoDynamicVersions test lint assembleDevDebug :app:assembleDevDebugAndroidTest
python -m unittest scripts/test_verify_android_policy.py
python scripts/verify_android_policy.py
Expected: every command exits zero and the newly built APK reports io.github.gilnetizen.aseh.dev.alpha7.debug, versionCode 7, and versionName 0.1.0-alpha.7-dev-debug.
```

## Progress

- [x] 2026-10-06 — User testing established that a date-and-solar-events shell did not satisfy the requested application and that manual coordinates could not be the primary mobile flow.
- [x] 2026-10-06 — Created branch `codex/shabbat-rehearsal-alpha7` from merged device-location work at `50a65a5` and began this plan.
- [x] 2026-10-06 — Added shared models, persistent experience state, development-only catalog, and connected Practice, Prayer, Study, and Build screens.
- [x] 2026-10-06 — Added GPS-first entry from Now while preserving manual location as an advanced fallback.
- [x] 2026-10-06 — Added focused conductor, role views, preflight, eight reading assignments, local teaching placement, packet generation, share, and Android print integration.
- [x] 2026-10-07 — Added typed service context, conditional include/omit/reorder traces, readiness blockers/warnings, and explicit development override.
- [x] 2026-10-07 — Added richer reading plans, explicit unavailable sacred content/provenance, qahal charter draft, unresolved lighting dossier, access profile, and local-data deletion.
- [x] 2026-10-07 — Added the deterministic fake-provider boundary, exact installed-source verification, unsupported-claim handling, and initial case-preparation routing.
- [x] 2026-10-07 — Added review-before-export UI and a structured-text accessible equivalent for packet output.
- [x] 2026-10-07 — Replaced the aggregate DataStore persistence with an ADR-0003-aligned Room operational database, retained small interface/service preferences in DataStore, added an idempotent one-time import, and made paired reading mutations transactional.
- [x] 2026-10-07 — Split conclusion status from editorial-review lifecycle status and made missing provenance fail closed without granting human review or approval.
- [x] 2026-10-07 — Isolated the deterministic fake Study provider to the `dev` flavor; staging and production compile without development provider or catalog classes.
- [x] 2026-10-07 — Expanded this ExecPlan and added the alpha.7 review guide without claiming a current candidate artifact.
- [x] 2026-10-07 — Rejected the superseded `io.github.gilnetizen.aseh.dev.debug` APK and removed it from candidate consideration.
- [x] 2026-10-07 — Captured representative phone screens and API 37 tablet screens at 200% text for Now, Practice, Prayer, Study, and Build.
- [x] 2026-10-07 — Added distinct Self, Household, and Qahal development dashboards with due-soon work, lifecycle actions, a versioned persisted command log, and context-filtered Now summaries. These dashboards use a labeled synthetic fixture; they do not claim general user-created workspace CRUD.
- [x] 2026-10-07 — Added a deterministic Ed25519-signed synthetic `.asehpack` to `dev`, exercised the real compile/verify/install/activate/FTS path on API 37, and made API 26 fail closed with an explicit unsupported state.
- [x] 2026-10-07 — Reran the complete settled-tree app instrumentation suite: API 26 passed `43/43`; API 37 passed `44/44` at 200% font scale.
- [x] 2026-10-07 — Passed clean `checkNoDynamicVersions test` (354 tasks), `lint` (428 tasks), and `assembleDevDebug assembleStagingRelease assembleProdRelease :app:assembleDevDebugAndroidTest :core:database:assembleDebugAndroidTest :core:content-android:assembleDebugAndroidTest` (790 tasks).
- [x] 2026-10-07 — Passed Android policy unit tests (`16/16`), source and merged-manifest/resolved-dependency policy verification, a 178-file staged forbidden-pattern scan, the staged 5 MB size limit, and `git diff --cached --check`.
- [x] 2026-10-07 — Installed the exact locally built `devDebug` APK and cold-launched it successfully on API 26 and API 37.
- [ ] Complete manual TalkBack and Android print/save-to-PDF review; automated semantics and 200% layout evidence do not substitute for these checks.
- [x] 2026-10-07 — Recorded the CI APK filename, package/version, size, SHA-256, v2 signer, source revision, and authenticated draft-release location after post-upload inspection.
- [x] 2026-10-07 — Opened [PR #11](https://github.com/Gil-netizen/aseh/pull/11) against `codex/7-manual-place-context` within the protected-main workflow.
- [ ] Obtain Gil's review of the concrete artifact and PR.

## Decisions

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| 2026-10-06 | Ship functional mechanics only through a development catalog. | It enables end-to-end testing without implying that synthetic content is approved prayer or practice. | None; development fixture boundary |
| 2026-10-06 | Keep sacred text and Torah passages explicitly unavailable. | `RIGHTS-001`, `RIGHTS-004`, and `LITURGY-001` remain open; filling gaps would invent an edition, license, or reconstruction. | None; canonical source policy controls |
| 2026-10-07 | Persist operational rehearsal records in Room, retain only small preferences in DataStore, and compose the repository in the application layer. | Adopted choices, review dates, public roles, schedules, checklists, and organizational records are operational state under ADR-0003; the split keeps feature modules behind the repository boundary from ADR-0006. | [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0006](../adr/0006-manual-app-composition.md) |
| 2026-10-06 | Keep one-shot GPS primary and manual coordinates secondary. | This follows direct mobile feedback and preserves local-only context behavior. | [ADR-0007](../adr/0007-foreground-location-and-local-calendar.md) |
| 2026-10-07 | Treat readiness blockers as retained facts; preview override permits rehearsal testing only. | A development escape hatch must not rewrite missing roles, readings, place, date, or preflight as complete. | None; production policy review still required |
| 2026-10-07 | Record local adoption separately from editorial evidence status. | METHOD forbids a community choice from upgrading historical evidence or editorial approval. | None; direct METHOD requirement |
| 2026-10-07 | Use a deterministic fake Ask ASEH provider before any real adapter. | The attached specification requires this sequence; it permits offline adversarial tests with no key or network. | None; provider ADR applies only when connected adapters are introduced |
| 2026-10-07 | Require exact packet preview before share or print. | External export is deliberate and irreversible from ASEH; users must see the payload first. | None; privacy/accessibility implementation |
| 2026-10-07 | Give alpha.7 review builds a distinct development package suffix. | Ephemeral debug certificates caused opaque update conflicts; a separate review package can install beside older prototypes. | None; not a production signing decision |
| 2026-10-07 | Exercise signed-pack mechanics only with a deterministic development fixture and fail closed where Ed25519 verification is unavailable. | This tests the real content pipeline without shipping an approved corpus, a runtime private key, or unverifiable content. | None; production key custody and content approval remain release decisions |
| 2026-10-07 | Keep dated service selection separate from the synthetic rehearsal conductor. | Planning metadata cannot truthfully become conductable prayer while the required liturgical corpus and anchor remain unresolved. | None; `RIGHTS-001`, `RIGHTS-004`, and `LITURGY-001` remain controlling |

## Surprises and discoveries

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| 2026-10-06 | Strong tests for a narrow shell did not make that shell a useful product. | Direct installed-app feedback; ExecPlan 0005 retrospective | Acceptance now requires an end-to-end user task, not screen or test counts alone. |
| 2026-10-06 | The repository has no distribution-approved liturgical corpus or selected anchor. | `DATA_LICENSES.md`; `RIGHTS-004`; `LITURGY-001` | The development build tests missing-content handling and operations; it cannot honestly claim a complete service. |
| 2026-10-06 | Device GPS and time zone cannot determine Israel/diaspora calendar policy. | Local location design and `CalendarRegion.UNSPECIFIED` readiness rule | Calendar region is a separate explicit setting in Build. |
| 2026-10-06 | Android print output is not automatically an accessible tagged PDF. | `PacketPrintDocumentAdapter.kt`; `ACCESSIBILITY.md` | Keep structured text as the accessible equivalent and require device review before release. |
| 2026-10-06 | A connected Gradle run exposed environment and test-assumption failures rather than a clean acceptance result. | Intermediate API 26 install/output failure; API 37 result 27/34 before subsequent focused fixes | Required a settled-tree rerun; the later definitive suites pass `43/43` and `44/44`. |
| 2026-10-07 | A deterministic exact-source fake can test provider boundaries without a real model. | `DeterministicStudyProvider.kt`; adversarial unit tests | Keep fake identity visible and defer connected adapters. |
| 2026-10-07 | A previous APK and current source disagreed about application ID. | `aapt dump badging` reported `io.github.gilnetizen.aseh.dev.debug`; current merged manifest/source expects `io.github.gilnetizen.aseh.dev.alpha7.debug` | The previous APK was rejected; accept only newly built and inspected bytes. |
| 2026-10-07 | Earlier full build evidence predates later readiness, governance, provider, export, and content-plumbing changes. | Command history and subsequent file timestamps | Both the connected suites and the split all-module Gradle gates were rerun against the settled tree and passed. |
| 2026-10-07 | Platform Ed25519 verification used by the pack path is available on the API 37 review device but not the API 26 implementation. | Focused signed-pack instrumentation on both emulators | API 26 shows an explicit unsupported state and never treats the pack as verified or active. |

## Verification and acceptance

### Passing checkpoints

These are real passing observations from the settled tree and staged CI candidate. The local build, lint, assembly, policy, scan, connected-device, and local APK install/cold-launch gates are complete. GitHub Actions built and staged a separately debug-signed candidate, then re-downloaded and inspected its exact bytes.

| Checkpoint | Result | What it proves | Limit |
|---|---|---|---|
| Clean dependency and unit gate | `clean checkNoDynamicVersions test`: passed, 354 tasks | The settled tree resolves pinned dependencies and passes unit tests from a clean state. | Does not replace connected-device or manual review. |
| Lint gate | `lint`: passed, 428 tasks | Android lint passes across the settled multi-module tree. | Manual accessibility and content review remain separate. |
| Flavor and test-APK assembly gate | `assembleDevDebug assembleStagingRelease assembleProdRelease :app:assembleDevDebugAndroidTest :core:database:assembleDebugAndroidTest :core:content-android:assembleDebugAndroidTest`: passed, 790 tasks | Development, staging, production, and required instrumentation APKs assemble from the settled tree. | Manual candidate review remains separate. |
| Focused model, database, app, Prayer, Study, and Build checks | Passed after the final functional changes | Typed assembly/status rules, Room persistence, transactional reading updates, atomic preference updates, fake-provider isolation, and app/test compilation have focused coverage. | Manual product and content review remain separate. |
| Room persistence checks | 21 JVM tests passed; 11 connected database tests passed on each API 26 and API 37 | Schema v2, v1-to-v2 migration, legacy import, transactions, deletion continuation, and persisted operational records behave at the repository boundary. | The definitive full-app suites are recorded separately below. |
| Android and repository policy checks | Policy unit tests `16/16`; source policy passed; merged-manifest/resolved-dependency policy passed | Both source and generated variant evidence satisfy the Android policy checks. | This does not grant content or release approval. |
| Staged repository checks | 178 files scanned with no forbidden secret patterns; no staged file exceeds 5 MB; `git diff --cached --check` passed | The staged change set passes the required secret-pattern, size, and whitespace checks. | Review is still required for semantic or editorial mistakes. |
| API 26 full app instrumentation, settled tree | `OK (43 tests)` | The complete app workflow, persistence, deletion, workspace repository behavior, and explicit unsupported signed-pack state pass on Android 8.0. | Does not substitute for manual TalkBack, print/save-to-PDF, or exact candidate installation review. |
| API 37 full app instrumentation, settled tree | `OK (44 tests)` at 200% font scale | The complete app workflow passes with expanded text, including Ed25519 pack verification, activation, and English/Hebrew search. | One additional API 34+ altitude test accounts for the count difference; manual accessibility and print review remain. |
| Signed development pack | API 37 verified/active and searchable; API 26 explicitly unsupported | The real compiler, signature verifier, installer, activation repository, and FTS path are exercised without a runtime private key. Pack SHA-256: `3ECFBE962044F7DE6D84528E0489C90EC33DAFBC1E8BDA59620452F2E98AD8DF`; key ID: `21fe31dfa154a261626bf854046fd2271b7bed4b6abe45aa58877ef47f9721b9`. | Synthetic `dev` fixture only; it is not approved liturgy or production content. |
| Exact local APK install and cold launch | Passed on API 26 and API 37 | The locally built `devDebug` APK installs and reaches a cold-launched app process on both review APIs. | The separately signed CI bytes still need device installation. |
| Exact CI candidate inspection | `ASEH-0.1.0-alpha.7-dev-debug.apk`; 15,477,164 bytes; SHA-256 `7c0bf0727f50b87dba2f6397ef974eb540e72bd69bfba26fb4c8e9cb09def192`; v2 signer `e86b941a3bce4c6f79f8cdf4c7af3952ff0a10dba299f3d44b98fe5ade80babe`; source `49c2aeedd5aa03e5d905590a636318d68f75f38b` | [Android CI](https://github.com/Gil-netizen/aseh/actions/runs/37565428658) build job passed; `android-apks` artifact `11459245329` (archive SHA-256 `ec68975b49a772121b2bf145715e3a2b215034499af2dd0e0b451f105e3c1ef5`) supplied the candidate, and the uploaded bytes were re-downloaded and inspected. | The unpublished draft link requires a signed-in GitHub account with repository access; exact CI-byte installation remains pending. |
| Representative visual evidence | Phone Now/Practice/Prayer/Study/Build and API 37 tablet Now/Prayer/Build at 200% text captured in `docs/evidence/alpha7/` | The implemented destinations render real workflow content across phone and expanded-text tablet surfaces. | Screenshots do not establish TalkBack behavior or actual print/save-to-PDF output. |

### Remaining verification

- Early API 26 installation/output and API 37 navigation failures were repaired. The final settled-tree suites now pass `43/43` on API 26 and `44/44` on API 37 at 200% font scale.
- The settled-tree Gradle, policy, staged scan, and local APK install/cold-launch gates are complete with the results above.
- Android packet preview/share/print code compiles and has unit/semantics coverage, but manual TalkBack review and an actual Android print-service/save-to-PDF walkthrough are not recorded. These remain explicit human-review residuals.
- Final CI candidate filename, size, package/version, SHA-256, signer fingerprint, source commit, and authenticated mobile download URL are recorded in the alpha.7 review guide. Exact CI-byte install evidence remains pending; local-build install/cold-launch evidence is complete.

### Remaining candidate and human evidence

- Install and cold-launch the separately signed CI-built APK on API 26 and API 37.
- Complete manual workflow, process death/restart, upgrade/reinstall expectations, local-data deletion, and uninstall review.
- Airplane-mode GPS/local calculations plus complete Practice → Build → Prayer → Study → packet flow.
- Phone and tablet evidence in English LTR and Hebrew-locale RTL fallback, mixed script, 200% font, TalkBack order/names/states, contrast, dark mode where supported, and touch targets.
- Packet preview cancel/confirm, share chooser, print service, save-to-PDF, selectable structured text, reading order, and disclosure behavior.
- Exact source navigation, unsupported Ask ASEH result, prompt injection, ordinary answer, and case-preparation UI.
- Manifest/package/version verification on the exact APK, SHA-256, signing-certificate status, source commit, and artifact filename.
- Human review of every visible development disclosure and confirmation that no bundled text is being treated as stable or approved.

## Rollout and recovery

Alpha.7 is first staged as a development review build. It must not be promoted to staging or production because those flavors have no approved content catalog. The intended review package is `io.github.gilnetizen.aseh.dev.alpha7.debug`; it installs separately from the production package and earlier development IDs. Its debug signature is temporary and does not establish upgrade continuity.

The exact locally built `devDebug` APK has been installed and cold-launched on API 26 and API 37. The CI artifact record now ties its manifest, digest, signer, source revision, and authenticated draft URL to one post-upload-inspected set of bytes. Its ephemeral signer differs from the local artifact, so exact CI-byte installation remains a review step. Never reuse the previously rejected bytes.

State recovery is intentionally modest. Room uses explicit schema migrations without destructive fallback, while the small DataStore preferences use additive defaults. The first Room-backed version imports any legacy development rehearsal aggregate once and keeps the import idempotent across interruption. The app offers confirmed local-data deletion across Room, preferences, and saved place context. Uninstalling the review package removes its internal state because backup is disabled. Users must export any packet they want to retain before deletion or uninstall. Externally shared, printed, or saved files remain outside ASEH and are not removed by app deletion.

If the review build is broken, remove it from the review instructions and issue a newly versioned or clearly rebuilt development candidate. Public release rollback follows `RELEASE.md` and normally uses a signed forward-fix; this debug package is not a public rollback mechanism.

## Residual risks and handoff

- **Blocking content gap:** `RIGHTS-001`, `RIGHTS-004`, and `LITURGY-001` prevent distribution of the intended Rambam-based liturgy. The app currently proves missing-content honesty, not prayer-text correctness.
- **Blocking editorial gap:** No synthetic practice card, running-order decision, charter, or dossier is human-reviewed stable content. Development status must remain visible.
- **Torah-reading gap:** Eight operational slots exist, but no calendar-linked portion or passage corpus is installed. Manual entries are local and explicitly unverified.
- **Workspace gap:** Self, Household, and Qahal dashboards currently replay a labeled synthetic development fixture through a persisted command log. They demonstrate due work and lifecycle behavior, but they do not yet provide general user-created workspace/item CRUD.
- **Service-occurrence gap:** Dated weekday, Shabbat, and festival entries are planning metadata only. Selecting an occurrence does not reconfigure the fixed synthetic rehearsal conductor.
- **Language gap:** English is the implemented content language. Hebrew locale provides RTL structure and an English fallback, not a reviewed Hebrew interface or Hebrew content.
- **Provider gap:** The fake provider is intentionally limited to exact tokens. The high-consequence classifier does not yet satisfy the release policy's Hebrew, mixed-script, euphemism, multi-turn, quoted/imported, ambiguity, or urgent-danger matrix.
- **Accessibility gap:** Automated semantics and representative 200% phone/tablet evidence exist, but manual TalkBack, contrast, and Android print/save-to-PDF review remain pending.
- **Artifact gap:** Local APK installation and cold launch pass on API 26 and API 37, and the CI candidate metadata and authenticated draft download are complete. The exact CI bytes still need device installation. This development APK uses a temporary debug signer and is not an AAB, signed public tag, SBOM, provenance bundle, or approved corpus release.
- **Device gap:** `RELEASE-001` remains open beyond API 26 and the current target. The final representative-device matrix needs human acceptance.
- **Policy gap:** The development override is acceptable only as a visibly disclosed rehearsal-preview tool. A production rule would require explicit product/editorial review.
- **Integration risk:** Multiple modules and persistence schemas changed in one working tree. The settled-tree clean, lint, assembly, policy, connected, and local install/cold-launch gates pass; manual end-to-end, TalkBack, and print-service review remain necessary.

Handoff should include this plan, [the alpha.7 review guide](../releases/0.1.0-alpha.7-review.md), the exact final command output, device reports, screenshots, APK digest, and a list of any deviations. No reviewer should infer completion from code volume or an APK filename.

## Outcomes

The implemented slice now demonstrates substantially more of ASEH's intended architecture than the earlier shell: context-aware offline assembly, preparation, role and reading coordination, equal-status access cues, source traces, local governance, an unresolved dossier with separate adoption, a source-bound fake provider, and previewed packet export. It also makes its most important absence visible: reviewed sacred content is not available.

Implementation of the alpha.7 operational rehearsal is complete and user-testable: Now supplies GPS-confirmed local context and due-work summaries; Practice records preparation; Build exposes the synthetic Self/Household/Qahal workspace rehearsal and the separately persisted service setup; Prayer exposes readiness, explainable assembly, roles, and conductor; Study provides local sources, a development-only verified fake provider, and a signed synthetic pack through the real install/search path. Operational service records live in Room, small interface/service preferences in DataStore, and workspace fixture actions in a versioned local command log.

This plan remains Active while exact CI-byte installation, manual TalkBack/print review, and Gil's artifact review are unresolved. The settled build and policy gates pass, the definitive connected results are `43/43` on API 26 and `44/44` on API 37 at 200% font scale, the exact locally built APK installs and cold-launches on both APIs, and the separately signed CI candidate is staged and inspected. Rights and editorial decisions also remain open, so closure would describe a verified operational development rehearsal with sacred text explicitly unavailable, not a complete or approved Shabbat service.
