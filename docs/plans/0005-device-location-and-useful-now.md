# Device location and useful offline Now

This ExecPlan is a living document. Keep `Progress`, `Surprises & Discoveries`,
`Decision Log`, and `Outcomes & Retrospective` current while implementing it.

## Purpose / big picture

The alpha.4 review showed a product failure: the app asked a mobile user to type
latitude and longitude, while the four non-Now destinations still contained no
testable task. Issue #9 corrects the first failure and establishes the first
useful local context. A user can deliberately request a one-shot foreground
device location and immediately see a Hebrew date plus sunrise, solar noon, and
sunset calculated on the device. Manual coordinates remain an advanced fallback.

This slice does not claim to complete ASEH. Prayer text remains blocked by
`RIGHTS-004` and `LITURGY-001`; Practice, Study, and Build remain unavailable
until each contains a complete task. Release notes must state those limits.

## Progress

- [x] 2026-10-06 — Mobile review established that manual coordinates are not a
  viable primary flow and that alpha.4 has no meaningful product task.
- [x] 2026-10-06 — Created GitHub issue #9 with the location, privacy, calendar,
  astronomy, accessibility, and device acceptance boundary.
- [x] 2026-10-06 — Reviewed Android foreground/approximate location guidance and
  `LocationManagerCompat`; selected a framework-only, one-shot design.
- [x] 2026-10-06 — Added backward-compatible source and optional horizontal-
  accuracy metadata to stored place context; existing records migrate as manual.
- [x] 2026-10-06 — Added foreground-only Android location acquisition, lifecycle
  cancellation, ordered provider fallback, and manifest/policy gates.
- [x] 2026-10-06 — Made device location the primary Now setup path and kept
  coordinate entry behind an explicit manual fallback.
- [x] 2026-10-06 — Added the local Hebrew-date and solar-time engine with method,
  high-latitude missing-result, and elevation-fallback disclosures.
- [x] 2026-10-06 — Completed focused unit, domain, Compose, policy, app, database,
  and API 26/API 37 device verification recorded below.
- [ ] Publish a review APK only after the full GPS-to-useful-Now path passes.

## Surprises & discoveries

- Alpha.4's automated coverage was strong for its narrow implementation, but the
  tested implementation was still a shell. Test count did not measure product utility.
- The approved privacy model already permits local time/place context, but any
  Android permission requires a new privacy review and just-in-time explanation.
- Device coordinates do not provide a city name or time zone offline. The first
  implementation uses Android's current device time zone and exposes correction;
  it does not reverse-geocode or infer Israel/diaspora status.
- Framework providers need a bounded fallback. With only coarse access, the app
  tries network and then GPS; with precise access, it tries GPS and then network.
  A stalled first provider cannot consume the entire 20-second request window.
- Android's ordinary `Location.altitude` is ellipsoid-relative, while the solar
  calculation expects mean-sea-level elevation. API 34 and newer persist elevation
  only when Android supplies an explicit MSL value; other fixes use a disclosed
  sea-level calculation rather than treating incompatible altitude as equivalent.
- A connected-test Gradle task can finish successfully when a device-side install
  failure leaves a zero-test XML report. The emulator runner now rejects zero tests
  and any nonzero failure or error count.

## Decision log

- **Device location is primary.** The user explicitly rejected manual coordinates
  as the normal mobile flow; manual input remains under an advanced action.
- **One-shot foreground access only.** The app requests COARSE and FINE together,
  accepts approximate access, never requests background location, and never starts
  a location foreground service.
- **No location network path.** Use Android framework providers through
  `LocationManagerCompat`; add no Play Services, geocoder, map, IP lookup, or account.
- **Ordered provider fallback.** Prefer GPS for precise access and the network
  provider for coarse access, then try the other enabled provider within one bounded
  request. Provider choice changes acquisition only; no app network permission or
  outbound location path is introduced.
