# ADR-0002: Provider boundary and OpenAI Responses adapter

- **Status:** Accepted
- **Accepted:** Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2)
- **Date:** 2026-10-06
- **Owner:** Gil (`@Gil-netizen`); future re-review roles: AI safety, privacy, and Android network maintainers
- **Decision scope:** `data:ai`, provider configuration, outbound payloads, answer verification, high-consequence routing
- **Supersedes:** none
- **Related:** [Privacy model](../PRIVACY_MODEL.md), [Threat model](../THREAT_MODEL.md), [Method](../METHOD.md), [Editorial policy](../EDITORIAL_POLICY.md)

## Context

Connected AI is optional, user-funded BYOK. It is a research and explanation
layer over the installed corpus, not a canonical source or decision maker. A
provider request can disclose user context, retrieved source text, and a
credential. A provider response can contain invented citations, prompt
injection, or an unsafe answer to a high-consequence question.

The first real adapter uses OpenAI Responses directly from the device. The
alpha therefore needs a provider boundary that is narrower than a generic
OpenAI-compatible client and that fails closed before any network request or
rendered answer.

## Decision

### Fixed official origin and transport

The first adapter has the immutable provider identifier `openai-responses-v1`
and the exact origin `https://api.openai.com:443`. Its only request endpoint is
`/v1/responses`. The adapter does not accept a user-supplied base URL, alternate
scheme, alternate port, URL user information, or provider alias. Local and
custom OpenAI-compatible endpoints are separate future adapters and require a
new security review.

URL construction starts from the constant origin and a constant relative path;
it never resolves a URL returned by content or the provider. Immediately before
sending, the network layer canonicalizes and compares scheme, ASCII host, and
effective port to the fixed origin. It rejects cleartext and mismatches.
OkHttp redirect following is disabled for both ordinary and TLS redirects; any
3xx response is a terminal, sanitized error. The authorization interceptor runs
only after the origin check and never retries against another URL.

Android's reviewed platform trust store validates TLS. Static certificate or
SPKI pinning is not used in this ADR because it would add a separate certificate
rotation failure mode; `Network Security Config` still disables cleartext for
release builds. Authorization and request bodies are redacted at the logging
boundary, and release builds have no body-logging interceptor.

### Local gate before disclosure

Before provider use, local code must complete, in order:

1. intent and risk classification;
2. local lexical/graph retrieval;
3. exact edition and citation resolution;
4. minimum-excerpt selection and size enforcement; and
5. the user-visible outbound-payload preview required by the privacy model.

The classifier returns `Ordinary`, a named `HighConsequence` category, or
`Unknown`. An exception, timeout, missing classifier asset, ambiguous result,
or unknown category becomes `Unknown`; it never defaults to `Ordinary`.
`HighConsequence` and `Unknown` bind the request and result to
`CasePreparation`. Provider text cannot change that binding or produce a
`DirectAnswer`. If required source IDs, edition metadata, or consent state are
missing, no request is sent.

### Responses request contract

The adapter sends a fresh `POST /v1/responses` request. It does not use a
server-side conversation, `previous_response_id`, remote file, vector store, or
provider-held corpus. The body always includes:

```json
{
  "store": false,
  "stream": false,
  "text": {
    "format": {
      "type": "json_schema",
      "name": "aseh_response_v1",
      "strict": true,
      "schema": {}
    }
  }
}
```

The actual schema replaces the empty object above. It has a fixed version,
requires every property, sets `additionalProperties` to `false` at every object
level, restricts result kind to the locally chosen kind, and restricts citation
references to immutable IDs included in that request. The request contains only
the chosen model, fixed ASEH instructions, the user's confirmed minimum context,
and delimited retrieved excerpts. Retrieved passages and user text are labeled
as untrusted quoted data.

The schema preserves eight ordered semantic sections: short answer or preparation
scope; explicit source findings; Rambam/Geonic understanding; necessary
inferences; material disagreements; bounded practical next steps; exact
citations; and confidence/limitations. Each factual or normative claim includes
its immutable citation IDs and an `explicit`, `inferred`, or `unsupported`
relation. Missing applicable sections or free-typed citation identities fail
schema or local verification.

