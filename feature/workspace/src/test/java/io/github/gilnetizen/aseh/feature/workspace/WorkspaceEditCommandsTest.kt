package io.github.gilnetizen.aseh.feature.workspace

import io.github.gilnetizen.aseh.domain.workspace.CalendarItemStatus
import io.github.gilnetizen.aseh.domain.workspace.DecisionClassification
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarItem
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarKind
import io.github.gilnetizen.aseh.domain.workspace.HouseholdResponsibility
import io.github.gilnetizen.aseh.domain.workspace.InventoryItem
import io.github.gilnetizen.aseh.domain.workspace.LocalPersonId
import io.github.gilnetizen.aseh.domain.workspace.LocalWorkspaceApi
import io.github.gilnetizen.aseh.domain.workspace.QahalDecision
import io.github.gilnetizen.aseh.domain.workspace.QahalDecisionStatus
import io.github.gilnetizen.aseh.domain.workspace.SyntheticWorkspaceFixtures
import io.github.gilnetizen.aseh.domain.workspace.WorkItemStatus
import io.github.gilnetizen.aseh.domain.workspace.VolunteerSlotStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceUpdateResult
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceEditCommandsTest {
    private val fixture = SyntheticWorkspaceFixtures.reviewScenario.snapshot
    private val asOf = LocalDate.parse("2026-10-07")

    @Test
    fun renameCommandsContainOnlyIdentityAndTrimmedLabel() {
        val self = fixture.selfWorkspaces.single()
        val household = fixture.households.single()
        val qahal = fixture.qahalWorkspaces.single()

        assertEquals(WorkspaceCommand.RenameSelf(self.id, "My practice"), WorkspaceEditCommands.rename(self, "  My practice  "))
        assertEquals(
            WorkspaceCommand.RenameHousehold(household.id, "Our home"),
            WorkspaceEditCommands.rename(household, "  Our home  "),
        )
        assertEquals(WorkspaceCommand.RenameQahal(qahal.id, "Local qahal"), WorkspaceEditCommands.rename(qahal, "  Local qahal  "))
    }

    @Test
    fun staleHouseholdEditorCommandPreservesNewerUnrelatedRepositoryState() {
        val household = fixture.households.single()
        val calendarItem = household.calendarItems.single()
        val responsibility = HouseholdResponsibility(
            id = WorkspaceRecordId("local.responsibility.test"),
            label = "Arrange a ride",
            dueOn = asOf.plusDays(2),
            status = WorkItemStatus.TODO,
            statusChangedOn = asOf,
        )
        val staleEditorCommand = WorkspaceEditCommands.putResponsibility(household, responsibility)
        val newerSnapshot = applied(
            fixture,
            WorkspaceCommand.ChangeCalendarItemStatus(
                household.id,
                calendarItem.id,
                CalendarItemStatus.COMPLETED,
                asOf.plusDays(3),
            ),
        )

        val result = applied(newerSnapshot, staleEditorCommand)

        assertEquals(CalendarItemStatus.COMPLETED, result.households.single().calendarItems.single().status)
        assertEquals(responsibility, result.households.single().responsibilities.last())
        assertTrue(WorkspaceEditCommands.isApplied(result, staleEditorCommand))
    }

    @Test
    fun householdAndQahalRecordCommandsCreateEditAndDeleteExactRecords() {
        val household = fixture.households.single()
        val qahal = fixture.qahalWorkspaces.single()
        val calendar = HouseholdCalendarItem(
            id = WorkspaceRecordId("local.calendar.test"),
            label = "Family check-in",
            kind = HouseholdCalendarKind.HOUSEHOLD_MEETING,
            startsOn = asOf.plusDays(3),
            statusChangedOn = asOf,
        )
        val decision = QahalDecision(
            id = WorkspaceRecordId("local.decision.test"),
            label = "Meeting schedule",
            classification = DecisionClassification.POLICY,
            publicSummary = "Meet once each month.",
            authorityScope = "This local qahal only",
            statusChangedOn = asOf,
        )
        val inventory = InventoryItem(
            id = WorkspaceRecordId("local.inventory.test"),
            label = "Folding tables",
            quantityOnHand = 4,
            minimumDesired = 2,
            statusChangedOn = asOf,
        )
        var snapshot = applied(fixture, WorkspaceEditCommands.putCalendarItem(household, calendar))
        snapshot = applied(snapshot, WorkspaceEditCommands.putDecision(qahal, decision))
        snapshot = applied(snapshot, WorkspaceEditCommands.putInventoryItem(qahal, inventory))
        val editedInventory = inventory.copy(quantityOnHand = 5)
        snapshot = applied(snapshot, WorkspaceEditCommands.putInventoryItem(qahal, editedInventory))
        val deleteCalendar = WorkspaceEditCommands.deleteCalendarItem(household, calendar.id)
        val deleteDecision = WorkspaceEditCommands.deleteDecision(qahal, decision.id)
        val deleteInventory = WorkspaceEditCommands.deleteInventoryItem(qahal, inventory.id)
        snapshot = applied(snapshot, deleteCalendar)
        snapshot = applied(snapshot, deleteDecision)
        snapshot = applied(snapshot, deleteInventory)

        assertTrue(snapshot.households.single().calendarItems.none { it.id == calendar.id })
        assertTrue(snapshot.qahalWorkspaces.single().decisions.none { it.id == decision.id })
        assertTrue(snapshot.qahalWorkspaces.single().inventory.none { it.id == inventory.id })
        assertTrue(WorkspaceEditCommands.isApplied(snapshot, deleteCalendar))
        assertTrue(WorkspaceEditCommands.isApplied(snapshot, deleteDecision))
        assertTrue(WorkspaceEditCommands.isApplied(snapshot, deleteInventory))
        assertFalse(WorkspaceEditCommands.isApplied(fixture, WorkspaceEditCommands.putInventoryItem(qahal, inventory)))
    }

    @Test
    fun staleVolunteerEditorPreservesConcurrentUnassignmentAndStillCompletesSave() {
        val qahal = fixture.qahalWorkspaces.single()
        val rotation = qahal.volunteerRotations.single()
        val original = rotation.slots.first { it.status == VolunteerSlotStatus.ASSIGNED }
        val staleEditorCommand = WorkspaceEditCommands.putVolunteerSlot(
            qahal,
            rotation,
            original.copy(serviceOn = original.serviceOn.plusDays(7)),
        )
        val unassigned = applied(
            fixture,
            WorkspaceCommand.ChangeVolunteerSlotStatus(
                qahalId = qahal.id,
                rotationId = rotation.id,
                id = original.id,
                target = VolunteerSlotStatus.OPEN,
                changedOn = asOf.plusDays(1),
                assignedTo = null,
            ),
        )

        val result = applied(unassigned, staleEditorCommand)
        val saved = result.qahalWorkspaces.single().volunteerRotations.single()
            .slots.single { it.id == original.id }

        assertEquals(original.serviceOn.plusDays(7), saved.serviceOn)
        assertEquals(VolunteerSlotStatus.OPEN, saved.status)
        assertEquals(null, saved.assignedTo)
        assertTrue(WorkspaceEditCommands.isApplied(result, staleEditorCommand))
    }

    @Test
    fun staleDecisionEditorPreservesConcurrentAdoptionDateAndStillCompletesSave() {
        val qahal = fixture.qahalWorkspaces.single()
        val original = qahal.decisions.single().copy(
            status = QahalDecisionStatus.DRAFT,
            effectiveOn = null,
            reviewOn = null,
            statusChangedOn = asOf,
            dissentSummary = null,
        )
        val draftSnapshot = fixture.copy(
            qahalWorkspaces = listOf(qahal.copy(decisions = listOf(original))),
        )
        val staleEditorCommand = WorkspaceEditCommands.putDecision(
            qahal,
            original.copy(publicSummary = "Edited public summary"),
        )
        val adopted = applied(
            draftSnapshot,
            WorkspaceCommand.ChangeDecisionStatus(
                qahalId = qahal.id,
                id = original.id,
                target = QahalDecisionStatus.ADOPTED,
                changedOn = asOf.plusDays(1),
                nextReviewOn = asOf.plusMonths(6),
            ),
        )

        val result = applied(adopted, staleEditorCommand)
        val saved = result.qahalWorkspaces.single().decisions.single()

        assertEquals("Edited public summary", saved.publicSummary)
        assertEquals(QahalDecisionStatus.ADOPTED, saved.status)
        assertEquals(asOf.plusDays(1), saved.effectiveOn)
        assertTrue(WorkspaceEditCommands.isApplied(result, staleEditorCommand))
    }

    private fun applied(snapshot: WorkspaceSnapshot, command: WorkspaceCommand): WorkspaceSnapshot =
        (LocalWorkspaceApi.apply(snapshot, command) as WorkspaceUpdateResult.Applied).snapshot
}
