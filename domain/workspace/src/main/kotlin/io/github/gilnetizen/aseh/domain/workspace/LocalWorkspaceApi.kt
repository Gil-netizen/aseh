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

    private inline fun <A, B> WorkspaceOperationResult<A>.map(
        transform: (A) -> B,
    ): WorkspaceOperationResult<B> = when (this) {
        is WorkspaceOperationResult.Rejected -> this
        is WorkspaceOperationResult.Success -> WorkspaceOperationResult.Success(transform(value))
    }
}
