# ADR-0006: Manual application composition and constructor injection

- **Status:** Accepted
- **Accepted:** Gil on 2026-10-06 in the implementation conversation for [pull request #4](https://github.com/Gil-netizen/aseh/pull/4)
- **Date:** 2026-10-06
- **Owners:** Gil (`@Gil-netizen`); future Android architecture maintainers
- **Decision scope:** Android application composition, feature entry points, repository construction, and dependency direction
- **Supersedes:** none
- **Related:** [Issue #3](https://github.com/Gil-netizen/aseh/issues/3), [pull request #4](https://github.com/Gil-netizen/aseh/pull/4), [ExecPlan 0002](../plans/0002-android-walking-skeleton.md), [Issue #5](https://github.com/Gil-netizen/aseh/issues/5), [ExecPlan 0003](../plans/0003-offline-now-context.md), [ADR-0003](0003-database-and-data-class-separation.md)

## Context

The walking skeleton needs an explicit dependency-injection choice before
multiple Android modules establish incompatible construction patterns. The
current graph is small, has no network provider, and contains one operational
database plus non-sensitive preferences. Adding a framework now would increase
bootstrap code generation and supply-chain surface without solving a current
runtime problem.

The app must remain offline, testable, and clear about persistence boundaries.
Feature modules must not obtain Room DAOs, database files, Android service
locators, or future credentials.

## Decision

ASEH uses constructor injection and one application-owned manual composition
root for the walking skeleton.

- `app` contains `AppGraph`, which constructs platform adapters and repository
  implementations once from the application context. It owns the application
  scope that serializes and conflates preference writes so the latest selection
  survives activity recreation.
- Activities and top-level composables receive dependencies from `AppGraph` and
  pass only narrow state, callbacks, or repository interfaces downward.
- Feature modules do not read a global container and do not depend on
  `core:database`.
- DAOs, Room database handles, DataStore instances, filesystem paths, and
  Android contexts remain inside their owning adapter or the composition root.
- Tests construct the same interfaces with deterministic in-memory or temporary
  implementations. Production selection never depends on reflection or string
  class names.
- A future DI framework requires a superseding ADR with measured build,
  lifecycle, generated-code, testing, and migration evidence.

## Alternatives considered

- **Hilt:** mature and compile-time checked, but it adds generated components,
  annotations across module APIs, and another compiler/plugin before the graph
  needs automatic wiring.
- **Koin or another runtime service locator:** concise, but runtime lookup and
  implicit global access weaken compile-time graph visibility and make forbidden
  persistence access harder to audit.
- **Construct dependencies inside activities or feature composables:** rejected
  because lifecycle ownership and adapter boundaries would be duplicated.

## Consequences

The initial graph is plain Kotlin and visible in one place. Unit and Compose
tests can pass fakes without framework support. Adding dependencies requires an
explicit `AppGraph` edit, which is acceptable for this small graph and makes
review straightforward.

The graph will become verbose as features grow. It does not automatically
validate every scope or cycle, so architecture tests and module dependencies
must enforce the boundary. The team must revisit this decision before manual
wiring becomes error-prone rather than introducing a framework piecemeal.

## Verification

- Compile all modules without a DI framework dependency or DI annotation
  processor. Room continues to use KSP for its own schema generation.
- Gradle module inspection and a source audit confirm feature modules depend
  only on `core:ui` and do not import Room, DataStore, database handles, or
  `AppGraph`.
- Unit and instrumented tests exercise temporary DataStore repositories and an
  in-memory Room database without changing production construction.
- The Compose test obtains the preference repository from the application-owned
  graph, verifies activity recreation, and separately restarts the repository
  over the same DataStore file. Full process-death validation remains a manual
  review item.
