package io.github.gilnetizen.aseh.core.model

import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val TEACHING_SEGMENT_ID = "segment.teaching"
const val TEACHING_AFTER_READING_OPTION_ID = "choice.teaching.after-reading"
const val TEACHING_BEFORE_CLOSE_OPTION_ID = "choice.teaching.before-close"

private const val READING_SEGMENT_ID = "segment.reading"
private const val CLOSE_SEGMENT_ID = "segment.close"

data class ServiceContextFact(
    val label: String,
    val value: String,
)

data class ServiceSourceReference(
    val id: String,
    val locator: String?,
)

data class AssembledServiceSegment(
    val segment: ServiceSegment,
    val sourceReferences: List<ServiceSourceReference>,
    val inclusionReason: String = "Included by the installed service definition.",
    val inclusionFacts: List<String> = listOf("No conditional inclusion policy is attached."),
    val effectiveMovementCue: String = segment.movementCue,
    val movementCueReason: String = "The standard movement cue is active.",
)

enum class ServiceAssemblyResolution {
    DEFAULT_UNRESOLVED,
    LOCAL_OPTION_APPLIED,
    DEFAULT_UNRECOGNIZED_OPTION,
    DEFAULT_UNAVAILABLE,
}

data class ServiceOrderingDecision(
    val communityChoiceId: String,
    val selectedOptionId: String?,
    val selectedOptionTitle: String?,
    val resolution: ServiceAssemblyResolution,
    val conclusionStatus: ConclusionStatus,
    val reviewState: EditorialReviewState,
    val orderChanged: Boolean,
    val explanation: String,
    val facts: List<String>,
)

enum class ServiceCompositionAction {
    INCLUDE,
    OMIT,
    REORDER,
}

data class ServiceCompositionDecision(
    val id: String,
    val action: ServiceCompositionAction,
    val targetSegmentId: String,
    val applied: Boolean,
    val reason: String,
    val facts: List<String>,
    val sourceReferences: List<ServiceSourceReference>,
)

data class ServiceAssembly(
    val serviceId: String,
    val serviceTitle: String,
    val context: ServiceAssemblyContext,
    val contextFacts: List<ServiceContextFact>,
    val segments: List<AssembledServiceSegment>,
    val compositionDecisions: List<ServiceCompositionDecision>,
    val orderingDecision: ServiceOrderingDecision,
    val readiness: ServiceReadiness,
)

/**
 * Compatibility entry point for existing callers that only have display labels.
 *
 * New callers should use the typed [ServiceAssemblyContext] overload so calendar region,
 * place availability, accessibility needs, and any rehearsal override remain explicit.
 */
fun assembleService(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    dateLabel: String,
    locationLabel: String,
): ServiceAssembly = assembleService(
    catalog = catalog,
    state = state,
    context = serviceAssemblyContext(
        catalog = catalog,
        state = state,
        date = ServiceDateContext(
            displayLabel = dateLabel,
            dayKind = catalog.service.expectedDayKind,
        ),
        calendarRegion = CalendarRegion.UNSPECIFIED,
        place = if (locationLabel.isBlank()) {
            ServicePlaceAvailability.Unavailable()
        } else {
            ServicePlaceAvailability.Available(locationLabel)
        },
    ),
)

