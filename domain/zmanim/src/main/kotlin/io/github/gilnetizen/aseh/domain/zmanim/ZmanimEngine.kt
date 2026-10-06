package io.github.gilnetizen.aseh.domain.zmanim

import com.kosherjava.zmanim.AstronomicalCalendar
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.kosherjava.zmanim.util.GeoLocation
import com.kosherjava.zmanim.util.NOAACalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.TimeZone

/** Inputs for one deterministic, offline calculation. */
data class ZmanimRequest(
    val date: LocalDate,
    val referenceInstant: Instant,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double? = null,
    val zoneId: ZoneId,
)

/** Jewish month numbering follows KosherJava: Nisan is 1 and Tishrei is 7. */
data class HebrewDate(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val transliterated: String,
    val hebrew: String,
)

enum class HebrewDayBoundary {
    BEFORE_SUNSET,
    AT_OR_AFTER_SUNSET,
}

enum class HebrewDateUnavailability {
    SUNSET_UNAVAILABLE,
}

sealed interface HebrewDateCalculation {
    data class Available(
        val date: HebrewDate,
        val boundary: HebrewDayBoundary,
    ) : HebrewDateCalculation

    data class Unavailable(
        val reason: HebrewDateUnavailability,
    ) : HebrewDateCalculation
}

enum class ZmanimEventType {
    SUNRISE,
    SOLAR_NOON_CHATZOT,
    SUNSET,
}

enum class ZmanUnavailability {
    NO_HORIZON_CROSSING,
    ASTRONOMICAL_CALCULATION_UNAVAILABLE,
}

sealed interface ZmanCalculation {
    val type: ZmanimEventType

    data class Available(
        override val type: ZmanimEventType,
        val instant: Instant,
    ) : ZmanCalculation

    data class Unavailable(
        override val type: ZmanimEventType,
        val reason: ZmanUnavailability,
    ) : ZmanCalculation
}

data class ZmanimEvent(
    val type: ZmanimEventType,
    val instant: Instant,
)

enum class SolarNoonMethod {
    NOAA_SOLAR_TRANSIT,
}

enum class HebrewDateBoundaryRule {
    LOCAL_SUNSET,
}

enum class NextEventRule {
    FIRST_EVENT_AT_OR_AFTER_REFERENCE_INSTANT_ACROSS_REQUESTED_AND_FOLLOWING_DATE,
}

enum class ElevationHandling {
    REPORTED_ELEVATION,
    SEA_LEVEL_WHEN_NOT_REPORTED,
    SEA_LEVEL_FOR_BELOW_SEA_LEVEL_LOCATION,
}

/** Machine-readable details needed to explain or reproduce a result. */
data class CalculationMetadata(
    val engineName: String,
    val engineVersion: String,
    val algorithmName: String,
    val date: LocalDate,
    val referenceInstant: Instant,
    val latitude: Double,
    val longitude: Double,
    val reportedElevationMeters: Double?,
    val effectiveElevationMeters: Double,
    val elevationHandling: ElevationHandling,
    val zoneId: ZoneId,
    val geometricZenithDegrees: Double,
    val standardRefractionDegrees: Double,
    val solarRadiusDegrees: Double,
    val sunriseAndSunsetAreElevationAdjusted: Boolean,
    val solarNoonMethod: SolarNoonMethod,
    val hebrewDateBoundaryRule: HebrewDateBoundaryRule,
    val nextEventRule: NextEventRule,
)

data class ZmanimSnapshot(
    val date: LocalDate,
    val referenceInstant: Instant,
    val hebrewDate: HebrewDateCalculation,
    val sunrise: ZmanCalculation,
    val solarNoonChatzot: ZmanCalculation,
    val sunset: ZmanCalculation,
    val nextEvent: ZmanimEvent?,
    val metadata: CalculationMetadata,
)

