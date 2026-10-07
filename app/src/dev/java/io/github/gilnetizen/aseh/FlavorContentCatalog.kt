package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.ChoiceOption
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityChoice
import io.github.gilnetizen.aseh.core.model.ConclusionStatus
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.EditorialReviewState
import io.github.gilnetizen.aseh.core.model.ExplanationTrace
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.PracticeCard
import io.github.gilnetizen.aseh.core.model.PracticeStep
import io.github.gilnetizen.aseh.core.model.ReadingSlot
import io.github.gilnetizen.aseh.core.model.ReadingSlotKind
import io.github.gilnetizen.aseh.core.model.ServiceRoleResponse
import io.github.gilnetizen.aseh.core.model.ServiceDefinition
import io.github.gilnetizen.aseh.core.model.ServiceAssemblyCondition
import io.github.gilnetizen.aseh.core.model.ServiceSegment
import io.github.gilnetizen.aseh.core.model.SegmentInclusionPolicy
import io.github.gilnetizen.aseh.core.model.ServiceSegmentContent
import io.github.gilnetizen.aseh.core.model.ServiceTextAvailability
import io.github.gilnetizen.aseh.core.model.ServiceTextField
import io.github.gilnetizen.aseh.core.model.ServiceTextProvenance
import io.github.gilnetizen.aseh.core.model.SourceUnit
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceCatalog
import io.github.gilnetizen.aseh.domain.servicecatalog.SyntheticDevelopmentServiceCatalog
import io.github.gilnetizen.aseh.domain.workspace.SyntheticWorkspaceFixtures
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceFixture

internal fun flavorServiceCatalog(): ServiceCatalog =
    SyntheticDevelopmentServiceCatalog.catalog

internal fun flavorWorkspaceFixture(): WorkspaceFixture =
    SyntheticWorkspaceFixtures.reviewScenario

