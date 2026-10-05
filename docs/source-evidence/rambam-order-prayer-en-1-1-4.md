# Source evidence: Rambam, The Order of Prayer 1:1–4, English

**Status:** Technical evidence complete; named human rights review pending\
**Verified:** 2026-10-06\
**Edition ID:** `rambam_order_prayer_en_sefaria_1_1_4`

## Exact acquisition contract

- Work and range: `Mishneh Torah, The Order of Prayer 1:1–4`.
- Language: English.
- Version title: `Sefaria Community Translation`.
- Request method: `GET`.
- Request URL: <https://www.sefaria.org/api/v3/texts/Mishneh_Torah%2C_The_Order_of_Prayer.1.1-4?version=english%7CSefaria%20Community%20Translation&fill_in_missing_segments=0&return_format=default>.
- API contract: [Sefaria Get Texts v3](https://developers.sefaria.org/reference/get-v3-texts).
- Human-readable edition view: [Sefaria range and version](https://www.sefaria.org/Mishneh_Torah%2C_The_Order_of_Prayer.1.1-4?lang=en&ven=Sefaria_Community_Translation).

The response contained exactly four nonempty segments, one returned English
version, and no warnings. `fill_in_missing_segments=0` and the exact version
selector are mandatory so that ingestion fails instead of merging or silently
substituting another edition.

## Recorded evidence

| Evidence | Byte length | SHA-256 |
|---|---:|---|
| Raw API v3 response | 11,866 | `facea14e286cc2810aa485296aba7cea3150c22683e5c08e8b2858e67df193c4` |
| Selected compact payload used by an importer fixture | 6,761 | `e1d8ab5744ff0c49d70d9bfbc1e563d5dfb32af2d66479010bafe9a77d447f93` |
| Exact-work versions endpoint snapshot | 3,279 | `fc20a8c459ab445b6fa0205faa180d3d94b3bbe4de2c67321020e2ab574d4a61` |

The exact-work version inventory was retrieved from [Sefaria's versions
endpoint](https://www.sefaria.org/api/texts/versions/Mishneh_Torah%2C_The_Order_of_Prayer).
The work identity was cross-checked against [Sefaria's index
record](https://www.sefaria.org/api/v2/index/Mishneh_Torah%2C_The_Order_of_Prayer).

The checksums bind this evidence to the responses observed on the verification
date. The upstream response bodies are not committed in Phase 0 and are not yet
licensed as an ASEH distributable content pack.

## Rights metadata and limits

The exact English version metadata labeled the version `CC0`. The rights review
must compare that metadata with the [CC0 1.0 summary](https://creativecommons.org/publicdomain/zero/1.0/)
and [CC0 1.0 legal code](https://creativecommons.org/publicdomain/zero/1.0/legalcode.en),
then decide what provenance and attribution ASEH will retain even where CC0 does
not require attribution. Any use of Sefaria's name or marks must follow its
[name and logo guidance](https://developers.sefaria.org/docs/usage-of-our-name-and-logo).

This record supports only a narrow technical ingestion pilot. It does not:

- approve this text for a public `.asehpack`;
- establish rights for the entire work or another language or version;
- resolve blanks, placeholders, or missing chapters in the whole-work response;
- choose the project's canonical liturgical anchor; or
- permit default, fallback, merged, or substituted Sefaria versions.

Public distribution remains blocked until a named human rights reviewer resolves
`RIGHTS-004` in [the decision register](../OPEN_QUESTIONS.md) and the resulting
manifest, notices, attribution, checksums, and approval record pass the release
gate in [DATA_LICENSES.md](../DATA_LICENSES.md).
