package io.github.gilnetizen.aseh.domain.servicecatalog

import java.time.Duration
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceCatalogModelsTest {
    @Test
    fun developmentCatalogCoversWeekdayShabbatAndFestivalWithoutConductableContent() {
        val catalog = SyntheticDevelopmentServiceCatalog.catalog

        assertEquals(CatalogMaturity.DEVELOPMENT_SYNTHETIC, catalog.maturity)
        assertEquals(
            ServiceDayKind.entries.toSet(),
            catalog.serviceDefinitions.flatMap(ServiceDefinition::supportedDayKinds).toSet(),
        )
        assertTrue(catalog.serviceDefinitions.size > ServiceDayKind.entries.size)
        assertTrue(catalog.serviceDefinitions.all { it.contentState is ServiceContentState.MetadataOnly })
        assertTrue(catalog.notice.fallbackEnglish.contains("no prayer or Torah text"))
        assertEquals(
            setOf(CalendarRegion.ISRAEL, CalendarRegion.DIASPORA),
            catalog.opinionProfiles.map(OpinionProfile::calendarRegion).toSet(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateServiceDefinitionIdsAreRejected() {
        val definition = serviceDefinition("service.duplicate")
        ServiceCatalog(
            id = "catalog.test",
            title = text("catalog", "Catalog"),
            maturity = CatalogMaturity.DEVELOPMENT_SYNTHETIC,
            notice = text("notice", "Synthetic test catalog."),
            opinionProfiles = listOf(profile()),
            serviceDefinitions = listOf(definition, definition),
            calendarAdditions = emptyList(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun todayPreparationCannotSilentlyOpenBeforeToday() {
        PreparationPolicy(
            kind = PreparationHorizonKind.TODAY,
            leadDays = 1,
            title = text("today", "Today"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun stableIdsRejectDisplayTextAndWhitespace() {
        serviceDefinition("Not a stable ID")
    }

    private fun serviceDefinition(id: String): ServiceDefinition = ServiceDefinition(
        id = id,
        title = text("service", "Service"),
        serviceKind = ServiceKind.MORNING,
        supportedDayKinds = setOf(ServiceDayKind.WEEKDAY),
        supportedSettings = setOf(ServiceSetting.INDIVIDUAL),
        scheduledLocalTime = LocalTime.of(8, 0),
        nominalDuration = Duration.ofMinutes(30),
        preparationPolicy = PreparationPolicy(
            kind = PreparationHorizonKind.TODAY,
            leadDays = 0,
            title = text("today", "Today"),
        ),
        contentState = ServiceContentState.MetadataOnly(text("missing", "No content.")),
    )

    private fun profile(): OpinionProfile = OpinionProfile(
        id = "profile.test",
        title = text("profile", "Profile"),
        calendarRegion = CalendarRegion.ISRAEL,
        calendarDateBasis = CalendarDateBasis.CIVIL_DATE_DEVELOPMENT_ONLY,
        dayBoundaryOpinionId = "opinion.boundary.test",
        zmanimOpinionId = "opinion.zmanim.test",
        serviceTimingOpinionId = "opinion.timing.test",
        maturity = CatalogMaturity.DEVELOPMENT_SYNTHETIC,
        notice = text("profile_notice", "Test profile."),
    )

    private fun text(key: String, fallback: String): UserFacingText = UserFacingText(key, fallback)
}
