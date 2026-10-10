# ASEH Android content runtime

This module opens an already signature- and hash-verified `ActiveContentStore`
through Android platform SQLite. It verifies the immutable database read-only,
enforces the schema/table/metadata allowlists, and exposes parameterized FTS4
search. Search input is reduced to bounded quoted prefix terms before it reaches
SQLite, so MATCH operators and SQL punctuation cannot become query syntax.

The module has no SQLite JDBC runtime dependency. `sqlite-jdbc` remains a JVM
compiler/test dependency in `:core:content` only.

`androidVerifiedPackInstaller` is the Android construction boundary. It injects
the platform database verifier and an Ed25519 verifier that fails explicitly if
the default JCA provider is absent. Android API 26 has no default JCA Ed25519
provider, so signed pack installation remains disabled there unless the project
later approves a reviewed API-compatible cryptography implementation. The
algorithm, signed message, key identity, and trust policy must not be weakened.
