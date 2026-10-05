# ADR-0001: Sensitive storage and case-export cryptography

- **Status:** Proposed for human approval
- **Date:** 2026-10-06
- **Owner:** Gil (`@Gil-netizen`); reviewers: future Android security and privacy maintainers
- **Decision scope:** `core:security`, sensitive persistence, `.asehcase` export and import, backup rules
- **Supersedes:** none
- **Related:** [Privacy model](../PRIVACY_MODEL.md), [Threat model](../THREAT_MODEL.md), [Release policy](../RELEASE.md)

## Context

ASEH stores case-preparation data that can concern health, fertility, marriage,
divorce, conversion, vows, abuse, death, accusations, and monetary disputes.
The public alpha must keep that data local by default, must not put a secret in
Room or DataStore plaintext, and must offer only encrypted sensitive exports.
The minimum supported Android version is API 26.

Android Keystore can make key extraction harder, but it does not make an
unlocked, rooted, or compromised device trustworthy. Export passphrases also
need a portable format: a device-bound Keystore key cannot protect a file that
another device must open.

## Decision

### Local sensitive-record envelope

Sensitive records use application-layer envelope encryption. The database may
store only an opaque random record ID and the following versioned envelope:

- envelope and payload-schema versions;
- key version;
- a 96-bit nonce;
- ciphertext; and
- a 128-bit authentication tag.

All case fields, exact timestamps, free text, participant data, and display
labels stay inside the ciphertext. The alpha creates no plaintext or
deterministic full-text index over sensitive fields.

Each payload is encrypted with AES-256-GCM. A 256-bit data-encryption key (DEK)
is generated with `SecureRandom`. Every encryption receives a fresh 96-bit
random nonce; a nonce is never derived from a record ID or counter and a
`(DEK, nonce)` pair must never be reused. The authenticated additional data
(AAD) is an unambiguous length-prefixed encoding of:

1. the ASCII domain `io.github.gilnetizen.aseh/sensitive-record`;
2. envelope version;
3. payload-schema version;
4. key version;
5. the fixed record-class code `case-record`; and
6. the opaque record ID.

Changing any of those values therefore causes authentication failure. The
reader validates lengths and supported versions before decryption and returns
one generic authentication error for a wrong key, altered AAD, ciphertext, or
tag.

The sensitive database enforces uniqueness of `(key version, nonce)` before a
ciphertext can commit. A random collision or repeated test RNG causes the write
to discard that nonce and retry with a bounded failure limit; it never replaces
an existing envelope or commits a reused pair.

The DEK is never stored in plaintext. A non-exportable 256-bit AES-GCM
key-encryption key (KEK) is generated in Android Keystore with encryption and
decryption purposes, GCM mode, no padding, and randomized encryption required.
The KEK encrypts the DEK with a separate random 96-bit wrap nonce and AAD that
binds the application ID, purpose, envelope version, and key version. The
wrapped DEK and wrap nonce may be stored beside the sensitive database; the
Keystore alias and both artifacts are excluded from backup. Session plaintext
keys live only as long as needed and are cleared on a best-effort basis.

If the Keystore key is lost or invalidated, ASEH fails closed. It never creates
a replacement key and treats existing ciphertext as successfully decrypted.
The user may delete the inaccessible local store or restore data from a
separately created encrypted export.

### `.asehcase` export envelope

An export is assembled in memory or through an encrypted stream. ASEH does not
write the unencrypted export payload to a temporary file. The binary format is:

1. eight ASCII bytes `ASEHCASE`;
2. a 32-bit unsigned big-endian canonical-header length;
3. an RFC 8785 canonical JSON header encoded as UTF-8; and
4. AES-256-GCM ciphertext followed by its 128-bit tag.

The header contains only format metadata: envelope version, payload-schema
version, cipher identifier, KDF identifier and version, KDF parameters, random
salt, random nonce, and plaintext byte length. It contains no person, case,
recipient, or subject label. The exact magic bytes, header-length bytes, and
canonical header bytes are AAD, so version, KDF parameters, salt, nonce, and
payload metadata are authenticated.

