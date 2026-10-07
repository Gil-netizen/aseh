package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityAdoption
import io.github.gilnetizen.aseh.core.model.CommunityCharter
import io.github.gilnetizen.aseh.core.model.ConclusionStatus
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.EditorialReviewState
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingSlotKind
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ReadingPreparationStatus
import io.github.gilnetizen.aseh.core.model.ServiceTextAvailability
import io.github.gilnetizen.aseh.core.model.ServiceAccessibilityProfile
import io.github.gilnetizen.aseh.core.model.ServiceAssembly
import io.github.gilnetizen.aseh.core.model.ServiceAssemblyContext
import io.github.gilnetizen.aseh.core.model.ServiceCompositionAction
import io.github.gilnetizen.aseh.core.model.ServiceDateContext
import io.github.gilnetizen.aseh.core.model.ServiceDayKind
import io.github.gilnetizen.aseh.core.model.ServicePlaceAvailability
import io.github.gilnetizen.aseh.core.model.ServiceReadinessStatus
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import io.github.gilnetizen.aseh.core.model.assembleService
import io.github.gilnetizen.aseh.core.model.buildServicePacket
import io.github.gilnetizen.aseh.core.model.serviceAssemblyContext
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentCatalogTest {
    private val catalog = flavorContentCatalog()

    @Test
    fun everyCatalogIdIsUniqueAndEveryRelationshipResolves() {
        val practiceIds = catalog.practiceCards.map { it.id }
        val practiceStepIds = catalog.practiceCards.flatMap { card -> card.steps.map { it.id } }
        val preflightStepIds = catalog.service.preflightSteps.map { it.id }
        val segmentIds = catalog.service.segments.map { it.id }
        val readingSlotIds = catalog.service.readingSlots.map { it.id }
        val sourceIds = catalog.sources.map { it.id }
        val optionIds = catalog.communityChoice.options.map { it.id }
        val dossier = requireNotNull(catalog.disputedPracticeDossier)
        val dossierFactIds = dossier.factualQuestions.indices.map { index ->
            "${dossier.id}.fact.${index + 1}"
        }
        val allIds = buildList {
            addAll(practiceIds)
            addAll(practiceStepIds)
            addAll(preflightStepIds)
            add(catalog.service.id)
            addAll(segmentIds)
            addAll(readingSlotIds)
            addAll(sourceIds)
            add(catalog.communityChoice.id)
            addAll(optionIds)
            add(dossier.id)
            addAll(dossierFactIds)
            addAll(dossier.arguments.map { argument -> argument.id })
            addAll(dossier.adoptionOptions.map { option -> option.id })
        }

        assertTrue(allIds.all(String::isNotBlank))
        assertEquals("Catalog IDs must be globally unique", allIds.size, allIds.toSet().size)
        catalog.practiceCards.forEach { card ->
            assertTrue(
                "Practice card ${card.id} references a missing source",
                sourceIds.containsAll(card.primarySourceIds),
            )
        }
        catalog.service.segments.forEach { segment ->
            assertTrue(
                "Service segment ${segment.id} references a missing source",
                sourceIds.containsAll(segment.explanation.sourceIds),
            )
            assertEquals(
                "Service segment ${segment.id} must have a cue for every role",
                ParticipantRole.entries.toSet(),
                segment.roleCues.keys,
            )
        }
        assertEquals(8, catalog.service.readingSlots.size)
        assertEquals(
            7,
            catalog.service.readingSlots.count { slot -> slot.kind == ReadingSlotKind.ALIYAH },
        )
        assertEquals(
            1,
            catalog.service.readingSlots.count { slot -> slot.kind == ReadingSlotKind.MAFTIR },
        )
        assertEquals(
            (1..8).toList(),
            catalog.service.readingSlots.sortedBy { slot -> slot.sequence }.map { slot -> slot.sequence },
        )
        catalog.service.readingSlots.forEach { slot ->
            assertTrue(slot.portionTitle.isBlank())
            assertTrue(slot.locator.isBlank())
            assertTrue(slot.passageRange.isBlank())
            assertTrue(slot.passageUnavailableReason.contains("not installed", ignoreCase = true))
            slot.sourceId?.let { sourceId -> assertTrue(sourceId in sourceIds) }
        }
        catalog.sources.forEach { source ->
            assertTrue(
                "Source ${source.id} references a missing practice card",
                practiceIds.containsAll(source.relatedPracticeIds),
            )
            assertTrue(
                "Source ${source.id} references a missing service segment",
                segmentIds.containsAll(source.relatedSegmentIds),
            )
        }
        assertTrue("Dossier references must resolve", sourceIds.containsAll(dossier.sourceIds))
        dossier.arguments.forEach { argument ->
            assertTrue(
                "Dossier argument ${argument.id} references a missing source",
                sourceIds.containsAll(argument.sourceIds),
            )
        }
    }

    @Test
    fun developmentCatalogIsExplicitlySyntheticAndNeverApproved() {
        assertTrue(catalog.label.contains("development", ignoreCase = true))
        assertTrue(catalog.noticeTitle.contains("synthetic", ignoreCase = true))
        assertTrue(catalog.noticeBody.contains("unreviewed", ignoreCase = true))
        assertTrue(catalog.noticeBody.contains("non-normative", ignoreCase = true))
        assertTrue(catalog.noticeBody.contains("no prayer text", ignoreCase = true))

        val dossier = requireNotNull(catalog.disputedPracticeDossier)
        val conclusionStatuses = catalog.practiceCards.map { it.conclusionStatus } +
            catalog.sources.map { it.conclusionStatus } +
            catalog.communityChoice.conclusionStatus +
            dossier.conclusionStatus +
            catalog.service.segments.map { it.content.provenance.conclusionStatus }
        val reviewStates = catalog.practiceCards.map { it.reviewState } +
            catalog.sources.map { it.reviewState } +
            catalog.communityChoice.reviewState +
            dossier.reviewState +
            catalog.service.segments.map { it.content.provenance.reviewState }
        assertTrue(conclusionStatuses.isNotEmpty())
        assertTrue(reviewStates.isNotEmpty())
        assertTrue(reviewStates.all { state -> state == EditorialReviewState.DRAFTED })
        catalog.sources.forEach { source ->
            assertTrue(source.id.startsWith("source.demo."))
            assertTrue(source.edition.contains("synthetic", ignoreCase = true))
            assertTrue(source.provenance.contains("development", ignoreCase = true))
            assertTrue(source.provenance.contains("no historical edition", ignoreCase = true))
        }
        assertTrue(catalog.practiceCards.all { it.id.startsWith("practice.rehearsal.") })
        assertTrue(catalog.service.segments.all { it.id.startsWith("segment.") })
        catalog.service.segments.forEach { segment ->
            assertTrue(segment.content.sourceText.text.isBlank())
            assertTrue(segment.content.translation.text.isBlank())
            assertTrue(segment.content.transliteration.text.isBlank())
            assertTrue(segment.content.provenance.editionId.isNotBlank())
            assertTrue(segment.content.provenance.license.isNotBlank())
        }
        val responseSegment = catalog.service.segments.single { it.id == "segment.response" }
        assertEquals(ServiceTextAvailability.UNAVAILABLE, responseSegment.content.sourceText.availability)
        assertTrue(responseSegment.content.roleTexts.keys.containsAll(
            setOf(ParticipantRole.LEADER, ParticipantRole.CONGREGANT),
        ))
        assertEquals(1, responseSegment.content.responses.size)
        assertTrue(responseSegment.content.provenance.editionId.contains("LITURGY-001"))
        assertEquals(ConclusionStatus.UNRESOLVED, dossier.conclusionStatus)
        assertEquals(EditorialReviewState.DRAFTED, dossier.reviewState)
        assertTrue(dossier.historicalPosition.contains("NOT ESTABLISHED"))
        assertTrue(dossier.editorialConclusion.contains("UNRESOLVED"))
        assertTrue(dossier.reviewRequirement.contains("named human approval"))
        assertEquals(4, dossier.factualQuestions.size)
        assertEquals(2, dossier.arguments.size)
        assertEquals(2, dossier.adoptionOptions.size)
        dossier.adoptionOptions.forEach { option ->
            assertTrue(option.scopeNote.contains("does not", ignoreCase = true))
        }
    }

    @Test
    fun conditionalSegmentsFollowCalendarAccessibilityDeviceAndPlaceContext() {
        val israelState = completeState(
            deviceUseMode = DeviceUseMode.DEVICE_PERMITTED,
            accessibility = ServiceAccessibilityProfile(participantNeedsReviewed = true),
        )
        val israelContext = completeContext(
            state = israelState,
            calendarRegion = CalendarRegion.ISRAEL,
        )
        val israel = assembleService(catalog, israelState, israelContext)
        val israelIds = israel.segments.map { assembled -> assembled.segment.id }

        assertEquals(ServiceReadinessStatus.READY, israel.readiness.status)
        assertTrue("segment.gather" in israelIds)
        assertTrue("segment.calendar.israel" in israelIds)
        assertFalse("segment.calendar.diaspora" in israelIds)
        assertFalse("segment.accessibility-profile" in israelIds)
        assertFalse("segment.offline-handoff" in israelIds)
        val omittedDiaspora = israel.compositionDecision("segment.calendar.diaspora")
        assertEquals(ServiceCompositionAction.OMIT, omittedDiaspora.action)
        assertTrue(omittedDiaspora.reason.contains("diaspora calendar region is not active"))
        assertTrue(omittedDiaspora.facts.any { fact -> fact.contains("Calendar region: ISRAEL") })
        assertTrue(
            omittedDiaspora.sourceReferences.any { source -> source.id == "source.demo.context-rules" },
        )

        val diasporaState = completeState(
            deviceUseMode = DeviceUseMode.PRINT_ONLY,
            accessibility = ServiceAccessibilityProfile(
                participantNeedsReviewed = true,
                useMovementAlternatives = true,
                useLargeText = true,
            ),
        )
        val diaspora = assembleService(
            catalog,
            diasporaState,
            completeContext(diasporaState, CalendarRegion.DIASPORA),
        )
        val diasporaIds = diaspora.segments.map { assembled -> assembled.segment.id }

        assertEquals(ServiceReadinessStatus.READY, diaspora.readiness.status)
        assertFalse("segment.calendar.israel" in diasporaIds)
        assertTrue("segment.calendar.diaspora" in diasporaIds)
        assertTrue("segment.accessibility-profile" in diasporaIds)
        assertTrue("segment.offline-handoff" in diasporaIds)
        assertTrue(diaspora.readiness.warnings.any { issue -> issue.id == "readiness.device.print-only" })
        diaspora.segments.forEach { assembled ->
            assertEquals(assembled.segment.accessibleAlternative, assembled.effectiveMovementCue)
        }

        val placeUnavailable = assembleService(
            catalog,
            israelState,
            israelContext.copy(
                place = ServicePlaceAvailability.Unavailable("GPS and manual place are unavailable."),
            ),
        )
        assertFalse(placeUnavailable.segments.any { assembled -> assembled.segment.id == "segment.gather" })
        assertEquals(
            ServiceCompositionAction.OMIT,
            placeUnavailable.compositionDecision("segment.gather").action,
        )
        assertEquals(ServiceReadinessStatus.BLOCKED, placeUnavailable.readiness.status)
        assertTrue(placeUnavailable.readiness.blockers.any { issue -> issue.id == "readiness.place.unavailable" })
    }

    @Test
    fun readinessRequiresEveryCurrentPreparationStepWithoutRequiringGovernanceAdoption() {
        val readyState = completeState(
            deviceUseMode = DeviceUseMode.DEVICE_PERMITTED,
            accessibility = ServiceAccessibilityProfile(participantNeedsReviewed = true),
        )
        val incompleteState = readyState.copy(
            completedPracticeStepIds = readyState.completedPracticeStepIds - "practice.offline.backup",
        )

        val blocked = assembleService(
            catalog,
            incompleteState,
            completeContext(incompleteState, CalendarRegion.ISRAEL),
        )
        val ready = assembleService(
            catalog,
            readyState,
            completeContext(readyState, CalendarRegion.ISRAEL),
        )

        assertEquals(ServiceReadinessStatus.BLOCKED, blocked.readiness.status)
        assertEquals(
            listOf("readiness.preparation.incomplete"),
            blocked.readiness.blockers.map { issue -> issue.id },
        )
        assertEquals(ServiceReadinessStatus.READY, ready.readiness.status)
        assertFalse(readyState.communityCharter.adopted)
        assertEquals(null, readyState.disputedPracticeAdoption.optionId)
    }

    @Test
    fun generatedPacketIncludesReadyContextGovernanceDossierAndAssignments() {
        val state = completeState(
            deviceUseMode = DeviceUseMode.PRINT_ONLY,
            accessibility = ServiceAccessibilityProfile(
                participantNeedsReviewed = true,
                useMovementAlternatives = true,
                useVisualVoiceCues = true,
                useLargeText = true,
            ),
        ).copy(
            completedServiceSegmentIds = setOf("segment.gather"),
            communityCharter = CommunityCharter(
                purpose = "Rehearse a participatory service",
                participants = "Members and guests",
                authorityLimits = "Local operations only",
                decisionProcess = "Consensus recorded by the coordinator",
                roleTerms = "Review each quarter",
                accessibilityCommitment = "Ask participants and prepare equal alternatives",
                effectiveDate = "2026-10-07",
                reviewDate = "2027-01-07",
                version = "Trial 2",
                adopted = true,
            ),
            disputedPracticeAdoption = CommunityAdoption(
                optionId = "adoption.lighting.prepared-precaution",
                scope = "Harimon rehearsal",
                effectiveDate = "2026-10-07",
                reviewDate = "2026-11-07",
                recordedBy = "Community coordinator",
            ),
            reviewedDossierFactIds = setOf("dossier.shabbat-electric-lighting.fact.1"),
        )
        val context = completeContext(state, CalendarRegion.ISRAEL)

        val packet = buildServicePacket(catalog, state, context)

        assertTrue(packet.contains(catalog.noticeBody))
        assertTrue(packet.contains("Calendar region: Israel"))
        assertTrue(packet.contains("Place: Jerusalem"))
        assertTrue(packet.contains("Workspace: Harimon rehearsal (Qahal)"))
        assertTrue(packet.contains("View: Prayer leader"))
        assertTrue(packet.contains("QAHAL CHARTER\nAdoption: Adopted for this workspace"))
        assertTrue(packet.contains("Version: Trial 2"))
        assertTrue(packet.contains("READINESS"))
        assertTrue(packet.contains("Status: READY"))
        assertTrue(packet.contains("Prayer leader: Ari"))
        assertTrue(packet.contains("Reader: Miriam"))
        assertTrue(packet.contains("Gabbai: Noam"))
        assertTrue(packet.contains("Host: Leah"))
        assertTrue(packet.contains("Device use: Print only"))
        assertTrue(packet.contains("First aliyah (Aliyah): Reader 1"))
        assertTrue(packet.contains("Second aliyah (Aliyah): Reader 2"))
        assertTrue(packet.contains("Maftir (Maftir): Reader 8"))
        assertTrue(packet.contains("Practice preparation: 12 of 12 complete"))
        assertTrue(packet.contains("Service preflight: 4 of 4 complete"))
        assertTrue(packet.contains("Movement reason: The accessibility profile requests movement alternatives."))
        assertTrue(packet.contains("Provenance: Original ASEH development text."))
        assertTrue(packet.contains("1. [x] Prepare"))
        assertTrue(packet.contains("Placement of the teaching pause: After the reading handoff"))
        assertTrue(packet.contains("DISPUTED-PRACTICE DOSSIER"))
        assertTrue(packet.contains("Evidence status: Unresolved"))
        assertTrue(packet.contains("Review state: Drafted"))
        assertTrue(packet.contains("Historical position: NOT ESTABLISHED"))
        assertTrue(packet.contains("[x] Which lights, switches, timers, sensors"))
        assertTrue(packet.contains("Option: Prepared-lighting precaution"))
        assertTrue(packet.contains("Recorded scope: Harimon rehearsal"))
        assertTrue(packet.contains("Recorded by: Community coordinator"))
        assertTrue(packet.contains("Adoption does not change the dossier's Unresolved evidence status."))
        assertTrue(packet.contains("No prayer text is included in this installed catalog."))
        assertTrue(packet.contains("Source text: UNAVAILABLE — No liturgical text is bundled"))
        assertTrue(packet.contains("Edition ID: NOT SELECTED — LITURGY-001 open"))
        assertTrue(packet.contains("License: NOT ESTABLISHED — distribution blocked"))
        assertTrue(packet.contains("Portion: Synthetic local portion"))
        assertTrue(packet.contains("Locator: User-provided rehearsal plan"))
        assertTrue(packet.contains("Range: Demo sections 1–8"))
        assertTrue(packet.contains("Backup: Backup 1"))
        assertTrue(packet.contains("Preparation: Ready"))
        assertTrue(packet.contains("Passage status: Manual local override; not verified by an installed source"))
        assertTrue(packet.contains("Passage status: Calendar-linked Torah portion data is not installed"))
        assertFalse(packet.contains("Open assignment"))
        assertFalse(packet.contains("Unassigned"))
    }

    private fun completeState(
        deviceUseMode: DeviceUseMode,
        accessibility: ServiceAccessibilityProfile,
    ) = ExperienceState(
        completedPracticeStepIds = catalog.practiceCards
            .flatMap { card -> card.steps }
            .map { step -> step.id }
            .toSet(),
        completedPreflightStepIds = catalog.service.preflightSteps.map { step -> step.id }.toSet(),
        selectedRole = ParticipantRole.LEADER,
        workspaceName = "Harimon rehearsal",
        workspaceKind = WorkspaceKind.QAHAL,
        roleAssignments = mapOf(
            ParticipantRole.LEADER to "Ari",
            ParticipantRole.READER to "Miriam",
            ParticipantRole.GABBAI to "Noam",
            ParticipantRole.HOST to "Leah",
        ),
        readingAssignments = catalog.service.readingSlots.associate { slot ->
            slot.id to "Reader ${slot.sequence}"
        },
        readingPlans = catalog.service.readingSlots.associate { slot ->
            slot.id to ReadingPlanEntry(
                portionTitle = if (slot.sequence == 1) "Synthetic local portion" else "",
                locator = if (slot.sequence == 1) "User-provided rehearsal plan" else "",
                passageRange = if (slot.sequence == 1) "Demo sections 1–8" else "",
                assignee = "Reader ${slot.sequence}",
                backupAssignee = "Backup ${slot.sequence}",
                preparationStatus = ReadingPreparationStatus.READY,
                manualOverride = slot.sequence == 1,
                overrideReason = if (slot.sequence == 1) "Synthetic test override" else "",
            )
        },
        deviceUseMode = deviceUseMode,
        accessibilityProfile = accessibility,
        selectedCommunityOptionId = "choice.teaching.after-reading",
    )

    private fun completeContext(
        state: ExperienceState,
        calendarRegion: CalendarRegion,
    ): ServiceAssemblyContext = serviceAssemblyContext(
        catalog = catalog,
        state = state,
        date = ServiceDateContext(
            civilDate = LocalDate.of(2026, 10, 10),
            displayLabel = "Saturday, October 10",
            dayKind = ServiceDayKind.SHABBAT,
        ),
        calendarRegion = calendarRegion,
        place = ServicePlaceAvailability.Available(
            label = "Jerusalem",
            timeZoneId = "Asia/Jerusalem",
        ),
        accessibility = state.accessibilityProfile,
    )

    private fun ServiceAssembly.compositionDecision(segmentId: String) =
        compositionDecisions.single { decision ->
            decision.targetSegmentId == segmentId && decision.action != ServiceCompositionAction.REORDER
        }

}
