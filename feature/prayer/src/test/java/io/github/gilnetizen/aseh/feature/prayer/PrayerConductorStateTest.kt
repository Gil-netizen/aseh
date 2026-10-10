package io.github.gilnetizen.aseh.feature.prayer

import io.github.gilnetizen.aseh.core.model.AssembledServiceSegment
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ExplanationTrace
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ReadingSlot
import io.github.gilnetizen.aseh.core.model.ReadingSlotKind
import io.github.gilnetizen.aseh.core.model.ServiceReadiness
import io.github.gilnetizen.aseh.core.model.ServiceReadinessStatus
import io.github.gilnetizen.aseh.core.model.ServiceSegment
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceScheduleSelector
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionRequest
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionResult
import io.github.gilnetizen.aseh.domain.servicecatalog.SyntheticDevelopmentServiceCatalog
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerConductorStateTest {
    @Test
    fun initialSegmentSelectsTheFirstIncompleteStep() {
        val segments = listOf(segment("first"), segment("second"), segment("third"))

        assertEquals(
            1,
            initialSegmentIndex(
                segments = segments,
                completedSegmentIds = setOf("first", "unknown"),
            ),
        )
    }

    @Test
    fun fullyCompletedAndEmptyServicesHaveAStableStartingIndex() {
        val segments = listOf(segment("first"), segment("second"))

        assertEquals(
            0,
            initialSegmentIndex(
                segments = segments,
                completedSegmentIds = setOf("first", "second"),
            ),
        )
        assertEquals(
            0,
            initialSegmentIndex(
                segments = emptyList(),
                completedSegmentIds = emptySet(),
            ),
        )
    }

    @Test
    fun previousAndNextStayInsideTheRunningOrder() {
        assertEquals(0, boundedSegmentIndex(currentIndex = 0, change = -1, segmentCount = 3))
        assertEquals(1, boundedSegmentIndex(currentIndex = 0, change = 1, segmentCount = 3))
        assertEquals(2, boundedSegmentIndex(currentIndex = 2, change = 1, segmentCount = 3))
        assertEquals(0, boundedSegmentIndex(currentIndex = 7, change = 1, segmentCount = 0))
    }

    @Test
    fun rehearsalEntryReflectsPersistentCompletionState() {
        val segments = listOf(segment("first"), segment("second"), segment("third"))

        assertEquals(
            RehearsalEntryAction.START,
            rehearsalEntryAction(segments, completedSegmentIds = emptySet()),
        )
        assertEquals(
            RehearsalEntryAction.RESUME,
            rehearsalEntryAction(segments, completedSegmentIds = setOf("first", "unknown")),
        )
        assertEquals(
            RehearsalEntryAction.REVIEW,
            rehearsalEntryAction(segments, completedSegmentIds = setOf("first", "second", "third")),
        )
    }

    @Test
    fun resumeSelectsFirstUnfinishedEvenWhenLaterStepsAreComplete() {
        val segments = listOf(segment("first"), segment("second"), segment("third"))

        assertEquals(
            1,
            initialSegmentIndex(
                segments = segments,
                completedSegmentIds = setOf("first", "third"),
            ),
        )
    }

    @Test
    fun assembledProgressExcludesSegmentsOmittedFromThisService() {
        val assembled = listOf(assembledSegment("segment.opening"), assembledSegment("segment.close"))

        assertEquals(
            1 to 2,
            assembledServiceProgress(
                assembledSegments = assembled,
                completedSegmentIds = setOf("segment.opening", "segment.teaching"),
            ),
        )
    }

    @Test
    fun focusedConductorRequiresReadinessOrExplicitOverride() {
        assertEquals(
            false,
            canEnterFocusedConductor(
                ServiceReadiness(
                    status = ServiceReadinessStatus.BLOCKED,
                    issues = emptyList(),
                    overrideReason = null,
                ),
            ),
        )
        assertEquals(
            true,
            canEnterFocusedConductor(
                ServiceReadiness(
                    status = ServiceReadinessStatus.READY,
                    issues = emptyList(),
                    overrideReason = null,
                ),
            ),
        )
        assertEquals(
            true,
            canEnterFocusedConductor(
                ServiceReadiness(
                    status = ServiceReadinessStatus.OVERRIDDEN_FOR_REHEARSAL,
                    issues = emptyList(),
                    overrideReason = "Explicit local preview",
                ),
            ),
        )
    }

    @Test
    fun roleHandoffsOnlyUseSegmentsInTheAssembledRunningOrder() {
        val assembled = listOf(
            assembledSegment("segment.reading"),
            assembledSegment("segment.close"),
            assembledSegment("segment.unrelated"),
        )

        assertEquals(
            listOf("segment.reading"),
            assembledRoleHandoffs(assembled, ParticipantRole.READER)
                .map { it.segment.id },
        )
    }

    @Test
    fun openReadingCountUsesCanonicalReadingPlanWithLegacyCompatibility() {
        val slots = listOf(readingSlot("reading.1"), readingSlot("reading.2"))
        val state = ExperienceState(
            readingAssignments = mapOf("reading.1" to "Legacy reader"),
            readingPlans = mapOf(
                "reading.2" to ReadingPlanEntry(assignee = "Planned reader"),
            ),
        )

        assertEquals(0, openReadingSlotCount(slots, state))
    }

    @Test
    fun selectedServiceRemainsVisibleBeyondTheChronologicalPreview() {
        val selection = ServiceScheduleSelector(SyntheticDevelopmentServiceCatalog.catalog)
            .select(
                ServiceSelectionRequest(
                    now = Instant.parse("2026-10-04T00:00:00Z"),
                    zoneId = ZoneId.of("Asia/Jerusalem"),
                    opinionProfileId = SyntheticDevelopmentServiceCatalog.ISRAEL_PROFILE_ID,
                ),
            ) as ServiceSelectionResult.Available
        val selected = selection.agenda.upcomingServices.first { service ->
            service.definition.id == "dev.service.shabbat.morning"
        }

        val visible = visibleScheduledServices(
            upcomingServices = selection.agenda.upcomingServices,
            selectedServiceId = selected.id,
        )

        assertEquals(13, visible.size)
        assertTrue(selected in visible)
    }

    private fun assembledSegment(id: String): AssembledServiceSegment =
        AssembledServiceSegment(
            segment = segment(id),
            sourceReferences = emptyList(),
        )

    private fun readingSlot(id: String): ReadingSlot = ReadingSlot(
        id = id,
        sequence = id.substringAfterLast('.').toInt(),
        label = "Synthetic reading",
        kind = ReadingSlotKind.ALIYAH,
        description = "Synthetic assignment slot",
    )

    private fun segment(id: String): ServiceSegment =
        ServiceSegment(
            id = id,
            phase = "Synthetic phase",
            title = "Synthetic title",
            summary = "Synthetic summary",
            movementCue = "Synthetic movement cue",
            accessibleAlternative = "Synthetic accessible alternative",
            voiceCue = "Synthetic voice cue",
            roleCues = emptyMap(),
            explanation = ExplanationTrace(
                result = "Synthetic result",
                facts = emptyList(),
                rule = "Synthetic rule",
                sourceIds = emptyList(),
            ),
        )
}
