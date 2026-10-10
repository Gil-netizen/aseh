package io.github.gilnetizen.aseh.domain.workspace

enum class WorkspaceIssueCode {
    REQUIRED_FIELD,
    DUPLICATE_ID,
    DUPLICATE_CONTROL_KIND,
    INVALID_DATE_RANGE,
    INVALID_QUANTITY,
    STATUS_REQUIREMENT_NOT_MET,
    ILLEGAL_TRANSITION,
    NON_MONOTONIC_STATUS_DATE,
    NO_STATE_CHANGE,
    RECORD_NOT_FOUND,
}

data class WorkspaceIssue(
    val code: WorkspaceIssueCode,
    val path: String,
    /** Developer-facing detail. Product UI should localize from [code] and [path]. */
    val detail: String,
)

object WorkspaceValidator {
    fun validate(snapshot: WorkspaceSnapshot): List<WorkspaceIssue> = buildList {
        duplicateIds("selfWorkspaces", snapshot.selfWorkspaces.map { it.id.value })
        snapshot.selfWorkspaces.forEachIndexed { index, self ->
            val path = "selfWorkspaces[$index]"
            required("$path.id", self.id.value)
            required("$path.label", self.label)
            duplicateIds("$path.practiceAdoptions", self.practiceAdoptions.map { it.id.value })
            self.practiceAdoptions.forEachIndexed { childIndex, adoption ->
                addAll(validate(adoption, "$path.practiceAdoptions[$childIndex]"))
            }
        }

        duplicateIds("households", snapshot.households.map { it.id.value })
        snapshot.households.forEachIndexed { index, household ->
            val path = "households[$index]"
            required("$path.id", household.id.value)
            required("$path.label", household.label)
            duplicateIds("$path.responsibilities", household.responsibilities.map { it.id.value })
            duplicateIds("$path.calendarItems", household.calendarItems.map { it.id.value })
            duplicateIds("$path.preparationKits", household.preparationKits.map { it.id.value })
            household.responsibilities.forEachIndexed { childIndex, responsibility ->
                addAll(validate(responsibility, "$path.responsibilities[$childIndex]"))
            }
            household.calendarItems.forEachIndexed { childIndex, item ->
                addAll(validate(item, "$path.calendarItems[$childIndex]"))
            }
            household.preparationKits.forEachIndexed { childIndex, kit ->
                addAll(validate(kit, "$path.preparationKits[$childIndex]"))
            }
        }

        duplicateIds("qahalWorkspaces", snapshot.qahalWorkspaces.map { it.id.value })
        snapshot.qahalWorkspaces.forEachIndexed { index, qahal ->
            val path = "qahalWorkspaces[$index]"
            required("$path.id", qahal.id.value)
            required("$path.label", qahal.label)
            duplicateIds("$path.decisions", qahal.decisions.map { it.id.value })
            duplicateIds("$path.volunteerRotations", qahal.volunteerRotations.map { it.id.value })
            duplicateIds("$path.inventory", qahal.inventory.map { it.id.value })
            duplicateIds("$path.financialControls", qahal.financialControls.map { it.id.value })
            qahal.decisions.forEachIndexed { childIndex, decision ->
                addAll(validate(decision, "$path.decisions[$childIndex]"))
            }
            qahal.volunteerRotations.forEachIndexed { childIndex, rotation ->
                addAll(validate(rotation, "$path.volunteerRotations[$childIndex]"))
            }
            qahal.inventory.forEachIndexed { childIndex, item ->
                addAll(validate(item, "$path.inventory[$childIndex]"))
            }
            qahal.financialControls.forEachIndexed { childIndex, checklist ->
                addAll(validate(checklist, "$path.financialControls[$childIndex]"))
            }
        }
    }.sortedWith(compareBy(WorkspaceIssue::path, { it.code.name }, WorkspaceIssue::detail))

    internal fun validate(
        adoption: PersonalPracticeAdoption,
        path: String = "personalPractice",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", adoption.id.value)
        required("$path.label", adoption.label)
        required("$path.practice.practiceCardId", adoption.practice.practiceCardId)
        addAll(validate(adoption.practice.content, "$path.practice.content"))
        if (adoption.status == PracticeAdoptionStatus.DRAFT && adoption.adoptedOn != null) {
            statusIssue("$path.adoptedOn", "A draft practice cannot have an adoption date.")
        }
        if (adoption.status in setOf(PracticeAdoptionStatus.ADOPTED, PracticeAdoptionStatus.PAUSED)) {
            if (adoption.adoptedOn == null) {
                statusIssue("$path.adoptedOn", "An adopted or paused practice requires its adoption date.")
            }
            if (adoption.reviewOn == null) {
                statusIssue("$path.reviewOn", "An adopted or paused practice requires a review date.")
            }
        }
        if (adoption.adoptedOn != null && adoption.reviewOn != null && adoption.reviewOn < adoption.adoptedOn) {
            dateIssue("$path.reviewOn", "The review date cannot precede the adoption date.")
        }
    }

