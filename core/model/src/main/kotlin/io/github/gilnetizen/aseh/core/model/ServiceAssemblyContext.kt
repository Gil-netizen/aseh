package io.github.gilnetizen.aseh.core.model

import java.time.Instant
import java.time.LocalDate

const val SHABBAT_READING_ASSIGNMENT_COUNT = 8

enum class CalendarRegion(val label: String) {
    ISRAEL("Israel"),
    DIASPORA("Diaspora"),
    UNSPECIFIED("Not selected"),
}

enum class ServiceDayKind(val label: String) {
    WEEKDAY("Weekday"),
    SHABBAT("Shabbat"),
    FESTIVAL("Festival"),
    FAST("Fast"),
    OTHER("Other"),
    UNKNOWN("Not established"),
}

data class ServiceDateContext(
    val civilDate: LocalDate? = null,
    val displayLabel: String = "",
    val dayKind: ServiceDayKind = ServiceDayKind.UNKNOWN,
    val hebrewDate: ServiceHebrewDateContext = ServiceHebrewDateContext.Unavailable(),
) {
    val isAvailable: Boolean
        get() = civilDate != null || displayLabel.isNotBlank()

    val effectiveLabel: String
        get() = displayLabel.ifBlank { civilDate?.toString().orEmpty() }
}

sealed interface ServiceHebrewDateContext {
    data class Available(
        val transliteratedLabel: String,
        val hebrewLabel: String,
        val boundaryLabel: String,
    ) : ServiceHebrewDateContext {
        init {
            require(transliteratedLabel.isNotBlank()) {
                "An available Hebrew date requires a transliterated label."
            }
            require(hebrewLabel.isNotBlank()) {
                "An available Hebrew date requires a Hebrew label."
            }
            require(boundaryLabel.isNotBlank()) {
                "An available Hebrew date requires its evaluated day-boundary label."
            }
        }
    }

    data class Unavailable(
        val reason: String = "Not calculated for this service context.",
    ) : ServiceHebrewDateContext {
        init {
            require(reason.isNotBlank()) { "An unavailable Hebrew date requires a reason." }
        }
    }
}

data class ServiceCoordinates(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double? = null,
    val horizontalAccuracyMeters: Double? = null,
) {
    init {
        require(latitudeDegrees.isFinite() && latitudeDegrees in -90.0..90.0) {
            "Latitude must be finite and between -90 and 90 degrees."
        }
        require(longitudeDegrees.isFinite() && longitudeDegrees in -180.0..180.0) {
            "Longitude must be finite and between -180 and 180 degrees."
        }
        require(elevationMeters == null || elevationMeters.isFinite()) {
            "Elevation must be null or finite."
        }
        require(
            horizontalAccuracyMeters == null ||
                horizontalAccuracyMeters.isFinite() && horizontalAccuracyMeters >= 0.0,
        ) {
            "Horizontal accuracy must be null or a finite value of zero or greater."
        }
    }
}

sealed interface ServicePlaceAvailability {
    data class Available(
        val label: String,
        val timeZoneId: String? = null,
        val coordinates: ServiceCoordinates? = null,
    ) : ServicePlaceAvailability {
        init {
            require(label.isNotBlank()) { "An available place requires a display label." }
        }
    }

    data class Unavailable(
        val reason: String = "No place has been selected.",
    ) : ServicePlaceAvailability
}

enum class ServiceSolarEventKind(val label: String) {
    SUNRISE("Sunrise"),
    SOLAR_NOON_CHATZOT("Solar noon / chatzot"),
    SUNSET("Sunset"),
}

sealed interface ServiceSolarEvent {
    val kind: ServiceSolarEventKind

    data class Available(
        override val kind: ServiceSolarEventKind,
        val instant: Instant,
    ) : ServiceSolarEvent

    data class Unavailable(
        override val kind: ServiceSolarEventKind,
        val reason: String,
    ) : ServiceSolarEvent {
        init {
            require(reason.isNotBlank()) { "An unavailable solar event requires a reason." }
        }
    }
}

