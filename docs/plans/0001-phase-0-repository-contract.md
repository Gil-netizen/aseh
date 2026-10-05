# ExecPlan 0001: Phase 0 repository contract

**Status:** Complete\
**Owner:** Gil\
**Started:** 2026-10-06\
**Completed:** 2026-10-06\
**Tracking:** [GitHub issue #1](https://github.com/Gil-netizen/aseh/issues/1)\
**Scope:** Repository governance, canonical-document approval, verified toolchain, source-rights inventory, and the review gate before Android scaffolding

## Outcome

Prepare a public ASEH repository whose instructions, product boundaries,
editorial method, source policy, privacy and threat controls, accessibility
requirements, and release process are concrete enough for a human to approve.
Do not scaffold application code until that review is recorded.

## Decisions already supplied by the owner

- Repository: `Gil-netizen/aseh`, public open-source alpha.
- Package ID: `io.github.gilnetizen.aseh`.
- Code and project documentation: Apache-2.0.
- Original distributable editorial content: CC BY-SA 4.0.
- Ordinary-content approver: Gil.
- High-consequence promotion: Gil plus a different named human reviewer;
  unapproved work remains `Drafted` and routes to `CasePreparation`.
- First optional connected adapter: OpenAI Responses with direct on-device BYOK.
- First release channel: GitHub prerelease `v0.1.0-alpha.1`.

## Work completed

- Created the public repository and local Git remote.
- Established the repository instructions, contribution and review rules, issue
  forms, CODEOWNERS, pull-request template, and policy workflow.
- Approved the product, method, editorial, source, privacy, threat,
  accessibility, release, and traceability documents.
- Retained the rights inventory as Draft and the decision register as Active.
- Accepted ADR-0001 through ADR-0005 for implementation.
- Installed and verified JDK 17, Android Studio, Android SDK Platform 37.0,
  Build Tools 37.0.0, and Platform-Tools 37.0.1 locally; the reviewed
  compatibility set and exact evidence are recorded in ADR-0005.
- Enabled GitHub private vulnerability reporting and Discussions.
- Created the initially empty default branch through a four-commit GitHub API
  bootstrap sequence: `.editorconfig` (`7559363`), `.gitignore` (`6d69147`),
  `LICENSE` (`c98d8be`), and `NOTICE` (`11fe8dc`). Branch protection was then
  enabled before publishing policy work. This is the recorded one-time
  exception to the no-direct-AI-push rule; all later work uses PRs.
- Created the `content` and `needs-editorial-review` issue labels referenced by
  the repository forms.
- Published [pull request #2](https://github.com/Gil-netizen/aseh/pull/2), passed
  the repository-policy workflow, and made `Links and secret patterns` a
  required check on protected `main`.
- Recorded Gil's named approval in [the Phase 0 review](https://github.com/Gil-netizen/aseh/pull/2#issuecomment-6004033585).
- Kept all open source-rights questions explicitly fail-closed and excluded
  unapproved material from distributable packs.

## Completion state

The repository contract, canonical policies, and ADRs are approved. Rights
questions in `OPEN_QUESTIONS.md` and the draft rights inventory remain active
release gates; their open status does not authorize affected content for a
public pack. The repository owner must merge pull request #2 before a separate
scaffolding branch starts from `main`.

## Verification

- All local Markdown links resolve.
- GitHub workflow and issue-form YAML parse successfully.
- The root Apache-2.0 text matches the official license.
- Secret-policy checks scan the tracked tree and every new history blob,
  including binary content, and reject secret-bearing artifact types.
- Repository settings are read back after mutation.
- No Android application module exists before the human gate.

## Gate to the next plan

Gil recorded the required approval of `AGENTS.md`, the contribution/governance
contract, the canonical documents, and ADR-0001 through ADR-0005 in pull request
#2. After the repository owner merges that PR, the scaffolding PR creates its
own ExecPlan, implements and verifies the accepted pins in ADR-0005, and records
the module layout. A demonstrated incompatibility requires a superseding ADR.
Because Gil is both repository owner and Phase 0 pull-request author, GitHub
cannot count a self-review; Gil performs the final owner merge. No AI agent uses
the administrator bypass.
