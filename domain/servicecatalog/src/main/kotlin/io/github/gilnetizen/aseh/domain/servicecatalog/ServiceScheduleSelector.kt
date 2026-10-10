package io.github.gilnetizen.aseh.domain.servicecatalog

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class CalendarDayOverride(
    val date: LocalDate,
    val dayKind: ServiceDayKind,
    val label: UserFacingText,
    val applicableRegions: Set<CalendarRegion> = CalendarRegion.entries.toSet(),
    val additionalCalendarAdditionIds: Set<String> = emptySet(),
) {
    init {
        require(applicableRegions.isNotEmpty()) {
            "A calendar-day override must apply to at least one calendar region."
        }
        additionalCalendarAdditionIds.forEach { id -> requireStableId(id, "calendar addition") }
    }
}

data class ResolvedCalendarDay(
    val civilDate: LocalDate,
    val dayKind: ServiceDayKind,
    val label: UserFacingText,
    val resolutionReason: UserFacingText,
)

data class CalendarAddition(
    val id: String,
    val title: UserFacingText,
    val summary: UserFacingText,
    val kind: CalendarAdditionKind,
)

enum class PreparationHorizonState {
    UPCOMING,
    ACTIVE,
    CLOSED,
}

data class PreparationHorizon(
    val kind: PreparationHorizonKind,
    val title: UserFacingText,
    val opensAt: ZonedDateTime,
    /** The occurrence this horizon prepares for; this is not a halakhic deadline. */
    val serviceStartsAt: ZonedDateTime,
    val state: PreparationHorizonState,
)

enum class ServiceTemporalState {
    UPCOMING,
    IN_PROGRESS_WINDOW,
    PASSED,
}

enum class ServiceAvailabilityStatus {
    AVAILABLE,
    PLANNING_ONLY,
    BLOCKED,
}

enum class ServiceAvailabilityReasonCode {
    TIME_ZONE_REQUIRED,
    OPINION_PROFILE_REQUIRED,
    OPINION_PROFILE_UNKNOWN,
    AMBIGUOUS_CALENDAR_DAY,
    CALENDAR_ADDITION_UNKNOWN,
    CALENDAR_ADDITION_NOT_APPLICABLE,
    CONTENT_METADATA_ONLY,
    CONTENT_BUNDLE_MISSING,
    NO_SERVICE_IN_RANGE,
}

data class ServiceAvailabilityReason(
    val code: ServiceAvailabilityReasonCode,
    val title: UserFacingText,
    val detail: UserFacingText,
    val suggestedAction: UserFacingText? = null,
)

data class ServiceAvailability(
    val status: ServiceAvailabilityStatus,
    val reasons: List<ServiceAvailabilityReason> = emptyList(),
) {
    init {
        require(status == ServiceAvailabilityStatus.AVAILABLE || reasons.isNotEmpty()) {
            "A limited or blocked service requires a user-facing reason."
        }
        require(status != ServiceAvailabilityStatus.AVAILABLE || reasons.isEmpty()) {
            "An available service cannot also carry block reasons."
        }
    }

    val canConduct: Boolean
        get() = status == ServiceAvailabilityStatus.AVAILABLE
}

data class DatedServiceInstance(
    val id: String,
    val definition: ServiceDefinition,
    val serviceDate: LocalDate,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime,
    val calendarDay: ResolvedCalendarDay,
    val opinionProfile: OpinionProfile,
    val calendarAdditions: List<CalendarAddition>,
    val preparationHorizon: PreparationHorizon,
    val temporalState: ServiceTemporalState,
    val availability: ServiceAvailability,
)

data class ServiceAgenda(
    val generatedAt: Instant,
    val zoneId: ZoneId,
    val opinionProfile: OpinionProfile,
    val today: LocalDate,
    val todayServices: List<DatedServiceInstance>,
    /** Ordered current and future choices, capped at [MAX_UPCOMING_SERVICES]. */
    val upcomingServices: List<DatedServiceInstance>,
    val nextService: DatedServiceInstance?,
    val nextServiceUnavailableReason: ServiceAvailabilityReason?,
    val catalogNotice: UserFacingText,
)