/**
 * Narrow adapter around KosherJava. It performs no I/O and does not consult the
 * system clock, locale, location service, or default time zone.
 */
class KosherJavaZmanimEngine {
    fun calculate(request: ZmanimRequest): ZmanimSnapshot {
        validate(request)

        val elevation = effectiveElevation(request.elevationMeters)
        val day = calculateSolarDay(request, request.date, elevation.meters)
        val followingDay = calculateSolarDay(request, request.date.plusDays(1), elevation.meters)
        val sunset = day.sunset

        return ZmanimSnapshot(
            date = request.date,
            referenceInstant = request.referenceInstant,
            hebrewDate = calculateHebrewDate(request, sunset),
            sunrise = day.sunrise,
            solarNoonChatzot = day.solarNoon,
            sunset = sunset,
            nextEvent = (day.availableEvents + followingDay.availableEvents)
                .asSequence()
                .filter { !it.instant.isBefore(request.referenceInstant) }
                .minByOrNull(ZmanimEvent::instant),
            metadata = CalculationMetadata(
                engineName = ENGINE_NAME,
                engineVersion = ENGINE_VERSION,
                algorithmName = day.algorithmName,
                date = request.date,
                referenceInstant = request.referenceInstant,
                latitude = request.latitude,
                longitude = request.longitude,
                reportedElevationMeters = request.elevationMeters,
                effectiveElevationMeters = elevation.meters,
                elevationHandling = elevation.handling,
                zoneId = request.zoneId,
                geometricZenithDegrees = GEOMETRIC_ZENITH_DEGREES,
                standardRefractionDegrees = STANDARD_REFRACTION_DEGREES,
                solarRadiusDegrees = SOLAR_RADIUS_DEGREES,
                sunriseAndSunsetAreElevationAdjusted = true,
                solarNoonMethod = SolarNoonMethod.NOAA_SOLAR_TRANSIT,
                hebrewDateBoundaryRule = HebrewDateBoundaryRule.LOCAL_SUNSET,
                nextEventRule = NextEventRule
                    .FIRST_EVENT_AT_OR_AFTER_REFERENCE_INSTANT_ACROSS_REQUESTED_AND_FOLLOWING_DATE,
            ),
        )
    }

    private fun validate(request: ZmanimRequest) {
        require(request.date.year >= 1 && request.date < LocalDate.MAX) {
            "date must be between 0001-01-01 and ${LocalDate.MAX.minusDays(1)}"
        }
        require(request.latitude.isFinite() && request.latitude in -90.0..90.0) {
            "latitude must be finite and between -90 and 90 degrees"
        }
        require(request.longitude.isFinite() && request.longitude in -180.0..180.0) {
            "longitude must be finite and between -180 and 180 degrees"
        }
        require(request.elevationMeters == null || request.elevationMeters.isFinite()) {
            "elevationMeters must be null or finite"
        }
        require(request.referenceInstant.atZone(request.zoneId).toLocalDate() == request.date) {
            "referenceInstant must fall on date in zoneId"
        }
    }

    private fun calculateHebrewDate(
        request: ZmanimRequest,
        sunset: ZmanCalculation,
    ): HebrewDateCalculation {
        if (sunset !is ZmanCalculation.Available) {
            return HebrewDateCalculation.Unavailable(HebrewDateUnavailability.SUNSET_UNAVAILABLE)
        }

        val atOrAfterSunset = !request.referenceInstant.isBefore(sunset.instant)
        val effectiveCivilDate = if (atOrAfterSunset) request.date.plusDays(1) else request.date
        val jewishDate = JewishDate(effectiveCivilDate)
        val transliteratedFormatter = HebrewDateFormatter()
        val hebrewFormatter = HebrewDateFormatter().apply { setHebrewFormat(true) }

        return HebrewDateCalculation.Available(
            date = HebrewDate(
                year = jewishDate.jewishYear,
                month = jewishDate.jewishMonth,
                dayOfMonth = jewishDate.jewishDayOfMonth,
                transliterated = transliteratedFormatter.format(jewishDate),
                hebrew = hebrewFormatter.format(jewishDate),
            ),
            boundary = if (atOrAfterSunset) {
                HebrewDayBoundary.AT_OR_AFTER_SUNSET
            } else {
                HebrewDayBoundary.BEFORE_SUNSET
            },
        )
    }

