package io.github.gilnetizen.aseh.domain.servicecatalog

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceScheduleSelectorTest {
    private val catalog = SyntheticDevelopmentServiceCatalog.catalog
    private val selector = ServiceScheduleSelector(catalog)
    private val jerusalem = ZoneId.of("Asia/Jerusalem")

    @Test
    fun weekdayAgendaListsAllChoicesAndSelectsTheMorningOccurrence() {
        val result = selector.select(
            requestAt("2026-10-05T03:30:00Z"), // Monday, 06:30 in Jerusalem.
        ).availableAgenda()

        assertEquals(LocalDate.parse("2026-10-05"), result.today)
        assertEquals(
            listOf(ServiceKind.MORNING, ServiceKind.AFTERNOON, ServiceKind.EVENING),
            result.todayServices.map { instance -> instance.definition.serviceKind },
        )
        assertEquals("dev.service.weekday.morning", result.nextService?.definition?.id)
        assertEquals("2026-10-05", result.nextService?.serviceDate.toString())
        assertEquals(result.nextService, result.upcomingServices.first())
        assertEquals(MAX_UPCOMING_SERVICES, result.upcomingServices.size)
        assertEquals(
            result.upcomingServices.map(DatedServiceInstance::startsAt).sorted(),
            result.upcomingServices.map(DatedServiceInstance::startsAt),
        )
        assertTrue(result.upcomingServices.any { it.calendarDay.dayKind == ServiceDayKind.SHABBAT })
        assertTrue(result.todayServices.all { it.availability.status == ServiceAvailabilityStatus.PLANNING_ONLY })
        assertTrue(
            result.todayServices.all { instance ->
                instance.calendarAdditions.map(CalendarAddition::id) ==
                    listOf("dev.calendar-context.weekday")
            },
        )
    }

    @Test
    fun selectorMovesToTheNextDatedServiceAfterTodaysChoicesPass() {
        val result = selector.select(
            requestAt("2026-10-05T18:00:00Z"), // Monday, 21:00 in Jerusalem.
        ).availableAgenda()

        assertEquals(3, result.todayServices.size)
        assertTrue(result.todayServices.all { it.temporalState == ServiceTemporalState.PASSED })
        assertEquals(LocalDate.parse("2026-10-06"), result.nextService?.serviceDate)
        assertEquals("dev.service.weekday.morning", result.nextService?.definition?.id)
    }

    @Test
    fun shabbatInstancesCarryAnAdvancePreparationHorizon() {
        val result = selector.select(
            requestAt("2026-10-09T17:00:00Z"), // Friday evening, next occurrence is Saturday.
        ).availableAgenda()
        val next = requireNotNull(result.nextService)

        assertEquals(ServiceDayKind.SHABBAT, next.calendarDay.dayKind)
        assertEquals(PreparationHorizonKind.BEFORE_SHABBAT, next.preparationHorizon.kind)
        assertEquals(LocalDate.parse("2026-10-08"), next.preparationHorizon.opensAt.toLocalDate())
        assertEquals(PreparationHorizonState.ACTIVE, next.preparationHorizon.state)
        assertEquals(ServiceAvailabilityStatus.PLANNING_ONLY, next.availability.status)
        assertTrue(
            ServiceOperationalCapability.SHABBAT_MORNING_REHEARSAL in
                next.definition.operationalCapabilities,
        )
        assertEquals(listOf("dev.calendar-context.shabbat"), next.calendarAdditions.map(CalendarAddition::id))
    }

    @Test
    fun explicitFestivalOverrideIsRegionSpecificAndCarriesFestivalPreparation() {
        val festivalDate = LocalDate.parse("2026-10-07")
        val override = CalendarDayOverride(
            date = festivalDate,
            dayKind = ServiceDayKind.FESTIVAL,
            label = text("test_festival", "Synthetic festival"),
            applicableRegions = setOf(CalendarRegion.DIASPORA),
        )
        val diaspora = selector.select(
            requestAt(
                instant = "2026-10-07T03:00:00Z",
                profileId = SyntheticDevelopmentServiceCatalog.DIASPORA_PROFILE_ID,
                overrides = listOf(override),
            ),
        ).availableAgenda()
        val israel = selector.select(
            requestAt(
                instant = "2026-10-07T03:00:00Z",
                profileId = SyntheticDevelopmentServiceCatalog.ISRAEL_PROFILE_ID,
                overrides = listOf(override),
            ),
        ).availableAgenda()

        assertEquals(listOf(ServiceDayKind.FESTIVAL), diaspora.todayServices.map { it.calendarDay.dayKind }.distinct())
        assertEquals(1, diaspora.todayServices.size)
        assertEquals(PreparationHorizonKind.BEFORE_FESTIVAL, diaspora.todayServices.single().preparationHorizon.kind)
        assertEquals(LocalDate.parse("2026-09-30"), diaspora.todayServices.single().preparationHorizon.opensAt.toLocalDate())
        assertEquals(
            listOf("dev.calendar-context.festival"),
            diaspora.todayServices.single().calendarAdditions.map(CalendarAddition::id),
        )
        assertEquals(3, israel.todayServices.size)
        assertTrue(israel.todayServices.all { it.calendarDay.dayKind == ServiceDayKind.WEEKDAY })
    }

    @Test
    fun missingContextReturnsSpecificUserFacingBlockReasons() {
        val result = selector.select(
            ServiceSelectionRequest(
                now = Instant.parse("2026-10-05T03:30:00Z"),
                zoneId = null,
                opinionProfileId = null,
            ),
        ) as ServiceSelectionResult.Blocked

        assertEquals(
            setOf(
                ServiceAvailabilityReasonCode.TIME_ZONE_REQUIRED,
                ServiceAvailabilityReasonCode.OPINION_PROFILE_REQUIRED,
            ),
            result.reasons.map(ServiceAvailabilityReason::code).toSet(),
        )
        assertTrue(result.reasons.all { it.title.fallbackEnglish.isNotBlank() })
        assertTrue(result.reasons.all { it.detail.fallbackEnglish.isNotBlank() })
        assertTrue(result.reasons.all { it.suggestedAction?.fallbackEnglish?.isNotBlank() == true })
    }

    @Test
    fun unknownProfileAndAmbiguousCalendarFailClosed() {
        val unknown = selector.select(
            requestAt("2026-10-05T03:30:00Z", profileId = "profile.not-installed"),
        ) as ServiceSelectionResult.Blocked
        assertEquals(
            listOf(ServiceAvailabilityReasonCode.OPINION_PROFILE_UNKNOWN),
            unknown.reasons.map(ServiceAvailabilityReason::code),
        )

        val date = LocalDate.parse("2026-10-07")
        val ambiguous = selector.select(
            requestAt(
                instant = "2026-10-07T03:30:00Z",
                overrides = listOf(
                    CalendarDayOverride(date, ServiceDayKind.FESTIVAL, text("first", "First classification")),
                    CalendarDayOverride(date, ServiceDayKind.SHABBAT, text("second", "Second classification")),
                ),
            ),
        ) as ServiceSelectionResult.Blocked
        assertEquals(
            listOf(ServiceAvailabilityReasonCode.AMBIGUOUS_CALENDAR_DAY),
            ambiguous.reasons.map(ServiceAvailabilityReason::code),
        )
    }

    @Test
    fun selectorIsDeterministicForTheSameExplicitInputs() {
        val request = requestAt("2026-10-05T03:30:00Z")

        assertEquals(selector.select(request), selector.select(request))
    }

    @Test
    fun requiredBundlesProduceABlockReasonUntilEveryBundleIsInstalled() {
        val restrictedDefinition = catalog.serviceDefinitions.first().copy(
            contentState = ServiceContentState.RequiresBundles(
                bundleIds = setOf("bundle.schedule", "bundle.service-content"),
                reason = text("install_reviewed_content", "Install the reviewed service content."),
            ),
        )
        val restrictedCatalog = catalog.copy(
            id = "dev.service-catalog.restricted-test",
            serviceDefinitions = listOf(restrictedDefinition),
        )
        val restrictedSelector = ServiceScheduleSelector(restrictedCatalog)
        val blocked = restrictedSelector.select(
            requestAt("2026-10-05T03:30:00Z", bundles = setOf("bundle.schedule")),
        ).availableAgenda().todayServices.single()
        val ready = restrictedSelector.select(
            requestAt(
                "2026-10-05T03:30:00Z",
                bundles = setOf("bundle.schedule", "bundle.service-content"),
            ),
        ).availableAgenda().todayServices.single()

        assertEquals(ServiceAvailabilityStatus.BLOCKED, blocked.availability.status)
        assertFalse(blocked.availability.canConduct)
        assertEquals(
            ServiceAvailabilityReasonCode.CONTENT_BUNDLE_MISSING,
            blocked.availability.reasons.single().code,
        )
        assertEquals(ServiceAvailabilityStatus.AVAILABLE, ready.availability.status)
        assertTrue(ready.availability.canConduct)
        assertTrue(ready.availability.reasons.isEmpty())
    }

    @Test
    fun explicitCalendarAdditionAppearsOnlyOnItsDatedCalendarRecord() {
        val specialAddition = CalendarAdditionDefinition(
            id = "dev.calendar-addition.special",
            title = text("special_title", "Special event context"),
            summary = text("special_summary", "Synthetic special-event metadata."),
            kind = CalendarAdditionKind.SPECIAL_EVENT,
            activation = CalendarAdditionActivation.EXPLICIT_CALENDAR_RECORD,
            applicableDayKinds = setOf(ServiceDayKind.WEEKDAY),
            applicableRegions = setOf(CalendarRegion.ISRAEL),
        )
        val selectorWithSpecial = ServiceScheduleSelector(
            catalog.copy(
                id = "dev.service-catalog.special-addition-test",
                calendarAdditions = catalog.calendarAdditions + specialAddition,
            ),
        )
        val date = LocalDate.parse("2026-10-05")
        val withoutRecord = selectorWithSpecial.select(
            requestAt("2026-10-05T03:30:00Z"),
        ).availableAgenda()
        val withRecord = selectorWithSpecial.select(
            requestAt(
                instant = "2026-10-05T03:30:00Z",
                overrides = listOf(
                    CalendarDayOverride(
                        date = date,
                        dayKind = ServiceDayKind.WEEKDAY,
                        label = text("weekday_special", "Weekday with a special event"),
                        applicableRegions = setOf(CalendarRegion.ISRAEL),
                        additionalCalendarAdditionIds = setOf(specialAddition.id),
                    ),
                ),
            ),
        ).availableAgenda()

        assertFalse(
            withoutRecord.todayServices.single { it.definition.serviceKind == ServiceKind.MORNING }
                .calendarAdditions.any { it.id == specialAddition.id },
        )
        assertTrue(
            withRecord.todayServices.single { it.definition.serviceKind == ServiceKind.MORNING }
                .calendarAdditions.any { it.id == specialAddition.id },
        )
    }

    @Test
    fun noApplicableDefinitionReturnsAVisibleNextServiceReason() {
        val festivalOnly = catalog.copy(
            id = "dev.service-catalog.festival-only-test",
            serviceDefinitions = catalog.serviceDefinitions.filter { definition ->
                ServiceDayKind.FESTIVAL in definition.supportedDayKinds
            },
        )
        val result = ServiceScheduleSelector(festivalOnly).select(
            requestAt("2026-10-05T03:30:00Z"),
        ).availableAgenda()

        assertTrue(result.todayServices.isEmpty())
        assertNull(result.nextService)
        assertNotNull(result.nextServiceUnavailableReason)
        assertEquals(
            ServiceAvailabilityReasonCode.NO_SERVICE_IN_RANGE,
            result.nextServiceUnavailableReason?.code,
        )
    }

    private fun requestAt(
        instant: String,
        profileId: String = SyntheticDevelopmentServiceCatalog.ISRAEL_PROFILE_ID,
        overrides: List<CalendarDayOverride> = emptyList(),
        bundles: Set<String> = emptySet(),
    ): ServiceSelectionRequest = ServiceSelectionRequest(
        now = Instant.parse(instant),
        zoneId = jerusalem,
        opinionProfileId = profileId,
        calendarOverrides = overrides,
        installedContentBundleIds = bundles,
    )

    private fun ServiceSelectionResult.availableAgenda(): ServiceAgenda =
        (this as ServiceSelectionResult.Available).agenda

    private fun text(key: String, fallback: String): UserFacingText = UserFacingText(key, fallback)
}