const val MAX_UPCOMING_SERVICES = 32

sealed interface ServiceSelectionResult {
    data class Available(
        val agenda: ServiceAgenda,
    ) : ServiceSelectionResult

    data class Blocked(
        val reasons: List<ServiceAvailabilityReason>,
    ) : ServiceSelectionResult {
        init {
            require(reasons.isNotEmpty()) { "A blocked selection requires a reason." }
        }
    }
}

data class ServiceSelectionRequest(
    val now: Instant,
    val zoneId: ZoneId?,
    val opinionProfileId: String?,
    val calendarOverrides: List<CalendarDayOverride> = emptyList(),
    val installedContentBundleIds: Set<String> = emptySet(),
    val lookAheadDays: Int = DEFAULT_LOOK_AHEAD_DAYS,
) {
    init {
        require(lookAheadDays in 1..MAX_LOOK_AHEAD_DAYS) {
            "Service look-ahead must be between 1 and $MAX_LOOK_AHEAD_DAYS days."
        }
        installedContentBundleIds.forEach { id -> requireStableId(id, "content bundle") }
    }

    companion object {
        const val DEFAULT_LOOK_AHEAD_DAYS = 21
        const val MAX_LOOK_AHEAD_DAYS = 366
    }
}

/**
 * Selects dated occurrences using explicit time-zone, calendar, and opinion inputs.
 *
 * It intentionally does not calculate the Hebrew calendar or infer Israel/diaspora from a
 * location. The built-in Saturday/weekday classification is development scaffolding and each
 * festival must arrive as an explicit [CalendarDayOverride] until a reviewed calendar engine is
 * integrated.
 */