fun assembleService(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    context: ServiceAssemblyContext,
): ServiceAssembly {
    val sourceById = catalog.sources.associateBy(SourceUnit::id)
    val inclusionResults = catalog.service.segments.map { segment ->
        val policy = segment.inclusionPolicy
        val evaluation = policy?.condition?.evaluate(context)
            ?: ServiceConditionEvaluation(
                matches = true,
                facts = listOf("No conditional inclusion policy is attached."),
            )
        SegmentInclusionResult(
            segment = segment,
            included = evaluation.matches,
            reason = when {
                policy == null -> "Included by the installed service definition."
                evaluation.matches -> policy.includedReason
                else -> policy.omittedReason
            },
            facts = evaluation.facts,
        )
    }
    val includedSegments = inclusionResults
        .filter(SegmentInclusionResult::included)
        .map(SegmentInclusionResult::segment)

    val selectedOptionId = state.selectedCommunityOptionId
    val selectedOption = catalog.communityChoice.options.firstOrNull { option ->
        option.id == selectedOptionId
    }
    val placement = when (selectedOptionId) {
        TEACHING_AFTER_READING_OPTION_ID -> TeachingPlacement.AfterReading
        TEACHING_BEFORE_CLOSE_OPTION_ID -> TeachingPlacement.BeforeClose
        else -> null
    }

    val orderingResult = when {
        selectedOptionId == null -> OrderingResult(
            segments = includedSegments,
            resolution = ServiceAssemblyResolution.DEFAULT_UNRESOLVED,
            explanation = "No local option is selected. The service keeps the catalog's default segment order after context-based omissions.",
            placementAnchorId = null,
        )

        selectedOption == null || placement == null -> OrderingResult(
            segments = includedSegments,
            resolution = ServiceAssemblyResolution.DEFAULT_UNRECOGNIZED_OPTION,
            explanation = "The selected option is not available in this catalog. The service keeps the catalog's default segment order after context-based omissions and the choice remains unresolved.",
            placementAnchorId = null,
        )

        else -> applyTeachingPlacement(
            segments = includedSegments,
            placement = placement,
            selectedOptionTitle = selectedOption.title,
        )
    }

    val includedIds = includedSegments.map(ServiceSegment::id)
    val assembledIds = orderingResult.segments.map(ServiceSegment::id)
    val orderChanged = includedIds != assembledIds
    val sourceReferencesBySegmentId = catalog.service.segments.associate { segment ->
        segment.id to segment.sourceReferences(sourceById)
    }
    val inclusionResultBySegmentId = inclusionResults.associateBy { result -> result.segment.id }
    val orderingFacts = buildList {
        add("Community choice: ${catalog.communityChoice.id}")
        add("Choice conclusion evidence status: ${catalog.communityChoice.conclusionStatus.label}")
        add("Choice review state: ${catalog.communityChoice.reviewState.label}")
        add("Selected option: ${selectedOptionId ?: "none"}")
        add("Teaching segment: $TEACHING_SEGMENT_ID")
        orderingResult.placementAnchorId?.let { anchorId ->
            add("Placement anchor: $anchorId")
        }
        add("Included order: ${includedIds.joinToString(" -> ")}")
        add("Assembled order: ${assembledIds.joinToString(" -> ")}")
    }
    val orderingDecision = ServiceOrderingDecision(
        communityChoiceId = catalog.communityChoice.id,
        selectedOptionId = selectedOptionId,
        selectedOptionTitle = selectedOption?.title,
        resolution = orderingResult.resolution,
        conclusionStatus = catalog.communityChoice.conclusionStatus,
        reviewState = catalog.communityChoice.reviewState,
        orderChanged = orderChanged,
        explanation = orderingResult.explanation,
        facts = orderingFacts,
    )

    val compositionDecisions = inclusionResults.map { result ->
        ServiceCompositionDecision(
            id = "composition.${result.segment.id}.inclusion",
            action = if (result.included) {
                ServiceCompositionAction.INCLUDE
            } else {
                ServiceCompositionAction.OMIT
            },
            targetSegmentId = result.segment.id,
            applied = true,
            reason = result.reason,
            facts = result.facts,
            sourceReferences = sourceReferencesBySegmentId[result.segment.id].orEmpty(),
        )
    } + ServiceCompositionDecision(
        id = "composition.$TEACHING_SEGMENT_ID.order",
        action = ServiceCompositionAction.REORDER,
        targetSegmentId = TEACHING_SEGMENT_ID,
        applied = orderingResult.resolution == ServiceAssemblyResolution.LOCAL_OPTION_APPLIED,
        reason = orderingResult.explanation,
        facts = orderingFacts,
        sourceReferences = sourceReferencesBySegmentId[TEACHING_SEGMENT_ID].orEmpty(),
    )

    return ServiceAssembly(
        serviceId = catalog.service.id,
        serviceTitle = catalog.service.title,
        context = context,
        contextFacts = buildContextFacts(state, context),
        segments = orderingResult.segments.map { segment ->
            val useAlternative = context.accessibility.useMovementAlternatives
            val inclusionResult = checkNotNull(inclusionResultBySegmentId[segment.id]) {
                "Every assembled segment must retain its evaluated inclusion trace."
            }
            AssembledServiceSegment(
                segment = segment,
                sourceReferences = sourceReferencesBySegmentId[segment.id].orEmpty(),
                inclusionReason = inclusionResult.reason,
                inclusionFacts = inclusionResult.facts,
                effectiveMovementCue = if (useAlternative) {
                    segment.accessibleAlternative
                } else {
                    segment.movementCue
                },
                movementCueReason = if (useAlternative) {
                    "The accessibility profile requests movement alternatives."
                } else {
                    "The standard movement cue is active; the accessible alternative remains available."
                },
            )
        },
        compositionDecisions = compositionDecisions,
        orderingDecision = orderingDecision,
        readiness = evaluateServiceReadiness(
            expectedDayKind = catalog.service.expectedDayKind,
            context = context,
        ),
    )
}

