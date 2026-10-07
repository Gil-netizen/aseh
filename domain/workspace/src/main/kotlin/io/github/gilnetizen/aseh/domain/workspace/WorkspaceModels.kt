package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate

@JvmInline
value class WorkspaceId(val value: String)

@JvmInline
value class WorkspaceRecordId(val value: String)

@JvmInline
value class LocalPersonId(val value: String)

@JvmInline
value class PublicRoleId(val value: String)

/**
 * An immutable pointer into one exact, verified content-pack edition.
 *
 * User records keep this identity rather than copying corpus text or silently following a newer
 * edition. Resolution and pack availability belong outside this pure domain module.
 */
data class VerifiedContentReference(
    val packId: String,
    val packVersion: String,
    val editionId: String,
    val sourceUnitId: String,
)

data class PracticeCardReference(
    val practiceCardId: String,
    val content: VerifiedContentReference,
)

enum class PracticeAdoptionStatus {
    DRAFT,
    ADOPTED,
    PAUSED,
    RETIRED,
}

/** A local choice. It does not change the cited card's evidence or editorial status. */
data class PersonalPracticeAdoption(
    val id: WorkspaceRecordId,
    val practice: PracticeCardReference,
    val label: String,
    val status: PracticeAdoptionStatus,
    val adoptedOn: LocalDate? = null,
    val reviewOn: LocalDate? = null,
    val statusChangedOn: LocalDate,
)

data class SelfWorkspace(
    val id: WorkspaceId,
    val label: String,
    val practiceAdoptions: List<PersonalPracticeAdoption> = emptyList(),
)

enum class WorkItemStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED,
}

data class HouseholdResponsibility(
    val id: WorkspaceRecordId,
    val label: String,
    val dueOn: LocalDate,
    val assignedTo: LocalPersonId? = null,
    val status: WorkItemStatus = WorkItemStatus.TODO,
    val statusChangedOn: LocalDate,
)

enum class HouseholdCalendarKind {
    PREPARATION,
    SHARED_MEAL,
    PRAYER,
    STUDY,
    CARE,
    HOUSEHOLD_MEETING,
    OTHER,
}

enum class CalendarItemStatus {
    DRAFT,
    SCHEDULED,
    COMPLETED,
    CANCELLED,
}

data class HouseholdCalendarItem(
    val id: WorkspaceRecordId,
    val label: String,
    val kind: HouseholdCalendarKind,
    val startsOn: LocalDate,
    val endsOn: LocalDate = startsOn,
    val status: CalendarItemStatus = CalendarItemStatus.DRAFT,
    val statusChangedOn: LocalDate,
)

data class PreparationKitTask(
    val id: WorkspaceRecordId,
    val label: String,
    val dueOn: LocalDate,
    val assignedTo: LocalPersonId? = null,
    val status: WorkItemStatus = WorkItemStatus.TODO,
    val statusChangedOn: LocalDate,
)

enum class PreparationKitStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    CANCELLED,
}

data class HouseholdPreparationKit(
    val id: WorkspaceRecordId,
    val label: String,
    val targetOn: LocalDate,
    val status: PreparationKitStatus = PreparationKitStatus.DRAFT,
    val statusChangedOn: LocalDate,
    val tasks: List<PreparationKitTask> = emptyList(),
)

data class HouseholdWorkspace(
    val id: WorkspaceId,
    val label: String,
    val responsibilities: List<HouseholdResponsibility> = emptyList(),
    val calendarItems: List<HouseholdCalendarItem> = emptyList(),
    val preparationKits: List<HouseholdPreparationKit> = emptyList(),
)

enum class DecisionClassification {
    JUDGMENT,
    ENACTMENT,
    CUSTOM,
    POLICY,
    ADVICE,
}

enum class QahalDecisionStatus {
    DRAFT,
    ADOPTED,
    SUPERSEDED,
    RETIRED,
}

/**
 * A qahal's local operating decision. Adoption grants no universal, civil, or denominational force.
 * Private evidence and high-consequence narratives do not belong in this record.
 */
data class QahalDecision(
    val id: WorkspaceRecordId,
    val label: String,
    val classification: DecisionClassification,
    val publicSummary: String,
    val authorityScope: String,
    val status: QahalDecisionStatus = QahalDecisionStatus.DRAFT,
    val effectiveOn: LocalDate? = null,
    val reviewOn: LocalDate? = null,
    val statusChangedOn: LocalDate,
    val sourceReferences: List<VerifiedContentReference> = emptyList(),
    val dissentSummary: String? = null,
)

