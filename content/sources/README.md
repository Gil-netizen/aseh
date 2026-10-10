# Development prayer-source evidence

These pinned inputs support the development-only weekday individual-prayer reader. They are separated from production packs and do not resolve `LITURGY-001` or grant human editorial approval.

## Rambam, Order of Prayer

- Work: *Mishneh Torah, The Order of Prayer*, Hebrew.
- Provider/source: Hebrew Wikisource.
- Pinned revisions: chapter 1 `2998697`, chapter 2 `2870245`, chapter 3 `2870237`.
- Retrieved: 2026-10-08 through the MediaWiki `action=parse` API with `prop=wikitext|revid`.
- License shown on each source page: [Creative Commons Attribution-ShareAlike 4.0 International](https://creativecommons.org/licenses/by-sa/4.0/).
- Attribution: Hebrew Wikisource contributors; revision history is available from each pinned page.
- Changes in the app: MediaWiki templates and headings are omitted; source paragraphs are grouped into reader sections; spelling, punctuation, and abbreviations are otherwise preserved. The app labels the source pages as unreviewed and retains every `וכו׳`/`וגו׳` gap.

Pinned pages:

- [Order of Prayer 1, revision 2998697](https://he.wikisource.org/w/index.php?title=%D7%A8%D7%9E%D7%91%22%D7%9D_%D7%A1%D7%93%D7%A8_%D7%94%D7%AA%D7%A4%D7%99%D7%9C%D7%94_%D7%90&oldid=2998697)
- [Order of Prayer 2, revision 2870245](https://he.wikisource.org/w/index.php?title=%D7%A8%D7%9E%D7%91%22%D7%9D_%D7%A1%D7%93%D7%A8_%D7%94%D7%AA%D7%A4%D7%99%D7%9C%D7%94_%D7%91&oldid=2870245)
- [Order of Prayer 3, revision 2870237](https://he.wikisource.org/w/index.php?title=%D7%A8%D7%9E%D7%91%22%D7%9D_%D7%A1%D7%93%D7%A8_%D7%94%D7%AA%D7%A4%D7%99%D7%9C%D7%94_%D7%92&oldid=2870237)

The pages explicitly display “not reviewed.” They are research witnesses, not a selected canonical anchor or a complete siddur.

## Shema biblical passages

- Edition: `Miqra according to the Masorah`.
- Provider: Sefaria API v3, using the exact selector `hebrew|Miqra according to the Masorah` and `fill_in_missing_segments=0`.
- Ranges: Deuteronomy 6:4–9; Deuteronomy 11:13–21; Numbers 15:37–41.
- Retrieved: 2026-10-08.
- Returned language: `he`; returned license label: `CC-BY-SA`.
- Upstream source: [Miqra according to the Masorah on Hebrew Wikisource](https://he.wikisource.org/wiki/%D7%9E%D7%A9%D7%AA%D7%9E%D7%A9:Dovi/%D7%9E%D7%A7%D7%A8%D7%90_%D7%A2%D7%9C_%D7%A4%D7%99_%D7%94%D7%9E%D7%A1%D7%95%D7%A8%D7%94), whose text is supplied under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
- Changes in the app: HTML presentation elements, non-breaking spacing, page-only documentation annotations, and cantillation marks are removed for a calmer reader; Hebrew letters, vocalization, and verse punctuation are retained.

The raw API responses retain the complete returned metadata so an unexpected edition, language, warning, or license can be detected during review.

## Yemenite Baladi weekday completion witness

Alpha.10 adds pinned Hebrew Wikisource revisions for weekday Shacharit, Mincha,
and Arvit under `baladi-wikisource/`. These pages provide a complete readable
development witness where the Rambam Order of Prayer abbreviates familiar
passages. They are collaborative, unreviewed composites rather than a named
historical edition, so the app identifies them as a Baladi completion witness
and never presents them as a settled pure Nusach HaRambam text.

The directory retains the exact wikitext, normalized display text, checksums,
revision URLs, license, attribution, and transformations. Shacharit also keeps
the rendered HTML snapshot because its page transcludes subpages and templates.
The runtime selects individual-prayer passages and omits communal Kaddish,
repetition, and leader-only cues.