    internal fun validate(
        responsibility: HouseholdResponsibility,
        path: String = "householdResponsibility",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", responsibility.id.value)
        required("$path.label", responsibility.label)
        responsibility.assignedTo?.let { required("$path.assignedTo", it.value) }
    }

    internal fun validate(
        item: HouseholdCalendarItem,
        path: String = "householdCalendarItem",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", item.id.value)
        required("$path.label", item.label)
        if (item.endsOn < item.startsOn) {
            dateIssue("$path.endsOn", "A calendar item cannot end before it starts.")
        }
    }

    internal fun validate(
        task: PreparationKitTask,
        path: String = "preparationKitTask",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", task.id.value)
        required("$path.label", task.label)
        task.assignedTo?.let { required("$path.assignedTo", it.value) }
    }

    internal fun validate(
        kit: HouseholdPreparationKit,
        path: String = "householdPreparationKit",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", kit.id.value)
        required("$path.label", kit.label)
        duplicateIds("$path.tasks", kit.tasks.map { it.id.value })
        kit.tasks.forEachIndexed { index, task -> addAll(validate(task, "$path.tasks[$index]")) }
        if (kit.status == PreparationKitStatus.ACTIVE && kit.tasks.isEmpty()) {
            statusIssue("$path.tasks", "An active preparation kit requires at least one task.")
        }
        if (
            kit.status == PreparationKitStatus.COMPLETED &&
            kit.tasks.any { it.status !in setOf(WorkItemStatus.DONE, WorkItemStatus.CANCELLED) }
        ) {
            statusIssue("$path.status", "A preparation kit can be completed only after every task is resolved.")
        }
    }

    internal fun validate(
        decision: QahalDecision,
        path: String = "qahalDecision",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", decision.id.value)
        required("$path.label", decision.label)
        decision.dissentSummary?.let { required("$path.dissentSummary", it) }
        decision.sourceReferences.forEachIndexed { index, reference ->
            addAll(validate(reference, "$path.sourceReferences[$index]"))
        }
        if (decision.status == QahalDecisionStatus.DRAFT && decision.effectiveOn != null) {
            statusIssue("$path.effectiveOn", "A draft decision cannot be effective.")
        }
        if (decision.status in setOf(QahalDecisionStatus.ADOPTED, QahalDecisionStatus.SUPERSEDED)) {
            required("$path.publicSummary", decision.publicSummary)
            required("$path.authorityScope", decision.authorityScope)
            if (decision.effectiveOn == null) {
                statusIssue("$path.effectiveOn", "An adopted or superseded decision requires an effective date.")
            }
            if (decision.reviewOn == null) {
                statusIssue("$path.reviewOn", "An adopted or superseded decision requires a review date.")
            }
        }
        if (decision.effectiveOn != null && decision.reviewOn != null && decision.reviewOn < decision.effectiveOn) {
            dateIssue("$path.reviewOn", "The review date cannot precede the effective date.")
        }
    }

    internal fun validate(
        slot: VolunteerSlot,
        path: String = "volunteerSlot",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", slot.id.value)
        slot.assignedTo?.let { required("$path.assignedTo", it.value) }
        if (slot.status in setOf(VolunteerSlotStatus.ASSIGNED, VolunteerSlotStatus.COMPLETED) && slot.assignedTo == null) {
            statusIssue("$path.assignedTo", "An assigned or completed slot requires a local person reference.")
        }
        if (slot.status == VolunteerSlotStatus.OPEN && slot.assignedTo != null) {
            statusIssue("$path.assignedTo", "An open slot cannot retain an assignee.")
        }
    }