internal fun flavorContentCatalog(): DemonstratorCatalog = DemonstratorCatalog(
    label = "Development rehearsal",
    noticeTitle = "Development rehearsal · synthetic",
    noticeBody = "Unreviewed and non-normative. No prayer text; not a siddur or practical religious guidance.",
    practiceCards = listOf(
        PracticeCard(
            id = "practice.rehearsal.team",
            topic = "Shabbat rehearsal",
            title = "Confirm the service team",
            summary = "Give every rehearsal role a named owner before the group begins.",
            action = "Open Build, record available people, then complete this readiness check.",
            context = "For a small group rehearsing its coordination before Shabbat.",
            supplies = "A role list and a way to contact participants.",
            steps = listOf(
                PracticeStep("practice.team.leader", "Confirm the leader", "Record who will advance the rehearsal and call transitions."),
                PracticeStep("practice.team.reader", "Confirm the reader", "Record who will rehearse the reading handoff."),
                PracticeStep("practice.team.gabbai", "Confirm the gabbai", "Record who will track roles and the running order."),
                PracticeStep("practice.team.backup", "Name one backup", "Agree who can cover an unavailable role."),
            ),
            circumstancesDiffer = "One person may hold more than one rehearsal role. Leave unavailable roles visibly unassigned instead of inventing a name.",
            purpose = "A visible role map exposes gaps early and gives the conductor useful cues.",
            conclusionStatus = ConclusionStatus.PERSONAL_DISCIPLINE,
            reviewState = EditorialReviewState.DRAFTED,
            primarySourceIds = listOf("source.demo.role-readiness"),
            reasoning = "This is an ASEH-authored operational rehearsal pattern. It makes no claim about religious office or eligibility.",
            otherReadings = "A group may use different role names or combine roles when it records that choice.",
            confidence = "High confidence in the software workflow; no religious conclusion is asserted.",
            reviewDue = "2026-11-06",
        ),
        PracticeCard(
            id = "practice.rehearsal.access",
            topic = "Access and welcome",
            title = "Prepare an accessible path",
            summary = "Check that every participant can enter, sit, read, hear, and follow the rehearsal.",
            action = "Walk the participant path and record an alternative for every movement cue.",
            context = "Before the rehearsal, in the room that will be used.",
            supplies = "Seating plan, large-text copy, lighting controls, and contact information for access requests.",
            steps = listOf(
                PracticeStep("practice.access.path", "Check the route", "Remove avoidable barriers between the entrance and seating."),
                PracticeStep("practice.access.seating", "Offer flexible seating", "Keep at least one seated alternative visible for movement cues."),
                PracticeStep("practice.access.text", "Prepare readable text", "Test the packet at large text and high contrast."),
                PracticeStep("practice.access.sound", "Test audibility", "Confirm that spoken transitions can be heard or followed visually."),
            ),
            circumstancesDiffer = "Ask participants what works for them. Do not infer a person's needs from appearance or diagnosis.",
            purpose = "Access is part of planning rather than an exception added after the service is assembled.",
            conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
            reviewState = EditorialReviewState.DRAFTED,
            primarySourceIds = listOf("source.demo.access-path"),
            reasoning = "The card demonstrates how ASEH pairs every embodied cue with an equal alternative.",
            otherReadings = "The exact room plan must come from the participants and site, not this fixture.",
            confidence = "Draft product guidance; human accessibility review is still required.",
            reviewDue = "2026-11-06",
        ),
        PracticeCard(
            id = "practice.rehearsal.offline",
            topic = "Prepared display or print",
            title = "Prepare the offline packet",
            summary = "Export the running order before the device or network becomes unavailable.",
            action = "Review the generated packet, share or print it, and keep one accessible backup.",
            context = "Before Shabbat or whenever the active workspace uses a print-only or prepared-display practice.",
            supplies = "The completed role map, selected local option, and a printer or receiving app if desired.",
            steps = listOf(
                PracticeStep("practice.offline.review", "Review names and date", "Check the packet header and every assignment."),
                PracticeStep("practice.offline.choice", "Review the local choice", "Make sure an unresolved option is not shown as adopted."),
                PracticeStep("practice.offline.export", "Export the packet", "Use Build or Pray to open the system share sheet."),
                PracticeStep("practice.offline.backup", "Keep an accessible backup", "Confirm the packet remains readable without this app."),
            ),
            circumstancesDiffer = "If no printer or receiving app is available, rehearse from the offline screen before the relevant time.",
            purpose = "The workflow supports communities that do not use devices on Shabbat and avoids a network dependency.",
            conclusionStatus = ConclusionStatus.PERSONAL_DISCIPLINE,
            reviewState = EditorialReviewState.DRAFTED,
            primarySourceIds = listOf("source.demo.transition"),
            reasoning = "This is an operational export pattern, not a ruling about device use.",
            otherReadings = "Each workspace chooses its own device, prepared-display, or print-only practice.",
            confidence = "High confidence that the generated text is local; print behavior depends on the receiving Android app.",
            reviewDue = "2026-11-06",
        ),
    ),
    service = ServiceDefinition(
        id = "service.rehearsal.shabbat-morning",
        title = "Shabbat morning service rehearsal",
        subtitle = "A role-aware conductor that exercises the workflow without supplying liturgy.",
        preflightSteps = listOf(
            PracticeStep("preflight.roles", "Roles checked", "Leader, reader, gabbai, and host are assigned or visibly open."),
            PracticeStep("preflight.packet", "Packet checked", "Date, place, assignments, and local choice are current."),
            PracticeStep("preflight.access", "Access checked", "Movement alternatives and readable output are ready."),
            PracticeStep("preflight.offline", "Offline path checked", "The rehearsal can continue with network access disabled."),
        ),
        readingSlots = listOf(
            readingSlot("reading.aliyah.1", 1, "First aliyah", ReadingSlotKind.ALIYAH),
            readingSlot("reading.aliyah.2", 2, "Second aliyah", ReadingSlotKind.ALIYAH),
            readingSlot("reading.aliyah.3", 3, "Third aliyah", ReadingSlotKind.ALIYAH),
            readingSlot("reading.aliyah.4", 4, "Fourth aliyah", ReadingSlotKind.ALIYAH),
            readingSlot("reading.aliyah.5", 5, "Fifth aliyah", ReadingSlotKind.ALIYAH),
            readingSlot("reading.aliyah.6", 6, "Sixth aliyah", ReadingSlotKind.ALIYAH),
            readingSlot("reading.aliyah.7", 7, "Seventh aliyah", ReadingSlotKind.ALIYAH),
            readingSlot("reading.maftir", 8, "Maftir", ReadingSlotKind.MAFTIR),
        ),
        segments = listOf(
            segment(
                id = "segment.gather",
                phase = "Prepare",
                title = "Gather and orient",
                summary = "Confirm the room, roles, access needs, and running order.",
                movement = "Settle in the position that works for you.",
                alternative = "Remain seated or use another stable position without explanation.",
                voice = "Leader speaks; participants confirm readiness.",
                rule = "Always include the orientation in this development rehearsal.",
                sourceIds = listOf("source.demo.role-readiness", "source.demo.access-path"),
                inclusionPolicy = SegmentInclusionPolicy(
                    condition = ServiceAssemblyCondition.All(
                        listOf(
                            ServiceAssemblyCondition.DayKindIn(setOf(io.github.gilnetizen.aseh.core.model.ServiceDayKind.SHABBAT)),
                            ServiceAssemblyCondition.PlaceAvailable(),
                        ),
                    ),
                    includedReason = "Included because this is a Shabbat service and a service place is available.",
                    omittedReason = "Omitted until the service is confirmed as Shabbat and a place is available.",
                ),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Confirm that you can follow the packet and signal an access need.",
                    ParticipantRole.LEADER to "Name the current step and check that every role is ready.",
                    ParticipantRole.READER to "Confirm the reading handoff and your place in the packet.",
                    ParticipantRole.GABBAI to "Check assignments and announce any open role.",
                    ParticipantRole.HOST to "Confirm the space, seating, lighting, and printed backup.",
                ),
            ),
            segment(
                id = "segment.calendar.israel",
                phase = "Context",
                title = "Confirm the Israel calendar profile",
                summary = "Verify that the running order and reading plan use the Israel festival and calendar scheme.",
                movement = "No movement is required.",
                alternative = "Review the same context line in the printed packet.",
                voice = "Leader names the active calendar region; gabbai confirms the packet.",
                rule = "Include only when the workspace explicitly selects the Israel calendar region.",
                sourceIds = listOf("source.demo.context-rules"),
                inclusionPolicy = SegmentInclusionPolicy(
                    condition = ServiceAssemblyCondition.CalendarRegionIn(setOf(CalendarRegion.ISRAEL)),
                    includedReason = "Included because the workspace selected the Israel calendar region.",
                    omittedReason = "Omitted because the Israel calendar region is not active.",
                ),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Confirm that the packet visibly says Israel calendar.",
                    ParticipantRole.LEADER to "Name the Israel calendar profile before advancing.",
                    ParticipantRole.READER to "Confirm the reading plan was prepared for this calendar region.",
                    ParticipantRole.GABBAI to "Check the region label against the prepared packet.",
                    ParticipantRole.HOST to "Keep the region label visible on the room copy.",
                ),
            ),
            segment(
                id = "segment.calendar.diaspora",
                phase = "Context",
                title = "Confirm the diaspora calendar profile",
                summary = "Verify that the running order and reading plan use the diaspora festival and calendar scheme.",
                movement = "No movement is required.",
                alternative = "Review the same context line in the printed packet.",
                voice = "Leader names the active calendar region; gabbai confirms the packet.",
                rule = "Include only when the workspace explicitly selects the diaspora calendar region.",
                sourceIds = listOf("source.demo.context-rules"),
                inclusionPolicy = SegmentInclusionPolicy(
                    condition = ServiceAssemblyCondition.CalendarRegionIn(setOf(CalendarRegion.DIASPORA)),
                    includedReason = "Included because the workspace selected the diaspora calendar region.",
                    omittedReason = "Omitted because the diaspora calendar region is not active.",
                ),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Confirm that the packet visibly says diaspora calendar.",
                    ParticipantRole.LEADER to "Name the diaspora calendar profile before advancing.",
                    ParticipantRole.READER to "Confirm the reading plan was prepared for this calendar region.",
                    ParticipantRole.GABBAI to "Check the region label against the prepared packet.",
                    ParticipantRole.HOST to "Keep the region label visible on the room copy.",
                ),
            ),
            segment(
                id = "segment.accessibility-profile",
                phase = "Access",
                title = "Activate movement alternatives",
                summary = "Confirm that the conductor and packet lead with equal stationary alternatives for embodied cues.",
                movement = "Demonstrate both the standard cue and the stationary alternative.",
                alternative = "Use the stationary alternative without explanation or delay.",
                voice = "Leader names both options once; participants choose privately.",
                rule = "Include when the active accessibility profile requests movement alternatives.",
                sourceIds = listOf("source.demo.access-path", "source.demo.context-rules"),
                inclusionPolicy = SegmentInclusionPolicy(
                    condition = ServiceAssemblyCondition.MovementAlternativesRequested(),
                    includedReason = "Included because movement alternatives are active in the accessibility profile.",
                    omittedReason = "Omitted because movement alternatives are not selected; alternatives remain visible on every cue.",
                ),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Choose either cue without needing to explain the choice.",
                    ParticipantRole.LEADER to "Present both options with equal weight.",
                    ParticipantRole.READER to "Confirm the reading handoff also has a stationary route.",
                    ParticipantRole.GABBAI to "Watch for any cue that lacks an equal alternative.",
                    ParticipantRole.HOST to "Check that both options work in the actual room.",
                ),
            ),
            segment(
                id = "segment.offline-handoff",
                phase = "Offline",
                title = "Hand off to the prepared packet",
                summary = "Confirm that every participant has the final offline copy before interactive device use ends.",
                movement = "Distribute or point to the prepared copies before the run begins.",
                alternative = "Provide a large-text or assisted-reading copy in the same sequence.",
                voice = "Host confirms copy count; leader announces the final packet version.",
                rule = "Include for prepared-display and print-only device profiles.",
                sourceIds = listOf("source.demo.transition", "source.demo.context-rules"),
                inclusionPolicy = SegmentInclusionPolicy(
                    condition = ServiceAssemblyCondition.DeviceModeIn(
                        setOf(DeviceUseMode.PREPARED_DISPLAY_ONLY, DeviceUseMode.PRINT_ONLY),
                    ),
                    includedReason = "Included because the selected device profile requires a prepared offline handoff.",
                    omittedReason = "Omitted because interactive device use is permitted for this rehearsal.",
                ),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Confirm that your copy is readable and matches the announced version.",
                    ParticipantRole.LEADER to "Announce the packet version before the device is put away.",
                    ParticipantRole.READER to "Confirm your assignments appear in the final packet.",
                    ParticipantRole.GABBAI to "Resolve any open assignment before the final copy is used.",
                    ParticipantRole.HOST to "Distribute accessible copies and retain one backup.",
                ),
            ),
            segment(
                id = "segment.opening",
                phase = "Open",
                title = "Opening transition",
                summary = "Practice a clear transition from gathering into the service order.",
                movement = "Use the workspace's chosen posture cue.",
                alternative = "A seated posture is an equal rehearsal option.",
                voice = "Leader cue followed by a group response placeholder.",
                rule = "Included to test leader and congregant views. No prayer text is attached.",
                sourceIds = listOf("source.demo.transition"),
                liturgicalContentExpected = true,
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Follow the visible transition and rehearse the response timing.",
                    ParticipantRole.LEADER to "Announce the transition, pause, then advance after the response window.",
                    ParticipantRole.READER to "Observe the transition and prepare for the later handoff.",
                    ParticipantRole.GABBAI to "Track the current position and note any timing issue.",
                    ParticipantRole.HOST to "Watch sight lines and audibility from the room.",
                ),
            ),
            segment(
                id = "segment.response",
                phase = "Participate",
                title = "Leader and group response pattern",
                summary = "Rehearse the conductor's cue, a pause, and a visible group response.",
                movement = "No movement is required for this fixture.",
                alternative = "Participants may follow visually without speaking.",
                voice = "Call, pause, response; synthetic placeholders only.",
                rule = "Included because the selected service is communal rehearsal.",
                sourceIds = listOf("source.demo.transition"),
                liturgicalContentExpected = true,
                roleTextRoles = setOf(ParticipantRole.LEADER, ParticipantRole.CONGREGANT),
                responsePair = ParticipantRole.LEADER to ParticipantRole.CONGREGANT,
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Wait for the response marker, then use the agreed placeholder.",
                    ParticipantRole.LEADER to "Give the cue and leave a full response window before advancing.",
                    ParticipantRole.READER to "Follow the group response and hold your next handoff.",
                    ParticipantRole.GABBAI to "Note whether the cue was visible and audible.",
                    ParticipantRole.HOST to "Check that remote corners of the room can follow the cue.",
                ),
            ),
            segment(
                id = "segment.reading",
                phase = "Read",
                title = "Reading handoff rehearsal",
                summary = "Practice calling the reader, confirming the assignment, and returning control.",
                movement = "Reader moves only if the route is clear and desired.",
                alternative = "Bring the reading position to the reader or rehearse from the current seat.",
                voice = "Gabbai cue, reader confirmation, leader resumes.",
                rule = "Included to exercise reader and gabbai roles; no sacred text is supplied.",
                sourceIds = listOf("source.demo.role-readiness", "source.demo.access-path"),
                liturgicalContentExpected = true,
                roleTextRoles = setOf(ParticipantRole.READER, ParticipantRole.GABBAI),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Follow the handoff marker and keep the route clear.",
                    ParticipantRole.LEADER to "Yield to the gabbai, then resume when the handoff closes.",
                    ParticipantRole.READER to "Confirm the assignment and rehearse the start and finish signals.",
                    ParticipantRole.GABBAI to "Name the assigned reader and verify readiness before the handoff.",
                    ParticipantRole.HOST to "Check the route, reading surface, light, and audibility.",
                ),
            ),
            segment(
                id = "segment.teaching",
                phase = "Learn",
                title = "Optional teaching pause",
                summary = "Practice a short, timed explanation with a clear return to the running order.",
                movement = "Keep the current comfortable position.",
                alternative = "Provide the teaching note in the printed packet.",
                voice = "Assigned speaker; conductor gives a return cue.",
                rule = "Included as a development fixture; its adopted placement is recorded in Build.",
                sourceIds = listOf("source.demo.local-choice"),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Follow the teaching note and the visible return marker.",
                    ParticipantRole.LEADER to "Introduce the pause, keep time, and announce the return.",
                    ParticipantRole.READER to "Hold the packet position until the return cue.",
                    ParticipantRole.GABBAI to "Track the agreed placement and time limit.",
                    ParticipantRole.HOST to "Confirm the note is readable in the printed fallback.",
                ),
            ),
            segment(
                id = "segment.close",
                phase = "Close",
                title = "Close and review",
                summary = "End the rehearsal and capture concrete changes for the next run.",
                movement = "Use any stable closing posture.",
                alternative = "Remain in place and participate through the review prompt.",
                voice = "Leader closes; each role may name one correction.",
                rule = "Always include a review so rehearsal findings become preparation work.",
                sourceIds = listOf("source.demo.local-choice"),
                cues = mapOf(
                    ParticipantRole.CONGREGANT to "Name one point that made participation clearer or harder.",
                    ParticipantRole.LEADER to "Close the running order and invite one concise correction per role.",
                    ParticipantRole.READER to "Confirm whether the handoff and packet position were clear.",
                    ParticipantRole.GABBAI to "Record open assignments and timing corrections.",
                    ParticipantRole.HOST to "Record room, access, and printed-output changes.",
                ),
            ),
        ),
    ),
    sources = listOf(
        source(
            id = "source.demo.role-readiness",
            title = "ASEH rehearsal protocol",
            locator = "Development fixture 1:1",
            body = "Before a rehearsal starts, the coordinator records who owns each active role and leaves every unfilled role visibly open.",
            practiceIds = listOf("practice.rehearsal.team"),
            segmentIds = listOf("segment.gather", "segment.reading"),
        ),
        source(
            id = "source.demo.access-path",
            title = "ASEH accessible-cue protocol",
            locator = "Development fixture 1:2",
            body = "Every movement cue in the rehearsal is paired with a seated or stationary alternative that preserves the same place in the running order.",
            practiceIds = listOf("practice.rehearsal.access"),
            segmentIds = listOf("segment.gather", "segment.reading"),
        ),
        source(
            id = "source.demo.transition",
            title = "ASEH conductor protocol",
            locator = "Development fixture 1:3",
            body = "The conductor names each transition, leaves time for the intended response, and advances only after the handoff is clear.",
            practiceIds = listOf("practice.rehearsal.offline"),
            segmentIds = listOf("segment.opening", "segment.response"),
        ),
        source(
            id = "source.demo.local-choice",
            title = "ASEH local-choice protocol",
            locator = "Development fixture 1:4",
            body = "When more than one rehearsal option is workable, the workspace records the selected option, its scope, and the fact that the choice is local.",
            practiceIds = emptyList(),
            segmentIds = listOf("segment.teaching", "segment.close"),
        ),
        source(
            id = "source.demo.context-rules",
            title = "ASEH context-assembly protocol",
            locator = "Development fixture 1:5",
            body = "The service assembler records the date, calendar region, place availability, device profile, assignments, and accessibility profile, then exposes every include or omit decision with its evaluated facts.",
            practiceIds = listOf("practice.rehearsal.access", "practice.rehearsal.offline"),
            segmentIds = listOf(
                "segment.gather",
                "segment.calendar.israel",
                "segment.calendar.diaspora",
                "segment.accessibility-profile",
                "segment.offline-handoff",
            ),
        ),
        source(
            id = "source.demo.dossier-method",
            title = "ASEH disputed-practice dossier protocol",
            locator = "Development fixture 1:6",
            body = "A disputed modern practice keeps factual questions, source evidence, arguments, editorial status, and a community's local adoption in separate records. Adoption never upgrades the evidence status.",
            practiceIds = emptyList(),
            segmentIds = emptyList(),
        ),
    ),
    communityChoice = CommunityChoice(
        id = "choice.teaching-placement",
        title = "Placement of the teaching pause",
        summary = "Both choices are synthetic rehearsal options. Neither is presented as a religious conclusion.",
        options = listOf(
            ChoiceOption(
                id = "choice.teaching.after-reading",
                title = "After the reading handoff",
                argument = "The transition is already paused, so the teaching note can stay near the material it explains.",
            ),
            ChoiceOption(
                id = "choice.teaching.before-close",
                title = "Before the closing review",
                argument = "The main running order stays continuous, while the teaching note leads naturally into reflection.",
            ),
        ),
        conclusionStatus = ConclusionStatus.UNRESOLVED,
        reviewState = EditorialReviewState.DRAFTED,
    ),
    disputedPracticeDossier = developmentLightingDossier(),
)

