# ASEH content-pack foundation

This module implements the policy boundary for typed editorial content and
signed `.asehpack` files. Its test corpus is invented development material; it
contains no sacred, liturgical, or Torah text.

## Portable API surface

- `EditorialPackSource`, `PackManifest`, and the related typed metadata models
- `ContentSourceJson`, `PackManifestJson`, `PackSignatureJson`, and strict
  canonical JSON parsing with duplicate-key rejection
- `ContentPolicyValidator` for rights, review, citations, localization,
  dependency cycles, and unsafe-content gates
- `PackSigner`, `PackSignatureVerifier`, `PackTrustStore`, and the JDK Ed25519
  implementations
- `DeterministicPackCompiler`, with an injected `ContentDatabaseCompiler`
- `VerifiedPackInstaller`, with an injected
  `InstalledContentDatabaseVerifier`
- `InstalledPackCatalogState` and `ActiveContentStore` for repository-layer
  handoff

The archive compiler writes raw STORED ZIP records in UTF-8 path order with a
fixed DOS timestamp, fixed `0644` file mode, no comments or extra fields, and a
SHA-256 inventory. Installation verifies limits, paths, canonical metadata,
trust scope, Ed25519 signature, schema ranges, dependencies, size, and hash
before extracting into isolated staging. Catalog activation and rollback use an
atomic pointer update.

## Build-tool adapters and Android boundary

`JdbcDeterministicContentDatabaseCompiler`, `JdbcContentDatabaseVerifier`, and
`JdbcInstalledContentSearch` are JVM build-tool/test adapters. Generated packs
use FTS4 because it is available in the Android API 26 platform SQLite; FTS5 is
not available there. `sqlite-jdbc` is declared `testRuntimeOnly`; it is not
exported to Android consumers.

Android integration must supply a platform-SQLite implementation of
`InstalledContentDatabaseVerifier`, open the verified database query-only, and
wrap `ActiveContentStore` in a repository so feature modules never receive a
filesystem path. The Android adapter should run the same `quick_check`,
application ID, schema, metadata identity, foreign-key, and FTS checks covered
by the JDBC adapter. API 26 support must also exercise the platform Ed25519
provider; if `Signature("Ed25519")` is unavailable, inject an API-compatible
`PackSignatureVerifier` rather than changing the signed message or trust model.

No private signing key belongs in the repository. Tests generate ephemeral
Ed25519 keys. A committed signed development asset can be produced by a build
tool using an externally supplied development key, then verified in a debug-only
trust configuration that contains only its public key. Production trust remains
separate.