data class ServiceZmanimContext(
    val date: LocalDate? = null,
    val timeZoneId: String? = null,
    val events: List<ServiceSolarEvent> = ServiceSolarEventKind.entries.map { kind ->
        ServiceSolarEvent.Unavailable(kind, "Not calculated for this service context.")
    },
    val methodLabel: String = "Not calculated.",
) {
    init {
        require(methodLabel.isNotBlank()) { "A zmanim context requires a method or status label." }
        require(events.map(ServiceSolarEvent::kind).distinct().size == events.size) {
            "A zmanim context cannot contain duplicate event kinds."
        }
    }
}

enum class ServiceCommunalSetting(val label: String) {
    INDIVIDUAL("Individual workspace"),
    HOUSEHOLD("Household workspace"),
    QAHAL_WORKSPACE("Qahal workspace"),
    UNSPECIFIED("Not selected"),
}

enum class ServiceQuorumStatus(val label: String) {
    CONFIRMED_PRESENT("Confirmed present"),
    CONFIRMED_NOT_PRESENT("Confirmed not present"),
    NOT_REQUIRED("Recorded as not required for this service"),
    NOT_RECORDED("Not recorded"),
}

data class ServiceQuorumContext(
    val status: ServiceQuorumStatus = ServiceQuorumStatus.NOT_RECORDED,
    val countedParticipants: Int? = null,
    val requiredParticipants: Int? = null,
    val note: String = "",
) {
    init {
        require(countedParticipants == null || countedParticipants >= 0) {
            "Counted participants cannot be negative."
        }
        require(requiredParticipants == null || requiredParticipants > 0) {
            "A recorded required participant count must be positive."
        }
    }
}

data class ServicePracticeProfileContext(
    val profileId: String? = null,
    val label: String = "Not selected",
    val adopted: Boolean = false,
    val adoptionScope: String = "",
) {
    init {
        require(label.isNotBlank()) { "A practice-profile context requires a visible label." }
        require(!adopted || !profileId.isNullOrBlank()) {
            "An adopted practice profile requires a stable profile ID."
        }
        require(!adopted || adoptionScope.isNotBlank()) {
            "An adopted practice profile requires its local adoption scope."
        }
    }
}

data class ServiceSeasonalContext(
    val seasonLabel: String = "Not established",
    val additions: List<String> = emptyList(),
    val established: Boolean = false,
) {
    init {
        require(seasonLabel.isNotBlank()) { "A seasonal context requires a visible label." }
        require(established || additions.isEmpty()) {
            "Seasonal additions cannot be applied before the seasonal context is established."
        }
        require(additions.none(String::isBlank)) { "Seasonal additions cannot be blank." }
    }
}

data class ServicePreparationProgress(
    val requiredStepIds: List<String>,
    val completedStepIds: Set<String>,
) {
    val distinctRequiredStepIds: List<String>
        get() = requiredStepIds.distinct()

    val completedCount: Int
        get() = distinctRequiredStepIds.count(completedStepIds::contains)

    val totalCount: Int
        get() = distinctRequiredStepIds.size

    val incompleteStepIds: List<String>
        get() = distinctRequiredStepIds.filterNot(completedStepIds::contains)

    val isComplete: Boolean
        get() = incompleteStepIds.isEmpty()
}

data class ServicePreflightProgress(
    val requiredStepIds: List<String>,
    val completedStepIds: Set<String>,
) {
    val distinctRequiredStepIds: List<String>
        get() = requiredStepIds.distinct()

    val completedCount: Int
        get() = distinctRequiredStepIds.count(completedStepIds::contains)

    val totalCount: Int
        get() = distinctRequiredStepIds.size

    val incompleteStepIds: List<String>
        get() = distinctRequiredStepIds.filterNot(completedStepIds::contains)

    val isComplete: Boolean
        get() = incompleteStepIds.isEmpty()
}

data class RequiredRoleAssignments(
    val requiredRoles: Set<ParticipantRole>,
    val assignments: Map<ParticipantRole, String>,
) {
    val missingRoles: List<ParticipantRole>
        get() = requiredRoles
            .filter { role -> assignments[role].isNullOrBlank() }
            .sortedBy(ParticipantRole::ordinal)
}

