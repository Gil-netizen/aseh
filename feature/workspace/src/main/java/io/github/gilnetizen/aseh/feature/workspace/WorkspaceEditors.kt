package io.github.gilnetizen.aseh.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.domain.workspace.CalendarItemStatus
import io.github.gilnetizen.aseh.domain.workspace.DecisionClassification
import io.github.gilnetizen.aseh.domain.workspace.FinancialChecklistStatus
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlKind
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlStatus
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarItem
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarKind
import io.github.gilnetizen.aseh.domain.workspace.HouseholdResponsibility
import io.github.gilnetizen.aseh.domain.workspace.HouseholdPreparationKit
import io.github.gilnetizen.aseh.domain.workspace.HouseholdWorkspace
import io.github.gilnetizen.aseh.domain.workspace.InventoryItem
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.LocalPersonId
import io.github.gilnetizen.aseh.domain.workspace.PreparationKitStatus
import io.github.gilnetizen.aseh.domain.workspace.PublicRoleId
import io.github.gilnetizen.aseh.domain.workspace.QahalDecision
import io.github.gilnetizen.aseh.domain.workspace.QahalDecisionStatus
import io.github.gilnetizen.aseh.domain.workspace.QahalWorkspace
import io.github.gilnetizen.aseh.domain.workspace.PreparationKitTask
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlChecklist
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlItem
import io.github.gilnetizen.aseh.domain.workspace.SelfWorkspace
import io.github.gilnetizen.aseh.domain.workspace.WorkItemStatus
import io.github.gilnetizen.aseh.domain.workspace.VolunteerRotationStatus
import io.github.gilnetizen.aseh.domain.workspace.VolunteerSlotStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.VolunteerRotation
import io.github.gilnetizen.aseh.domain.workspace.VolunteerSlot
import java.time.LocalDate
import java.util.UUID

internal sealed interface WorkspaceEditor {
    data class SelfName(val workspace: SelfWorkspace) : WorkspaceEditor

    data class HouseholdName(val workspace: HouseholdWorkspace) : WorkspaceEditor

    data class QahalName(val workspace: QahalWorkspace) : WorkspaceEditor

    data class Responsibility(
        val workspace: HouseholdWorkspace,
        val item: HouseholdResponsibility? = null,
        val newId: WorkspaceRecordId = newRecordId("responsibility"),
    ) : WorkspaceEditor

    data class CalendarItem(
        val workspace: HouseholdWorkspace,
        val item: HouseholdCalendarItem? = null,
        val newId: WorkspaceRecordId = newRecordId("calendar"),
    ) : WorkspaceEditor

    data class PreparationKit(
        val workspace: HouseholdWorkspace,
        val item: HouseholdPreparationKit? = null,
        val newId: WorkspaceRecordId = newRecordId("preparation-kit"),
    ) : WorkspaceEditor

    data class PreparationTask(
        val workspace: HouseholdWorkspace,
        val kit: HouseholdPreparationKit,
        val item: PreparationKitTask? = null,
        val newId: WorkspaceRecordId = newRecordId("preparation-task"),
    ) : WorkspaceEditor

    data class Decision(
        val workspace: QahalWorkspace,
        val item: QahalDecision? = null,
        val newId: WorkspaceRecordId = newRecordId("decision"),
    ) : WorkspaceEditor

    data class Inventory(
        val workspace: QahalWorkspace,
        val item: InventoryItem? = null,
        val newId: WorkspaceRecordId = newRecordId("inventory"),
    ) : WorkspaceEditor

    data class VolunteerRotationEditor(
        val workspace: QahalWorkspace,
        val item: VolunteerRotation? = null,
        val newId: WorkspaceRecordId = newRecordId("volunteer-rotation"),
    ) : WorkspaceEditor

    data class VolunteerSlotEditor(
        val workspace: QahalWorkspace,
        val rotation: VolunteerRotation,
        val item: VolunteerSlot? = null,
        val newId: WorkspaceRecordId = newRecordId("volunteer-slot"),
    ) : WorkspaceEditor

