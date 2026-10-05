# ASEH Threat Model

**Status:** Proposed for human approval\
**Method:** Asset and trust-boundary review with misuse cases\
**Review when:** A provider, pack format, deep link, export, sync service, permission, cryptographic design, or release path changes

## Security objectives

ASEH must preserve:

- **Confidentiality:** private practice, household, community, case, and credential data do not leave the device without a deliberate user action.
- **Integrity:** installed sources, citations, rules, liturgy, practice content, releases, and generated claims can be traced to the exact reviewed material.
- **Availability:** core prayer, source, search, rule, and export workflows remain usable without a network or provider.
- **Safety and agency:** the product does not turn an unsafe inference, malicious source, community preference, or AI output into an authoritative default.

The threat model assumes ordinary Android platform protections are available. It does not promise confidentiality on a rooted or actively compromised device, against a person who controls the unlocked device, or after data has been deliberately exported to another system.

## Assets

1. Provider API credentials and signing keys.
2. Sensitive case, practice, household, community, aid, and dispute records.
3. Source and citation integrity, including exact edition identity.
4. Rules, liturgy, practice cards, editorial states, and human approvals.
5. Installed pack and application update integrity.
6. User intent, consent, religious agency, and accurate recognition boundaries.
7. Release provenance and the project signing identity.

## Trust boundaries

```text
Untrusted network/provider
          |
          | TLS, minimum reviewed request, untrusted response
          v
+---------------- Android application ----------------+
| Network adapter | local verifier | UI/export preview |
|-----------------|----------------|-------------------|
| Keystore        | sensitive DB   | operational DB    |
|-----------------|----------------|-------------------|
| signed read-only content packs and search indexes    |
+------------------------------------------------------+
          ^                         |
          | verified pack import    | explicit encrypted export
          |                         v
Untrusted pack/file/deep link   User-chosen recipient/storage
```

Source passages, imported notes, pack metadata, provider responses, deep-link parameters, filenames, and exported templates are data. None are instructions to the application or to an AI system.

## Threat actors and failure sources

- an attacker distributing a modified app, pack, update, link, or export;
- a compromised dependency, build runner, developer account, or release credential;
- a remote provider, network observer, or malicious provider endpoint;
- hostile text embedded in a source, note, translation, or provider response;
- another person with physical access to the device, notification surface, screenshot, or backup;
- a community administrator exceeding a legitimate role;
- an editor, automation, or model making a confident but unsupported claim;
- coercive product defaults that disguise a community choice as universal law; and
- accidental disclosure through logging, debugging, fixtures, clipboard, or broad export.

## Threats, controls, and required evidence

| Threat | Primary controls | Release evidence |
|---|---|---|
| Malicious or corrupted content pack | Ed25519 signature verification, per-file SHA-256 checksums, canonical manifest, strict versioned schemas, dependency and compatibility checks, no executable content, atomic install | Valid/tampered/wrong-key fixtures; rollback test; deterministic manifest comparison |
| Archive traversal, overwrite, or decompression bomb | Reject absolute and parent paths, links, duplicate destinations, excess entries, excess expanded size, and abnormal compression ratios; extract to an isolated temporary location before validation | ZIP traversal, symlink, duplicate-path, oversized, and bomb fixtures |
| Prompt injection in source text or notes | Delimit retrieved material as quoted data; fixed system policy; closed provider tool set; no provider web/file/code tools; local intent and risk classification; adversarial fixtures | Injections cannot change provider host, reveal secrets, bypass `CasePreparation`, or create an approved claim |
| Provider disclosure beyond user intent | Local retrieval, minimum excerpts, payload preview, history excluded by default, size limit, TLS, fixed OpenAI origin, redirect denial, `store: false` | Captured contract tests confirm body, headers, origin, redirect behavior, and omitted data |
| API-key extraction | Session-only option; Keystore-backed encryption; no plaintext database, backup, log, export, fixture, screenshot, or repository storage; one-tap revocation guidance | Static/secret scan, backup inspection, log inspection, deletion and root-risk UX tests |
| Authorization header leakage or redirect | Network-layer redaction; disable redirects; pin the alpha adapter to normalized `https://api.openai.com:443`; reject cleartext, userinfo, Unicode lookalikes, trailing-dot variants, and non-default ports | Hostile URI, Unicode/trailing-dot, port, redirect, cross-origin, and logging tests; Network Security Config review |
| False citation or silent edition substitution | Immutable source and edition IDs, exact pack version, citation resolver, claim-to-excerpt verifier, rendered attribution, fail-closed missing references | Every rendered claim opens its exact installed source; edition mismatch fixtures fail |
| High-consequence answer presented as a ruling | Local fail-closed classification before any provider call; `CasePreparation` result type; no operative document; immediate offline urgent-danger route; clear recognition limits; named human review; provider output cannot downgrade the route | Hebrew, English, mixed-script, euphemism, multi-turn, quoted/imported, injection, and ambiguous fixtures always route safely |
| Malicious deep link or intent | Allowlisted routes and parameters, size limits, canonical parsing, no privileged action on open, confirmation before import/provider/export | Fuzz, malformed URI, oversized parameter, and privilege-boundary tests |
| Unsafe exported template or macro | Data-only formats, JavaScript-disabled and network-blocked print renderer, escape all user content, encrypted sensitive archives | Script/HTML injection fixtures; rendered output makes no network requests |
| Backup, screenshot, notification, or app-switcher leakage | Backup exclusion, mandatory secure display on credential, case, history, and payload-preview screens, redacted notifications, no sensitive recents preview, short clipboard exposure | API 26 and current-target backup/restore, screenshot, screen-recording, notification, recents, and process-death tests |
| Community administrator overreach | Separate editorial and administrative roles in future services, least privilege, local private records, reviewable audit trail, export authority checks | Role matrix and denied-operation tests before any sync ships |
| Supply-chain or CI compromise | Pinned toolchain and dependencies, lockfiles and verification metadata, minimal CI permissions, secret/code/dependency scanning, SBOM, provenance, protected release environment | Clean-clone build, dependency verification, scan reports, provenance verification |
| App or pack signing-key compromise | Separate project app-signing, upload, and pack-signing keys; protected access; encrypted offline backup; rotation/revocation runbook | Signing dry run, certificate fingerprint check, recovery drill, signer allowlist test |
| Coercive or misleading defaults | Visible source layer and editorial status, dissent and alternatives, reversible adoption, no content pack may silently change personal settings | Content review confirms labels, reversal path, and absence of hidden adoption |