class ServiceScheduleSelector(
    private val catalog: ServiceCatalog,
) {
    fun select(request: ServiceSelectionRequest): ServiceSelectionResult {
        val contextReasons = buildList {
            if (request.zoneId == null) add(timeZoneRequiredReason())
            if (request.opinionProfileId.isNullOrBlank()) add(opinionProfileRequiredReason())
        }
        if (contextReasons.isNotEmpty()) return ServiceSelectionResult.Blocked(contextReasons)

        val zoneId = checkNotNull(request.zoneId)
        val profileId = checkNotNull(request.opinionProfileId)
        val profile = catalog.opinionProfiles.firstOrNull { candidate -> candidate.id == profileId }
            ?: return ServiceSelectionResult.Blocked(listOf(unknownOpinionProfileReason(profileId)))
        val today = request.now.atZone(zoneId).toLocalDate()

        val occurrences = mutableListOf<DatedServiceInstance>()
        for (offset in 0..request.lookAheadDays) {
            val date = today.plusDays(offset.toLong())
            when (val dayResult = resolveCalendarDay(date, profile, request.calendarOverrides)) {
                is CalendarDayResult.Blocked -> return ServiceSelectionResult.Blocked(dayResult.reasons)
                is CalendarDayResult.Resolved -> {
                    val additionsResult = resolveCalendarAdditions(dayResult.day, profile, dayResult.override)
                    if (additionsResult is CalendarAdditionsResult.Blocked) {
                        return ServiceSelectionResult.Blocked(additionsResult.reasons)
                    }
                    val additions = (additionsResult as CalendarAdditionsResult.Resolved).additions
                    occurrences += catalog.serviceDefinitions
                        .asSequence()
                        .filter { definition -> dayResult.day.dayKind in definition.supportedDayKinds }
                        .map { definition ->
                            instanceFor(
                                definition = definition,
                                date = date,
                                day = dayResult.day,
                                profile = profile,
                                zoneId = zoneId,
                                now = request.now,
                                additions = additions,
                                installedBundleIds = request.installedContentBundleIds,
                            )
                        }
                        .toList()
                }
            }
        }

        val ordered = occurrences.sortedWith(
            compareBy<DatedServiceInstance>(DatedServiceInstance::startsAt)
                .thenBy { instance -> instance.definition.id },
        )
        val todayServices = ordered.filter { instance -> instance.serviceDate == today }
        val upcoming = ordered
            .asSequence()
            .filter { instance -> instance.endsAt.toInstant() > request.now }
            .take(MAX_UPCOMING_SERVICES)
            .toList()
        val next = upcoming.firstOrNull()
        val noNextReason = if (next == null) noServiceInRangeReason(request.lookAheadDays) else null
        return ServiceSelectionResult.Available(
            ServiceAgenda(
                generatedAt = request.now,
                zoneId = zoneId,
                opinionProfile = profile,
                today = today,
                todayServices = todayServices,
                upcomingServices = upcoming,
                nextService = next,
                nextServiceUnavailableReason = noNextReason,
                catalogNotice = catalog.notice,
            ),
        )
    }

    private fun resolveCalendarDay(
        date: LocalDate,
        profile: OpinionProfile,
        overrides: List<CalendarDayOverride>,
    ): CalendarDayResult {
        val matches = overrides.filter { override ->
            override.date == date && profile.calendarRegion in override.applicableRegions
        }
        if (matches.size > 1) {
            return CalendarDayResult.Blocked(
                listOf(
                    reason(
                        ServiceAvailabilityReasonCode.AMBIGUOUS_CALENDAR_DAY,
                        "service_reason_calendar_ambiguous_title",
                        "Calendar needs review",
                        "service_reason_calendar_ambiguous_detail",
                        "More than one calendar classification applies to $date for ${profile.calendarRegion.name.lowercase()}.",
                        "service_reason_calendar_ambiguous_action",
                        "Correct the calendar context before choosing a service.",
                    ),
                ),
            )
        }
        val override = matches.singleOrNull()
        if (override != null) {
            return CalendarDayResult.Resolved(
                day = ResolvedCalendarDay(
                    civilDate = date,
                    dayKind = override.dayKind,
                    label = override.label,
                    resolutionReason = text(
                        "service_calendar_explicit_override",
                        "An explicit calendar-day record supplied this classification.",
                    ),
                ),
                override = override,
            )
        }

        val dayKind = if (date.dayOfWeek == DayOfWeek.SATURDAY) {
            ServiceDayKind.SHABBAT
        } else {
            ServiceDayKind.WEEKDAY
        }
        val label = when (dayKind) {
            ServiceDayKind.WEEKDAY -> text("service_day_weekday", "Weekday")
            ServiceDayKind.SHABBAT -> text("service_day_shabbat", "Shabbat")
            ServiceDayKind.FESTIVAL -> error("Festival days require an explicit calendar record.")
        }
        return CalendarDayResult.Resolved(
            day = ResolvedCalendarDay(
                civilDate = date,
                dayKind = dayKind,
                label = label,
                resolutionReason = text(
                    "service_calendar_development_civil_rule",
                    "Synthetic development calendar: Saturday is Shabbat and other days are weekdays.",
                ),
            ),
            override = null,
        )
    }

    private fun resolveCalendarAdditions(
        day: ResolvedCalendarDay,
        profile: OpinionProfile,
        override: CalendarDayOverride?,
    ): CalendarAdditionsResult {
        val additionById = catalog.calendarAdditions.associateBy(CalendarAdditionDefinition::id)
        val unknownIds = override?.additionalCalendarAdditionIds.orEmpty() - additionById.keys
        if (unknownIds.isNotEmpty()) {
            return CalendarAdditionsResult.Blocked(
                unknownIds.sorted().map { id ->
                    reason(
                        ServiceAvailabilityReasonCode.CALENDAR_ADDITION_UNKNOWN,
                        "service_reason_addition_unknown_title",
                        "Calendar addition unavailable",
                        "service_reason_addition_unknown_detail",
                        "The calendar refers to '$id', which is not in the active service catalog.",
                        "service_reason_addition_unknown_action",
                        "Review the calendar and installed catalog.",
                    )
                },
            )
        }
        val inapplicableExplicitIds = override?.additionalCalendarAdditionIds.orEmpty().filter { id ->
            val definition = checkNotNull(additionById[id])
            day.dayKind !in definition.applicableDayKinds ||
                profile.calendarRegion !in definition.applicableRegions
        }
        if (inapplicableExplicitIds.isNotEmpty()) {
            return CalendarAdditionsResult.Blocked(
                inapplicableExplicitIds.sorted().map { id ->
                    reason(
                        ServiceAvailabilityReasonCode.CALENDAR_ADDITION_NOT_APPLICABLE,
                        "service_reason_addition_inapplicable_title",
                        "Calendar addition does not apply",
                        "service_reason_addition_inapplicable_detail",
                        "The calendar addition '$id' does not apply to ${day.dayKind.name.lowercase()} in ${profile.calendarRegion.name.lowercase()}.",
                        "service_reason_addition_inapplicable_action",
                        "Correct the dated calendar record or choose the applicable profile.",
                    )
                },
            )
        }
        val applicable = catalog.calendarAdditions.filter { definition ->
            definition.activation == CalendarAdditionActivation.AUTOMATIC_FOR_DAY_KIND &&
                day.dayKind in definition.applicableDayKinds &&
                profile.calendarRegion in definition.applicableRegions
        }
        val selected = (applicable.map(CalendarAdditionDefinition::id) +
            override?.additionalCalendarAdditionIds.orEmpty())
            .distinct()
            .mapNotNull(additionById::get)
            .sortedBy(CalendarAdditionDefinition::id)
            .map { definition ->
                CalendarAddition(
                    id = definition.id,
                    title = definition.title,
                    summary = definition.summary,
                    kind = definition.kind,
                )
            }
        return CalendarAdditionsResult.Resolved(selected)
    }

    private fun instanceFor(
        definition: ServiceDefinition,
        date: LocalDate,
        day: ResolvedCalendarDay,
        profile: OpinionProfile,
        zoneId: ZoneId,
        now: Instant,
        additions: List<CalendarAddition>,
        installedBundleIds: Set<String>,
    ): DatedServiceInstance {
        val startsAt = date.atTime(definition.scheduledLocalTime).atZone(zoneId)
        val endsAt = startsAt.plus(definition.nominalDuration)
        val opensAt = date
            .minusDays(definition.preparationPolicy.leadDays.toLong())
            .atStartOfDay(zoneId)
        val horizonState = when {
            now < opensAt.toInstant() -> PreparationHorizonState.UPCOMING
            now < startsAt.toInstant() -> PreparationHorizonState.ACTIVE
            else -> PreparationHorizonState.CLOSED
        }
        val temporalState = when {
            now < startsAt.toInstant() -> ServiceTemporalState.UPCOMING
            now < endsAt.toInstant() -> ServiceTemporalState.IN_PROGRESS_WINDOW
            else -> ServiceTemporalState.PASSED
        }
        return DatedServiceInstance(
            id = "${definition.id}@$date#${profile.id}",
            definition = definition,
            serviceDate = date,
            startsAt = startsAt,
            endsAt = endsAt,
            calendarDay = day,
            opinionProfile = profile,
            calendarAdditions = additions,
            preparationHorizon = PreparationHorizon(
                kind = definition.preparationPolicy.kind,
                title = definition.preparationPolicy.title,
                opensAt = opensAt,
                serviceStartsAt = startsAt,
                state = horizonState,
            ),
            temporalState = temporalState,
            availability = availabilityFor(definition.contentState, installedBundleIds),
        )
    }

    private fun availabilityFor(
        contentState: ServiceContentState,
        installedBundleIds: Set<String>,
    ): ServiceAvailability = when (contentState) {
        ServiceContentState.Ready -> ServiceAvailability(ServiceAvailabilityStatus.AVAILABLE)
        is ServiceContentState.MetadataOnly -> ServiceAvailability(
            status = ServiceAvailabilityStatus.PLANNING_ONLY,
            reasons = listOf(
                ServiceAvailabilityReason(
                    code = ServiceAvailabilityReasonCode.CONTENT_METADATA_ONLY,
                    title = text("service_reason_metadata_only_title", "Planning available"),
                    detail = contentState.reason,
                    suggestedAction = text(
                        "service_reason_metadata_only_action",
                        "Use the schedule and preparation tools; conducting remains unavailable.",
                    ),
                ),
            ),
        )
        is ServiceContentState.RequiresBundles -> {
            val missing = contentState.bundleIds - installedBundleIds
            if (missing.isEmpty()) {
                ServiceAvailability(ServiceAvailabilityStatus.AVAILABLE)
            } else {
                ServiceAvailability(
                    status = ServiceAvailabilityStatus.BLOCKED,
                    reasons = listOf(
                        ServiceAvailabilityReason(
                            code = ServiceAvailabilityReasonCode.CONTENT_BUNDLE_MISSING,
                            title = text("service_reason_bundle_missing_title", "Service content unavailable"),
                            detail = text(
                                "service_reason_bundle_missing_detail",
                                "Missing required content: ${missing.sorted().joinToString()}.",
                            ),
                            suggestedAction = contentState.reason,
                        ),
                    ),
                )
            }
        }
    }

    private sealed interface CalendarDayResult {
        data class Resolved(
            val day: ResolvedCalendarDay,
            val override: CalendarDayOverride?,
        ) : CalendarDayResult

        data class Blocked(val reasons: List<ServiceAvailabilityReason>) : CalendarDayResult
    }

    private sealed interface CalendarAdditionsResult {
        data class Resolved(val additions: List<CalendarAddition>) : CalendarAdditionsResult
        data class Blocked(val reasons: List<ServiceAvailabilityReason>) : CalendarAdditionsResult
    }
}