    private fun effectiveElevation(reportedMeters: Double?): EffectiveElevation = when {
        reportedMeters == null -> EffectiveElevation(
            meters = 0.0,
            handling = ElevationHandling.SEA_LEVEL_WHEN_NOT_REPORTED,
        )
        reportedMeters < 0.0 -> EffectiveElevation(
            meters = 0.0,
            handling = ElevationHandling.SEA_LEVEL_FOR_BELOW_SEA_LEVEL_LOCATION,
        )
        else -> EffectiveElevation(
            meters = reportedMeters,
            handling = ElevationHandling.REPORTED_ELEVATION,
        )
    }

    private fun calculateSolarDay(
        request: ZmanimRequest,
        date: LocalDate,
        effectiveElevationMeters: Double,
    ): SolarDay {
        val timeZone = TimeZone.getTimeZone(request.zoneId)
        val geoLocation = GeoLocation(
            "ASEH calculation",
            request.latitude,
            request.longitude,
            effectiveElevationMeters,
            timeZone,
        )
        val calculator = NOAACalculator()
        val calendar = AstronomicalCalendar(geoLocation).apply {
            setAstronomicalCalculator(calculator)
            setCalendar(
                GregorianCalendar(timeZone).apply {
                    clear()
                    set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0, 0)
                },
            )
        }

        val sunrise = calendar.sunrise?.toInstant()?.let {
            ZmanCalculation.Available(ZmanimEventType.SUNRISE, it)
        } ?: ZmanCalculation.Unavailable(
            ZmanimEventType.SUNRISE,
            ZmanUnavailability.NO_HORIZON_CROSSING,
        )
        val solarNoon = calendar.sunTransit?.toInstant()?.let {
            ZmanCalculation.Available(ZmanimEventType.SOLAR_NOON_CHATZOT, it)
        } ?: ZmanCalculation.Unavailable(
            ZmanimEventType.SOLAR_NOON_CHATZOT,
            ZmanUnavailability.ASTRONOMICAL_CALCULATION_UNAVAILABLE,
        )
        val sunset = calendar.sunset?.toInstant()?.let {
            ZmanCalculation.Available(ZmanimEventType.SUNSET, it)
        } ?: ZmanCalculation.Unavailable(
            ZmanimEventType.SUNSET,
            ZmanUnavailability.NO_HORIZON_CROSSING,
        )

        return SolarDay(
            sunrise = sunrise,
            solarNoon = solarNoon,
            sunset = sunset,
            algorithmName = calculator.calculatorName,
        )
    }

    private data class SolarDay(
        val sunrise: ZmanCalculation,
        val solarNoon: ZmanCalculation,
        val sunset: ZmanCalculation,
        val algorithmName: String,
    ) {
        val availableEvents: List<ZmanimEvent>
            get() = listOf(sunrise, solarNoon, sunset).mapNotNull { calculation ->
                (calculation as? ZmanCalculation.Available)?.let {
                    ZmanimEvent(it.type, it.instant)
                }
            }
    }

    private data class EffectiveElevation(
        val meters: Double,
        val handling: ElevationHandling,
    )

    private companion object {
        const val ENGINE_NAME = "KosherJava Zmanim API"
        const val ENGINE_VERSION = "2.5.0"
        const val GEOMETRIC_ZENITH_DEGREES = 90.0
        const val STANDARD_REFRACTION_DEGREES = 34.0 / 60.0
        const val SOLAR_RADIUS_DEGREES = 16.0 / 60.0
    }
}
