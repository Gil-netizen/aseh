# ADR-0004: App, pack, and release signing

- **Status:** Accepted
- **Accepted:** Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2)
- **Date:** 2026-10-06
- **Owner:** Gil (`@Gil-netizen`); future re-review roles: release, Android security, and pack maintainers
- **Decision scope:** `.asehpack` compiler/verifier, Android artifacts, GitHub releases, future Play enrollment, signing-key custody and recovery
- **Supersedes:** none
- **Related:** [Source policy](../SOURCE_POLICY.md), [Threat model](../THREAT_MODEL.md), [Release policy](../RELEASE.md)

## Context

ASEH distributes executable Android artifacts and independently updateable
offline content. Users must be able to distinguish a project release from a
modified APK and a reviewed pack from an unsigned or altered archive. The first
public channel is GitHub, while future Play distribution must preserve the same
Android application identity. A compromise in one signing system must not
authorize another artifact class.

## Decision

### Deterministic signed `.asehpack`

A production `.asehpack` is a deterministic ZIP archive using the `STORED`
method for every entry, lexicographic UTF-8 path order, fixed DOS timestamp
`1980-01-01T00:00:00`, fixed file modes, no comments, and no platform-specific
extra fields. Paths use NFC-normalized UTF-8 and `/`; absolute paths, `..`,
backslashes, links, duplicates, and normalization or case-fold collisions are
rejected. The compiler precomputes size and CRC values.

The archive contains exactly one canonical `manifest.json` and one
`signature.json`. `manifest.json` is RFC 8785 canonical JSON and includes:

- pack identity and immutable version;
- pack and application schema-compatibility ranges;
- signing-key ID;
- dependencies and pack type;
- provenance, review, edition, rights, and attribution metadata; and
- for every other payload entry except `signature.json`, its normalized path,
  byte size, and lowercase SHA-256 digest.

The manifest does not list itself or `signature.json`. Its exact archived bytes
must equal its canonical encoding. The signing message is the concatenation of
the ASCII bytes `ASEH-PACK-SIGNATURE-v1`, a zero byte, and the exact canonical
manifest bytes. The pack signer creates a standard Ed25519 signature over that
message.

`signature.json` is also RFC 8785 canonical JSON. It is a strict object
containing only format version, algorithm `Ed25519`, signing-key ID, and base64
signature. The key ID is the lowercase hexadecimal SHA-256 digest of the raw
32-byte Ed25519 public key. The key ID in the signature must match the signed
manifest.

Production trust comes only from an app-bundled allowlist mapping key IDs to
the exact public keys and permitted pack scopes. A key carried by the pack,
download response, registry, or deep link is not a trust root. A remote registry
may announce a revocation with a public reason, but it cannot add a trusted key;
adding or changing trust requires an app update and review. Unsigned development
packs are accepted only by an explicitly separate debug trust configuration and
never by a production build.

The verifier, before activation:

1. enforces archive entry-count, individual-size, total-size, path, type, and
   compression limits, including rejection of every method except `STORED`,
   without writing outside isolated staging;
2. requires one manifest and signature and parses both with strict duplicate-
   key rejection;
3. reproduces the canonical manifest bytes and resolves the key ID from the
   bundled trust store;
4. verifies the Ed25519 signature before trusting manifest metadata;
5. streams every declared file, checking exact size and SHA-256, and rejects
   undeclared or missing entries;
6. validates schema compatibility, dependencies, rights and review state, and
   SQLite integrity/query-only behavior; and
7. moves the verified staged version into place and changes the active catalog
   pointer atomically.

Any failure removes only staging data and leaves the prior active pack intact.
Rollback selects a still-verified installed version and never deletes user
records.

### Separate signing identities

Four identities remain separate:

1. **Android app-signing key.** A project-owned key and long-lived X.509
   certificate are the permanent identity of package
   `io.github.gilnetizen.aseh`. Every production GitHub APK is signed with this
   key. The same existing key is used when enrolling in Play App Signing so an
   APK delivered through GitHub and an APK delivered through Play have the same
   application signer and upgrade identity.
2. **Play upload key.** After Play enrollment, a distinct key signs submitted
   AABs. It authorizes upload but is not the application identity and never
   signs a direct-install APK. Before and after enrollment, release evidence
   labels which certificate signed each AAB.
3. **Pack-signing keys.** One or more scoped Ed25519 keys sign content packs.
   They never sign an APK, AAB, Git tag, or provenance statement.
