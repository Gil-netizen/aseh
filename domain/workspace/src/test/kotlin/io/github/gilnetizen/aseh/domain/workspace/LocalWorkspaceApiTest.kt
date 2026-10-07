package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWorkspaceApiTest {
    private val snapshot = SyntheticWorkspaceFixtures.reviewScenario.snapshot

    @Test
    fun putAddsOrReplacesByStableIdAndRejectsInvalidAggregate() {
        val self = snapshot.selfWorkspaces.single()
        val newDraft = self.practiceAdoptions.single().copy(
            id = WorkspaceRecordId("dev.self.practice.second"),
            label = "Synthetic second draft",
            status = PracticeAdoptionStatus.DRAFT,
            adoptedOn = null,
            reviewOn = null,
            statusChangedOn = date("2026-10-07"),
        )
        val added = LocalWorkspaceApi.apply(
            snapshot,
            WorkspaceCommand.PutPersonalPractice(self.id, newDraft),
        ).applied()
        val replaced = LocalWorkspaceApi.apply(
            added,
            WorkspaceCommand.PutPersonalPractice(self.id, newDraft.copy(label = "Synthetic renamed draft")),
        ).applied()
        val invalid = LocalWorkspaceApi.apply(
            replaced,
            WorkspaceCommand.PutPersonalPractice(self.id, newDraft.copy(label = "")),
        ).rejected()

        assertEquals(2, added.selfWorkspaces.single().practiceAdoptions.size)
        assertEquals(2, replaced.selfWorkspaces.single().practiceAdoptions.size)
        assertEquals("Synthetic renamed draft", replaced.selfWorkspaces.single().practiceAdoptions.last().label)
        assertSame(replaced, invalid.unchangedSnapshot)
        assertTrue(invalid.issues.any { it.code == WorkspaceIssueCode.REQUIRED_FIELD })
    }

    @Test
    fun nestedHouseholdCommandsReturnNewStateWithoutMutatingInput() {
        val household = snapshot.households.single()
        val responsibility = household.responsibilities.single()
        val updated = LocalWorkspaceApi.apply(
            snapshot,
            WorkspaceCommand.ChangeResponsibilityStatus(
                householdId = household.id,
                id = responsibility.id,
                target = WorkItemStatus.DONE,
                changedOn = date("2026-10-07"),
            ),
        ).applied()

        assertEquals(WorkItemStatus.IN_PROGRESS, snapshot.households.single().responsibilities.single().status)
        assertEquals(WorkItemStatus.DONE, updated.households.single().responsibilities.single().status)
        assertNotEquals(snapshot, updated)
    }

    @Test
    fun completePreparationFlowUsesOnlyExplicitCommandsAndDates() {
        val household = snapshot.households.single()
        val kit = household.preparationKits.single()
        val openTask = kit.tasks.single { it.status != WorkItemStatus.DONE }
        val taskDone = LocalWorkspaceApi.apply(
            snapshot,
            WorkspaceCommand.ChangePreparationTaskStatus(
                householdId = household.id,
                kitId = kit.id,
                id = openTask.id,
                target = WorkItemStatus.DONE,
                changedOn = date("2026-10-07"),
            ),
        ).applied()
        val completed = LocalWorkspaceApi.apply(
            taskDone,
            WorkspaceCommand.ChangePreparationKitStatus(
                householdId = household.id,
                id = kit.id,
                target = PreparationKitStatus.COMPLETED,
                changedOn = date("2026-10-07"),
            ),
        ).applied()

        assertEquals(PreparationKitStatus.COMPLETED, completed.households.single().preparationKits.single().status)
        assertTrue(completed.households.single().preparationKits.single().tasks.all { it.status == WorkItemStatus.DONE })
    }

    @Test
    fun qahalControlAndVolunteerUpdatesAreAddressedByParentIds() {
        val qahal = snapshot.qahalWorkspaces.single()
        val rotation = qahal.volunteerRotations.single()
        val openSlot = rotation.slots.single { it.status == VolunteerSlotStatus.OPEN }
        val checklist = qahal.financialControls.single()
        val pendingControl = checklist.controls.single { it.status == FinancialControlStatus.PENDING }
        val assigned = LocalWorkspaceApi.apply(
            snapshot,
            WorkspaceCommand.ChangeVolunteerSlotStatus(
                qahalId = qahal.id,
                rotationId = rotation.id,
                id = openSlot.id,
                target = VolunteerSlotStatus.ASSIGNED,
                changedOn = date("2026-10-07"),
                assignedTo = LocalPersonId("dev.person.new-volunteer"),
            ),
        ).applied()
        val verified = LocalWorkspaceApi.apply(
            assigned,
            WorkspaceCommand.ChangeFinancialControlStatus(
                qahalId = qahal.id,
                checklistId = checklist.id,
                id = pendingControl.id,
                target = FinancialControlStatus.VERIFIED,
                changedOn = date("2026-10-08"),
            ),
        ).applied()

        val changedQahal = verified.qahalWorkspaces.single()
        assertEquals(
            VolunteerSlotStatus.ASSIGNED,
            changedQahal.volunteerRotations.single().slots.first { it.id == openSlot.id }.status,
        )
        assertEquals(
            FinancialControlStatus.VERIFIED,
            changedQahal.financialControls.single().controls.first { it.id == pendingControl.id }.status,
        )
    }

    @Test
    fun missingRecordRejectsAndReturnsOriginalSnapshot() {
        val result = LocalWorkspaceApi.apply(
            snapshot,
            WorkspaceCommand.ChangeInventoryStatus(
                qahalId = WorkspaceId("missing-qahal"),
                id = WorkspaceRecordId("missing-item"),
                target = InventoryItemStatus.RETIRED,
                changedOn = date("2026-10-07"),
            ),
        ).rejected()

        assertSame(snapshot, result.unchangedSnapshot)
        assertEquals(WorkspaceIssueCode.RECORD_NOT_FOUND, result.issues.single().code)
        assertEquals("qahalWorkspaces", result.issues.single().path)
    }

    @Test
    fun sameSnapshotCommandAndDateAlwaysProduceEqualResult() {
        val household = snapshot.households.single()
        val command = WorkspaceCommand.ChangeCalendarItemStatus(
            householdId = household.id,
            id = household.calendarItems.single().id,
            target = CalendarItemStatus.COMPLETED,
            changedOn = date("2026-10-10"),
        )

        assertEquals(LocalWorkspaceApi.apply(snapshot, command), LocalWorkspaceApi.apply(snapshot, command))
        assertEquals(
            LocalWorkspaceApi.dueSoon(snapshot, date("2026-10-07"), 7),
            LocalWorkspaceApi.dueSoon(snapshot, date("2026-10-07"), 7),
        )
    }

    private fun date(value: String): LocalDate = LocalDate.parse(value)

    private fun WorkspaceUpdateResult.applied(): WorkspaceSnapshot =
        (this as WorkspaceUpdateResult.Applied).snapshot

    private fun WorkspaceUpdateResult.rejected(): WorkspaceUpdateResult.Rejected =
        this as WorkspaceUpdateResult.Rejected
}
