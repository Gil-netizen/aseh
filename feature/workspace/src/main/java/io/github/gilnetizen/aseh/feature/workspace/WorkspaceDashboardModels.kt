package io.github.gilnetizen.aseh.feature.workspace

import io.github.gilnetizen.aseh.domain.workspace.CalendarItemStatus
import io.github.gilnetizen.aseh.domain.workspace.DueSoonItem
import io.github.gilnetizen.aseh.domain.workspace.FinancialChecklistStatus
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlStatus
import io.github.gilnetizen.aseh.domain.workspace.HouseholdWorkspace
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.LocalWorkspaceApi
import io.github.gilnetizen.aseh.domain.workspace.PracticeAdoptionStatus
import io.github.gilnetizen.aseh.domain.workspace.PreparationKitStatus
import io.github.gilnetizen.aseh.domain.workspace.QahalDecisionStatus
import io.github.gilnetizen.aseh.domain.workspace.QahalWorkspace
import io.github.gilnetizen.aseh.domain.workspace.SelfWorkspace
import io.github.gilnetizen.aseh.domain.workspace.VolunteerRotationStatus
import io.github.gilnetizen.aseh.domain.workspace.VolunteerSlotStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkItemStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceArea
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssue
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssueCode
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceOperationResult
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceUpdateResult
import java.time.LocalDate

sealed interface ActiveWorkspaceContext {
    val workspaceId: WorkspaceId
    val area: WorkspaceArea

    data class Self(override val workspaceId: WorkspaceId) : ActiveWorkspaceContext {
        override val area = WorkspaceArea.SELF
    }

    data class Household(override val workspaceId: WorkspaceId) : ActiveWorkspaceContext {
        override val area = WorkspaceArea.HOUSEHOLD
    }

    data class Qahal(override val workspaceId: WorkspaceId) : ActiveWorkspaceContext {
        override val area = WorkspaceArea.QAHAL
    }
}

data class WorkspaceDashboardFeedback(
    val validationIssues: List<WorkspaceIssue> = emptyList(),
    val rejectedCommandIssues: List<WorkspaceIssue> = emptyList(),
    val persistenceFailed: Boolean = false,
)

sealed interface WorkspaceDashboardContent {
    data class Self(val workspace: SelfWorkspace) : WorkspaceDashboardContent

    data class Household(val workspace: HouseholdWorkspace) : WorkspaceDashboardContent

    data class Qahal(val workspace: QahalWorkspace) : WorkspaceDashboardContent

    data object Missing : WorkspaceDashboardContent
}

enum class WorkspaceRecordKind {
    PRACTICE,
    RESPONSIBILITY,
    CALENDAR_ITEM,
    PREPARATION_KIT,
    PREPARATION_TASK,
    DECISION,
    VOLUNTEER_ROTATION,
    VOLUNTEER_SLOT,
    INVENTORY,
    FINANCIAL_CHECKLIST,
    FINANCIAL_CONTROL,
}

data class WorkspaceActionTarget(
    val kind: WorkspaceRecordKind,
    val recordId: WorkspaceRecordId,
    val parentRecordId: WorkspaceRecordId? = null,
)

enum class WorkspaceActionLabel {
    ADOPT,
    PAUSE,
    RESUME,
    RETIRE,
    RESTORE_DRAFT,
    START,
    MARK_DONE,
    REOPEN,
    CANCEL,
    SCHEDULE,
    COMPLETE,
    ACTIVATE,
    SUPERSEDE,
    UNASSIGN,
    MARK_OUT_OF_SERVICE,
    RETURN_TO_SERVICE,
    VERIFY,
    NOT_APPLICABLE,
    RESET,
    CLOSE,
}

data class WorkspaceDashboardAction(
    val target: WorkspaceActionTarget,
    val label: WorkspaceActionLabel,
    val command: WorkspaceCommand,
) {
    fun dispatch(onCommand: (WorkspaceCommand) -> Unit) = onCommand(command)
}

data class WorkspaceDashboardModel(
    val activeContext: ActiveWorkspaceContext,
    val contextLabel: String?,
    val content: WorkspaceDashboardContent,
    val dueSoonItems: List<DueSoonItem>,
    val actions: List<WorkspaceDashboardAction>,
    val domainIssues: List<WorkspaceIssue>,
) {
    fun actionsFor(
        kind: WorkspaceRecordKind,
        recordId: WorkspaceRecordId,
        parentRecordId: WorkspaceRecordId? = null,
    ): List<WorkspaceDashboardAction> = actions.filter { action ->
        action.target.kind == kind &&
            action.target.recordId == recordId &&
            action.target.parentRecordId == parentRecordId
    }
}

