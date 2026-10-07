package io.github.gilnetizen.aseh.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ConclusionStatus(val label: String) {
    SOURCE_EXPLICIT("Source explicit"),
    AUTHORITY_EXPLICIT("Authority explicit"),
    STRONG_SYNTHESIS("Strong synthesis"),
    PLAUSIBLE_SYNTHESIS("Plausible synthesis"),
    ANALOGICAL_APPLICATION("Analogical application"),
    EDITORIAL_PROPOSAL("Editorial proposal"),
    COMMUNITY_ENACTMENT("Community enactment"),
    PERSONAL_DISCIPLINE("Personal discipline"),
    DISPUTED("Disputed"),
    UNRESOLVED("Unresolved"),
}

enum class EditorialReviewState(val label: String) {
    PROPOSED("Proposed"),
    RESEARCHED("Researched"),
    DRAFTED("Drafted"),
    SOURCE_VERIFIED("Source verified"),
    HUMAN_REVIEWED("Human reviewed"),
    APPROVED("Approved"),
    PUBLISHED("Published"),
    UNDER_REVIEW("Under review"),
}

enum class ParticipantRole(val id: String, val label: String) {
    CONGREGANT("congregant", "Congregant"),
    LEADER("leader", "Prayer leader"),
    READER("reader", "Reader"),
    GABBAI("gabbai", "Gabbai"),
    HOST("host", "Host"),
}

enum class WorkspaceKind(val id: String, val label: String) {
    SELF("self", "Self"),
    HOUSEHOLD("household", "Household"),
    QAHAL("qahal", "Qahal"),
}

enum class DeviceUseMode(
    val id: String,
    val label: String,
    val description: String,
) {
    DEVICE_PERMITTED(
        id = "device_permitted",
        label = "Device permitted",
        description = "A device may be used during the rehearsal; keep the prepared packet available as a fallback.",
    ),
    PREPARED_DISPLAY_ONLY(
        id = "prepared_display_only",
        label = "Prepared display only",
        description = "Prepare the display before the rehearsal and avoid interactive device use during the running order.",
    ),
    PRINT_ONLY(
        id = "print_only",
        label = "Print only",
        description = "Use the offline printed packet during the rehearsal and keep devices put away.",
    ),
}

enum class ReadingSlotKind(val label: String) {
    ALIYAH("Aliyah"),
    MAFTIR("Maftir"),
}

enum class ReadingPassageAvailability(val label: String) {
    AVAILABLE("Installed passage"),
    UNAVAILABLE("Passage unavailable"),
}

enum class ReadingPreparationStatus(val label: String) {
    NOT_STARTED("Not started"),
    IN_PREPARATION("In preparation"),
    READY("Ready"),
}

data class ReadingSlot(
    val id: String,
    val sequence: Int,
    val label: String,
    val kind: ReadingSlotKind,
    val description: String,
    val portionTitle: String = "",
    val locator: String = "",
    val passageRange: String = "",
    val sourceId: String? = null,
    val passageAvailability: ReadingPassageAvailability = ReadingPassageAvailability.UNAVAILABLE,
    val passageUnavailableReason: String = "No Torah passage is installed for this reading slot.",
)

data class ReadingPlanEntry(
    val portionTitle: String = "",
    val locator: String = "",
    val passageRange: String = "",
    val assignee: String = "",
    val backupAssignee: String = "",
    val preparationStatus: ReadingPreparationStatus = ReadingPreparationStatus.NOT_STARTED,
    val manualOverride: Boolean = false,
    val overrideReason: String = "",
)

fun ExperienceState.readingPlanFor(slot: ReadingSlot): ReadingPlanEntry {
    val saved = readingPlans[slot.id] ?: ReadingPlanEntry()
    val legacyAssignee = readingAssignments[slot.id].orEmpty()
    return saved.copy(
        portionTitle = if (saved.manualOverride) saved.portionTitle else slot.portionTitle,
        locator = if (saved.manualOverride) saved.locator else slot.locator,
        passageRange = if (saved.manualOverride) saved.passageRange else slot.passageRange,
        assignee = saved.assignee.ifBlank { legacyAssignee },
    )
}

enum class ServiceTextAvailability(val label: String) {
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    NOT_APPLICABLE("Not applicable"),
}

data class ServiceTextField(
    val availability: ServiceTextAvailability = ServiceTextAvailability.UNAVAILABLE,
    val text: String = "",
    val languageTag: String = "",
    val detail: String = "Text is not included in this installed catalog.",
)

