# ADR-0008: Local calendar-context engine

- **Status:** Proposed
- **Date:** 2026-10-07
- **Owners:** Gil (`@Gil-netizen`), product owner; Android architecture and editorial maintainers
- **Decision scope:** `domain:calendar`, KosherJava holiday-calendar boundary, and `domain:servicecatalog` calendar-day inputs
- **Supersedes:** None; complements ADR-0007
- **Related:** `PRODUCT.md` section 7.5, `METHOD.md`, `SOURCE_POLICY.md`, ExecPlan 0006

## Context

ASEH's service catalog accepts explicit `CalendarDayOverride` values, but it
intentionally does not calculate the Hebrew calendar or choose an Israel or
diaspora scheme. The app currently falls back to a synthetic Saturday/weekday
rule. The product specification requires local Hebrew/Gregorian conversion,
first-class Israel and diaspora schemes, festivals, fasts, Rosh Hodesh, special
days, and transparent details. It also requires independent golden cases before
a calendar implementation is trusted.

KosherJava 2.5.0 is already pinned and distributed for the local zmanim module.
Its `JewishCalendar` surface supplies date conversion, an explicit Israel flag,
holiday indices, work-restricted-festival and fast flags, Rosh Hodesh, and named
special-Shabbat metadata. The same library also exposes weekly readings and
prayer-rule helpers. Using those broader surfaces without an accepted textual,
editorial, and opinion-profile decision would cross ASEH's source and method
boundaries.

## Decision

Add a pure JVM `domain:calendar` module. Its API accepts a civil service date, an
explicit `CalendarRegion`, and optional explicit walled-city status. It reads no
clock, location, time zone, locale, network, or process defaults. Coordinates do
not select Israel or diaspora, and Israel/diaspora does not select walled-city
status.

The adapter constructs KosherJava `JewishCalendar` with modern holidays disabled.
It exposes:

- numeric Hebrew date and typed month, including Adar I/II;
- typed observance metadata with the exact KosherJava signal that produced it;
- Rosh Hodesh, fast, work-restricted-festival, and special-Shabbat flags;
- an explicit service-day classification rule and raw input signals;
- a region-scoped `CalendarDayOverride`; and
- typed unresolved fields for anything the adapter did not establish.

For the service-catalog bridge, a work-restricted festival maps to `FESTIVAL`.
Otherwise Saturday maps to `SHABBAT`; all other dates map to `WEEKDAY`. The trace
records which rule won, including a date that is both Shabbat and a festival.
Intermediate festival days remain weekday or Shabbat in the current three-value
service-day taxonomy while retaining their typed festival metadata.

The adapter does not invent `CalendarAdditionDefinition` IDs. Its override carries
an empty addition-ID set until a reviewed catalog explicitly maps the typed
calendar metadata to additions. It does not call KosherJava's weekly-reading,
upcoming-reading, or prayer-rule APIs. Weekly and festival readings, liturgical
wording and insertions, seasonal prayer changes, modern observances, sunset
boundary selection, and user/community events remain visibly unevaluated.

The civil input is the service date interpreted for its daytime Jewish date.
ADR-0007's local zmanim boundary must resolve any current instant or evening
transition before a caller chooses a service date. A library exception returns an
explicit unavailable result; the adapter does not infer a fallback event.

KosherJava remains pinned to 2.5.0. The module packages the existing LGPL 2.1
license and notice resources. Offline golden tests compare selected results with
Hebcal API observations whose provenance and CC BY 4.0 terms are recorded beside
the test fixture.

## Alternatives considered

- **Keep manual festival overrides:** rejected because it leaves the core calendar
  nonfunctional and makes Israel/diaspora divergence impractical to maintain.
- **Use KosherJava directly from UI code:** rejected because library constants,
  defaults, and unmapped values would bypass a reviewable domain boundary.
- **Expose KosherJava weekly readings and prayer rules now:** deferred because the
  pinned library result alone does not establish ASEH's editions, textual variants,
  liturgical profile, or editorial approval.
- **Infer Israel/diaspora or walled-city status from coordinates:** rejected because
  ADR-0007 requires the calendar profile to remain an explicit user choice, and no
  reviewed offline locality dataset establishes either policy input.
- **Treat every holiday index as a festival service day:** rejected because minor
  observances, fasts, eves, intermediate days, and post-festival days do not all
  select the same service definition.

## Consequences

The service scheduler can receive deterministic local overrides for an inclusive
range and expose the metadata behind them. Israel and diaspora produce separate,
testable results without a network dependency. Callers can explain both the raw
library signals and ASEH's narrow classification rule.

The three-value `ServiceDayKind` still cannot represent combinations such as
Shabbat during an intermediate festival. Typed calendar metadata preserves that
information, but catalog additions and service assembly must be designed and
reviewed before they act on it. The adapter's English fallbacks are identifiers
for metadata, not approved Hebrew localization or liturgical text.

This proposed record does not approve a calendar opinion, prayer edition,
reading, modern-observance policy, or catalog-addition mapping. Human architecture
and editorial review remain required before changing this ADR to Accepted.

## Verification

- Unit tests cover Adar I/II, Rosh Hodesh, shifted fasts, the Fast of the
  Firstborn, special Shabbat metadata, festival/Shabbat precedence, Purim locality,
  unresolved surfaces, deterministic range output, and direct service-catalog use.
- Frozen Hebcal cases cover Israel/diaspora divergence on Pesach, Shavuot, and
  Simchat Torah; leap and non-leap Adar; fasts; Rosh Hodesh; and an intermediate
  festival day on Shabbat.
- Dependency locking, dependency verification, pinned-version checks, and the
  packaged KosherJava license/notice remain part of the repository verification
  gate.
