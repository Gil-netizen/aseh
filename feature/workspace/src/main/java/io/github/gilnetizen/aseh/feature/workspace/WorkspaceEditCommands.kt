package io.github.gilnetizen.aseh.feature.workspace

import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarItem
import io.github.gilnetizen.aseh.domain.workspace.HouseholdResponsibility
import io.github.gilnetizen.aseh.domain.workspace.HouseholdPreparationKit
import io.github.gilnetizen.aseh.domain.workspace.HouseholdWorkspace
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlChecklist
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlItem
import io.github.gilnetizen.aseh.domain.workspace.InventoryItem
import io.github.gilnetizen.aseh.domain.workspace.QahalDecision
import io.github.gilnetizen.aseh.domain.workspace.QahalWorkspace
import io.github.gilnetizen.aseh.domain.workspace.PreparationKitTask
import io.github.gilnetizen.aseh.domain.workspace.SelfWorkspace
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot
import io.github.gilnetizen.aseh.domain.workspace.VolunteerRotation
import io.github.gilnetizen.aseh.domain.workspace.VolunteerSlot

/** Builds record-level commands that the repository applies to its latest snapshot. */
internal object WorkspaceEditCommands {
    fun rename(workspace: SelfWorkspace, label: String): WorkspaceCommand.RenameSelf =
        WorkspaceCommand.RenameSelf(workspace.id, label.trim())

    fun rename(workspace: HouseholdWorkspace, label: String): WorkspaceCommand.RenameHousehold =
        WorkspaceCommand.RenameHousehold(workspace.id, label.trim())

    fun rename(workspace: QahalWorkspace, label: String): WorkspaceCommand.RenameQahal =
        WorkspaceCommand.RenameQahal(workspace.id, label.trim())

    fun putResponsibility(
        workspace: HouseholdWorkspace,
        responsibility: HouseholdResponsibility,
    ): WorkspaceCommand.PutHouseholdResponsibility =
        WorkspaceCommand.PutHouseholdResponsibility(workspace.id, responsibility)