data class ServiceReadingAssignment(
    val slotId: String,
    val sequence: Int,
    val label: String,
    val kind: ReadingSlotKind,
    val assignee: String?,
    val portionTitle: String = "",
    val locator: String = "",
    val passageRange: String = "",
    val sourceId: String? = null,
    val backupAssignee: String = "",
    val preparationStatus: ReadingPreparationStatus = ReadingPreparationStatus.NOT_STARTED,
    val manualOverride: Boolean = false,
    val overrideReason: String = "",
    val passageAvailability: ReadingPassageAvailability = ReadingPassageAvailability.UNAVAILABLE,
    val passageUnavailableReason: String = "No Torah passage is installed for this reading slot.",
) {
    val isAssigned: Boolean
        get() = !assignee.isNullOrBlank()
}

data class ServiceReadingAssignments(
    val assignments: List<ServiceReadingAssignment>,
    val expectedCount: Int = SHABBAT_READING_ASSIGNMENT_COUNT,
) {
    val orderedAssignments: List<ServiceReadingAssignment>
        get() = assignments.sortedWith(compareBy(ServiceReadingAssignment::sequence, ServiceReadingAssignment::slotId))

    val unassigned: List<ServiceReadingAssignment>
        get() = orderedAssignments.filterNot(ServiceReadingAssignment::isAssigned)

    val duplicateSlotIds: Set<String>
        get() = assignments
            .groupingBy(ServiceReadingAssignment::slotId)
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys

    val duplicateSequences: Set<Int>
        get() = assignments
            .groupingBy(ServiceReadingAssignment::sequence)
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
}

data class ServiceAccessibilityProfile(
    val participantNeedsReviewed: Boolean = false,
    val useMovementAlternatives: Boolean = false,
    val useVisualVoiceCues: Boolean = false,
    val useLargeText: Boolean = false,
    val useHighContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val keepScreenAwake: Boolean = false,
    val lowLightMode: Boolean = false,
) {
    val enabledSupportLabels: List<String>
        get() = buildList {
            if (useMovementAlternatives) add("movement alternatives")
            if (useVisualVoiceCues) add("visual voice cues")
            if (useLargeText) add("large text")
            if (useHighContrast) add("high contrast")
            if (reduceMotion) add("reduced motion")
            if (keepScreenAwake) add("keep screen awake")
            if (lowLightMode) add("low-light mode")
        }

    val summaryLabel: String
        get() = enabledSupportLabels.joinToString().ifBlank {
            if (participantNeedsReviewed) {
                "Participant needs reviewed; standard cues selected"
            } else {
                "Standard cues; participant needs review not recorded"
            }
        }
}

sealed interface RehearsalReadinessOverride {
    object None : RehearsalReadinessOverride

    data class Proceed(
        val reason: String,
    ) : RehearsalReadinessOverride {
        init {
            require(reason.isNotBlank()) { "A rehearsal override requires a visible reason." }
        }
    }
}

data class ServiceAssemblyContext(
    val date: ServiceDateContext,
    val calendarRegion: CalendarRegion,
    val place: ServicePlaceAvailability,
    val preparation: ServicePreparationProgress,
    val preflight: ServicePreflightProgress,
    val roles: RequiredRoleAssignments,
    val readings: ServiceReadingAssignments,
    val deviceUseMode: DeviceUseMode,
    val accessibility: ServiceAccessibilityProfile,
    val zmanim: ServiceZmanimContext = ServiceZmanimContext(),
    val communalSetting: ServiceCommunalSetting = ServiceCommunalSetting.UNSPECIFIED,
    val quorum: ServiceQuorumContext = ServiceQuorumContext(),
    val practiceProfile: ServicePracticeProfileContext = ServicePracticeProfileContext(),
    val seasonal: ServiceSeasonalContext = ServiceSeasonalContext(),
    val rehearsalOverride: RehearsalReadinessOverride = RehearsalReadinessOverride.None,
)

enum class ServiceReadinessSeverity {
    BLOCKER,
    WARNING,
}

