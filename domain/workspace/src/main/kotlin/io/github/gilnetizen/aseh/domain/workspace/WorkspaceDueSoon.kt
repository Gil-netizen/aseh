package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate

enum class WorkspaceArea {
    SELF,
    HOUSEHOLD,
    QAHAL,
}

enum class DueSoonKind {
    PRACTICE_REVIEW,
    HOUSEHOLD_RESPONSIBILITY,
    HOUSEHOLD_CALENDAR_ITEM,
    PREPARATION_KIT_TARGET,
    PREPARATION_KIT_TASK,
    QAHAL_DECISION_REVIEW,
    VOLUNTEER_SLOT,
    INVENTORY_ATTENTION,
    FINANCIAL_CONTROL,
    FINANCIAL_CHECKLIST_REVIEW,
}

enum class DueTiming {
    OVERDUE,
    TODAY,
    UPCOMING,
}

data class DueSoonItem(
    val area: WorkspaceArea,
    val kind: DueSoonKind,
    val workspaceId: WorkspaceId? = null,
    val recordId: WorkspaceRecordId,
    val parentRecordId: WorkspaceRecordId? = null,
    /** Unformatted domain label; the UI supplies localized surrounding text. */
    val label: String,
    val dueOn: LocalDate,
    val timing: DueTiming,
)

data class DueSoonSummary(
    val asOf: LocalDate,
    val through: LocalDate,
    val items: List<DueSoonItem>,
) {
    val overdueCount: Int = items.count { it.timing == DueTiming.OVERDUE }
    val todayCount: Int = items.count { it.timing == DueTiming.TODAY }
    val upcomingCount: Int = items.count { it.timing == DueTiming.UPCOMING }
}

/** Produces stable Now-screen input from explicit local dates and immutable local state. */
object WorkspaceDueSoon {
    const val MAX_HORIZON_DAYS: Long = 365