    internal fun validate(
        rotation: VolunteerRotation,
        path: String = "volunteerRotation",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", rotation.id.value)
        required("$path.label", rotation.label)
        required("$path.publicRole", rotation.publicRole.value)
        duplicateIds("$path.slots", rotation.slots.map { it.id.value })
        rotation.slots.forEachIndexed { index, slot -> addAll(validate(slot, "$path.slots[$index]")) }
        if (rotation.status == VolunteerRotationStatus.ACTIVE && rotation.slots.isEmpty()) {
            statusIssue("$path.slots", "An active volunteer rotation requires at least one slot.")
        }
        if (
            rotation.status == VolunteerRotationStatus.CLOSED &&
            rotation.slots.any { it.status !in setOf(VolunteerSlotStatus.COMPLETED, VolunteerSlotStatus.CANCELLED) }
        ) {
            statusIssue("$path.status", "A rotation can close only after every slot is resolved.")
        }
    }

    internal fun validate(
        item: InventoryItem,
        path: String = "inventoryItem",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", item.id.value)
        required("$path.label", item.label)
        if (item.quantityOnHand < 0) {
            quantityIssue("$path.quantityOnHand", "Inventory quantity cannot be negative.")
        }
        if (item.minimumDesired < 0) {
            quantityIssue("$path.minimumDesired", "The desired minimum cannot be negative.")
        }
    }

    internal fun validate(
        control: FinancialControlItem,
        path: String = "financialControlItem",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", control.id.value)
        control.responsibleRole?.let { required("$path.responsibleRole", it.value) }
    }

    internal fun validate(
        checklist: FinancialControlChecklist,
        path: String = "financialControlChecklist",
    ): List<WorkspaceIssue> = buildList {
        required("$path.id", checklist.id.value)
        required("$path.label", checklist.label)
        duplicateIds("$path.controls", checklist.controls.map { it.id.value })
        checklist.controls.groupingBy { it.kind }.eachCount()
            .filterValues { count -> count > 1 }
            .keys
            .sortedBy(FinancialControlKind::name)
            .forEach { kind ->
                add(
                    WorkspaceIssue(
                        WorkspaceIssueCode.DUPLICATE_CONTROL_KIND,
                        "$path.controls",
                        "Financial control kind ${kind.name} appears more than once.",
                    ),
                )
            }
        checklist.controls.forEachIndexed { index, control -> addAll(validate(control, "$path.controls[$index]")) }
        if (checklist.status == FinancialChecklistStatus.ACTIVE) {
            if (checklist.controls.isEmpty()) {
                statusIssue("$path.controls", "An active financial-control checklist requires controls.")
            }
            if (checklist.reviewOn == null) {
                statusIssue("$path.reviewOn", "An active financial-control checklist requires a review date.")
            }
        }
        if (
            checklist.status == FinancialChecklistStatus.COMPLETED &&
            checklist.controls.any { it.status == FinancialControlStatus.PENDING }
        ) {
            statusIssue("$path.status", "A financial-control checklist can complete only after every control is addressed.")
        }
    }

    private fun validate(reference: VerifiedContentReference, path: String): List<WorkspaceIssue> = buildList {
        required("$path.packId", reference.packId)
        required("$path.packVersion", reference.packVersion)
        required("$path.editionId", reference.editionId)
        required("$path.sourceUnitId", reference.sourceUnitId)
    }

    private fun MutableList<WorkspaceIssue>.duplicateIds(path: String, ids: List<String>) {
        ids.groupingBy { it }.eachCount()
            .filterValues { count -> count > 1 }
            .keys
            .sorted()
            .forEach { duplicate ->
                add(WorkspaceIssue(WorkspaceIssueCode.DUPLICATE_ID, path, "Duplicate ID: $duplicate"))
            }
    }

    private fun MutableList<WorkspaceIssue>.required(path: String, value: String) {
        if (value.isBlank()) {
            add(WorkspaceIssue(WorkspaceIssueCode.REQUIRED_FIELD, path, "A non-blank value is required."))
        }
    }

    private fun MutableList<WorkspaceIssue>.statusIssue(path: String, detail: String) {
        add(WorkspaceIssue(WorkspaceIssueCode.STATUS_REQUIREMENT_NOT_MET, path, detail))
    }

    private fun MutableList<WorkspaceIssue>.dateIssue(path: String, detail: String) {
        add(WorkspaceIssue(WorkspaceIssueCode.INVALID_DATE_RANGE, path, detail))
    }

    private fun MutableList<WorkspaceIssue>.quantityIssue(path: String, detail: String) {
        add(WorkspaceIssue(WorkspaceIssueCode.INVALID_QUANTITY, path, detail))
    }
}