data class ServiceReadinessIssue(
    val id: String,
    val severity: ServiceReadinessSeverity,
    val title: String,
    val detail: String,
)

enum class ServiceReadinessStatus {
    READY,
    BLOCKED,
    OVERRIDDEN_FOR_REHEARSAL,
}

data class ServiceReadiness(
    val status: ServiceReadinessStatus,
    val issues: List<ServiceReadinessIssue>,
    val overrideReason: String?,
) {
    val blockers: List<ServiceReadinessIssue>
        get() = issues.filter { issue -> issue.severity == ServiceReadinessSeverity.BLOCKER }

    val warnings: List<ServiceReadinessIssue>
        get() = issues.filter { issue -> issue.severity == ServiceReadinessSeverity.WARNING }

    val canBeginRehearsal: Boolean
        get() = status != ServiceReadinessStatus.BLOCKED
}

fun serviceAssemblyContext(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    date: ServiceDateContext,
    calendarRegion: CalendarRegion,
    place: ServicePlaceAvailability,
    accessibility: ServiceAccessibilityProfile = ServiceAccessibilityProfile(),
    zmanim: ServiceZmanimContext = ServiceZmanimContext(),
    communalSetting: ServiceCommunalSetting = when (state.workspaceKind) {
        WorkspaceKind.SELF -> ServiceCommunalSetting.INDIVIDUAL
        WorkspaceKind.HOUSEHOLD -> ServiceCommunalSetting.HOUSEHOLD
        WorkspaceKind.QAHAL -> ServiceCommunalSetting.QAHAL_WORKSPACE
    },
    quorum: ServiceQuorumContext = ServiceQuorumContext(),
    practiceProfile: ServicePracticeProfileContext = ServicePracticeProfileContext(),
    seasonal: ServiceSeasonalContext = ServiceSeasonalContext(),
    rehearsalOverride: RehearsalReadinessOverride = RehearsalReadinessOverride.None,
    requiredRoles: Set<ParticipantRole> = setOf(
        ParticipantRole.LEADER,
        ParticipantRole.READER,
        ParticipantRole.GABBAI,
    ),
): ServiceAssemblyContext = ServiceAssemblyContext(
    date = date,
    calendarRegion = calendarRegion,
    place = place,
    preparation = ServicePreparationProgress(
        requiredStepIds = catalog.practiceCards.flatMap(PracticeCard::steps).map(PracticeStep::id),
        completedStepIds = state.completedPracticeStepIds,
    ),
    preflight = ServicePreflightProgress(
        requiredStepIds = catalog.service.preflightSteps.map(PracticeStep::id),
        completedStepIds = state.completedPreflightStepIds,
    ),
    roles = RequiredRoleAssignments(
        requiredRoles = requiredRoles,
        assignments = state.roleAssignments,
    ),
    readings = ServiceReadingAssignments(
        assignments = catalog.service.readingSlots.map { slot ->
            val plan = state.readingPlanFor(slot)
            ServiceReadingAssignment(
                slotId = slot.id,
                sequence = slot.sequence,
                label = slot.label,
                kind = slot.kind,
                assignee = plan.assignee,
                portionTitle = plan.portionTitle,
                locator = plan.locator,
                passageRange = plan.passageRange,
                sourceId = slot.sourceId.takeUnless { plan.manualOverride },
                backupAssignee = plan.backupAssignee,
                preparationStatus = plan.preparationStatus,
                manualOverride = plan.manualOverride,
                overrideReason = plan.overrideReason,
                passageAvailability = if (
                    plan.manualOverride &&
                    plan.portionTitle.isNotBlank() &&
                    plan.locator.isNotBlank() &&
                    plan.passageRange.isNotBlank()
                ) {
                    ReadingPassageAvailability.AVAILABLE
                } else {
                    slot.passageAvailability
                },
                passageUnavailableReason = slot.passageUnavailableReason,
            )
        },
    ),
    deviceUseMode = state.deviceUseMode,
    accessibility = accessibility,
    zmanim = zmanim,
    communalSetting = communalSetting,
    quorum = quorum,
    practiceProfile = practiceProfile,
    seasonal = seasonal,
    rehearsalOverride = rehearsalOverride,
)

