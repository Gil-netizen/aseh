# ExecPlan: [bounded outcome]

**Status:** Draft | Active | Blocked | Complete\
**Owner:** [name or role]\
**Started:** YYYY-MM-DD\
**Last updated:** YYYY-MM-DD\
**Issue/PR:** [links]\
**Related ADRs:** [links or none]

This file is a living implementation record for work that spans multiple modules or more than one pull request. Keep it current while work proceeds. A new contributor should be able to continue using only this plan, the repository, and the linked canonical documents.

## Outcome

State the observable user or system result in one paragraph. Define who benefits and what becomes possible when the plan is complete.

## Success criteria

- [Externally observable behavior]
- [Compatibility, offline, and failure behavior]
- [Evidence that makes completion reviewable]

## Canonical requirements

Read only the documents relevant to this work and record them here.

- `AGENTS.md`
- [product/method/editorial/source policy]
- [privacy/threat/accessibility/release policy]
- [relevant ADRs and schemas]

If requirements conflict, stop and record the conflict. Do not create a new product, editorial, source, privacy, or security policy inside this plan.

## Scope and non-goals

**In scope**

- [bounded change]

**Out of scope**

- [explicit exclusion that prevents scope drift]

## Current state and context

Describe the existing behavior, relevant modules, interfaces, schemas, fixtures, and constraints. Define project-specific terms. Include enough path and symbol names to let an unfamiliar implementer find the starting point, but do not copy large code blocks.

## Design and interfaces

Explain the selected approach and data flow. Specify new or changed APIs, persisted data, schemas, result types, error behavior, compatibility rules, and trust boundaries. Link accepted ADRs for decisions that outlive this implementation.

Address when applicable:

- offline behavior and network failure;
- Hebrew RTL, English LTR, mixed scripts, TalkBack, and 200% text;
- source identity, citations, licensing, and editorial state;
- secrets, backups, logs, exports, and provider disclosure;
- `CasePreparation` routing for high-consequence input;
- migrations, pack compatibility, and rollback; and
- deterministic fake/test implementations for external providers.

## Milestones

Each milestone ends in a coherent, testable state. Include commands and expected observations rather than vague statements such as “finish backend.”

### 1. [Milestone name]

**Result:** [what works]

**Work:**

- [implementation action]

**Verification:**

```text
[command]
Expected: [exit code, artifact, or behavior]
```

### 2. [Milestone name]

**Result:** [what works]

**Work:**

- [implementation action]

**Verification:**

```text
[command]
Expected: [exit code, artifact, or behavior]
```

## Progress

Use timestamped checkboxes. Split partially complete work into completed and remaining entries so this section always reflects reality.

- [ ] YYYY-MM-DD HH:MM Z — [work item]

## Decisions

Record implementation decisions and their reasons. Durable architecture choices also require an ADR.

| Date | Decision | Rationale | ADR |
|---|---|---|---|
| YYYY-MM-DD | [decision] | [reason] | [link or none] |

## Surprises and discoveries

Record unexpected repository facts, tool behavior, failures, performance results, or specification conflicts with concise evidence.

| Date | Finding | Evidence | Effect on plan |
|---|---|---|---|
| YYYY-MM-DD | [finding] | [command/output/path] | [change or none] |

## Verification and acceptance

List the final commands, fixtures, device scenarios, screenshots, reports, and manual reviews. State what each proves. At minimum, select applicable gates from compilation, unit tests, static analysis, migrations, secret/dependency scans, offline behavior, source/citation integrity, pack tampering, provider contract tests, privacy, threat model, accessibility, and release signing.

Use synthetic data in tests and evidence. Never paste credentials, copyrighted corpus content, or real sensitive case data into the plan, prompts, fixtures, screenshots, or logs.

## Rollout and recovery

Describe feature flags or staged exposure, migration order, compatibility, monitoring that does not violate the no-analytics posture, rollback, and data recovery. For app releases, prefer a signed forward-fix. Pack rollback must preserve user-created content.

## Residual risks and handoff

List known limitations, owners, follow-up issues, and facts a reviewer must check. Distinguish an accepted alpha risk from unfinished required work.

## Outcomes

Complete this section when the plan closes. Summarize what shipped, which evidence passed, what changed from the original plan and why, and which follow-ups remain. A plan is not complete merely because code was written.
