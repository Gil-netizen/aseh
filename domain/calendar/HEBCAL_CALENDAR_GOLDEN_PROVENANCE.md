# Hebcal calendar-context golden fixtures

Status: test evidence; not production calendar or liturgical content

Retrieved: 2026-10-07

The offline fixture at
`src/test/resources/hebcal-calendar-6.13.1-4.2.2-golden.tsv` records selected
metadata returned by the public Hebcal Jewish calendar API. The response identified
the deployed implementation as `6.13.1-4.2.2`. Tests never call Hebcal or any other
network service.

## Scope

The selected observations independently check:

- Hebrew/Gregorian conversion across Adar I and Adar II in leap year 5784;
- Rosh Hodesh on both 30 Adar I and 1 Adar II;
- a shifted Fast of Esther and the Fast of the Firstborn flag on Erev Pesach;
- Shabbat Shekalim in a leap year and in non-leap year 5785;
- work-restricted festival versus intermediate-festival metadata;
- Israel/diaspora divergence on the second and eighth days of Pesach, the second
  day of Shavuot, and Simchat Torah; and
- a festival intermediate day that also falls on Shabbat.

The fixture retains only the civil date, numeric Hebrew date, Hebcal's event
identity/category flags needed for comparison, region, and selected boolean
signals. Three Israel rows separately mark KosherJava's `Isru Chag` result as a
known production-library-only value because Hebcal returned only the Hebrew date
on those dates. The test does not misstate those values as independent agreement.
It contains no prayer text, Torah or haftarah readings, citations to a
reading, Hebrew event text, explanatory memo, or normative instruction. API
requests set `leyning=off`, and the transformation omits all fields outside this
narrow test surface.

## Source queries

All queries use explicit date ranges, disable modern holidays, disable reading
payloads, and select only calendar event classes exercised by the adapter.

- Diaspora leap-year and fast cases:
  <https://www.hebcal.com/hebcal?cfg=json&v=1&maj=on&min=on&nx=on&mf=on&ss=on&mod=off&d=on&leyning=off&start=2024-03-09&end=2024-03-21>
- Diaspora festival and divergence cases:
  <https://www.hebcal.com/hebcal?cfg=json&v=1&maj=on&min=on&nx=on&mf=on&ss=on&mod=off&d=on&leyning=off&start=2024-04-22&end=2024-06-13>
- Israel festival and divergence cases:
  <https://www.hebcal.com/hebcal?cfg=json&v=1&maj=on&min=on&nx=on&mf=on&ss=on&mod=off&d=on&leyning=off&i=on&start=2024-04-22&end=2024-06-13>
- Diaspora and Israel Tishrei divergence:
  <https://www.hebcal.com/hebcal?cfg=json&v=1&maj=on&min=on&nx=on&mf=on&ss=on&mod=off&d=on&leyning=off&start=2024-10-24&end=2024-10-25>
  and the same query with `i=on`.
- Non-leap Adar and simultaneous Rosh Hodesh/special-Shabbat case:
  <https://www.hebcal.com/hebcal?cfg=json&v=1&maj=on&min=on&nx=on&mf=on&ss=on&mod=off&d=on&leyning=off&start=2025-03-01&end=2025-03-01>

Primary API documentation:

- <https://www.hebcal.com/home/195/jewish-calendar-rest-api>
- <https://www.hebcal.com/home/developer-apis>

## Independence and limits

Hebcal is a separately deployed implementation and is useful for catching region,
leap-year, conversion, and event-mapping mistakes. This evidence does not prove a
normative calendar method, service composition, weekly or festival reading,
liturgical insertion, modern observance policy, walled-city status, or local
custom. The production adapter leaves those surfaces explicitly unevaluated.

Hebcal did not emit an Isru Chag event for the three selected post-festival Israel
dates, while KosherJava 2.5.0 returned `JewishCalendar.ISRU_CHAG`. The fixture and
test identify this difference explicitly; they use Hebcal's event absence to check
that the day is not work-restricted, but do not claim independent agreement on the
Isru Chag metadata.

The fixture tests selected dates rather than exhaustively comparing every Hebrew
calendar year shape. KosherJava remains the production implementation, pinned to
2.5.0 and wrapped behind ASEH's typed result and resolution trace.

## License and transformation

Hebcal's developer API notice states that Jewish holiday output is available under
the Creative Commons Attribution 4.0 International license. Attribution: selected
calendar metadata is from [Hebcal.com](https://www.hebcal.com/).

The TSV is a transformed selection rather than a response archive. Transformations
are limited to tabular layout, ASEH enum spelling, boolean normalization, region
labels, omission of unused response fields, and stable test-case IDs.
