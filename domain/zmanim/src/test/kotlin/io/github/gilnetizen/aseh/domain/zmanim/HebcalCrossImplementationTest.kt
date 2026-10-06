package io.github.gilnetizen.aseh.domain.zmanim

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline comparison against observations frozen from the independently deployed Hebcal API.
 * See HEBCAL_GOLDEN_PROVENANCE.md for queries, licensing, lineage, and accepted tolerances.
 */
class HebcalCrossImplementationTest {
    private val engine = KosherJavaZmanimEngine()

    @Test
    fun frozenHebcalCasesAgreeWithinTheDocumentedTwoMinuteTolerance() {
        val fixtures = loadFixtures()

        assertEquals(
            setOf(
                "jerusalem-baseline",
                "jerusalem-after-sunset",
                "new-york-diaspora",
                "new-york-dst-standard",
                "new-york-dst-daylight",
                "adar-leap-boundary-before",
                "adar-leap-boundary-after",
                "tromso-polar-day",
            ),
            fixtures.mapTo(mutableSetOf(), HebcalFixture::id),
        )

        fixtures.forEach { fixture ->
            val result = engine.calculate(fixture.toRequest())

            assertHorizonEvent(
                fixture = fixture,
                label = "sunrise",
                expected = fixture.hebcalSunrise,
                actual = result.sunrise,
                expectedUnavailableReason = ZmanUnavailability.NO_HORIZON_CROSSING,
            )
            assertHorizonEvent(
                fixture = fixture,
                label = "sunset",
                expected = fixture.hebcalSunset,
                actual = result.sunset,
                expectedUnavailableReason = ZmanUnavailability.NO_HORIZON_CROSSING,
            )

            if (fixture.hebcalChatzot == null) {
                // Hebcal defines chatzot as the midpoint of sea-level sunrise and sunset,
                // while ASEH exposes NOAA solar transit. During polar day the former is
                // unavailable and the latter is still calculable.
                assertEquals("tromso-polar-day", fixture.id)
                assertTrue(
                    "${fixture.id} should retain NOAA solar transit despite Hebcal chatzot=null",
                    result.solarNoonChatzot is ZmanCalculation.Available,
                )
            } else {
                assertAvailableWithinTolerance(
                    fixture = fixture,
                    label = "chatzot",
                    expected = fixture.hebcalChatzot,
                    actual = result.solarNoonChatzot,
                )
            }

            assertHebrewDate(fixture, result.hebrewDate)
        }
    }

    @Test
    fun newYorkFixturesExerciseThe2026SpringDstOffsetChange() {
        val fixtures = loadFixtures().associateBy(HebcalFixture::id)
        val standard = fixtures.getValue("new-york-dst-standard")
        val daylight = fixtures.getValue("new-york-dst-daylight")

        assertEquals(ZoneOffset.ofHours(-5), standard.hebcalSunrise!!.offset)
        assertEquals(ZoneOffset.ofHours(-4), daylight.hebcalSunrise!!.offset)

        val standardResult = engine.calculate(standard.toRequest())
        val daylightResult = engine.calculate(daylight.toRequest())
        assertEquals(
            ZoneOffset.ofHours(-5),
            (standardResult.sunrise as ZmanCalculation.Available)
                .instant.atZone(standard.zoneId).offset,
        )
        assertEquals(
            ZoneOffset.ofHours(-4),
            (daylightResult.sunrise as ZmanCalculation.Available)
                .instant.atZone(daylight.zoneId).offset,
        )
    }

    private fun assertHorizonEvent(
        fixture: HebcalFixture,
        label: String,
        expected: OffsetDateTime?,
        actual: ZmanCalculation,
        expectedUnavailableReason: ZmanUnavailability,
    ) {
        if (expected == null) {
            assertEquals(
                "${fixture.id} $label availability",
                ZmanCalculation.Unavailable(actual.type, expectedUnavailableReason),
                actual,
            )
        } else {
            assertAvailableWithinTolerance(fixture, label, expected, actual)
        }
    }

    private fun assertAvailableWithinTolerance(
        fixture: HebcalFixture,
        label: String,
        expected: OffsetDateTime,
        actual: ZmanCalculation,
    ) {
        assertTrue("${fixture.id} $label should be available", actual is ZmanCalculation.Available)
        actual as ZmanCalculation.Available

        val difference = abs(Duration.between(expected.toInstant(), actual.instant).toMillis())
        assertTrue(
            "${fixture.id} $label differs from Hebcal by ${difference}ms",
            difference <= HEBCAL_DOCUMENTED_TOLERANCE.toMillis(),
        )
        assertEquals(
            "${fixture.id} $label local UTC offset",
            expected.offset,
            actual.instant.atZone(fixture.zoneId).offset,
        )
    }

