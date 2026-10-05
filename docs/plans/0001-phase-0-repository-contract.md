# ExecPlan 0001: Phase 0 repository contract

**Status:** Active pending human review\
**Owner:** Gil\
**Started:** 2026-10-06\
**Tracking:** [GitHub issue #1](https://github.com/Gil-netizen/aseh/issues/1)\
**Scope:** Repository governance, canonical-document proposals, verified toolchain, source-rights inventory, and the review gate before Android scaffolding

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

## Work completed for review

- Created the public repository and local Git remote.
- Drafted the repository instructions, contribution and review rules, issue
  forms, CODEOWNERS, pull-request template, and policy workflow.
- Drafted the product, method, editorial, source, licensing, privacy, threat,
  accessibility, release, and open-question documents.
- Recorded proposed technical decisions for human review before implementation.
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

## Remaining Phase 0 work

1. Publish this document set on a review branch and open a pull request.
2. Verify protected `main` after the bootstrap sequence and require the
   repository-policy check once its first PR run establishes the check context.
3. Obtain and record Gil's review of the repository instructions and proposed
   canonical documents.
4. Resolve or explicitly defer every open source-rights item before a public
   content pack includes the affected text.
5. After approval, change the reviewed documents from proposed/draft to their
   approved status in the merge commit or a human-authored follow-up.

## Verification

- All local Markdown links resolve.
- GitHub workflow and issue-form YAML parse successfully.
- The root Apache-2.0 text matches the official license.
- Secret-policy checks scan the tracked tree and every new history blob,
  including binary content, and reject secret-bearing artifact types.
- Repository settings are read back after mutation.
- No Android application module exists before the human gate.

## Gate to the next plan

Android scaffolding begins only after a named human records approval of
`AGENTS.md`, the contribution/governance contract, and the proposed canonical
documents in the Phase 0 pull request. The scaffolding PR will create its own
ExecPlan, implement and verify the reviewed pins in ADR-0005, and record the
module layout. A demonstrated incompatibility requires a superseding ADR rather
than an unreviewed version change.
Because Gil is both repository owner and Phase 0 pull-request author, GitHub
cannot count a self-review; Gil performs the final owner merge after recording
the review. No AI agent uses the administrator bypass.