data class ServiceRoleResponse(
    val speakerRole: ParticipantRole,
    val responderRole: ParticipantRole,
    val prompt: ServiceTextField = ServiceTextField(),
    val response: ServiceTextField = ServiceTextField(),
)

data class ServiceTextProvenance(
    val editionId: String = "Not established",
    val editionTitle: String = "Not established",
    val sourceUnitId: String = "Not established",
    val locator: String = "Not established",
    val provenance: String = "Not established",
    val license: String = "Not established",
    val conclusionStatus: ConclusionStatus = ConclusionStatus.UNRESOLVED,
    val reviewState: EditorialReviewState = EditorialReviewState.PROPOSED,
    val editorialTreatment: String = "Not established",
    val punctuationSource: String = "Not established",
    val vocalizationSource: String = "Not established",
    val variantNotes: String = "Not established",
)

data class ServiceSegmentContent(
    val sourceText: ServiceTextField = ServiceTextField(),
    val translation: ServiceTextField = ServiceTextField(),
    val transliteration: ServiceTextField = ServiceTextField(),
    val roleTexts: Map<ParticipantRole, ServiceTextField> = emptyMap(),
    val responses: List<ServiceRoleResponse> = emptyList(),
    val provenance: ServiceTextProvenance = ServiceTextProvenance(),
)

data class PracticeStep(
    val id: String,
    val title: String,
    val detail: String,
)

data class PracticeCard(
    val id: String,
    val topic: String,
    val title: String,
    val summary: String,
    val action: String,
    val context: String,
    val supplies: String,
    val steps: List<PracticeStep>,
    val circumstancesDiffer: String,
    val purpose: String,
    val conclusionStatus: ConclusionStatus,
    val reviewState: EditorialReviewState,
    val primarySourceIds: List<String>,
    val reasoning: String,
    val otherReadings: String,
    val confidence: String,
    val reviewDue: String,
)

data class ExplanationTrace(
    val result: String,
    val facts: List<String>,
    val rule: String,
    val sourceIds: List<String>,
)

data class ServiceSegment(
    val id: String,
    val phase: String,
    val title: String,
    val summary: String,
    val movementCue: String,
    val accessibleAlternative: String,
    val voiceCue: String,
    val roleCues: Map<ParticipantRole, String>,
    val explanation: ExplanationTrace,
    val inclusionPolicy: SegmentInclusionPolicy? = null,
    val content: ServiceSegmentContent = ServiceSegmentContent(),
)

data class ServiceDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val segments: List<ServiceSegment>,
    val preflightSteps: List<PracticeStep>,
    val readingSlots: List<ReadingSlot> = emptyList(),
    val expectedDayKind: ServiceDayKind = ServiceDayKind.SHABBAT,
)

data class SourceUnit(
    val id: String,
    val title: String,
    val locator: String,
    val language: String,
    val body: String,
    val edition: String,
    val provenance: String,
    val license: String,
    val conclusionStatus: ConclusionStatus,
    val reviewState: EditorialReviewState,
    val relatedPracticeIds: List<String>,
    val relatedSegmentIds: List<String>,
)

data class ChoiceOption(
    val id: String,
    val title: String,
    val argument: String,
)

data class CommunityChoice(
    val id: String,
    val title: String,
    val summary: String,
    val options: List<ChoiceOption>,
    val conclusionStatus: ConclusionStatus,
    val reviewState: EditorialReviewState,
)

data class DemonstratorCatalog(
    val label: String,
    val noticeTitle: String,
    val noticeBody: String,
    val practiceCards: List<PracticeCard>,
    val service: ServiceDefinition,
    val sources: List<SourceUnit>,
    val communityChoice: CommunityChoice,
    val disputedPracticeDossier: DisputedPracticeDossier? = null,
)

