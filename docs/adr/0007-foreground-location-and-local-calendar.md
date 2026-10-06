# ADR-0007: Foreground device location and local calendar calculations

- **Status:** Proposed
- **Date:** 2026-10-06
- **Owners:** Gil (`@Gil-netizen`), product and privacy owner; Android architecture maintainers
- **Decision scope:** Android foreground location, stored place metadata, local calendar/zmanim boundary
- **Supersedes:** None; expands the intentionally manual-only scope of ExecPlan 0004
- **Related:** GitHub issue #9, ExecPlan 0005, `PRIVACY_MODEL.md`, `THREAT_MODEL.md`

## Context

ASEH needs coordinates for local calendar and solar calculations. Alpha.4 exposed
manual latitude and longitude as the main setup flow. Mobile users ordinarily do
not know those values, and the product specification explicitly allows device GPS.
The privacy model requires a new review whenever a permission is introduced and
forbids unexplained or background collection.

## Decision

Use Android framework location through `androidx.core.location.LocationManagerCompat`.
Request `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` only after a visible
`Use my location` action and a local-only explanation. Accept approximate access.
Acquire one bounded fix while the app is foreground, cancel it when the requesting
scope ends, and persist it only after explicit confirmation. Declare no background
location, location service, Internet permission, geocoder, map, analytics, or
transmission path.

When only coarse access is available, try the Android network provider before GPS;
with fine access, try GPS before the network provider. If the preferred enabled
provider does not return a usable fix in its allotted portion of the request, try
the other enabled provider without extending the 20-second overall deadline. This
is framework-provider fallback, not an application network request.

Treat elevation as optional and datum-specific. Android's ordinary
`Location.altitude` is ellipsoid-relative and is not used as mean-sea-level input.
On API 34 and newer, persist elevation only when `hasMslAltitude()` reports an MSL
value. On earlier APIs, or when MSL elevation is absent, persist no elevation and
use the calculation engine's disclosed sea-level fallback.

Use Android's current device time zone as an inspectable initial value. Do not infer
a city name, time zone, or Israel/diaspora calendar profile from coordinates. Keep
manual coordinates as an advanced fallback and correction path.

Wrap the local Hebrew-date and astronomical implementation behind a small domain
API. The UI shows its method and uncertainty; missing high-latitude results remain
missing rather than fabricated. Fixed cases are compared with an independent
implementation before release.

## Alternatives considered

- **Keep manual coordinates primary:** rejected by mobile review because ordinary
  users do not know latitude and longitude.
- **Google Play Services fused location:** not selected for the first slice because
  the framework API provides a one-shot offline-capable path without another SDK.
- **Reverse geocoding or IP location:** rejected because it can require a network,
  obscures the data boundary, and is unnecessary for astronomy.
- **Continuous/background location:** rejected because Now needs a user-requested
  current context, not tracking.
- **Infer calendar profile/time zone from coordinates:** deferred until a reviewed
  offline data source and explicit product rule exist.

## Consequences

The Android manifest gains foreground coarse/fine location permissions. The app
must handle denial, approximate access, disabled services, timeout, revocation,
and process death. Exact coordinates remain local ordinary app data and can be
cleared. Horizontal accuracy and source may be stored with the confirmed context;
incompatible ellipsoid altitude is discarded. There is no continuous or background
tracking.

This decision enables useful Now context but does not authorize a prayer edition
or resolve `RIGHTS-004` or `LITURGY-001`.

## Verification

Merged-manifest policy tests require COARSE/FINE and reject background location,
location services, and Internet. Unit tests cover provider order and fallback,
invalid and stale fixes, timeout/cancellation, MSL-only elevation, stored-source
migration, astronomy cases, and missing solar events. Compose and integration tests
cover rationale and permission-result states, denial recovery, disabled services,
confirmation-only persistence, hidden technical coordinates, lifecycle and sunset
refresh, manual fallback, restart, and clear. Eight frozen Hebcal API cases compare
the separately deployed implementation across Jerusalem, New York/DST, the Adar
I/II leap-year boundary, sunset transitions, and Tromsø polar day; provenance and
the shared NOAA lineage limitation are recorded in `domain/zmanim/HEBCAL_GOLDEN_PROVENANCE.md`.

On 2026-10-06 the final full app device suite passed 33/33 tests on API 26 and 34/34 on
API 37 with 200% font; the database suite passed 7/7 on each. In airplane mode,
the focused framework-location suite passed 2/2 on API 26 and 3/3 on API 37. A
manual API 37 walkthrough used the real Android permission dialog for a precise
while-in-use grant, previewed and confirmed the candidate, force-stopped the app,
and verified persisted Hebrew-date and solar output after relaunch. Denial and
other error states are covered by injected automated cases; this record does not
claim an automated operating-system denial-dialog journey.