- **MSL elevation or explicit sea-level fallback.** Persist elevation only from
  Android's API 34+ mean-sea-level field. Never substitute the ordinary ellipsoid
  altitude. When MSL elevation is absent, calculate at sea level and disclose that.
- **Confirmation before persistence.** A location fix is a transient candidate.
  The user sees source, accuracy, device time zone, and local-only disclosure before
  choosing `Use this location`; cancel discards the candidate.
- **Calendar implementation is wrapped.** A narrow domain interface isolates the
  selected JVM implementation, exposes calculation method and missing-result states,
  and is tested against independent fixed examples.
- **Content integrity still applies.** This code slice cannot silently package
  unapproved prayer text to make the APK appear more complete.

## User-visible behavior

On an empty Now screen, `Use my location` is the primary action. It first explains
that ASEH asks Android for one foreground fix, saves nothing until confirmation,
does not track in the background, and sends nothing away. Android then offers its
normal approximate/precise and duration choices. A successful fix produces a
confirmation card with approximate/precise status, accuracy when supplied, the
device time zone, and a collapsed technical-coordinate section. Confirmation saves
the context atomically and renders Hebrew date, sunrise, solar noon, sunset, the
next event, and a calculation-method disclosure.

Permission denial, permanent denial, disabled location services, timeout, invalid
fix, and save failure each have a named recovery. No state repeatedly launches the
system dialog. Manual coordinates remain reachable and functional.

## Validation

Run focused tests while iterating, then the repository's strict clean gate:

    .\gradlew.bat --no-daemon --no-parallel --dependency-verification strict clean checkNoDynamicVersions test lint assembleDevDebug assembleStagingRelease assembleProdRelease :app:assembleDevDebugAndroidTest :core:database:assembleDebugAndroidTest
    python -m unittest scripts/test_verify_android_policy.py
    python scripts/verify_android_policy.py

Recorded device evidence on 2026-10-06:

- The final complete app connected suite passed 33/33 tests on API 26 and 34/34 tests on
  API 37; the API 37 run used Android's 200% font setting.
- The database connected suite passed 7/7 tests on each API level.
- With airplane mode enabled and an injected framework GPS fix, the focused location
  client passed 2/2 tests on API 26 and 3/3 tests on API 37. These focused tests
  establish offline provider acquisition and fallback, not the whole persisted UI
  journey in airplane mode.
- A manual API 37 walkthrough exercised the real Android permission dialog, selected
  precise while-in-use access, showed a roughly 5-meter candidate and device time
  zone, confirmed it, force-stopped the app, and verified the persisted Hebrew date
  and solar events after relaunch.
- Automated Compose and integration cases cover denial, disabled services, timeout,
  hidden saved-device coordinates, a direct fresh-location action, lifecycle and
  sunset refresh, RTL Hebrew-date runs, manual fallback, confirmation-only
  persistence, restart, and clear. The Android system-dialog denial journey was not
  itself manually automated.
- Eight frozen Hebcal API fixtures cross-check Jerusalem, New York/DST, the Adar
  I/II leap-year boundary, after-sunset behavior, and Tromsø polar day within the
  documented two-minute tolerance. The evidence records the shared NOAA lineage and
  does not claim agreement between unrelated astronomical models.

## Outcomes & retrospective

The implementation and local device gates are complete. The slice replaces the
latitude/longitude-first setup with an Android permission flow and makes Now useful
offline after confirmation: it shows the Hebrew date, sunrise, solar noon, sunset,
and the next solar event, and it refreshes those results on resume and at time/solar
boundaries. Saved coordinates remain under technical details and can be reacquired
directly. It deliberately remains a bounded alpha slice; Practice,
Pray, Study, and Build are still incomplete, and no prayer text was added without
rights and liturgical review.

CI review, the review-only APK, and publication remain pending. This plan does not
claim a published artifact or an automated Android system-dialog denial walkthrough.
