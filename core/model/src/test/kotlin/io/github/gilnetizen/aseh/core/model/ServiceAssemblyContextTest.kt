package io.github.gilnetizen.aseh.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceAssemblyContextTest {
    @Test
    fun `unrecorded communal decisions remain explicit safe defaults`() {
        val context = completeContext()

        assertEquals(ServiceCommunalSetting.UNSPECIFIED, context.communalSetting)
        assertEquals(ServiceQuorumStatus.NOT_RECORDED, context.quorum.status)
        assertEquals(null, context.practiceProfile.profileId)
        assertFalse(context.practiceProfile.adopted)
        assertEquals("Not established", context.seasonal.seasonLabel)
        assertFalse(context.seasonal.established)
        assertTrue(context.seasonal.additions.isEmpty())
    }

    @Test
    fun `complete typed context is ready`() {
        val readiness = evaluateServiceReadiness(
            expectedDayKind = ServiceDayKind.SHABBAT,
            context = completeContext(),
        )

        assertEquals(ServiceReadinessStatus.READY, readiness.status)
        assertTrue(readiness.canBeginRehearsal)
        assertTrue(readiness.blockers.isEmpty())
        assertEquals(
            listOf("readiness.readings.preparation"),
            readiness.warnings.map(ServiceReadinessIssue::id),
        )
    }

    @Test
    fun `missing context preparation roles and readings are visible blockers`() {
        val context = ServiceAssemblyContext(
            date = ServiceDateContext(),
            calendarRegion = CalendarRegion.UNSPECIFIED,
            place = ServicePlaceAvailability.Unavailable("Device location has not been granted."),
            preparation = ServicePreparationProgress(
                requiredStepIds = listOf("practice.team", "practice.access"),
                completedStepIds = setOf("practice.team", "stale.step"),
            ),
            preflight = ServicePreflightProgress(
                requiredStepIds = listOf("preflight.roles", "preflight.room"),
                completedStepIds = setOf("preflight.room", "stale.step"),
            ),
            roles = RequiredRoleAssignments(
                requiredRoles = setOf(ParticipantRole.LEADER, ParticipantRole.READER),
                assignments = mapOf(ParticipantRole.LEADER to ""),
            ),
            readings = ServiceReadingAssignments(
                assignments = readingAssignments(count = 7, assigned = false),
            ),
            deviceUseMode = DeviceUseMode.PRINT_ONLY,
            accessibility = ServiceAccessibilityProfile(),
        )

        val readiness = evaluateServiceReadiness(ServiceDayKind.SHABBAT, context)
        val issueIds = readiness.issues.map(ServiceReadinessIssue::id)

        assertEquals(ServiceReadinessStatus.BLOCKED, readiness.status)
        assertFalse(readiness.canBeginRehearsal)
        assertTrue("readiness.date.unavailable" in issueIds)
        assertTrue("readiness.date.day-kind" in issueIds)
        assertTrue("readiness.calendar-region.unselected" in issueIds)
        assertTrue("readiness.place.unavailable" in issueIds)
        assertTrue("readiness.preparation.incomplete" in issueIds)
        assertTrue("readiness.preflight.incomplete" in issueIds)
        assertTrue("readiness.role.leader" in issueIds)
        assertTrue("readiness.role.reader" in issueIds)
        assertTrue("readiness.readings.count" in issueIds)
        assertTrue("readiness.readings.unassigned" in issueIds)
        assertTrue("readiness.device.print-only" in issueIds)
        assertTrue("readiness.accessibility.unreviewed" in issueIds)
    }

    @Test
    fun `explicit rehearsal override preserves blockers and permits rehearsal`() {
        val blocked = completeContext().copy(
            place = ServicePlaceAvailability.Unavailable("GPS unavailable."),
            rehearsalOverride = RehearsalReadinessOverride.Proceed(
                "Tabletop rehearsal with an explicitly stated placeholder place.",
            ),
        )

        val readiness = evaluateServiceReadiness(ServiceDayKind.SHABBAT, blocked)

        assertEquals(ServiceReadinessStatus.OVERRIDDEN_FOR_REHEARSAL, readiness.status)
        assertTrue(readiness.canBeginRehearsal)
        assertEquals(1, readiness.blockers.size)
        assertEquals("readiness.place.unavailable", readiness.blockers.single().id)
        assertEquals(
            "Tabletop rehearsal with an explicitly stated placeholder place.",
            readiness.overrideReason,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rehearsal override cannot hide its reason`() {
        RehearsalReadinessOverride.Proceed("  ")
    }

    @Test
    fun `duplicate reading slots and order are deterministic blockers`() {
        val duplicate = ServiceReadingAssignment(
            slotId = "reading.1",
            sequence = 1,
            label = "Duplicate first aliyah",
            kind = ReadingSlotKind.ALIYAH,
            assignee = "Second reader",
        )
        val context = completeContext().copy(
            readings = ServiceReadingAssignments(
                assignments = readingAssignments(count = 7, assigned = true) + duplicate,
            ),
        )

        val readiness = evaluateServiceReadiness(ServiceDayKind.SHABBAT, context)
        val issueIds = readiness.blockers.map(ServiceReadinessIssue::id)

        assertTrue("readiness.readings.duplicate-slots" in issueIds)
        assertTrue("readiness.readings.duplicate-sequences" in issueIds)
    }

    private fun completeContext() = ServiceAssemblyContext(
        date = ServiceDateContext(
            civilDate = LocalDate.of(2026, 10, 10),
            displayLabel = "Saturday, October 10",
            dayKind = ServiceDayKind.SHABBAT,
        ),
        calendarRegion = CalendarRegion.ISRAEL,
        place = ServicePlaceAvailability.Available(
            label = "Jerusalem",
            timeZoneId = "Asia/Jerusalem",
        ),
        preparation = ServicePreparationProgress(
            requiredStepIds = listOf("practice.team", "practice.access"),
            completedStepIds = setOf("practice.team", "practice.access"),
        ),
        preflight = ServicePreflightProgress(
            requiredStepIds = listOf("preflight.roles", "preflight.room"),
            completedStepIds = setOf("preflight.roles", "preflight.room"),
        ),
        roles = RequiredRoleAssignments(
            requiredRoles = setOf(
                ParticipantRole.LEADER,
                ParticipantRole.READER,
                ParticipantRole.GABBAI,
            ),
            assignments = mapOf(
                ParticipantRole.LEADER to "Leader",
                ParticipantRole.READER to "Reader",
                ParticipantRole.GABBAI to "Gabbai",
            ),
        ),
        readings = ServiceReadingAssignments(
            assignments = readingAssignments(count = SHABBAT_READING_ASSIGNMENT_COUNT, assigned = true),
        ),
        deviceUseMode = DeviceUseMode.DEVICE_PERMITTED,
        accessibility = ServiceAccessibilityProfile(
            participantNeedsReviewed = true,
        ),
    )

    private fun readingAssignments(
        count: Int,
        assigned: Boolean,
    ): List<ServiceReadingAssignment> = (1..count).map { sequence ->
        ServiceReadingAssignment(
            slotId = "reading.$sequence",
            sequence = sequence,
            label = if (sequence == SHABBAT_READING_ASSIGNMENT_COUNT) {
                "Maftir"
            } else {
                "Aliyah $sequence"
            },
            kind = if (sequence == SHABBAT_READING_ASSIGNMENT_COUNT) {
                ReadingSlotKind.MAFTIR
            } else {
                ReadingSlotKind.ALIYAH
            },
            assignee = if (assigned) "Reader $sequence" else null,
        )
    }
}
