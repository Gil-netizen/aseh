# ASEH Data and Content Licenses

Status: Draft rights inventory

Last metadata verification: 2026-10-06

## Project licensing posture

| Material | Intended license | Rule |
|---|---|---|
| ASEH source code | Apache License 2.0 | Apply the repository license and preserve third-party notices |
| Original ASEH editorial content | Creative Commons Attribution-ShareAlike 4.0 International | Mark ASEH-authored content clearly and preserve attribution/share-alike terms |
| Third-party text, translations, data, and media | The license of each exact edition or asset | Never relicense as ASEH content; preserve notices, provenance, and any downstream conditions |
| User-authored private data | No project license grant by default | Keep local and export only by deliberate user action |

Code and content have separate license notices. A repository-level license does not absorb third-party works or user data. Mixed artifacts must identify their components and applicable licenses.

This file records the current rights decision for each pilot input. Machine-readable edition manifests and bundled license/attribution files are required before distribution. When this inventory and a manifest disagree, distribution stops for human review.

## Allowlisted pilot edition identities

These records were checked against live Sefaria version metadata on 2026-10-06. Approval here is limited to the named version and language; it does not extend to default, merged, related, or replacement versions.

| ID | Work or corpus | Language | Exact version title | Recorded source | Upstream license label | Distribution readiness |
|---|---|---|---|---|---|---|
| `rambam_order_prayer_he_wikisource` | Mishneh Torah, The Order of Prayer | Hebrew | `Wikisource Mishneh Torah` | Sefaria metadata; upstream he.wikisource.org | `CC-BY-SA` | Conditional: record exact CC version/URL and attribution |
| `rambam_order_prayer_en_sefaria` | Mishneh Torah, The Order of Prayer | English | `Sefaria Community Translation` | Sefaria | `CC0` | Conditional: preserve CC0 source/provenance record |
| `rambam_order_prayer_en_sefaria_1_1_4` | Mishneh Torah, The Order of Prayer 1:1–4 | English | `Sefaria Community Translation` | [Technical evidence](source-evidence/rambam-order-prayer-en-1-1-4.md) | `CC0` | Technically complete; human rights review pending; not distribution-approved |
| `rambam_prayer_blessing_he_wikisource` | Mishneh Torah, Prayer and the Priestly Blessing | Hebrew | `Wikisource Mishneh Torah` | Sefaria metadata; upstream he.wikisource.org | `CC-BY-SA` | Conditional: record exact CC version/URL and attribution |
| `rambam_prayer_blessing_en_sefaria` | Mishneh Torah, Prayer and the Priestly Blessing | English | `Sefaria Community Translation` | Sefaria | `CC0` | Conditional: preserve CC0 source/provenance record |
| `tanakh_he_miqra_masorah` | Tanakh | Hebrew | `Miqra according to the Masorah` | Sefaria metadata; upstream he.wikisource.org | `CC-BY-SA` | Conditional: verify each included book and record exact CC version/attribution |
| `tanakh_en_jps_1917` | Tanakh | English | `The Holy Scriptures: A New Translation (JPS 1917)` | Sefaria metadata; upstream Open Siddur record | `Public Domain` | Conditional: verify each included book and record provenance/jurisdiction note |

"Conditional" means the edition identity is allowlisted for pilot ingestion and evaluation, while a public pack remains blocked until the full provenance manifest, exact notices, checksums, and required attribution are present. It is not a finding that the source may be used without conditions.

The narrow `1:1–4` record documents a complete, nonempty API response and exact
version metadata for a technical ingestion pilot. It does not cure the blanks,
placeholders, or missing chapters found in the whole-work response, choose a
canonical liturgical anchor, or authorize public distribution before the named
human rights review in `RIGHTS-004`.

Sefaria is the retrieval provider for these records, not a blanket license for all Sefaria content. License status is version-specific and language-specific. The importer must request the exact `versionTitle`, reject substitution or merge behavior, and retain the source's own metadata.

## Excluded or permission-required material

| Source | Current decision | What would change the decision |
|---|---|---|
| Mechon Mamre | Excluded from distributable corpora and adapted ASEH content | Written permission or a verified license grant covering the exact intended use |
| Mifal Mishneh Torah / `rambam.plus` | Excluded from distributable corpora, explanations, indexes, diagrams, and templates | Written permission or partnership terms recorded for each asset class |
| Unnamed/default/merged Sefaria versions | Excluded | Exact edition selection and completed rights record |
| Other modern editions and translations | Excluded by default | Edition-level rights verification and allowlist approval |
| Third-party images, audio, diagrams, and fonts | Excluded by default | Asset-level license, source, attribution, and redistribution record |

ASEH may cite or link to a source when legally and editorially appropriate without storing or redistributing its protected text. Such references still require accurate metadata and must not imply permission to package the work.

## Required attribution and notices

Every public pack and release must include:

- a machine-readable manifest per edition and asset;
- a human-readable attribution and license report;
- the complete license or notice text when required;
- source URL, exact version title, language, contributor/publisher, retrieval date, and checksum;
- a record of ASEH normalization, segmentation, corrections, translation, or other derivatives; and
- any share-alike source offer or downstream condition required by the applicable license.

UI displays the exact edition and attribution wherever a user inspects source provenance. Exports retain the notices for every included source.

## Release gate

A rights check fails closed if:

- an edition or asset is not allowlisted;
- the upstream version or license label changed;
- a license URL, attribution, permission, provenance note, or checksum required by the manifest is missing;
- commercial or derivative use is incompatible with the release;
- share-alike obligations are not satisfied;
- material from an excluded source appears in a build; or
- a source provider returns a fallback, merged, or different version.

No contributor or automated system may resolve a rights ambiguity by guessing. Record the issue in [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md), keep the material out of public artifacts, and obtain human rights review.