private fun timeZoneRequiredReason(): ServiceAvailabilityReason = reason(
    ServiceAvailabilityReasonCode.TIME_ZONE_REQUIRED,
    "service_reason_timezone_title",
    "Time zone needed",
    "service_reason_timezone_detail",
    "ASEH needs a confirmed time zone to date and order local services.",
    "service_reason_timezone_action",
    "Confirm the device time zone or choose one manually.",
)

private fun opinionProfileRequiredReason(): ServiceAvailabilityReason = reason(
    ServiceAvailabilityReasonCode.OPINION_PROFILE_REQUIRED,
    "service_reason_profile_required_title",
    "Calendar profile needed",
    "service_reason_profile_required_detail",
    "Choose an explicit opinion profile before ASEH selects services.",
    "service_reason_profile_required_action",
    "Choose an Israel or diaspora development profile.",
)

private fun unknownOpinionProfileReason(profileId: String): ServiceAvailabilityReason = reason(
    ServiceAvailabilityReasonCode.OPINION_PROFILE_UNKNOWN,
    "service_reason_profile_unknown_title",
    "Calendar profile unavailable",
    "service_reason_profile_unknown_detail",
    "The selected profile '$profileId' is not in the active service catalog.",
    "service_reason_profile_unknown_action",
    "Choose an installed profile.",
)

private fun noServiceInRangeReason(days: Int): ServiceAvailabilityReason = reason(
    ServiceAvailabilityReasonCode.NO_SERVICE_IN_RANGE,
    "service_reason_no_service_title",
    "No next service found",
    "service_reason_no_service_detail",
    "No service definition applies within the next $days days.",
    "service_reason_no_service_action",
    "Review the calendar profile or install additional service definitions.",
)

private fun reason(
    code: ServiceAvailabilityReasonCode,
    titleKey: String,
    title: String,
    detailKey: String,
    detail: String,
    actionKey: String,
    action: String,
): ServiceAvailabilityReason = ServiceAvailabilityReason(
    code = code,
    title = text(titleKey, title),
    detail = text(detailKey, detail),
    suggestedAction = text(actionKey, action),
)

private fun text(key: String, fallback: String): UserFacingText = UserFacingText(key, fallback)
