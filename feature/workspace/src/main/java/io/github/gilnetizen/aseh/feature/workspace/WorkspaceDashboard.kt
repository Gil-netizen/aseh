package io.github.gilnetizen.aseh.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.domain.workspace.CalendarItemStatus
import io.github.gilnetizen.aseh.domain.workspace.DecisionClassification
import io.github.gilnetizen.aseh.domain.workspace.DueSoonItem
import io.github.gilnetizen.aseh.domain.workspace.DueSoonKind
import io.github.gilnetizen.aseh.domain.workspace.DueTiming
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlKind
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarKind
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.PracticeAdoptionStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssue
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssueCode
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordAddress
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordKind as WorkspaceSearchRecordKind
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot
import java.time.LocalDate

/**
 * Renders one caller-selected local workspace. The host owns persistence and reducer execution,
 * then returns validation or rejected-command issues through [feedback]. No fixture or clock is
 * selected inside this production API.
 */
@Composable
fun WorkspaceDashboard(
    snapshot: WorkspaceSnapshot,
    activeContext: ActiveWorkspaceContext,
    asOf: LocalDate,
    onCommand: (WorkspaceCommand) -> Unit,
    modifier: Modifier = Modifier,
    feedback: WorkspaceDashboardFeedback = WorkspaceDashboardFeedback(),
    requestedRecord: WorkspaceRecordAddress? = null,
    onRequestedRecordConsumed: () -> Unit = {},
    dismissEditorRequest: Int = 0,
    onEditorOpenChanged: (Boolean) -> Unit = {},
) {
    val model = remember(snapshot, activeContext, asOf) {
        WorkspaceDashboardPresenter.present(snapshot, activeContext, asOf)
    }
    val validationIssues = (model.domainIssues + feedback.validationIssues).distinct()
    val localeIsHebrew = LocalConfiguration.current.locales[0].language in setOf("he", "iw")
    val highlightedDescription = stringResource(R.string.workspace_search_result_highlighted)
    val workspaceTitle = stringResource(R.string.workspace_title)
    val workspaceHeadingAccessibilityLabel = listOfNotNull(workspaceTitle, model.contextLabel)
        .joinToString(". ")
    var editor by remember(activeContext) { mutableStateOf<WorkspaceEditor?>(null) }
    var pendingEditorCommand by remember(activeContext) { mutableStateOf<WorkspaceCommand?>(null) }
    var pendingEditor by remember(activeContext) { mutableStateOf<WorkspaceEditor?>(null) }
    var highlightedRecord by remember(activeContext) { mutableStateOf<WorkspaceRecordAddress?>(null) }
    val listState = rememberLazyListState()
    val headingBringIntoViewRequester = remember { BringIntoViewRequester() }
    val headingFocusRequester = remember { FocusRequester() }
    val workspaceHeadingHighlighted = highlightedRecord.matchesWorkspace(activeContext.workspaceId)

    LaunchedEffect(editor) {
        if (editor != null) listState.animateScrollToItem(1)
    }

    LaunchedEffect(editor != null) {
        onEditorOpenChanged(editor != null)
    }

    LaunchedEffect(dismissEditorRequest) {
        if (editor != null && pendingEditorCommand == null) {
            pendingEditor = null
            editor = null
        }
    }

    LaunchedEffect(requestedRecord, model.content) {
        val requested = requestedRecord
        if (requested?.workspaceId == activeContext.workspaceId) {
            highlightedRecord = requested
            if (requested.kind == WorkspaceSearchRecordKind.WORKSPACE) {
                listState.scrollToItem(0)
                // Item zero may have been disposed after a prior record result. Wait until the
                // scroll recomposes it, then take focus after the departing dialog restores focus.
                withFrameNanos { }
                headingBringIntoViewRequester.bringIntoView()
                withFrameNanos { }
                headingFocusRequester.requestFocus()
                onRequestedRecordConsumed()
            } else if (requested.recordId != null) {
                withFrameNanos { }
                val contentIndex = listState.layoutInfo.totalItemsCount - 1
                if (contentIndex >= 0) listState.scrollToItem(contentIndex)
            }
        }
    }

    LaunchedEffect(snapshot, pendingEditorCommand) {
        val pending = pendingEditorCommand
        if (pending != null && WorkspaceEditCommands.isApplied(snapshot, pending)) {
            val submittedEditor = pendingEditor
            pendingEditorCommand = null
            pendingEditor = null
            if (editor == submittedEditor) editor = null
        }
    }

    LaunchedEffect(
        pendingEditorCommand,
        feedback.rejectedCommandIssues,
        feedback.persistenceFailed,
    ) {
        if (pendingEditorCommand != null &&
            (feedback.rejectedCommandIssues.isNotEmpty() || feedback.persistenceFailed)
        ) {
            pendingEditorCommand = null
            pendingEditor = null
        }
    }

    fun openWorkspaceEditor() {
        editor = when (val content = model.content) {
            is WorkspaceDashboardContent.Self -> WorkspaceEditor.SelfName(content.workspace)
            is WorkspaceDashboardContent.Household -> WorkspaceEditor.HouseholdName(content.workspace)
            is WorkspaceDashboardContent.Qahal -> WorkspaceEditor.QahalName(content.workspace)
            WorkspaceDashboardContent.Missing -> null
        }
    }

    fun dispatchEditorCommand(command: WorkspaceCommand) {
        if (pendingEditorCommand != null) return
        pendingEditor = editor
        pendingEditorCommand = command
        onCommand(command)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("workspace-dashboard"),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("heading") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(headingBringIntoViewRequester)
                    .focusRequester(headingFocusRequester)
                    .focusable()
                    .testTag("workspace-heading")
                    .semantics {
                        contentDescription = workspaceHeadingAccessibilityLabel
                        if (workspaceHeadingHighlighted) {
                            stateDescription = highlightedDescription
                        }
                    },
                color = if (workspaceHeadingHighlighted) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.background
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = if (workspaceHeadingHighlighted) Modifier.padding(12.dp) else Modifier,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = workspaceTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    model.contextLabel?.let { label ->
                        Text(isolate(label), style = MaterialTheme.typography.titleLarge)
                    }
                    Text(stringResource(R.string.workspace_summary), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.workspace_as_of, isolate(asOf.toString())),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (model.content != WorkspaceDashboardContent.Missing) {
                        OutlinedButton(
                            onClick = ::openWorkspaceEditor,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("workspace-edit-name"),
                        ) {
                            Text(stringResource(R.string.workspace_edit_workspace))
                        }
                    }
                }
            }
        }

        editor?.let { activeEditor ->
            item("workspace-editor") {
                WorkspaceEditorCard(
                    editor = activeEditor,
                    asOf = asOf,
                    onCancel = {
                        if (pendingEditorCommand == null) {
                            pendingEditor = null
                            editor = null
                        }
                    },
                    onCommand = ::dispatchEditorCommand,
                    actionsEnabled = pendingEditorCommand == null,
                )
            }
        }

        if (localeIsHebrew) {
            item("language-notice") {
                NoticeCard(stringResource(R.string.workspace_english_fallback))
            }
        }

        if (validationIssues.isNotEmpty()) {
            item("validation-feedback") {
                IssueCard(
                    title = stringResource(R.string.workspace_validation_heading),
                    issues = validationIssues,
                    tag = "workspace-validation-feedback",
                )
            }
        }
        if (feedback.rejectedCommandIssues.isNotEmpty()) {
            item("rejection-feedback") {
                IssueCard(
                    title = stringResource(R.string.workspace_rejection_heading),
                    issues = feedback.rejectedCommandIssues,
                    tag = "workspace-rejection-feedback",
                )
            }
        }
        if (feedback.persistenceFailed) {
            item("persistence-feedback") {
                val message = stringResource(R.string.workspace_persistence_failed)
                NoticeCard(
                    text = message,
                    modifier = Modifier
                        .testTag("workspace-persistence-feedback")
                        .semantics {
                            error(message)
                            liveRegion = LiveRegionMode.Polite
                        },
                )
            }
        }

        item("due-soon") {
            DashboardSection(stringResource(R.string.workspace_due_soon)) {
                if (model.dueSoonItems.isEmpty()) {
                    Text(stringResource(R.string.workspace_due_none))
                } else {
                    model.dueSoonItems.forEach { dueItem -> DueSoonRow(dueItem) }
                }
            }
        }

        when (val content = model.content) {
            is WorkspaceDashboardContent.Self -> item("self-content") {
                DashboardSection(stringResource(R.string.workspace_personal_practices)) {
                    if (content.workspace.practiceAdoptions.isEmpty()) {
                        NoticeCard(stringResource(R.string.workspace_practice_creation_limited))
                    }
                    content.workspace.practiceAdoptions.forEach { adoption ->
                        RecordCard(
                            title = adoption.label,
                            status = statusText(adoption.status),
                            details = listOf(
                                stringResource(
                                    R.string.workspace_adopted_value,
                                    isolate(dateOrNot(adoption.adoptedOn)),
                                ),
                                stringResource(
                                    R.string.workspace_review_value,
                                    isolate(dateOrNot(adoption.reviewOn)),
                                ),
                            ),
                            actions = model.actionsFor(WorkspaceRecordKind.PRACTICE, adoption.id),
                            onCommand = onCommand,
                            recordId = adoption.id,
                            revealRequested = requestedRecord.matches(
                                content.workspace.id,
                                WorkspaceSearchRecordKind.PERSONAL_PRACTICE,
                                adoption.id,
                            ),
                            highlighted = highlightedRecord.matches(
                                content.workspace.id,
                                WorkspaceSearchRecordKind.PERSONAL_PRACTICE,
                                adoption.id,
                            ),
                            onRequestedRecordConsumed = onRequestedRecordConsumed,
                        )
                    }
                }
            }
            is WorkspaceDashboardContent.Household -> item("household-content") {
                HouseholdContent(
                    workspace = content.workspace,
                    model = model,
                    onCommand = onCommand,
                    onEdit = { editor = it },
                    requestedRecord = requestedRecord,
                    highlightedRecord = highlightedRecord,
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                )
            }
            is WorkspaceDashboardContent.Qahal -> item("qahal-content") {
                QahalContent(
                    workspace = content.workspace,
                    model = model,
                    onCommand = onCommand,
                    onEdit = { editor = it },
                    requestedRecord = requestedRecord,
                    highlightedRecord = highlightedRecord,
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                )
            }
            WorkspaceDashboardContent.Missing -> item("missing") {
                NoticeCard(stringResource(R.string.workspace_missing))
            }
        }
    }
}

