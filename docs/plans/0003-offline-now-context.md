# ExecPlan 0003: Offline Now civil context

**Status:** Active\
**Owner:** Gil\
**Started:** 2026-10-06\
**Last updated:** 2026-10-06\
**Issue/PR:** [GitHub issue #5](https://github.com/Gil-netizen/aseh/issues/5); pull request pending\
**Related ADRs:** [ADR-0005](../adr/0005-pinned-android-toolchain.md), [ADR-0006](../adr/0006-manual-app-composition.md)

This plan replaces the first static destination in the Android walking skeleton
with one bounded, useful offline feature. It does not introduce religious or
calendar calculation policy.

## Outcome

The Now destination shows a fresh snapshot of the device's civil context: civil
date, local time, weekday, and IANA time-zone identifier. It works without an
account, location, or network, states plainly that no location is configured,
and lets the user refresh the snapshot with an accessible visible action.

## Success criteria

- Entering Now samples the injected time source and renders civil date, local
  time, weekday, and the complete IANA time-zone identifier.
- The visible refresh action has a concise accessible name, a touch target of
  at least 48 dp, and resamples the time source without sleeping or starting a
  network request.
- A deterministic test controls the first and refreshed values. Production
  construction obtains the time source from the application composition root.
- The no-location state explains that the displayed civil context remains
  usable and that location plus a reviewed calculation profile are prerequisites
  for future Hebrew day-boundary or zmanim results.
- English LTR and Hebrew-locale RTL layouts preserve logical reading order,
  isolate mixed-direction values, and remain usable at 200% font scale. Until
  reviewed Hebrew copy exists, the Hebrew locale visibly labels English fallback
  text instead of presenting it as Hebrew.
- The merged manifest retains no `INTERNET`, coarse-location, or fine-location
  permission, and no network, authentication, telemetry, religious-calendar,
  or location dependency is added.
- Existing strict build, lint, policy, API 26, and API 37 gates remain green,
  and CI retains a dev debug APK for mobile testing.

## Canonical requirements

- `AGENTS.md`
- `docs/PRODUCT.md`
- `docs/PRIVACY_MODEL.md`
- `docs/THREAT_MODEL.md`
- `docs/ACCESSIBILITY.md`
- `docs/RELEASE.md`
- `docs/adr/0005-pinned-android-toolchain.md`
- `docs/adr/0006-manual-app-composition.md`

No conflict was found. `PRODUCT.md` defines Now as the destination for date and
context and requires the core to work without a network. `PRIVACY_MODEL.md`
requires time and place context to be calculated locally and says a new Android
permission needs separate privacy review. `ACCESSIBILITY.md` requires an
explicit no-location state that explains what remains usable. This slice meets
those requirements with civil device context only and defers every policy choice
needed for religious calculations.

## Scope and non-goals

**In scope**

- Inject a `java.time.Clock` or equivalently narrow time source from `AppGraph`
  through the app entry point into `feature:now`.
- Replace `AsehFeaturePlaceholder` in Now with a scrollable civil-context screen.
- Sample and format civil date, local time, weekday, and IANA time-zone ID on
  screen entry and on an explicit refresh action.
- Show a permanent, explicit no-location status and bounded explanation.
- Add English strings and visibly labeled English fallback resources for the
  Hebrew locale, with direction-safe dynamic values.
- Add deterministic JVM and Compose/device tests and preserve the existing
  build, policy, emulator, screenshot, and APK evidence paths.

**Out of scope**

- Location permission, device coordinates, place lookup, manual location
  selection, geocoding, networking, accounts, telemetry, or background work.
- Hebrew date or day-boundary calculation, calendar scheme selection, solar
  events, derived zmanim, holiday or prayer additions, rulings, or uncertainty
  policy for any of those results.
- Corpus text, citations, content packs, prayer assembly, notifications, widgets,
  settings persistence, or automatic ticking while the screen remains open.
- Reviewed Hebrew translation. English fallback copy remains explicitly marked
  in the Hebrew locale until human language review supplies a later change.

## Current state and context

The walking skeleton routes `AsehDestination.NOW` from
`app/src/main/java/io/github/gilnetizen/aseh/AsehApp.kt` to
`feature/now/src/main/java/io/github/gilnetizen/aseh/feature/now/NowScreen.kt`.
That screen currently delegates to `AsehFeaturePlaceholder` and contains no
time source, feature state, user action, or calculation.

`AppGraph` is the accepted application-owned composition root. The app already
passes narrow dependencies and callbacks to top-level composables, while
feature modules remain isolated from Android storage implementations. The
repository has offline and forbidden-permission checks plus fixed API 26 and API
37 device profiles; this slice extends those existing boundaries rather than
creating a service or persistence layer.

In this plan, a **civil-context snapshot** is one instant interpreted in the
zone exposed by the separately injected live device-zone source. It is
informational device context.
It is not a Hebrew date, a sunset boundary, a solar event, a zman, or an adopted
practice. A **refresh** replaces that snapshot after the user activates the
visible action; it does not imply continuous background updates.

## Design and interfaces

`AppGraph` owns the production UTC instant clock and current-device-zone
provider and passes them through `AsehApp` to `NowScreen`, following ADR-0006.
`feature:now` owns the pure conversion from those injected sources to display
state. Capturing a snapshot reads the instant once and the zone provider once so
every value on a rendered screen describes the same moment. The screen samples
once when it enters composition and again only when the user activates Refresh.
Tests substitute a fixed or controllably advancing clock and a fixed or mutable
zone provider and never wait for wall time.

The display state contains separate values for civil date, local time, weekday,
and IANA zone ID. It contains no coordinate, persisted setting, source claim,
calendar result, or network-derived field. Dynamic values use explicit
direction isolation where necessary, especially the Latin-script IANA ID inside
an RTL layout. Resource strings hold complete phrases rather than assembling
localized sentences from fragments.

The screen exposes one semantic heading followed by labeled date, time,
weekday, and time-zone values, then the no-location status and refresh control
in logical order. The no-location copy says what works now and why religious
results are absent. Refresh updates values without moving accessibility focus or
hiding the control. Content can reflow or scroll vertically at 200% text; no
essential value requires horizontal scrolling.

There is no persistence or migration. The Android permission set and network
boundary remain unchanged. Location, a calculation profile, and religious
calendar logic require later reviewed work before the app may display a Hebrew
day boundary or zmanim.

## Milestones

### 1. Deterministic civil-context model

**Result:** `feature:now` can derive one internally consistent civil-context
snapshot from an injected time source and format it without Android services.

**Work:**

- Pass the time source from `AppGraph` through `AsehApp` to Now.
- Add a small immutable display state and pure snapshot/formatting logic.
- Add fixed and advancing-clock tests for initial and refreshed values, including
  a non-UTC IANA zone and a date boundary.

**Verification:**

```text
.\gradlew.bat --no-daemon :feature:now:testDebugUnitTest
Expected: deterministic initial and refresh assertions pass without sleeps.
```

### 2. Accessible offline Now screen

**Result:** Now displays the current civil snapshot, explicit no-location state,
and visible refresh action in English LTR and Hebrew-locale RTL.

**Work:**

- Replace `AsehFeaturePlaceholder` with the Now layout and complete semantics.
- Add English copy and visibly labeled English fallback copy for the Hebrew
  locale; isolate mixed-direction values.
- Keep the layout usable at 200% text and retain at least a 48 dp refresh target.
- Add Compose assertions for content, order, labels, fallback treatment, and
  refresh behavior.

**Verification:**

```text
.\gradlew.bat --no-daemon :feature:now:testDebugUnitTest :app:connectedDevDebugAndroidTest
Expected: semantic content and deterministic refresh pass, including the fixed
English-LTR and Hebrew-locale RTL fixtures.
```

### 3. Repository and device evidence

**Result:** The complete app remains offline, permission-minimal, buildable, and
installable on the supported device matrix.

**Work:**

- Extend instrumentation evidence for Now at API 26 and API 37, including an API
  37 capture at 200% font scale and offline launch.
- Run the existing manifest, dependency, repository-policy, unit, lint, and
  assembly gates.
- Retain the dev debug APK for direct mobile installation and review.

**Verification:**

```text
.\gradlew.bat --no-daemon --dependency-verification strict checkNoDynamicVersions test lint assembleDevDebug assembleStagingRelease assembleProdRelease :app:assembleDevDebugAndroidTest
Expected: all tasks pass; the merged manifests contain no INTERNET or location
permission; app/build/outputs/apk/dev/debug/app-dev-debug.apk exists.

bash scripts/run_android_emulator_tests.sh <emulator-label>
Expected: the supported API 26 and API 37 profiles pass navigation, Now content,
refresh, semantics, offline launch, and required screenshot evidence.
```

## Progress

- [x] 2026-10-06 10:15 +03:00 — Issue #5 scope, canonical requirements, and
  accepted manual-composition decision recorded in this ExecPlan.
- [x] 2026-10-06 10:40 +03:00 — Injected instant and live device-zone sources,
  deterministic snapshot/refresh behavior, separate weekday state, and five JVM
  tests implemented.
- [x] 2026-10-06 10:43 +03:00 — Replaced the placeholder with the scrollable Now
  screen and added device assertions for one heading, merged labels and values,
  logical semantics-tree order, labeled Hebrew-locale fallback, reachable
  refresh, and a 48 dp target.
- [x] 2026-10-06 10:56 +03:00 — Strict dependency, unit, lint, assembly, merged
  manifest, API 26 phone, and API 37 tablet-at-200%-text local gates pass. The
  launcher capture now waits for ASEH focus on both supported Android versions.
- [ ] Record latest-head CI runs, retained CI APK, and named human accessibility
  and language review results.

## Decisions

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| 2026-10-06 | Sample on screen entry and explicit refresh | Gives the user inspectable, deterministic context without background work or an implied continuously ticking clock | none |
| 2026-10-06 | Obtain the production time source from `AppGraph` | Preserves explicit construction and permits fixed-clock tests without global time lookup in feature code | [ADR-0006](../adr/0006-manual-app-composition.md) |
| 2026-10-06 | Show a no-location state and no religious result | Civil device context is useful without inventing location, calculation profile, or religious-calendar policy | none |
| 2026-10-06 | Keep Hebrew-locale copy as visibly labeled English fallback | Human-reviewed Hebrew does not yet exist; silent or unreviewed translation is prohibited | none |

## Surprises and discoveries

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| 2026-10-06 | The Now feature is a single placeholder composable and has no state or dependency surface | `feature/now/.../NowScreen.kt` delegates directly to `AsehFeaturePlaceholder` | Keep the first feature state local and narrow; no new shared state module is needed. |
| 2026-10-06 | Device time-and-place context is already classified as local data, while new permissions require privacy review | `docs/PRIVACY_MODEL.md` data inventory and permission rule | Read civil time locally, persist nothing, and add no location permission. |
| 2026-10-06 | `Clock.systemDefaultZone()` retains the zone captured when the process constructs the clock | Independent diff review plus a mutable-zone regression test | Inject UTC instant time separately from a `ZoneId.systemDefault()` provider sampled on every snapshot. |
| 2026-10-06 | Waiting only for the app process allowed an API 26 launch screenshot to capture the launcher transition | Local `launch.png` disagreed with the later window hierarchy | Require the package to appear in WindowManager's focused-app records and wait one bounded second before capture. |
| 2026-10-06 | Android 17 ignores the legacy `dumpsys window windows` selector while the full dump retains focus records | API 37 focus evidence was empty until the unqualified dump was inspected | Use the unqualified `dumpsys window` output for the cross-version focus guard. |

## Verification and acceptance

Completion requires the focused Now unit and Compose tests; the existing strict
unit, lint, assembly, dependency, manifest, and repository-policy gates; and the
fixed API 26 and API 37 device profiles. A fixed or controllably advancing clock
must prove both the first snapshot and refreshed snapshot without sleeping.

Device evidence must show English LTR and Hebrew-locale RTL, logical TalkBack
semantics, complete IANA time-zone text, a reachable 48 dp refresh action, an
explicit no-location state, offline launch, and 200% text without clipped values
or controls. Static inspection must prove the merged manifest has no Internet or
location permission and the dependency graph gained no network, authentication,
telemetry, location, or religious-calendar library.

Human review remains required for English copy, the visibly labeled Hebrew-locale
fallback, TalkBack reading order, RTL direction, and 200% reflow. Automated
checks cannot grant language or accessibility approval.

## Rollout and recovery

The slice changes no schema, stored preference, account, pack, or migration. It
can be removed by restoring the placeholder and removing the clock and device-
zone parameters without data recovery. Review builds use the existing dev flavor
and APK evidence path.
Any public release still follows `docs/RELEASE.md`; a test APK is not evidence of
a production-signed release.

## Residual risks and handoff

- A sampled screen can become stale while it remains open, including after an
  operating-system time-zone change. Refresh resamples both the instant and the
  current device zone; automatic ticking plus lifecycle or system-event refresh
  are deferred.
- Device civil time and zone can be misconfigured by the user or operating
  system. The screen reports them as device context and makes no correctness
  claim for religious use.
- Hebrew translation, location selection, calendar profiles, Hebrew day
  boundaries, solar inputs, and zmanim remain unimplemented and must not be
  inferred from this screen.

## Outcomes

The local implementation outcome is complete. Now reports a single sampled
device instant as civil date, weekday, local time, and zone ID; refresh resamples
both the instant and the current operating-system zone. The screen persists
nothing, adds no permission or network dependency, and explains why Hebrew day
boundaries and zmanim are absent without a location and reviewed method.

Local verification on 2026-10-06 produced:

- 16 JVM tests passed with zero failures, including five deterministic Now
  formatting, midnight, refresh, and live-zone cases;
- the strict 831-task Gradle gate passed, including locked dependency checks,
  lint, all unit tests, dev debug APK, staging release APK, production release
  APK, and the dev instrumentation APK;
- the Android offline-policy checker passed for source declarations and every
  merged manifest;
- API 26 and API 37 each passed six app tests, six database tests, English LTR
  and Hebrew-locale RTL captures, focused-app launch evidence, and a radios-
  disabled offline launch; API 37 used the pinned tablet profile at 200% text;
  and
- the local test-only APK is
  `app/build/outputs/apk/dev/debug/app-dev-debug.apk`, 14,634,912 bytes, SHA-256
  `180B8017B5DE62B042C22E2A45BF3AA8D3CF6423DE9173D1B9ED8135B473A7FD`.

The APK is Android debug-signed and is a review build, not a policy-compliant
public ASEH alpha. Latest-head GitHub CI, a retained CI artifact, and named human
review of English copy, labeled Hebrew fallback, TalkBack order, RTL, and 200%
reflow remain before this ExecPlan can be marked complete. No religious
calendar result or source claim was added.
