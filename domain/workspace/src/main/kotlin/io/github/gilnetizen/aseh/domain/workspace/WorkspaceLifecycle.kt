package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate

sealed interface WorkspaceOperationResult<out T> {
    data class Success<T>(val value: T) : WorkspaceOperationResult<T>

    data class Rejected(val issues: List<WorkspaceIssue>) : WorkspaceOperationResult<Nothing>
}

/** Explicit lifecycle rules. No transition consults a clock or performs I/O. */
object WorkspaceLifecycle {
    fun transition(
        adoption: PersonalPracticeAdoption,
        target: PracticeAdoptionStatus,
        changedOn: LocalDate,
        nextReviewOn: LocalDate? = adoption.reviewOn,
    ): WorkspaceOperationResult<PersonalPracticeAdoption> {
        preflight(adoption.status, target, adoption.statusChangedOn, changedOn, PRACTICE_TRANSITIONS)?.let { return it }
        val candidate = adoption.copy(
            status = target,
            adoptedOn = when (target) {
                PracticeAdoptionStatus.DRAFT -> null
                PracticeAdoptionStatus.ADOPTED -> adoption.adoptedOn ?: changedOn
                PracticeAdoptionStatus.PAUSED,
                PracticeAdoptionStatus.RETIRED,
                -> adoption.adoptedOn
            },
            reviewOn = when (target) {
                PracticeAdoptionStatus.DRAFT -> null
                PracticeAdoptionStatus.ADOPTED,
                PracticeAdoptionStatus.PAUSED,
                -> nextReviewOn
                PracticeAdoptionStatus.RETIRED -> adoption.reviewOn
            },
            statusChangedOn = changedOn,
        )
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        responsibility: HouseholdResponsibility,
        target: WorkItemStatus,
        changedOn: LocalDate,
    ): WorkspaceOperationResult<HouseholdResponsibility> {
        preflight(responsibility.status, target, responsibility.statusChangedOn, changedOn, WORK_ITEM_TRANSITIONS)?.let {
            return it
        }
        val candidate = responsibility.copy(status = target, statusChangedOn = changedOn)
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        item: HouseholdCalendarItem,
        target: CalendarItemStatus,
        changedOn: LocalDate,
    ): WorkspaceOperationResult<HouseholdCalendarItem> {
        preflight(item.status, target, item.statusChangedOn, changedOn, CALENDAR_TRANSITIONS)?.let { return it }
        val candidate = item.copy(status = target, statusChangedOn = changedOn)
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        task: PreparationKitTask,
        target: WorkItemStatus,
        changedOn: LocalDate,
    ): WorkspaceOperationResult<PreparationKitTask> {
        preflight(task.status, target, task.statusChangedOn, changedOn, WORK_ITEM_TRANSITIONS)?.let { return it }
        val candidate = task.copy(status = target, statusChangedOn = changedOn)
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        kit: HouseholdPreparationKit,
        target: PreparationKitStatus,
        changedOn: LocalDate,
    ): WorkspaceOperationResult<HouseholdPreparationKit> {
        preflight(kit.status, target, kit.statusChangedOn, changedOn, PREPARATION_KIT_TRANSITIONS)?.let { return it }
        val candidate = kit.copy(status = target, statusChangedOn = changedOn)
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        decision: QahalDecision,
        target: QahalDecisionStatus,
        changedOn: LocalDate,
        nextReviewOn: LocalDate? = decision.reviewOn,
    ): WorkspaceOperationResult<QahalDecision> {
        preflight(decision.status, target, decision.statusChangedOn, changedOn, DECISION_TRANSITIONS)?.let { return it }
        val candidate = decision.copy(
            status = target,
            effectiveOn = if (target == QahalDecisionStatus.ADOPTED) changedOn else decision.effectiveOn,
            reviewOn = if (target == QahalDecisionStatus.ADOPTED) nextReviewOn else decision.reviewOn,
            statusChangedOn = changedOn,
        )
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        slot: VolunteerSlot,
        target: VolunteerSlotStatus,
        changedOn: LocalDate,
        assignedTo: LocalPersonId? = slot.assignedTo,
    ): WorkspaceOperationResult<VolunteerSlot> {
        preflight(slot.status, target, slot.statusChangedOn, changedOn, VOLUNTEER_SLOT_TRANSITIONS)?.let { return it }
        val candidate = slot.copy(
            status = target,
            assignedTo = if (target == VolunteerSlotStatus.OPEN) null else assignedTo,
            statusChangedOn = changedOn,
        )
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        rotation: VolunteerRotation,
        target: VolunteerRotationStatus,
        changedOn: LocalDate,
    ): WorkspaceOperationResult<VolunteerRotation> {
        preflight(rotation.status, target, rotation.statusChangedOn, changedOn, ROTATION_TRANSITIONS)?.let { return it }
        val candidate = rotation.copy(status = target, statusChangedOn = changedOn)
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        item: InventoryItem,
        target: InventoryItemStatus,
        changedOn: LocalDate,
    ): WorkspaceOperationResult<InventoryItem> {
        preflight(item.status, target, item.statusChangedOn, changedOn, INVENTORY_TRANSITIONS)?.let { return it }
        val candidate = item.copy(status = target, statusChangedOn = changedOn)
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        control: FinancialControlItem,
        target: FinancialControlStatus,
        changedOn: LocalDate,
    ): WorkspaceOperationResult<FinancialControlItem> {
        preflight(control.status, target, control.statusChangedOn, changedOn, FINANCIAL_CONTROL_TRANSITIONS)?.let {
            return it
        }
        val candidate = control.copy(status = target, statusChangedOn = changedOn)
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    fun transition(
        checklist: FinancialControlChecklist,
        target: FinancialChecklistStatus,
        changedOn: LocalDate,
        nextReviewOn: LocalDate? = checklist.reviewOn,
    ): WorkspaceOperationResult<FinancialControlChecklist> {
        preflight(checklist.status, target, checklist.statusChangedOn, changedOn, FINANCIAL_CHECKLIST_TRANSITIONS)?.let {
            return it
        }
        val candidate = checklist.copy(
            status = target,
            reviewOn = if (target == FinancialChecklistStatus.ACTIVE) nextReviewOn else checklist.reviewOn,
            statusChangedOn = changedOn,
        )
        return validated(candidate, WorkspaceValidator.validate(candidate))
    }

    private fun <T> validated(candidate: T, issues: List<WorkspaceIssue>): WorkspaceOperationResult<T> =
        if (issues.isEmpty()) WorkspaceOperationResult.Success(candidate) else WorkspaceOperationResult.Rejected(issues)

    private fun <S> preflight(
        current: S,
        target: S,
        currentChangedOn: LocalDate,
        changedOn: LocalDate,
        allowed: Map<S, Set<S>>,
    ): WorkspaceOperationResult.Rejected? {
        if (current == target) {
            return rejected(
                WorkspaceIssueCode.NO_STATE_CHANGE,
                "status",
                "The requested status is already active.",
            )
        }
        if (changedOn < currentChangedOn) {
            return rejected(
                WorkspaceIssueCode.NON_MONOTONIC_STATUS_DATE,
                "statusChangedOn",
                "A status change cannot predate the current status.",
            )
        }
        if (target !in allowed.getValue(current)) {
            return rejected(
                WorkspaceIssueCode.ILLEGAL_TRANSITION,
                "status",
                "Transition from $current to $target is not allowed.",
            )
        }
        return null
    }

    private fun rejected(
        code: WorkspaceIssueCode,
        path: String,
        detail: String,
    ): WorkspaceOperationResult.Rejected = WorkspaceOperationResult.Rejected(
        listOf(WorkspaceIssue(code, path, detail)),
    )

    private val PRACTICE_TRANSITIONS = mapOf(
        PracticeAdoptionStatus.DRAFT to setOf(PracticeAdoptionStatus.ADOPTED, PracticeAdoptionStatus.RETIRED),
        PracticeAdoptionStatus.ADOPTED to setOf(PracticeAdoptionStatus.PAUSED, PracticeAdoptionStatus.RETIRED),
        PracticeAdoptionStatus.PAUSED to setOf(PracticeAdoptionStatus.ADOPTED, PracticeAdoptionStatus.RETIRED),
        PracticeAdoptionStatus.RETIRED to setOf(PracticeAdoptionStatus.DRAFT),
    )

    private val WORK_ITEM_TRANSITIONS = mapOf(
        WorkItemStatus.TODO to setOf(WorkItemStatus.IN_PROGRESS, WorkItemStatus.DONE, WorkItemStatus.CANCELLED),
        WorkItemStatus.IN_PROGRESS to setOf(WorkItemStatus.TODO, WorkItemStatus.DONE, WorkItemStatus.CANCELLED),
        WorkItemStatus.DONE to setOf(WorkItemStatus.TODO),
        WorkItemStatus.CANCELLED to setOf(WorkItemStatus.TODO),
    )

    private val CALENDAR_TRANSITIONS = mapOf(
        CalendarItemStatus.DRAFT to setOf(CalendarItemStatus.SCHEDULED, CalendarItemStatus.CANCELLED),
        CalendarItemStatus.SCHEDULED to setOf(CalendarItemStatus.COMPLETED, CalendarItemStatus.CANCELLED),
        CalendarItemStatus.COMPLETED to setOf(CalendarItemStatus.DRAFT),
        CalendarItemStatus.CANCELLED to setOf(CalendarItemStatus.DRAFT),
    )

    private val PREPARATION_KIT_TRANSITIONS = mapOf(
        PreparationKitStatus.DRAFT to setOf(PreparationKitStatus.ACTIVE, PreparationKitStatus.CANCELLED),
        PreparationKitStatus.ACTIVE to setOf(PreparationKitStatus.COMPLETED, PreparationKitStatus.CANCELLED),
        PreparationKitStatus.COMPLETED to setOf(PreparationKitStatus.DRAFT),
        PreparationKitStatus.CANCELLED to setOf(PreparationKitStatus.DRAFT),
    )

    private val DECISION_TRANSITIONS = mapOf(
        QahalDecisionStatus.DRAFT to setOf(QahalDecisionStatus.ADOPTED, QahalDecisionStatus.RETIRED),
        QahalDecisionStatus.ADOPTED to setOf(QahalDecisionStatus.SUPERSEDED, QahalDecisionStatus.RETIRED),
        QahalDecisionStatus.SUPERSEDED to emptySet(),
        QahalDecisionStatus.RETIRED to emptySet(),
    )

    private val VOLUNTEER_SLOT_TRANSITIONS = mapOf(
        VolunteerSlotStatus.OPEN to setOf(VolunteerSlotStatus.ASSIGNED, VolunteerSlotStatus.CANCELLED),
        VolunteerSlotStatus.ASSIGNED to setOf(
            VolunteerSlotStatus.OPEN,
            VolunteerSlotStatus.COMPLETED,
            VolunteerSlotStatus.CANCELLED,
        ),
        VolunteerSlotStatus.COMPLETED to setOf(VolunteerSlotStatus.OPEN),
        VolunteerSlotStatus.CANCELLED to setOf(VolunteerSlotStatus.OPEN),
    )

    private val ROTATION_TRANSITIONS = mapOf(
        VolunteerRotationStatus.DRAFT to setOf(VolunteerRotationStatus.ACTIVE, VolunteerRotationStatus.CANCELLED),
        VolunteerRotationStatus.ACTIVE to setOf(VolunteerRotationStatus.CLOSED, VolunteerRotationStatus.CANCELLED),
        VolunteerRotationStatus.CLOSED to emptySet(),
        VolunteerRotationStatus.CANCELLED to emptySet(),
    )

    private val INVENTORY_TRANSITIONS = mapOf(
        InventoryItemStatus.ACTIVE to setOf(InventoryItemStatus.OUT_OF_SERVICE, InventoryItemStatus.RETIRED),
        InventoryItemStatus.OUT_OF_SERVICE to setOf(InventoryItemStatus.ACTIVE, InventoryItemStatus.RETIRED),
        InventoryItemStatus.RETIRED to emptySet(),
    )

    private val FINANCIAL_CONTROL_TRANSITIONS = mapOf(
        FinancialControlStatus.PENDING to setOf(FinancialControlStatus.VERIFIED, FinancialControlStatus.NOT_APPLICABLE),
        FinancialControlStatus.VERIFIED to setOf(FinancialControlStatus.PENDING),
        FinancialControlStatus.NOT_APPLICABLE to setOf(FinancialControlStatus.PENDING),
    )

    private val FINANCIAL_CHECKLIST_TRANSITIONS = mapOf(
        FinancialChecklistStatus.DRAFT to setOf(FinancialChecklistStatus.ACTIVE, FinancialChecklistStatus.RETIRED),
        FinancialChecklistStatus.ACTIVE to setOf(FinancialChecklistStatus.COMPLETED, FinancialChecklistStatus.RETIRED),
        FinancialChecklistStatus.COMPLETED to emptySet(),
        FinancialChecklistStatus.RETIRED to emptySet(),
    )
}
