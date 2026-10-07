package io.github.gilnetizen.aseh.domain.calendar

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar.Parsha
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import io.github.gilnetizen.aseh.domain.servicecatalog.CalendarDayOverride
import io.github.gilnetizen.aseh.domain.servicecatalog.CalendarRegion
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceDayKind
import io.github.gilnetizen.aseh.domain.servicecatalog.UserFacingText
import java.time.DayOfWeek
import java.time.DateTimeException

/**
 * Deterministic, offline adapter around the pinned KosherJava calendar API.
 *
 * The adapter never reads a clock, time zone, locale, location, network, or default calendar
 * profile. It deliberately does not call KosherJava's weekly-parsha or tefila-rule APIs.
 */
class KosherJavaCalendarContextEngine {
    fun resolve(request: CalendarContextRequest): CalendarContextResolution {
        return try {
            CalendarContextResolution.Resolved(resolveChecked(request))
        } catch (_: DateTimeException) {
            calculationFailure(request)
        } catch (_: IllegalArgumentException) {
            calculationFailure(request)
        } catch (_: IllegalStateException) {
            calculationFailure(request)
        }
    }

    fun resolveRange(request: CalendarContextRangeRequest): CalendarContextRangeResolution {
        val days = buildList {
            var date = request.startDate
            while (true) {
                add(
                    resolve(
                        CalendarContextRequest(
                            civilServiceDate = date,
                            calendarRegion = request.calendarRegion,
                            purimLocality = request.purimLocality,
                        ),
                    ),
                )
                if (date == request.endDateInclusive) break
                date = date.plusDays(1)
            }
        }
        return CalendarContextRangeResolution(days)
    }

    private fun resolveChecked(request: CalendarContextRequest): ResolvedCalendarContext {
        val calendar = JewishCalendar(request.civilServiceDate).apply {
            setInIsrael(request.calendarRegion == CalendarRegion.ISRAEL)
            setUseModernHolidays(false)
            when (request.purimLocality) {
                PurimLocality.WALLED_CITY -> setIsMukafChoma(true)
                PurimLocality.NOT_WALLED_CITY -> setIsMukafChoma(false)
                PurimLocality.UNVERIFIED -> Unit
            }
        }
        val holidayIndex = calendar.yomTovIndex.takeUnless { it < 0 }
        val specialShabbat = calendar.specialShabbos.takeUnless { it == Parsha.NONE }
        val rawSignals = RawCalendarSignals(
            dayOfWeek = request.civilServiceDate.dayOfWeek,
            kosherJavaHolidayIndex = holidayIndex,
            kosherJavaSpecialShabbatName = specialShabbat?.name,
            isYomTov = calendar.isYomTov,
            isWorkRestrictedFestival = calendar.isYomTovAssurBemelacha,
            isFastDay = calendar.isTaanis,
            isFastOfFirstborn = calendar.isTaanisBechoros,
            isRoshHodesh = calendar.isRoshChodesh,
        )
        val classification = classify(rawSignals)
        val observances = resolveObservances(request, rawSignals)
        val unresolved = unresolvedValues(request, rawSignals, observances)
        val label = labelFor(classification.dayKind, observances)

        return ResolvedCalendarContext(
            request = request,
            jewishDate = ResolvedJewishDate(
                year = calendar.jewishYear,
                month = resolveMonth(calendar.jewishMonth, calendar.isJewishLeapYear),
                dayOfMonth = calendar.jewishDayOfMonth,
                isLeapYear = calendar.isJewishLeapYear,
            ),
            observances = observances,
            isShabbat = rawSignals.dayOfWeek == DayOfWeek.SATURDAY,
            isFastDay = rawSignals.isFastDay || rawSignals.isFastOfFirstborn,
            isRoshHodesh = rawSignals.isRoshHodesh,
            serviceDayKind = classification.dayKind,
            serviceCatalogOverride = CalendarDayOverride(
                date = request.civilServiceDate,
                dayKind = classification.dayKind,
                label = label,
                applicableRegions = setOf(request.calendarRegion),
                // Addition IDs belong to a reviewed ServiceCatalog. Calendar metadata remains
                // available above without fabricating a catalog-to-observance mapping here.
                additionalCalendarAdditionIds = emptySet(),
            ),
            details = CalendarResolutionDetails(
                engineName = ENGINE_NAME,
                engineVersion = ENGINE_VERSION,
                dateBasis = CalendarDateBasis.DAYTIME_OF_CIVIL_SERVICE_DATE,
                regionWasExplicitInput = true,
                modernObservancePolicy = ModernObservancePolicy.EXCLUDED_PENDING_REVIEW,
                purimLocality = request.purimLocality,
                classificationRule = classification.rule,
                rawSignals = rawSignals,
            ),
            unresolvedValues = unresolved,
        )
    }

