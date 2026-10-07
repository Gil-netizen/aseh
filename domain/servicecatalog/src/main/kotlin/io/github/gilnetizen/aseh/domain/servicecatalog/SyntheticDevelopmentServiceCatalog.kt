package io.github.gilnetizen.aseh.domain.servicecatalog

import java.time.Duration
import java.time.LocalTime

/**
 * Non-sacred scheduling fixtures for development and UI integration.
 *
 * Titles identify service choices, but this catalog contains no prayers, readings, citations,
 * translations, liturgical ordering, or normative calendar rulings. Every definition is marked
 * planning-only so it cannot be mistaken for approved conductable content.
 */
object SyntheticDevelopmentServiceCatalog {
    const val ISRAEL_PROFILE_ID = "dev.profile.israel"
    const val DIASPORA_PROFILE_ID = "dev.profile.diaspora"

    val catalog: ServiceCatalog = ServiceCatalog(
        id = "dev.service-catalog.synthetic-v1",
        title = text("dev_service_catalog_title", "Development service schedule"),
        maturity = CatalogMaturity.DEVELOPMENT_SYNTHETIC,
        notice = text(
            "dev_service_catalog_notice",
            "Synthetic schedule metadata only. Times are illustrative and no prayer or Torah text, ruling, or approved calendar calculation is included.",
        ),
        opinionProfiles = listOf(
            developmentProfile(
                id = ISRAEL_PROFILE_ID,
                title = "Israel development calendar",
                region = CalendarRegion.ISRAEL,
            ),
            developmentProfile(
                id = DIASPORA_PROFILE_ID,
                title = "Diaspora development calendar",
                region = CalendarRegion.DIASPORA,
            ),
        ),
        serviceDefinitions = listOf(
            definition(
                id = "dev.service.weekday.morning",
                title = "Weekday morning service",
                kind = ServiceKind.MORNING,
                dayKind = ServiceDayKind.WEEKDAY,
                start = LocalTime.of(7, 0),
                durationMinutes = 45,
                preparation = PreparationPolicy(
                    kind = PreparationHorizonKind.TODAY,
                    leadDays = 0,
                    title = text("preparation_today", "Prepare today"),
                ),
            ),
            definition(
                id = "dev.service.weekday.afternoon",
                title = "Weekday afternoon service",
                kind = ServiceKind.AFTERNOON,
                dayKind = ServiceDayKind.WEEKDAY,
                start = LocalTime.of(13, 30),
                durationMinutes = 30,
                preparation = PreparationPolicy(
                    kind = PreparationHorizonKind.TODAY,
                    leadDays = 0,
                    title = text("preparation_today", "Prepare today"),
                ),
            ),
            definition(
                id = "dev.service.weekday.evening",
                title = "Weekday evening service",
                kind = ServiceKind.EVENING,
                dayKind = ServiceDayKind.WEEKDAY,
                start = LocalTime.of(19, 0),
                durationMinutes = 30,
                preparation = PreparationPolicy(
                    kind = PreparationHorizonKind.TODAY,
                    leadDays = 0,
                    title = text("preparation_today", "Prepare today"),
                ),
            ),
            definition(
                id = "dev.service.shabbat.morning",
                title = "Shabbat morning service",
                kind = ServiceKind.MORNING,
                dayKind = ServiceDayKind.SHABBAT,
                start = LocalTime.of(9, 0),
                durationMinutes = 150,
                preparation = PreparationPolicy(
                    kind = PreparationHorizonKind.BEFORE_SHABBAT,
                    leadDays = 2,
                    title = text("preparation_before_shabbat", "Prepare before Shabbat"),
                ),
            ),
            definition(
                id = "dev.service.shabbat.afternoon",
                title = "Shabbat afternoon service",
                kind = ServiceKind.AFTERNOON,
                dayKind = ServiceDayKind.SHABBAT,
                start = LocalTime.of(16, 30),
                durationMinutes = 45,
                preparation = PreparationPolicy(
                    kind = PreparationHorizonKind.BEFORE_SHABBAT,
                    leadDays = 2,
                    title = text("preparation_before_shabbat", "Prepare before Shabbat"),
                ),
            ),
            definition(
                id = "dev.service.festival.morning",
                title = "Festival morning service",
                kind = ServiceKind.MORNING,
                dayKind = ServiceDayKind.FESTIVAL,
                start = LocalTime.of(9, 0),
                durationMinutes = 150,
                preparation = PreparationPolicy(
                    kind = PreparationHorizonKind.BEFORE_FESTIVAL,
                    leadDays = 7,
                    title = text("preparation_before_festival", "Prepare before the festival"),
                ),
            ),
        ),
        calendarAdditions = listOf(
            CalendarAdditionDefinition(
                id = "dev.calendar-context.weekday",
                title = text("calendar_context_weekday_title", "Weekday context"),
                summary = text(
                    "calendar_context_weekday_summary",
                    "The synthetic calendar classified this civil date as a weekday.",
                ),
                kind = CalendarAdditionKind.DAY_CONTEXT,
                activation = CalendarAdditionActivation.AUTOMATIC_FOR_DAY_KIND,
                applicableDayKinds = setOf(ServiceDayKind.WEEKDAY),
            ),
            CalendarAdditionDefinition(
                id = "dev.calendar-context.shabbat",
                title = text("calendar_context_shabbat_title", "Shabbat context"),
                summary = text(
                    "calendar_context_shabbat_summary",
                    "The synthetic calendar classified this civil date as Shabbat.",
                ),
                kind = CalendarAdditionKind.DAY_CONTEXT,
                activation = CalendarAdditionActivation.AUTOMATIC_FOR_DAY_KIND,
                applicableDayKinds = setOf(ServiceDayKind.SHABBAT),
            ),
            CalendarAdditionDefinition(
                id = "dev.calendar-context.festival",
                title = text("calendar_context_festival_title", "Festival context"),
                summary = text(
                    "calendar_context_festival_summary",
                    "An explicit development calendar record classified this date as a festival.",
                ),
                kind = CalendarAdditionKind.DAY_CONTEXT,
                activation = CalendarAdditionActivation.AUTOMATIC_FOR_DAY_KIND,
                applicableDayKinds = setOf(ServiceDayKind.FESTIVAL),
            ),
        ),
    )

