# Architecture Decision Records

Architecture Decision Records (ADRs) capture consequential technical choices whose rationale would otherwise disappear from code or pull-request history. They explain the decision available to implementers; they do not replace product, editorial, source, privacy, accessibility, or release policy.

## When an ADR is required

Create or update an ADR before changing:

- module boundaries, persistent schemas, public interfaces, or data flow;
- the pinned Android, Kotlin, Gradle, or Java toolchain;
- cryptography, key custody, authentication, backup, export, or network trust boundaries;
- content-pack format, signing, compatibility, installation, or revocation;
- AI provider contracts, outbound-data rules, or high-consequence routing;
- app/package identity, build flavors, signing, distribution, or release provenance; or
- an established decision that affects more than one feature or migration.

Routine implementation detail that stays inside an accepted decision does not need an ADR.

## Naming and lifecycle

Files use `NNNN-short-kebab-title.md`, beginning with `0001`. Numbers are never reused. Each ADR has one status:

- `Proposed`: open for review and not safe to depend on;
- `Accepted`: approved and binding;
- `Superseded by ADR-NNNN`: retained for history;
- `Deprecated`: no longer recommended but not replaced; or
- `Rejected`: considered and intentionally not adopted.

An accepted ADR is changed only to correct facts or links. A later decision gets a new ADR that links to and supersedes the old one.

## Required format

```markdown
# ADR-NNNN: Decision title

- **Status:** Proposed
- **Date:** YYYY-MM-DD
- **Owners:** names or roles
- **Decision scope:** affected modules and interfaces
- **Supersedes:** ADR-NNNN or none
- **Related:** issues, PRs, ExecPlan, canonical policy documents

## Context

What problem and constraints require a durable decision? Include privacy,
security, offline, RTL/accessibility, source/licensing, and migration constraints
that actually apply.

## Decision

State the chosen behavior precisely enough to implement and test it.

## Alternatives considered

Record credible alternatives and the decisive tradeoffs.

## Consequences

List benefits, costs, residual risks, compatibility effects, and follow-up work.

## Verification

Name the tests or evidence that demonstrate the decision is implemented.
```

## Review rules

- Link the ADR from the implementing issue, ExecPlan, and pull request.
- Name human owners. Generated text cannot approve an ADR.
- Security, privacy, signing, provider, or high-consequence changes require the relevant policy reviewers.
- Record unresolved questions explicitly; do not hide a product decision inside implementation detail.
- Update this index in the same pull request that adds or supersedes an ADR.

## Index

The following records were accepted by Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2):

- [ADR-0001: Sensitive storage and case-export cryptography](0001-sensitive-storage-and-case-export-cryptography.md)
- [ADR-0002: Provider boundary and OpenAI Responses adapter](0002-provider-boundary-and-openai-responses.md)
- [ADR-0003: Database and data-class separation](0003-database-and-data-class-separation.md)
- [ADR-0004: App, pack, and release signing](0004-app-pack-and-release-signing.md)
- [ADR-0005: Pinned Android bootstrap toolchain](0005-pinned-android-toolchain.md)

The following record was accepted by Gil on 2026-10-06 in the implementation
conversation for [pull request #4](https://github.com/Gil-netizen/aseh/pull/4):

- [ADR-0006: Manual application composition and constructor injection](0006-manual-app-composition.md)

The following proposed record implements the explicit device-location product
direction recorded in [issue #9](https://github.com/Gil-netizen/aseh/issues/9),
but remains open for human architecture and privacy approval:

- [ADR-0007: Foreground device location and local calendar calculations](0007-foreground-location-and-local-calendar.md)

The following proposed record adds the reviewed-code calendar-context boundary
required to replace synthetic festival overrides, but remains open for human
architecture and editorial approval:

- [ADR-0008: Local calendar-context engine](0008-local-calendar-context-engine.md)

The Android module layout remains for the scaffolding ExecPlan. Scaffolding may
depend on the accepted compatibility set in ADR-0005; a demonstrated
incompatibility requires a superseding ADR.
