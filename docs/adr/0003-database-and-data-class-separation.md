# ADR-0003: Database and data-class separation

- **Status:** Proposed for human approval
- **Date:** 2026-10-06
- **Owner:** Gil (`@Gil-netizen`); reviewers: future Android data, privacy, and corpus maintainers
- **Decision scope:** `core:database`, `core:security`, `data:corpus`, `data:practice`, `data:community`, search and repository interfaces
- **Supersedes:** none
- **Related:** [Privacy model](../PRIVACY_MODEL.md), [Threat model](../THREAT_MODEL.md), [Source policy](../SOURCE_POLICY.md), [ADR-0001](0001-sensitive-storage-and-case-export-cryptography.md)

## Context

ASEH combines distributable reference material, ordinary user state, and
high-consequence case records. They have different authorship, integrity,
backup, deletion, migration, and disclosure rules. A single Room database would
make it easy to back up sensitive data accidentally, modify signed corpus data,
join private facts into broad queries, or destroy user records during a pack
rollback.

Core screens must remain offline, installed packs must be replaceable
atomically, and sensitive records must use the cryptographic boundary in
ADR-0001.

## Decision

ASEH has three physical persistence classes with separate database handles,
DAOs, repositories, migrations, files, and backup rules.

### 1. Verified content and reference stores

Each installed `.asehpack` supplies one or more immutable SQLite databases and
its own FTS indexes. A database is opened only after the pack verifier has
validated the signature, hashes, manifest, rights metadata, schema range, and
SQLite integrity in staging. Runtime connections are query-only and never run
Room migrations, DDL, or writes against a pack.

Pack identity consists of pack ID, immutable version, manifest digest, schema
version, and signing-key ID. The active-pack catalog points to verified
versions; activation changes the catalog atomically only after staging and
index validation. Failure leaves the previous active version untouched.
Rollback changes the active pointer and never opens or edits a user database.

### 2. Operational user store

A Room database holds non-case operational state such as adopted-practice
choices, checklist state, review dates, local pack catalog state, public roles
and schedules, and user-created organizational records classified as
non-sensitive. Small preferences remain in DataStore. Aid requests, dispute
facts, accusations, safeguarding records, private decision evidence, and other
high-consequence community narratives are never operational records. No provider
credential, cryptographic key, authorization header, sensitive payload, or
copied corpus passage may be stored in either Room operational storage or
DataStore.

Operational entities may refer to content only by a typed immutable reference:
pack ID, exact pack version, edition ID, and source-unit ID. Repository code
resolves that reference against an installed verified pack. A missing or
incompatible reference is surfaced as unavailable; it is not silently
redirected to another edition or pack version. Moving a user record to a newer
content identity is an explicit, reviewable migration.

### 3. Sensitive user store

A distinct Room database file stores only the opaque record IDs and encrypted
envelopes defined by ADR-0001, plus the minimum non-sensitive rotation state
needed to recover an interrupted transaction. Case details, aid requests,
dispute facts, accusations, safeguarding matters, vulnerable-person records,
private decision evidence, participant data, free text, and exact timestamps
are ciphertext. The sensitive store has no FTS table, plaintext shadow column,
analytics table, preview cache, or trigger that copies content into the
operational store.

Sensitive and operational databases have separate deletion controls. The
sensitive database, its journal files, encrypted staging files, wrapped-key
artifacts, and derived indexes are explicitly excluded from Android backup and
device-to-device transfer rules. Debug and release manifests use the same
exclusion. Provider keys stay in the Keystore-backed credential store, not in
any of the three database classes.

### Boundary rules

- The app never uses SQLite `ATTACH DATABASE` across persistence classes.
- A SQL query or transaction never spans a pack, operational, and sensitive
  database. Coordination occurs in repositories through stable typed IDs.
- A content pack cannot declare a foreign key, trigger, view, or writable path
  into a user store. Pack files contain no executable extensions.
- Pack entities, operational persistence entities, encrypted sensitive
  envelopes, and domain models are distinct Kotlin types. UI and domain modules
  receive repository interfaces and domain models, not DAOs or database files.
- Mapping code must name the source class explicitly. General-purpose
  serialization of a domain object directly into multiple stores is forbidden.
- Search operates within a verified content store or an approved non-sensitive
  operational index. The alpha does not index decrypted sensitive text.
- Export code selects records through class-specific repositories and applies
  the preview and encryption policy for the most sensitive selected class.
- Destructive Room migration fallback is disabled for user stores. Pack schema
  upgrades install a new verified pack instead of mutating the old one.

Database filenames and directories are constants in the owning module and
covered by backup-rule tests. Production code cannot obtain a raw filesystem
path or generic `SupportSQLiteDatabase` from a feature module.

## Alternatives considered

- **One Room database for all data:** rejected because its shared backup,
  migration, query, and deletion surface violates the distinct trust and
  lifecycle requirements.
- **Copy pack content into the operational database:** rejected because it
  weakens signature provenance, makes rollback destructive, and can silently
  substitute editions.
- **Attach pack databases for convenient SQL joins:** rejected because attached
  databases blur query-only and writable boundaries and complicate atomic pack
  replacement.
- **Sensitive full-text search over plaintext or deterministic tokens:**
  rejected for the alpha because it creates another disclosure surface and
  equality patterns.
- **Encrypt the entire operational database:** deferred because not all
  operational data is classified as high-consequence, while sensitive records
  already need an explicit, testable envelope and deletion boundary.

## Consequences

Cross-class screens perform repository-level joins and must handle an installed
pack being missing, upgraded, or rolled back. A user operation that touches two
stores cannot rely on a global SQL transaction; it must use idempotent steps and
explicit recovery state where atomic behavior matters.

The separation adds Room configurations, migration suites, and mapping code.
It also makes backup inspection, deletion, pack rollback, least-privilege
queries, and privacy review materially simpler. Pack updates cannot delete or
rewrite user-created records.

## Verification

- Static architecture tests prohibit DAO/database dependencies from feature and
  domain modules and prohibit SQLite `ATTACH` in production code.
- Schema snapshots assert an allowlist of tables and columns for each user
  database; sensitive schemas contain no plaintext or FTS columns.
- Build and runtime tests open pack databases query-only and reject mutation,
  unverified paths, incompatible schemas, and altered manifest identities.
- Pack install, failed import, activation, update, and rollback tests preserve
  both user stores and exact edition references.
- Migration tests begin from every shipped operational and sensitive schema;
  destructive fallback is absent and interrupted migrations fail safely.
- Backup/restore and device-transfer inspection on API 26 and the current target
  confirms that the sensitive database, journals, key envelopes, and staging
  files are absent.
- Deletion tests independently remove sensitive records, operational records,
  provider configuration, and provider credentials without crossing classes.
- Secret and fixture scans confirm that Room, DataStore, logs, diagnostics, and
  test assets contain no provider key or plaintext sensitive case data.
