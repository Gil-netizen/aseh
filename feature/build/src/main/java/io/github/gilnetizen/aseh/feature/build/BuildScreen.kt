package io.github.gilnetizen.aseh.feature.build

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityAdoption
import io.github.gilnetizen.aseh.core.model.CommunityCharter
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.DisputedPracticeDossier
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingSlot
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ReadingPreparationStatus
import io.github.gilnetizen.aseh.core.model.ReadingPassageAvailability
import io.github.gilnetizen.aseh.core.model.ServiceAccessibilityProfile
import io.github.gilnetizen.aseh.core.model.ServiceAssembly
import io.github.gilnetizen.aseh.core.model.ServiceAssemblyContext
import io.github.gilnetizen.aseh.core.model.ServiceReadinessSeverity
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import io.github.gilnetizen.aseh.core.model.assembleService
import io.github.gilnetizen.aseh.core.model.buildServicePacket
import io.github.gilnetizen.aseh.core.model.missingFields
import io.github.gilnetizen.aseh.core.model.practiceProgress
import io.github.gilnetizen.aseh.core.model.readingPlanFor
import io.github.gilnetizen.aseh.core.ui.PacketExportPreviewDialog

@Composable
fun BuildScreen(
    catalog: DemonstratorCatalog? = null,
    serviceOccurrenceSupported: Boolean = true,
    serviceOccurrenceActivating: Boolean = false,
    state: ExperienceState = ExperienceState(),
    dateLabel: String = "Next Shabbat",
    locationLabel: String = "Location not set",
    assemblyContext: ServiceAssemblyContext? = null,
    onWorkspaceSaved: (String, WorkspaceKind) -> Unit = { _, _ -> },
    onRoleAssignmentChanged: (ParticipantRole, String) -> Unit = { _, _ -> },
    onReadingPlanChanged: (String, ReadingPlanEntry) -> Unit = { _, _ -> },
    onDeviceUseModeChanged: (DeviceUseMode) -> Unit = {},
    onCalendarRegionChanged: (CalendarRegion) -> Unit = {},
    onAccessibilityProfileChanged: (
        (ServiceAccessibilityProfile) -> ServiceAccessibilityProfile,
    ) -> Unit = {},
    onCommunityOptionSelected: (String?) -> Unit = {},
    onCommunityCharterSaved: (CommunityCharter) -> Unit = {},
    onDossierAdoptionSaved: (CommunityAdoption) -> Unit = {},
    onDossierFactReviewed: (String, Boolean) -> Unit = { _, _ -> },
    onOpenSource: (String) -> Unit = {},
    onOpenNow: () -> Unit = {},
    onOpenPractice: () -> Unit = {},
    onOpenPrayer: () -> Unit = {},
    onSharePacket: (String) -> Unit = {},
    onPrintPacket: (String) -> Unit = {},
    isDeletingLocalData: Boolean = false,
    localDataDeletionFailed: Boolean = false,
    onDeleteAllLocalData: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var workspaceName by rememberSaveable(state.workspaceName) {
        mutableStateOf(state.workspaceName)
    }
    var workspaceKindName by rememberSaveable(state.workspaceKind.name) {
        mutableStateOf(state.workspaceKind.name)
    }
    var pendingPacketExport by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val rehearsalCatalog = catalog.takeIf { serviceOccurrenceSupported }
    var selectedStageName by rememberSaveable {
        mutableStateOf(recommendedBuildStage(state, rehearsalCatalog).name)
    }
    val selectedStage = BuildStage.valueOf(selectedStageName)
    val screenScrollState = rememberScrollState()
    val workspaceKind = WorkspaceKind.valueOf(workspaceKindName)
    val practice = rehearsalCatalog?.let { practiceProgress(it, state) }
    val assembly = rehearsalCatalog?.let { activeCatalog ->
        if (assemblyContext != null) {
            assembleService(activeCatalog, state, assemblyContext)
        } else {
            assembleService(activeCatalog, state, dateLabel, locationLabel)
        }
    }
    val service = assembly?.let { assembledService ->
        val assembledSegmentIds = assembledService.segments.map { it.segment.id }
        assembledSegmentIds.count(state.completedServiceSegmentIds::contains) to
            assembledSegmentIds.size
    }
    val packetPayload = rehearsalCatalog?.let { activeCatalog ->
        if (assemblyContext != null) {
            buildServicePacket(activeCatalog, state, assemblyContext)
        } else {
            buildServicePacket(activeCatalog, state, dateLabel, locationLabel)
        }
    }
    val localeIsHebrew = LocalConfiguration.current.locales[0].language in setOf("he", "iw")

    LaunchedEffect(selectedStageName) {
        screenScrollState.scrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(screenScrollState)
            .padding(PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Build",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .testTag("build-heading")
                .semantics { heading() },
        )
        Text(
            text = "Set up a local workspace, assign the rehearsal team, record a community choice, and export the current packet.",
            style = MaterialTheme.typography.bodyLarge,
        )

        if (localeIsHebrew) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "EN · This Build workflow is currently an English fallback. Hebrew labels and content review are still incomplete.",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (catalog != null && serviceOccurrenceSupported) {
            NoticeCard(catalog.noticeTitle, catalog.noticeBody)
        } else if (catalog != null && serviceOccurrenceActivating) {
            NoticeCard(
                title = "Activating selected service",
                body = "ASEH is loading this dated occurrence from local storage. Assignment and packet controls will appear when it is ready.",
            )
        } else {
            NoticeCard(
                title = "Service setup unavailable",
                body = "Workspace and local-data settings remain available. Install the required local content and select a rehearsal-capable occurrence before editing assignments or generating a packet.",
            )
        }

        BuildStageSelector(
            selectedStage = selectedStage,
            state = state,
            catalog = rehearsalCatalog,
            assembly = assembly,
            onStageSelected = { selectedStageName = it.name },
        )

        Text(
            text = "Step ${selectedStage.ordinal + 1} of ${BuildStage.entries.size}: ${selectedStage.label}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .testTag("build-stage-content-heading")
                .semantics { heading() },
        )
        Text(
            text = selectedStage.description,
            style = MaterialTheme.typography.bodyLarge,
        )

        if (selectedStage == BuildStage.SETUP) {
            SectionCard(title = "Active workspace") {
            Text(
                text = "Choose who this plan belongs to. Everything here stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                WorkspaceKind.entries.forEach { kind ->
                    FilterChip(
                        selected = workspaceKind == kind,
                        onClick = { workspaceKindName = kind.name },
                        label = { Text(kind.label) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    )
                }
            }
            OutlinedTextField(
                value = workspaceName,
                onValueChange = { workspaceName = it.take(80) },
                label = { Text("Workspace name") },
                supportingText = { Text("For example: Home, Friday group, or your qahal name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("build-workspace-name"),
            )
            Button(
                onClick = { onWorkspaceSaved(workspaceName, workspaceKind) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("build-save-workspace"),
            ) {
                Text("Save workspace")
            }
            }
        }

        if (selectedStage == BuildStage.TEAM_AND_READINGS && rehearsalCatalog != null) {
            SectionCard(title = "Rehearsal team") {
            Text(
                text = "Names are operational assignments for this rehearsal. They make no statement about religious eligibility or office.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ParticipantRole.entries
                .filter { it != ParticipantRole.CONGREGANT }
                .forEach { role ->
                    RoleAssignmentField(
                        role = role,
                        initialValue = state.roleAssignments[role].orEmpty(),
                        onSave = { onRoleAssignmentChanged(role, it) },
                    )
                }
            }
        }

        if (catalog != null) {
            if (selectedStage == BuildStage.SETUP) {
                ServiceContextProfileSection(
                    state = state,
                    onCalendarRegionChanged = onCalendarRegionChanged,
                    onAccessibilityProfileChanged = onAccessibilityProfileChanged,
                )
            }

            if (selectedStage == BuildStage.TEAM_AND_READINGS && serviceOccurrenceSupported) {
                SectionCard(title = "Torah-reading assignments") {
                Text(
                    text = "Assign the seven aliyot and maftir for this rehearsal. These are operational slots only; no sacred text or eligibility decision is supplied.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val readingPlans = catalog.service.readingSlots.associateWith(state::readingPlanFor)
                val openSlots = readingPlans.values.count { plan -> plan.assignee.isBlank() }
                val notReadySlots = readingPlans.values.count { plan ->
                    plan.assignee.isNotBlank() && plan.preparationStatus != ReadingPreparationStatus.READY
                }
                val repeatedAssignees = readingPlans.values
                    .map(ReadingPlanEntry::assignee)
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .groupingBy { it.lowercase() }
                    .eachCount()
                    .filterValues { count -> count > 1 }
                Text(
                    text = "$openSlots of ${catalog.service.readingSlots.size} reading slots remain open.",
                    fontWeight = if (openSlots > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.testTag("build-reading-open-summary"),
                )
                Text(
                    text = "$notReadySlots assigned reading slots are not marked ready.",
                    color = if (notReadySlots > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("build-reading-preparation-summary"),
                )
                if (repeatedAssignees.isNotEmpty()) {
                    Text(
                        text = "One or more people appear in multiple reading slots. Verify that each repeat is intentional.",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("build-reading-repeat-warning"),
                    )
                }
                catalog.service.readingSlots
                    .sortedBy(ReadingSlot::sequence)
                    .forEach { slot ->
                        ReadingPlanField(
                            slot = slot,
                            initialPlan = state.readingPlanFor(slot),
                            onSave = { plan ->
                                onReadingPlanChanged(slot.id, plan)
                            },
                        )
                    }
                if (catalog.service.readingSlots.isEmpty()) {
                    Text("No reading slots are available in this installed content pack.")
                }
                }
            }

            if (selectedStage == BuildStage.PACKET_AND_DATA) {
                SectionCard(title = "Offline output profile") {
                Text(
                    text = "Choose how this workspace plans to use the prepared packet. This records a local operating choice, not a religious ruling.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeviceUseMode.entries.forEach { mode ->
                        val selected = state.deviceUseMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .testTag("build-device-mode-${mode.id}")
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onDeviceUseModeChanged(mode) },
                                ),
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(mode.label, fontWeight = FontWeight.SemiBold)
                                    Text(mode.description, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                }
            }

            if (selectedStage == BuildStage.COMMUNITY) {
                CommunityCharterSection(
                    initialCharter = state.communityCharter.copy(
                        adopted = state.communityCharter.adopted || state.provisionalCharterAdopted,
                    ),
                    onSave = onCommunityCharterSaved,
                )
            }

            if (selectedStage == BuildStage.PACKET_AND_DATA && serviceOccurrenceSupported) {
                SectionCard(title = "Readiness") {
                Text(
                    text = "Practice: ${practice?.first ?: 0} of ${practice?.second ?: 0} steps",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Service: ${service?.first ?: 0} of ${service?.second ?: 0} segments",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (assembly != null) {
                    Text(
                        text = "Status: ${assembly.readiness.status.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (assembly.readiness.blockers.isEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("build-readiness-status"),
                    )
                    if (assembly.readiness.issues.isEmpty()) {
                        Text("All required service context is ready.")
                    } else {
                        assembly.readiness.issues.forEach { issue ->
                            Surface(
                                color = if (issue.severity == ServiceReadinessSeverity.BLOCKER) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                },
                                contentColor = if (issue.severity == ServiceReadinessSeverity.BLOCKER) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("build-readiness-${issue.id}"),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(issue.title, fontWeight = FontWeight.Bold)
                                    Text(issue.detail)
                                }
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onOpenPractice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text("Prepare")
                    }
                    if (assembly?.readiness?.issues?.any { it.id.startsWith("readiness.place") } == true) {
                        OutlinedButton(
                            onClick = onOpenNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("build-set-place"),
                        ) {
                            Text("Set service place")
                        }
                    }
                    Button(
                        onClick = onOpenPrayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text("Rehearse")
                    }
                }
                }
            }

            if (selectedStage == BuildStage.COMMUNITY) {
                catalog.disputedPracticeDossier?.let { dossier ->
                    DisputedPracticeSection(
                        dossier = dossier,
                        state = state,
                        onFactReviewed = onDossierFactReviewed,
                        onAdoptionSaved = onDossierAdoptionSaved,
                        onOpenSource = onOpenSource,
                    )
                }

                SectionCard(title = catalog.communityChoice.title) {
                Text(catalog.communityChoice.summary)
                Text(
                    text = "Conclusion evidence status: ${catalog.communityChoice.conclusionStatus.label}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Review state: ${catalog.communityChoice.reviewState.label}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    catalog.communityChoice.options.forEach { option ->
                        val selected = state.selectedCommunityOptionId == option.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onCommunityOptionSelected(option.id) },
                                ),
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(option.title, fontWeight = FontWeight.SemiBold)
                                    Text(option.argument, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { onCommunityOptionSelected(null) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Leave unresolved")
                    }
                }
                }
            }

            if (selectedStage == BuildStage.PACKET_AND_DATA && serviceOccurrenceSupported) {
                SectionCard(title = "Service packet") {
                Text("$dateLabel · $locationLabel")
                Text("${state.deviceUseMode.label}: the packet contains checklist status, every reading slot, role assignments, accessibility cues, source provenance, and the adopted local option. It contains no prayer text.")
                if (state.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                    Text(
                        text = "Print-only is active. Print and review the final packet before the service; the on-device conductor remains a preparation preview.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Button(
                    onClick = {
                        pendingPacketExport = if (state.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                            PacketExportAction.PRINT.name
                        } else {
                            PacketExportAction.SHARE.name
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(
                            if (state.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                                "build-print-packet"
                            } else {
                                "build-share-packet"
                            },
                        ),
                ) {
                    Text(
                        if (state.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                            "Review and print packet"
                        } else {
                            "Share accessible text packet"
                        },
                    )
                }
                OutlinedButton(
                    onClick = {
                        pendingPacketExport = if (state.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                            PacketExportAction.SHARE.name
                        } else {
                            PacketExportAction.PRINT.name
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(
                            if (state.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                                "build-share-packet"
                            } else {
                                "build-print-packet"
                            },
                        ),
                ) {
                    Text(
                        if (state.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                            "Share accessible text instead"
                        } else {
                            "Print visual copy or save as PDF"
                        },
                    )
                }
                }
            }
        }

        if (selectedStage == BuildStage.PACKET_AND_DATA) {
            SectionCard(title = stringResource(R.string.feature_build_local_data_title)) {
            Text(
                text = stringResource(R.string.feature_build_local_data_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (isDeletingLocalData) {
                Text(
                    text = stringResource(R.string.feature_build_deleting_local_data),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("build-deleting-local-data"),
                )
            }
            if (localDataDeletionFailed) {
                Text(
                    text = stringResource(R.string.feature_build_delete_local_data_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("build-delete-local-data-error"),
                )
            }
            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                enabled = !isDeletingLocalData,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("build-delete-local-data"),
            ) {
                Text(
                    stringResource(
                        if (isDeletingLocalData) {
                            R.string.feature_build_deleting_local_data
                        } else {
                            R.string.feature_build_delete_local_data
                        },
                    ),
                )
            }
            }
        }

        BuildStageNavigation(
            selectedStage = selectedStage,
            onStageSelected = { selectedStageName = it.name },
        )
    }

    val exportAction = pendingPacketExport
        ?.let { name -> PacketExportAction.entries.firstOrNull { it.name == name } }
    if (exportAction != null && packetPayload != null) {
        PacketExportPreviewDialog(
            packet = packetPayload,
            title = stringResource(
                if (exportAction == PacketExportAction.SHARE) {
                    R.string.feature_build_review_share
                } else {
                    R.string.feature_build_review_print
                },
            ),
            accessibleEquivalentLabel = stringResource(
                R.string.feature_build_accessible_equivalent,
            ),
            disclosure = stringResource(R.string.feature_build_export_disclosure),
            confirmLabel = stringResource(
                if (exportAction == PacketExportAction.SHARE) {
                    R.string.feature_build_confirm_share
                } else {
                    R.string.feature_build_confirm_print
                },
            ),
            cancelLabel = stringResource(R.string.feature_build_cancel_export),
            testTagPrefix = "build-export-preview",
            onConfirm = {
                pendingPacketExport = null
                when (exportAction) {
                    PacketExportAction.SHARE -> onSharePacket(packetPayload)
                    PacketExportAction.PRINT -> onPrintPacket(packetPayload)
                }
            },
            onCancel = { pendingPacketExport = null },
        )
    }

    if (showDeleteConfirmation) {
        LocalDataDeletionDialog(
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteAllLocalData()
            },
            onCancel = { showDeleteConfirmation = false },
        )
    }
}

private enum class BuildStage(
    val id: String,
    val label: String,
    val description: String,
) {
    SETUP(
        id = "setup",
        label = "Setup",
        description = "Name the workspace, choose its calendar region, and record the access profile.",
    ),
    TEAM_AND_READINGS(
        id = "team-readings",
        label = "Team & readings",
        description = "Assign the rehearsal team and prepare every Torah-reading slot.",
    ),
    COMMUNITY(
        id = "community",
        label = "Community",
        description = "Draft the local charter and record community operating choices separately from source evidence.",
    ),
    PACKET_AND_DATA(
        id = "packet-data",
        label = "Packet & data",
        description = "Review readiness, choose the offline output, export the exact packet, or manage local data.",
    ),
}

private data class BuildStageStatus(
    val complete: Boolean,
    val summary: String,
)

@Composable
private fun BuildStageSelector(
    selectedStage: BuildStage,
    state: ExperienceState,
    catalog: DemonstratorCatalog?,
    assembly: ServiceAssembly?,
    onStageSelected: (BuildStage) -> Unit,
) {
    val statuses = BuildStage.entries.associateWith { stage ->
        buildStageStatus(stage, state, catalog, assembly)
    }
    val readyCount = statuses.values.count(BuildStageStatus::complete)

    SectionCard(title = "Build steps") {
        Text(
            text = "$readyCount of ${BuildStage.entries.size} sections ready. Choose any section to review or edit it.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("build-stage-progress"),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .testTag("build-stage-selector"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BuildStage.entries.forEach { stage ->
                val status = statuses.getValue(stage)
                val selected = stage == selectedStage
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .testTag("build-stage-${stage.id}")
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onStageSelected(stage) },
                        ),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "${stage.ordinal + 1}. ${stage.label}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (status.complete) "Ready" else "Needs attention",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (status.complete) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.testTag("build-stage-status-${stage.id}"),
                            )
                            Text(
                                text = status.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("build-stage-summary-${stage.id}"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildStageNavigation(
    selectedStage: BuildStage,
    onStageSelected: (BuildStage) -> Unit,
) {
    val previous = BuildStage.entries.getOrNull(selectedStage.ordinal - 1)
    val next = BuildStage.entries.getOrNull(selectedStage.ordinal + 1)

    SectionCard(title = "Continue") {
        if (next != null) {
            Button(
                onClick = { onStageSelected(next) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("build-stage-next"),
            ) {
                Text("Next: ${next.label}")
            }
        }
        if (previous != null) {
            OutlinedButton(
                onClick = { onStageSelected(previous) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("build-stage-previous"),
            ) {
                Text("Back to ${previous.label}")
            }
        }
        if (next == null) {
            Text(
                text = "Use the section chooser above whenever you need to revise an earlier value.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun recommendedBuildStage(
    state: ExperienceState,
    catalog: DemonstratorCatalog?,
): BuildStage = when {
    !isSetupComplete(state, catalog) -> BuildStage.SETUP
    !isTeamAndReadingsComplete(state, catalog) -> BuildStage.TEAM_AND_READINGS
    catalog == null -> BuildStage.PACKET_AND_DATA
    !isCommunityComplete(state, catalog) -> BuildStage.COMMUNITY
    else -> BuildStage.PACKET_AND_DATA
}

private fun buildStageStatus(
    stage: BuildStage,
    state: ExperienceState,
    catalog: DemonstratorCatalog?,
    assembly: ServiceAssembly?,
): BuildStageStatus = when (stage) {
    BuildStage.SETUP -> {
        val workspace = if (state.workspaceName.isBlank()) {
            "No workspace saved"
        } else {
            "${state.workspaceKind.label}: ${state.workspaceName}"
        }
        val context = if (catalog == null) {
            "Content pack needed for calendar and access setup"
        } else {
            val access = if (state.accessibilityProfile.participantNeedsReviewed) {
                "access reviewed"
            } else {
                "access review pending"
            }
            "${state.calendarRegion.label}; $access"
        }
        BuildStageStatus(
            complete = isSetupComplete(state, catalog),
            summary = "$workspace · $context",
        )
    }

    BuildStage.TEAM_AND_READINGS -> {
        val operationalRoles = ParticipantRole.entries.filter { it != ParticipantRole.CONGREGANT }
        val assignedRoles = operationalRoles.count { role ->
            state.roleAssignments[role]?.isNotBlank() == true
        }
        val slots = catalog?.service?.readingSlots.orEmpty()
        val readyReadings = slots.count { slot ->
            state.readingPlanFor(slot).let { plan ->
                plan.assignee.isNotBlank() && plan.preparationStatus == ReadingPreparationStatus.READY
            }
        }
        val readings = if (catalog == null) {
            "readings need a content pack"
        } else {
            "$readyReadings/${slots.size} readings ready"
        }
        BuildStageStatus(
            complete = isTeamAndReadingsComplete(state, catalog),
            summary = "$assignedRoles/${operationalRoles.size} roles assigned · $readings",
        )
    }

    BuildStage.COMMUNITY -> {
        val charter = if (
            (state.communityCharter.adopted || state.provisionalCharterAdopted) &&
            state.communityCharter.missingFields().isEmpty()
        ) {
            "charter adopted"
        } else {
            "charter incomplete"
        }
        val selectedOption = catalog?.communityChoice?.options
            ?.firstOrNull { option -> option.id == state.selectedCommunityOptionId }
            ?.title
            ?: "choice unresolved"
        val dossier = if (catalog?.disputedPracticeDossier == null) {
            null
        } else if (state.disputedPracticeAdoption.missingFields().isEmpty()) {
            "dossier adoption recorded"
        } else {
            "dossier adoption pending"
        }
        BuildStageStatus(
            complete = isCommunityComplete(state, catalog),
            summary = listOfNotNull(charter, selectedOption, dossier).joinToString(" · "),
        )
    }

    BuildStage.PACKET_AND_DATA -> {
        val readiness = assembly?.readiness
        val summary = if (readiness == null) {
            "No packet until a reviewed content pack is installed"
        } else {
            val blockers = if (readiness.blockers.isEmpty()) {
                "no blockers"
            } else {
                "${readiness.blockers.size} blockers"
            }
            "${readiness.status.name.replace('_', ' ')} · $blockers · ${state.deviceUseMode.label}"
        }
        BuildStageStatus(
            complete = readiness?.blockers?.isEmpty() == true,
            summary = summary,
        )
    }
}

private fun isSetupComplete(
    state: ExperienceState,
    catalog: DemonstratorCatalog?,
): Boolean = state.workspaceName.isNotBlank() && (
    catalog == null || (
        state.calendarRegion != CalendarRegion.UNSPECIFIED &&
            state.accessibilityProfile.participantNeedsReviewed
        )
    )

private fun isTeamAndReadingsComplete(
    state: ExperienceState,
    catalog: DemonstratorCatalog?,
): Boolean {
    val rolesComplete = ParticipantRole.entries
        .filter { it != ParticipantRole.CONGREGANT }
        .all { role -> state.roleAssignments[role]?.isNotBlank() == true }
    val readingsComplete = catalog?.service?.readingSlots?.all { slot ->
        state.readingPlanFor(slot).let { plan ->
            plan.assignee.isNotBlank() && plan.preparationStatus == ReadingPreparationStatus.READY
        }
    } ?: true
    return rolesComplete && readingsComplete
}

private fun isCommunityComplete(
    state: ExperienceState,
    catalog: DemonstratorCatalog?,
): Boolean {
    if (catalog == null) return false
    val charterComplete = (
        state.communityCharter.adopted || state.provisionalCharterAdopted
        ) && state.communityCharter.missingFields().isEmpty()
    val choiceComplete = state.selectedCommunityOptionId != null
    val dossierComplete = catalog.disputedPracticeDossier == null ||
        state.disputedPracticeAdoption.missingFields().isEmpty()
    return charterComplete && choiceComplete && dossierComplete
}

private enum class PacketExportAction {
    SHARE,
    PRINT,
}

@Composable
private fun LocalDataDeletionDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag("build-delete-local-data-dialog"),
        title = {
            Text(
                text = stringResource(R.string.feature_build_delete_confirmation_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(stringResource(R.string.feature_build_delete_confirmation_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("build-confirm-delete-local-data"),
            ) {
                Text(stringResource(R.string.feature_build_confirm_delete_local_data))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("build-cancel-delete-local-data"),
            ) {
                Text(stringResource(R.string.feature_build_cancel_delete_local_data))
            }
        },
    )
}

@Composable
private fun ServiceContextProfileSection(
    state: ExperienceState,
    onCalendarRegionChanged: (CalendarRegion) -> Unit,
    onAccessibilityProfileChanged: (
        (ServiceAccessibilityProfile) -> ServiceAccessibilityProfile,
    ) -> Unit,
) {
    SectionCard(title = "Service context and access profile") {
        Text(
            text = "These local settings feed the deterministic service assembler. Every include or omit decision remains visible in Pray and in the packet.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Calendar region", fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CalendarRegion.entries.forEach { region ->
                val selected = state.calendarRegion == region
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onCalendarRegionChanged(region) },
                        )
                        .testTag("build-calendar-region-${region.name.lowercase()}")
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(region.label)
                }
            }
        }

        HorizontalDivider()
        Text("Accessibility profile", fontWeight = FontWeight.Bold)
        Text(
            text = "Record only what the group has actually reviewed. These choices shape the rehearsal workflow throughout the app; they do not diagnose or rank participants.",
            style = MaterialTheme.typography.bodyMedium,
        )
        ProfileToggle(
            label = "Participant needs reviewed with the group",
            checked = state.accessibilityProfile.participantNeedsReviewed,
            testTag = "build-access-reviewed",
        ) {
            onAccessibilityProfileChanged { current ->
                current.copy(participantNeedsReviewed = it)
            }
        }
        ProfileToggle(
            label = "Lead with movement alternatives",
            checked = state.accessibilityProfile.useMovementAlternatives,
            testTag = "build-access-movement",
        ) {
            onAccessibilityProfileChanged { current ->
                current.copy(useMovementAlternatives = it)
            }
        }
        ProfileToggle(
            label = "Use visual voice and response cues",
            checked = state.accessibilityProfile.useVisualVoiceCues,
            testTag = "build-access-visual-cues",
        ) {
            onAccessibilityProfileChanged { current ->
                current.copy(useVisualVoiceCues = it)
            }
        }
        ProfileToggle(
            label = "Prepare large-text output",
            checked = state.accessibilityProfile.useLargeText,
            testTag = "build-access-large-text",
        ) {
            onAccessibilityProfileChanged { current -> current.copy(useLargeText = it) }
        }
        ProfileToggle(
            label = "Use high contrast",
            checked = state.accessibilityProfile.useHighContrast,
            testTag = "build-access-high-contrast",
        ) {
            onAccessibilityProfileChanged { current -> current.copy(useHighContrast = it) }
        }
        ProfileToggle(
            label = "Reduce motion",
            checked = state.accessibilityProfile.reduceMotion,
            testTag = "build-access-reduce-motion",
        ) {
            onAccessibilityProfileChanged { current -> current.copy(reduceMotion = it) }
        }
        ProfileToggle(
            label = "Keep the app awake during rehearsal work",
            checked = state.accessibilityProfile.keepScreenAwake,
            testTag = "build-access-keep-screen-awake",
        ) {
            onAccessibilityProfileChanged { current -> current.copy(keepScreenAwake = it) }
        }
        ProfileToggle(
            label = "Use low-light appearance throughout the rehearsal workflow",
            checked = state.accessibilityProfile.lowLightMode,
            testTag = "build-access-low-light",
        ) {
            onAccessibilityProfileChanged { current -> current.copy(lowLightMode = it) }
        }
    }
}

@Composable
private fun ProfileToggle(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .testTag(testTag)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CommunityCharterSection(
    initialCharter: CommunityCharter,
    onSave: (CommunityCharter) -> Unit,
) {
    var purpose by rememberSaveable(initialCharter.purpose) { mutableStateOf(initialCharter.purpose) }
    var participants by rememberSaveable(initialCharter.participants) {
        mutableStateOf(initialCharter.participants)
    }
    var authorityLimits by rememberSaveable(initialCharter.authorityLimits) {
        mutableStateOf(initialCharter.authorityLimits)
    }
    var decisionProcess by rememberSaveable(initialCharter.decisionProcess) {
        mutableStateOf(initialCharter.decisionProcess)
    }
    var roleTerms by rememberSaveable(initialCharter.roleTerms) {
        mutableStateOf(initialCharter.roleTerms)
    }
    var accessibilityCommitment by rememberSaveable(initialCharter.accessibilityCommitment) {
        mutableStateOf(initialCharter.accessibilityCommitment)
    }
    var effectiveDate by rememberSaveable(initialCharter.effectiveDate) {
        mutableStateOf(initialCharter.effectiveDate)
    }
    var reviewDate by rememberSaveable(initialCharter.reviewDate) {
        mutableStateOf(initialCharter.reviewDate)
    }
    var version by rememberSaveable(initialCharter.version) { mutableStateOf(initialCharter.version) }
    val draft = CommunityCharter(
        purpose = purpose,
        participants = participants,
        authorityLimits = authorityLimits,
        decisionProcess = decisionProcess,
        roleTerms = roleTerms,
        accessibilityCommitment = accessibilityCommitment,
        effectiveDate = effectiveDate,
        reviewDate = reviewDate,
        version = version,
        adopted = initialCharter.adopted,
    )
    val missingFields = draft.missingFields()

    SectionCard(title = "Small-qahal charter") {
        Text(
            text = "A versioned local governance record for this workspace. It cannot create universal authority, civil recognition, or a court.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (initialCharter.adopted) {
                "ADOPTED LOCALLY · ${version.ifBlank { "Draft 1" }}"
            } else {
                "DRAFT · COMMUNITY ENACTMENT TEMPLATE"
            },
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("build-charter-status"),
        )
        OutlinedButton(
            onClick = {
                purpose = "Coordinate prayer, learning, hospitality, mutual responsibility, and review for this small qahal."
                participants = "People who voluntarily join this local workspace and its published participation process."
                authorityLimits = "This qahal records local operating choices only. It does not claim universal law, civil recognition, a court judgment, or authority over nonparticipants."
                decisionProcess = "Seek consensus when practical; record the question, evidence status, scope, dissent, responsible people, effective date, and review date for every adoption."
                roleTerms = "Service and coordination roles are temporary, named for each rehearsal, reviewable, and open to recall or reassignment by the local process."
                accessibilityCommitment = "Ask participants about access needs, provide equal alternatives, minimize stored personal detail, and review the actual room and packet."
                version = "Draft 1"
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("build-charter-template"),
        ) {
            Text("Use the small-qahal template")
        }
        CharterField("Purpose", purpose, "build-charter-purpose") { purpose = it }
        CharterField("Participants and scope", participants, "build-charter-participants") {
            participants = it
        }
        CharterField("Limits of local authority", authorityLimits, "build-charter-authority") {
            authorityLimits = it
        }
        CharterField("Decision and dissent process", decisionProcess, "build-charter-decisions") {
            decisionProcess = it
        }
        CharterField("Role terms, review, and recall", roleTerms, "build-charter-role-terms") {
            roleTerms = it
        }
        CharterField(
            "Accessibility and privacy commitment",
            accessibilityCommitment,
            "build-charter-accessibility",
        ) { accessibilityCommitment = it }
        OutlinedTextField(
            value = version,
            onValueChange = { version = it.take(80) },
            label = { Text("Version") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("build-charter-version"),
        )
        OutlinedTextField(
            value = effectiveDate,
            onValueChange = { effectiveDate = it.take(80) },
            label = { Text("Effective date or event") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("build-charter-effective-date"),
        )
        OutlinedTextField(
            value = reviewDate,
            onValueChange = { reviewDate = it.take(80) },
            label = { Text("Required review date or event") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("build-charter-review-date"),
        )
        if (missingFields.isNotEmpty()) {
            Text(
                text = "Complete before adoption: ${missingFields.joinToString()}.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("build-charter-missing"),
            )
        }
        Button(
            onClick = { onSave(draft) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("build-save-charter"),
        ) {
            Text("Save charter draft")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(
                    value = initialCharter.adopted,
                    enabled = missingFields.isEmpty(),
                    role = Role.Checkbox,
                    onValueChange = { adopted -> onSave(draft.copy(adopted = adopted)) },
                )
                .testTag("build-adopt-charter"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(
                checked = initialCharter.adopted,
                enabled = missingFields.isEmpty(),
                onCheckedChange = null,
            )
            Text(
                if (initialCharter.adopted) {
                    "Adopted for this workspace; tap to return it to draft"
                } else {
                    "Adopt this complete charter for this workspace"
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CharterField(
    label: String,
    value: String,
    testTag: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(600)) },
        label = { Text(label) },
        minLines = 2,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
}

@Composable
private fun DisputedPracticeSection(
    dossier: DisputedPracticeDossier,
    state: ExperienceState,
    onFactReviewed: (String, Boolean) -> Unit,
    onAdoptionSaved: (CommunityAdoption) -> Unit,
    onOpenSource: (String) -> Unit,
) {
    val initialAdoption = state.disputedPracticeAdoption
    var selectedOptionId by rememberSaveable(initialAdoption.optionId) {
        mutableStateOf(initialAdoption.optionId)
    }
    var scope by rememberSaveable(initialAdoption.scope) { mutableStateOf(initialAdoption.scope) }
    var effectiveDate by rememberSaveable(initialAdoption.effectiveDate) {
        mutableStateOf(initialAdoption.effectiveDate)
    }
    var reviewDate by rememberSaveable(initialAdoption.reviewDate) {
        mutableStateOf(initialAdoption.reviewDate)
    }
    var recordedBy by rememberSaveable(initialAdoption.recordedBy) {
        mutableStateOf(initialAdoption.recordedBy)
    }
    val draft = CommunityAdoption(
        optionId = selectedOptionId,
        scope = scope,
        effectiveDate = effectiveDate,
        reviewDate = reviewDate,
        recordedBy = recordedBy,
    )
    val missing = draft.missingFields()

    SectionCard(title = dossier.title) {
        Text(
            text = "EVIDENCE STATUS: ${dossier.conclusionStatus.label.uppercase()}",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("build-dossier-status"),
        )
        Text(
            text = "REVIEW STATE: ${dossier.reviewState.label.uppercase()}",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("build-dossier-review-state"),
        )
        Text(dossier.question, style = MaterialTheme.typography.titleMedium)
        Text(dossier.scope)
        Text("Facts that can change the analysis", fontWeight = FontWeight.Bold)
        dossier.factualQuestions.forEachIndexed { index, question ->
            val factId = "${dossier.id}.fact.${index + 1}"
            val reviewed = factId in state.reviewedDossierFactIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = reviewed,
                        role = Role.Checkbox,
                        onValueChange = { onFactReviewed(factId, it) },
                    )
                    .testTag("build-dossier-fact-${index + 1}")
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(checked = reviewed, onCheckedChange = null)
                Text(question, modifier = Modifier.weight(1f).padding(top = 12.dp))
            }
        }
        Text("Historical / authority layer", fontWeight = FontWeight.Bold)
        Text(dossier.historicalPosition)
        dossier.arguments.forEach { argument ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag("build-dossier-${argument.id}"),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(argument.title, fontWeight = FontWeight.Bold)
                    Text(argument.claim)
                    Text("Why this path may help", fontWeight = FontWeight.SemiBold)
                    argument.supports.forEach { Text("• $it") }
                    Text("Challenges and limits", fontWeight = FontWeight.SemiBold)
                    argument.challenges.forEach { Text("• $it") }
                    argument.sourceIds.forEach { sourceId ->
                        OutlinedButton(
                            onClick = { onOpenSource(sourceId) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Open exact source record: $sourceId")
                        }
                    }
                }
            }
        }
        Text("Editorial conclusion", fontWeight = FontWeight.Bold)
        Text(dossier.editorialConclusion)
        Text(dossier.reviewRequirement)

        HorizontalDivider()
        Text("Separate community adoption", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "A local adoption records what this workspace will do. It never changes the dossier's ${dossier.conclusionStatus.label} evidence status.",
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dossier.adoptionOptions.forEach { option ->
                val selected = selectedOptionId == option.id
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { selectedOptionId = option.id },
                        )
                        .testTag("build-dossier-option-${option.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(option.title, fontWeight = FontWeight.Bold)
                            Text(option.practice)
                            Text(option.scopeNote, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        CharterField("Adoption scope", scope, "build-dossier-adoption-scope") { scope = it }
        OutlinedTextField(
            value = effectiveDate,
            onValueChange = { effectiveDate = it.take(80) },
            label = { Text("Effective date or event") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("build-dossier-effective-date"),
        )
        OutlinedTextField(
            value = reviewDate,
            onValueChange = { reviewDate = it.take(80) },
            label = { Text("Review date or event") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("build-dossier-review-date"),
        )
        OutlinedTextField(
            value = recordedBy,
            onValueChange = { recordedBy = it.take(80) },
            label = { Text("Recorded by") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("build-dossier-recorded-by"),
        )
        if (missing.isNotEmpty()) {
            Text(
                text = "Complete to record adoption: ${missing.joinToString()}.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("build-dossier-adoption-missing"),
            )
        }
        Button(
            onClick = { onAdoptionSaved(draft) },
            enabled = missing.isEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("build-save-dossier-adoption"),
        ) {
            Text("Record local adoption")
        }
        OutlinedButton(
            onClick = {
                selectedOptionId = null
                scope = ""
                effectiveDate = ""
                reviewDate = ""
                recordedBy = ""
                onAdoptionSaved(CommunityAdoption())
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Clear adoption and leave dossier unresolved")
        }
    }
}

@Composable
private fun ReadingPlanField(
    slot: ReadingSlot,
    initialPlan: ReadingPlanEntry,
    onSave: (ReadingPlanEntry) -> Unit,
) {
    var portionTitle by rememberSaveable(slot.id, initialPlan.portionTitle) {
        mutableStateOf(initialPlan.portionTitle)
    }
    var locator by rememberSaveable(slot.id, initialPlan.locator) {
        mutableStateOf(initialPlan.locator)
    }
    var passageRange by rememberSaveable(slot.id, initialPlan.passageRange) {
        mutableStateOf(initialPlan.passageRange)
    }
    var assignee by rememberSaveable(slot.id, initialPlan.assignee) {
        mutableStateOf(initialPlan.assignee)
    }
    var backupAssignee by rememberSaveable(slot.id, initialPlan.backupAssignee) {
        mutableStateOf(initialPlan.backupAssignee)
    }
    var preparationStatusName by rememberSaveable(slot.id, initialPlan.preparationStatus.name) {
        mutableStateOf(initialPlan.preparationStatus.name)
    }
    var manualOverride by rememberSaveable(slot.id, initialPlan.manualOverride) {
        mutableStateOf(initialPlan.manualOverride)
    }
    var overrideReason by rememberSaveable(slot.id, initialPlan.overrideReason) {
        mutableStateOf(initialPlan.overrideReason)
    }
    val preparationStatus = ReadingPreparationStatus.valueOf(preparationStatusName)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("build-reading-plan-${slot.id}"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("${slot.sequence}. ${slot.label}", fontWeight = FontWeight.Bold)
            Text(slot.description, style = MaterialTheme.typography.bodySmall)

            if (!manualOverride) {
                if (slot.passageAvailability == ReadingPassageAvailability.AVAILABLE) {
                    Text(
                        text = listOf(slot.portionTitle, slot.locator, slot.passageRange)
                            .filter(String::isNotBlank)
                            .joinToString(" · "),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Installed source: ${slot.sourceId ?: "Not recorded"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = "PASSAGE UNAVAILABLE · ${slot.passageUnavailableReason}",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("build-reading-passage-unavailable-${slot.id}"),
                    )
                }
            }

            ProfileToggle(
                label = "Use a manual local passage override",
                checked = manualOverride,
                testTag = "build-reading-manual-override-${slot.id}",
            ) { enabled ->
                manualOverride = enabled
                if (!enabled) {
                    portionTitle = slot.portionTitle
                    locator = slot.locator
                    passageRange = slot.passageRange
                    overrideReason = ""
                }
            }
            if (manualOverride) {
                Text(
                    text = "Manual passage details are local planning data. ASEH does not verify them against an installed edition.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = portionTitle,
                    onValueChange = { portionTitle = it.take(80) },
                    label = { Text("Portion title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("build-reading-portion-${slot.id}"),
                )
                OutlinedTextField(
                    value = locator,
                    onValueChange = { locator = it.take(80) },
                    label = { Text("Book / locator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("build-reading-locator-${slot.id}"),
                )
                OutlinedTextField(
                    value = passageRange,
                    onValueChange = { passageRange = it.take(80) },
                    label = { Text("Verse or section range") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("build-reading-range-${slot.id}"),
                )
                OutlinedTextField(
                    value = overrideReason,
                    onValueChange = { overrideReason = it.take(600) },
                    label = { Text("Override source or reason") },
                    supportingText = { Text("Record how the group verified this manual plan.") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("build-reading-override-reason-${slot.id}"),
                )
            }

            OutlinedTextField(
                value = assignee,
                onValueChange = { assignee = it.take(80) },
                label = { Text("Primary reader") },
                supportingText = { Text(if (assignee.isBlank()) "Open assignment" else slot.description) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("build-reading-${slot.id}"),
            )
            OutlinedTextField(
                value = backupAssignee,
                onValueChange = { backupAssignee = it.take(80) },
                label = { Text("Backup reader") },
                supportingText = { Text("Optional operational backup for this rehearsal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("build-reading-backup-${slot.id}"),
            )

            Text("Preparation status", fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ReadingPreparationStatus.entries.forEach { status ->
                    FilterChip(
                        selected = preparationStatus == status,
                        onClick = { preparationStatusName = status.name },
                        label = { Text(status.label) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("build-reading-status-${slot.id}-${status.name.lowercase()}"),
                    )
                }
            }
            if (manualOverride && (
                    portionTitle.isBlank() || locator.isBlank() || passageRange.isBlank() || overrideReason.isBlank()
                )
            ) {
                Text(
                    text = "Complete the portion, locator, range, and verification note before saving a manual override.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(
                // Read each snapshot value at click time. This prevents a quick
                // final edit followed by Save from persisting the prior frame's
                // draft before Compose has recomposed the button callback.
                onClick = {
                    onSave(
                        ReadingPlanEntry(
                            portionTitle = portionTitle,
                            locator = locator,
                            passageRange = passageRange,
                            assignee = assignee,
                            backupAssignee = backupAssignee,
                            preparationStatus = ReadingPreparationStatus.valueOf(preparationStatusName),
                            manualOverride = manualOverride,
                            overrideReason = overrideReason,
                        ),
                    )
                },
                enabled = !manualOverride || (
                    portionTitle.isNotBlank() && locator.isNotBlank() &&
                        passageRange.isNotBlank() && overrideReason.isNotBlank()
                    ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("build-save-reading-${slot.id}"),
            ) {
                Text("Save ${slot.label}")
            }
        }
    }
}

@Composable
private fun RoleAssignmentField(
    role: ParticipantRole,
    initialValue: String,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(role.id, initialValue) { mutableStateOf(initialValue) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it.take(80) },
            label = { Text(role.label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("build-role-${role.id}"),
        )
        OutlinedButton(
            onClick = { onSave(value) },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("build-save-role-${role.id}"),
        ) {
            Text("Save ${role.label.lowercase()}")
        }
    }
}

@Composable
private fun NoticeCard(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            HorizontalDivider()
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun BuildPreview() {
    BuildScreen()
}
