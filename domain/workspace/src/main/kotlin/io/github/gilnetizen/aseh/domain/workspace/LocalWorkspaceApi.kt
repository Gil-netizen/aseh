package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate

sealed interface WorkspaceCommand {
    data class PutSelf(val self: SelfWorkspace) : WorkspaceCommand

    data class PutPersonalPractice(
        val selfId: WorkspaceId,
        val adoption: PersonalPracticeAdoption,
    ) : WorkspaceCommand

    data class PutHousehold(val household: HouseholdWorkspace) : WorkspaceCommand

    data class PutQahal(val qahal: QahalWorkspace) : WorkspaceCommand

    data class RenameSelf(val selfId: WorkspaceId, val label: String) : WorkspaceCommand

    data class RenameHousehold(val householdId: WorkspaceId, val label: String) : WorkspaceCommand

    data class RenameQahal(val qahalId: WorkspaceId, val label: String) : WorkspaceCommand

    data class PutHouseholdResponsibility(
        val householdId: WorkspaceId,
        val responsibility: HouseholdResponsibility,
    ) : WorkspaceCommand

    data class DeleteHouseholdResponsibility(
        val householdId: WorkspaceId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutHouseholdCalendarItem(
        val householdId: WorkspaceId,
        val item: HouseholdCalendarItem,
    ) : WorkspaceCommand

    data class DeleteHouseholdCalendarItem(
        val householdId: WorkspaceId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutHouseholdPreparationKit(
        val householdId: WorkspaceId,
        val kit: HouseholdPreparationKit,
    ) : WorkspaceCommand

    data class DeleteHouseholdPreparationKit(
        val householdId: WorkspaceId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutPreparationKitTask(
        val householdId: WorkspaceId,
        val kitId: WorkspaceRecordId,
        val task: PreparationKitTask,
    ) : WorkspaceCommand

    data class DeletePreparationKitTask(
        val householdId: WorkspaceId,
        val kitId: WorkspaceRecordId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutQahalDecision(
        val qahalId: WorkspaceId,
        val decision: QahalDecision,
    ) : WorkspaceCommand

    data class DeleteQahalDecision(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutInventoryItem(
        val qahalId: WorkspaceId,
        val item: InventoryItem,
    ) : WorkspaceCommand

    data class DeleteInventoryItem(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutVolunteerRotation(
        val qahalId: WorkspaceId,
        val rotation: VolunteerRotation,
    ) : WorkspaceCommand

    data class DeleteVolunteerRotation(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutVolunteerSlot(
        val qahalId: WorkspaceId,
        val rotationId: WorkspaceRecordId,
        val slot: VolunteerSlot,
        val expectedStatus: VolunteerSlotStatus = slot.status,
        val expectedStatusChangedOn: LocalDate = slot.statusChangedOn,
        val expectedAssignedTo: LocalPersonId? = slot.assignedTo,
    ) : WorkspaceCommand

    data class DeleteVolunteerSlot(
        val qahalId: WorkspaceId,
        val rotationId: WorkspaceRecordId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutFinancialChecklist(
        val qahalId: WorkspaceId,
        val checklist: FinancialControlChecklist,
    ) : WorkspaceCommand

    data class DeleteFinancialChecklist(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class PutFinancialControl(
        val qahalId: WorkspaceId,
        val checklistId: WorkspaceRecordId,
        val control: FinancialControlItem,
    ) : WorkspaceCommand

    data class DeleteFinancialControl(
        val qahalId: WorkspaceId,
        val checklistId: WorkspaceRecordId,
        val id: WorkspaceRecordId,
    ) : WorkspaceCommand

    data class ChangePracticeStatus(
        val selfId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: PracticeAdoptionStatus,
        val changedOn: LocalDate,
        val nextReviewOn: LocalDate? = null,
    ) : WorkspaceCommand

    data class ChangeResponsibilityStatus(
        val householdId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: WorkItemStatus,
        val changedOn: LocalDate,
    ) : WorkspaceCommand

    data class ChangeCalendarItemStatus(
        val householdId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: CalendarItemStatus,
        val changedOn: LocalDate,
    ) : WorkspaceCommand

    data class ChangePreparationTaskStatus(
        val householdId: WorkspaceId,
        val kitId: WorkspaceRecordId,
        val id: WorkspaceRecordId,
        val target: WorkItemStatus,
        val changedOn: LocalDate,
    ) : WorkspaceCommand

    data class ChangePreparationKitStatus(
        val householdId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: PreparationKitStatus,
        val changedOn: LocalDate,
    ) : WorkspaceCommand

    data class ChangeDecisionStatus(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: QahalDecisionStatus,
        val changedOn: LocalDate,
        val nextReviewOn: LocalDate? = null,
    ) : WorkspaceCommand

    data class ChangeVolunteerSlotStatus(
        val qahalId: WorkspaceId,
        val rotationId: WorkspaceRecordId,
        val id: WorkspaceRecordId,
        val target: VolunteerSlotStatus,
        val changedOn: LocalDate,
        val assignedTo: LocalPersonId? = null,
    ) : WorkspaceCommand

    data class ChangeVolunteerRotationStatus(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: VolunteerRotationStatus,
        val changedOn: LocalDate,
    ) : WorkspaceCommand

    data class ChangeInventoryStatus(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: InventoryItemStatus,
        val changedOn: LocalDate,
    ) : WorkspaceCommand

    data class ChangeFinancialControlStatus(
        val qahalId: WorkspaceId,
        val checklistId: WorkspaceRecordId,
        val id: WorkspaceRecordId,
        val target: FinancialControlStatus,
        val changedOn: LocalDate,
    ) : WorkspaceCommand

    data class ChangeFinancialChecklistStatus(
        val qahalId: WorkspaceId,
        val id: WorkspaceRecordId,
        val target: FinancialChecklistStatus,
        val changedOn: LocalDate,
        val nextReviewOn: LocalDate? = null,
    ) : WorkspaceCommand
}

sealed interface WorkspaceUpdateResult {
    data class Applied(val snapshot: WorkspaceSnapshot) : WorkspaceUpdateResult

    data class Rejected(
        val unchangedSnapshot: WorkspaceSnapshot,
        val issues: List<WorkspaceIssue>,
    ) : WorkspaceUpdateResult
}

/**
 * A deterministic, local-only API. Callers own persistence and supply every date explicitly;
 * this reducer has no Android, database, clock, network, provider, or analytics dependency.
 */
object LocalWorkspaceApi {
    fun validate(snapshot: WorkspaceSnapshot): List<WorkspaceIssue> = WorkspaceValidator.validate(snapshot)

    fun dueSoon(
        snapshot: WorkspaceSnapshot,
        asOf: LocalDate,
        horizonDays: Long = 7,
    ): WorkspaceOperationResult<DueSoonSummary> = WorkspaceDueSoon.summarize(snapshot, asOf, horizonDays)

    fun apply(snapshot: WorkspaceSnapshot, command: WorkspaceCommand): WorkspaceUpdateResult {
        val candidate = when (command) {
            is WorkspaceCommand.PutSelf -> WorkspaceOperationResult.Success(
                snapshot.copy(selfWorkspaces = snapshot.selfWorkspaces.replaceOrAppend(command.self) { it.id }),
            )
            is WorkspaceCommand.PutPersonalPractice -> updateSelf(snapshot, command.selfId) { self ->
                WorkspaceOperationResult.Success(
                    self.copy(
                        practiceAdoptions = self.practiceAdoptions.replaceOrAppend(command.adoption) { it.id },
                    ),
                )
            }
            is WorkspaceCommand.PutHousehold -> WorkspaceOperationResult.Success(
                snapshot.copy(households = snapshot.households.replaceOrAppend(command.household) { it.id }),
            )
            is WorkspaceCommand.PutQahal -> WorkspaceOperationResult.Success(
                snapshot.copy(qahalWorkspaces = snapshot.qahalWorkspaces.replaceOrAppend(command.qahal) { it.id }),
            )
            is WorkspaceCommand.RenameSelf -> updateSelf(snapshot, command.selfId) { self ->
                WorkspaceOperationResult.Success(self.copy(label = command.label.trim()))
            }
            is WorkspaceCommand.RenameHousehold -> updateHousehold(snapshot, command.householdId) { household ->
                WorkspaceOperationResult.Success(household.copy(label = command.label.trim()))
            }
            is WorkspaceCommand.RenameQahal -> updateQahal(snapshot, command.qahalId) { qahal ->
                WorkspaceOperationResult.Success(qahal.copy(label = command.label.trim()))
            }
            is WorkspaceCommand.PutHouseholdResponsibility -> updateHousehold(snapshot, command.householdId) { household ->
                val current = household.responsibilities.firstOrNull { it.id == command.responsibility.id }
                val merged = current?.let {
                    command.responsibility.copy(status = it.status, statusChangedOn = it.statusChangedOn)
                } ?: command.responsibility
                WorkspaceOperationResult.Success(
                    household.copy(
                        responsibilities = household.responsibilities.replaceOrAppend(merged) { it.id },
                    ),
                )
            }
            is WorkspaceCommand.DeleteHouseholdResponsibility -> updateHousehold(snapshot, command.householdId) { household ->
                household.responsibilities
                    .removeFirst("household.responsibilities") { it.id == command.id }
                    .map { household.copy(responsibilities = it) }
            }
            is WorkspaceCommand.PutHouseholdCalendarItem -> updateHousehold(snapshot, command.householdId) { household ->
                val current = household.calendarItems.firstOrNull { it.id == command.item.id }
                val merged = current?.let {
                    command.item.copy(status = it.status, statusChangedOn = it.statusChangedOn)
                } ?: command.item
                WorkspaceOperationResult.Success(
                    household.copy(calendarItems = household.calendarItems.replaceOrAppend(merged) { it.id }),
                )
            }
            is WorkspaceCommand.DeleteHouseholdCalendarItem -> updateHousehold(snapshot, command.householdId) { household ->
                household.calendarItems
                    .removeFirst("household.calendarItems") { it.id == command.id }
                    .map { household.copy(calendarItems = it) }
            }
            is WorkspaceCommand.PutHouseholdPreparationKit -> updateHousehold(snapshot, command.householdId) { household ->
                val current = household.preparationKits.firstOrNull { it.id == command.kit.id }
                val merged = current?.let {
                    command.kit.copy(
                        status = it.status,
                        statusChangedOn = it.statusChangedOn,
                        tasks = it.tasks,
                    )
                } ?: command.kit
                WorkspaceOperationResult.Success(
                    household.copy(preparationKits = household.preparationKits.replaceOrAppend(merged) { it.id }),
                )
            }
            is WorkspaceCommand.DeleteHouseholdPreparationKit -> updateHousehold(snapshot, command.householdId) { household ->
                household.preparationKits
                    .removeFirst("household.preparationKits") { it.id == command.id }
                    .map { household.copy(preparationKits = it) }
            }
            is WorkspaceCommand.PutPreparationKitTask -> updateHousehold(snapshot, command.householdId) { household ->
                household.preparationKits
                    .updateFirst("household.preparationKits", { it.id == command.kitId }) { kit ->
                        val current = kit.tasks.firstOrNull { it.id == command.task.id }
                        val merged = current?.let {
                            command.task.copy(status = it.status, statusChangedOn = it.statusChangedOn)
                        } ?: command.task
                        WorkspaceOperationResult.Success(
                            kit.copy(tasks = kit.tasks.replaceOrAppend(merged) { it.id }),
                        )
                    }.map { household.copy(preparationKits = it) }
            }
            is WorkspaceCommand.DeletePreparationKitTask -> updateHousehold(snapshot, command.householdId) { household ->
                household.preparationKits
                    .updateFirst("household.preparationKits", { it.id == command.kitId }) { kit ->
                        kit.tasks
                            .removeFirst("household.preparationKits.tasks") { it.id == command.id }
                            .map { kit.copy(tasks = it) }
                    }.map { household.copy(preparationKits = it) }
            }
            is WorkspaceCommand.PutQahalDecision -> updateQahal(snapshot, command.qahalId) { qahal ->
                val current = qahal.decisions.firstOrNull { it.id == command.decision.id }
                val merged = current?.let {
                    command.decision.copy(
                        status = it.status,
                        effectiveOn = if (it.status == command.decision.status) command.decision.effectiveOn else it.effectiveOn,
                        reviewOn = if (it.status == command.decision.status) command.decision.reviewOn else it.reviewOn,
                        statusChangedOn = it.statusChangedOn,
                        sourceReferences = it.sourceReferences,
                        dissentSummary = it.dissentSummary,
                    )
                } ?: command.decision
                WorkspaceOperationResult.Success(
                    qahal.copy(decisions = qahal.decisions.replaceOrAppend(merged) { it.id }),
                )
            }
            is WorkspaceCommand.DeleteQahalDecision -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.decisions
                    .removeFirst("qahal.decisions") { it.id == command.id }
                    .map { qahal.copy(decisions = it) }
            }
            is WorkspaceCommand.PutInventoryItem -> updateQahal(snapshot, command.qahalId) { qahal ->
                val current = qahal.inventory.firstOrNull { it.id == command.item.id }
                val merged = current?.let {
                    command.item.copy(status = it.status, statusChangedOn = it.statusChangedOn)
                } ?: command.item
                WorkspaceOperationResult.Success(
                    qahal.copy(inventory = qahal.inventory.replaceOrAppend(merged) { it.id }),
                )
            }
            is WorkspaceCommand.DeleteInventoryItem -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.inventory
                    .removeFirst("qahal.inventory") { it.id == command.id }
                    .map { qahal.copy(inventory = it) }
            }
            is WorkspaceCommand.PutVolunteerRotation -> updateQahal(snapshot, command.qahalId) { qahal ->
                val current = qahal.volunteerRotations.firstOrNull { it.id == command.rotation.id }
                val merged = current?.let {
                    command.rotation.copy(
                        status = it.status,
                        statusChangedOn = it.statusChangedOn,
                        slots = it.slots,
                    )
                } ?: command.rotation
                WorkspaceOperationResult.Success(
                    qahal.copy(
                        volunteerRotations = qahal.volunteerRotations.replaceOrAppend(merged) { it.id },
                    ),
                )
            }
            is WorkspaceCommand.DeleteVolunteerRotation -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.volunteerRotations
                    .removeFirst("qahal.volunteerRotations") { it.id == command.id }
                    .map { qahal.copy(volunteerRotations = it) }
            }
            is WorkspaceCommand.PutVolunteerSlot -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.volunteerRotations
                    .updateFirst("qahal.volunteerRotations", { it.id == command.rotationId }) { rotation ->
                        val current = rotation.slots.firstOrNull { it.id == command.slot.id }
                        val merged = current?.let {
                            val lifecycleChangedSinceEditorOpened =
                                it.status != command.expectedStatus ||
                                    it.statusChangedOn != command.expectedStatusChangedOn ||
                                    it.assignedTo != command.expectedAssignedTo
                            if (lifecycleChangedSinceEditorOpened) {
                                command.slot.copy(
                                    assignedTo = it.assignedTo,
                                    status = it.status,
                                    statusChangedOn = it.statusChangedOn,
                                )
                            } else {
                                command.slot
                            }
                        } ?: command.slot
                        WorkspaceOperationResult.Success(
                            rotation.copy(slots = rotation.slots.replaceOrAppend(merged) { it.id }),
                        )
                    }.map { qahal.copy(volunteerRotations = it) }
            }
            is WorkspaceCommand.DeleteVolunteerSlot -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.volunteerRotations
                    .updateFirst("qahal.volunteerRotations", { it.id == command.rotationId }) { rotation ->
                        rotation.slots
                            .removeFirst("qahal.volunteerRotations.slots") { it.id == command.id }
                            .map { rotation.copy(slots = it) }
                    }.map { qahal.copy(volunteerRotations = it) }
            }
            is WorkspaceCommand.PutFinancialChecklist -> updateQahal(snapshot, command.qahalId) { qahal ->
                val current = qahal.financialControls.firstOrNull { it.id == command.checklist.id }
                val merged = current?.let {
                    command.checklist.copy(
                        status = it.status,
                        statusChangedOn = it.statusChangedOn,
                        controls = it.controls,
                    )
                } ?: command.checklist
                WorkspaceOperationResult.Success(
                    qahal.copy(financialControls = qahal.financialControls.replaceOrAppend(merged) { it.id }),
                )
            }
            is WorkspaceCommand.DeleteFinancialChecklist -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.financialControls
                    .removeFirst("qahal.financialControls") { it.id == command.id }
                    .map { qahal.copy(financialControls = it) }
            }
            is WorkspaceCommand.PutFinancialControl -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.financialControls
                    .updateFirst("qahal.financialControls", { it.id == command.checklistId }) { checklist ->
                        val current = checklist.controls.firstOrNull { it.id == command.control.id }
                        val merged = current?.let {
                            command.control.copy(status = it.status, statusChangedOn = it.statusChangedOn)
                        } ?: command.control
                        WorkspaceOperationResult.Success(
                            checklist.copy(controls = checklist.controls.replaceOrAppend(merged) { it.id }),
                        )
                    }.map { qahal.copy(financialControls = it) }
            }
            is WorkspaceCommand.DeleteFinancialControl -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.financialControls
                    .updateFirst("qahal.financialControls", { it.id == command.checklistId }) { checklist ->
                        checklist.controls
                            .removeFirst("qahal.financialControls.controls") { it.id == command.id }
                            .map { checklist.copy(controls = it) }
                    }.map { qahal.copy(financialControls = it) }
            }
            is WorkspaceCommand.ChangePracticeStatus -> updateSelf(snapshot, command.selfId) { self ->
                self.practiceAdoptions
                    .updateFirst("self.practiceAdoptions", { it.id == command.id }) { adoption ->
                        WorkspaceLifecycle.transition(
                            adoption = adoption,
                            target = command.target,
                            changedOn = command.changedOn,
                            nextReviewOn = command.nextReviewOn ?: adoption.reviewOn,
                        )
                    }.map { self.copy(practiceAdoptions = it) }
            }
            is WorkspaceCommand.ChangeResponsibilityStatus -> updateHousehold(snapshot, command.householdId) { household ->
                household.responsibilities
                    .updateFirst("household.responsibilities", { it.id == command.id }) { responsibility ->
                        WorkspaceLifecycle.transition(responsibility, command.target, command.changedOn)
                    }.map { household.copy(responsibilities = it) }
            }
            is WorkspaceCommand.ChangeCalendarItemStatus -> updateHousehold(snapshot, command.householdId) { household ->
                household.calendarItems
                    .updateFirst("household.calendarItems", { it.id == command.id }) { item ->
                        WorkspaceLifecycle.transition(item, command.target, command.changedOn)
                    }.map { household.copy(calendarItems = it) }
            }
            is WorkspaceCommand.ChangePreparationTaskStatus -> updateHousehold(snapshot, command.householdId) { household ->
                household.preparationKits
                    .updateFirst("household.preparationKits", { it.id == command.kitId }) { kit ->
                        kit.tasks
                            .updateFirst("household.preparationKits.tasks", { it.id == command.id }) { task ->
                                WorkspaceLifecycle.transition(task, command.target, command.changedOn)
                            }.map { kit.copy(tasks = it) }
                    }.map { household.copy(preparationKits = it) }
            }
            is WorkspaceCommand.ChangePreparationKitStatus -> updateHousehold(snapshot, command.householdId) { household ->
                household.preparationKits
                    .updateFirst("household.preparationKits", { it.id == command.id }) { kit ->
                        WorkspaceLifecycle.transition(kit, command.target, command.changedOn)
                    }.map { household.copy(preparationKits = it) }
            }
            is WorkspaceCommand.ChangeDecisionStatus -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.decisions
                    .updateFirst("qahal.decisions", { it.id == command.id }) { decision ->
                        WorkspaceLifecycle.transition(
                            decision = decision,
                            target = command.target,
                            changedOn = command.changedOn,
                            nextReviewOn = command.nextReviewOn ?: decision.reviewOn,
                        )
                    }.map { qahal.copy(decisions = it) }
            }
            is WorkspaceCommand.ChangeVolunteerSlotStatus -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.volunteerRotations
                    .updateFirst("qahal.volunteerRotations", { it.id == command.rotationId }) { rotation ->
                        rotation.slots
                            .updateFirst("qahal.volunteerRotations.slots", { it.id == command.id }) { slot ->
                                WorkspaceLifecycle.transition(
                                    slot = slot,
                                    target = command.target,
                                    changedOn = command.changedOn,
                                    assignedTo = command.assignedTo ?: slot.assignedTo,
                                )
                            }.map { rotation.copy(slots = it) }
                    }.map { qahal.copy(volunteerRotations = it) }
            }
            is WorkspaceCommand.ChangeVolunteerRotationStatus -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.volunteerRotations
                    .updateFirst("qahal.volunteerRotations", { it.id == command.id }) { rotation ->
                        WorkspaceLifecycle.transition(rotation, command.target, command.changedOn)
                    }.map { qahal.copy(volunteerRotations = it) }
            }
            is WorkspaceCommand.ChangeInventoryStatus -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.inventory
                    .updateFirst("qahal.inventory", { it.id == command.id }) { item ->
                        WorkspaceLifecycle.transition(item, command.target, command.changedOn)
                    }.map { qahal.copy(inventory = it) }
            }
            is WorkspaceCommand.ChangeFinancialControlStatus -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.financialControls
                    .updateFirst("qahal.financialControls", { it.id == command.checklistId }) { checklist ->
                        checklist.controls
                            .updateFirst("qahal.financialControls.controls", { it.id == command.id }) { control ->
                                WorkspaceLifecycle.transition(control, command.target, command.changedOn)
                            }.map { checklist.copy(controls = it) }
                    }.map { qahal.copy(financialControls = it) }
            }
            is WorkspaceCommand.ChangeFinancialChecklistStatus -> updateQahal(snapshot, command.qahalId) { qahal ->
                qahal.financialControls
                    .updateFirst("qahal.financialControls", { it.id == command.id }) { checklist ->
                        WorkspaceLifecycle.transition(
                            checklist = checklist,
                            target = command.target,
                            changedOn = command.changedOn,
                            nextReviewOn = command.nextReviewOn ?: checklist.reviewOn,
                        )
                    }.map { qahal.copy(financialControls = it) }
            }
        }
        return when (candidate) {
            is WorkspaceOperationResult.Rejected -> WorkspaceUpdateResult.Rejected(snapshot, candidate.issues)
            is WorkspaceOperationResult.Success -> {
                val issues = WorkspaceValidator.validate(candidate.value)
                if (issues.isEmpty()) {
                    WorkspaceUpdateResult.Applied(candidate.value)
                } else {
                    WorkspaceUpdateResult.Rejected(snapshot, issues)
                }
            }
        }
    }

    private fun updateHousehold(
        snapshot: WorkspaceSnapshot,
        id: WorkspaceId,
        update: (HouseholdWorkspace) -> WorkspaceOperationResult<HouseholdWorkspace>,
    ): WorkspaceOperationResult<WorkspaceSnapshot> = snapshot.households
        .updateFirst("households", { it.id == id }, update)
        .map { snapshot.copy(households = it) }

    private fun updateSelf(
        snapshot: WorkspaceSnapshot,
        id: WorkspaceId,
        update: (SelfWorkspace) -> WorkspaceOperationResult<SelfWorkspace>,
    ): WorkspaceOperationResult<WorkspaceSnapshot> = snapshot.selfWorkspaces
        .updateFirst("selfWorkspaces", { it.id == id }, update)
        .map { snapshot.copy(selfWorkspaces = it) }

    private fun updateQahal(
        snapshot: WorkspaceSnapshot,
        id: WorkspaceId,
        update: (QahalWorkspace) -> WorkspaceOperationResult<QahalWorkspace>,
    ): WorkspaceOperationResult<WorkspaceSnapshot> = snapshot.qahalWorkspaces
        .updateFirst("qahalWorkspaces", { it.id == id }, update)
        .map { snapshot.copy(qahalWorkspaces = it) }

    private inline fun <T> List<T>.updateFirst(
        path: String,
        predicate: (T) -> Boolean,
        update: (T) -> WorkspaceOperationResult<T>,
    ): WorkspaceOperationResult<List<T>> {
        val index = indexOfFirst(predicate)
        if (index < 0) {
            return WorkspaceOperationResult.Rejected(
                listOf(
                    WorkspaceIssue(
                        WorkspaceIssueCode.RECORD_NOT_FOUND,
                        path,
                        "The requested local record was not found.",
                    ),
                ),
            )
        }
        return update(this[index]).map { changed ->
            toMutableList().also { it[index] = changed }.toList()
        }
    }

    private inline fun <T, K> List<T>.replaceOrAppend(
        item: T,
        id: (T) -> K,
    ): List<T> {
        val index = indexOfFirst { existing -> id(existing) == id(item) }
        if (index < 0) return this + item
        return toMutableList().also { it[index] = item }.toList()
    }

    private inline fun <T> List<T>.removeFirst(
        path: String,
        predicate: (T) -> Boolean,
    ): WorkspaceOperationResult<List<T>> {
        val index = indexOfFirst(predicate)
        if (index < 0) {
            return WorkspaceOperationResult.Rejected(
                listOf(
                    WorkspaceIssue(
                        WorkspaceIssueCode.RECORD_NOT_FOUND,
                        path,
                        "The requested local record was not found.",
                    ),
                ),
            )
        }
        return WorkspaceOperationResult.Success(toMutableList().also { it.removeAt(index) }.toList())
    }

    private inline fun <A, B> WorkspaceOperationResult<A>.map(
        transform: (A) -> B,
    ): WorkspaceOperationResult<B> = when (this) {
        is WorkspaceOperationResult.Rejected -> this
        is WorkspaceOperationResult.Success -> WorkspaceOperationResult.Success(transform(value))
    }
}
