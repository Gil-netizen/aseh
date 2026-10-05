# Contributing to ASEH

Thank you for helping build ASEH. Contributions may include code, tests, documentation, translations, source data, editorial material, and review evidence.

## Before opening a pull request

1. Open or reference an issue that describes the problem and the intended outcome.
2. Keep the change focused. Separate unrelated code, content, and infrastructure changes.
3. Record how the change was verified, including commands run and any manual checks.
4. Remove credentials, private case material, personal information, and generated build artifacts.
5. Complete every applicable section of the pull request template.

## Evidence and provenance

Claims about texts, practices, liturgy, calendar rules, or community usage must be reviewable from the repository. For every source-dependent change, provide:

- the source title, author or issuing body, and exact edition or version;
- a stable URL or bibliographic reference and a precise location such as chapter, section, page, or folio;
- the original language when relevant and who produced any translation;
- the license, public-domain basis, or written permission that permits the proposed use;
- a short explanation of what the source supports and any material limits or disagreement;
- checksums or archived metadata for imported machine-readable material.

Do not cite an AI system, search-result summary, or unattributed compilation as authority. AI may assist with a draft, but a human contributor remains responsible for checking every claim, quotation, citation, license, and translation against the named source.

Code and behavior changes also require evidence appropriate to their risk: tests, reproducible steps, screenshots or recordings for visible behavior, and links to authoritative specifications when compatibility or security claims are involved.

## Content classification and review

Every content pull request must identify whether it is high consequence. When uncertain, classify it as high consequence until a reviewer resolves the classification.

High-consequence content includes material that could reasonably be understood as an operative religious or legal ruling, individualized case answer, safety or medical direction, financial direction, or other instruction where an error could materially affect a person or community. It also includes changes to rules that determine what the product recommends or assembles in those areas.

Ordinary content requires approval from a human repository maintainer. High-consequence research and drafts may merge only when they remain visibly `Drafted`, route product behavior to `CasePreparation`, and are excluded from approved or published packs. High-consequence content is blocked from the `Approved` or `Published` editorial state and from a distributable pack until all of the following are present:

- approvals from two distinct, named human reviewers;
- at least one approval from the repository maintainer or CODEOWNER;
- at least one approval from a reviewer qualified to assess the relevant subject matter;
- explicit review of the cited evidence, dissenting or limiting views, and the user-facing consequence of the change.

Gil may count as the product/editorial approver even when he authored or revised the item, but he cannot count as the independent second reviewer. The second reviewer must be a different named human with the relevant role or expertise. Bot reviews, AI-generated reviews, and automated checks do not count as human approval. A new commit that materially changes the reviewed conclusion, sources, or behavior invalidates the affected approvals and requires renewed review.

## Pull request expectations

`main` is protected. Except for the recorded bootstrap sequence needed to create the initially empty default branch, changes reach `main` only through a pull request from one issue or coherent-slice branch named `codex/<issue>-<slug>`. AI agents do not push or merge directly to `main`, approve their own work, or substitute automated review for the named human approval required by branch and content policy.

Every pull request must pass required CI and receive applicable CODEOWNERS review. The repository owner performs or explicitly authorizes the final merge. Public app releases and every published content-pack version have signed Git tags in addition to Android artifact signatures and `.asehpack` Ed25519 signatures.

During the single-owner incubator phase, GitHub cannot count the pull-request author as their own approving reviewer. Gil must record the concrete human review and perform the final owner merge; no AI agent uses the administrator bypass. Once a second maintainer is named, branch protection must require that person's formal approval where the policy calls for independent review.

A pull request is ready for review when it:

- explains the user-visible outcome and risk;
- links the issue and lists the files or data sets affected;
- includes complete provenance and licensing information where applicable;
- includes tests or a clear reason that automated testing does not apply;
- passes repository checks without suppressing failures;
- marks unresolved questions and does not present draft or disputed content as approved.

Maintainers may request that a large change be divided, that source material be replaced for rights or reliability reasons, or that a security-sensitive discussion move to the private process in [SECURITY.md](SECURITY.md).

## Commit and review hygiene

Write clear commit messages in the imperative mood. Do not rewrite another contributor's work without preserving attribution. Resolve review threads only after the requested change or an agreed explanation is present in the pull request.

By contributing, you agree that your contribution may be distributed under the licenses declared by the repository for the affected code or content, and that you have the right to submit it on those terms.