object WorkspaceDashboardPresenter {
    fun present(
        snapshot: WorkspaceSnapshot,
        activeContext: ActiveWorkspaceContext,
        asOf: LocalDate,
        dueSoonHorizonDays: Long = 7,
    ): WorkspaceDashboardModel {
        val content = when (activeContext) {
            is ActiveWorkspaceContext.Self -> snapshot.selfWorkspaces
                .firstOrNull { it.id == activeContext.workspaceId }
                ?.let(WorkspaceDashboardContent::Self)
                ?: WorkspaceDashboardContent.Missing
            is ActiveWorkspaceContext.Household -> snapshot.households
                .firstOrNull { it.id == activeContext.workspaceId }
                ?.let(WorkspaceDashboardContent::Household)
                ?: WorkspaceDashboardContent.Missing
            is ActiveWorkspaceContext.Qahal -> snapshot.qahalWorkspaces
                .firstOrNull { it.id == activeContext.workspaceId }
                ?.let(WorkspaceDashboardContent::Qahal)
                ?: WorkspaceDashboardContent.Missing
        }
        val contextLabel = when (content) {
            is WorkspaceDashboardContent.Self -> content.workspace.label
            is WorkspaceDashboardContent.Household -> content.workspace.label
            is WorkspaceDashboardContent.Qahal -> content.workspace.label
            WorkspaceDashboardContent.Missing -> null
        }
        val baseIssues = LocalWorkspaceApi.validate(snapshot)
        val dueResult = LocalWorkspaceApi.dueSoon(snapshot, asOf, dueSoonHorizonDays)
        val dueItems = when (dueResult) {
            is WorkspaceOperationResult.Success -> dueResult.value.items.filter { item ->
                item.area == activeContext.area && item.workspaceId == activeContext.workspaceId
            }
            is WorkspaceOperationResult.Rejected -> emptyList()
        }
        val missingIssue = if (content == WorkspaceDashboardContent.Missing) {
            listOf(
                WorkspaceIssue(
                    code = WorkspaceIssueCode.RECORD_NOT_FOUND,
                    path = "activeContext",
                    detail = "The selected workspace is unavailable.",
                ),
            )
        } else {
            emptyList()
        }
        val dueIssues = (dueResult as? WorkspaceOperationResult.Rejected)?.issues.orEmpty()
        return WorkspaceDashboardModel(
            activeContext = activeContext,
            contextLabel = contextLabel,
            content = content,
            dueSoonItems = dueItems,
            actions = if (content == WorkspaceDashboardContent.Missing) {
                emptyList()
            } else {
                WorkspaceDashboardActions.create(snapshot, activeContext, asOf)
            },
            domainIssues = (baseIssues + dueIssues + missingIssue).distinct(),
        )
    }
}