    fun deleteResponsibility(
        workspace: HouseholdWorkspace,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteHouseholdResponsibility =
        WorkspaceCommand.DeleteHouseholdResponsibility(workspace.id, id)

    fun putCalendarItem(
        workspace: HouseholdWorkspace,
        item: HouseholdCalendarItem,
    ): WorkspaceCommand.PutHouseholdCalendarItem =
        WorkspaceCommand.PutHouseholdCalendarItem(workspace.id, item)

    fun deleteCalendarItem(
        workspace: HouseholdWorkspace,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteHouseholdCalendarItem =
        WorkspaceCommand.DeleteHouseholdCalendarItem(workspace.id, id)

    fun putPreparationKit(
        workspace: HouseholdWorkspace,
        kit: HouseholdPreparationKit,
    ): WorkspaceCommand.PutHouseholdPreparationKit = WorkspaceCommand.PutHouseholdPreparationKit(workspace.id, kit)

    fun deletePreparationKit(
        workspace: HouseholdWorkspace,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteHouseholdPreparationKit = WorkspaceCommand.DeleteHouseholdPreparationKit(workspace.id, id)

    fun putPreparationTask(
        workspace: HouseholdWorkspace,
        kit: HouseholdPreparationKit,
        task: PreparationKitTask,
    ): WorkspaceCommand.PutPreparationKitTask = WorkspaceCommand.PutPreparationKitTask(workspace.id, kit.id, task)

    fun deletePreparationTask(
        workspace: HouseholdWorkspace,
        kit: HouseholdPreparationKit,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeletePreparationKitTask = WorkspaceCommand.DeletePreparationKitTask(workspace.id, kit.id, id)

    fun putDecision(
        workspace: QahalWorkspace,
        decision: QahalDecision,
    ): WorkspaceCommand.PutQahalDecision = WorkspaceCommand.PutQahalDecision(workspace.id, decision)

    fun deleteDecision(
        workspace: QahalWorkspace,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteQahalDecision = WorkspaceCommand.DeleteQahalDecision(workspace.id, id)

    fun putInventoryItem(
        workspace: QahalWorkspace,
        item: InventoryItem,
    ): WorkspaceCommand.PutInventoryItem = WorkspaceCommand.PutInventoryItem(workspace.id, item)

    fun deleteInventoryItem(
        workspace: QahalWorkspace,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteInventoryItem = WorkspaceCommand.DeleteInventoryItem(workspace.id, id)

    fun putVolunteerRotation(
        workspace: QahalWorkspace,
        rotation: VolunteerRotation,
    ): WorkspaceCommand.PutVolunteerRotation = WorkspaceCommand.PutVolunteerRotation(workspace.id, rotation)

    fun deleteVolunteerRotation(
        workspace: QahalWorkspace,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteVolunteerRotation = WorkspaceCommand.DeleteVolunteerRotation(workspace.id, id)

    fun putVolunteerSlot(
        workspace: QahalWorkspace,
        rotation: VolunteerRotation,
        slot: VolunteerSlot,
    ): WorkspaceCommand.PutVolunteerSlot {
        val expected = rotation.slots.firstOrNull { it.id == slot.id } ?: slot
        return WorkspaceCommand.PutVolunteerSlot(
            qahalId = workspace.id,
            rotationId = rotation.id,
            slot = slot,
            expectedStatus = expected.status,
            expectedStatusChangedOn = expected.statusChangedOn,
            expectedAssignedTo = expected.assignedTo,
        )
    }

    fun deleteVolunteerSlot(
        workspace: QahalWorkspace,
        rotation: VolunteerRotation,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteVolunteerSlot = WorkspaceCommand.DeleteVolunteerSlot(workspace.id, rotation.id, id)

    fun putFinancialChecklist(
        workspace: QahalWorkspace,
        checklist: FinancialControlChecklist,
    ): WorkspaceCommand.PutFinancialChecklist = WorkspaceCommand.PutFinancialChecklist(workspace.id, checklist)

    fun deleteFinancialChecklist(
        workspace: QahalWorkspace,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteFinancialChecklist = WorkspaceCommand.DeleteFinancialChecklist(workspace.id, id)

    fun putFinancialControl(
        workspace: QahalWorkspace,
        checklist: FinancialControlChecklist,
        control: FinancialControlItem,
    ): WorkspaceCommand.PutFinancialControl = WorkspaceCommand.PutFinancialControl(workspace.id, checklist.id, control)

    fun deleteFinancialControl(
        workspace: QahalWorkspace,
        checklist: FinancialControlChecklist,
        id: WorkspaceRecordId,
    ): WorkspaceCommand.DeleteFinancialControl = WorkspaceCommand.DeleteFinancialControl(workspace.id, checklist.id, id)

    fun isApplied(snapshot: WorkspaceSnapshot, command: WorkspaceCommand): Boolean = when (command) {
        is WorkspaceCommand.RenameSelf -> snapshot.selfWorkspaces
            .firstOrNull { it.id == command.selfId }
            ?.label == command.label.trim()
        is WorkspaceCommand.RenameHousehold -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.label == command.label.trim()
        is WorkspaceCommand.RenameQahal -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.label == command.label.trim()
        is WorkspaceCommand.PutHouseholdResponsibility -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.responsibilities
            ?.any {
                it.id == command.responsibility.id &&
                    it.label == command.responsibility.label &&
                    it.dueOn == command.responsibility.dueOn &&
                    it.assignedTo == command.responsibility.assignedTo
            } == true
        is WorkspaceCommand.DeleteHouseholdResponsibility -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.responsibilities
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutHouseholdCalendarItem -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.calendarItems
            ?.any {
                it.id == command.item.id && it.label == command.item.label &&
                    it.kind == command.item.kind && it.startsOn == command.item.startsOn &&
                    it.endsOn == command.item.endsOn
            } == true
        is WorkspaceCommand.DeleteHouseholdCalendarItem -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.calendarItems
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutHouseholdPreparationKit -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.preparationKits
            ?.any {
                it.id == command.kit.id && it.label == command.kit.label &&
                    it.targetOn == command.kit.targetOn
            } == true
        is WorkspaceCommand.DeleteHouseholdPreparationKit -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.preparationKits
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutPreparationKitTask -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.preparationKits
            ?.firstOrNull { it.id == command.kitId }
            ?.tasks
            ?.any {
                it.id == command.task.id && it.label == command.task.label &&
                    it.dueOn == command.task.dueOn && it.assignedTo == command.task.assignedTo
            } == true
        is WorkspaceCommand.DeletePreparationKitTask -> snapshot.households
            .firstOrNull { it.id == command.householdId }
            ?.preparationKits
            ?.firstOrNull { it.id == command.kitId }
            ?.tasks
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutQahalDecision -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.decisions
            ?.any {
                it.id == command.decision.id && it.label == command.decision.label &&
                    it.classification == command.decision.classification &&
                    it.publicSummary == command.decision.publicSummary &&
                    it.authorityScope == command.decision.authorityScope &&
                    (
                        it.effectiveOn == command.decision.effectiveOn ||
                            it.status != command.decision.status
                        ) &&
                    (
                        it.reviewOn == command.decision.reviewOn ||
                            it.status != command.decision.status
                        )
            } == true
        is WorkspaceCommand.DeleteQahalDecision -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.decisions
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutInventoryItem -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.inventory
            ?.any {
                it.id == command.item.id && it.label == command.item.label &&
                    it.quantityOnHand == command.item.quantityOnHand &&
                    it.minimumDesired == command.item.minimumDesired &&
                    it.nextCheckOn == command.item.nextCheckOn
            } == true
        is WorkspaceCommand.DeleteInventoryItem -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.inventory
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutVolunteerRotation -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.volunteerRotations
            ?.any {
                it.id == command.rotation.id && it.label == command.rotation.label &&
                    it.publicRole == command.rotation.publicRole
            } == true
        is WorkspaceCommand.DeleteVolunteerRotation -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.volunteerRotations
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutVolunteerSlot -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.volunteerRotations
            ?.firstOrNull { it.id == command.rotationId }
            ?.slots
            ?.any { current ->
                val concurrentLifecycleWasPreserved =
                    current.status != command.expectedStatus ||
                        current.statusChangedOn != command.expectedStatusChangedOn ||
                        current.assignedTo != command.expectedAssignedTo
                current.id == command.slot.id && current.serviceOn == command.slot.serviceOn &&
                    (current.assignedTo == command.slot.assignedTo || concurrentLifecycleWasPreserved)
            } == true
        is WorkspaceCommand.DeleteVolunteerSlot -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.volunteerRotations
            ?.firstOrNull { it.id == command.rotationId }
            ?.slots
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutFinancialChecklist -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.financialControls
            ?.any {
                it.id == command.checklist.id && it.label == command.checklist.label &&
                    it.reviewOn == command.checklist.reviewOn
            } == true
        is WorkspaceCommand.DeleteFinancialChecklist -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.financialControls
            ?.none { it.id == command.id } == true
        is WorkspaceCommand.PutFinancialControl -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.financialControls
            ?.firstOrNull { it.id == command.checklistId }
            ?.controls
            ?.any {
                it.id == command.control.id && it.kind == command.control.kind &&
                    it.dueOn == command.control.dueOn &&
                    it.responsibleRole == command.control.responsibleRole
            } == true
        is WorkspaceCommand.DeleteFinancialControl -> snapshot.qahalWorkspaces
            .firstOrNull { it.id == command.qahalId }
            ?.financialControls
            ?.firstOrNull { it.id == command.checklistId }
            ?.controls
            ?.none { it.id == command.id } == true
        else -> false
    }
}
