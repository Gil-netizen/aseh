# ASEH Product Constitution

Status: Approved

Approved by: Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2)

Product: ASEH / עֲשֵׂה

Working subtitle: Make Jewish life

Primary platform: Native Android, offline first

## Product promise

ASEH helps an individual, household, or emerging qahal understand what to do, why to do it, how to do it, what sources support it, where real uncertainty remains, and what communal structures are needed to act together.

ASEH is a portable operating system for a full Jewish life. It combines:

- a context-aware siddur and service conductor;
- practical guides for personal, household, annual, ethical, economic, and communal life;
- an offline library of primary texts with visibly separate interpretive layers;
- procedures, checklists, forms, diagrams, and printable kits;
- study organized around questions, practices, concepts, and source relationships;
- tools for forming and maintaining a qahal; and
- source-bound AI research and explanation using a provider and key chosen by the user.

ASEH is organized initially through Rambam because his work provides a broad, structured account of individual and collective Jewish life. Rambam is a principal system-builder, not an infallible authority. Primary evidence, Geonic material, early disagreement, responsa, the Commentary on the Mishnah, letters, the Guide, family tradition, and relevant Yemenite and Sepharadi reception must remain visible where they affect a conclusion.

The product does not present a denominational authority chain in Maimonidean language. It supports reasoned disagreement with Rambam and makes every departure, reconstruction, and modern application auditable.

## Product constitution

Every feature and content item must follow these rules:

1. **Sources before slogans.** Normative and historical claims resolve to exact source units, edition metadata, rights metadata, and an editorial record.
2. **Method over personality loyalty.** Named authorities and their evidence are represented accurately, including disagreement and credible reasons to depart from a conclusion.
3. **Five layers stay separate.** Source text, an authority's position, editorial synthesis, contemporary application, and adopted practice are stored and shown as distinct objects. [METHOD.md](METHOD.md) defines the binding model.
4. **Scope travels with every rule.** A conclusion states who or what can make it operative and whether it is law, regulation, custom, discipline, advice, disputed, or unresolved.
5. **Leniency has a legal route.** A lenient result identifies whether it follows from absence of a prohibition, a narrow definition, doubt, an exception, changed facts, or affirmative permission.
6. **Practice is embodied.** Posture, movement, voice, silence, direction, shared meals, acts of care, public responsibility, and material practice belong in the product.
7. **Individual and society belong together.** Worship, learning, ethics, economic justice, mutual aid, governance, courts, labor, charity, conflict resolution, and public institutions share one system.
8. **Israel and the diaspora are first-class contexts.** Neither is a reduced variant of the other. Location may affect calendar, agriculture, language, institutions, communal possibility, and civic responsibility.
9. **The core requires neither account nor network.** Calendar, core prayer, core guides, local search, settings, adopted practices, and installed packs must work in airplane mode.
10. **The product never simulates infallibility.** It states what is explicit, inferred, disputed, or not established. It does not impersonate Rambam, a rabbi, a court, or another authority.

## Users and contexts

ASEH serves:

- a reconstructing individual seeking a coherent path from minimal to fuller practice;
- a household coordinating calendars, meals, learning, preparation, care, and differing practices;
- an emerging qahal needing roles, procedures, worship, documents, and legitimate local decisions;
- a community maintainer managing schedules, facilities, funds, volunteers, learning, conflict, and succession; and
- a serious student or editor needing primary texts, variants, source relationships, notes, search, comparison, and evidence-bound research.

Profiles are purpose-limited and optional. A user supplies only what a feature needs: language, transliteration, location, elevation, timezone, calendar profile, active personal/household/community context, prayer role, adopted practice profile, device-use preference, accessibility needs, learning level, installed editions, and optional AI settings. Sensitive data stays local unless the user deliberately exports it.

## Product structure

The application has five primary destinations:

| Destination | User question | Core capability |
|---|---|---|
| Now | What is relevant now? | Date, location, zmanim, services, calendar additions, preparation, study, and responsibilities |
| Practice | How do I do this? | Actionable, cited practice cards and procedures |
| Pray | What do we say and do here? | Context-built prayer, service roles, source explanations, and print preparation |
| Study | What do the sources establish? | Library, comparison, source graph, notes, plans, and Ask ASEH |
| Build | How do we sustain this? | Self, Household, and Qahal workspaces, charters, decisions, roles, care, and templates |

Global search and context switching are available throughout. Automation must always offer a clear way to inspect and correct its assumed context.

## Product boundaries

ASEH may explain ordinary practice, assemble a service, show competing source pathways, prepare documents and events, guide the formation of communal structures, support source-bound research, and let a community record its own adopted practices without claiming universal force.

ASEH cannot by itself:

- determine emergency medical action or resolve abuse, coercion, incapacity, or immediate danger;
- establish facts requiring testimony, examination, specialist inspection, or adversarial adjudication;
- guarantee the civil, legal, denominational, or communal recognition of a document or act;
- replace a court, qualified reviewer, clinician, civil professional, or local expert when the method requires one;
- distribute a modern edition, translation, illustration, or explanation without compatible rights; or
- turn a retrieved answer into a live ruling merely because its citations are accurate.

Marriage, divorce or annulment, conversion, vows, complex monetary disputes, medical danger, fertility and pregnancy, abuse, death, burial, and public accusations are high-consequence domains. Their workflow must explain the general framework, identify outcome-changing facts, show evidence and editorial reasoning, identify safe preparatory acts, name required human roles, and create a private case-preparation packet. The public alpha produces no operative legal or religious document in these domains and makes no external recognition claim.

## Public-alpha demonstrator

Implementation establishes the weekday individual-prayer foundation first: context selection, deterministic service assembly, exact-source display, embodied cues, accessibility, print output, and offline verification. The first public-alpha end-to-end demonstrator then extends that foundation into a complete Shabbat-morning beit knesset experience for a small emerging qahal. It includes:

- a preparation checklist and explicit active context;
- date-, location-, and profile-aware service assembly;
- Rambam-based text whose exact edition and editorial treatment are visible;
- embodied and accessible prayer cues;
- leader, reader, gabbai, and congregant views;
- Torah-reading assignments;
- exact-source explanations and a "why included" trace;
- offline and print-only operation;
- a small-community charter and role template;
- electricity and lighting as a disputed-practice dossier with arguments and a separately recorded community adoption;
- one source-bound AI question with claim-level citations; and
- an offline-generated service packet.

The electricity and lighting dossier remains `DISPUTED` or `UNRESOLVED` until human review establishes a narrower conclusion. Community adoption does not change the dossier's evidence status.

## Quality and success

A release succeeds when people can act with understanding and eventually rely less on the application because a living practice and community exist around them. Product measures therefore emphasize:

- completion of common tasks without a network;
- correct performance and comprehension by a new user;
- successful service, household kit, and qahal-kit completion;
- exact resolution of every published normative citation;
- zero unsupported claims in stable content;
- visible treatment of genuine disputes;
- full edition and license metadata;
- human review of every stable practice card;
- successful accessibility tasks in Hebrew RTL and English LTR; and
- reliable pack installation, update, and rollback.

Engagement time and notification response are not success measures.