4. **Source-release identity.** Signed Git tags and GitHub provenance use the
   maintainer or platform release identity, separate from Android, upload, and
   pack private keys. Published evidence records the source tag, commit, subject
   digest, workflow, and signer identity.

The SHA-256 fingerprint of the Android certificate, Play upload certificate,
each trusted pack public key, and the applicable source-release identity is
published in release evidence. Automation compares observed fingerprints to
reviewed constants before publication.

A future Play application-signing key upgrade, Android signing lineage, package
ID change, or distribution channel that would make Play and GitHub signer
identity diverge requires a new ADR and explicit migration behavior for API 26
and later. It is not enabled as a routine console setting.

### Custody, CI, rotation, and recovery

Private app and pack keys have encrypted offline backups under project-owner
control, with recovery material kept separately from its decryption secret.
Backup readability and fingerprints are checked in a documented recovery drill
before the first public release and periodically thereafter. No private key,
keystore password, recovery secret, provider key, or export passphrase is stored
in the repository, workflow source, cache, SBOM, provenance, fixture, log, or
artifact.

Release CI can receive the minimum signing material only in a protected GitHub
`release` environment after explicit owner approval and only for a tagged
release job. Pull requests, forks, ordinary CI, and pack-verification jobs have
no private signing material. Temporary key files are permission-restricted and
removed at job completion; logs expose only public fingerprints and artifact
digests.

Recovery is deliberately independent:

- loss or compromise of the Play upload key uses Play's upload-key reset and
  does not change the app signer;
- a new pack key is first shipped as a trusted scoped public key in an app
  update, then used for later packs; a compromised pack key is revoked with an
  advisory and cannot authorize an app update;
- source tag/provenance credentials rotate through GitHub and maintainer account
  recovery without changing Android or pack trust; and
- loss or compromise of the permanent Android app-signing key stops releases
  immediately and follows available Android/Play recovery. The project does not
  silently substitute a key: if signer continuity cannot be preserved for all
  supported channels and API levels, recovery requires a public incident plan,
  a new ADR, and potentially a new application identity.

## Alternatives considered

- **One key for apps, uploads, packs, and tags:** rejected because one
  compromise would authorize every artifact class and make selective rotation
  impossible.
- **Trust the public key embedded in each pack:** rejected because an attacker
  could replace both payload and key.
- **Sign the ZIP bytes without a canonical manifest:** rejected because ZIP
  tooling differences obscure what is authorized and complicate independent
  verification. The signature instead binds canonical metadata and every
  payload digest, while deterministic ZIP rules remain independently tested.
- **Use different app signers for GitHub and Play:** rejected because Android
  would not treat the packages as a continuous upgrade identity.
- **Allow production trust-on-first-use for community packs:** rejected for the
  alpha. Third-party trust and delegation require a separate policy and ADR.

## Consequences

Using `STORED` entries produces larger packs than deflate but makes archive bytes
reproducible across supported build hosts and reduces decompression-bomb risk.
Every content change creates a new immutable pack version and signature.

The Android app-signing key is difficult to rotate, especially while supporting
API 26 and direct distribution, so custody and tested offline recovery are
critical. Pack and upload keys can rotate without changing the installed app's
identity. A production build cannot install a locally created unsigned pack.

## Verification

- Build the same fixture twice in clean environments and compare archive bytes,
  canonical manifest bytes, file order, digests, and signatures.
- Verify valid packs and reject changed manifest bytes, changed payload bytes,
  wrong keys, unknown or out-of-scope keys, altered key IDs, invalid encodings,
  duplicate JSON keys, missing/extra files, incompatible schemas, and revoked
  keys.
- Exercise ZIP traversal, absolute path, link, duplicate path, normalization
  collision, case-fold collision, excess entries, excess size, and compressed-
  entry fixtures without modifying the active install.
- Test install, interrupted staging, atomic activation, update, pinning, and
  rollback while confirming that user stores are unchanged.
- In a clean release dry run, verify the GitHub APK signer fingerprint, AAB
  signer role, signed tag, provenance subjects, checksums, and every pack key
  against published release evidence.
- Perform separate app-key backup restore, pack-key rotation/revocation, Play
  upload-key recovery rehearsal, and source-release credential rotation without
  exposing private material.
- Scan repository history, CI output, caches, artifacts, SBOM, provenance, and
  fixtures for private keys, passwords, and provider credentials.