@Composable
private fun HouseholdContent(
    workspace: io.github.gilnetizen.aseh.domain.workspace.HouseholdWorkspace,
    model: WorkspaceDashboardModel,
    onCommand: (WorkspaceCommand) -> Unit,
    onEdit: (WorkspaceEditor) -> Unit,
    requestedRecord: WorkspaceRecordAddress?,
    highlightedRecord: WorkspaceRecordAddress?,
    onRequestedRecordConsumed: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DashboardSection(stringResource(R.string.workspace_household_responsibilities)) {
            AddRecordButton(
                text = stringResource(R.string.workspace_add_responsibility),
                tag = "workspace-add-responsibility",
                onClick = { onEdit(WorkspaceEditor.Responsibility(workspace)) },
            )
            if (workspace.responsibilities.isEmpty()) EmptySection()
            workspace.responsibilities.forEach { item ->
                RecordCard(
                    title = item.label,
                    status = statusText(item.status),
                    details = listOf(
                        stringResource(R.string.workspace_due_value, isolate(item.dueOn.toString())),
                        assignedText(item.assignedTo != null),
                    ),
                    actions = model.actionsFor(WorkspaceRecordKind.RESPONSIBILITY, item.id),
                    onCommand = onCommand,
                    onEdit = { onEdit(WorkspaceEditor.Responsibility(workspace, item)) },
                    editTag = "workspace-edit-responsibility-${item.id.value}",
                    revealRequested = requestedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.HOUSEHOLD_RESPONSIBILITY,
                        item.id,
                    ),
                    highlighted = highlightedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.HOUSEHOLD_RESPONSIBILITY,
                        item.id,
                    ),
                    recordId = item.id,
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                )
            }
        }
        DashboardSection(stringResource(R.string.workspace_household_calendar)) {
            AddRecordButton(
                text = stringResource(R.string.workspace_add_calendar),
                tag = "workspace-add-calendar",
                onClick = { onEdit(WorkspaceEditor.CalendarItem(workspace)) },
            )
            if (workspace.calendarItems.isEmpty()) EmptySection()
            workspace.calendarItems.forEach { item ->
                RecordCard(
                    title = item.label,
                    status = statusText(item.status),
                    details = listOf(
                        stringResource(R.string.workspace_kind_value, calendarKindText(item.kind)),
                        stringResource(R.string.workspace_starts_value, isolate(item.startsOn.toString())),
                        stringResource(R.string.workspace_ends_value, isolate(item.endsOn.toString())),
                    ),
                    actions = model.actionsFor(WorkspaceRecordKind.CALENDAR_ITEM, item.id),
                    onCommand = onCommand,
                    onEdit = { onEdit(WorkspaceEditor.CalendarItem(workspace, item)) },
                    editTag = "workspace-edit-calendar-${item.id.value}",
                    revealRequested = requestedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.HOUSEHOLD_CALENDAR_ITEM,
                        item.id,
                    ),
                    highlighted = highlightedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.HOUSEHOLD_CALENDAR_ITEM,
                        item.id,
                    ),
                    recordId = item.id,
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                )
            }
        }
        DashboardSection(stringResource(R.string.workspace_preparation_kits)) {
            AddRecordButton(
                text = stringResource(R.string.workspace_add_preparation_kit),
                tag = "workspace-add-preparation-kit",
                onClick = { onEdit(WorkspaceEditor.PreparationKit(workspace)) },
            )
            if (workspace.preparationKits.isEmpty()) EmptySection()
            workspace.preparationKits.forEach { kit ->
                RecordCard(
                    title = kit.label,
                    status = statusText(kit.status),
                    details = listOf(
                        stringResource(R.string.workspace_target_value, isolate(kit.targetOn.toString())),
                    ),
                    actions = model.actionsFor(WorkspaceRecordKind.PREPARATION_KIT, kit.id),
                    onCommand = onCommand,
                    onEdit = { onEdit(WorkspaceEditor.PreparationKit(workspace, kit)) },
                    editTag = "workspace-edit-preparation-kit-${kit.id.value}",
                    recordId = kit.id,
                    revealRequested = requestedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.HOUSEHOLD_PREPARATION_KIT,
                        kit.id,
                    ),
                    highlighted = highlightedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.HOUSEHOLD_PREPARATION_KIT,
                        kit.id,
                    ),
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                ) {
                    AddRecordButton(
                        text = stringResource(R.string.workspace_add_preparation_task),
                        tag = "workspace-add-preparation-task-${kit.id.value}",
                        onClick = { onEdit(WorkspaceEditor.PreparationTask(workspace, kit)) },
                    )
                    kit.tasks.forEach { task ->
                        RecordCard(
                            title = task.label,
                            status = statusText(task.status),
                            details = listOf(
                                stringResource(R.string.workspace_due_value, isolate(task.dueOn.toString())),
                                assignedText(task.assignedTo != null),
                            ),
                            actions = model.actionsFor(
                                WorkspaceRecordKind.PREPARATION_TASK,
                                task.id,
                                kit.id,
                            ),
                            onCommand = onCommand,
                            compact = true,
                            onEdit = { onEdit(WorkspaceEditor.PreparationTask(workspace, kit, task)) },
                            editTag = "workspace-edit-preparation-task-${task.id.value}",
                            recordId = task.id,
                            revealRequested = requestedRecord.matches(
                                workspace.id,
                                WorkspaceSearchRecordKind.PREPARATION_KIT_TASK,
                                task.id,
                                kit.id,
                            ),
                            highlighted = highlightedRecord.matches(
                                workspace.id,
                                WorkspaceSearchRecordKind.PREPARATION_KIT_TASK,
                                task.id,
                                kit.id,
                            ),
                            onRequestedRecordConsumed = onRequestedRecordConsumed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QahalContent(
    workspace: io.github.gilnetizen.aseh.domain.workspace.QahalWorkspace,
    model: WorkspaceDashboardModel,
    onCommand: (WorkspaceCommand) -> Unit,
    onEdit: (WorkspaceEditor) -> Unit,
    requestedRecord: WorkspaceRecordAddress?,
    highlightedRecord: WorkspaceRecordAddress?,
    onRequestedRecordConsumed: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DashboardSection(stringResource(R.string.workspace_qahal_decisions)) {
            AddRecordButton(
                text = stringResource(R.string.workspace_add_decision),
                tag = "workspace-add-decision",
                onClick = { onEdit(WorkspaceEditor.Decision(workspace)) },
            )
            if (workspace.decisions.isEmpty()) EmptySection()
            workspace.decisions.forEach { decision ->
                val details = buildList {
                    add(stringResource(R.string.workspace_classification_value, decisionTypeText(decision.classification)))
                    add(stringResource(R.string.workspace_scope_value, isolate(decision.authorityScope)))
                    add(stringResource(R.string.workspace_effective_value, isolate(dateOrNot(decision.effectiveOn))))
                    add(stringResource(R.string.workspace_review_value, isolate(dateOrNot(decision.reviewOn))))
                    decision.dissentSummary?.let {
                        add(stringResource(R.string.workspace_dissent_value, isolate(it)))
                    }
                }
                RecordCard(
                    title = decision.label,
                    status = statusText(decision.status),
                    summary = decision.publicSummary,
                    details = details,
                    actions = model.actionsFor(WorkspaceRecordKind.DECISION, decision.id),
                    onCommand = onCommand,
                    onEdit = { onEdit(WorkspaceEditor.Decision(workspace, decision)) },
                    editTag = "workspace-edit-decision-${decision.id.value}",
                    revealRequested = requestedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.QAHAL_DECISION,
                        decision.id,
                    ),
                    highlighted = highlightedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.QAHAL_DECISION,
                        decision.id,
                    ),
                    recordId = decision.id,
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                )
            }
        }
        DashboardSection(stringResource(R.string.workspace_volunteer_rotations)) {
            AddRecordButton(
                text = stringResource(R.string.workspace_add_volunteer_rotation),
                tag = "workspace-add-volunteer-rotation",
                onClick = { onEdit(WorkspaceEditor.VolunteerRotationEditor(workspace)) },
            )
            if (workspace.volunteerRotations.isEmpty()) EmptySection()
            workspace.volunteerRotations.forEach { rotation ->
                RecordCard(
                    title = rotation.label,
                    status = statusText(rotation.status),
                    details = emptyList(),
                    actions = model.actionsFor(WorkspaceRecordKind.VOLUNTEER_ROTATION, rotation.id),
                    onCommand = onCommand,
                    onEdit = { onEdit(WorkspaceEditor.VolunteerRotationEditor(workspace, rotation)) },
                    editTag = "workspace-edit-volunteer-rotation-${rotation.id.value}",
                    recordId = rotation.id,
                    revealRequested = requestedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.VOLUNTEER_ROTATION,
                        rotation.id,
                    ),
                    highlighted = highlightedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.VOLUNTEER_ROTATION,
                        rotation.id,
                    ),
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                ) {
                    AddRecordButton(
                        text = stringResource(R.string.workspace_add_volunteer_slot),
                        tag = "workspace-add-volunteer-slot-${rotation.id.value}",
                        onClick = { onEdit(WorkspaceEditor.VolunteerSlotEditor(workspace, rotation)) },
                    )
                    rotation.slots.forEach { slot ->
                        RecordCard(
                            title = stringResource(
                                R.string.workspace_service_date_value,
                                isolate(slot.serviceOn.toString()),
                            ),
                            status = statusText(slot.status),
                            details = listOf(assignedText(slot.assignedTo != null)),
                            actions = model.actionsFor(
                                WorkspaceRecordKind.VOLUNTEER_SLOT,
                                slot.id,
                                rotation.id,
                            ),
                            onCommand = onCommand,
                            compact = true,
                            onEdit = { onEdit(WorkspaceEditor.VolunteerSlotEditor(workspace, rotation, slot)) },
                            editTag = "workspace-edit-volunteer-slot-${slot.id.value}",
                            recordId = slot.id,
                            revealRequested = requestedRecord.matches(
                                workspace.id,
                                WorkspaceSearchRecordKind.VOLUNTEER_SLOT,
                                slot.id,
                                rotation.id,
                            ),
                            highlighted = highlightedRecord.matches(
                                workspace.id,
                                WorkspaceSearchRecordKind.VOLUNTEER_SLOT,
                                slot.id,
                                rotation.id,
                            ),
                            onRequestedRecordConsumed = onRequestedRecordConsumed,
                        )
                    }
                }
            }
        }
        DashboardSection(stringResource(R.string.workspace_inventory)) {
            AddRecordButton(
                text = stringResource(R.string.workspace_add_inventory),
                tag = "workspace-add-inventory",
                onClick = { onEdit(WorkspaceEditor.Inventory(workspace)) },
            )
            if (workspace.inventory.isEmpty()) EmptySection()
            workspace.inventory.forEach { item ->
                RecordCard(
                    title = item.label,
                    status = statusText(item.status),
                    details = listOf(
                        stringResource(
                            R.string.workspace_inventory_counts,
                            item.quantityOnHand,
                            item.minimumDesired,
                        ),
                        stringResource(
                            R.string.workspace_inventory_check,
                            isolate(dateOrNot(item.nextCheckOn)),
                        ),
                    ),
                    actions = model.actionsFor(WorkspaceRecordKind.INVENTORY, item.id),
                    onCommand = onCommand,
                    onEdit = { onEdit(WorkspaceEditor.Inventory(workspace, item)) },
                    editTag = "workspace-edit-inventory-${item.id.value}",
                    revealRequested = requestedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.INVENTORY_ITEM,
                        item.id,
                    ),
                    highlighted = highlightedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.INVENTORY_ITEM,
                        item.id,
                    ),
                    recordId = item.id,
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                )
            }
        }
        DashboardSection(stringResource(R.string.workspace_financial_controls)) {
            AddRecordButton(
                text = stringResource(R.string.workspace_add_financial_checklist),
                tag = "workspace-add-financial-checklist",
                onClick = { onEdit(WorkspaceEditor.FinancialChecklistEditor(workspace)) },
            )
            if (workspace.financialControls.isEmpty()) EmptySection()
            workspace.financialControls.forEach { checklist ->
                RecordCard(
                    title = checklist.label,
                    status = statusText(checklist.status),
                    details = listOf(
                        stringResource(
                            R.string.workspace_review_value,
                            isolate(dateOrNot(checklist.reviewOn)),
                        ),
                    ),
                    actions = model.actionsFor(WorkspaceRecordKind.FINANCIAL_CHECKLIST, checklist.id),
                    onCommand = onCommand,
                    onEdit = { onEdit(WorkspaceEditor.FinancialChecklistEditor(workspace, checklist)) },
                    editTag = "workspace-edit-financial-checklist-${checklist.id.value}",
                    recordId = checklist.id,
                    revealRequested = requestedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.FINANCIAL_CHECKLIST,
                        checklist.id,
                    ),
                    highlighted = highlightedRecord.matches(
                        workspace.id,
                        WorkspaceSearchRecordKind.FINANCIAL_CHECKLIST,
                        checklist.id,
                    ),
                    onRequestedRecordConsumed = onRequestedRecordConsumed,
                ) {
                    AddRecordButton(
                        text = stringResource(R.string.workspace_add_financial_control),
                        tag = "workspace-add-financial-control-${checklist.id.value}",
                        onClick = { onEdit(WorkspaceEditor.FinancialControlEditor(workspace, checklist)) },
                    )
                    checklist.controls.forEach { control ->
                        RecordCard(
                            title = financialControlText(control.kind),
                            status = statusText(control.status),
                            details = buildList {
                                control.dueOn?.let {
                                    add(stringResource(R.string.workspace_due_value, isolate(it.toString())))
                                }
                                if (control.responsibleRole != null) {
                                    add(stringResource(R.string.workspace_control_owner))
                                }
                            },
                            actions = model.actionsFor(
                                WorkspaceRecordKind.FINANCIAL_CONTROL,
                                control.id,
                                checklist.id,
                            ),
                            onCommand = onCommand,
                            compact = true,
                            onEdit = {
                                onEdit(WorkspaceEditor.FinancialControlEditor(workspace, checklist, control))
                            },
                            editTag = "workspace-edit-financial-control-${control.id.value}",
                            recordId = control.id,
                            revealRequested = requestedRecord.matches(
                                workspace.id,
                                WorkspaceSearchRecordKind.FINANCIAL_CONTROL,
                                control.id,
                                checklist.id,
                            ),
                            highlighted = highlightedRecord.matches(
                                workspace.id,
                                WorkspaceSearchRecordKind.FINANCIAL_CONTROL,
                                control.id,
                                checklist.id,
                            ),
                            onRequestedRecordConsumed = onRequestedRecordConsumed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

private fun WorkspaceRecordAddress?.matches(
    workspaceId: WorkspaceId,
    kind: WorkspaceSearchRecordKind,
    recordId: WorkspaceRecordId,
    parentRecordId: WorkspaceRecordId? = null,
): Boolean = this != null &&
    this.workspaceId == workspaceId &&
    this.kind == kind &&
    this.recordId == recordId &&
    this.parentRecordId == parentRecordId

private fun WorkspaceRecordAddress?.matchesWorkspace(workspaceId: WorkspaceId): Boolean =
    this != null &&
        this.workspaceId == workspaceId &&
        this.kind == WorkspaceSearchRecordKind.WORKSPACE &&
        this.recordId == null &&
        this.parentRecordId == null

@Composable
private fun RecordCard(
    title: String,
    status: String,
    details: List<String>,
    actions: List<WorkspaceDashboardAction>,
    onCommand: (WorkspaceCommand) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    compact: Boolean = false,
    onEdit: (() -> Unit)? = null,
    editTag: String = "workspace-edit-record",
    recordId: WorkspaceRecordId? = null,
    revealRequested: Boolean = false,
    highlighted: Boolean = false,
    onRequestedRecordConsumed: () -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    var isPlaced by remember { mutableStateOf(false) }
    val highlightedDescription = stringResource(R.string.workspace_search_result_highlighted)
    val statusDescription = stringResource(R.string.workspace_status_value, status)
    val accessibleLabel = listOfNotNull(title, statusDescription, summary)
        .plus(details.filter(String::isNotBlank))
        .joinToString(". ")
    LaunchedEffect(revealRequested, isPlaced) {
        if (revealRequested && isPlaced) {
            bringIntoViewRequester.bringIntoView()
            // Allow the search dialog to finish restoring focus before taking final focus here.
            withFrameNanos { }
            focusRequester.requestFocus()
            onRequestedRecordConsumed()
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { isPlaced = true }
            .focusRequester(focusRequester)
            .focusable()
            .then(if (recordId != null) Modifier.testTag("workspace-record-${recordId.value}") else Modifier)
            .semantics {
                contentDescription = accessibleLabel
                if (highlighted) stateDescription = highlightedDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(isolate(title), style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.workspace_status_value, status), fontWeight = FontWeight.SemiBold)
            if (!summary.isNullOrBlank()) Text(isolate(summary), style = MaterialTheme.typography.bodyMedium)
            details.filter(String::isNotBlank).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            content?.invoke(this)
            onEdit?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag(editTag),
                ) {
                    Text(stringResource(R.string.workspace_form_edit))
                }
            }
            ActionButtons(actions, onCommand)
        }
    }
}

@Composable
private fun AddRecordButton(text: String, tag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag(tag),
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtons(
    actions: List<WorkspaceDashboardAction>,
    onCommand: (WorkspaceCommand) -> Unit,
) {
    if (actions.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            OutlinedButton(
                onClick = { action.dispatch(onCommand) },
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("workspace-action-${action.label.name.lowercase()}-${action.target.recordId.value}"),
            ) {
                Text(actionText(action.label))
            }
        }
    }
}

@Composable
private fun DueSoonRow(item: DueSoonItem) {
    val financialKind = if (item.kind == DueSoonKind.FINANCIAL_CONTROL) {
        runCatching { FinancialControlKind.valueOf(item.label) }.getOrNull()
    } else {
        null
    }
    val displayLabel = if (financialKind != null) financialControlText(financialKind) else isolate(item.label)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workspace-due-${item.recordId.value}"),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(
                R.string.workspace_due_line,
                displayLabel,
                dueKindText(item.kind),
                stringResource(
                    R.string.workspace_due_when,
                    dueTimingText(item.timing),
                    isolate(item.dueOn.toString()),
                ),
            ),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun IssueCard(title: String, issues: List<WorkspaceIssue>, tag: String) {
    val issueMessages = issues.map { issue -> issueText(issue.code) }
    val announcement = (listOf(title) + issueMessages).joinToString(". ")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics {
                error(announcement)
                liveRegion = LiveRegionMode.Polite
            },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            issues.forEachIndexed { index, issue ->
                Text(issueMessages[index])
                Text(
                    stringResource(R.string.workspace_issue_path, isolate(issue.path)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(text, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptySection() {
    Text(stringResource(R.string.workspace_empty_section), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun assignedText(assigned: Boolean): String = stringResource(
    if (assigned) R.string.workspace_assigned else R.string.workspace_unassigned,
)

@Composable
private fun dateOrNot(date: LocalDate?): String = date?.toString()
    ?: stringResource(R.string.workspace_no_date)

@Composable
private fun statusText(status: Enum<*>): String = stringResource(
    when (status.name) {
        "DRAFT" -> R.string.workspace_status_draft
        "ADOPTED" -> R.string.workspace_status_adopted
        "PAUSED" -> R.string.workspace_status_paused
        "RETIRED" -> R.string.workspace_status_retired
        "TODO" -> R.string.workspace_status_todo
        "IN_PROGRESS" -> R.string.workspace_status_in_progress
        "DONE" -> R.string.workspace_status_done
        "CANCELLED" -> R.string.workspace_status_cancelled
        "SCHEDULED" -> R.string.workspace_status_scheduled
        "COMPLETED" -> R.string.workspace_status_completed
        "ACTIVE" -> R.string.workspace_status_active
        "SUPERSEDED" -> R.string.workspace_status_superseded
        "OPEN" -> R.string.workspace_status_open
        "ASSIGNED" -> R.string.workspace_status_assigned
        "CLOSED" -> R.string.workspace_status_closed
        "OUT_OF_SERVICE" -> R.string.workspace_status_out_of_service
        "PENDING" -> R.string.workspace_status_pending
        "VERIFIED" -> R.string.workspace_status_verified
        "NOT_APPLICABLE" -> R.string.workspace_status_not_applicable
        else -> error("Unsupported workspace status ${status.name}")
    },
)

@Composable
private fun actionText(label: WorkspaceActionLabel): String = stringResource(
    when (label) {
        WorkspaceActionLabel.ADOPT -> R.string.workspace_action_adopt
        WorkspaceActionLabel.PAUSE -> R.string.workspace_action_pause
        WorkspaceActionLabel.RESUME -> R.string.workspace_action_resume
        WorkspaceActionLabel.RETIRE -> R.string.workspace_action_retire
        WorkspaceActionLabel.RESTORE_DRAFT -> R.string.workspace_action_restore_draft
        WorkspaceActionLabel.START -> R.string.workspace_action_start
        WorkspaceActionLabel.MARK_DONE -> R.string.workspace_action_mark_done
        WorkspaceActionLabel.REOPEN -> R.string.workspace_action_reopen
        WorkspaceActionLabel.CANCEL -> R.string.workspace_action_cancel
        WorkspaceActionLabel.SCHEDULE -> R.string.workspace_action_schedule
        WorkspaceActionLabel.COMPLETE -> R.string.workspace_action_complete
        WorkspaceActionLabel.ACTIVATE -> R.string.workspace_action_activate
        WorkspaceActionLabel.SUPERSEDE -> R.string.workspace_action_supersede
        WorkspaceActionLabel.UNASSIGN -> R.string.workspace_action_unassign
        WorkspaceActionLabel.MARK_OUT_OF_SERVICE -> R.string.workspace_action_out_of_service
        WorkspaceActionLabel.RETURN_TO_SERVICE -> R.string.workspace_action_return_to_service
        WorkspaceActionLabel.VERIFY -> R.string.workspace_action_verify
        WorkspaceActionLabel.NOT_APPLICABLE -> R.string.workspace_action_not_applicable
        WorkspaceActionLabel.RESET -> R.string.workspace_action_reset
        WorkspaceActionLabel.CLOSE -> R.string.workspace_action_close
    },
)

@Composable
private fun issueText(code: WorkspaceIssueCode): String = stringResource(
    when (code) {
        WorkspaceIssueCode.REQUIRED_FIELD -> R.string.workspace_issue_required
        WorkspaceIssueCode.DUPLICATE_ID -> R.string.workspace_issue_duplicate
        WorkspaceIssueCode.DUPLICATE_CONTROL_KIND -> R.string.workspace_issue_duplicate_control_kind
        WorkspaceIssueCode.INVALID_DATE_RANGE -> R.string.workspace_issue_date
        WorkspaceIssueCode.INVALID_QUANTITY -> R.string.workspace_issue_quantity
        WorkspaceIssueCode.STATUS_REQUIREMENT_NOT_MET -> R.string.workspace_issue_status
        WorkspaceIssueCode.ILLEGAL_TRANSITION -> R.string.workspace_issue_transition
        WorkspaceIssueCode.NON_MONOTONIC_STATUS_DATE -> R.string.workspace_issue_status_date
        WorkspaceIssueCode.NO_STATE_CHANGE -> R.string.workspace_issue_no_change
        WorkspaceIssueCode.RECORD_NOT_FOUND -> R.string.workspace_issue_not_found
    },
)

@Composable
private fun dueTimingText(timing: DueTiming): String = stringResource(
    when (timing) {
        DueTiming.OVERDUE -> R.string.workspace_due_overdue
        DueTiming.TODAY -> R.string.workspace_due_today
        DueTiming.UPCOMING -> R.string.workspace_due_upcoming
    },
)

@Composable
private fun dueKindText(kind: DueSoonKind): String = stringResource(
    when (kind) {
        DueSoonKind.PRACTICE_REVIEW -> R.string.workspace_due_practice_review
        DueSoonKind.HOUSEHOLD_RESPONSIBILITY -> R.string.workspace_due_responsibility
        DueSoonKind.HOUSEHOLD_CALENDAR_ITEM -> R.string.workspace_due_calendar
        DueSoonKind.PREPARATION_KIT_TARGET -> R.string.workspace_due_kit
        DueSoonKind.PREPARATION_KIT_TASK -> R.string.workspace_due_kit_task
        DueSoonKind.QAHAL_DECISION_REVIEW -> R.string.workspace_due_decision
        DueSoonKind.VOLUNTEER_SLOT -> R.string.workspace_due_volunteer
        DueSoonKind.INVENTORY_ATTENTION -> R.string.workspace_due_inventory
        DueSoonKind.FINANCIAL_CONTROL -> R.string.workspace_due_financial_control
        DueSoonKind.FINANCIAL_CHECKLIST_REVIEW -> R.string.workspace_due_financial_review
    },
)

@Composable
internal fun calendarKindText(kind: HouseholdCalendarKind): String = stringResource(
    when (kind) {
        HouseholdCalendarKind.PREPARATION -> R.string.workspace_calendar_preparation
        HouseholdCalendarKind.SHARED_MEAL -> R.string.workspace_calendar_meal
        HouseholdCalendarKind.PRAYER -> R.string.workspace_calendar_prayer
        HouseholdCalendarKind.STUDY -> R.string.workspace_calendar_study
        HouseholdCalendarKind.CARE -> R.string.workspace_calendar_care
        HouseholdCalendarKind.HOUSEHOLD_MEETING -> R.string.workspace_calendar_meeting
        HouseholdCalendarKind.OTHER -> R.string.workspace_other
    },
)

@Composable
internal fun decisionTypeText(kind: DecisionClassification): String = stringResource(
    when (kind) {
        DecisionClassification.JUDGMENT -> R.string.workspace_decision_judgment
        DecisionClassification.ENACTMENT -> R.string.workspace_decision_enactment
        DecisionClassification.CUSTOM -> R.string.workspace_decision_custom
        DecisionClassification.POLICY -> R.string.workspace_decision_policy
        DecisionClassification.ADVICE -> R.string.workspace_decision_advice
    },
)

@Composable
internal fun financialControlText(kind: FinancialControlKind): String = stringResource(
    when (kind) {
        FinancialControlKind.DOCUMENTED_AUTHORITY -> R.string.workspace_control_documented_authority
        FinancialControlKind.DUAL_APPROVAL -> R.string.workspace_control_dual_approval
        FinancialControlKind.SEPARATION_OF_ROLES -> R.string.workspace_control_separation
        FinancialControlKind.RECEIPT_RETENTION -> R.string.workspace_control_receipts
        FinancialControlKind.PERIODIC_RECONCILIATION -> R.string.workspace_control_reconciliation
        FinancialControlKind.INDEPENDENT_REVIEW -> R.string.workspace_control_independent_review
        FinancialControlKind.CONFLICT_DISCLOSURE -> R.string.workspace_control_conflict
        FinancialControlKind.MEMBER_REPORTING -> R.string.workspace_control_reporting
    },
)

private fun isolate(value: String): String = "\u2068$value\u2069"