fun evaluateServiceReadiness(
    expectedDayKind: ServiceDayKind,
    context: ServiceAssemblyContext,
): ServiceReadiness {
    val issues = buildList {
        if (!context.date.isAvailable) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.date.unavailable",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Choose a service date",
                    detail = "The service date is required before the assembled order can be treated as current.",
                ),
            )
        }
        if (context.date.dayKind != expectedDayKind) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.date.day-kind",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Review the service day",
                    detail = "This service expects ${expectedDayKind.label}; the selected date is ${context.date.dayKind.label}.",
                ),
            )
        }
        if (context.calendarRegion == CalendarRegion.UNSPECIFIED) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.calendar-region.unselected",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Choose Israel or diaspora calendar",
                    detail = "Calendar region can change the service and must be explicit.",
                ),
            )
        }
        if (context.place is ServicePlaceAvailability.Unavailable) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.place.unavailable",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Set the service place",
                    detail = context.place.reason,
                ),
            )
        }
        if (!context.preparation.isComplete) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.preparation.incomplete",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Complete Shabbat preparation",
                    detail = "${context.preparation.incompleteStepIds.size} of " +
                        "${context.preparation.totalCount} required preparation steps remain.",
                ),
            )
        }
        if (!context.preflight.isComplete) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.preflight.incomplete",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Complete preflight",
                    detail = "${context.preflight.incompleteStepIds.size} of ${context.preflight.totalCount} required preflight steps remain.",
                ),
            )
        }
        context.roles.missingRoles.forEach { role ->
            add(
                ServiceReadinessIssue(
                    id = "readiness.role.${role.id}",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Assign ${role.label.lowercase()}",
                    detail = "${role.label} is required for this rehearsal and has no assignment.",
                ),
            )
        }
        if (context.readings.assignments.size != context.readings.expectedCount) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.readings.count",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Define all reading assignments",
                    detail = "Expected ${context.readings.expectedCount} reading slots; found ${context.readings.assignments.size}.",
                ),
            )
        }
        if (context.readings.duplicateSlotIds.isNotEmpty()) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.readings.duplicate-slots",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Resolve duplicate reading slots",
                    detail = "Duplicate slot IDs: ${context.readings.duplicateSlotIds.sorted().joinToString()}.",
                ),
            )
        }
        if (context.readings.duplicateSequences.isNotEmpty()) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.readings.duplicate-sequences",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Resolve duplicate reading order",
                    detail = "Duplicate sequence numbers: ${context.readings.duplicateSequences.sorted().joinToString()}.",
                ),
            )
        }
        if (context.readings.unassigned.isNotEmpty()) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.readings.unassigned",
                    severity = ServiceReadinessSeverity.BLOCKER,
                    title = "Assign every reading",
                    detail = context.readings.unassigned.joinToString(
                        prefix = "Open: ",
                        separator = ", ",
                        postfix = ".",
                        transform = ServiceReadingAssignment::label,
                    ),
                ),
            )
        }
        val readingsNotReady = context.readings.orderedAssignments.filter { assignment ->
            assignment.isAssigned && assignment.preparationStatus != ReadingPreparationStatus.READY
        }
        if (readingsNotReady.isNotEmpty()) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.readings.preparation",
                    severity = ServiceReadinessSeverity.WARNING,
                    title = "Confirm reader preparation",
                    detail = readingsNotReady.joinToString(
                        prefix = "Not marked ready: ",
                        separator = ", ",
                        postfix = ".",
                        transform = ServiceReadingAssignment::label,
                    ),
                ),
            )
        }
        when (context.deviceUseMode) {
            DeviceUseMode.DEVICE_PERMITTED -> Unit
            DeviceUseMode.PREPARED_DISPLAY_ONLY -> add(
                ServiceReadinessIssue(
                    id = "readiness.device.prepared-display",
                    severity = ServiceReadinessSeverity.WARNING,
                    title = "Prepare the display before the service",
                    detail = context.deviceUseMode.description,
                ),
            )
            DeviceUseMode.PRINT_ONLY -> add(
                ServiceReadinessIssue(
                    id = "readiness.device.print-only",
                    severity = ServiceReadinessSeverity.WARNING,
                    title = "Print the packet before the service",
                    detail = context.deviceUseMode.description,
                ),
            )
        }
        if (!context.accessibility.participantNeedsReviewed) {
            add(
                ServiceReadinessIssue(
                    id = "readiness.accessibility.unreviewed",
                    severity = ServiceReadinessSeverity.WARNING,
                    title = "Review participant access needs",
                    detail = "Confirm movement, voice, visual, and motion supports with participants.",
                ),
            )
        }
    }

    val blockers = issues.filter { issue -> issue.severity == ServiceReadinessSeverity.BLOCKER }
    val overrideReason = (context.rehearsalOverride as? RehearsalReadinessOverride.Proceed)?.reason
    val status = when {
        blockers.isEmpty() -> ServiceReadinessStatus.READY
        overrideReason != null -> ServiceReadinessStatus.OVERRIDDEN_FOR_REHEARSAL
        else -> ServiceReadinessStatus.BLOCKED
    }
    return ServiceReadiness(
        status = status,
        issues = issues,
        overrideReason = overrideReason,
    )
}