    data class FinancialChecklistEditor(
        val workspace: QahalWorkspace,
        val item: FinancialControlChecklist? = null,
        val newId: WorkspaceRecordId = newRecordId("financial-checklist"),
    ) : WorkspaceEditor

    data class FinancialControlEditor(
        val workspace: QahalWorkspace,
        val checklist: FinancialControlChecklist,
        val item: FinancialControlItem? = null,
        val newId: WorkspaceRecordId = newRecordId("financial-control"),
    ) : WorkspaceEditor
}

@Composable
internal fun WorkspaceEditorCard(
    editor: WorkspaceEditor,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
    actionsEnabled: Boolean = true,
) {
    CompositionLocalProvider(LocalEditorActionsEnabled provides actionsEnabled) {
      when (editor) {
        is WorkspaceEditor.SelfName -> WorkspaceNameEditor(
            initialLabel = editor.workspace.label,
            onCancel = onCancel,
            onSave = { label ->
                onCommand(WorkspaceEditCommands.rename(editor.workspace, label))
            },
        )
        is WorkspaceEditor.HouseholdName -> WorkspaceNameEditor(
            initialLabel = editor.workspace.label,
            onCancel = onCancel,
            onSave = { label ->
                onCommand(WorkspaceEditCommands.rename(editor.workspace, label))
            },
        )
        is WorkspaceEditor.QahalName -> WorkspaceNameEditor(
            initialLabel = editor.workspace.label,
            onCancel = onCancel,
            onSave = { label ->
                onCommand(WorkspaceEditCommands.rename(editor.workspace, label))
            },
        )
        is WorkspaceEditor.Responsibility -> ResponsibilityEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.CalendarItem -> CalendarItemEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.PreparationKit -> PreparationKitEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.PreparationTask -> PreparationTaskEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.Decision -> DecisionEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.Inventory -> InventoryEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.VolunteerRotationEditor -> VolunteerRotationEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.VolunteerSlotEditor -> VolunteerSlotEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.FinancialChecklistEditor -> FinancialChecklistEditor(editor, asOf, onCancel, onCommand)
        is WorkspaceEditor.FinancialControlEditor -> FinancialControlEditor(editor, asOf, onCancel, onCommand)
      }
    }
}

@Composable
private fun WorkspaceNameEditor(
    initialLabel: String,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
) {
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }
    EditorShell(
        title = stringResource(R.string.workspace_edit_workspace),
        tag = "workspace-editor-name",
        saveEnabled = label.isNotBlank(),
        onCancel = onCancel,
        onSave = { onSave(label) },
    ) {
        RequiredTextField(
            value = label,
            onValueChange = { label = it },
            label = stringResource(R.string.workspace_form_workspace_name),
            tag = "workspace-field-name",
        )
    }
}