    private fun classify(signals: RawCalendarSignals): Classification = when {
        signals.isWorkRestrictedFestival -> Classification(
            ServiceDayKind.FESTIVAL,
            ServiceDayClassificationRule.WORK_RESTRICTED_FESTIVAL_TAKES_PRECEDENCE,
        )

        signals.dayOfWeek == DayOfWeek.SATURDAY -> Classification(
            ServiceDayKind.SHABBAT,
            ServiceDayClassificationRule.SATURDAY_WHEN_NO_WORK_RESTRICTED_FESTIVAL,
        )

        else -> Classification(
            ServiceDayKind.WEEKDAY,
            ServiceDayClassificationRule.WEEKDAY_WHEN_NEITHER_RULE_APPLIES,
        )
    }

    private fun resolveObservances(
        request: CalendarContextRequest,
        signals: RawCalendarSignals,
    ): List<CalendarObservance> {
        val events = mutableListOf<CalendarObservance>()
        fun add(event: CalendarObservance) {
            // Known semantic identities appear once, with the first signal below defining
            // provenance precedence. Unknown library values remain separate by source so a
            // later signal cannot erase evidence that needs a mapping update.
            if (
                event.id == CalendarObservanceId.UNRECOGNIZED_LIBRARY_EVENT ||
                events.none { existing -> existing.id == event.id }
            ) {
                events += event
            }
        }
        signals.kosherJavaHolidayIndex?.let { index ->
            val id = HOLIDAY_INDEX_TO_ID[index] ?: CalendarObservanceId.UNRECOGNIZED_LIBRARY_EVENT
            if (
                request.purimLocality != PurimLocality.UNVERIFIED ||
                id !in LOCALITY_DEPENDENT_PURIM_IDS
            ) {
                add(
                    CalendarObservance(
                        id = id,
                        source = CalendarSignalSource.KOSHERJAVA_HOLIDAY_INDEX,
                        sourceValue = index.toString(),
                    ),
                )
            }
        }
        if (signals.isRoshHodesh) {
            add(
                CalendarObservance(
                    id = CalendarObservanceId.ROSH_HODESH,
                    source = CalendarSignalSource.KOSHERJAVA_ROSH_HODESH_FLAG,
                    sourceValue = "true",
                ),
            )
        }
        if (signals.isFastOfFirstborn) {
            add(
                CalendarObservance(
                    id = CalendarObservanceId.FAST_OF_FIRSTBORN,
                    source = CalendarSignalSource.KOSHERJAVA_FAST_OF_FIRSTBORN_FLAG,
                    sourceValue = "true",
                ),
            )
        }
        signals.kosherJavaSpecialShabbatName?.let { specialName ->
            val id = SPECIAL_SHABBAT_TO_ID[specialName]
                ?: CalendarObservanceId.UNRECOGNIZED_LIBRARY_EVENT
            add(
                CalendarObservance(
                    id = id,
                    source = CalendarSignalSource.KOSHERJAVA_SPECIAL_SHABBAT,
                    sourceValue = specialName,
                ),
            )
        }
        return events
    }

