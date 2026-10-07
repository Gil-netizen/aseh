package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceDueSoonTest {
    private val snapshot = SyntheticWorkspaceFixtures.reviewScenario.snapshot
    private val asOf = LocalDate.parse("2026-10-07")

    @Test
    fun summaryCoversAllThreeWorkspacesAndSortsDeterministically() {
        val summary = WorkspaceDueSoon.summarize(snapshot, asOf, 7).success()

        assertEquals(asOf, summary.asOf)
        assertEquals(LocalDate.parse("2026-10-14"), summary.through)
        assertEquals(11, summary.items.size)
        assertEquals(1, summary.overdueCount)
        assertEquals(2, summary.todayCount)
        assertEquals(8, summary.upcomingCount)
        assertEquals(setOf(WorkspaceArea.SELF, WorkspaceArea.HOUSEHOLD, WorkspaceArea.QAHAL), summary.items.map { it.area }.toSet())
        assertEquals(
            setOf(
                DueSoonKind.PRACTICE_REVIEW,
                DueSoonKind.HOUSEHOLD_RESPONSIBILITY,
                DueSoonKind.HOUSEHOLD_CALENDAR_ITEM,
                DueSoonKind.PREPARATION_KIT_TARGET,
                DueSoonKind.PREPARATION_KIT_TASK,
                DueSoonKind.QAHAL_DECISION_REVIEW,
                DueSoonKind.VOLUNTEER_SLOT,
                DueSoonKind.INVENTORY_ATTENTION,
                DueSoonKind.FINANCIAL_CONTROL,
                DueSoonKind.FINANCIAL_CHECKLIST_REVIEW,
            ),
            summary.items.map(DueSoonItem::kind).toSet(),
        )
        assertEquals(summary.items.map(DueSoonItem::dueOn).sorted(), summary.items.map(DueSoonItem::dueOn))
        assertEquals(summary, WorkspaceDueSoon.summarize(snapshot, asOf, 7).success())
    }

    @Test
    fun todayWindowIncludesOverdueAndTodayButNoFutureItems() {
        val summary = WorkspaceDueSoon.summarize(snapshot, asOf, 0).success()

        assertEquals(3, summary.items.size)
        assertEquals(setOf(DueTiming.OVERDUE, DueTiming.TODAY), summary.items.map { it.timing }.toSet())
        assertTrue(summary.items.all { it.dueOn <= asOf })
    }

    @Test
    fun resolvedCancelledAndRetiredRecordsDoNotAppear() {
        val household = snapshot.households.single()
        val qahal = snapshot.qahalWorkspaces.single()
        val resolved = snapshot.copy(
            selfWorkspaces = snapshot.selfWorkspaces.map { self ->
                self.copy(
                    practiceAdoptions = self.practiceAdoptions.map { it.copy(status = PracticeAdoptionStatus.RETIRED) },
                )
            },
            households = listOf(
                household.copy(
                    responsibilities = household.responsibilities.map { it.copy(status = WorkItemStatus.DONE) },
                    calendarItems = household.calendarItems.map { it.copy(status = CalendarItemStatus.CANCELLED) },
                    preparationKits = household.preparationKits.map { it.copy(status = PreparationKitStatus.CANCELLED) },
                ),
            ),
            qahalWorkspaces = listOf(
                qahal.copy(
                    decisions = qahal.decisions.map { it.copy(status = QahalDecisionStatus.RETIRED) },
                    volunteerRotations = qahal.volunteerRotations.map { it.copy(status = VolunteerRotationStatus.CANCELLED) },
                    inventory = qahal.inventory.map { it.copy(status = InventoryItemStatus.RETIRED) },
                    financialControls = qahal.financialControls.map { it.copy(status = FinancialChecklistStatus.RETIRED) },
                ),
            ),
        )

        val summary = WorkspaceDueSoon.summarize(resolved, asOf, 30).success()

        assertTrue(summary.items.isEmpty())
    }

    @Test
    fun pendingControlWithoutDueDateDoesNotInventOne() {
        val qahal = snapshot.qahalWorkspaces.single()
        val checklist = qahal.financialControls.single()
        val noDate = snapshot.copy(
            qahalWorkspaces = listOf(
                qahal.copy(
                    financialControls = listOf(
                        checklist.copy(
                            reviewOn = asOf.plusDays(20),
                            controls = checklist.controls.map { it.copy(dueOn = null) },
                        ),
                    ),
                ),
            ),
        )

        val summary = WorkspaceDueSoon.summarize(noDate, asOf, 7).success()

        assertFalse(summary.items.any { it.kind == DueSoonKind.FINANCIAL_CONTROL })
        assertFalse(summary.items.any { it.kind == DueSoonKind.FINANCIAL_CHECKLIST_REVIEW })
    }

    @Test
    fun invalidHorizonAndInvalidStateFailClosed() {
        val invalidHorizon = WorkspaceDueSoon.summarize(snapshot, asOf, -1).rejected()
        val self = snapshot.selfWorkspaces.single()
        val invalidState = WorkspaceDueSoon.summarize(
            snapshot.copy(
                selfWorkspaces = listOf(
                    self.copy(practiceAdoptions = self.practiceAdoptions + self.practiceAdoptions.single()),
                ),
            ),
            asOf,
            7,
        ).rejected()

        assertEquals(WorkspaceIssueCode.INVALID_DATE_RANGE, invalidHorizon.issues.single().code)
        assertTrue(invalidState.issues.any { it.code == WorkspaceIssueCode.DUPLICATE_ID })
    }

    private fun WorkspaceOperationResult<DueSoonSummary>.success(): DueSoonSummary =
        (this as WorkspaceOperationResult.Success).value

    private fun WorkspaceOperationResult<*>.rejected(): WorkspaceOperationResult.Rejected =
        this as WorkspaceOperationResult.Rejected
}