private object WorkspaceDashboardActions {
    fun create(
        snapshot: WorkspaceSnapshot,
        activeContext: ActiveWorkspaceContext,
        asOf: LocalDate,
    ): List<WorkspaceDashboardAction> = buildList {
        when (activeContext) {
            is ActiveWorkspaceContext.Self -> snapshot.selfWorkspaces
                .firstOrNull { it.id == activeContext.workspaceId }
                ?.practiceAdoptions
                .orEmpty()
                .forEach { adoption ->
                    fun offer(label: WorkspaceActionLabel, target: PracticeAdoptionStatus) {
                        offerIfValid(
                            snapshot,
                            WorkspaceDashboardAction(
                                target = WorkspaceActionTarget(WorkspaceRecordKind.PRACTICE, adoption.id),
                                label = label,
                                command = WorkspaceCommand.ChangePracticeStatus(
                                    selfId = activeContext.workspaceId,
                                    id = adoption.id,
                                    target = target,
                                    changedOn = asOf,
                                    nextReviewOn = adoption.reviewOn,
                                ),
                            ),
                        )
                    }
                    when (adoption.status) {
                        PracticeAdoptionStatus.DRAFT -> offer(WorkspaceActionLabel.ADOPT, PracticeAdoptionStatus.ADOPTED)
                        PracticeAdoptionStatus.ADOPTED -> {
                            offer(WorkspaceActionLabel.PAUSE, PracticeAdoptionStatus.PAUSED)
                            offer(WorkspaceActionLabel.RETIRE, PracticeAdoptionStatus.RETIRED)
                        }
                        PracticeAdoptionStatus.PAUSED -> {
                            offer(WorkspaceActionLabel.RESUME, PracticeAdoptionStatus.ADOPTED)
                            offer(WorkspaceActionLabel.RETIRE, PracticeAdoptionStatus.RETIRED)
                        }
                        PracticeAdoptionStatus.RETIRED -> offer(
                            WorkspaceActionLabel.RESTORE_DRAFT,
                            PracticeAdoptionStatus.DRAFT,
                        )
                    }
                }
            is ActiveWorkspaceContext.Household -> snapshot.households
                .firstOrNull { it.id == activeContext.workspaceId }
                ?.let { household ->
                    household.responsibilities.forEach { item ->
                        workActions(item.status).forEach { (label, target) ->
                            offerIfValid(
                                snapshot,
                                WorkspaceDashboardAction(
                                    WorkspaceActionTarget(WorkspaceRecordKind.RESPONSIBILITY, item.id),
                                    label,
                                    WorkspaceCommand.ChangeResponsibilityStatus(
                                        household.id,
                                        item.id,
                                        target,
                                        asOf,
                                    ),
                                ),
                            )
                        }
                    }
                    household.calendarItems.forEach { item ->
                        calendarActions(item.status).forEach { (label, target) ->
                            offerIfValid(
                                snapshot,
                                WorkspaceDashboardAction(
                                    WorkspaceActionTarget(WorkspaceRecordKind.CALENDAR_ITEM, item.id),
                                    label,
                                    WorkspaceCommand.ChangeCalendarItemStatus(household.id, item.id, target, asOf),
                                ),
                            )
                        }
                    }
                    household.preparationKits.forEach { kit ->
                        kit.tasks.forEach { task ->
                            workActions(task.status).forEach { (label, target) ->
                                offerIfValid(
                                    snapshot,
                                    WorkspaceDashboardAction(
                                        WorkspaceActionTarget(
                                            WorkspaceRecordKind.PREPARATION_TASK,
                                            task.id,
                                            kit.id,
                                        ),
                                        label,
                                        WorkspaceCommand.ChangePreparationTaskStatus(
                                            household.id,
                                            kit.id,
                                            task.id,
                                            target,
                                            asOf,
                                        ),
                                    ),
                                )
                            }
                        }
                        preparationKitActions(kit.status).forEach { (label, target) ->
                            offerIfValid(
                                snapshot,
                                WorkspaceDashboardAction(
                                    WorkspaceActionTarget(WorkspaceRecordKind.PREPARATION_KIT, kit.id),
                                    label,
                                    WorkspaceCommand.ChangePreparationKitStatus(household.id, kit.id, target, asOf),
                                ),
                            )
                        }
                    }
                }
            is ActiveWorkspaceContext.Qahal -> snapshot.qahalWorkspaces
                .firstOrNull { it.id == activeContext.workspaceId }
                ?.let { qahal -> addQahalActions(snapshot, qahal, asOf) }
        }
    }

