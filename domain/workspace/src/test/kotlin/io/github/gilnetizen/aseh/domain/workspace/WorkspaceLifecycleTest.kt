package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLifecycleTest {
    private val fixture = SyntheticWorkspaceFixtures.reviewScenario.snapshot

    @Test
    fun personalAdoptionIsExplicitReviewableAndReversible() {
        val adopted = fixture.selfWorkspaces.single().practiceAdoptions.single()
        val paused = WorkspaceLifecycle.transition(
            adopted,
            PracticeAdoptionStatus.PAUSED,
            date("2026-10-07"),
        ).success()
        val resumed = WorkspaceLifecycle.transition(
            paused,
            PracticeAdoptionStatus.ADOPTED,
            date("2026-10-08"),
            date("2027-01-01"),
        ).success()
        val retired = WorkspaceLifecycle.transition(
            resumed,
            PracticeAdoptionStatus.RETIRED,
            date("2026-10-09"),
        ).success()
        val restoredDraft = WorkspaceLifecycle.transition(
            retired,
            PracticeAdoptionStatus.DRAFT,
            date("2026-10-10"),
        ).success()

        assertEquals(PracticeAdoptionStatus.PAUSED, paused.status)
        assertEquals(adopted.adoptedOn, paused.adoptedOn)
        assertEquals(date("2027-01-01"), resumed.reviewOn)
        assertEquals(PracticeAdoptionStatus.RETIRED, retired.status)
        assertEquals(PracticeAdoptionStatus.DRAFT, restoredDraft.status)
        assertNull(restoredDraft.adoptedOn)
        assertNull(restoredDraft.reviewOn)
    }

    @Test
    fun adoptingPracticeWithoutReviewDateFailsClosed() {
        val draft = fixture.selfWorkspaces.single().practiceAdoptions.single().copy(
            status = PracticeAdoptionStatus.DRAFT,
            adoptedOn = null,
            reviewOn = null,
            statusChangedOn = date("2026-10-01"),
        )

        val result = WorkspaceLifecycle.transition(
            draft,
            PracticeAdoptionStatus.ADOPTED,
            date("2026-10-07"),
            nextReviewOn = null,
        ).rejected()

        assertTrue(result.issues.any { it.path == "personalPractice.reviewOn" })
    }

    @Test
    fun statusDatesCannotMoveBackwardAndNoOpIsRejected() {
        val responsibility = fixture.households.single().responsibilities.single()
        val backward = WorkspaceLifecycle.transition(
            responsibility,
            WorkItemStatus.DONE,
            responsibility.statusChangedOn.minusDays(1),
        ).rejected()
        val noOp = WorkspaceLifecycle.transition(
            responsibility,
            responsibility.status,
            responsibility.statusChangedOn.plusDays(1),
        ).rejected()

        assertEquals(WorkspaceIssueCode.NON_MONOTONIC_STATUS_DATE, backward.issues.single().code)
        assertEquals(WorkspaceIssueCode.NO_STATE_CHANGE, noOp.issues.single().code)
    }

    @Test
    fun preparationKitCannotCompleteWhileAnyTaskRemainsOpen() {
        val kit = fixture.households.single().preparationKits.single()

        val rejected = WorkspaceLifecycle.transition(
            kit,
            PreparationKitStatus.COMPLETED,
            date("2026-10-07"),
        ).rejected()
        val resolvedTasks = kit.tasks.map { task ->
            if (task.status == WorkItemStatus.DONE) task else task.copy(status = WorkItemStatus.DONE)
        }
        val completed = WorkspaceLifecycle.transition(
            kit.copy(tasks = resolvedTasks),
            PreparationKitStatus.COMPLETED,
            date("2026-10-07"),
        ).success()

        assertTrue(rejected.issues.any { it.code == WorkspaceIssueCode.STATUS_REQUIREMENT_NOT_MET })
        assertEquals(PreparationKitStatus.COMPLETED, completed.status)
    }

    @Test
    fun adoptedQahalDecisionCanBeSupersededButNotReactivated() {
        val decision = fixture.qahalWorkspaces.single().decisions.single()
        val superseded = WorkspaceLifecycle.transition(
            decision,
            QahalDecisionStatus.SUPERSEDED,
            date("2026-10-11"),
        ).success()
        val reactivation = WorkspaceLifecycle.transition(
            superseded,
            QahalDecisionStatus.ADOPTED,
            date("2026-10-12"),
        ).rejected()

        assertEquals(QahalDecisionStatus.SUPERSEDED, superseded.status)
        assertEquals(WorkspaceIssueCode.ILLEGAL_TRANSITION, reactivation.issues.single().code)
    }

    @Test
    fun qahalDecisionCannotBecomeAdoptedWithoutScopeSummaryAndReview() {
        val draft = fixture.qahalWorkspaces.single().decisions.single().copy(
            publicSummary = "",
            authorityScope = "",
            status = QahalDecisionStatus.DRAFT,
            effectiveOn = null,
            reviewOn = null,
            statusChangedOn = date("2026-10-01"),
        )

        val result = WorkspaceLifecycle.transition(
            draft,
            QahalDecisionStatus.ADOPTED,
            date("2026-10-07"),
            nextReviewOn = null,
        ).rejected()

        assertEquals(
            setOf("qahalDecision.publicSummary", "qahalDecision.authorityScope", "qahalDecision.reviewOn"),
            result.issues.map(WorkspaceIssue::path).toSet(),
        )
    }

    @Test
    fun volunteerAssignmentAndRotationCloseEnforceCompletion() {
        val rotation = fixture.qahalWorkspaces.single().volunteerRotations.single()
        val open = rotation.slots.first()
        val missingAssignee = WorkspaceLifecycle.transition(
            open,
            VolunteerSlotStatus.ASSIGNED,
            date("2026-10-07"),
        ).rejected()
        val assigned = WorkspaceLifecycle.transition(
            open,
            VolunteerSlotStatus.ASSIGNED,
            date("2026-10-07"),
            LocalPersonId("dev.person.assigned"),
        ).success()
        val cannotClose = WorkspaceLifecycle.transition(
            rotation,
            VolunteerRotationStatus.CLOSED,
            date("2026-10-10"),
        ).rejected()
        val resolvedRotation = rotation.copy(
            slots = rotation.slots.map { slot ->
                slot.copy(
                    assignedTo = slot.assignedTo ?: LocalPersonId("dev.person.assigned"),
                    status = VolunteerSlotStatus.COMPLETED,
                )
            },
        )
        val closed = WorkspaceLifecycle.transition(
            resolvedRotation,
            VolunteerRotationStatus.CLOSED,
            date("2026-10-10"),
        ).success()

        assertTrue(missingAssignee.issues.any { it.path == "volunteerSlot.assignedTo" })
        assertNotNull(assigned.assignedTo)
        assertTrue(cannotClose.issues.any { it.path == "volunteerRotation.status" })
        assertEquals(VolunteerRotationStatus.CLOSED, closed.status)
    }

    @Test
    fun financialChecklistCompletesOnlyAfterEveryProcedureIsAddressed() {
        val checklist = fixture.qahalWorkspaces.single().financialControls.single()

        val rejected = WorkspaceLifecycle.transition(
            checklist,
            FinancialChecklistStatus.COMPLETED,
            date("2026-10-14"),
        ).rejected()
        val addressed = checklist.copy(
            controls = checklist.controls.map { it.copy(status = FinancialControlStatus.VERIFIED) },
        )
        val completed = WorkspaceLifecycle.transition(
            addressed,
            FinancialChecklistStatus.COMPLETED,
            date("2026-10-14"),
        ).success()

        assertTrue(rejected.issues.any { it.path == "financialControlChecklist.status" })
        assertEquals(FinancialChecklistStatus.COMPLETED, completed.status)
    }

    private fun date(value: String): LocalDate = LocalDate.parse(value)

    private fun <T> WorkspaceOperationResult<T>.success(): T =
        (this as WorkspaceOperationResult.Success<T>).value

    private fun WorkspaceOperationResult<*>.rejected(): WorkspaceOperationResult.Rejected =
        this as WorkspaceOperationResult.Rejected
}