The adapter omits `tools` and all tool-choice fields. It does not enable web
search, file search, code execution, computer use, image generation, remote MCP,
or function calling. It also omits background execution and remote include
expansions. A response containing a tool call, unexpected output item, or an
unknown schema version fails closed.

Requests are non-streaming for the alpha. The complete provider result is held
off-screen until local code has:

1. parsed it against the exact strict schema;
2. confirmed the locally bound result kind;
3. resolved every citation ID to the exact installed pack and edition;
4. mapped every factual or normative claim to one or more supplied excerpts;
5. removed or rejected unsupported claims; and
6. rendered the required confidence and limitation fields.

No partial model output is rendered before those checks. Failure yields a
local error or a local `CasePreparation` shell with no model-authored conclusion;
it never falls back to showing raw provider text.

The rendered answer lets the user flag a claim. Claim flags and review bundles
are assembled locally from the verified structured result, source and edition
IDs, verifier outcome, and app/pack versions. Export requires a field-level
preview and never includes provider credentials, authorization headers,
unselected history, unrelated case data, or hidden diagnostics.

### Credentials, retries, and provider records

The user may supply a memory-only session key or a persistent credential
protected through Android Keystore under ADR-0001. A credential is never stored
in Room or DataStore plaintext. The setup screen displays the fixed origin and
selected model, explains provider billing and retention rules, and recommends a
dedicated, revocable, expiring, spend-limited project key.

ASEH sends the key only in the authorization header after the exact-origin
check. Connectivity tests send no case data or corpus excerpt. There is no
automatic retry after a request may have reached the provider; OkHttp connection
retry is disabled for this client, and a retry that can incur cost requires a
visible user action. Provider response IDs are not used as future context and
are not persisted by default. Sanitized errors never echo headers, prompts,
excerpts, case data, or raw response bodies.

## Alternatives considered

- **Generic configurable OpenAI-compatible base URL in the first adapter:**
  rejected because it broadens credential and transport trust before the exact
  official-origin path is proven.
- **Allow redirects but strip authorization cross-origin:** rejected because a
  redirect is unnecessary for this fixed API contract and complicates audit and
  retry behavior.
- **Provider-side retrieval or web/file tools:** rejected because it bypasses
  ASEH's edition allowlist, outbound preview, offline corpus, and citation
  verifier.
- **Render streaming text while verifying later:** rejected because unsafe or
  unsupported text would already have reached the user.
- **Let the model classify high-consequence requests:** rejected because prompt
  injection or provider failure could select the less restrictive product path.

## Consequences

The initial adapter cannot serve Azure OpenAI, proxies, local models, or custom
OpenAI-compatible hosts. Those remain possible through distinct adapters with
their own origin, authentication, retention, and contract tests.

Buffering increases latency and removes token-by-token feedback. Local strict
verification can reject a response that looks useful. These costs preserve the
product's fail-closed safety boundary. `store: false` is a request setting, not
a promise that provider abuse monitoring or applicable terms retain no data;
the UI and release notes must say so.

## Verification

- Capture requests with a synthetic key and assert exact scheme, host, port,
  path, method, `store: false`, `stream: false`, strict `text.format`, and absence of tools,
  files, conversations, and unrelated history.
- Test same-origin, cross-origin, scheme-changing, port-changing, protocol-
  relative, user-info, Unicode-host, and redirect responses; none may receive
  or forward authorization except the exact fixed request origin.
- Verify release logging and exception paths redact keys, authorization,
  prompts, excerpts, case data, and raw bodies.
- Exercise every high-consequence category plus oblique, multilingual,
  misspelled, adversarial, classifier-error, and unknown inputs; none may
  produce `DirectAnswer`.
- Reject malformed JSON, extra properties, unknown output items, tool calls,
  wrong result kinds, missing IDs, fabricated IDs, wrong editions, and claims
  unsupported by the request's excerpts.
- Confirm no provider output appears before all schema and citation checks pass.
- Verify airplane-mode prayer, calendar, content, search, and case-preparation
  paths remain functional with no key or provider configured.