    private fun unresolvedValues(
        request: CalendarContextRequest,
        signals: RawCalendarSignals,
        observances: List<CalendarObservance>,
    ): List<UnresolvedCalendarValue> = buildList {
        add(
            unresolved(
                UnresolvedCalendarField.SUNSET_DAY_BOUNDARY,
                UnresolvedReason.OUTSIDE_MODULE_SCOPE,
                "The input is a civil service date interpreted for its daytime Jewish date; use the local zmanim engine to resolve an evening instant across sunset.",
            ),
        )
        add(
            unresolved(
                UnresolvedCalendarField.WEEKLY_TORAH_READING,
                UnresolvedReason.REQUIRES_REVIEWED_CONTENT_OR_POLICY,
                "Weekly readings are not calculated or named by this adapter.",
            ),
        )
        add(
            unresolved(
                UnresolvedCalendarField.FESTIVAL_TORAH_READING,
                UnresolvedReason.REQUIRES_REVIEWED_CONTENT_OR_POLICY,
                "Festival readings are not calculated or named by this adapter.",
            ),
        )
        add(
            unresolved(
                UnresolvedCalendarField.LITURGICAL_INSERTIONS,
                UnresolvedReason.REQUIRES_REVIEWED_CONTENT_OR_POLICY,
                "Calendar metadata does not establish liturgical wording or insertions.",
            ),
        )
        add(
            unresolved(
                UnresolvedCalendarField.SEASONAL_PRAYER_CHANGES,
                UnresolvedReason.REQUIRES_REVIEWED_CONTENT_OR_POLICY,
                "Seasonal prayer changes require a reviewed opinion profile and are not evaluated.",
            ),
        )
        add(
            unresolved(
                UnresolvedCalendarField.MODERN_OBSERVANCES,
                UnresolvedReason.REQUIRES_REVIEWED_CONTENT_OR_POLICY,
                "KosherJava modern holidays are disabled pending explicit product and editorial review.",
            ),
        )
        add(
            unresolved(
                UnresolvedCalendarField.USER_OR_COMMUNITY_EVENTS,
                UnresolvedReason.OUTSIDE_MODULE_SCOPE,
                "User and community events must be supplied by a separate local data source.",
            ),
        )
        add(
            unresolved(
                UnresolvedCalendarField.SERVICE_CATALOG_ADDITION_MAPPING,
                UnresolvedReason.REQUIRES_REVIEWED_CONTENT_OR_POLICY,
                "The bridge classifies the service day but does not invent ServiceCatalog calendar-addition IDs.",
            ),
        )
        val libraryHolidayId = signals.kosherJavaHolidayIndex?.let(HOLIDAY_INDEX_TO_ID::get)
        if (
            request.purimLocality == PurimLocality.UNVERIFIED &&
            libraryHolidayId != null &&
            libraryHolidayId in LOCALITY_DEPENDENT_PURIM_IDS
        ) {
            add(
                unresolved(
                    UnresolvedCalendarField.PURIM_LOCALITY,
                    UnresolvedReason.REQUIRES_EXPLICIT_LOCAL_INPUT,
                    "Israel/diaspora does not establish walled-city status; locality-dependent applicability remains unverified.",
                ),
            )
        }
        if (observances.any { it.id == CalendarObservanceId.UNRECOGNIZED_LIBRARY_EVENT }) {
            add(
                unresolved(
                    UnresolvedCalendarField.LIBRARY_EVENT_MAPPING,
                    UnresolvedReason.UNRECOGNIZED_PINNED_LIBRARY_VALUE,
                    "The pinned library returned a value this adapter does not map; no event identity was inferred.",
                ),
            )
        }
    }