private fun buildContextFacts(
    state: ExperienceState,
    context: ServiceAssemblyContext,
): List<ServiceContextFact> = buildList {
    add(
        ServiceContextFact(
            "Date",
            context.date.effectiveLabel.ifBlank { "Not selected" },
        ),
    )
    add(ServiceContextFact("Day kind", context.date.dayKind.label))
    add(
        ServiceContextFact(
            "Hebrew date",
            when (val hebrewDate = context.date.hebrewDate) {
                is ServiceHebrewDateContext.Available ->
                    "${hebrewDate.transliteratedLabel} / ${hebrewDate.hebrewLabel} " +
                        "(${hebrewDate.boundaryLabel})"
                is ServiceHebrewDateContext.Unavailable -> "Unavailable: ${hebrewDate.reason}"
            },
        ),
    )
    add(ServiceContextFact("Calendar region", context.calendarRegion.label))
    add(
        ServiceContextFact(
            "Place",
            when (val place = context.place) {
                is ServicePlaceAvailability.Available -> place.label
                is ServicePlaceAvailability.Unavailable -> "Unavailable: ${place.reason}"
            },
        ),
    )
    val availablePlace = context.place as? ServicePlaceAvailability.Available
    add(
        ServiceContextFact(
            "Time zone",
            availablePlace?.timeZoneId ?: context.zmanim.timeZoneId ?: "Not recorded",
        ),
    )
    val coordinates = availablePlace?.coordinates
    add(
        ServiceContextFact(
            "Coordinates",
            coordinates?.let {
                "${it.latitudeDegrees.toPlainString()}, ${it.longitudeDegrees.toPlainString()}"
            } ?: "Not recorded",
        ),
    )
    add(
        ServiceContextFact(
            "Elevation",
            coordinates?.elevationMeters?.let { meters -> "${meters.toPlainString()} m" }
                ?: "Not reported; calculation method may use its disclosed fallback",
        ),
    )
    add(
        ServiceContextFact(
            "Horizontal accuracy",
            coordinates?.horizontalAccuracyMeters?.let { meters ->
                "${meters.toPlainString()} m"
            } ?: "Not reported",
        ),
    )
    add(
        ServiceContextFact(
            "Zmanim date",
            context.zmanim.date?.toString() ?: "Not calculated",
        ),
    )
    ServiceSolarEventKind.entries.forEach { kind ->
        val event = context.zmanim.events.firstOrNull { candidate -> candidate.kind == kind }
        add(
            ServiceContextFact(
                kind.label,
                when (event) {
                    is ServiceSolarEvent.Available -> event.localDisplay(context.zmanim.timeZoneId)
                    is ServiceSolarEvent.Unavailable -> "Unavailable: ${event.reason}"
                    null -> "Unavailable: event was not evaluated"
                },
            ),
        )
    }
    add(ServiceContextFact("Zmanim method", context.zmanim.methodLabel))
    add(ServiceContextFact("Communal setting", context.communalSetting.label))
    add(
        ServiceContextFact(
            "Quorum",
            buildString {
                append(context.quorum.status.label)
                context.quorum.countedParticipants?.let { append("; counted: $it") }
                context.quorum.requiredParticipants?.let { append("; recorded requirement: $it") }
                context.quorum.note.takeIf(String::isNotBlank)?.let { append("; $it") }
            },
        ),
    )
    add(
        ServiceContextFact(
            "Practice profile",
            buildString {
                append(context.practiceProfile.label)
                append(if (context.practiceProfile.adopted) "; adopted locally" else "; not adopted")
                context.practiceProfile.adoptionScope.takeIf(String::isNotBlank)?.let {
                    append("; scope: $it")
                }
            },
        ),
    )
    add(
        ServiceContextFact(
            "Seasonal context",
            buildString {
                append(context.seasonal.seasonLabel)
                if (context.seasonal.established) append("; established")
                if (context.seasonal.additions.isNotEmpty()) {
                    append("; additions: ${context.seasonal.additions.joinToString()}")
                }
            },
        ),
    )
    add(
        ServiceContextFact(
            "Shabbat preparation",
            "${context.preparation.completedCount} of ${context.preparation.totalCount} complete",
        ),
    )
    add(
        ServiceContextFact(
            "Preflight",
            "${context.preflight.completedCount} of ${context.preflight.totalCount} complete",
        ),
    )
    add(
        ServiceContextFact(
            "Required roles",
            if (context.roles.missingRoles.isEmpty()) {
                "Assigned"
            } else {
                "Open: ${context.roles.missingRoles.joinToString { role -> role.label }}"
            },
        ),
    )
    add(
        ServiceContextFact(
            "Reading assignments",
            "${context.readings.assignments.count(ServiceReadingAssignment::isAssigned)} of " +
                "${context.readings.expectedCount} assigned",
        ),
    )
    add(ServiceContextFact("Device use", context.deviceUseMode.label))
    add(
        ServiceContextFact(
            "Accessibility",
            context.accessibility.summaryLabel,
        ),
    )
    add(ServiceContextFact("Workspace", state.workspaceName.ifBlank { "Not named" }))
    add(ServiceContextFact("Workspace type", state.workspaceKind.label))
    add(ServiceContextFact("View", state.selectedRole.label))
    add(
        ServiceContextFact(
            "Provisional rehearsal charter",
            if (state.communityCharter.adopted || state.provisionalCharterAdopted) {
                "Adopted"
            } else {
                "Not adopted"
            },
        ),
    )
    val override = context.rehearsalOverride as? RehearsalReadinessOverride.Proceed
    if (override != null) {
        add(ServiceContextFact("Rehearsal override", override.reason))
    }
}

