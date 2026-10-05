# ASEH repository instructions

Use an ExecPlan from `docs/plans/` for work spanning multiple modules or more than one pull request.

Do not scaffold Android application code until the Phase 0 repository-contract pull request records Gil's human review of these instructions and the proposed canonical documents.

Read only the canonical documents relevant to the task:

- product scope or user-visible capability: `docs/PRODUCT.md`
- normative or editorial content: `docs/METHOD.md` and `docs/EDITORIAL_POLICY.md`
- corpus, citation, or license changes: `docs/SOURCE_POLICY.md` and `docs/DATA_LICENSES.md`
- personal or sensitive data: `docs/PRIVACY_MODEL.md` and `docs/THREAT_MODEL.md`
- UI, language, or interaction: `docs/ACCESSIBILITY.md`
- architecture: the applicable record in `docs/adr/`
- packaging or deployment: `docs/RELEASE.md`
- unresolved policy or implementation choice: `docs/OPEN_QUESTIONS.md`

Never invent a citation, license, translation, textual variant, approval, or review identity. Generated content may reach only Proposed, Researched, or Drafted. A human must grant HumanReviewed and Approved.

Keep core prayer, calendar, installed content, and search functional offline. Every UI change must support Hebrew RTL, English LTR, mixed-script text, TalkBack, and large text. Never place secrets or sensitive case data in code, fixtures, logs, screenshots, prompts, or build artifacts.

Preserve user-written changes. Work in small reviewable branches named `codex/<issue>-<slug>`. Protected `main` accepts changes only through a pull request with required CI, applicable CODEOWNERS review, and named human approval. An AI agent never pushes or merges directly to `main` and never approves its own work. App releases and content-pack versions use signed Git tags under the release policy. Run the task-specific verification commands before finishing and report commands, artifacts, provenance changes, assumptions, known gaps, and residual risk in the pull request.

If requirements conflict, stop and report the conflict instead of creating product policy.