@Composable
private fun ResponsibilityEditor(
    editor: WorkspaceEditor.Responsibility,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var dueOn by remember(existing, asOf) { mutableStateOf(existing?.dueOn?.toString() ?: asOf.toString()) }
    var assignedTo by remember(existing) { mutableStateOf(existing?.assignedTo?.value.orEmpty()) }
    val dueDate = dueOn.strictDateOrNull()
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_responsibility else R.string.workspace_edit_responsibility,
        ),
        tag = "workspace-editor-responsibility",
        saveEnabled = label.isNotBlank() && dueDate != null,
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deleteResponsibility(editor.workspace, it.id)) }
        },
        onSave = {
            val saved = HouseholdResponsibility(
                id = existing?.id ?: editor.newId,
                label = label.trim(),
                dueOn = requireNotNull(dueDate),
                assignedTo = assignedTo.trim().takeIf(String::isNotEmpty)?.let(::LocalPersonId),
                status = existing?.status ?: WorkItemStatus.TODO,
                statusChangedOn = existing?.statusChangedOn ?: asOf,
            )
            onCommand(WorkspaceEditCommands.putResponsibility(editor.workspace, saved))
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        DateTextField(dueOn, { dueOn = it }, stringResource(R.string.workspace_form_due_on), "workspace-field-date")
        OutlinedTextField(
            value = assignedTo,
            onValueChange = { assignedTo = it },
            label = { Text(stringResource(R.string.workspace_form_assigned_to)) },
            supportingText = { Text(stringResource(R.string.workspace_form_optional_local_name)) },
            modifier = Modifier.fillMaxWidth().testTag("workspace-field-assigned-to"),
            singleLine = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarItemEditor(
    editor: WorkspaceEditor.CalendarItem,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var kind by remember(existing) { mutableStateOf(existing?.kind ?: HouseholdCalendarKind.OTHER) }
    var startsOn by remember(existing, asOf) { mutableStateOf(existing?.startsOn?.toString() ?: asOf.toString()) }
    var endsOn by remember(existing, asOf) { mutableStateOf(existing?.endsOn?.toString() ?: asOf.toString()) }
    val startDate = startsOn.strictDateOrNull()
    val endDate = endsOn.strictDateOrNull()
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_calendar else R.string.workspace_edit_calendar,
        ),
        tag = "workspace-editor-calendar",
        saveEnabled = label.isNotBlank() && startDate != null && endDate != null && endDate >= startDate,
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deleteCalendarItem(editor.workspace, it.id)) }
        },
        onSave = {
            val saved = HouseholdCalendarItem(
                id = existing?.id ?: editor.newId,
                label = label.trim(),
                kind = kind,
                startsOn = requireNotNull(startDate),
                endsOn = requireNotNull(endDate),
                status = existing?.status ?: CalendarItemStatus.DRAFT,
                statusChangedOn = existing?.statusChangedOn ?: asOf,
            )
            onCommand(WorkspaceEditCommands.putCalendarItem(editor.workspace, saved))
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        Text(stringResource(R.string.workspace_form_kind), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HouseholdCalendarKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option,
                    onClick = { kind = option },
                    label = { Text(calendarKindText(option)) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("workspace-field-kind-${option.name.lowercase()}"),
                )
            }
        }
        DateTextField(startsOn, { startsOn = it }, stringResource(R.string.workspace_form_starts_on), "workspace-field-start")
        DateTextField(endsOn, { endsOn = it }, stringResource(R.string.workspace_form_ends_on), "workspace-field-end")
        if (startDate != null && endDate != null && endDate < startDate) {
            FormError(stringResource(R.string.workspace_form_end_before_start))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionEditor(
    editor: WorkspaceEditor.Decision,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var summary by remember(existing) { mutableStateOf(existing?.publicSummary.orEmpty()) }
    var scope by remember(existing) { mutableStateOf(existing?.authorityScope.orEmpty()) }
    var classification by remember(existing) {
        mutableStateOf(existing?.classification ?: DecisionClassification.POLICY)
    }
    var effectiveOn by remember(existing) { mutableStateOf(existing?.effectiveOn?.toString().orEmpty()) }
    var reviewOn by remember(existing) { mutableStateOf(existing?.reviewOn?.toString().orEmpty()) }
    val status = existing?.status ?: QahalDecisionStatus.DRAFT
    val effectiveDate = effectiveOn.optionalDateOrInvalid()
    val reviewDate = reviewOn.optionalDateOrInvalid()
    val effectiveValue = effectiveDate.valueOrNull
    val reviewValue = reviewDate.valueOrNull
    val datesRequired = status in setOf(QahalDecisionStatus.ADOPTED, QahalDecisionStatus.SUPERSEDED)
    val adoptedDatesValid = !datesRequired ||
        (effectiveDate is OptionalDate.Valid && effectiveValue != null &&
            reviewDate is OptionalDate.Valid && reviewValue != null)
    val rangeValid = effectiveValue == null || reviewValue == null || reviewValue >= effectiveValue
    EditorShell(
        title = stringResource(if (existing == null) R.string.workspace_add_decision else R.string.workspace_edit_decision),
        tag = "workspace-editor-decision",
        saveEnabled = label.isNotBlank() && summary.isNotBlank() && scope.isNotBlank() &&
            effectiveDate is OptionalDate.Valid && reviewDate is OptionalDate.Valid && adoptedDatesValid && rangeValid,
        onCancel = onCancel,
        onDelete = existing?.let { { onCommand(WorkspaceEditCommands.deleteDecision(editor.workspace, it.id)) } },
        onSave = {
            val saved = QahalDecision(
                id = existing?.id ?: editor.newId,
                label = label.trim(),
                classification = classification,
                publicSummary = summary.trim(),
                authorityScope = scope.trim(),
                status = status,
                effectiveOn = if (status == QahalDecisionStatus.DRAFT) null else effectiveValue,
                reviewOn = reviewValue,
                statusChangedOn = existing?.statusChangedOn ?: asOf,
                sourceReferences = existing?.sourceReferences.orEmpty(),
                dissentSummary = existing?.dissentSummary,
            )
            onCommand(WorkspaceEditCommands.putDecision(editor.workspace, saved))
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        RequiredTextField(summary, { summary = it }, stringResource(R.string.workspace_form_public_summary), "workspace-field-summary", false)
        RequiredTextField(scope, { scope = it }, stringResource(R.string.workspace_form_authority_scope), "workspace-field-scope")
        Text(stringResource(R.string.workspace_form_classification), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DecisionClassification.entries.forEach { option ->
                FilterChip(
                    selected = classification == option,
                    onClick = { classification = option },
                    label = { Text(decisionTypeText(option)) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("workspace-field-classification-${option.name.lowercase()}"),
                )
            }
        }
        if (status != QahalDecisionStatus.DRAFT) {
            if (datesRequired) {
                DateTextField(
                    effectiveOn,
                    { effectiveOn = it },
                    stringResource(R.string.workspace_form_effective_on),
                    "workspace-field-effective",
                    stringResource(R.string.workspace_form_decision_date_required),
                )
            } else {
                OptionalDateTextField(
                    effectiveOn,
                    { effectiveOn = it },
                    stringResource(R.string.workspace_form_effective_on),
                    "workspace-field-effective",
                )
            }
        }
        if (datesRequired) {
            DateTextField(
                reviewOn,
                { reviewOn = it },
                stringResource(R.string.workspace_form_review_on),
                "workspace-field-review",
                stringResource(R.string.workspace_form_decision_date_required),
            )
        } else {
            OptionalDateTextField(
                reviewOn,
                { reviewOn = it },
                stringResource(R.string.workspace_form_review_on),
                "workspace-field-review",
            )
        }
        if (!rangeValid) FormError(stringResource(R.string.workspace_form_review_before_effective))
        Text(stringResource(R.string.workspace_form_local_decision_notice), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InventoryEditor(
    editor: WorkspaceEditor.Inventory,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var quantity by remember(existing) { mutableStateOf(existing?.quantityOnHand?.toString() ?: "0") }
    var minimum by remember(existing) { mutableStateOf(existing?.minimumDesired?.toString() ?: "0") }
    var nextCheck by remember(existing) { mutableStateOf(existing?.nextCheckOn?.toString().orEmpty()) }
    val quantityValue = quantity.toIntOrNull()
    val minimumValue = minimum.toIntOrNull()
    val checkDate = nextCheck.optionalDateOrInvalid()
    EditorShell(
        title = stringResource(if (existing == null) R.string.workspace_add_inventory else R.string.workspace_edit_inventory),
        tag = "workspace-editor-inventory",
        saveEnabled = label.isNotBlank() && quantityValue != null && quantityValue >= 0 &&
            minimumValue != null && minimumValue >= 0 && checkDate is OptionalDate.Valid,
        onCancel = onCancel,
        onDelete = existing?.let { { onCommand(WorkspaceEditCommands.deleteInventoryItem(editor.workspace, it.id)) } },
        onSave = {
            val saved = InventoryItem(
                id = existing?.id ?: editor.newId,
                label = label.trim(),
                quantityOnHand = requireNotNull(quantityValue),
                minimumDesired = requireNotNull(minimumValue),
                nextCheckOn = checkDate.valueOrNull,
                status = existing?.status ?: InventoryItemStatus.ACTIVE,
                statusChangedOn = existing?.statusChangedOn ?: asOf,
            )
            onCommand(WorkspaceEditCommands.putInventoryItem(editor.workspace, saved))
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        NumberTextField(quantity, { quantity = it }, stringResource(R.string.workspace_form_quantity), "workspace-field-quantity")
        NumberTextField(minimum, { minimum = it }, stringResource(R.string.workspace_form_minimum), "workspace-field-minimum")
        OptionalDateTextField(
            nextCheck,
            { nextCheck = it },
            stringResource(R.string.workspace_form_next_check),
            "workspace-field-next-check",
        )
    }
}

@Composable
private fun PreparationKitEditor(
    editor: WorkspaceEditor.PreparationKit,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var targetOn by remember(existing, asOf) {
        mutableStateOf(existing?.targetOn?.toString() ?: asOf.plusDays(1).toString())
    }
    val targetDate = targetOn.strictDateOrNull()
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_preparation_kit else R.string.workspace_edit_preparation_kit,
        ),
        tag = "workspace-editor-preparation-kit",
        saveEnabled = label.isNotBlank() && targetDate != null,
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deletePreparationKit(editor.workspace, it.id)) }
        },
        onSave = {
            onCommand(
                WorkspaceEditCommands.putPreparationKit(
                    editor.workspace,
                    HouseholdPreparationKit(
                        id = existing?.id ?: editor.newId,
                        label = label.trim(),
                        targetOn = requireNotNull(targetDate),
                        status = existing?.status ?: PreparationKitStatus.DRAFT,
                        statusChangedOn = existing?.statusChangedOn ?: asOf,
                        tasks = existing?.tasks.orEmpty(),
                    ),
                ),
            )
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        DateTextField(targetOn, { targetOn = it }, stringResource(R.string.workspace_form_target_on), "workspace-field-target")
    }
}

@Composable
private fun PreparationTaskEditor(
    editor: WorkspaceEditor.PreparationTask,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var dueOn by remember(existing, asOf) { mutableStateOf(existing?.dueOn?.toString() ?: asOf.toString()) }
    var assignedTo by remember(existing) { mutableStateOf(existing?.assignedTo?.value.orEmpty()) }
    val dueDate = dueOn.strictDateOrNull()
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_preparation_task else R.string.workspace_edit_preparation_task,
        ),
        tag = "workspace-editor-preparation-task",
        saveEnabled = label.isNotBlank() && dueDate != null,
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deletePreparationTask(editor.workspace, editor.kit, it.id)) }
        },
        onSave = {
            onCommand(
                WorkspaceEditCommands.putPreparationTask(
                    editor.workspace,
                    editor.kit,
                    PreparationKitTask(
                        id = existing?.id ?: editor.newId,
                        label = label.trim(),
                        dueOn = requireNotNull(dueDate),
                        assignedTo = assignedTo.trim().takeIf(String::isNotEmpty)?.let(::LocalPersonId),
                        status = existing?.status ?: WorkItemStatus.TODO,
                        statusChangedOn = existing?.statusChangedOn ?: asOf,
                    ),
                ),
            )
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        DateTextField(dueOn, { dueOn = it }, stringResource(R.string.workspace_form_due_on), "workspace-field-date")
        OutlinedTextField(
            value = assignedTo,
            onValueChange = { assignedTo = it },
            label = { Text(stringResource(R.string.workspace_form_assigned_to)) },
            supportingText = { Text(stringResource(R.string.workspace_form_optional_local_name)) },
            modifier = Modifier.fillMaxWidth().testTag("workspace-field-assigned-to"),
            singleLine = true,
        )
    }
}