    fun summarize(
        snapshot: WorkspaceSnapshot,
        asOf: LocalDate,
        horizonDays: Long = 7,
    ): WorkspaceOperationResult<DueSoonSummary> {
        if (horizonDays !in 0..MAX_HORIZON_DAYS) {
            return WorkspaceOperationResult.Rejected(
                listOf(
                    WorkspaceIssue(
                        WorkspaceIssueCode.INVALID_DATE_RANGE,
                        "horizonDays",
                        "The due-soon horizon must be between 0 and $MAX_HORIZON_DAYS days.",
                    ),
                ),
            )
        }
        val validationIssues = WorkspaceValidator.validate(snapshot)
        if (validationIssues.isNotEmpty()) return WorkspaceOperationResult.Rejected(validationIssues)

        val through = asOf.plusDays(horizonDays)
        val items = buildList {
            snapshot.selfWorkspaces.forEach { self ->
                self.practiceAdoptions
                    .filter { it.status in setOf(PracticeAdoptionStatus.ADOPTED, PracticeAdoptionStatus.PAUSED) }
                    .forEach { adoption ->
                        adoption.reviewOn?.let { dueOn ->
                            addIfDue(
                                through,
                                DueSoonItem(
                                    area = WorkspaceArea.SELF,
                                    kind = DueSoonKind.PRACTICE_REVIEW,
                                    workspaceId = self.id,
                                    recordId = adoption.id,
                                    label = adoption.label,
                                    dueOn = dueOn,
                                    timing = dueOn.timing(asOf),
                                ),
                            )
                        }
                    }
            }

            snapshot.households.forEach { household ->
                household.responsibilities
                    .filter { it.status in setOf(WorkItemStatus.TODO, WorkItemStatus.IN_PROGRESS) }
                    .forEach { responsibility ->
                        addIfDue(
                            through,
                            DueSoonItem(
                                area = WorkspaceArea.HOUSEHOLD,
                                kind = DueSoonKind.HOUSEHOLD_RESPONSIBILITY,
                                workspaceId = household.id,
                                recordId = responsibility.id,
                                label = responsibility.label,
                                dueOn = responsibility.dueOn,
                                timing = responsibility.dueOn.timing(asOf),
                            ),
                        )
                    }
                household.calendarItems
                    .filter { it.status == CalendarItemStatus.SCHEDULED }
                    .forEach { calendarItem ->
                        addIfDue(
                            through,
                            DueSoonItem(
                                area = WorkspaceArea.HOUSEHOLD,
                                kind = DueSoonKind.HOUSEHOLD_CALENDAR_ITEM,
                                workspaceId = household.id,
                                recordId = calendarItem.id,
                                label = calendarItem.label,
                                dueOn = calendarItem.startsOn,
                                timing = calendarItem.startsOn.timing(asOf),
                            ),
                        )
                    }
                household.preparationKits
                    .filter { it.status == PreparationKitStatus.ACTIVE }
                    .forEach { kit ->
                        addIfDue(
                            through,
                            DueSoonItem(
                                area = WorkspaceArea.HOUSEHOLD,
                                kind = DueSoonKind.PREPARATION_KIT_TARGET,
                                workspaceId = household.id,
                                recordId = kit.id,
                                label = kit.label,
                                dueOn = kit.targetOn,
                                timing = kit.targetOn.timing(asOf),
                            ),
                        )
                        kit.tasks
                            .filter { it.status in setOf(WorkItemStatus.TODO, WorkItemStatus.IN_PROGRESS) }
                            .forEach { task ->
                                addIfDue(
                                    through,
                                    DueSoonItem(
                                        area = WorkspaceArea.HOUSEHOLD,
                                        kind = DueSoonKind.PREPARATION_KIT_TASK,
                                        workspaceId = household.id,
                                        recordId = task.id,
                                        parentRecordId = kit.id,
                                        label = task.label,
                                        dueOn = task.dueOn,
                                        timing = task.dueOn.timing(asOf),
                                    ),
                                )
                            }
                    }
            }

            snapshot.qahalWorkspaces.forEach { qahal ->
                qahal.decisions
                    .filter { it.status == QahalDecisionStatus.ADOPTED }
                    .forEach { decision ->
                        decision.reviewOn?.let { dueOn ->
                            addIfDue(
                                through,
                                DueSoonItem(
                                    area = WorkspaceArea.QAHAL,
                                    kind = DueSoonKind.QAHAL_DECISION_REVIEW,
                                    workspaceId = qahal.id,
                                    recordId = decision.id,
                                    label = decision.label,
                                    dueOn = dueOn,
                                    timing = dueOn.timing(asOf),
                                ),
                            )
                        }
                    }
                qahal.volunteerRotations
                    .filter { it.status == VolunteerRotationStatus.ACTIVE }
                    .forEach { rotation ->
                        rotation.slots
                            .filter { it.status in setOf(VolunteerSlotStatus.OPEN, VolunteerSlotStatus.ASSIGNED) }
                            .forEach { slot ->
                                addIfDue(
                                    through,
                                    DueSoonItem(
                                        area = WorkspaceArea.QAHAL,
                                        kind = DueSoonKind.VOLUNTEER_SLOT,
                                        workspaceId = qahal.id,
                                        recordId = slot.id,
                                        parentRecordId = rotation.id,
                                        label = rotation.label,
                                        dueOn = slot.serviceOn,
                                        timing = slot.serviceOn.timing(asOf),
                                    ),
                                )
                            }
                    }
                qahal.inventory
                    .filter { item ->
                        item.status == InventoryItemStatus.OUT_OF_SERVICE ||
                            (item.status == InventoryItemStatus.ACTIVE && item.quantityOnHand <= item.minimumDesired)
                    }
                    .forEach { item ->
                        val dueOn = item.nextCheckOn ?: asOf
                        addIfDue(
                            through,
                            DueSoonItem(
                                area = WorkspaceArea.QAHAL,
                                kind = DueSoonKind.INVENTORY_ATTENTION,
                                workspaceId = qahal.id,
                                recordId = item.id,
                                label = item.label,
                                dueOn = dueOn,
                                timing = dueOn.timing(asOf),
                            ),
                        )
                    }
                qahal.financialControls
                    .filter { it.status == FinancialChecklistStatus.ACTIVE }
                    .forEach { checklist ->
                        checklist.reviewOn?.let { dueOn ->
                            addIfDue(
                                through,
                                DueSoonItem(
                                    area = WorkspaceArea.QAHAL,
                                    kind = DueSoonKind.FINANCIAL_CHECKLIST_REVIEW,
                                    workspaceId = qahal.id,
                                    recordId = checklist.id,
                                    label = checklist.label,
                                    dueOn = dueOn,
                                    timing = dueOn.timing(asOf),
                                ),
                            )
                        }
                        checklist.controls
                            .filter { it.status == FinancialControlStatus.PENDING && it.dueOn != null }
                            .forEach { control ->
                                val dueOn = requireNotNull(control.dueOn)
                                addIfDue(
                                    through,
                                    DueSoonItem(
                                        area = WorkspaceArea.QAHAL,
                                        kind = DueSoonKind.FINANCIAL_CONTROL,
                                        workspaceId = qahal.id,
                                        recordId = control.id,
                                        parentRecordId = checklist.id,
                                        label = control.kind.name,
                                        dueOn = dueOn,
                                        timing = dueOn.timing(asOf),
                                    ),
                                )
                            }
                    }
            }
        }.sortedWith(
            compareBy(
                DueSoonItem::dueOn,
                { it.area.ordinal },
                { it.kind.ordinal },
                { it.workspaceId?.value.orEmpty() },
                { it.parentRecordId?.value.orEmpty() },
                { it.recordId.value },
            ),
        )
        return WorkspaceOperationResult.Success(DueSoonSummary(asOf, through, items))
    }

    private fun MutableList<DueSoonItem>.addIfDue(through: LocalDate, item: DueSoonItem) {
        if (item.dueOn <= through) add(item)
    }

    private fun LocalDate.timing(asOf: LocalDate): DueTiming = when {
        this < asOf -> DueTiming.OVERDUE
        this == asOf -> DueTiming.TODAY
        else -> DueTiming.UPCOMING
    }
}