    private fun developmentProfile(
        id: String,
        title: String,
        region: CalendarRegion,
    ): OpinionProfile = OpinionProfile(
        id = id,
        title = text("${id.replace('.', '_')}_title", title),
        calendarRegion = region,
        calendarDateBasis = CalendarDateBasis.CIVIL_DATE_DEVELOPMENT_ONLY,
        dayBoundaryOpinionId = "dev.opinion.day-boundary.unresolved",
        zmanimOpinionId = "dev.opinion.zmanim.unresolved",
        serviceTimingOpinionId = "dev.opinion.service-time.illustrative",
        maturity = CatalogMaturity.DEVELOPMENT_SYNTHETIC,
        notice = text(
            "dev_opinion_profile_notice",
            "Development profile only. It does not establish a halakhic day boundary, zmanim method, or service time.",
        ),
    )

    private fun definition(
        id: String,
        title: String,
        kind: ServiceKind,
        dayKind: ServiceDayKind,
        start: LocalTime,
        durationMinutes: Long,
        preparation: PreparationPolicy,
    ): ServiceDefinition = ServiceDefinition(
        id = id,
        title = text("${id.replace('.', '_')}_title", title),
        serviceKind = kind,
        supportedDayKinds = setOf(dayKind),
        supportedSettings = ServiceSetting.entries.toSet(),
        scheduledLocalTime = start,
        nominalDuration = Duration.ofMinutes(durationMinutes),
        preparationPolicy = preparation,
        contentState = ServiceContentState.MetadataOnly(
            text(
                "dev_service_content_unavailable",
                "Reviewed service content is not installed; this development definition provides schedule and preparation metadata only.",
            ),
        ),
    )

    private fun text(key: String, fallback: String): UserFacingText = UserFacingText(key, fallback)
}