@Composable
private fun VolunteerRotationEditor(
    editor: WorkspaceEditor.VolunteerRotationEditor,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var role by remember(existing) { mutableStateOf(existing?.publicRole?.value.orEmpty()) }
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_volunteer_rotation else R.string.workspace_edit_volunteer_rotation,
        ),
        tag = "workspace-editor-volunteer-rotation",
        saveEnabled = label.isNotBlank() && role.isNotBlank(),
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deleteVolunteerRotation(editor.workspace, it.id)) }
        },
        onSave = {
            onCommand(
                WorkspaceEditCommands.putVolunteerRotation(
                    editor.workspace,
                    VolunteerRotation(
                        id = existing?.id ?: editor.newId,
                        label = label.trim(),
                        publicRole = PublicRoleId(role.trim()),
                        status = existing?.status ?: VolunteerRotationStatus.DRAFT,
                        statusChangedOn = existing?.statusChangedOn ?: asOf,
                        slots = existing?.slots.orEmpty(),
                    ),
                ),
            )
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        RequiredTextField(role, { role = it }, stringResource(R.string.workspace_form_public_role), "workspace-field-role")
    }
}

@Composable
private fun VolunteerSlotEditor(
    editor: WorkspaceEditor.VolunteerSlotEditor,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var serviceOn by remember(existing, asOf) {
        mutableStateOf(existing?.serviceOn?.toString() ?: asOf.plusDays(1).toString())
    }
    var assignedTo by remember(existing) { mutableStateOf(existing?.assignedTo?.value.orEmpty()) }
    val serviceDate = serviceOn.strictDateOrNull()
    val assignment = assignedTo.trim().takeIf(String::isNotEmpty)?.let(::LocalPersonId)
    val savedStatus = when (existing?.status) {
        VolunteerSlotStatus.COMPLETED -> VolunteerSlotStatus.COMPLETED
        VolunteerSlotStatus.CANCELLED -> VolunteerSlotStatus.CANCELLED
        else -> if (assignment == null) VolunteerSlotStatus.OPEN else VolunteerSlotStatus.ASSIGNED
    }
    val assignmentValid = savedStatus != VolunteerSlotStatus.COMPLETED || assignment != null
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_volunteer_slot else R.string.workspace_edit_volunteer_slot,
        ),
        tag = "workspace-editor-volunteer-slot",
        saveEnabled = serviceDate != null && assignmentValid,
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deleteVolunteerSlot(editor.workspace, editor.rotation, it.id)) }
        },
        onSave = {
            onCommand(
                WorkspaceEditCommands.putVolunteerSlot(
                    editor.workspace,
                    editor.rotation,
                    VolunteerSlot(
                        id = existing?.id ?: editor.newId,
                        serviceOn = requireNotNull(serviceDate),
                        assignedTo = assignment,
                        status = savedStatus,
                        statusChangedOn = existing?.statusChangedOn ?: asOf,
                    ),
                ),
            )
        },
    ) {
        DateTextField(
            serviceOn,
            { serviceOn = it },
            stringResource(R.string.workspace_form_service_on),
            "workspace-field-service-on",
        )
        OutlinedTextField(
            value = assignedTo,
            onValueChange = { assignedTo = it },
            label = { Text(stringResource(R.string.workspace_form_assigned_to)) },
            isError = !assignmentValid,
            supportingText = {
                Text(
                    stringResource(
                        if (assignmentValid) {
                            R.string.workspace_form_optional_local_name
                        } else {
                            R.string.workspace_form_completed_assignment_required
                        },
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("workspace-field-assigned-to"),
            singleLine = true,
        )
    }
}

@Composable
private fun FinancialChecklistEditor(
    editor: WorkspaceEditor.FinancialChecklistEditor,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var label by remember(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var reviewOn by remember(existing) { mutableStateOf(existing?.reviewOn?.toString().orEmpty()) }
    val reviewDate = reviewOn.optionalDateOrInvalid()
    val status = existing?.status ?: FinancialChecklistStatus.DRAFT
    val activeReviewValid = status != FinancialChecklistStatus.ACTIVE || reviewDate.valueOrNull != null
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_financial_checklist else R.string.workspace_edit_financial_checklist,
        ),
        tag = "workspace-editor-financial-checklist",
        saveEnabled = label.isNotBlank() && reviewDate is OptionalDate.Valid && activeReviewValid,
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deleteFinancialChecklist(editor.workspace, it.id)) }
        },
        onSave = {
            onCommand(
                WorkspaceEditCommands.putFinancialChecklist(
                    editor.workspace,
                    FinancialControlChecklist(
                        id = existing?.id ?: editor.newId,
                        label = label.trim(),
                        status = status,
                        reviewOn = reviewDate.valueOrNull,
                        statusChangedOn = existing?.statusChangedOn ?: asOf,
                        controls = existing?.controls.orEmpty(),
                    ),
                ),
            )
        },
    ) {
        RequiredTextField(label, { label = it }, stringResource(R.string.workspace_form_label), "workspace-field-label")
        if (status == FinancialChecklistStatus.ACTIVE) {
            DateTextField(
                reviewOn,
                { reviewOn = it },
                stringResource(R.string.workspace_form_review_on),
                "workspace-field-review",
                stringResource(R.string.workspace_form_active_review_required),
            )
        } else {
            OptionalDateTextField(
                reviewOn,
                { reviewOn = it },
                stringResource(R.string.workspace_form_review_on),
                "workspace-field-review",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinancialControlEditor(
    editor: WorkspaceEditor.FinancialControlEditor,
    asOf: LocalDate,
    onCancel: () -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    val existing = editor.item
    var kind by remember(existing) { mutableStateOf(existing?.kind ?: FinancialControlKind.DOCUMENTED_AUTHORITY) }
    var dueOn by remember(existing) { mutableStateOf(existing?.dueOn?.toString().orEmpty()) }
    var responsibleRole by remember(existing) { mutableStateOf(existing?.responsibleRole?.value.orEmpty()) }
    val dueDate = dueOn.optionalDateOrInvalid()
    val kindsUsedByOtherControls = editor.checklist.controls
        .filterNot { control -> control.id == existing?.id }
        .map(FinancialControlItem::kind)
        .toSet()
    val kindAvailable = kind !in kindsUsedByOtherControls
    EditorShell(
        title = stringResource(
            if (existing == null) R.string.workspace_add_financial_control else R.string.workspace_edit_financial_control,
        ),
        tag = "workspace-editor-financial-control",
        saveEnabled = dueDate is OptionalDate.Valid && kindAvailable,
        onCancel = onCancel,
        onDelete = existing?.let {
            { onCommand(WorkspaceEditCommands.deleteFinancialControl(editor.workspace, editor.checklist, it.id)) }
        },
        onSave = {
            onCommand(
                WorkspaceEditCommands.putFinancialControl(
                    editor.workspace,
                    editor.checklist,
                    FinancialControlItem(
                        id = existing?.id ?: editor.newId,
                        kind = kind,
                        status = existing?.status ?: FinancialControlStatus.PENDING,
                        dueOn = dueDate.valueOrNull,
                        responsibleRole = responsibleRole.trim().takeIf(String::isNotEmpty)?.let(::PublicRoleId),
                        statusChangedOn = existing?.statusChangedOn ?: asOf,
                    ),
                ),
            )
        },
    ) {
        Text(stringResource(R.string.workspace_form_kind), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FinancialControlKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option,
                    onClick = { kind = option },
                    enabled = option !in kindsUsedByOtherControls || kind == option,
                    label = { Text(financialControlText(option)) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("workspace-field-control-kind-${option.name.lowercase()}"),
                )
            }
        }
        if (!kindAvailable) {
            FormError(stringResource(R.string.workspace_form_control_kind_duplicate))
        }
        OptionalDateTextField(
            dueOn,
            { dueOn = it },
            stringResource(R.string.workspace_form_due_on),
            "workspace-field-date",
        )
        OutlinedTextField(
            value = responsibleRole,
            onValueChange = { responsibleRole = it },
            label = { Text(stringResource(R.string.workspace_form_responsible_role)) },
            modifier = Modifier.fillMaxWidth().testTag("workspace-field-responsible-role"),
            singleLine = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorShell(
    title: String,
    tag: String,
    saveEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val actionsEnabled = LocalEditorActionsEnabled.current
    val focusManager = LocalFocusManager.current
    var deleteConfirmationOpen by remember { mutableStateOf(false) }
    LaunchedEffect(actionsEnabled) {
        if (!actionsEnabled) focusManager.clearFocus(force = true)
    }
    Card(modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { canFocus = actionsEnabled }
                        .then(
                            if (actionsEnabled) {
                                Modifier
                            } else {
                                Modifier.clearAndSetSemantics { disabled() }
                            },
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                if (!actionsEnabled) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent().changes.forEach { change ->
                                            change.consume()
                                        }
                                    }
                                }
                            }
                            .clearAndSetSemantics { disabled() },
                    )
                }
            }
            if (!actionsEnabled) {
                Text(
                    text = stringResource(R.string.workspace_form_change_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("workspace-editor-pending"),
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSave,
                    enabled = saveEnabled && actionsEnabled,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("workspace-editor-save"),
                ) {
                    Text(stringResource(R.string.workspace_form_save))
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = actionsEnabled,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("workspace-editor-cancel"),
                ) {
                    Text(stringResource(R.string.workspace_form_cancel))
                }
                onDelete?.let { delete ->
                    TextButton(
                        onClick = { deleteConfirmationOpen = true },
                        enabled = actionsEnabled,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("workspace-editor-delete"),
                    ) {
                        Text(stringResource(R.string.workspace_form_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (deleteConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { deleteConfirmationOpen = false },
            title = { Text(stringResource(R.string.workspace_delete_confirm_title)) },
            text = { Text(stringResource(R.string.workspace_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmationOpen = false
                        onDelete?.invoke()
                    },
                    modifier = Modifier.testTag("workspace-editor-delete-confirm"),
                ) {
                    Text(stringResource(R.string.workspace_delete_confirm_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteConfirmationOpen = false },
                    modifier = Modifier.testTag("workspace-editor-delete-dismiss"),
                ) {
                    Text(stringResource(R.string.workspace_form_cancel))
                }
            },
            modifier = Modifier.testTag("workspace-editor-delete-dialog"),
        )
    }
}

private val LocalEditorActionsEnabled = staticCompositionLocalOf { true }

@Composable
private fun RequiredTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = value.isBlank(),
        supportingText = if (value.isBlank()) {
            { Text(stringResource(R.string.workspace_form_required)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth().testTag(tag),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
    )
}

@Composable
private fun DateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    blankError: String? = null,
) {
    val invalid = value.strictDateOrNull() == null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = invalid,
        supportingText = {
            Text(
                when {
                    value.isBlank() && blankError != null -> blankError
                    invalid -> stringResource(R.string.workspace_form_date_error)
                    else -> stringResource(R.string.workspace_form_date_hint)
                },
            )
        },
        modifier = Modifier.fillMaxWidth().testTag(tag),
        singleLine = true,
    )
}

@Composable
private fun OptionalDateTextField(value: String, onValueChange: (String) -> Unit, label: String, tag: String) {
    val invalid = value.optionalDateOrInvalid() is OptionalDate.Invalid
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = invalid,
        supportingText = {
            Text(
                stringResource(
                    if (invalid) R.string.workspace_form_date_error else R.string.workspace_form_optional_date_hint,
                ),
            )
        },
        modifier = Modifier.fillMaxWidth().testTag(tag),
        singleLine = true,
    )
}

@Composable
private fun NumberTextField(value: String, onValueChange: (String) -> Unit, label: String, tag: String) {
    val invalid = value.toIntOrNull()?.let { it < 0 } != false
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = invalid,
        supportingText = if (invalid) {
            { Text(stringResource(R.string.workspace_form_number_error)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth().testTag(tag),
        singleLine = true,
    )
}

@Composable
private fun FormError(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.semantics {
            error(text)
            liveRegion = LiveRegionMode.Polite
        },
    )
}

private sealed interface OptionalDate {
    data class Valid(val value: LocalDate?) : OptionalDate
    data object Invalid : OptionalDate
}

private val OptionalDate.valueOrNull: LocalDate?
    get() = (this as? OptionalDate.Valid)?.value

private fun String.strictDateOrNull(): LocalDate? = runCatching { LocalDate.parse(trim()) }.getOrNull()

private fun String.optionalDateOrInvalid(): OptionalDate = if (isBlank()) {
    OptionalDate.Valid(null)
} else {
    strictDateOrNull()?.let(OptionalDate::Valid) ?: OptionalDate.Invalid
}

private fun newRecordId(kind: String): WorkspaceRecordId =
    WorkspaceRecordId("local.$kind.${UUID.randomUUID()}")
