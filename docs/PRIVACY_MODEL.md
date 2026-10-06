# ASEH Privacy Model

**Status:** Approved baseline; device-location amendment under review\
**Baseline approved by:** Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2)\
**Current amendment:** Foreground device-location additions are under review in [pull request #10](https://github.com/Gil-netizen/aseh/pull/10)\
**Applies to:** The Android app, ASEH-authored content packs, exports, and optional connected AI\
**Review when:** A new data type, provider, permission, account system, synchronization feature, or telemetry system is proposed

## Privacy posture

ASEH is local-first. The public alpha has no account requirement, advertising, sale of data, or third-party analytics. Prayer, practice, household, community, and case-preparation data remain on the device unless the user deliberately exports them or invokes an optional network feature.

Offline use is the normal operating mode. Installing the app, reading installed sources, composing liturgy, running rules, searching the corpus, preparing a case, and generating a print packet must not require an ASEH server or an AI provider.

Privacy is part of religious and civic safety. A convenient default must never expose a person's practice, health, sexuality, family circumstances, finances, aid needs, disputes, conversion, or community participation.

## Data inventory and default handling

| Data | Examples | Default location | Leaves the device only when |
|---|---|---|---|
| Public corpus data | Editions, citations, practice cards, liturgy, pack metadata | Signed, read-only pack storage | The user downloads an update or opens an external source link |
| App preferences | Language, theme, accessibility, selected practice profile | Local preferences storage | The user includes them in an explicit export |
| Time and place context | Civil/Hebrew date, coarse or precise location, optional mean-sea-level elevation | Calculated locally; persist only what the user chooses | The user deliberately shares an export or provider request containing it |
| Personal and household records | Checklists, adopted practices, review dates, charters | Local operational database | The user previews and confirms an export |
| Sensitive case records | Medical, fertility, pregnancy, marriage, divorce, conversion, vows, abuse, death, burial, accusations, monetary disputes | Encrypted local sensitive-record store | The user creates an encrypted case packet or explicitly sends selected context to a provider |
| Community operational records | Roles, schedules, public procedures, and non-sensitive adopted decisions | Local operational database in the alpha | A user with authority creates a reviewed export |
| Sensitive community records | Aid requests, dispute facts, accusations, safeguarding matters, vulnerable-person records, and private decision evidence | Encrypted local sensitive-record store | An authorized user creates a previewed encrypted export or explicitly sends selected context to the pinned provider |
| Provider configuration | Model, excerpt limit, and history preference | Local preferences storage | Used to make the requested provider connection |
| API credentials | OpenAI API key or session-only credential | Android Keystore-backed encrypted storage, or memory only | Sent in an authorization header only to the pinned OpenAI origin after local consent |
| Provider conversation material | User prompts, selected excerpts, payload previews, generated answers, citation drafts, and conversation history | Memory for the active session by default; encrypted sensitive-record store only when the user explicitly enables local history | The user confirms the exact outbound payload for a provider request or deliberately exports selected records |
| Diagnostics | Local error details and release/build metadata | Local and minimized | The user explicitly exports a diagnostic bundle after preview |

The alpha does not collect behavioral analytics, advertising identifiers, contacts, call history, unrelated files, or background location. A future feature that needs a new Android permission requires a privacy review and a documented, just-in-time explanation before implementation.

### Foreground device location review

Issue #9 permits a foreground-only, user-initiated device-location action for
local Hebrew-date and solar calculations. Before Android's permission dialog,
ASEH explains that it requests one current fix, sends nothing away, does not run
in the background, and saves nothing until the user confirms the candidate.

The app declares coarse and fine foreground location so Android can offer
approximate or precise access. It accepts approximate access, does not repeatedly
request an upgrade, and remains usable after denial. It declares no background
location or location foreground service. It uses no geocoder, map SDK, IP lookup,
analytics, or network request. The confirmation view shows accuracy when Android
provides it and the device time zone; coordinates are available under technical
details and can be changed or cleared. A location fix never silently selects a
city, time zone, Israel/diaspora profile, practice profile, or community.

Acquisition may fall back between enabled Android framework providers within one
bounded request: network then GPS for coarse access, and GPS then network for fine
access. This does not grant the app Internet access or transmit the fix. Elevation
is minimized and datum-safe: the app ignores ordinary ellipsoid-relative altitude,
stores only an API 34+ MSL value when Android explicitly supplies one, and otherwise
stores no elevation and discloses the sea-level calculation fallback.

## Optional connected AI

Connected AI is opt-in and BYOK: the user supplies an API key and model, pays the provider directly, and may use ASEH without either. The first alpha adapter connects directly from the device only to the normalized origin `https://api.openai.com:443` and the OpenAI Responses endpoint. The origin is built into the adapter and is not user-editable. Any future origin change is a new provider configuration that requires a new credential, a new disclosure, and fresh consent; an existing credential is never reused at a changed origin.

Before first use, ASEH must show:

- the fixed provider origin and user-selected model;
- the selected local source excerpts and case context that will leave the device;
- whether conversation history is included;
- the configured excerpt-size limit;
- a reminder that the provider's terms, retention, and billing apply; and
- a clear cancel action that returns to a fully local workflow.

Provider requests must:

- contain only validated retrieved excerpts and the minimum user context required for the request;
- set `store: false` for OpenAI Responses requests;
- disable provider web search, file search, code execution, and other remote tools;
- never attach the local corpus, an entire case record, unrelated history, or hidden diagnostics;
- send authorization only to `https://api.openai.com:443` over TLS, reject cleartext and userinfo, disable redirects, and never forward authorization across an origin boundary; and
- remain outside backups, logs, screenshots, test fixtures, crash reports, analytics, exports, and repository secrets.

ASEH buffers generated output until local claim-to-citation checks pass. Provider output is untrusted data and never becomes canonical content or an approved religious conclusion automatically.

Conversation history is excluded from provider requests and is not persisted by default. When a user enables local history, ASEH stores the prompt, generated answer, citation state, and the exact selected context in the encrypted sensitive-record store; it creates no plaintext full-text index, excludes the store from backup, and applies the same deletion controls as case data. Turning history off stops new persistence and offers immediate deletion of existing history. Provider retention remains governed by the provider even when ASEH sends `store: false`, and the disclosure says so.

### Direct-BYOK alpha risk acceptance

Direct on-device BYOK is an explicit public-alpha tradeoff. It avoids an ASEH account and proxy that could observe requests, but it cannot protect a key from a compromised or rooted device, a malicious keyboard/accessibility service, a tampered build, or compromise of the pinned provider endpoint or its TLS trust path. Mobile applications also cannot guarantee that a persistent secret is permanently non-extractable.

The UI must state this limitation before saving a key and recommend a dedicated, revocable provider project key with a short lifetime, a hard spending limit, and no unrelated privileges. Users can instead choose a session-only key that is held in memory and discarded when the session ends. Persistent keys must support one-tap deletion.

This risk acceptance applies only to the public alpha. A stable release must revisit the architecture and document whether direct BYOK remains acceptable.

## High-consequence privacy

Marriage, divorce or annulment, conversion, vows, complex monetary disputes, medical danger, fertility and pregnancy, abuse, death, burial, and public accusations use `CasePreparation`, never `DirectAnswer`.

Case preparation stores the minimum facts needed to identify questions, sources, missing evidence, safe preparatory steps, and appropriate human participants. It must not produce a document that claims legal, medical, civil, denominational, or communal effect. A local audit trail records only event type, time, result, content digest, and the local record identifier; it contains no case narrative or prompt. Audit records live in the encrypted sensitive-record store. Deleting a case deletes its audit records, and deleting all user data deletes every audit record.

Sensitive case records use a data-encryption key protected by Android Keystore. A `.asehcase` export is encrypted with a user-entered passphrase that is never stored. The export screen shows included fields and recipients, permits field removal, and requires an explicit final action. Unencrypted sensitive exports are not offered.

## Storage, backup, display, and deletion

- Android Keystore protects persistent provider keys and wraps keys used for sensitive local data. No secret is stored in Room or DataStore plaintext.
- Secrets and sensitive databases are excluded from Android backup by default. Backup behavior is verified on every supported Android API level.
- Authorization headers and secret-shaped values are redacted at the network and logging boundaries. Release builds do not log request bodies containing user context.
- Credential entry, case, history, and outbound-payload preview screens enable Android secure display by default and do not offer an in-app override in the public alpha. This blocks app screenshots, screen recording, and app-switcher previews where Android enforces the flag. Notifications contain no case content by default.
- Normal app data and sensitive records have separate deletion controls. Deleting a provider key is immediate; deleting local records removes the live records, their minimal audit records, and every app-managed index or cache derived from them.
- ASEH cannot erase copies the user exported, printed, or sent to a provider. The confirmation screen explains this before disclosure.

## User controls

The Settings privacy screen must let the user:

1. inspect locally stored categories and their approximate size;
2. delete provider keys, provider configuration, conversation history, case records, or all user data;
3. choose session-only or Keystore-backed credential storage;
4. control history inclusion and maximum excerpt size, with persistence and transmission off by default;
5. preview every user-initiated export;
6. verify that secure display is active on sensitive workflows; and
7. verify that ordinary app use still works with networking disabled.

Defaults are visible and reversible. A community profile or content pack cannot silently weaken a user's privacy settings.

## Privacy release gates

A release is blocked unless tests and human review establish that:

- core workflows work in airplane mode;
- the build contains no tracker or advertising SDK and performs no unexplained network calls;
- credentials, authorization headers, prompts, and case data are absent from logs, backups, fixtures, crash output, screenshots, app-switcher previews, and exported diagnostics;
- OpenAI requests set `store: false`, target only `https://api.openai.com:443`, reject redirects, and never reuse a credential after an origin change;
- the user sees and can cancel the exact outbound payload before first provider use;
- high-consequence flows return case preparation and encrypted export only;
- history is opt-in, encrypted when persisted, absent from plaintext indexes and backups, and covered by deletion controls;
- deletion controls remove live records, derived indexes and caches, and the related minimal audit records; and
- backup, restore, export, screenshot, notification, and process-death scenarios have been exercised.

No exception can waive credential secrecy, exact-origin authorization, TLS, `store: false`, payload preview, local fail-closed high-consequence routing, encrypted sensitive storage and export, secure display on sensitive screens, deletion correctness, or signed artifact and pack verification. These are non-waivable release requirements. A dated ADR, threat-model update, release-note disclosure, named owner, and explicit human approval may accept only a residual risk outside that set, with containment and a removal or review date.
