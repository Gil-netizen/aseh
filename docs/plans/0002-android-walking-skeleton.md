# ExecPlan 0002: Android RTL/LTR walking skeleton

**Status:** Active\
**Owner:** Gil\
**Started:** 2026-10-06\
**Last updated:** 2026-10-06\
**Issue/PR:** [GitHub issue #3](https://github.com/Gil-netizen/aseh/issues/3)\
**Related ADRs:** [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0005](../adr/0005-pinned-android-toolchain.md), [proposed ADR-0006](../adr/0006-manual-app-composition.md)

This plan implements specification Step 2 as one reviewable pull request. It
starts from the Phase 0 merge on protected `main` and does not begin content or
feature implementation.

## Outcome

A clean checkout builds an offline Android application shell that launches in
Hebrew RTL and English LTR, exposes the five required destinations, preserves a
small non-sensitive preference, opens the operational Room boundary, and
produces reproducible build and test evidence in CI. The shell contains only
synthetic construction-state copy.

## Success criteria

- The pinned JDK, Gradle, AGP, built-in Kotlin, Compose, Android SDK, and
  dependency set resolves without dynamic or preview selectors.
- `dev`, `staging`, and `prod` variants build; unsigned staging and production
  release-like artifacts and a dev debug APK are retained as evidence.
- The five destinations appear in the exact product order: Now, Practice,
  Pray, Study, Build. Selection survives activity recreation, and the backing
  preference survives a separately constructed repository over the same
  DataStore file. Full process-death restoration remains manual evidence.
- English is LTR and the Hebrew locale is RTL, including intentional navigation
  mirroring, mixed-script isolation, tab semantics, logical visual order, 48 dp
  touch targets, and tablet adaptation. API 37 captures the layout at 200% font
  scale; human TalkBack, keyboard, and visual review remains required.
- The operational Room database exports schema version 1 and passes its opening
  and migration harness. DataStore preferences survive repository recreation.
- The merged manifest has no `INTERNET` permission and the dependency graph has
  no networking, authentication, analytics, advertising, AI, or remote-media
  client.
- Source and merged manifests keep backup disabled with deny-all backup rules;
  static policy rejects SQLite `ATTACH`, feature persistence imports, and direct
  feature dependencies on `core:database`.
- JVM, lint, assembly, dependency verification, API 26/API 37 smoke tests, and
  deterministic UI assertions pass. Instrumented tests capture synthetic
  English-LTR and Hebrew-locale RTL PNG evidence.

## Canonical requirements

- `AGENTS.md`
- `docs/PRODUCT.md`
- `docs/PRIVACY_MODEL.md`
- `docs/THREAT_MODEL.md`
- `docs/ACCESSIBILITY.md`
- `docs/RELEASE.md`
- `docs/adr/0003-database-and-data-class-separation.md`
- `docs/adr/0005-pinned-android-toolchain.md`
- specification sections 14.1-14.2, 19 Step 2, and 21

No conflict was found. The specification's complete future module graph is a
target architecture. Issue #3 deliberately creates only modules exercised by
the walking skeleton, avoiding empty modules that would claim unimplemented
boundaries.

## Scope and non-goals

**In scope**

- `:app`, `:core:database`, `:core:designsystem`, `:core:ui`, `:core:testing`,
  and the five feature modules named in issue #3.
- Exact toolchain pins, Gradle wrapper, version catalog, dependency locking and
  verification metadata.
- Manual constructor injection from an application-owned composition root.
- Five locale-aware English placeholder destinations, visibly labeled English
  fallbacks in the Hebrew locale, adaptive navigation, accessibility semantics,
  and synthetic visual evidence.
- Operational Room schema version 1, DataStore UI preferences, tests, and CI.

**Out of scope**

- Corpus text, translations, citations, practice rulings, prayer content,
  calendar or zmanim calculations, search, content packs, and editorial tools.
- Sensitive-case persistence, cryptography, export, provider credentials,
  networking, accounts, authentication, analytics, crash reporting, or AI.
- Production signing, signed tags, store submission, GitHub prerelease, or
  public deployment. Those require completed product features and the release
  gates in `docs/RELEASE.md`.
- Empty modules for later data, domain, security, settings, and tool work.

## Current state and context

Phase 0 merged at `1e36f2f8c89d4af3c5ca415a9a499c0bc61850d2`.
There was no Android project before this branch. The local workstation has the
accepted Temurin JDK and API 37.0 SDK packages, but the active shell must set
`JAVA_HOME` explicitly. Emulator images are installed or provisioned only for
connected verification; their absence must fail that verification rather than
silently change the API matrix.

The operational store is writable non-sensitive app state. This plan stores
only installed-pack catalog metadata there, without a pack payload. DataStore
contains only UI preferences. Feature modules have no dependency on either
storage implementation.

## Design and interfaces

`app` is the only composition root. It constructs the operational database and
preference repository, owns top-level state, and invokes feature composables.
An application-owned coroutine scope serializes and conflates destination
writes so activity recreation cannot cancel a selection and the latest rapid
selection wins.
The feature modules depend only on `core:ui`. The application also uses
`core:designsystem` to apply the shared theme. Features expose placeholder
screens and resources; they cannot receive a DAO, database, path, or generic
serialization surface.

`core:ui` defines the closed five-value destination registry and an adaptive
navigation scaffold. Phone layouts use bottom navigation and tablet widths use
a navigation rail. Android maps the modern `he` language tag to its legacy
`values-iw` resource qualifier; logical start/end primitives and Compose layout
direction perform the mirror. Synthetic mixed-direction text uses Unicode
isolation.

`core:database` owns `OperationalDatabase`, its internal DAO, the database
filename, Room schema exports, and a small `InterfacePreferencesRepository`. It does
not create a corpus database or sensitive store. Destructive fallback is not
configured. Android backup rules are fail-closed for this scaffold.

The project uses exact dependencies and strict SHA-256 verification metadata.
No Android module applies `org.jetbrains.kotlin.android`; AGP 9 built-in Kotlin
provides Kotlin 2.2.10. Compose modules apply only the matching Compose compiler
plugin. Room compilation uses KSP.

## Milestones

### 1. Reproducible Android build

**Result:** The complete module graph syncs and produces dev debug, staging
release-like, and production release-like APKs from the accepted toolchain.

**Work:**

- Commit Gradle 9.6.0 wrapper and checksum, exact version catalog, SDK/JVM DSL,
  flavors, locks, and dependency verification metadata.
- Add proposed ADR-0006 for the manual composition root.

**Verification:**

```text
./gradlew --version
./gradlew help
./gradlew assembleDevDebug assembleStagingRelease assembleProdRelease
Expected: Gradle 9.6.0 on JDK 17 and all three APK tasks succeed.
```

### 2. Offline locale-aware shell and local foundations

**Result:** The app renders all five English placeholders in English LTR and
visibly labeled English fallbacks in Hebrew-locale RTL, and persists
non-sensitive UI state without network access.

**Work:**

- Implement the design-system seed, adaptive shell, feature placeholders,
  composition root, Room operational catalog, and DataStore preferences.
- Add locale, backup, extraction, and cleartext-denial configuration.

**Verification:**

```text
./gradlew test lint
Expected: unit tests and Android lint pass with no INTERNET permission.
```

### 3. Device and CI evidence

**Result:** Fixed API 26 and API 37 emulators launch and navigate the app; CI
retains tests, lint, APKs, and synthetic screenshots.

**Work:**

- Add stable Compose instrumentation tests for LTR/RTL mirroring, semantics,
  large text, tablet layout, persistence, and screenshots.
- Add pinned-action CI and explicit SDK/emulator provisioning.

**Verification:**

```text
bash scripts/run_android_emulator_tests.sh <emulator-label>
Expected: app and database connected suites plus the offline launcher smoke
pass on each fixed runner and emit English-LTR/Hebrew-locale RTL PNG evidence.
```

## Progress

- [x] 2026-10-06 00:59 +03:00 — Phase 0 merge verified and issue #3 created.
- [x] 2026-10-06 01:23 +03:00 — Branch and this active ExecPlan created from protected `main`.
- [x] 2026-10-06 02:35 +03:00 — Final strictly locked pinned build, tests, lint, and dev/staging/prod assemblies pass after lifecycle and rail review fixes.
- [x] 2026-10-06 02:35 +03:00 — Locale-aware UI, rapid-selection persistence, Room, DataStore, and offline-policy tests pass.
- [x] 2026-10-06 02:48 +03:00 — Final local API 26 phone and API 37 tablet device suites pass; API 37 also passes at 200% font scale.
- [x] 2026-10-06 02:56 +03:00 — ADR-0003 backup and persistence-boundary policy checks pass with 13 focused checker tests.
- [ ] 2026-10-06 02:48 +03:00 — GitHub API 26/API 37 CI matrix passes.
- [ ] 2026-10-06 01:23 +03:00 — Pull request contains final evidence and is ready for Gil's review.

## Decisions

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| 2026-10-06 | Use manual constructor injection from `app` | Keeps the offline skeleton explicit and avoids adopting an unreviewed DI framework | [ADR-0006](../adr/0006-manual-app-composition.md) |
| 2026-10-06 | Create only issue #3 modules | Every committed module has executable behavior or test support; later boundaries remain deliberate follow-ups | none |
| 2026-10-06 | Capture screenshots with stable instrumented Compose APIs | The official host screenshot plugin is experimental and conflicts with stable-only bootstrap pins | none |

## Surprises and discoveries

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| 2026-10-06 | Stable API 37 is installed as minor package `platforms;android-37.0` | `sdkmanager --list` and ADR-0005 | Use the AGP `compileSdk` minor API DSL and exact CI package name. |
| 2026-10-06 | The active terminal did not inherit machine `JAVA_HOME` | `java` was initially unavailable; the accepted JDK exists at the recorded workstation path | Set environment only in local commands; do not commit a workstation path. |
| 2026-10-06 | GitHub CLI is absent | `gh auth status` returns command-not-found | Use the connected GitHub/browser interface for issue and PR operations. |
| 2026-10-06 | KSP 2.2.10-2.0.2 configures generated code through forbidden `kotlin.sourceSets` under AGP 9.4 built-in Kotlin | `gradle wrapper` failed while configuring `:core:database`; Google's AGP 9 migration guidance requires KSP 2.3.6 or later | Pin stable KSP 2.3.12; retain built-in Kotlin 2.2.10 and do not suppress the AGP check. |
| 2026-10-06 | AndroidX TestStorage requires `test-services` 1.6.0 plus the `useTestStorageService` runner argument | Device tests wrote PNGs that AGP did not initially collect | Wire the stable TestStorage service and fail CI when either PNG is missing or empty. |
| 2026-10-06 | Locale changes recreate the test activity | The first RTL click could target the stale pre-change activity | Wait for the new activity instance before interacting with the RTL semantics tree. |
| 2026-10-06 | Windows connected tests require the exact host TestStorage output directory to exist before the pull step | API 26 produced screenshots on-device but the host pull failed until the directory was created | Clear stale output and precreate the AVD/release-specific directory in the emulator script. |
| 2026-10-06 | Git Bash does not discover `sdkmanager.bat` and rewrites `/sdcard` arguments passed to native `adb.exe` | The first final Windows runner invocation could not find `sdkmanager`; the next converted the hierarchy pull path | Resolve Windows SDK executable suffixes and disable MSYS argument conversion without changing the Linux path. |
| 2026-10-06 | The launcher smoke step leaves the dev APK installed and the Android test engine can report a successful Gradle task when its replacement install fails | A repeated API 26 run emitted `INSTALL_FAILED_ALREADY_EXISTS` and produced no required screenshots | Uninstall the exact dev application before instrumentation and retain the fail-closed screenshot requirement. |
| 2026-10-06 | Strict dependency locks and `failOnNonReproducibleResolution()` are mutually exclusive in Gradle 9.6 | The first strict build failed before resolution | Keep strict locks and the catalog selector gate; remove the conflicting resolution strategy. |

## Verification and acceptance

Final evidence records exact command output for wrapper checksum, environment
diagnostics, dependency verification, locks, unit tests, lint, debug and
release-like assemblies, merged-manifest inspection, schema exports, and API
26/API 37 connected tests. Instrumented screenshots use only synthetic content
and cover English LTR, Hebrew-locale RTL, phone and tablet navigation, 200% font
scale, and mixed script. Automated checks cover tab roles, reachability, logical
visual order, minimum target size, mirroring, and persistence. A human
Hebrew/English visual, TalkBack, keyboard, and full process-death review remains
a PR review item; automation cannot grant it.

## Rollout and recovery

This scaffold is not published. Its release build is unsigned. Reviewers can
delete app data or uninstall it without migration impact because no user-facing
feature exists. A failed schema change must be fixed forward with an explicit
Room migration; destructive fallback remains disabled.

## Residual risks and handoff

- Emulator availability can prevent local connected evidence; fixed CI images
  remain required before merge.
- Automated bounds and semantics assertions do not replace human Hebrew,
  English, TalkBack, keyboard, and 200% visual review.
- Proposed ADR-0006 requires Gil's human review in the implementing PR before
  it becomes Accepted.
- `RIGHTS-004` remains open and blocks distributable corpus content; it does not
  block this synthetic no-content scaffold.

## Outcomes

The strictly verified local build, unit tests, lint, all three requested APK
variants, and both fixed local emulator profiles pass. The implementation is
ready for GitHub CI and human accessibility/ADR review; those results will be
recorded in the pull request before this plan is marked complete.