## Connected-AI boundary

OpenAI BYOK is optional and direct from the device in the alpha. Before any network call, local code classifies intent, retrieves sources, resolves editions, and selects the minimum excerpts. The request uses strict Structured Outputs through `text.format`, `store: false`, a bounded response size, explicit cancellation and timeouts, and no web, file, code, or other provider tools. The response is untrusted until local schema and citation verification succeeds; provider output cannot change the locally selected risk route.

The alpha credential is scoped to normalized `https://api.openai.com:443`. Redirects are denied rather than followed. Any future provider-origin change requires a separately stored credential and fresh user consent. Provider errors are displayed without echoing request bodies or secrets. Cancellation is immediate where the transport permits it, requests time out, and no paid request is retried automatically.

Direct BYOK does not defend against a compromised device or build. The app discloses that residual risk and recommends dedicated, revocable, spend-limited keys. A future stable release requires a new review of this accepted alpha risk.

## High-consequence boundary

The following categories always enter `CasePreparation`: marriage; divorce or annulment; conversion; vows; complex monetary disputes; medical danger; fertility and pregnancy; abuse or coercion; death and burial; and public accusations.

Classification runs locally before retrieval can disclose data or a provider call can begin. It fails closed on ambiguity and covers Hebrew, English, mixed script, euphemisms, multi-turn references, quoted or imported text, and prompt injection. An urgent-danger signal immediately presents an offline-safe route to local emergency help and trusted human support; it never waits for an AI response or lets generated output suppress the route.

The workflow may organize facts, sources, disagreements, questions, missing evidence, safe preparatory actions, and appropriate participants. It must not:

- diagnose, prescribe, determine emergency action, or delay emergency services;
- establish facts requiring testimony, examination, inspection, or adjudication;
- claim civil, denominational, communal, or court recognition;
- substitute generated text for a required court or qualified human review; or
- generate a document represented as operative.

Case data is encrypted locally. Export requires a preview, a user-entered passphrase, and an explicit final action. The local audit trail stores only event type, time, result, content digest, and local record identifier in the encrypted sensitive store. It contains no case narrative and is deleted with the case or by delete-all.

## Cryptographic and secret-management rules

- Use maintained platform cryptography; do not design custom algorithms or protocols.
- Android Keystore protects persistent provider credentials and wraps sensitive-data keys.
- Sensitive local and archive encryption follow ADR-0001: AES-256-GCM with unique nonces, authenticated versioned metadata, a reviewed passphrase KDF for `.asehcase`, explicit key rotation/version handling, and no stored passphrase.
- Application signing, Play upload signing, and content-pack signing use distinct keys and documented fingerprints.
- No private signing key or provider key is committed to the repository, embedded in an artifact, or printed in CI output.
- Key loss, compromise, rotation, and revocation procedures must be rehearsed before public release.

## Incident response and rollback

For a suspected compromise:

1. stop the affected release or pack publication path;
2. preserve non-sensitive build and provenance evidence;
3. identify affected versions, signer fingerprints, data classes, and users;
4. revoke provider or upload credentials and rotate keys where supported;
5. publish a clear advisory and a fixed, signed release;
6. revoke a pack only when it is demonstrably harmful or corrupt, with a transparent reason; and
7. preserve user-created records during app or pack rollback.

An app rollback must use a signed forward-fix release because Android does not generally permit an installed downgrade without data loss. Pack installation remains atomic and supports version pinning and rollback.

## Security review gate

The release owner blocks publication when any of the following is unresolved:

- a secret, sensitive payload, or authorization header appears in a log, backup, export, fixture, screenshot, artifact, or scan result;
- an unverified or incompatible pack can be installed;
- a citation can resolve to a different edition silently;
- high-consequence input can produce `DirectAnswer` or an operative document;
- a provider request can bypass preview, origin binding, TLS, or `store: false`;
- a release artifact lacks a verifiable signature, SBOM, provenance, or source correspondence; or
- a critical/high security, privacy, safety, accessibility, or dependency finding affects the shipped path.

The first six findings above are non-waivable, as are unencrypted sensitive data/export, incorrect deletion, or provider tools being enabled. No ADR or approval can authorize their release. A written ADR, release-note disclosure, named owner, containment, review/removal date, and explicit human approval may accept only a lower-severity residual risk outside this non-waivable set.