    private fun labelFor(
        dayKind: ServiceDayKind,
        observances: List<CalendarObservance>,
    ): UserFacingText {
        val primary = observances.firstOrNull { event ->
            CalendarObservanceCategory.UNKNOWN !in event.id.categories
        }
        val fallback = when {
            primary == null && dayKind == ServiceDayKind.SHABBAT -> "Shabbat"
            primary == null -> "Weekday"
            dayKind == ServiceDayKind.SHABBAT &&
                CalendarObservanceCategory.SPECIAL_SHABBAT !in primary.id.categories ->
                "Shabbat · ${primary.id.fallbackEnglish}"

            else -> primary.id.fallbackEnglish
        }
        val keyPart = primary?.id?.stableId?.replace('-', '_')
            ?: dayKind.name.lowercase()
        return UserFacingText(
            resourceKey = "calendar_day_$keyPart",
            fallbackEnglish = fallback,
        )
    }

    private fun resolveMonth(month: Int, leapYear: Boolean): JewishMonth = when (month) {
        JewishDate.NISSAN -> JewishMonth.NISAN
        JewishDate.IYAR -> JewishMonth.IYAR
        JewishDate.SIVAN -> JewishMonth.SIVAN
        JewishDate.TAMMUZ -> JewishMonth.TAMMUZ
        JewishDate.AV -> JewishMonth.AV
        JewishDate.ELUL -> JewishMonth.ELUL
        JewishDate.TISHREI -> JewishMonth.TISHREI
        JewishDate.CHESHVAN -> JewishMonth.CHESHVAN
        JewishDate.KISLEV -> JewishMonth.KISLEV
        JewishDate.TEVES -> JewishMonth.TEVET
        JewishDate.SHEVAT -> JewishMonth.SHEVAT
        JewishDate.ADAR -> if (leapYear) JewishMonth.ADAR_I else JewishMonth.ADAR
        JewishDate.ADAR_II -> JewishMonth.ADAR_II
        else -> error("KosherJava returned an unrecognized Jewish month number.")
    }

    private fun calculationFailure(request: CalendarContextRequest) =
        CalendarContextResolution.Unavailable(
            request = request,
            reason = CalendarContextUnavailability.CALCULATION_FAILED,
            detail = "The pinned local calendar library could not resolve this date; no calendar classification was inferred.",
        )

    private fun unresolved(
        field: UnresolvedCalendarField,
        reason: UnresolvedReason,
        detail: String,
    ) = UnresolvedCalendarValue(field, reason, detail)

    private data class Classification(
        val dayKind: ServiceDayKind,
        val rule: ServiceDayClassificationRule,
    )

