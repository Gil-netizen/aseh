package io.github.gilnetizen.aseh.domain.calendar

import io.github.gilnetizen.aseh.domain.servicecatalog.CalendarDayOverride
import io.github.gilnetizen.aseh.domain.servicecatalog.CalendarRegion
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceDayKind
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A civil service date whose daytime Jewish date is to be resolved.
 *
 * Sunset-boundary selection belongs to the local zmanim layer. This input intentionally does not
 * accept an instant and never guesses whether an evening occurrence belongs to the following day.
 */
data class CalendarContextRequest(
    val civilServiceDate: LocalDate,
    val calendarRegion: CalendarRegion,
    val purimLocality: PurimLocality = PurimLocality.UNVERIFIED,
)

/** Geographic status must be supplied explicitly; Israel/diaspora does not establish it. */
enum class PurimLocality {
    UNVERIFIED,
    WALLED_CITY,
    NOT_WALLED_CITY,
}

enum class JewishMonth {
    NISAN,
    IYAR,
    SIVAN,
    TAMMUZ,
    AV,
    ELUL,
    TISHREI,
    CHESHVAN,
    KISLEV,
    TEVET,
    SHEVAT,
    ADAR,
    ADAR_I,
    ADAR_II,
}

data class ResolvedJewishDate(
    val year: Int,
    val month: JewishMonth,
    val dayOfMonth: Int,
    val isLeapYear: Boolean,
)

enum class CalendarObservanceCategory {
    FESTIVAL_WORK_RESTRICTED,
    FESTIVAL_INTERMEDIATE_DAY,
    FESTIVAL_EVE,
    FAST,
    ROSH_HODESH,
    MINOR_OBSERVANCE,
    POST_FESTIVAL_DAY,
    SPECIAL_SHABBAT,
    MODERN_OBSERVANCE,
    UNKNOWN,
}

/**
 * Stable, non-textual calendar metadata. These IDs do not supply prayers, readings, rulings, or
 * a license to render content.
 */