For version 1, the export key is derived from the exact UTF-8 bytes of the
user-entered passphrase with Argon2id version 1.3 (`0x13`):

- 16-byte random salt;
- 64 MiB memory;
- three iterations;
- parallelism of one; and
- 32-byte output.

The implementation uses a maintained Argon2id library with native and Android
test coverage; it does not implement Argon2 itself. Passphrases are not Unicode
normalized, trimmed, logged, retained, or saved. The UI requires confirmation
when creating an export. Every export gets a new salt and a new 96-bit GCM
nonce, even when the same passphrase and payload are reused.

The importer checks the magic, integer bounds, supported versions, and KDF
resource limits before allocating memory or attempting decryption. Version 1
accepts only a 16-to-64-byte salt, 32-to-256 MiB memory, two-to-ten iterations,
parallelism one-to-four, a 96-bit nonce, and a 128-bit tag. New algorithms or
parameters require a new envelope version and an ADR; weakening the version 1
floor is not a compatibility mechanism.

### Rotation and deletion

Local DEKs have monotonically increasing key versions. Rotation creates and
wraps a new DEK before any record changes. New writes use the new version;
existing rows are decrypted and re-encrypted one transaction-sized batch at a
time. Each batch is authenticated and read back before progress is committed.
The old wrapped DEK remains available until every row and interrupted-rotation
recovery test passes, then is deleted from live storage. Process death may
resume the same rotation but may not silently discard either key.

An export key is never rotated in place. A changed passphrase creates a new
export with a new salt and nonce. Deleting sensitive data removes live rows,
derived app-managed artifacts, wrapped DEKs no longer needed for recovery, and
temporary encrypted staging files. ASEH cannot delete copies the user already
sent or saved.

## Alternatives considered

- **Plain Room protected only by Android file permissions:** rejected because
  it leaves high-consequence records readable in the database and in many
  forensic or accidental-copy scenarios.
- **One device-bound key for local storage and exports:** rejected because an
  exported file would not be portable and loss of the device key would make
  the export useless.
- **PBKDF2 as the export KDF:** rejected for version 1 because a
  memory-hard password KDF raises the cost of offline guessing.
- **Unauthenticated encryption or encryption without authenticated metadata:**
  rejected because it permits undetected header and context substitution.
- **Deterministic nonces:** rejected because rollback, restore, concurrency,
  and implementation defects can repeat a nonce under the same GCM key.

## Consequences

Sensitive fields cannot be queried with ordinary Room columns or FTS. Features
must decrypt the smallest necessary records after authorization and keep
plaintext lifetimes short. The envelope leaks approximate database size and
record count, but not record contents or labels.

Argon2id makes export and import intentionally expensive and adds a reviewed
crypto dependency. Low-memory-device behavior must be tested at API 26. The
design does not defend against an attacker controlling the unlocked process,
screen, input method, or accessibility service; the product disclosure and
secure-display controls remain required.

## Verification

- Run AES-GCM and Argon2id known-answer tests on API 26 and the current target.
- Prove that repeated encryptions never intentionally reuse nonces and that a
  test RNG that repeats a nonce is detected and fails the operation.
- Flip every header field, AAD component, nonce, ciphertext segment, and tag;
  every mutation must fail authentication without returning plaintext.
- Test wrong, empty, non-ASCII, combining-character, and very long
  passphrases, including confirmation and exact-byte behavior.
- Reject truncated files, integer overflows, unknown versions, and KDF
  parameters outside the accepted resource bounds before decryption.
- Interrupt DEK rotation before, during, and after a batch and confirm that all
  records remain readable with the correct recorded key version.
- Inspect Room files, DataStore, logs, crash output, backups, diagnostics,
  screenshots, fixtures, and exports for plaintext case data and keys.
- Exercise Keystore invalidation, deletion, backup/restore, process death, and
  no-space failure without data being reported as successfully decrypted.
