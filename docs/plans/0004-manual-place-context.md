# ExecPlan 0004: Manual offline place context

**Status:** Active\
**Owner:** Android implementation\
**Started:** 2026-10-06\
**Last updated:** 2026-10-06\
**Issue/PR:** [Issue #7](https://github.com/Gil-netizen/aseh/issues/7)\
**Related ADRs:** [ADR-0003](../adr/0003-database-and-data-class-separation.md), [ADR-0006](../adr/0006-manual-app-composition.md)

## Outcome

The user can explicitly save one manual place on the Now screen. The app shows
its label, coordinates, optional elevation, and IANA time zone, and interprets
the civil snapshot in that selected zone. The context works offline and remains
after activity recreation and process restart until the user clears it.

## Success criteria

- Change context opens an editor for a label, latitude, longitude, optional
  elevation, and an IANA time zone selected from the device's offline zone list.
- Save is all-or-nothing. Invalid input stays in the editor with inline errors;
  Cancel leaves the stored context untouched; Clear removes it.
- The saved context is inspectable, persists locally, and changes the displayed
  civil date and time at zone boundaries without implying Hebrew dates, zmanim,
  a calendar profile, or an Israel/diaspora conclusion.
- The workflow has no network, account, telemetry, location permission, device
  positioning, geocoder, or map dependency.
- English LTR and labeled-English-fallback RTL layouts remain usable with
  TalkBack, keyboard input, and 200 percent text. Interactive targets are at
  least 48 dp.
- Deterministic JVM and instrumented tests use synthetic places only and pass on
  supported API 26 and target API 37 profiles.

## Canonical requirements

- `AGENTS.md`
- `PRODUCT.md`: Now context and offline-core requirements
- `PRIVACY_MODEL.md`: time and place context is calculated locally and persisted
  only when the user chooses
- `ACCESSIBILITY.md`: RTL/LTR, mixed-script direction, TalkBack order, scalable
  text, and touch targets
- `THREAT_MODEL.md`: data minimization and no unexplained network behavior
- `RELEASE.md`: versioning and review-artifact boundaries
- [ADR-0003](../adr/0003-database-and-data-class-separation.md): small,
  non-sensitive preferences belong in DataStore and feature modules do not
  receive persistence types
- [ADR-0006](../adr/0006-manual-app-composition.md): the application owns the
  repository and passes narrow state and callbacks to the feature

No conflict was found. The approved privacy inventory explicitly allows coarse
or precise place context to persist when the user chooses. The Save action is
that explicit choice.

## Scope and non-goals

**In scope**

- One manual place context with explicit Save, Cancel, and Clear actions.
- Local validation and an offline IANA-zone search dialog.
- Dedicated Preferences DataStore persistence owned by `core:database`.
- Civil Now interpretation using the saved zone.

**Out of scope**

- GPS, location permission, background location, place search, geocoding, maps,
  IP inference, or network calls.
- Multiple saved places, import/export, sync, accounts, or telemetry.
- Hebrew date, sunset boundary, zmanim, holiday, prayer, calendar-profile, or
  Israel/diaspora calculations. Those require reviewed methods and sources.

## Current state and context

Issue #5 replaced the Now placeholder with a deterministic offline civil
snapshot supplied by the application-owned clock and device-zone provider.
`feature:now` has no database dependency. `AppGraph` already owns the application
scope used by its DataStore adapters. This slice extends the same composition
boundary with a narrow manual-place repository and maps its model to the
feature's UI model inside `app`.

## Design and interfaces

`ManualPlaceContextRepository` exposes `Flow<ManualPlaceContext?>`, `save`, and
`clear`. Its dedicated Preferences DataStore writes the complete validated
record in one edit. Reads return `null` for incomplete, wrong-type, non-finite,
out-of-range, unknown-zone, or corrupt data. The model contains a trimmed,
non-blank label of at most 80 characters, finite latitude and longitude within
their inclusive geographic bounds, optional finite elevation, and an ID from
`ZoneId.getAvailableZoneIds()`.

`AppGraph` owns one repository instance. `AsehApp` collects its flow, distinguishes
the first load from a confirmed empty value, and explicitly maps between the database and feature
models. `NowScreen` receives only `NowPlaceContext?` and suspending save/clear
callbacks. Draft fields stay only in the current in-memory composition and are
discarded on activity or process recreation; no draft enters Android saved
instance state. The editor closes only after a complete repository operation
succeeds and keeps the draft visible with an announced error when a write fails.

Numeric and zone values force LTR presentation inside either layout direction;
the user-authored label follows the surrounding locale. The English-only alpha
keeps the visible `EN ·` marker in the Hebrew locale. A scrollable screen and
dialog list keep controls reachable at large text sizes.

## Milestones

### 1. Validated local persistence

**Result:** One complete manual place can be saved, observed, and cleared; bad
stored or proposed values do not produce a partial context.

**Verification:**

```text
.\gradlew.bat :core:database:testDebugUnitTest :core:database:lintDebug
Expected: repository validation, corruption, round-trip, and clear tests pass.
```

### 2. Accessible manual editor and live civil context

**Result:** The user edits the context on Now, chooses an offline time zone, and
sees the saved values and civil snapshot for that zone.

**Verification:**

```text
.\gradlew.bat :feature:now:testDebugUnitTest :app:connectedDevDebugAndroidTest
Expected: validation, callbacks, time-zone boundary, navigation, RTL, 200 percent
text, recreation, and persistence scenarios pass with synthetic data.
```

### 3. Repository and supported-device gates

**Result:** The implementation is reviewable as an alpha.3 stacked pull request
with the same offline, policy, and device evidence as the walking skeleton.

**Verification:**

```text
.\gradlew.bat --dependency-verification strict clean checkNoDynamicVersions test lint assembleDevDebug assembleStagingRelease assembleProdRelease :app:assembleDevDebugAndroidTest :core:database:assembleDebugAndroidTest
python -m unittest scripts/test_verify_android_policy.py
python scripts/verify_android_policy.py
Expected: all gates pass; API 26 and API 37 suites pass offline.
```

## Progress

- [x] 2026-10-06 11:30 +03:00 — Added validated fail-closed DataStore
  persistence and focused JVM tests.
- [x] 2026-10-06 11:30 +03:00 — Added the manual editor, offline zone picker,
  inspectable context, and civil-zone interpretation.
- [x] 2026-10-06 11:30 +03:00 — Wired application-owned persistence through
  explicit model mappings and completion-aware callbacks; bumped the review build to
  `0.1.0-alpha.3` / code 3.
- [x] 2026-10-06 11:30 +03:00 — Completed instrumentation, accessibility,
  device, policy, and release-review evidence.
- [x] 2026-10-06 15:00 +03:00 — Corrected the alpha.3 review finding that
  place setup was below the initial phone viewport. The empty-state action now
  appears directly below the Now summary, and every other destination exposes
  the active or missing place with a direct route into the editor. The forward
  review build is `0.1.0-alpha.4` / code 4.

## Decisions

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| 2026-10-06 | Store one explicit place in a dedicated Preferences DataStore | This is small non-sensitive user-chosen context with a distinct lifecycle; an atomic record avoids mixed old/new fields | [ADR-0003](../adr/0003-database-and-data-class-separation.md) |
| 2026-10-06 | Map persistence and UI models in `app` | Keeps the feature independent of `core:database` and makes the trust boundary visible | [ADR-0006](../adr/0006-manual-app-composition.md) |
| 2026-10-06 | Use the platform IANA-zone list without inferring a zone from coordinates | Works offline and preserves the user's explicit choice | none |
| 2026-10-06 | Accept any finite elevation | The approved requirements provide no universal elevation policy; invented geographic bounds would reject valid or synthetic inputs | none |

## Surprises and discoveries

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| 2026-10-06 | Preferences values can be incomplete or carry unexpected primitive types after corruption or manual manipulation | Repository tests construct malformed and wrong-type records | Decode through `asMap()` and return no context unless the entire record validates |
| 2026-10-06 | The alpha.3 setup action was reachable but not discoverable on a phone because the unchanged civil values filled the first viewport | Mobile review plus the API 26 screenshot showed the Location card clipped above its action; the test called `performScrollTo()` before checking the action | Put the empty place card before the civil values, require its primary action to be displayed without scrolling, and expose place setup from the other destinations |

## Verification and acceptance

Use only synthetic coordinates and labels in tests and screenshots. Final
evidence must include focused model/repository tests, strict dependency
verification, lint, policy checks, merged-manifest inspection, release assembly,
and offline instrumented runs on API 26 and API 37. Inspect English LTR and
Hebrew-locale RTL at normal and 200 percent text, TalkBack semantics order,
touch-target size, cancel/no-write behavior, clear, activity recreation, and a
fresh-process restart over the same DataStore file.

## Rollout and recovery

This is an alpha.4 review build with no remote rollout or feature flag. Clearing
the context is the user recovery path. A code rollback leaves the isolated
preferences file unused; a forward fix may decode or clear it without touching
operational, sensitive, or content stores. No debug-signed review APK may be
published as the official release.

## Residual risks and handoff

The device's available zone list can differ by Android release, so saved IDs
that become unavailable fail closed to the no-location state. Coordinates are
accepted exactly as entered and do not prove a named place or its correct zone.
The screen states that no religious or calendar inference is performed.

## Outcomes

The final alpha.4 build passed 21 application tests and 7 database tests on
both API 26 and API 37. The API 37 run used 200 percent font scaling, with its
task-snapshot and luma
safeguards active throughout. English LTR and labeled-English-fallback RTL
screenshots were visually inspected, and static review found no blocker. A
human TalkBack and hardware-keyboard walkthrough remains part of release review;
the automated and screenshot evidence does not claim to replace it.

The first alpha.3 mobile review found a material discoverability defect despite
those passing checks: the setup control was below the fold and the shell
restored a placeholder destination after upgrade. Alpha.4 adds an above-fold
empty-state action and a global place-context route, and treats unassisted
visibility as an explicit test condition.

The exact alpha.4 APK was also installed directly over the published alpha.3
APK on API 26. The saved Study destination reopened after upgrade with the
missing-place panel visible above the placeholder and its action opened the
manual place editor.