private fun segment(
    id: String,
    phase: String,
    title: String,
    summary: String,
    movement: String,
    alternative: String,
    voice: String,
    rule: String,
    sourceIds: List<String>,
    inclusionPolicy: SegmentInclusionPolicy? = null,
    liturgicalContentExpected: Boolean = false,
    roleTextRoles: Set<ParticipantRole> = emptySet(),
    responsePair: Pair<ParticipantRole, ParticipantRole>? = null,
    cues: Map<ParticipantRole, String>,
) = ServiceSegment(
    id = id,
    phase = phase,
    title = title,
    summary = summary,
    movementCue = movement,
    accessibleAlternative = alternative,
    voiceCue = voice,
    roleCues = cues,
    explanation = ExplanationTrace(
        result = "Included",
        facts = listOf(
            "Service: communal development rehearsal",
            "Content pack: synthetic dev fixture",
            "Network: not required",
        ),
        rule = rule,
        sourceIds = sourceIds,
    ),
    inclusionPolicy = inclusionPolicy,
    content = developmentSegmentContent(
        liturgicalContentExpected = liturgicalContentExpected,
        roleTextRoles = roleTextRoles,
        responsePair = responsePair,
    ),
)

private fun readingSlot(
    id: String,
    sequence: Int,
    label: String,
    kind: ReadingSlotKind,
) = ReadingSlot(
    id = id,
    sequence = sequence,
    label = label,
    kind = kind,
    description = "Operational reader assignment for the ${label.lowercase()} reading slot.",
    passageUnavailableReason = "Calendar-linked Torah portion data is not installed in this synthetic development catalog. Enter a manual local plan only if you can verify it independently.",
)