    private fun MutableList<WorkspaceDashboardAction>.addQahalActions(
        snapshot: WorkspaceSnapshot,
        qahal: QahalWorkspace,
        asOf: LocalDate,
    ) {
        qahal.decisions.forEach { decision ->
            decisionActions(decision.status).forEach { (label, target) ->
                offerIfValid(
                    snapshot,
                    WorkspaceDashboardAction(
                        WorkspaceActionTarget(WorkspaceRecordKind.DECISION, decision.id),
                        label,
                        WorkspaceCommand.ChangeDecisionStatus(
                            qahal.id,
                            decision.id,
                            target,
                            asOf,
                            decision.reviewOn,
                        ),
                    ),
                )
            }
        }
        qahal.volunteerRotations.forEach { rotation ->
            rotation.slots.forEach { slot ->
                volunteerSlotActions(slot.status).forEach { (label, target) ->
                    offerIfValid(
                        snapshot,
                        WorkspaceDashboardAction(
                            WorkspaceActionTarget(
                                WorkspaceRecordKind.VOLUNTEER_SLOT,
                                slot.id,
                                rotation.id,
                            ),
                            label,
                            WorkspaceCommand.ChangeVolunteerSlotStatus(
                                qahal.id,
                                rotation.id,
                                slot.id,
                                target,
                                asOf,
                                slot.assignedTo,
                            ),
                        ),
                    )
                }
            }
            rotationActions(rotation.status).forEach { (label, target) ->
                offerIfValid(
                    snapshot,
                    WorkspaceDashboardAction(
                        WorkspaceActionTarget(WorkspaceRecordKind.VOLUNTEER_ROTATION, rotation.id),
                        label,
                        WorkspaceCommand.ChangeVolunteerRotationStatus(qahal.id, rotation.id, target, asOf),
                    ),
                )
            }
        }
        qahal.inventory.forEach { item ->
            inventoryActions(item.status).forEach { (label, target) ->
                offerIfValid(
                    snapshot,
                    WorkspaceDashboardAction(
                        WorkspaceActionTarget(WorkspaceRecordKind.INVENTORY, item.id),
                        label,
                        WorkspaceCommand.ChangeInventoryStatus(qahal.id, item.id, target, asOf),
                    ),
                )
            }
        }
        qahal.financialControls.forEach { checklist ->
            checklist.controls.forEach { control ->
                financialControlActions(control.status).forEach { (label, target) ->
                    offerIfValid(
                        snapshot,
                        WorkspaceDashboardAction(
                            WorkspaceActionTarget(
                                WorkspaceRecordKind.FINANCIAL_CONTROL,
                                control.id,
                                checklist.id,
                            ),
                            label,
                            WorkspaceCommand.ChangeFinancialControlStatus(
                                qahal.id,
                                checklist.id,
                                control.id,
                                target,
                                asOf,
                            ),
                        ),
                    )
                }
            }
            financialChecklistActions(checklist.status).forEach { (label, target) ->
                offerIfValid(
                    snapshot,
                    WorkspaceDashboardAction(
                        WorkspaceActionTarget(WorkspaceRecordKind.FINANCIAL_CHECKLIST, checklist.id),
                        label,
                        WorkspaceCommand.ChangeFinancialChecklistStatus(
                            qahal.id,
                            checklist.id,
                            target,
                            asOf,
                            checklist.reviewOn,
                        ),
                    ),
                )
            }
        }
    }

    private fun MutableList<WorkspaceDashboardAction>.offerIfValid(
        snapshot: WorkspaceSnapshot,
        action: WorkspaceDashboardAction,
    ) {
        if (LocalWorkspaceApi.apply(snapshot, action.command) is WorkspaceUpdateResult.Applied) add(action)
    }

    private fun workActions(status: WorkItemStatus) = when (status) {
        WorkItemStatus.TODO -> listOf(
            WorkspaceActionLabel.START to WorkItemStatus.IN_PROGRESS,
            WorkspaceActionLabel.MARK_DONE to WorkItemStatus.DONE,
            WorkspaceActionLabel.CANCEL to WorkItemStatus.CANCELLED,
        )
        WorkItemStatus.IN_PROGRESS -> listOf(
            WorkspaceActionLabel.REOPEN to WorkItemStatus.TODO,
            WorkspaceActionLabel.MARK_DONE to WorkItemStatus.DONE,
            WorkspaceActionLabel.CANCEL to WorkItemStatus.CANCELLED,
        )
        WorkItemStatus.DONE,
        WorkItemStatus.CANCELLED,
        -> listOf(WorkspaceActionLabel.REOPEN to WorkItemStatus.TODO)
    }

    private fun calendarActions(status: CalendarItemStatus) = when (status) {
        CalendarItemStatus.DRAFT -> listOf(
            WorkspaceActionLabel.SCHEDULE to CalendarItemStatus.SCHEDULED,
            WorkspaceActionLabel.CANCEL to CalendarItemStatus.CANCELLED,
        )
        CalendarItemStatus.SCHEDULED -> listOf(
            WorkspaceActionLabel.COMPLETE to CalendarItemStatus.COMPLETED,
            WorkspaceActionLabel.CANCEL to CalendarItemStatus.CANCELLED,
        )
        CalendarItemStatus.COMPLETED,
        CalendarItemStatus.CANCELLED,
        -> listOf(WorkspaceActionLabel.RESTORE_DRAFT to CalendarItemStatus.DRAFT)
    }

    private fun preparationKitActions(status: PreparationKitStatus) = when (status) {
        PreparationKitStatus.DRAFT -> listOf(
            WorkspaceActionLabel.ACTIVATE to PreparationKitStatus.ACTIVE,
            WorkspaceActionLabel.CANCEL to PreparationKitStatus.CANCELLED,
        )
        PreparationKitStatus.ACTIVE -> listOf(
            WorkspaceActionLabel.COMPLETE to PreparationKitStatus.COMPLETED,
            WorkspaceActionLabel.CANCEL to PreparationKitStatus.CANCELLED,
        )
        PreparationKitStatus.COMPLETED,
        PreparationKitStatus.CANCELLED,
        -> listOf(WorkspaceActionLabel.RESTORE_DRAFT to PreparationKitStatus.DRAFT)
    }