    companion object {
        const val ENGINE_NAME = "KosherJava JewishCalendar"
        const val ENGINE_VERSION = "2.5.0"

        private val HOLIDAY_INDEX_TO_ID = mapOf(
            JewishCalendar.EREV_PESACH to CalendarObservanceId.EREV_PESACH,
            JewishCalendar.PESACH to CalendarObservanceId.PESACH,
            JewishCalendar.CHOL_HAMOED_PESACH to CalendarObservanceId.HOL_HAMOED_PESACH,
            JewishCalendar.PESACH_SHENI to CalendarObservanceId.PESACH_SHENI,
            JewishCalendar.EREV_SHAVUOS to CalendarObservanceId.EREV_SHAVUOT,
            JewishCalendar.SHAVUOS to CalendarObservanceId.SHAVUOT,
            JewishCalendar.SEVENTEEN_OF_TAMMUZ to CalendarObservanceId.FAST_OF_SEVENTEENTH_OF_TAMMUZ,
            JewishCalendar.TISHA_BEAV to CalendarObservanceId.TISHA_BEAV,
            JewishCalendar.TU_BEAV to CalendarObservanceId.TU_BEAV,
            JewishCalendar.EREV_ROSH_HASHANA to CalendarObservanceId.EREV_ROSH_HASHANAH,
            JewishCalendar.ROSH_HASHANA to CalendarObservanceId.ROSH_HASHANAH,
            JewishCalendar.FAST_OF_GEDALYAH to CalendarObservanceId.FAST_OF_GEDALIAH,
            JewishCalendar.EREV_YOM_KIPPUR to CalendarObservanceId.EREV_YOM_KIPPUR,
            JewishCalendar.YOM_KIPPUR to CalendarObservanceId.YOM_KIPPUR,
            JewishCalendar.EREV_SUCCOS to CalendarObservanceId.EREV_SUKKOT,
            JewishCalendar.SUCCOS to CalendarObservanceId.SUKKOT,
            JewishCalendar.CHOL_HAMOED_SUCCOS to CalendarObservanceId.HOL_HAMOED_SUKKOT,
            JewishCalendar.HOSHANA_RABBA to CalendarObservanceId.HOSHANA_RABBAH,
            JewishCalendar.SHEMINI_ATZERES to CalendarObservanceId.SHEMINI_ATZERET,
            JewishCalendar.SIMCHAS_TORAH to CalendarObservanceId.SIMCHAT_TORAH,
            JewishCalendar.CHANUKAH to CalendarObservanceId.HANUKKAH,
            JewishCalendar.TENTH_OF_TEVES to CalendarObservanceId.FAST_OF_TENTH_OF_TEVET,
            JewishCalendar.TU_BESHVAT to CalendarObservanceId.TU_BISHVAT,
            JewishCalendar.FAST_OF_ESTHER to CalendarObservanceId.FAST_OF_ESTHER,
            JewishCalendar.PURIM to CalendarObservanceId.PURIM,
            JewishCalendar.SHUSHAN_PURIM to CalendarObservanceId.SHUSHAN_PURIM,
            JewishCalendar.PURIM_KATAN to CalendarObservanceId.PURIM_KATAN,
            JewishCalendar.ROSH_CHODESH to CalendarObservanceId.ROSH_HODESH,
            JewishCalendar.YOM_HASHOAH to CalendarObservanceId.YOM_HASHOAH,
            JewishCalendar.YOM_HAZIKARON to CalendarObservanceId.YOM_HAZIKARON,
            JewishCalendar.YOM_HAATZMAUT to CalendarObservanceId.YOM_HAATZMAUT,
            JewishCalendar.YOM_YERUSHALAYIM to CalendarObservanceId.YOM_YERUSHALAYIM,
            JewishCalendar.LAG_BAOMER to CalendarObservanceId.LAG_BAOMER,
            JewishCalendar.SHUSHAN_PURIM_KATAN to CalendarObservanceId.SHUSHAN_PURIM_KATAN,
            JewishCalendar.ISRU_CHAG to CalendarObservanceId.ISRU_HAG,
            JewishCalendar.YOM_KIPPUR_KATAN to CalendarObservanceId.YOM_KIPPUR_KATAN,
            JewishCalendar.BEHAB to CalendarObservanceId.BEHAB,
        )

        private val SPECIAL_SHABBAT_TO_ID = mapOf(
            Parsha.SHKALIM.name to CalendarObservanceId.SHABBAT_SHEKALIM,
            Parsha.ZACHOR.name to CalendarObservanceId.SHABBAT_ZACHOR,
            Parsha.PARA.name to CalendarObservanceId.SHABBAT_PARAH,
            Parsha.HACHODESH.name to CalendarObservanceId.SHABBAT_HAHODESH,
            Parsha.SHUVA.name to CalendarObservanceId.SHABBAT_SHUVA,
            Parsha.SHIRA.name to CalendarObservanceId.SHABBAT_SHIRAH,
            Parsha.HAGADOL.name to CalendarObservanceId.SHABBAT_HAGADOL,
            Parsha.CHAZON.name to CalendarObservanceId.SHABBAT_HAZON,
            Parsha.NACHAMU.name to CalendarObservanceId.SHABBAT_NAHAMU,
        )

        private val LOCALITY_DEPENDENT_PURIM_IDS = setOf(
            CalendarObservanceId.PURIM,
            CalendarObservanceId.SHUSHAN_PURIM,
            CalendarObservanceId.PURIM_KATAN,
            CalendarObservanceId.SHUSHAN_PURIM_KATAN,
        )
    }
}