private fun developmentSegmentContent(
    liturgicalContentExpected: Boolean,
    roleTextRoles: Set<ParticipantRole>,
    responsePair: Pair<ParticipantRole, ParticipantRole>?,
): ServiceSegmentContent {
    val field = if (liturgicalContentExpected) {
        ServiceTextField(
            availability = ServiceTextAvailability.UNAVAILABLE,
            detail = "No liturgical text is bundled: LITURGY-001 and the required edition-level rights review remain unresolved.",
        )
    } else {
        ServiceTextField(
            availability = ServiceTextAvailability.NOT_APPLICABLE,
            detail = "This is an operational rehearsal step rather than a liturgical text segment.",
        )
    }
    return ServiceSegmentContent(
        sourceText = field,
        translation = field.copy(
            detail = if (liturgicalContentExpected) {
                "No translation is bundled because no anchor text and distribution-approved edition have been selected."
            } else {
                field.detail
            },
        ),
        transliteration = field.copy(
            detail = if (liturgicalContentExpected) {
                "No transliteration is bundled because the underlying text and transliteration profile are not established."
            } else {
                field.detail
            },
        ),
        roleTexts = roleTextRoles.associateWith { role ->
            field.copy(
                detail = "${role.label} liturgical text is unavailable until an exact edition is selected and approved for distribution.",
            )
        },
        responses = responsePair?.let { (speaker, responder) ->
            listOf(
                ServiceRoleResponse(
                    speakerRole = speaker,
                    responderRole = responder,
                    prompt = field.copy(
                        detail = "The leader prompt is intentionally absent from this development catalog.",
                    ),
                    response = field.copy(
                        detail = "The congregational response is intentionally absent from this development catalog.",
                    ),
                ),
            )
        }.orEmpty(),
        provenance = if (liturgicalContentExpected) {
            ServiceTextProvenance(
                editionId = "NOT SELECTED — LITURGY-001 open",
                editionTitle = "NOT ESTABLISHED",
                sourceUnitId = "NOT ESTABLISHED",
                locator = "NOT ESTABLISHED",
                provenance = "No sacred text was ingested into this synthetic development catalog.",
                license = "NOT ESTABLISHED — distribution blocked",
                conclusionStatus = ConclusionStatus.UNRESOLVED,
                reviewState = EditorialReviewState.DRAFTED,
                editorialTreatment = "No reconstruction, normalization, or supplied wording has been performed.",
                punctuationSource = "NOT ESTABLISHED",
                vocalizationSource = "NOT ESTABLISHED",
                variantNotes = "No witness or variant set has been selected.",
            )
        } else {
            ServiceTextProvenance(
                editionId = "Not applicable",
                editionTitle = "ASEH operational development fixture",
                sourceUnitId = "Not applicable",
                locator = "Not applicable",
                provenance = "Original ASEH operational text; no sacred source text is represented.",
                license = "CC-BY-SA-4.0",
                conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
                reviewState = EditorialReviewState.DRAFTED,
                editorialTreatment = "Synthetic operational wording for development testing.",
                punctuationSource = "Not applicable",
                vocalizationSource = "Not applicable",
                variantNotes = "Not applicable",
            )
        },
    )
}

private fun source(
    id: String,
    title: String,
    locator: String,
    body: String,
    practiceIds: List<String>,
    segmentIds: List<String>,
) = SourceUnit(
    id = id,
    title = title,
    locator = locator,
    language = "English",
    body = body,
    edition = "ASEH synthetic development fixtures, revision 1",
    provenance = "Original ASEH development text. No historical edition or religious authority is claimed.",
    license = "CC-BY-SA-4.0",
    conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
    reviewState = EditorialReviewState.DRAFTED,
    relatedPracticeIds = practiceIds,
    relatedSegmentIds = segmentIds,
)