    private fun decisionActions(status: QahalDecisionStatus) = when (status) {
        QahalDecisionStatus.DRAFT -> listOf(
            WorkspaceActionLabel.ADOPT to QahalDecisionStatus.ADOPTED,
            WorkspaceActionLabel.RETIRE to QahalDecisionStatus.RETIRED,
        )
        QahalDecisionStatus.ADOPTED -> listOf(
            WorkspaceActionLabel.SUPERSEDE to QahalDecisionStatus.SUPERSEDED,
            WorkspaceActionLabel.RETIRE to QahalDecisionStatus.RETIRED,
        )
        QahalDecisionStatus.SUPERSEDED,
        QahalDecisionStatus.RETIRED,
        -> emptyList()
    }

    private fun volunteerSlotActions(status: VolunteerSlotStatus) = when (status) {
        VolunteerSlotStatus.OPEN -> listOf(WorkspaceActionLabel.CANCEL to VolunteerSlotStatus.CANCELLED)
        VolunteerSlotStatus.ASSIGNED -> listOf(
            WorkspaceActionLabel.UNASSIGN to VolunteerSlotStatus.OPEN,
            WorkspaceActionLabel.COMPLETE to VolunteerSlotStatus.COMPLETED,
            WorkspaceActionLabel.CANCEL to VolunteerSlotStatus.CANCELLED,
        )
        VolunteerSlotStatus.COMPLETED,
        VolunteerSlotStatus.CANCELLED,
        -> listOf(WorkspaceActionLabel.REOPEN to VolunteerSlotStatus.OPEN)
    }

    private fun rotationActions(status: VolunteerRotationStatus) = when (status) {
        VolunteerRotationStatus.DRAFT -> listOf(
            WorkspaceActionLabel.ACTIVATE to VolunteerRotationStatus.ACTIVE,
            WorkspaceActionLabel.CANCEL to VolunteerRotationStatus.CANCELLED,
        )
        VolunteerRotationStatus.ACTIVE -> listOf(
            WorkspaceActionLabel.CLOSE to VolunteerRotationStatus.CLOSED,
            WorkspaceActionLabel.CANCEL to VolunteerRotationStatus.CANCELLED,
        )
        VolunteerRotationStatus.CLOSED,
        VolunteerRotationStatus.CANCELLED,
        -> emptyList()
    }

    private fun inventoryActions(status: InventoryItemStatus) = when (status) {
        InventoryItemStatus.ACTIVE -> listOf(
            WorkspaceActionLabel.MARK_OUT_OF_SERVICE to InventoryItemStatus.OUT_OF_SERVICE,
            WorkspaceActionLabel.RETIRE to InventoryItemStatus.RETIRED,
        )
        InventoryItemStatus.OUT_OF_SERVICE -> listOf(
            WorkspaceActionLabel.RETURN_TO_SERVICE to InventoryItemStatus.ACTIVE,
            WorkspaceActionLabel.RETIRE to InventoryItemStatus.RETIRED,
        )
        InventoryItemStatus.RETIRED -> emptyList()
    }

    private fun financialControlActions(status: FinancialControlStatus) = when (status) {
        FinancialControlStatus.PENDING -> listOf(
            WorkspaceActionLabel.VERIFY to FinancialControlStatus.VERIFIED,
            WorkspaceActionLabel.NOT_APPLICABLE to FinancialControlStatus.NOT_APPLICABLE,
        )
        FinancialControlStatus.VERIFIED,
        FinancialControlStatus.NOT_APPLICABLE,
        -> listOf(WorkspaceActionLabel.RESET to FinancialControlStatus.PENDING)
    }

    private fun financialChecklistActions(status: FinancialChecklistStatus) = when (status) {
        FinancialChecklistStatus.DRAFT -> listOf(
            WorkspaceActionLabel.ACTIVATE to FinancialChecklistStatus.ACTIVE,
            WorkspaceActionLabel.RETIRE to FinancialChecklistStatus.RETIRED,
        )
        FinancialChecklistStatus.ACTIVE -> listOf(
            WorkspaceActionLabel.COMPLETE to FinancialChecklistStatus.COMPLETED,
            WorkspaceActionLabel.RETIRE to FinancialChecklistStatus.RETIRED,
        )
        FinancialChecklistStatus.COMPLETED,
        FinancialChecklistStatus.RETIRED,
        -> emptyList()
    }
}
