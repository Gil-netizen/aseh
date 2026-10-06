# Hebcal cross-implementation golden fixtures

Status: test evidence; not production calendar content

Retrieved: 2026-10-06

The offline fixture in
`src/test/resources/hebcal-v1.11.3-golden.tsv` records selected results from
the public Hebcal.com APIs. The responses identified the deployed Zmanim API
as `v1.11.3`. Tests never call Hebcal or any other network service.

## Why these observations are useful

ASEH calculates locally through KosherJava 2.5.0. The golden fixture provides
a separately deployed implementation and separately recorded output for:

- a Jerusalem baseline with elevation;
- New York as a diaspora location;
- New York immediately before and after the 2026 spring DST transition;
- the 5784 leap-year transition from 30 Adar I to 1 Adar II at sunset;
- ordinary and leap-year after-sunset date handling; and
- a Tromsø polar-day case with no sunrise or sunset.

Hebcal documents that its Zmanim API uses double-precision NOAA calculations,
that `sec=1` requests second precision, and that differences of one or two
minutes from other published zmanim are expected. The test therefore uses a
two-minute inclusive tolerance for available instants. Hebrew year, month, day,
the after-sunset flag, local UTC offset, and unavailable horizon crossings are
compared exactly.

Primary documentation:

- [Hebcal Zmanim API](https://www.hebcal.com/home/1663/zmanim-halachic-times-api)
- [Hebcal location and elevation parameters](https://www.hebcal.com/home/4912/specifying-a-location-for-jewish-calendar-apis)
- [Hebcal Hebrew date converter](https://www.hebcal.com/home/219/hebrew-date-converter-rest-api)
- [Hebcal `Zmanim` API reference](https://hebcal.github.io/api/core/classes/Zmanim.html)
- [Hebcal API Go service](https://github.com/hebcal/hebcal-api-go)
- [Hebcal NOAA implementation lineage](https://github.com/hebcal/noaa)

## Source queries and selected observations

All URLs pin an explicit civil date or range. Zmanim requests use decimal
coordinates and IANA time-zone IDs. Elevation-bearing requests set `ue=on`;
all requests set `sec=1`.

### Jerusalem baseline and ordinary after-sunset boundary

Zmanim query:

<https://www.hebcal.com/zmanim?cfg=json&latitude=31.778&longitude=35.235&tzid=Asia%2FJerusalem&elev=754&ue=on&date=2026-10-06&sec=1>

Selected response: sunrise `2026-10-06T06:31:50+03:00`, chatzot
`2026-10-06T12:26:58+03:00`, sunset
`2026-10-06T18:22:06+03:00`.

Converter queries:

- <https://www.hebcal.com/converter?cfg=json&date=2026-10-06&g2h=1&strict=1>
  returned 25 Tishrei 5787 and `afterSunset=false`.
- <https://www.hebcal.com/converter?cfg=json&date=2026-10-06&g2h=1&strict=1&gs=on>
  returned 26 Tishrei 5787 and `afterSunset=true`.

The after-sunset reference instant is five minutes after the independently
recorded Hebcal sunset.

### New York diaspora and DST transition

Ordinary diaspora query:

<https://www.hebcal.com/zmanim?cfg=json&latitude=40.7128&longitude=-74.006&tzid=America%2FNew_York&elev=10&ue=on&date=2026-12-01&sec=1>

Selected response: sunrise `2026-12-01T07:00:06-05:00`, chatzot
`2026-12-01T11:44:59-05:00`, sunset
`2026-12-01T16:29:53-05:00`.

Converter query:

<https://www.hebcal.com/converter?cfg=json&date=2026-12-01&g2h=1&strict=1>

Selected response: 21 Kislev 5787 and `afterSunset=false`.

DST range query:

<https://www.hebcal.com/zmanim?cfg=json&latitude=40.7128&longitude=-74.006&tzid=America%2FNew_York&elev=10&ue=on&start=2026-03-07&end=2026-03-09&sec=1>

Selected observations:

| Date | Sunrise | Chatzot | Sunset |
|---|---|---|---|
| 2026-03-07 | `06:19:55-05:00` | `12:07:17-05:00` | `17:54:39-05:00` |
| 2026-03-08 | `07:18:19-04:00` | `13:07:02-04:00` | `18:55:45-04:00` |

The offset change from `-05:00` to `-04:00` is asserted separately. Converter
queries for [March 7](https://www.hebcal.com/converter?cfg=json&date=2026-03-07&g2h=1&strict=1)
and [March 8](https://www.hebcal.com/converter?cfg=json&date=2026-03-08&g2h=1&strict=1)
returned 18 and 19 Adar 5786, respectively.

### Jewish leap-year and sunset boundary

Zmanim query:

<https://www.hebcal.com/zmanim?cfg=json&latitude=31.778&longitude=35.235&tzid=Asia%2FJerusalem&elev=754&ue=on&date=2024-03-10&sec=1>

Selected response: sunrise `2024-03-10T05:50:55+02:00`, chatzot
`2024-03-10T11:49:25+02:00`, sunset
`2024-03-10T17:47:56+02:00`.

Converter queries:

- <https://www.hebcal.com/converter?cfg=json&date=2024-03-10&g2h=1&strict=1>
  returned 30 Adar I 5784 and `afterSunset=false`.
- <https://www.hebcal.com/converter?cfg=json&date=2024-03-10&g2h=1&strict=1&gs=on>
  returned 1 Adar II 5784 and `afterSunset=true`.

Fixture reference instants are five minutes before and five minutes after the
independently recorded sunset. The TSV retains Hebcal's month names; the test
maps them to KosherJava's documented numbering, where Nisan is 1, Adar I is 12,
and Adar II is 13.

### High latitude

Zmanim query:

<https://www.hebcal.com/zmanim?cfg=json&latitude=69.6492&longitude=18.9553&tzid=Europe%2FOslo&date=2026-06-21&sec=1>

The response returned JSON `null` for sunrise, chatzot, and sunset. The civil
date [converter query](https://www.hebcal.com/converter?cfg=json&date=2026-06-21&g2h=1&strict=1)
returned 6 Tamuz 5786, but this is not used as an ASEH boundary result: without
a physical sunset, the current engine deliberately marks the location-based
Hebrew date unavailable.

Hebcal defines `chatzot` as sunrise plus six proportional hours, computed from
sea-level sunrise and sunset. ASEH currently exposes NOAA solar transit. During
polar day Hebcal therefore reports `chatzot=null`, while KosherJava can still
return solar transit. The test records and asserts this intentional method
difference rather than treating the two values as equivalent.

## Independence and limits

The production Hebcal endpoints provide an independently deployed service and
their current Go service documents separate `hebcal/hdate`, `hebcal-go`, and
`noaa-go` implementations. This gives useful protection against ASEH fixture
copying, time-zone mistakes, DST mistakes, and Hebrew-date boundary mistakes.

It is not fully independent astronomical lineage. Hebcal documents its NOAA
JavaScript implementation as a fork/subset ultimately based on KosherJava, and
the Go API describes `noaa-go` as the solar backend. The comparison therefore
checks separate ports and deployed outputs of the same NOAA family; it is not
evidence that two unrelated astronomical models agree.

Festival divergence between Israel and the diaspora and Torah-reading results
are deferred. The current `domain:zmanim` engine has no holiday calendar,
Israel/diaspora calendar-profile, or leyning output, so these fixtures make no
claim about those surfaces. They require independent golden cases when such an
API is added.

## License and transformation

Hebcal's [Developer APIs licensing notice](https://www.hebcal.com/home/developer-apis)
states that output from its web APIs is licensed under the
[Creative Commons Attribution 4.0 International License](https://creativecommons.org/licenses/by/4.0/).
Attribution: calendar conversion and zmanim observations are from
[Hebcal.com](https://www.hebcal.com/).

The TSV is a selection of response fields, not a verbatim archive. Transformations
are limited to tabular layout, `NULL`/`NONE` sentinels, fixture identifiers,
selection of reference instants around the recorded sunset, and an
ASEH-specific expected boundary classification. Source timestamps, Hebrew
year/month/day values, and `afterSunset` flags are retained as returned.