data class ExperienceState(
    val serviceInstanceDate: LocalDate? = null,
    val selectedServicePlanId: String? = null,
    val completedPracticeStepIds: Set<String> = emptySet(),
    val savedPracticeCardIds: Set<String> = emptySet(),
    val completedPreflightStepIds: Set<String> = emptySet(),
    val completedServiceSegmentIds: Set<String> = emptySet(),
    val selectedRole: ParticipantRole = ParticipantRole.CONGREGANT,
    val bookmarkedSourceIds: Set<String> = emptySet(),
    val workspaceName: String = "",
    val workspaceKind: WorkspaceKind = WorkspaceKind.QAHAL,
    val roleAssignments: Map<ParticipantRole, String> = emptyMap(),
    val readingAssignments: Map<String, String> = emptyMap(),
    val readingPlans: Map<String, ReadingPlanEntry> = emptyMap(),
    val deviceUseMode: DeviceUseMode = DeviceUseMode.DEVICE_PERMITTED,
    val calendarRegion: CalendarRegion = CalendarRegion.UNSPECIFIED,
    val accessibilityProfile: ServiceAccessibilityProfile = ServiceAccessibilityProfile(),
    val selectedCommunityOptionId: String? = null,
    val provisionalCharterAdopted: Boolean = false,
    val communityCharter: CommunityCharter = CommunityCharter(),
    val disputedPracticeAdoption: CommunityAdoption = CommunityAdoption(),
    val reviewedDossierFactIds: Set<String> = emptySet(),
)

/**
 * Returns the state that is safe to show for [serviceDate].
 *
 * A missing or different persisted service date means the weekly preparation, reading plan, and
 * conductor progress have unknown provenance. Those fields therefore fail closed until the
 * repository atomically activates the requested service instance. Workspace identity, saved
 * material, and community governance records intentionally survive the rollover. Current role
 * assignments do not: the model does not yet distinguish a durable roster from service roles.
 */
fun ExperienceState.forServiceInstance(serviceDate: LocalDate): ExperienceState =
    if (serviceInstanceDate == serviceDate) {
        this
    } else {
        copy(
            serviceInstanceDate = serviceDate,
            completedPracticeStepIds = emptySet(),
            completedPreflightStepIds = emptySet(),
            completedServiceSegmentIds = emptySet(),
            roleAssignments = emptyMap(),
            readingAssignments = emptyMap(),
            readingPlans = emptyMap(),
        )
    }

fun nextShabbat(from: LocalDate): LocalDate {
    val days = (DayOfWeek.SATURDAY.value - from.dayOfWeek.value + 7) % 7
    return from.plusDays(days.toLong())
}

fun serviceDateLabel(from: LocalDate): String =
    nextShabbat(from).format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))

fun serviceProgress(catalog: DemonstratorCatalog, state: ExperienceState): Pair<Int, Int> =
    state.completedServiceSegmentIds.count { completed ->
        catalog.service.segments.any { it.id == completed }
    } to catalog.service.segments.size

fun serviceProgress(assembly: ServiceAssembly, state: ExperienceState): Pair<Int, Int> {
    val assembledSegmentIds = assembly.segments
        .mapTo(linkedSetOf()) { assembled -> assembled.segment.id }
    return assembledSegmentIds.count(state.completedServiceSegmentIds::contains) to
        assembledSegmentIds.size
}

fun practiceProgress(catalog: DemonstratorCatalog, state: ExperienceState): Pair<Int, Int> {
    val allSteps = catalog.practiceCards.flatMap(PracticeCard::steps)
    return state.completedPracticeStepIds.count { completed ->
        allSteps.any { it.id == completed }
    } to allSteps.size
}

fun buildServicePacket(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    dateLabel: String,
    locationLabel: String,
): String = renderServicePacket(
    catalog = catalog,
    state = state,
    assembly = assembleService(
        catalog = catalog,
        state = state,
        dateLabel = dateLabel,
        locationLabel = locationLabel,
    ),
)

fun buildServicePacket(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    context: ServiceAssemblyContext,
): String = renderServicePacket(
    catalog = catalog,
    state = state,
    assembly = assembleService(catalog, state, context),
)

