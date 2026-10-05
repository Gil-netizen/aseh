# ASEH Open Questions and Decision Register

Status: Active

Decision owner for v0.1: Gil, with additional named reviewers where required

This file prevents product, editorial, source, governance, and release choices from becoming silent implementation defaults. A resolved item must link to a dated decision record or ADR; editing a default in code is not a decision record.

## Locked v0.1 decisions

These choices are part of the approved implementation direction and are not open implementation questions:

- Public identity: `ASEH / עֲשֵׂה`, repository slug `aseh`, package ID `io.github.gilnetizen.aseh`.
- Code license: Apache License 2.0.
- Original ASEH editorial-content license: CC BY-SA 4.0, including commercial reuse under its terms.
- Launch languages: Hebrew RTL and English LTR.
- Architecture: native Android, local-first, with no required account or network for core use.
- Pilot edition identities: only the exact Sefaria versions listed in [DATA_LICENSES.md](DATA_LICENSES.md).
- Mechon Mamre and Mifal Mishneh Torah content: excluded unless written permission is obtained and recorded.
- Ordinary-content approval: Gil.
- High-consequence approval: Gil plus a second named human reviewer with a recorded relevant role or expertise.
- Alpha high-consequence behavior: case preparation only; no operative legal or religious document.
- Alpha disputed-practice pilot: electricity and lighting remain `DISPUTED` or `UNRESOLVED` until the evidence and conclusion receive required human review.
- Initial release: public open-source alpha distributed as a GitHub prerelease; Play distribution is later.

Changing a locked decision requires an explicit product decision and updates to every affected canonical document.

## Blocking before public corpus distribution

| ID | Question | Decision needed | Owner/evidence | Default while open |
|---|---|---|---|---|
| `RIGHTS-001` | Which exact Creative Commons version and attribution apply to each upstream `CC-BY-SA` pilot edition? | Record license URL/version and required notices per edition | Human rights review using upstream records | Do not publish the affected text in a distributable pack |
| `RIGHTS-002` | What provenance or jurisdiction note will support the public-domain determination for JPS 1917 in each release market? | Approve a manifest note and evidence | Human rights review | Do not mark the pack distribution-ready |
| `RIGHTS-003` | Are all Tanakh books available under the same exact allowlisted Hebrew and English versions? | Verify per-book version metadata and checksums | Corpus maintainer plus rights reviewer | Include only individually verified books |
| `RIGHTS-004` | Does the evidence for the narrow Rambam Order of Prayer 1:1–4 English CC0 pilot satisfy ASEH's attribution and distribution requirements? | A named human rights reviewer accepts the evidence record and required notice | [Technical evidence](source-evidence/rambam-order-prayer-en-1-1-4.md) plus upstream CC0 and Sefaria records | Use for non-distributable ingestion tests only; exclude it from public packs |
| `LITURGY-001` | Which exact edition anchors Rambam's liturgical text? | Select an anchor and define how other witnesses and reconstructions appear | Editorial decision with source dossier | The Sefaria pilot editions are research inputs, not a silently chosen canonical anchor |

## Editorial and governance questions

| ID | Question | Decision required before | Default while open |
|---|---|---|---|
| `GOV-001` | What is the durable editorial-board structure, membership, expertise standard, recusals policy, and succession process? | Any governance claim beyond the v0.1 owner/reviewer rules | Gil approves ordinary content; high-consequence content uses the two-human gate |
| `GOV-002` | What legal and organizational form will ASEH take: nonprofit, cooperative, open-source project, publisher partnership, or company? | Contracts, fundraising, employment, or organizational claims | Make no organizational-status claim |
| `GOV-003` | How is the practical authority of an ad hoc local court or community body defined and limited? | Guidance that relies on such a body's authority | Mark authority and recognition as unresolved; provide preparation only where consequences are high |
| `GOV-004` | What recognition claims may lifecycle kits make in particular civil, denominational, or community contexts? | Any lifecycle-kit release | Make no external recognition claim |
| `GOV-005` | What qualifications are required for the second reviewer in each high-consequence domain? | Approval of the first such content item | Record relevant expertise and treat the item as unapprovable until Gil accepts that reviewer |

## Product and content questions

| ID | Question | Decision required before | Default while open |
|---|---|---|---|
| `CONTENT-001` | Which corpora ship in the base app rather than optional packs? | Freezing the production base-pack manifest | Ship only the smallest approved functional core; keep larger corpora optional |
| `CONTENT-002` | Which practice positions are defaults and which are optional profiles? | A production profile selects a normative result | Leave disputed positions unselected and require explicit adoption |
| `CONTENT-003` | How are women's communal roles represented in the initial and alternative profiles? | Publishing affected service-role rules or templates | Do not imply a settled universal role policy |
| `CONTENT-004` | Does the first alpha cover nidda, marriage, conversion, and divorce beyond general source navigation and case preparation? | Adding any workflow or guide in these domains | Defer operative workflows and stable normative cards |
| `CONTENT-005` | Is Arabic a launch or near-launch language for an Israel/Palestine-facing community vision? | Committing localization and review scope | Launch with Hebrew and English; keep data models localization-ready |
| `CONTENT-006` | What factual model and editorial conclusion, if any, should govern particular uses of electricity and lighting? | Publishing an actionable stable card | Display the dossier as `DISPUTED` or `UNRESOLVED`; adoption is a separate local choice |

## Trust, pack, and release questions

| ID | Question | Decision required before | Default while open |
|---|---|---|---|
| `PACK-001` | What trust roots, review requirements, revocation process, and UI labels govern third-party community packs? | Accepting any third-party pack in production | Trust only project-signed first-party packs |
| `PACK-002` | Which material ships under the base editorial license versus a separate pack-specific license? | First mixed-license pack release | Preserve per-file and per-edition licenses; never apply a blanket content license |
| `RELEASE-001` | What exact devices and Android versions form the signed alpha acceptance matrix beyond API 26 and the accepted current target API? | Public-alpha release candidate approval | Test representative low-, mid-, and high-range devices and record the actual matrix |

## Decision-record template

When an item is resolved, add or link a decision record containing:

- question ID and date;
- decision and scope;
- owner and named reviewers;
- evidence considered, including source and rights records;
- alternatives and why they were rejected;
- consequences for content, schemas, UI, packs, and releases;
- migration or re-review work; and
- next review date or reversal criteria.