sealed interface ServiceAssemblyCondition {
    object Always : ServiceAssemblyCondition

    data class All(
        val conditions: List<ServiceAssemblyCondition>,
    ) : ServiceAssemblyCondition

    data class Any(
        val conditions: List<ServiceAssemblyCondition>,
    ) : ServiceAssemblyCondition

    data class DayKindIn(
        val values: Set<ServiceDayKind>,
    ) : ServiceAssemblyCondition

    data class CalendarRegionIn(
        val values: Set<CalendarRegion>,
    ) : ServiceAssemblyCondition

    data class PlaceAvailable(
        val expected: Boolean = true,
    ) : ServiceAssemblyCondition

    data class PreflightComplete(
        val expected: Boolean = true,
    ) : ServiceAssemblyCondition

    data class RoleAssigned(
        val role: ParticipantRole,
        val expected: Boolean = true,
    ) : ServiceAssemblyCondition

    data class ReadingsComplete(
        val expected: Boolean = true,
    ) : ServiceAssemblyCondition

    data class DeviceModeIn(
        val values: Set<DeviceUseMode>,
    ) : ServiceAssemblyCondition

    data class MovementAlternativesRequested(
        val expected: Boolean = true,
    ) : ServiceAssemblyCondition

    data class CommunalSettingIn(
        val values: Set<ServiceCommunalSetting>,
    ) : ServiceAssemblyCondition

    data class QuorumStatusIn(
        val values: Set<ServiceQuorumStatus>,
    ) : ServiceAssemblyCondition

    data class PracticeProfileSelected(
        val expected: Boolean = true,
    ) : ServiceAssemblyCondition

    data class SeasonalContextEstablished(
        val expected: Boolean = true,
    ) : ServiceAssemblyCondition
}

data class SegmentInclusionPolicy(
    val condition: ServiceAssemblyCondition,
    val includedReason: String,
    val omittedReason: String,
) {
    init {
        require(includedReason.isNotBlank()) { "An inclusion policy requires an included reason." }
        require(omittedReason.isNotBlank()) { "An inclusion policy requires an omitted reason." }
    }
}

internal data class ServiceConditionEvaluation(
    val matches: Boolean,
    val facts: List<String>,
)

