package io.github.gilnetizen.aseh.domain.calendar

import io.github.gilnetizen.aseh.domain.servicecatalog.CalendarRegion
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceDayKind
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceScheduleSelector
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionRequest
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionResult
import io.github.gilnetizen.aseh.domain.servicecatalog.SyntheticDevelopmentServiceCatalog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class KosherJavaCalendarContextEngineTest {
    private val engine = KosherJavaCalendarContextEngine()

    @Test
    fun israelAndDiasporaAreExplicitAndDivergeOnSecondFestivalDay() {
        val date = LocalDate.parse("2024-04-24")
        val diaspora = resolve(date, CalendarRegion.DIASPORA)
        val israel = resolve(date, CalendarRegion.ISRAEL)

        assertEquals(ServiceDayKind.FESTIVAL, diaspora.serviceDayKind)
        assertEquals(listOf(CalendarObservanceId.PESACH), diaspora.observances.map { it.id })
        assertEquals(ServiceDayKind.WEEKDAY, israel.serviceDayKind)
        assertEquals(listOf(CalendarObservanceId.HOL_HAMOED_PESACH), israel.observances.map { it.id })
        assertNotEquals(diaspora.serviceCatalogOverride, israel.serviceCatalogOverride)
        assertEquals(setOf(CalendarRegion.DIASPORA), diaspora.serviceCatalogOverride.applicableRegions)
        assertEquals(setOf(CalendarRegion.ISRAEL), israel.serviceCatalogOverride.applicableRegions)
        assertTrue(diaspora.details.regionWasExplicitInput)
        assertTrue(israel.details.regionWasExplicitInput)
    }

    @Test
    fun directOverridesDriveServiceCatalogWithoutInventedAdditionIds() {
        val date = LocalDate.parse("2024-04-24")
        val selector = ServiceScheduleSelector(SyntheticDevelopmentServiceCatalog.catalog)
        val diaspora = resolve(date, CalendarRegion.DIASPORA)
        val israel = resolve(date, CalendarRegion.ISRAEL)

        assertTrue(diaspora.serviceCatalogOverride.additionalCalendarAdditionIds.isEmpty())
        assertTrue(israel.serviceCatalogOverride.additionalCalendarAdditionIds.isEmpty())

        val diasporaAgenda = selector.select(
            request(
                profileId = SyntheticDevelopmentServiceCatalog.DIASPORA_PROFILE_ID,
                override = diaspora.serviceCatalogOverride,
            ),
        ).agenda()
        val israelAgenda = selector.select(
            request(
                profileId = SyntheticDevelopmentServiceCatalog.ISRAEL_PROFILE_ID,
                override = israel.serviceCatalogOverride,
            ),
        ).agenda()

        assertEquals(setOf(ServiceDayKind.FESTIVAL), diasporaAgenda.todayServices.map { it.calendarDay.dayKind }.toSet())
        assertEquals(setOf(ServiceDayKind.WEEKDAY), israelAgenda.todayServices.map { it.calendarDay.dayKind }.toSet())
    }

    @Test
    fun leapMonthsRoshHodeshFastsAndSpecialShabbatRemainSeparateMetadata() {
        val adarOne = resolve(LocalDate.parse("2024-03-10"), CalendarRegion.DIASPORA)
        val adarTwo = resolve(LocalDate.parse("2024-03-11"), CalendarRegion.DIASPORA)
        val fast = resolve(LocalDate.parse("2024-03-21"), CalendarRegion.DIASPORA)
        val special = resolve(LocalDate.parse("2024-03-09"), CalendarRegion.DIASPORA)

        assertEquals(ResolvedJewishDate(5784, JewishMonth.ADAR_I, 30, true), adarOne.jewishDate)
        assertEquals(ResolvedJewishDate(5784, JewishMonth.ADAR_II, 1, true), adarTwo.jewishDate)
        assertEquals(listOf(CalendarObservanceId.ROSH_HODESH), adarOne.observances.map { it.id })
        assertEquals(listOf(CalendarObservanceId.ROSH_HODESH), adarTwo.observances.map { it.id })
        assertTrue(fast.isFastDay)
        assertEquals(listOf(CalendarObservanceId.FAST_OF_ESTHER), fast.observances.map { it.id })
        assertEquals(ServiceDayKind.WEEKDAY, fast.serviceDayKind)
        assertEquals(listOf(CalendarObservanceId.SHABBAT_SHEKALIM), special.observances.map { it.id })
        assertEquals(ServiceDayKind.SHABBAT, special.serviceDayKind)
    }

    @Test
    fun workRestrictedFestivalPrecedesShabbatForTheCatalogClassification() {
        val yomKippur = resolve(LocalDate.parse("2024-10-12"), CalendarRegion.DIASPORA)

        assertTrue(yomKippur.isShabbat)
        assertTrue(yomKippur.isFastDay)
        assertEquals(ServiceDayKind.FESTIVAL, yomKippur.serviceDayKind)
        assertEquals(
            ServiceDayClassificationRule.WORK_RESTRICTED_FESTIVAL_TAKES_PRECEDENCE,
            yomKippur.details.classificationRule,
        )
        assertEquals(listOf(CalendarObservanceId.YOM_KIPPUR), yomKippur.observances.map { it.id })
    }

    @Test
    fun holHamoedOnSaturdayRemainsShabbatWithFestivalMetadata() {
        val context = resolve(LocalDate.parse("2024-04-27"), CalendarRegion.ISRAEL)

        assertTrue(context.isShabbat)
        assertFalse(context.details.rawSignals.isWorkRestrictedFestival)
        assertEquals(ServiceDayKind.SHABBAT, context.serviceDayKind)
        assertEquals(listOf(CalendarObservanceId.HOL_HAMOED_PESACH), context.observances.map { it.id })
        assertEquals(
            ServiceDayClassificationRule.SATURDAY_WHEN_NO_WORK_RESTRICTED_FESTIVAL,
            context.details.classificationRule,
        )
    }

    @Test
    fun unsupportedCalendarSurfacesStayExplicitlyUnresolved() {
        val context = resolve(LocalDate.parse("2024-05-14"), CalendarRegion.ISRAEL)
        val unresolved = context.unresolvedValues.associateBy(UnresolvedCalendarValue::field)

        assertEquals(ModernObservancePolicy.EXCLUDED_PENDING_REVIEW, context.details.modernObservancePolicy)
        assertTrue(UnresolvedCalendarField.SUNSET_DAY_BOUNDARY in unresolved)
        assertTrue(UnresolvedCalendarField.WEEKLY_TORAH_READING in unresolved)
        assertTrue(UnresolvedCalendarField.FESTIVAL_TORAH_READING in unresolved)
        assertTrue(UnresolvedCalendarField.LITURGICAL_INSERTIONS in unresolved)
        assertTrue(UnresolvedCalendarField.SEASONAL_PRAYER_CHANGES in unresolved)
        assertTrue(UnresolvedCalendarField.MODERN_OBSERVANCES in unresolved)
        assertTrue(UnresolvedCalendarField.USER_OR_COMMUNITY_EVENTS in unresolved)
        assertTrue(UnresolvedCalendarField.SERVICE_CATALOG_ADDITION_MAPPING in unresolved)
        assertFalse(context.observances.any { CalendarObservanceCategory.MODERN_OBSERVANCE in it.id.categories })
    }

    @Test
    fun purimLocalityIsUnknownUntilExplicitlySupplied() {
        val date = LocalDate.parse("2024-03-24")
        val unknown = resolve(date, CalendarRegion.ISRAEL)
        val supplied = resolve(date, CalendarRegion.ISRAEL, PurimLocality.NOT_WALLED_CITY)

        assertTrue(unknown.unresolvedValues.any { it.field == UnresolvedCalendarField.PURIM_LOCALITY })
        assertFalse(unknown.observances.any { it.id in localityDependentPurimIds })
        assertEquals("Weekday", unknown.serviceCatalogOverride.label.fallbackEnglish)
        assertFalse(supplied.unresolvedValues.any { it.field == UnresolvedCalendarField.PURIM_LOCALITY })
        assertEquals(listOf(CalendarObservanceId.PURIM), supplied.observances.map { it.id })
        assertEquals(PurimLocality.UNVERIFIED, unknown.details.purimLocality)
        assertEquals(PurimLocality.NOT_WALLED_CITY, supplied.details.purimLocality)

        val shushanDate = LocalDate.parse("2024-03-25")
        val unknownShushan = resolve(shushanDate, CalendarRegion.ISRAEL)
        val suppliedShushan = resolve(
            shushanDate,
            CalendarRegion.ISRAEL,
            PurimLocality.WALLED_CITY,
        )
        assertTrue(unknownShushan.unresolvedValues.any { it.field == UnresolvedCalendarField.PURIM_LOCALITY })
        assertFalse(unknownShushan.observances.any { it.id in localityDependentPurimIds })
        assertEquals(listOf(CalendarObservanceId.SHUSHAN_PURIM), suppliedShushan.observances.map { it.id })
    }

    @Test
    fun partialRangeCannotBeConvertedIntoFallbackProneOverrides() {
        val date = LocalDate.parse("2024-04-24")
        val request = CalendarContextRequest(date, CalendarRegion.DIASPORA)
        val range = CalendarContextRangeResolution(
            days = listOf(
                engine.resolve(request),
                CalendarContextResolution.Unavailable(
                    request = request.copy(civilServiceDate = date.plusDays(1)),
                    reason = CalendarContextUnavailability.CALCULATION_FAILED,
                    detail = "Synthetic failure",
                ),
            ),
        )

        try {
            range.serviceCatalogOverrides
            fail("Expected a partial range to reject override extraction.")
        } catch (_: IllegalArgumentException) {
            Unit
        }
    }

    @Test
    fun rangeResolutionIsInclusiveOrderedAndDeterministic() {
        val request = CalendarContextRangeRequest(
            startDate = LocalDate.parse("2024-04-23"),
            endDateInclusive = LocalDate.parse("2024-04-25"),
            calendarRegion = CalendarRegion.ISRAEL,
        )

        val first = engine.resolveRange(request)
        val second = engine.resolveRange(request)

        assertEquals(first, second)
        assertEquals(
            listOf("2024-04-23", "2024-04-24", "2024-04-25"),
            first.resolvedContexts.map { it.request.civilServiceDate.toString() },
        )
        assertEquals(3, first.serviceCatalogOverrides.size)
        assertTrue(first.unavailableDays.isEmpty())
    }

    @Test
    fun rangeRejectsMoreThanServiceCatalogMaximumHorizon() {
        val start = LocalDate.parse("2024-01-01")
        CalendarContextRangeRequest(
            startDate = start,
            endDateInclusive = start.plusDays(366),
            calendarRegion = CalendarRegion.DIASPORA,
        )

        try {
            CalendarContextRangeRequest(
                startDate = start,
                endDateInclusive = start.plusDays(367),
                calendarRegion = CalendarRegion.DIASPORA,
            )
            fail("Expected an oversized range to be rejected.")
        } catch (_: IllegalArgumentException) {
            Unit
        }
    }

    private fun resolve(
        date: LocalDate,
        region: CalendarRegion,
        locality: PurimLocality = PurimLocality.UNVERIFIED,
    ): ResolvedCalendarContext {
        val result = engine.resolve(CalendarContextRequest(date, region, locality))
        check(result is CalendarContextResolution.Resolved) { "Expected resolved context, got $result" }
        return result.context
    }

    private fun request(
        profileId: String,
        override: io.github.gilnetizen.aseh.domain.servicecatalog.CalendarDayOverride,
    ) = ServiceSelectionRequest(
        now = Instant.parse("2024-04-24T06:00:00Z"),
        zoneId = ZoneId.of("UTC"),
        opinionProfileId = profileId,
        calendarOverrides = listOf(override),
        lookAheadDays = 1,
    )

    private fun ServiceSelectionResult.agenda() =
        (this as ServiceSelectionResult.Available).agenda

    private val localityDependentPurimIds = setOf(
        CalendarObservanceId.PURIM,
        CalendarObservanceId.SHUSHAN_PURIM,
        CalendarObservanceId.PURIM_KATAN,
        CalendarObservanceId.SHUSHAN_PURIM_KATAN,
    )
}
