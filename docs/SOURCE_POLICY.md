# ASEH Source and Corpus Policy

Status: Draft for human approval

Applies to: source acquisition, editions, citations, normalization, packs, translations, media, and relationship data

## Principles

ASEH treats a work, an edition, and a license record as different objects. Public availability, API access, scholarly value, and permission to redistribute are separate questions.

The project will:

- prefer public-domain originals and explicitly open editions;
- evaluate rights for every language and version separately;
- use exact edition identities rather than a provider's default or merged text;
- cite the smallest stable source unit that supports a claim;
- preserve raw input separately from normalized and display forms;
- record every transformation reproducibly;
- surface variants and gaps instead of silently filling them; and
- fail closed when provenance, integrity, or rights are uncertain.

The authoritative license inventory is [DATA_LICENSES.md](DATA_LICENSES.md) plus each machine-readable edition manifest. A prose description never overrides an edition manifest or upstream license grant.

## Acquisition rules

1. Record the proposed edition and intended use before ingestion.
2. Verify title, language, version title, source URL, publisher or contributor, upstream license label, redistribution terms, derivative terms, commercial-use terms, and required attribution.
3. Prefer an authorized bulk export or publisher-provided file. Do not scrape a reader when an authorized source exists.
4. Save a raw input checksum and retrieval date before transformation.
5. Preserve the raw source unchanged. Produce normalized search and display forms as derived artifacts.
6. Record normalization steps, segmentation rules, stable ID mappings, and any manual correction.
7. Sample-check ordinary passages and inspect every high-value passage used by a published conclusion.
8. Compile only allowlisted editions whose rights metadata and checksums pass validation.
9. Recheck upstream version and license metadata before every public release. A mismatch blocks the release until reviewed.

An API may be used for online lookup and relationship data without making every exposed edition eligible for an offline pack. No endpoint's default version, fallback, merge, translation fill-in, or commentary expansion may enter the corpus.

## Pilot Sefaria allowlist

Only these exact provider/version/language combinations are approved as pilot source inputs. The identities and upstream license labels were verified through Sefaria's version metadata on 2026-10-06.

| Work or corpus | Language | Exact Sefaria `versionTitle` | Upstream license label |
|---|---|---|---|
| Mishneh Torah, The Order of Prayer | Hebrew | `Wikisource Mishneh Torah` | `CC-BY-SA` |
| Mishneh Torah, The Order of Prayer | English | `Sefaria Community Translation` | `CC0` |
| Mishneh Torah, Prayer and the Priestly Blessing | Hebrew | `Wikisource Mishneh Torah` | `CC-BY-SA` |
| Mishneh Torah, Prayer and the Priestly Blessing | English | `Sefaria Community Translation` | `CC0` |
| Tanakh | Hebrew | `Miqra according to the Masorah` | `CC-BY-SA` |
| Tanakh | English | `The Holy Scriptures: A New Translation (JPS 1917)` | `Public Domain` |

For Tanakh, the manifest must record and verify the same exact version for every included biblical book; verification of one book does not authorize assuming corpus-wide consistency. For each Sefaria request, specify the exact version and language, reject an unexpected returned version, and disable merged/fill-in behavior. Archive the returned version metadata and checksum with the raw source.

The generic upstream label `CC-BY-SA` is not silently rewritten as a specific Creative Commons version. The exact license URL, version, and required attribution must be resolved and recorded before a distributable pack ships. `CC0` and public-domain records likewise retain the upstream source and jurisdiction or provenance notes needed by the rights review.

The allowlist approves source identity for the pilot; it does not make every related Sefaria text, translation, link, topic, note, or media item eligible for redistribution.

## Excluded sources

The following are excluded from ingestion, caching in distributable packs, quotation beyond applicable permission, adaptation, and redistribution unless a written license is obtained and recorded:

- Mechon Mamre editions and site content; and
- Mifal Mishneh Torah / `rambam.plus` editions, explanations, indexes, diagrams, templates, and site content.

Their public accessibility is not permission to repackage them. ASEH may study unprotected ideas and information architecture without copying protected expression. Any future permission creates a new license record and requires editorial and rights review before the source is allowlisted.

The same rule applies to every modern publisher, translation, critical edition, image, diagram, recording, and scholarly work not already recorded as compatible.

## Provenance manifest

Every edition manifest contains at least:

- stable `edition_id` and `work_id`;
- language, script, exact provider version title, and edition title;
- publisher, editor or contributor, and source URL;
- upstream license label, exact license URL when known, and rights-review status;
- redistribution, derivative, and commercial-use permissions;
- required attribution and notice text;
- retrieval timestamp and raw SHA-256 checksum;
- normalization and segmentation steps;
- checksum of each derived artifact;
- review state, named reviewer, and review date; and
- any territorial, quotation, media, or downstream share-alike constraints.

A distributable build fails if any included file lacks compatible rights metadata, attribution, a raw checksum, a reproducible transformation record, or the expected exact version identity.

## Editorial source format and generated artifacts

Human authors edit version-controlled YAML and Markdown validated by pinned JSON Schemas. These files are the editorial source of truth. SQLite databases, FTS indexes, rendered documents, manifests assembled from source records, and `.asehpack` archives are generated build artifacts; contributors do not hand-edit generated SQLite to change content.

The compiler must reject source that fails schema validation and must make every source-to-artifact transformation reproducible. Schema migrations update source records or deterministic compiler behavior and include invalid fixtures proving that missing licenses, dangling citations, unsupported claims, unknown enums, cyclic procedure dependencies, executable content, and invalid localization structures fail closed.

Every pack declares exactly one primary pack type from this v0.1 taxonomy:

- `core-practice`;
- `siddur-rambam`;
- `primary-corpus-he`;
- `translations-en`;
- `geonica`;
- `maimonidean-school`;
- `visual-aids`;
- `community-templates`; or
- a namespaced regional or community-authored type approved through the same schema, rights, and signing gates.

The application includes a small functional core. Large corpora remain optional downloads or sideloaded signed packs, and every installed pack works offline.

## Text integrity and citations

Stable internal source-unit IDs are independent of a display citation so equivalent conceptual passages can align across editions without erasing differences. Every citation records work, edition, source unit, human-readable locator, language, and relationship to the claim (`explicit`, `inferred`, `contextual`, `contradicting`, or `background`).

Normalization may include Unicode normalization and search-specific forms, but it must never overwrite the raw text. Punctuation, vocalization, qere/ketiv, supplied headings, paragraphing, and corrections must retain their provenance. A generated translation or editorial normalization is a new derivative object, never the original edition.

Generic references are insufficient when a narrower citation exists. If a source does not establish a claim, the correct result is `NOT ESTABLISHED`.

## Packs and publication

A signed `.asehpack` contains a manifest, one or more SQLite databases, optional media, attribution and license files, schema/migration compatibility, per-file SHA-256 checksums, and an Ed25519 signature. The archive must be deterministic and install atomically. Installed packs work offline and can roll back without deleting user-authored content.

Only approved content may enter a production pack. Development fixtures and unsigned sample packs must be clearly separated and cannot be accepted by a production trust configuration.

Before publication, generate and review a corpus manifest, attribution report, license report, source-citation integrity report, raw/normalized checksum report, and reproducibility result.