private val serviceTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d, uuuu h:mm a z", Locale.ENGLISH)

private fun ServiceSolarEvent.Available.localDisplay(timeZoneId: String?): String {
    val zone = timeZoneId
        ?.let { candidate -> runCatching { ZoneId.of(candidate) }.getOrNull() }
        ?: ZoneId.of("UTC")
    return serviceTimeFormatter.format(instant.atZone(zone))
}

private fun Double.toPlainString(): String =
    BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

private fun ServiceSegment.sourceReferences(
    sourceById: Map<String, SourceUnit>,
): List<ServiceSourceReference> = explanation.sourceIds.distinct().map { sourceId ->
    ServiceSourceReference(
        id = sourceId,
        locator = sourceById[sourceId]?.locator,
    )
}

private fun applyTeachingPlacement(
    segments: List<ServiceSegment>,
    placement: TeachingPlacement,
    selectedOptionTitle: String,
): OrderingResult {
    val teaching = segments.singleOrNull { segment -> segment.id == TEACHING_SEGMENT_ID }
        ?: return OrderingResult(
            segments = segments,
            resolution = ServiceAssemblyResolution.DEFAULT_UNAVAILABLE,
            explanation = "The optional teaching segment is not included for this context. The service keeps the remaining segments in catalog order.",
            placementAnchorId = null,
        )
    val anchorId = when (placement) {
        TeachingPlacement.AfterReading -> READING_SEGMENT_ID
        TeachingPlacement.BeforeClose -> CLOSE_SEGMENT_ID
    }
    val withoutTeaching = segments.filterNot { segment -> segment.id == TEACHING_SEGMENT_ID }
    val anchorIndex = withoutTeaching.indexOfFirst { segment -> segment.id == anchorId }
    if (anchorIndex < 0) {
        return OrderingResult(
            segments = segments,
            resolution = ServiceAssemblyResolution.DEFAULT_UNAVAILABLE,
            explanation = "The placement anchor is not included for this context. The service keeps the remaining segments in catalog order.",
            placementAnchorId = anchorId,
        )
    }

    val insertionIndex = when (placement) {
        TeachingPlacement.AfterReading -> anchorIndex + 1
        TeachingPlacement.BeforeClose -> anchorIndex
    }
    val reordered = withoutTeaching.toMutableList().apply {
        add(insertionIndex, teaching)
    }
    val changed = reordered.map(ServiceSegment::id) != segments.map(ServiceSegment::id)
    val action = if (changed) "Moved" else "Kept"
    val relation = when (placement) {
        TeachingPlacement.AfterReading -> "immediately after the reading handoff"
        TeachingPlacement.BeforeClose -> "immediately before the closing review"
    }
    return OrderingResult(
        segments = reordered,
        resolution = ServiceAssemblyResolution.LOCAL_OPTION_APPLIED,
        explanation = "$action the optional teaching segment $relation because this workspace selected \"$selectedOptionTitle\". This is a local rehearsal choice.",
        placementAnchorId = anchorId,
    )
}

private enum class TeachingPlacement {
    AfterReading,
    BeforeClose,
}

private data class SegmentInclusionResult(
    val segment: ServiceSegment,
    val included: Boolean,
    val reason: String,
    val facts: List<String>,
)

private data class OrderingResult(
    val segments: List<ServiceSegment>,
    val resolution: ServiceAssemblyResolution,
    val explanation: String,
    val placementAnchorId: String?,
)