enum class CalendarObservanceId(
    val stableId: String,
    val fallbackEnglish: String,
    val categories: Set<CalendarObservanceCategory>,
) {
    EREV_PESACH("erev-pesach", "Erev Pesach", categories(CalendarObservanceCategory.FESTIVAL_EVE)),
    PESACH("pesach", "Pesach", categories(CalendarObservanceCategory.FESTIVAL_WORK_RESTRICTED)),
    HOL_HAMOED_PESACH(
        "hol-hamoed-pesach",
        "Hol ha-mo'ed Pesach",
        categories(CalendarObservanceCategory.FESTIVAL_INTERMEDIATE_DAY),
    ),
    PESACH_SHENI("pesach-sheni", "Pesach Sheni", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    EREV_SHAVUOT("erev-shavuot", "Erev Shavuot", categories(CalendarObservanceCategory.FESTIVAL_EVE)),
    SHAVUOT("shavuot", "Shavuot", categories(CalendarObservanceCategory.FESTIVAL_WORK_RESTRICTED)),
    FAST_OF_SEVENTEENTH_OF_TAMMUZ(
        "fast-17-tammuz",
        "Fast of 17 Tammuz",
        categories(CalendarObservanceCategory.FAST),
    ),
    TISHA_BEAV("tisha-beav", "Tisha BeAv", categories(CalendarObservanceCategory.FAST)),
    TU_BEAV("tu-beav", "Tu BeAv", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    EREV_ROSH_HASHANAH(
        "erev-rosh-hashanah",
        "Erev Rosh Hashanah",
        categories(CalendarObservanceCategory.FESTIVAL_EVE),
    ),
    ROSH_HASHANAH(
        "rosh-hashanah",
        "Rosh Hashanah",
        categories(CalendarObservanceCategory.FESTIVAL_WORK_RESTRICTED),
    ),
    FAST_OF_GEDALIAH("fast-gedaliah", "Fast of Gedaliah", categories(CalendarObservanceCategory.FAST)),
    EREV_YOM_KIPPUR(
        "erev-yom-kippur",
        "Erev Yom Kippur",
        categories(CalendarObservanceCategory.FESTIVAL_EVE),
    ),
    YOM_KIPPUR(
        "yom-kippur",
        "Yom Kippur",
        categories(CalendarObservanceCategory.FESTIVAL_WORK_RESTRICTED, CalendarObservanceCategory.FAST),
    ),
    EREV_SUKKOT("erev-sukkot", "Erev Sukkot", categories(CalendarObservanceCategory.FESTIVAL_EVE)),
    SUKKOT("sukkot", "Sukkot", categories(CalendarObservanceCategory.FESTIVAL_WORK_RESTRICTED)),
    HOL_HAMOED_SUKKOT(
        "hol-hamoed-sukkot",
        "Hol ha-mo'ed Sukkot",
        categories(CalendarObservanceCategory.FESTIVAL_INTERMEDIATE_DAY),
    ),
    HOSHANA_RABBAH(
        "hoshana-rabbah",
        "Hoshana Rabbah",
        categories(CalendarObservanceCategory.FESTIVAL_INTERMEDIATE_DAY),
    ),
    SHEMINI_ATZERET(
        "shemini-atzeret",
        "Shemini Atzeret",
        categories(CalendarObservanceCategory.FESTIVAL_WORK_RESTRICTED),
    ),
    SIMCHAT_TORAH(
        "simchat-torah",
        "Simchat Torah",
        categories(CalendarObservanceCategory.FESTIVAL_WORK_RESTRICTED),
    ),
    HANUKKAH("hanukkah", "Hanukkah", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    FAST_OF_TENTH_OF_TEVET(
        "fast-10-tevet",
        "Fast of 10 Tevet",
        categories(CalendarObservanceCategory.FAST),
    ),
    TU_BISHVAT("tu-bishvat", "Tu BiShvat", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    FAST_OF_ESTHER("fast-esther", "Fast of Esther", categories(CalendarObservanceCategory.FAST)),
    PURIM("purim", "Purim", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    SHUSHAN_PURIM("shushan-purim", "Shushan Purim", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    PURIM_KATAN("purim-katan", "Purim Katan", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    SHUSHAN_PURIM_KATAN(
        "shushan-purim-katan",
        "Shushan Purim Katan",
        categories(CalendarObservanceCategory.MINOR_OBSERVANCE),
    ),
    ROSH_HODESH("rosh-hodesh", "Rosh Hodesh", categories(CalendarObservanceCategory.ROSH_HODESH)),
    LAG_BAOMER("lag-baomer", "Lag BaOmer", categories(CalendarObservanceCategory.MINOR_OBSERVANCE)),
    ISRU_HAG("isru-hag", "Isru Hag", categories(CalendarObservanceCategory.POST_FESTIVAL_DAY)),
    YOM_KIPPUR_KATAN("yom-kippur-katan", "Yom Kippur Katan", categories(CalendarObservanceCategory.FAST)),
    BEHAB("behab", "BeHaB", categories(CalendarObservanceCategory.FAST)),
    FAST_OF_FIRSTBORN("fast-firstborn", "Fast of the Firstborn", categories(CalendarObservanceCategory.FAST)),
    YOM_HASHOAH("yom-hashoah", "Yom HaShoah", categories(CalendarObservanceCategory.MODERN_OBSERVANCE)),
    YOM_HAZIKARON("yom-hazikaron", "Yom HaZikaron", categories(CalendarObservanceCategory.MODERN_OBSERVANCE)),
    YOM_HAATZMAUT("yom-haatzmaut", "Yom HaAtzmaut", categories(CalendarObservanceCategory.MODERN_OBSERVANCE)),
    YOM_YERUSHALAYIM(
        "yom-yerushalayim",
        "Yom Yerushalayim",
        categories(CalendarObservanceCategory.MODERN_OBSERVANCE),
    ),
    SHABBAT_SHEKALIM(
        "shabbat-shekalim",
        "Shabbat Shekalim",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_ZACHOR(
        "shabbat-zachor",
        "Shabbat Zachor",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_PARAH(
        "shabbat-parah",
        "Shabbat Parah",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_HAHODESH(
        "shabbat-hahodesh",
        "Shabbat HaHodesh",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_SHUVA(
        "shabbat-shuva",
        "Shabbat Shuva",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_SHIRAH(
        "shabbat-shirah",
        "Shabbat Shirah",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_HAGADOL(
        "shabbat-hagadol",
        "Shabbat HaGadol",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_HAZON(
        "shabbat-hazon",
        "Shabbat Hazon",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    SHABBAT_NAHAMU(
        "shabbat-nahamu",
        "Shabbat Nahamu",
        categories(CalendarObservanceCategory.SPECIAL_SHABBAT),
    ),
    UNRECOGNIZED_LIBRARY_EVENT(
        "unrecognized-library-event",
        "Unrecognized calendar event",
        categories(CalendarObservanceCategory.UNKNOWN),
    ),
}

private fun categories(
    vararg values: CalendarObservanceCategory,
): Set<CalendarObservanceCategory> = values.toSet()

enum class CalendarSignalSource {
    KOSHERJAVA_HOLIDAY_INDEX,
    KOSHERJAVA_ROSH_HODESH_FLAG,
    KOSHERJAVA_FAST_OF_FIRSTBORN_FLAG,
    KOSHERJAVA_SPECIAL_SHABBAT,
}

data class CalendarObservance(
    val id: CalendarObservanceId,
    val source: CalendarSignalSource,
    val sourceValue: String,
)

enum class ServiceDayClassificationRule {
    WORK_RESTRICTED_FESTIVAL_TAKES_PRECEDENCE,
    SATURDAY_WHEN_NO_WORK_RESTRICTED_FESTIVAL,
    WEEKDAY_WHEN_NEITHER_RULE_APPLIES,
}

enum class CalendarDateBasis {
    DAYTIME_OF_CIVIL_SERVICE_DATE,
}

enum class ModernObservancePolicy {
    EXCLUDED_PENDING_REVIEW,
}

data class RawCalendarSignals(
    val dayOfWeek: DayOfWeek,
    val kosherJavaHolidayIndex: Int?,
    val kosherJavaSpecialShabbatName: String?,
    val isYomTov: Boolean,
    val isWorkRestrictedFestival: Boolean,
    val isFastDay: Boolean,
    val isFastOfFirstborn: Boolean,
    val isRoshHodesh: Boolean,
)

enum class UnresolvedCalendarField {
    SUNSET_DAY_BOUNDARY,
    WEEKLY_TORAH_READING,
    FESTIVAL_TORAH_READING,
    LITURGICAL_INSERTIONS,
    SEASONAL_PRAYER_CHANGES,
    MODERN_OBSERVANCES,
    USER_OR_COMMUNITY_EVENTS,
    SERVICE_CATALOG_ADDITION_MAPPING,
    PURIM_LOCALITY,
    LIBRARY_EVENT_MAPPING,
}

enum class UnresolvedReason {
    OUTSIDE_MODULE_SCOPE,
    REQUIRES_REVIEWED_CONTENT_OR_POLICY,
    REQUIRES_EXPLICIT_LOCAL_INPUT,
    UNRECOGNIZED_PINNED_LIBRARY_VALUE,
}

data class UnresolvedCalendarValue(
    val field: UnresolvedCalendarField,
    val reason: UnresolvedReason,
    val detail: String,
)

data class CalendarResolutionDetails(
    val engineName: String,
    val engineVersion: String,
    val dateBasis: CalendarDateBasis,
    val regionWasExplicitInput: Boolean,
    val modernObservancePolicy: ModernObservancePolicy,
    val purimLocality: PurimLocality,
    val classificationRule: ServiceDayClassificationRule,
    val rawSignals: RawCalendarSignals,
)

data class ResolvedCalendarContext(
    val request: CalendarContextRequest,
    val jewishDate: ResolvedJewishDate,
    val observances: List<CalendarObservance>,
    val isShabbat: Boolean,
    val isFastDay: Boolean,
    val isRoshHodesh: Boolean,
    val serviceDayKind: ServiceDayKind,
    /** Direct bridge for [io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionRequest]. */
    val serviceCatalogOverride: CalendarDayOverride,
    val details: CalendarResolutionDetails,
    val unresolvedValues: List<UnresolvedCalendarValue>,
) {
    init {
        require(serviceCatalogOverride.date == request.civilServiceDate)
        require(serviceCatalogOverride.dayKind == serviceDayKind)
        require(serviceCatalogOverride.applicableRegions == setOf(request.calendarRegion))
    }
}

enum class CalendarContextUnavailability {
    CALCULATION_FAILED,
}

sealed interface CalendarContextResolution {
    data class Resolved(val context: ResolvedCalendarContext) : CalendarContextResolution

    data class Unavailable(
        val request: CalendarContextRequest,
        val reason: CalendarContextUnavailability,
        val detail: String,
    ) : CalendarContextResolution
}

data class CalendarContextRangeRequest(
    val startDate: LocalDate,
    val endDateInclusive: LocalDate,
    val calendarRegion: CalendarRegion,
    val purimLocality: PurimLocality = PurimLocality.UNVERIFIED,
) {
    init {
        require(!endDateInclusive.isBefore(startDate)) { "Calendar range cannot end before it starts." }
        require(endDateInclusive.toEpochDay() - startDate.toEpochDay() < MAX_RANGE_DAYS) {
            "Calendar range cannot exceed $MAX_RANGE_DAYS inclusive days."
        }
    }

    companion object {
        const val MAX_RANGE_DAYS = 367L
    }
}

data class CalendarContextRangeResolution(
    val days: List<CalendarContextResolution>,
) {
    val resolvedContexts: List<ResolvedCalendarContext>
        get() = days.mapNotNull { result ->
            (result as? CalendarContextResolution.Resolved)?.context
        }

    val serviceCatalogOverrides: List<CalendarDayOverride>
        get() {
            require(unavailableDays.isEmpty()) {
                "A partial calendar range cannot supply service-catalog overrides."
            }
            return resolvedContexts.map(ResolvedCalendarContext::serviceCatalogOverride)
        }

    val unavailableDays: List<CalendarContextResolution.Unavailable>
        get() = days.mapNotNull { result -> result as? CalendarContextResolution.Unavailable }
}