    private fun assertHebrewDate(
        fixture: HebcalFixture,
        actual: HebrewDateCalculation,
    ) {
        when (fixture.expectedEngineBoundary) {
            "UNAVAILABLE_SUNSET" -> assertEquals(
                "${fixture.id} Hebrew boundary availability",
                HebrewDateCalculation.Unavailable(HebrewDateUnavailability.SUNSET_UNAVAILABLE),
                actual,
            )

            "BEFORE_SUNSET", "AT_OR_AFTER_SUNSET" -> {
                assertTrue("${fixture.id} Hebrew date should be available", actual is HebrewDateCalculation.Available)
                actual as HebrewDateCalculation.Available
                assertEquals("${fixture.id} Hebrew year", fixture.hebcalHebrewYear, actual.date.year)
                assertEquals(
                    "${fixture.id} Hebrew month",
                    hebcalMonthToKosherJavaNumber(fixture.hebcalHebrewMonth),
                    actual.date.month,
                )
                assertEquals("${fixture.id} Hebrew day", fixture.hebcalHebrewDay, actual.date.dayOfMonth)
                assertEquals(
                    "${fixture.id} sunset-boundary flag",
                    fixture.hebcalAfterSunset,
                    actual.boundary == HebrewDayBoundary.AT_OR_AFTER_SUNSET,
                )
                assertEquals(
                    "${fixture.id} boundary classification",
                    HebrewDayBoundary.valueOf(fixture.expectedEngineBoundary),
                    actual.boundary,
                )
            }

            else -> error("Unknown expected boundary ${fixture.expectedEngineBoundary}")
        }
    }

    private fun loadFixtures(): List<HebcalFixture> {
        val lines = checkNotNull(javaClass.getResourceAsStream("/hebcal-v1.11.3-golden.tsv")) {
            "missing Hebcal golden fixture resource"
        }.bufferedReader().use { reader ->
            reader.lineSequence()
                .filterNot { it.isBlank() || it.startsWith('#') }
                .toList()
        }

        val header = lines.first().split('\t')
        assertEquals(EXPECTED_COLUMNS, header)
        return lines.drop(1).map { line ->
            val values = line.split('\t')
            assertEquals("fixture column count for $line", EXPECTED_COLUMNS.size, values.size)
            val row = header.zip(values).toMap()
            HebcalFixture(
                id = row.getValue("id"),
                date = LocalDate.parse(row.getValue("date")),
                referenceInstant = Instant.parse(row.getValue("referenceInstant")),
                latitude = row.getValue("latitude").toDouble(),
                longitude = row.getValue("longitude").toDouble(),
                elevationMeters = row.getValue("elevationMeters")
                    .takeUnless { it == "NONE" }
                    ?.toDouble(),
                zoneId = ZoneId.of(row.getValue("zoneId")),
                hebcalSunrise = row.getValue("hebcalSunrise").toOffsetDateTimeOrNull(),
                hebcalChatzot = row.getValue("hebcalChatzot").toOffsetDateTimeOrNull(),
                hebcalSunset = row.getValue("hebcalSunset").toOffsetDateTimeOrNull(),
                hebcalHebrewYear = row.getValue("hebcalHebrewYear").toInt(),
                hebcalHebrewMonth = row.getValue("hebcalHebrewMonth"),
                hebcalHebrewDay = row.getValue("hebcalHebrewDay").toInt(),
                hebcalAfterSunset = row.getValue("hebcalAfterSunset").toBooleanStrict(),
                expectedEngineBoundary = row.getValue("expectedEngineBoundary"),
            )
        }
    }

    private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? =
        takeUnless { it == "NULL" }?.let(OffsetDateTime::parse)

    private fun hebcalMonthToKosherJavaNumber(name: String): Int = when (name) {
        "Nisan" -> 1
        "Iyyar" -> 2
        "Sivan" -> 3
        "Tamuz" -> 4
        "Av" -> 5
        "Elul" -> 6
        "Tishrei" -> 7
        "Cheshvan" -> 8
        "Kislev" -> 9
        "Tevet" -> 10
        "Shvat" -> 11
        "Adar", "Adar I" -> 12
        "Adar II" -> 13
        else -> error("Unknown Hebcal month $name")
    }

    private data class HebcalFixture(
        val id: String,
        val date: LocalDate,
        val referenceInstant: Instant,
        val latitude: Double,
        val longitude: Double,
        val elevationMeters: Double?,
        val zoneId: ZoneId,
        val hebcalSunrise: OffsetDateTime?,
        val hebcalChatzot: OffsetDateTime?,
        val hebcalSunset: OffsetDateTime?,
        val hebcalHebrewYear: Int,
        val hebcalHebrewMonth: String,
        val hebcalHebrewDay: Int,
        val hebcalAfterSunset: Boolean,
        val expectedEngineBoundary: String,
    ) {
        fun toRequest() = ZmanimRequest(
            date = date,
            referenceInstant = referenceInstant,
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevationMeters,
            zoneId = zoneId,
        )
    }

    private companion object {
        val HEBCAL_DOCUMENTED_TOLERANCE: Duration = Duration.ofMinutes(2)

        val EXPECTED_COLUMNS = listOf(
            "id",
            "date",
            "referenceInstant",
            "latitude",
            "longitude",
            "elevationMeters",
            "zoneId",
            "hebcalSunrise",
            "hebcalChatzot",
            "hebcalSunset",
            "hebcalHebrewYear",
            "hebcalHebrewMonth",
            "hebcalHebrewDay",
            "hebcalAfterSunset",
            "expectedEngineBoundary",
        )
    }
}