enum class VolunteerSlotStatus {
    OPEN,
    ASSIGNED,
    COMPLETED,
    CANCELLED,
}

data class VolunteerSlot(
    val id: WorkspaceRecordId,
    val serviceOn: LocalDate,
    val assignedTo: LocalPersonId? = null,
    val status: VolunteerSlotStatus = VolunteerSlotStatus.OPEN,
    val statusChangedOn: LocalDate,
)

enum class VolunteerRotationStatus {
    DRAFT,
    ACTIVE,
    CLOSED,
    CANCELLED,
}

data class VolunteerRotation(
    val id: WorkspaceRecordId,
    val label: String,
    val publicRole: PublicRoleId,
    val status: VolunteerRotationStatus = VolunteerRotationStatus.DRAFT,
    val statusChangedOn: LocalDate,
    val slots: List<VolunteerSlot> = emptyList(),
)

enum class InventoryItemStatus {
    ACTIVE,
    OUT_OF_SERVICE,
    RETIRED,
}

data class InventoryItem(
    val id: WorkspaceRecordId,
    val label: String,
    val quantityOnHand: Int,
    val minimumDesired: Int,
    val nextCheckOn: LocalDate? = null,
    val status: InventoryItemStatus = InventoryItemStatus.ACTIVE,
    val statusChangedOn: LocalDate,
)

enum class FinancialControlKind {
    DOCUMENTED_AUTHORITY,
    DUAL_APPROVAL,
    SEPARATION_OF_ROLES,
    RECEIPT_RETENTION,
    PERIODIC_RECONCILIATION,
    INDEPENDENT_REVIEW,
    CONFLICT_DISCLOSURE,
    MEMBER_REPORTING,
}

enum class FinancialControlStatus {
    PENDING,
    VERIFIED,
    NOT_APPLICABLE,
}

data class FinancialControlItem(
    val id: WorkspaceRecordId,
    val kind: FinancialControlKind,
    val status: FinancialControlStatus = FinancialControlStatus.PENDING,
    val dueOn: LocalDate? = null,
    val responsibleRole: PublicRoleId? = null,
    val statusChangedOn: LocalDate,
)

enum class FinancialChecklistStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    RETIRED,
}

/**
 * Procedure-only financial controls. Deliberately contains no amount, balance, account,
 * credential, payment instruction, disbursement record, or transfer operation.
 */
data class FinancialControlChecklist(
    val id: WorkspaceRecordId,
    val label: String,
    val status: FinancialChecklistStatus = FinancialChecklistStatus.DRAFT,
    val reviewOn: LocalDate? = null,
    val statusChangedOn: LocalDate,
    val controls: List<FinancialControlItem> = emptyList(),
)

data class QahalWorkspace(
    val id: WorkspaceId,
    val label: String,
    val decisions: List<QahalDecision> = emptyList(),
    val volunteerRotations: List<VolunteerRotation> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val financialControls: List<FinancialControlChecklist> = emptyList(),
)

/** Immutable, persistence-agnostic state for all three Build workspaces. */
data class WorkspaceSnapshot(
    val selfWorkspaces: List<SelfWorkspace> = emptyList(),
    val households: List<HouseholdWorkspace> = emptyList(),
    val qahalWorkspaces: List<QahalWorkspace> = emptyList(),
)

/** Stable identity for opening one exact workspace record from device-local search. */
enum class WorkspaceRecordKind {
    WORKSPACE,
    PERSONAL_PRACTICE,
    HOUSEHOLD_RESPONSIBILITY,
    HOUSEHOLD_CALENDAR_ITEM,
    HOUSEHOLD_PREPARATION_KIT,
    PREPARATION_KIT_TASK,
    QAHAL_DECISION,
    VOLUNTEER_ROTATION,
    VOLUNTEER_SLOT,
    INVENTORY_ITEM,
    FINANCIAL_CHECKLIST,
    FINANCIAL_CONTROL,
}

data class WorkspaceRecordAddress(
    val workspaceId: WorkspaceId,
    val kind: WorkspaceRecordKind,
    val recordId: WorkspaceRecordId? = null,
    val parentRecordId: WorkspaceRecordId? = null,
) {
    init {
        require((kind == WorkspaceRecordKind.WORKSPACE) == (recordId == null)) {
            "Only a workspace search target may omit a record ID"
        }
        require(
            parentRecordId == null || kind in setOf(
                WorkspaceRecordKind.PREPARATION_KIT_TASK,
                WorkspaceRecordKind.VOLUNTEER_SLOT,
                WorkspaceRecordKind.FINANCIAL_CONTROL,
            ),
        ) { "Only a nested workspace record may include a parent ID" }
    }
}
