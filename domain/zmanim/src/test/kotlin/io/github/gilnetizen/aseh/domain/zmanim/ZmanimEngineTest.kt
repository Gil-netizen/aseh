package io.github.gilnetizen.aseh.domain.zmanim

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ZmanimEngineTest {
    private val engine = KosherJavaZmanimEngine()

    @Test
    fun jerusalemGoldenDay() {
        val result = engine.calculate(
            request(
                date = "2026-10-06",
                instant = "2026-10-06T09:00:00Z",
                latitude = 31.778,
                longitude = 35.235,
                elevationMeters = 754.0,
                zoneId = "Asia/Jerusalem",
            ),
        )

        assertEquals(
            ZmanCalculation.Available(
                ZmanimEventType.SUNRISE,
                Instant.parse("2026-10-06T03:31:50.442Z"),
            ),
            result.sunrise,
        )
        assertEquals(
            ZmanCalculation.Available(
                ZmanimEventType.SOLAR_NOON_CHATZOT,
                Instant.parse("2026-10-06T09:27:21.464Z"),
            ),
            result.solarNoonChatzot,
        )
        assertEquals(
            ZmanCalculation.Available(
                ZmanimEventType.SUNSET,
                Instant.parse("2026-10-06T15:22:06.393Z"),
            ),
            result.sunset,
        )
        assertEquals(
            HebrewDateCalculation.Available(
                date = HebrewDate(
                    year = 5787,
                    month = 7,
                    dayOfMonth = 25,
                    transliterated = "25 Tishrei, 5787",
                    hebrew = "כ״ה תשרי תשפ״ז",
                ),
                boundary = HebrewDayBoundary.BEFORE_SUNSET,
            ),
            result.hebrewDate,
        )
        assertEquals(
            ZmanimEvent(
                ZmanimEventType.SOLAR_NOON_CHATZOT,
                Instant.parse("2026-10-06T09:27:21.464Z"),
            ),
            result.nextEvent,
        )
        assertEquals("2.5.0", result.metadata.engineVersion)
        assertEquals(
            "US National Oceanic and Atmospheric Administration Algorithm",
            result.metadata.algorithmName,
        )
        assertEquals(ZoneId.of("Asia/Jerusalem"), result.metadata.zoneId)
        assertTrue(result.metadata.sunriseAndSunsetAreElevationAdjusted)
    }

    @Test
    fun newYorkDiasporaGoldenDay() {
        val result = engine.calculate(
            request(
                date = "2026-12-01",
                instant = "2026-12-01T15:00:00Z",
                latitude = 40.7128,
                longitude = -74.006,
                elevationMeters = 10.0,
                zoneId = "America/New_York",
            ),
        )

        assertEquals(
            ZmanCalculation.Available(
                ZmanimEventType.SUNRISE,
                Instant.parse("2026-12-01T12:00:07.703Z"),
            ),
            result.sunrise,
        )
        assertEquals(
            ZmanCalculation.Available(
                ZmanimEventType.SOLAR_NOON_CHATZOT,
                Instant.parse("2026-12-01T16:44:56.255Z"),
            ),
            result.solarNoonChatzot,
        )
        assertEquals(
            ZmanCalculation.Available(
                ZmanimEventType.SUNSET,
                Instant.parse("2026-12-01T21:29:52.298Z"),
            ),
            result.sunset,
        )
        assertEquals(
            HebrewDateCalculation.Available(
                date = HebrewDate(
                    year = 5787,
                    month = 9,
                    dayOfMonth = 21,
                    transliterated = "21 Kislev, 5787",
                    hebrew = "כ״א כסלו תשפ״ז",
                ),
                boundary = HebrewDayBoundary.BEFORE_SUNSET,
            ),
            result.hebrewDate,
        )
        assertEquals(
            ZmanimEvent(
                ZmanimEventType.SOLAR_NOON_CHATZOT,
                Instant.parse("2026-12-01T16:44:56.255Z"),
            ),
            result.nextEvent,
        )
    }

    @Test
    fun afterSunsetUsesNextHebrewDate() {
        val result = engine.calculate(
            request(
                date = "2026-10-06",
                instant = "2026-10-06T17:00:00Z",
                latitude = 31.778,
                longitude = 35.235,
                elevationMeters = 754.0,
                zoneId = "Asia/Jerusalem",
            ),
        )

        val hebrewDate = result.hebrewDate as HebrewDateCalculation.Available
        assertEquals(HebrewDayBoundary.AT_OR_AFTER_SUNSET, hebrewDate.boundary)
        assertEquals(26, hebrewDate.date.dayOfMonth)
        assertEquals(
            ZmanimEvent(
                ZmanimEventType.SUNRISE,
                Instant.parse("2026-10-07T03:32:29.922Z"),
            ),
            result.nextEvent,
        )
    }

    @Test
    fun polarDayMakesHorizonEventsAndHebrewBoundaryUnavailable() {
        val result = engine.calculate(
            request(
                date = "2026-06-21",
                instant = "2026-06-21T00:00:00Z",
                latitude = 69.6492,
                longitude = 18.9553,
                elevationMeters = 0.0,
                zoneId = "Europe/Oslo",
            ),
        )

        assertEquals(
            ZmanCalculation.Unavailable(
                ZmanimEventType.SUNRISE,
                ZmanUnavailability.NO_HORIZON_CROSSING,
            ),
            result.sunrise,
        )
        assertEquals(
            ZmanCalculation.Unavailable(
                ZmanimEventType.SUNSET,
                ZmanUnavailability.NO_HORIZON_CROSSING,
            ),
            result.sunset,
        )
        assertEquals(
            HebrewDateCalculation.Unavailable(HebrewDateUnavailability.SUNSET_UNAVAILABLE),
            result.hebrewDate,
        )
        assertEquals(
            ZmanCalculation.Available(
                ZmanimEventType.SOLAR_NOON_CHATZOT,
                Instant.parse("2026-06-21T10:45:52.985Z"),
            ),
            result.solarNoonChatzot,
        )
        assertEquals(
            ZmanimEvent(
                ZmanimEventType.SOLAR_NOON_CHATZOT,
                Instant.parse("2026-06-21T10:45:52.985Z"),
            ),
            result.nextEvent,
        )
    }

    @Test
    fun belowSeaLevelElevationUsesAnExplicitSeaLevelFallback() {
        val belowSeaLevel = engine.calculate(
            request(
                date = "2026-10-06",
                instant = "2026-10-06T09:00:00Z",
                latitude = 31.5,
                longitude = 35.5,
                elevationMeters = -430.0,
                zoneId = "Asia/Jerusalem",
            ),
        )
        val seaLevel = engine.calculate(
            request(
                date = "2026-10-06",
                instant = "2026-10-06T09:00:00Z",
                latitude = 31.5,
                longitude = 35.5,
                elevationMeters = 0.0,
                zoneId = "Asia/Jerusalem",
            ),
        )

        assertEquals(-430.0, belowSeaLevel.metadata.reportedElevationMeters)
        assertEquals(0.0, belowSeaLevel.metadata.effectiveElevationMeters, 0.0)
        assertEquals(
            ElevationHandling.SEA_LEVEL_FOR_BELOW_SEA_LEVEL_LOCATION,
            belowSeaLevel.metadata.elevationHandling,
        )
        assertEquals(seaLevel.sunrise, belowSeaLevel.sunrise)
        assertEquals(seaLevel.sunset, belowSeaLevel.sunset)
    }

    @Test
    fun rejectsInvalidAndInternallyInconsistentInputs() {
        val invalidLatitude = assertThrows(IllegalArgumentException::class.java) {
            engine.calculate(
                request(
                    date = "2026-10-06",
                    instant = "2026-10-06T09:00:00Z",
                    latitude = 91.0,
                    longitude = 35.235,
                    elevationMeters = 754.0,
                    zoneId = "Asia/Jerusalem",
                ),
            )
        }
        assertEquals(
            "latitude must be finite and between -90 and 90 degrees",
            invalidLatitude.message,
        )

        val mismatchedDate = assertThrows(IllegalArgumentException::class.java) {
            engine.calculate(
                request(
                    date = "2026-10-07",
                    instant = "2026-10-06T09:00:00Z",
                    latitude = 31.778,
                    longitude = 35.235,
                    elevationMeters = 754.0,
                    zoneId = "Asia/Jerusalem",
                ),
            )
        }
        assertEquals("referenceInstant must fall on date in zoneId", mismatchedDate.message)

        val invalidElevation = assertThrows(IllegalArgumentException::class.java) {
            engine.calculate(
                request(
                    date = "2026-10-06",
                    instant = "2026-10-06T09:00:00Z",
                    latitude = 31.778,
                    longitude = 35.235,
                    elevationMeters = Double.NaN,
                    zoneId = "Asia/Jerusalem",
                ),
            )
        }
        assertEquals("elevationMeters must be null or finite", invalidElevation.message)
    }

    private fun request(
        date: String,
        instant: String,
        latitude: Double,
        longitude: Double,
        elevationMeters: Double?,
        zoneId: String,
    ) = ZmanimRequest(
        date = LocalDate.parse(date),
        referenceInstant = Instant.parse(instant),
        latitude = latitude,
        longitude = longitude,
        elevationMeters = elevationMeters,
        zoneId = ZoneId.of(zoneId),
    )
}
