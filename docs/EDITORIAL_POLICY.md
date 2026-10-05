# ASEH Editorial Policy

Status: Draft for human approval

Owner for v0.1: Gil

## Purpose

ASEH publishes practical guidance while preserving the difference between evidence, interpretation, present application, and local adoption. This policy defines who may move content through the editorial process and what evidence is required before publication.

The substantive method in [METHOD.md](METHOD.md), source controls in [SOURCE_POLICY.md](SOURCE_POLICY.md), and rights records in [DATA_LICENSES.md](DATA_LICENSES.md) are mandatory editorial inputs.

## Editorial states

Content moves through this state machine:

`Proposed` → `Researched` → `Drafted` → `SourceVerified` → `HumanReviewed` → `Approved` → `Published` → `UnderReview`

`HumanReviewed` may return to `Drafted`. `UnderReview` may return to `Approved` after the required review or be withdrawn from publication. A correction that changes a conclusion, scope, status, source, or required action creates a new version and review record.

This lifecycle is independent of the conclusion evidence status defined in [METHOD.md](METHOD.md). Fields such as `DISPUTED` and `UNRESOLVED` describe the evidence conclusion, while `HumanReviewed`, `Approved`, and `Published` describe review and release state. Implementations must use separate `conclusion_status` and `review_state` schema fields and enums. Approval confirms that the representation is responsible; it does not convert disagreement into certainty.

### State requirements

| State | Required evidence | Who may grant it |
|---|---|---|
| `Proposed` | Bounded question and intended user outcome | Human or AI-assisted workflow |
| `Researched` | Source dossier, edition identities, rights check, claims, and disagreements | Human or AI-assisted workflow |
| `Drafted` | Structured content using the five layers, status, scope, confidence, and citations | Human or AI-assisted workflow |
| `SourceVerified` | Independent check of every normative and historical claim against exact source units and editions | Named human source verifier |
| `HumanReviewed` | Review of substance, language, safety, dissent, and rendered presentation | Named human reviewer |
| `Approved` | All applicable gates met and an authorized approval recorded | Gil for ordinary content; high-consequence rule below |
| `Published` | Approved content included in a signed release or pack with provenance | Authorized release operator after release checks |
| `UnderReview` | A credible error, source change, rights issue, safety issue, or due review requires reassessment | Any editor may trigger; only authorized reviewers may clear |

AI may move an item only through `Proposed`, `Researched`, and `Drafted`. Automated validation can report that mechanical checks passed, but it cannot grant a human state.

## Approval authority

Gil is the v0.1 approver for ordinary content. An ordinary item cannot reach `Approved` without Gil's named approval in its review record.

High-consequence content requires two named human reviewers before `Approved`:

1. Gil, as product/editorial approver; and
2. a second human reviewer whose relevant role or expertise is recorded.

The second reviewer must assess the actual rendered content, sources, safety boundaries, and recognition language. A generic repository approval, automated check, AI review, or author self-review does not satisfy this gate. The author may participate in revision but cannot be the independent second reviewer.

High-consequence content includes marriage, divorce or annulment, conversion, vows, complex monetary disputes, medical danger, fertility and pregnancy, abuse, death, burial, public accusations, and any item that could create comparable physical, legal, financial, reputational, or status harm. When classification is uncertain, apply the high-consequence gate until a named human editor documents why it is ordinary.

The membership, credentials, and durable governance of a future editorial board remain open; see [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md). This does not weaken the v0.1 approval gate.

## Separation of duties

For substantive content, preserve these roles in the record even if one person performs more than one ordinary-content role:

- **Researcher:** builds the source dossier without deciding the conclusion.
- **Analyst:** maps claims, arguments, factual premises, exceptions, and dissent.
- **Draftsperson:** creates the practice card, guide, liturgy, rule, or template.
- **Source verifier:** checks every claim against the cited source unit and edition.
- **Adversarial reviewer:** identifies the strongest counterreadings, missing cases, and recognition risks.
- **Language editor:** edits Hebrew and English without changing substance.
- **Approver:** makes and records the human editorial decision.

The same AI session must not research, adjudicate, draft, and approve its own work. No AI session may act as source verifier or human approver.

## Required content record

Every normative item includes:

- stable item and version IDs;
- layer classification for each claim;
- precise status and scope;
- structured confidence assessment;
- exact source-unit and edition IDs;
- reasoning, dissent, exceptions, and present factual premises;
- author and reviewer identities;
- state transitions with dates;
- approval decision and any conditions;
- review due date; and
- rendered Hebrew and English review evidence for launch content.

Practice cards also include actionable instructions, relevant people/time/place, needed materials, alternative circumstances, purpose, source status, other credible readings, and export behavior. A community or personal adoption is stored separately from the editorial item it references.

## Blocking gates

Content cannot reach `Approved` when any of these conditions exists:

- a normative or historical claim lacks an exact supporting citation;
- a citation does not resolve to the named edition and source unit;
- a quotation, translation, variant, attribution, or license was invented or inferred;
- rights metadata is missing, incompatible, or changed since verification;
- a contemporary application is presented as explicit ancient law;
- material disagreement is omitted or misrepresented;
- a modern factual claim lacks a dated source or does not fit the case;
- status, scope, confidence, reasoning, reviewer, or review date is missing;
- a high-consequence item lacks both required named human reviewers;
- an operative or external-recognition claim exceeds the product boundary;
- an approved rule has unresolved conflicts; or
- the actual rendered launch-language content has not been reviewed.

Stable publication additionally requires schema validation, citation integrity, license compatibility, pack provenance, accessibility review, and the relevant automated quality gates.

## Corrections and review

Any contributor may report a suspected content error. Credible source, rights, safety, or recognition concerns move the item to `UnderReview`; the release process may revoke or roll back a pack when continued distribution would be harmful or unlawful. The reason must be recorded transparently, and user-created content must not be deleted by a pack rollback.

Published items carry a review due date. Source-version changes, new material evidence, changed modern facts, rights changes, or a materially different community context trigger early review. Historical versions and their approval records remain auditable.

Generated text is never silently corrected in place after publication. Material corrections create a new version and release note; urgent unsafe content may be withdrawn while review proceeds.
