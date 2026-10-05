# ASEH Release Policy

**Status:** Proposed for human approval\
**First release:** `v0.1.0-alpha.1`, public GitHub prerelease\
**Primary package:** `io.github.gilnetizen.aseh`

## Channels and scope

The first ASEH release is a public, open-source GitHub alpha. It distributes a signed universal APK for direct installation, an Android App Bundle for future channel testing, source and verification artifacts, and signed ASEH content packs.

The alpha is explicitly pre-stable. GitHub is the publication channel; Google Play submission, open beta, production, F-Droid publication, accounts, mandatory services, and synchronization are later milestones. Core behavior must remain available offline.

Build flavors are:

- `dev`: sample corpus, debug tools, and deterministic fake AI provider;
- `staging`: release-like behavior, test registries, and no production signing; and
- `prod`: approved bundled packs and production endpoints only.

An optional `foss` flavor may be added only when its dependency and feature contract is documented.

## Versioning

Application releases use Semantic Versioning with prerelease identifiers, beginning at `0.1.0-alpha.1`. Signed Git tags add the `v` prefix. The Android `versionCode` increases monotonically across every published artifact.

Content packs have independent immutable versions, schema compatibility ranges, manifest hashes, and signing identities. Rebuilding an already published app or pack version with different bytes is prohibited; corrections receive a new version.

Every published pack version also has a signed annotated Git tag named `pack/<pack-id>/v<version>`. The tag points to the source commit used by the deterministic compiler and records the pack manifest digest and Ed25519 key fingerprint. The signed Git tag establishes source history; the Ed25519 pack signature remains the runtime authenticity check.

Release notes identify:

- user-visible changes and known limitations;
- minimum and target Android versions;
- database, pack-schema, and migration compatibility;
- bundled corpus and pack versions;
- signing certificate and pack-key fingerprints;
- privacy, accessibility, source, and security changes; and
- rollback or upgrade constraints.

## Signing and key custody

ASEH uses a project-owned Android app-signing key as the permanent application identity for GitHub APKs and future Play App Signing enrollment. A distinct Play upload key is used for future Play submissions. Content packs use separate Ed25519 signing keys.

The private app-signing key has an encrypted offline backup controlled by the project owner. Its certificate fingerprint is published and checked in release automation. The upload key and pack keys are stored and rotated independently so compromise of one does not authorize another artifact class.

GitHub APK and Google Play continuity are separate recovery concerns. GitHub APK recovery must preserve a signer or supported signer lineage that Android accepts for direct upgrades on each supported API level. Play App Signing recovery applies only to Play-distributed installs. If the GitHub signing identity cannot be recovered or validly rotated, affected direct-install users must reinstall or migrate to a new package identity; release notes and the incident advisory must state the data-export and migration path before publication.

Release automation may access signing material only inside a protected GitHub `release` environment after the project owner's explicit approval. Jobs use least-privilege tokens, do not expose secrets to pull requests or forks, redact paths and values, and delete temporary key material at job completion.

No provider API key, app-signing private key, upload private key, pack private key, keystore password, or export passphrase may appear in the repository, build cache, logs, provenance, SBOM, fixtures, or release artifact.

Key fingerprints, custody, backup verification, compromise response, and recovery drill results are recorded privately by the owner and summarized without secrets in release evidence.

## Release artifacts

Every public alpha release includes:

1. signed universal APK;
2. signed Android App Bundle;
3. source archive corresponding to the tag;
4. software bill of materials;
5. build provenance/attestation;
6. corpus manifest with exact edition, license, version, and checksum data;
7. license and attribution report;
8. pack catalog, pack manifests, checksums, and signatures;
9. database and pack-schema compatibility report; and
10. release notes with known risks and verification instructions.

Artifact filenames include the application version and flavor. A checksum file covers every downloadable artifact and is signed or included in signed provenance.

## Required release evidence

The release PR contains or links to:

- a clean-clone build from the pinned toolchain and locked dependencies, plus byte-for-byte reproduction of every unsigned APK, AAB, pack, corpus manifest, SBOM, and other deterministic artifact claimed reproducible;
- compilation, unit, static-analysis, formatting, migration, dependency, secret, and code-scan results;
- offline end-to-end results;
- source/citation integrity, corpus, and license reports;
- deterministic pack build, signature verification, update, tamper, and rollback results;
- Android API 26/current-target installation, launch, update, backup/restore, and process-death tests;
- phone/tablet, Hebrew RTL, English LTR, mixed-script, 200% text, TalkBack, contrast, and print review;
- privacy and threat-model review, including captured provider contract tests with redacted synthetic data that assert strict `text.format`, `store: false`, no provider tools, minimal history, bounded output, cancellation, timeouts, redirect denial, fixed-origin authorization, redacted errors, and no automatic paid retry;
- local, pre-network high-consequence classification and `CasePreparation` tests across Hebrew, English, mixed script, euphemisms, multi-turn references, quoted/imported text, prompt injection, and ambiguous input, plus an offline urgent-danger route that cannot be overridden by provider output;
- SBOM and provenance verification; and
- human review of every stable practice card in both launch languages; and
- immutable approval records that bind content or pack SHA-256 digest, version, language, approver name, approval date, and review reference. Any byte change invalidates the corresponding approval.

Critical or high findings in the shipped path block publication. No exception can waive credential secrecy, fixed-origin authorization, TLS, `store: false`, payload preview, local fail-closed high-consequence routing, encrypted sensitive storage/export, deletion correctness, secure display on sensitive screens, or signature verification. A dated exception may cover only a lower-severity residual risk outside that set and must identify the owner, rationale, containment, release-note disclosure, and review/removal date. High-consequence practice content requires two named human approvals bound to the immutable content or pack digest.

“Clean-clone build” means that a fresh environment can resolve locked inputs, compile, test, and produce the artifact set. “Reproducible” means byte-for-byte equality for the named unsigned deterministic subjects. Signed Android artifacts may differ because of protected signing operations or timestamps; provenance must name both the reproducible unsigned subject digest and the released signed artifact digest, and verification must establish their source and signing relationship.

## Release procedure

1. Freeze the intended source commit and pack inputs; ensure the working tree and generated-source checks are clean.
2. Update version, compatibility metadata, release notes, attribution, corpus manifest, and known limitations through a reviewed PR.
3. Run all required CI and independent human review. No AI agent approves or merges its own release work.
4. Create a signed tag from protected `main`.
5. After protected-environment approval, build and sign artifacts from the tag in a fresh runner.
6. Verify signatures, certificate fingerprints, checksums, provenance subject digests, install/upgrade behavior, and source correspondence.
7. Create a draft GitHub prerelease and attach the complete artifact set and evidence.
8. The project owner reviews the concrete draft, performs the final human publication action, and records the publication time.
9. Verify the public downloads from a clean environment and keep the release marked prerelease.

Automation may prepare the PR, tag proposal, evidence, and draft. It does not bypass branch protection, protected-environment approval, or the owner's final publication action.

## Alpha-specific disclosures

The first release notes and in-app provider setup must disclose:

- connected AI is optional and uses a user-supplied key directly from the device;
- `store: false` is sent to OpenAI, while OpenAI's applicable terms and abuse-monitoring rules still govern the request;
- a compromised device or build may expose an on-device key;
- dedicated, revocable, expiring, spend-limited keys are recommended;
- high-consequence output is preparation material and has no claimed legal, medical, civil, denominational, or communal recognition; and
- no telemetry or account is required for core use.

## Rollback and incident releases

Android app rollback normally occurs through a signed forward-fix because installed downgrades can be unsafe or unavailable. The release owner can remove a broken prerelease from prominent documentation, but published artifacts and incident history remain transparent unless continued distribution creates a concrete safety risk.

Content packs install atomically, remain version-pinned, and support rollback to the last verified compatible version. A remote revocation entry is used only for demonstrably harmful or corrupt packs and includes a public reason. Pack rollback never deletes user-created records.

For a signing or supply-chain incident, stop publication, revoke affected credentials where possible, identify affected digests and versions, publish an advisory, rotate the separable key, and issue a newly signed fixed release. A Play-distributed install follows Play App Signing recovery. A GitHub direct-install release follows the documented signer-lineage recovery runbook for the supported Android API; when continuity is impossible, the advisory states that direct-install users must export recoverable data and reinstall or migrate package identity. Compromise of the permanent app-signing key receives immediate public disclosure.

## Completion rule

A release is complete only when the GitHub prerelease is public, its downloads and signatures verify from a clean environment, installation and upgrade succeed on representative devices, and the release record links all required evidence. A locally signed build or draft release is not a deployed release.
