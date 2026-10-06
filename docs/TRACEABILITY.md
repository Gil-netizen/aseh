# ASEH specification authority map

**Status:** Approved\
**Approved by:** Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2)\
**Source:** `ASEH_full_product_and_build_spec.md`, Step 0 acceptance scope

This map assigns one primary repository document to each requirement group in
specification sections 3, 4, 11, 13, and 15. Supporting documents may repeat a
constraint for implementation or release use. When summaries diverge, the
primary authority named here controls until a reviewed decision updates both.

| Specification requirement | Primary authority | Supporting implementation or gate documents |
|---|---|---|
| 3.1–3.2, 3.6–3.10 Product constitution: orientation, embodiment, individual/community, Israel/diaspora, offline core, and no simulated infallibility | [PRODUCT.md](PRODUCT.md) | [METHOD.md](METHOD.md), [ACCESSIBILITY.md](ACCESSIBILITY.md) |
| 3.3–3.5 Binding five-layer method, source distinctions, scope, and leniency | [METHOD.md](METHOD.md) | [PRODUCT.md](PRODUCT.md), [EDITORIAL_POLICY.md](EDITORIAL_POLICY.md) |
| 4.1–4.3 Product capabilities, limits, and high-consequence workflow | [PRODUCT.md](PRODUCT.md) | [EDITORIAL_POLICY.md](EDITORIAL_POLICY.md), [THREAT_MODEL.md](THREAT_MODEL.md) |
| 11.1–11.4 Provenance, acquisition, pack format/types, and YAML/Markdown plus JSON Schema authoring | [SOURCE_POLICY.md](SOURCE_POLICY.md) | [DATA_LICENSES.md](DATA_LICENSES.md), [ADR-0004](adr/0004-app-pack-and-release-signing.md) |
| 13.1–13.5 AI role, offline/connected modes, retrieval, answer structure, citation verification, claim flags, and review bundles | [METHOD.md](METHOD.md) | [PRODUCT.md](PRODUCT.md), [ADR-0002](adr/0002-provider-boundary-and-openai-responses.md) |
| 13.6 Provider-key security and outbound disclosure | [PRIVACY_MODEL.md](PRIVACY_MODEL.md) | [THREAT_MODEL.md](THREAT_MODEL.md), [ADR-0001](adr/0001-sensitive-storage-and-case-export-cryptography.md), [ADR-0002](adr/0002-provider-boundary-and-openai-responses.md) |
| 15.1 Privacy posture and data minimization | [PRIVACY_MODEL.md](PRIVACY_MODEL.md) | [THREAT_MODEL.md](THREAT_MODEL.md), [ADR-0003](adr/0003-database-and-data-class-separation.md) |
| 15.2–15.3 Threats and mandatory security controls | [THREAT_MODEL.md](THREAT_MODEL.md) | [PRIVACY_MODEL.md](PRIVACY_MODEL.md), [RELEASE.md](RELEASE.md), [ADR-0003](adr/0003-database-and-data-class-separation.md) |

Decisions intentionally left open are recorded in
[OPEN_QUESTIONS.md](OPEN_QUESTIONS.md). That register cannot weaken a
non-waivable privacy, security, source-integrity, or high-consequence gate.

## Active implementation trace

This table links bounded implementation slices to their controlling approved
requirements. It does not replace the authority assignments above.

| Slice | Product and policy requirements | Architecture and implementation record | Verification boundary |
|---|---|---|---|
| [Issue #5: offline Now civil context](https://github.com/Gil-netizen/aseh/issues/5) | `PRODUCT.md`: Now date/context capability, inspectable context, and offline core; `PRIVACY_MODEL.md`: local time/place processing and separate review for new permissions; `ACCESSIBILITY.md`: RTL/LTR, semantics, 200% text, and explicit no-location behavior | [Accepted ADR-0006](adr/0006-manual-app-composition.md) supplies the application-owned time source; [ExecPlan 0003](plans/0003-offline-now-context.md) bounds the slice to civil date, time, weekday, IANA zone, no-location status, and manual refresh | Fixed-clock unit and Compose tests; no `INTERNET` or location permission; API 26/API 37 offline launch; English LTR, Hebrew-locale RTL, TalkBack order, 48 dp refresh, and 200% text evidence |
| [Issue #7: manual offline place context](https://github.com/Gil-netizen/aseh/issues/7) | `PRODUCT.md`: inspectable Now context and offline core; `PRIVACY_MODEL.md`: locally calculated time/place context persisted only by user choice; `ACCESSIBILITY.md`: RTL/LTR, mixed-script values, semantics, 200% text, and 48 dp targets | [Accepted ADR-0003](adr/0003-database-and-data-class-separation.md) places small non-sensitive preferences in DataStore; [Accepted ADR-0006](adr/0006-manual-app-composition.md) keeps persistence application-owned; [ExecPlan 0004](plans/0004-manual-place-context.md) bounds the slice to one explicitly entered place and civil-zone interpretation | Synthetic repository/model/Compose tests; corrupt data fails closed; no `INTERNET` or location permission; API 26/API 37 offline editor, save/cancel/clear, recreation/process restart, English LTR, Hebrew-locale RTL, TalkBack, 48 dp targets, and 200% text evidence |
| [Issue #9: device location and useful offline Now](https://github.com/Gil-netizen/aseh/issues/9) | `PRODUCT.md`: location/Hebrew date/current zmanim and manual correction; `PRIVACY_MODEL.md`: one-shot foreground location, local processing, confirmation before persistence, denial fallback; `ACCESSIBILITY.md`: structured no-location/calculation-uncertainty states | [Proposed ADR-0007](adr/0007-foreground-location-and-local-calendar.md) selects framework-only foreground location, ordered provider fallback, MSL-only elevation, and the wrapped local calendar boundary; [ExecPlan 0005](plans/0005-device-location-and-useful-now.md) makes GPS primary and manual coordinates advanced | Provider/policy, migration, eight frozen Hebcal cross-implementation cases, calendar/zmanim, lifecycle/sunset refresh, and Compose state tests; full app API 26 33/33 and API 37 34/34 at 200% font; database 7/7 on each; focused airplane-mode location client API 26 2/2 and API 37 3/3; manual API 37 system-permission allow, candidate preview, confirm, force-stop, and persisted relaunch. Denial/error UI is covered with injected states; no automated system denial-dialog claim. |