private fun renderServicePacket(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    assembly: ServiceAssembly,
): String = buildString {
    appendLine(catalog.service.title)
    appendLine(catalog.label)
    appendLine(catalog.noticeBody)
    val containsInstalledText = catalog.service.segments.any { segment ->
        segment.content.containsAvailableText()
    }
    if (!containsInstalledText) {
        appendLine("WARNING: No prayer text is included in this installed catalog.")
    }
    appendLine()
    appendLine("CONTEXT")
    val workspaceType = assembly.contextFacts
        .firstOrNull { fact -> fact.label == "Workspace type" }
        ?.value
    assembly.contextFacts
        .filterNot { fact -> fact.label == "Workspace type" }
        .forEach { fact ->
            val value = if (fact.label == "Workspace" && workspaceType != null) {
                "${fact.value} ($workspaceType)"
            } else {
                fact.value
            }
            appendLine("${fact.label}: $value")
    }
    appendLine()
    appendLine("QAHAL CHARTER")
    val charter = state.communityCharter
    appendLine("Adoption: ${if (charter.adopted) "Adopted for this workspace" else "Draft; not adopted"}")
    appendLine("Version: ${charter.version.ifBlank { "Draft 1" }}")
    appendLine("Purpose: ${charter.purpose.ifBlank { "Incomplete" }}")
    appendLine("Participants: ${charter.participants.ifBlank { "Incomplete" }}")
    appendLine("Authority limits: ${charter.authorityLimits.ifBlank { "Incomplete" }}")
    appendLine("Decision process: ${charter.decisionProcess.ifBlank { "Incomplete" }}")
    appendLine("Role terms: ${charter.roleTerms.ifBlank { "Incomplete" }}")
    appendLine("Accessibility commitment: ${charter.accessibilityCommitment.ifBlank { "Incomplete" }}")
    appendLine("Effective date: ${charter.effectiveDate.ifBlank { "Incomplete" }}")
    appendLine("Review date: ${charter.reviewDate.ifBlank { "Incomplete" }}")
    val missingCharterFields = charter.missingFields()
    if (missingCharterFields.isNotEmpty()) {
        appendLine("Incomplete fields: ${missingCharterFields.joinToString()}")
    }
    appendLine()
    appendLine("READINESS")
    appendLine("Status: ${assembly.readiness.status.name}")
    assembly.readiness.overrideReason?.let { reason ->
        appendLine("Rehearsal override: $reason")
    }
    if (assembly.readiness.issues.isEmpty()) {
        appendLine("No readiness blockers or warnings.")
    } else {
        assembly.readiness.issues.forEach { issue ->
            appendLine("${issue.severity.name}: ${issue.title} — ${issue.detail}")
        }
    }
    appendLine()
    appendLine("ROLE ASSIGNMENTS")
    ParticipantRole.entries.filter { it != ParticipantRole.CONGREGANT }.forEach { role ->
        val assignee = assembly.context.roles.assignments[role].orEmpty().ifBlank { "Unassigned" }
        appendLine("${role.label}: $assignee")
    }
    appendLine()
    appendLine("OUTPUT PROFILE")
    appendLine("Device use: ${assembly.context.deviceUseMode.label}")
    appendLine(assembly.context.deviceUseMode.description)
    appendLine()
    appendLine("CHECKLIST STATUS")
    val practiceSteps = catalog.practiceCards.flatMap { card ->
        card.steps.map { step -> card.title to step }
    }
    val completedPractice = practiceSteps.count { (_, step) ->
        step.id in state.completedPracticeStepIds
    }
    appendLine("Practice preparation: $completedPractice of ${practiceSteps.size} complete")
    practiceSteps.forEach { (cardTitle, step) ->
        val done = if (step.id in state.completedPracticeStepIds) "[x]" else "[ ]"
        appendLine("$done $cardTitle — ${step.title}")
    }
    val completedPreflightIds = assembly.context.preflight.completedStepIds
    val completedPreflight = catalog.service.preflightSteps.count { step -> step.id in completedPreflightIds }
    appendLine("Service preflight: $completedPreflight of ${catalog.service.preflightSteps.size} complete")
    catalog.service.preflightSteps.forEach { step ->
        val done = if (step.id in completedPreflightIds) "[x]" else "[ ]"
        appendLine("$done ${step.title}")
    }
    appendLine()
    appendLine("TORAH-READING ASSIGNMENTS")
    if (assembly.context.readings.assignments.isEmpty()) {
        appendLine("No reading slots are defined in this installed catalog.")
    } else {
        assembly.context.readings.orderedAssignments.forEach { assignment ->
            val assignee = assignment.assignee.orEmpty().ifBlank { "Open assignment" }
            appendLine(
                "${assignment.sequence}. ${assignment.label} (${assignment.kind.label}): $assignee",
            )
            appendLine("   Portion: ${assignment.portionTitle.ifBlank { "Unavailable" }}")
            appendLine("   Locator: ${assignment.locator.ifBlank { "Unavailable" }}")
            appendLine("   Range: ${assignment.passageRange.ifBlank { "Unavailable" }}")
            appendLine(
                "   Passage source: ${if (assignment.manualOverride) "Manual local plan; no installed source" else assignment.sourceId ?: "Unavailable"}",
            )
            if (assignment.passageAvailability == ReadingPassageAvailability.UNAVAILABLE) {
                appendLine("   Passage status: ${assignment.passageUnavailableReason}")
            } else if (assignment.manualOverride) {
                appendLine("   Passage status: Manual local override; not verified by an installed source")
                appendLine("   Override reason: ${assignment.overrideReason.ifBlank { "Not recorded" }}")
            }
            appendLine("   Backup: ${assignment.backupAssignee.ifBlank { "Not assigned" }}")
            appendLine("   Preparation: ${assignment.preparationStatus.label}")
        }
    }
    appendLine()
    appendLine("COMPOSITION DECISIONS")
    assembly.compositionDecisions.forEach { decision ->
        val applied = if (decision.applied) "applied" else "not applied"
        appendLine("${decision.action.name}: ${decision.targetSegmentId} ($applied) — ${decision.reason}")
        decision.facts.forEach { fact -> appendLine("- $fact") }
    }
    appendLine()
    appendLine("ORDERING DECISION")
    appendLine(assembly.orderingDecision.explanation)
    appendLine("Conclusion evidence status: ${assembly.orderingDecision.conclusionStatus.label}")
    appendLine("Review state: ${assembly.orderingDecision.reviewState.label}")
    assembly.orderingDecision.facts.forEach { fact ->
        appendLine("- $fact")
    }
    appendLine()
    appendLine("SERVICE REHEARSAL")
    assembly.segments.forEachIndexed { index, assembledSegment ->
        val segment = assembledSegment.segment
        val done = if (segment.id in state.completedServiceSegmentIds) "[x]" else "[ ]"
        appendLine("${index + 1}. $done ${segment.phase} — ${segment.title}")
        appendLine("   Inclusion: ${assembledSegment.inclusionReason}")
        assembledSegment.inclusionFacts.forEach { fact ->
            appendLine("   Evaluated fact: $fact")
        }
        appendLine("   ${segment.roleCues[state.selectedRole] ?: segment.summary}")
        appendLine("   Movement: ${assembledSegment.effectiveMovementCue}")
        appendLine("   Movement reason: ${assembledSegment.movementCueReason}")
        appendLine("   Voice: ${segment.voiceCue}")
        appendLine("   Accessible option: ${segment.accessibleAlternative}")
        appendLine("   Liturgical content:")
        appendServiceTextField("Source text", segment.content.sourceText, indent = "      ")
        appendServiceTextField("Translation", segment.content.translation, indent = "      ")
        appendServiceTextField("Transliteration", segment.content.transliteration, indent = "      ")
        if (segment.content.roleTexts.isEmpty()) {
            appendLine("      Role-specific text: None included")
        } else {
            segment.content.roleTexts.entries.sortedBy { entry -> entry.key.ordinal }.forEach { (role, text) ->
                appendServiceTextField("${role.label} text", text, indent = "      ")
            }
        }
        if (segment.content.responses.isEmpty()) {
            appendLine("      Responses: None included")
        } else {
            segment.content.responses.forEach { response ->
                appendLine("      ${response.speakerRole.label} → ${response.responderRole.label}")
                appendServiceTextField("Prompt", response.prompt, indent = "         ")
                appendServiceTextField("Response", response.response, indent = "         ")
            }
        }
        val provenance = segment.content.provenance
        appendLine("      Edition ID: ${provenance.editionId}")
        appendLine("      Edition: ${provenance.editionTitle}")
        appendLine("      Source unit: ${provenance.sourceUnitId}")
        appendLine("      Locator: ${provenance.locator}")
        appendLine("      Provenance: ${provenance.provenance}")
        appendLine("      License: ${provenance.license}")
        appendLine("      Conclusion evidence status: ${provenance.conclusionStatus.label}")
        appendLine("      Review state: ${provenance.reviewState.label}")
        appendLine("      Editorial treatment: ${provenance.editorialTreatment}")
        appendLine("      Punctuation source: ${provenance.punctuationSource}")
        appendLine("      Vocalization source: ${provenance.vocalizationSource}")
        appendLine("      Variants: ${provenance.variantNotes}")
        if (assembledSegment.sourceReferences.isEmpty()) {
            appendLine("   Sources: None listed")
        } else {
            assembledSegment.sourceReferences.forEach { source ->
                appendLine(
                    "   Source: ${source.id} — ${source.locator ?: "Locator unavailable in installed catalog"}",
                )
            }
        }
    }
    appendLine()
    appendLine("LOCAL CHOICE")
    val selected = catalog.communityChoice.options.firstOrNull {
        it.id == state.selectedCommunityOptionId
    }
    appendLine("${catalog.communityChoice.title}: ${selected?.title ?: "No option adopted"}")
    appendLine()
    catalog.disputedPracticeDossier?.let { dossier ->
        appendLine("DISPUTED-PRACTICE DOSSIER")
        appendLine(dossier.title)
        appendLine("Question: ${dossier.question}")
        appendLine("Scope: ${dossier.scope}")
        appendLine("Evidence status: ${dossier.conclusionStatus.label}")
        appendLine("Review state: ${dossier.reviewState.label}")
        appendLine("Historical position: ${dossier.historicalPosition}")
        appendLine("Editorial conclusion: ${dossier.editorialConclusion}")
        appendLine("Factual review:")
        dossier.factualQuestions.forEachIndexed { index, question ->
            val factId = "${dossier.id}.fact.${index + 1}"
            val reviewed = if (factId in state.reviewedDossierFactIds) "[x]" else "[ ]"
            appendLine("$reviewed $question")
        }
        appendLine("Arguments:")
        dossier.arguments.forEach { argument ->
            appendLine("- ${argument.title}: ${argument.claim}")
            argument.supports.forEach { support -> appendLine("  Supports: $support") }
            argument.challenges.forEach { challenge -> appendLine("  Challenge: $challenge") }
            appendLine("  Sources: ${argument.sourceIds.joinToString().ifBlank { "None" }}")
        }
        val adoption = state.disputedPracticeAdoption
        val adoptedOption = dossier.adoptionOptions.firstOrNull { it.id == adoption.optionId }
        appendLine("Separate community adoption:")
        appendLine("Option: ${adoptedOption?.title ?: "No local option adopted"}")
        adoptedOption?.let { option ->
            appendLine("Practice: ${option.practice}")
            appendLine("Scope note: ${option.scopeNote}")
        }
        appendLine("Recorded scope: ${adoption.scope.ifBlank { "Not recorded" }}")
        appendLine("Effective date: ${adoption.effectiveDate.ifBlank { "Not recorded" }}")
        appendLine("Review date: ${adoption.reviewDate.ifBlank { "Not recorded" }}")
        appendLine("Recorded by: ${adoption.recordedBy.ifBlank { "Not recorded" }}")
        appendLine("Adoption does not change the dossier's ${dossier.conclusionStatus.label} evidence status.")
        appendLine()
    }
    appendLine("SOURCE RECORD")
    if (catalog.sources.isEmpty()) {
        appendLine("No source records are included in this installed catalog.")
    } else {
        catalog.sources.forEach { source ->
            appendLine("${source.title} — ${source.locator}")
            appendLine("   Source ID: ${source.id}")
            appendLine("   Edition: ${source.edition}")
            appendLine("   Provenance: ${source.provenance}")
            appendLine("   License: ${source.license}")
            appendLine("   Conclusion evidence status: ${source.conclusionStatus.label}")
            appendLine("   Review state: ${source.reviewState.label}")
        }
    }
    appendLine()
    appendLine(
        if (containsInstalledText) {
            "Generated locally by ASEH. Verify the edition, license, conclusion status, and review state shown for every text."
        } else {
            "Generated locally by ASEH. No prayer text is included in this installed catalog."
        },
    )
}

private fun ServiceSegmentContent.containsAvailableText(): Boolean =
    sourceText.availability == ServiceTextAvailability.AVAILABLE ||
        translation.availability == ServiceTextAvailability.AVAILABLE ||
        transliteration.availability == ServiceTextAvailability.AVAILABLE ||
        roleTexts.values.any { field -> field.availability == ServiceTextAvailability.AVAILABLE } ||
        responses.any { response ->
            response.prompt.availability == ServiceTextAvailability.AVAILABLE ||
                response.response.availability == ServiceTextAvailability.AVAILABLE
        }

private fun StringBuilder.appendServiceTextField(
    label: String,
    field: ServiceTextField,
    indent: String,
) {
    when (field.availability) {
        ServiceTextAvailability.AVAILABLE -> {
            val language = field.languageTag.ifBlank { "language not recorded" }
            appendLine("$indent$label ($language): ${field.text}")
        }
        ServiceTextAvailability.UNAVAILABLE ->
            appendLine("$indent$label: UNAVAILABLE — ${field.detail}")
        ServiceTextAvailability.NOT_APPLICABLE ->
            appendLine("$indent$label: NOT APPLICABLE — ${field.detail}")
    }
}