internal fun ServiceAssemblyCondition.evaluate(
    context: ServiceAssemblyContext,
): ServiceConditionEvaluation = when (this) {
    ServiceAssemblyCondition.Always -> ServiceConditionEvaluation(
        matches = true,
        facts = listOf("Condition: always"),
    )
    is ServiceAssemblyCondition.All -> {
        val results = conditions.map { condition -> condition.evaluate(context) }
        ServiceConditionEvaluation(
            matches = results.all(ServiceConditionEvaluation::matches),
            facts = results.flatMap(ServiceConditionEvaluation::facts),
        )
    }
    is ServiceAssemblyCondition.Any -> {
        val results = conditions.map { condition -> condition.evaluate(context) }
        ServiceConditionEvaluation(
            matches = results.any(ServiceConditionEvaluation::matches),
            facts = results.flatMap(ServiceConditionEvaluation::facts),
        )
    }
    is ServiceAssemblyCondition.DayKindIn -> ServiceConditionEvaluation(
        matches = context.date.dayKind in values,
        facts = listOf(
            "Day kind: ${context.date.dayKind.name}; accepted: ${values.map(ServiceDayKind::name).sorted().joinToString()}",
        ),
    )
    is ServiceAssemblyCondition.CalendarRegionIn -> ServiceConditionEvaluation(
        matches = context.calendarRegion in values,
        facts = listOf(
            "Calendar region: ${context.calendarRegion.name}; accepted: ${values.map(CalendarRegion::name).sorted().joinToString()}",
        ),
    )
    is ServiceAssemblyCondition.PlaceAvailable -> {
        val actual = context.place is ServicePlaceAvailability.Available
        ServiceConditionEvaluation(
            matches = actual == expected,
            facts = listOf("Place available: $actual; expected: $expected"),
        )
    }
    is ServiceAssemblyCondition.PreflightComplete -> ServiceConditionEvaluation(
        matches = context.preflight.isComplete == expected,
        facts = listOf("Preflight complete: ${context.preflight.isComplete}; expected: $expected"),
    )
    is ServiceAssemblyCondition.RoleAssigned -> {
        val actual = !context.roles.assignments[role].isNullOrBlank()
        ServiceConditionEvaluation(
            matches = actual == expected,
            facts = listOf("${role.label} assigned: $actual; expected: $expected"),
        )
    }
    is ServiceAssemblyCondition.ReadingsComplete -> {
        val actual = context.readings.assignments.size == context.readings.expectedCount &&
            context.readings.unassigned.isEmpty() &&
            context.readings.duplicateSlotIds.isEmpty() &&
            context.readings.duplicateSequences.isEmpty()
        ServiceConditionEvaluation(
            matches = actual == expected,
            facts = listOf("Reading assignments complete: $actual; expected: $expected"),
        )
    }
    is ServiceAssemblyCondition.DeviceModeIn -> ServiceConditionEvaluation(
        matches = context.deviceUseMode in values,
        facts = listOf(
            "Device mode: ${context.deviceUseMode.name}; accepted: ${values.map(DeviceUseMode::name).sorted().joinToString()}",
        ),
    )
    is ServiceAssemblyCondition.MovementAlternativesRequested -> {
        val actual = context.accessibility.useMovementAlternatives
        ServiceConditionEvaluation(
            matches = actual == expected,
            facts = listOf("Movement alternatives requested: $actual; expected: $expected"),
        )
    }
    is ServiceAssemblyCondition.CommunalSettingIn -> ServiceConditionEvaluation(
        matches = context.communalSetting in values,
        facts = listOf(
            "Communal setting: ${context.communalSetting.name}; accepted: " +
                values.map(ServiceCommunalSetting::name).sorted().joinToString(),
        ),
    )
    is ServiceAssemblyCondition.QuorumStatusIn -> ServiceConditionEvaluation(
        matches = context.quorum.status in values,
        facts = listOf(
            "Quorum status: ${context.quorum.status.name}; accepted: " +
                values.map(ServiceQuorumStatus::name).sorted().joinToString(),
        ),
    )
    is ServiceAssemblyCondition.PracticeProfileSelected -> {
        val actual = !context.practiceProfile.profileId.isNullOrBlank()
        ServiceConditionEvaluation(
            matches = actual == expected,
            facts = listOf("Practice profile selected: $actual; expected: $expected"),
        )
    }
    is ServiceAssemblyCondition.SeasonalContextEstablished -> ServiceConditionEvaluation(
        matches = context.seasonal.established == expected,
        facts = listOf(
            "Seasonal context established: ${context.seasonal.established}; expected: $expected",
        ),
    )
}
