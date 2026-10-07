package io.github.gilnetizen.aseh.feature.prayer

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.AssembledServiceSegment
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPassageAvailability
import io.github.gilnetizen.aseh.core.model.ReadingSlot
import io.github.gilnetizen.aseh.core.model.RehearsalReadinessOverride
import io.github.gilnetizen.aseh.core.model.ServiceAssembly
import io.github.gilnetizen.aseh.core.model.ServiceAssemblyContext
import io.github.gilnetizen.aseh.core.model.ServiceOrderingDecision
import io.github.gilnetizen.aseh.core.model.ServiceReadiness
import io.github.gilnetizen.aseh.core.model.ServiceReadinessSeverity
import io.github.gilnetizen.aseh.core.model.ServiceReadinessStatus
import io.github.gilnetizen.aseh.core.model.ServiceSegment
import io.github.gilnetizen.aseh.core.model.ServiceSegmentContent
import io.github.gilnetizen.aseh.core.model.ServiceTextAvailability
import io.github.gilnetizen.aseh.core.model.ServiceTextField
import io.github.gilnetizen.aseh.core.model.SourceUnit
import io.github.gilnetizen.aseh.core.model.assembleService
import io.github.gilnetizen.aseh.core.model.buildServicePacket
import io.github.gilnetizen.aseh.core.model.readingPlanFor
import io.github.gilnetizen.aseh.core.ui.PacketExportPreviewDialog
import io.github.gilnetizen.aseh.domain.servicecatalog.DatedServiceInstance
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceAgenda
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceAvailabilityStatus
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionResult
import java.time.format.DateTimeFormatter
import java.util.Locale

private val scheduledServiceFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.ENGLISH)

/**
 * A local, role-aware service rehearsal. Catalog content is supplied by the
 * application flavor, and this screen deliberately never renders prayer text.
 */
@SuppressLint("ModifierParameter")
@Composable
fun PrayerScreen(
    catalog: DemonstratorCatalog? = null,
    state: ExperienceState = ExperienceState(),
    dateLabel: String = "Next Shabbat",
    locationLabel: String = "Location not set",
    assemblyContext: ServiceAssemblyContext? = null,
    serviceSelection: ServiceSelectionResult? = null,
    selectedScheduledServiceId: String? = null,
    selectedScheduledService: DatedServiceInstance? = null,
    selectedServiceContentSupported: Boolean = true,
    serviceOccurrenceReady: Boolean = true,
    onScheduledServiceSelected: (String) -> Unit = {},
    onRoleSelected: (ParticipantRole) -> Unit = {},
    onPreflightCompleted: (String, Boolean) -> Unit = { _, _ -> },
    onSegmentCompleted: (String, Boolean) -> Unit = { _, _ -> },
    onOpenSource: (String) -> Unit = {},
    onOpenPractice: () -> Unit = {},
    onOpenBuild: () -> Unit = {},
    onOpenNow: () -> Unit = {},
    onSharePacket: (String) -> Unit = {},
    onPrintPacket: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (catalog == null) {
        UnavailablePrayerScreen(
            onOpenBuild = onOpenBuild,
            modifier = modifier,
        )
        return
    }

    if (!selectedServiceContentSupported) {
        UnsupportedSelectedServiceScreen(
            catalog = catalog,
            selection = serviceSelection,
            selectedServiceId = selectedScheduledServiceId,
            selectedService = selectedScheduledService,
            onServiceSelected = onScheduledServiceSelected,
            modifier = modifier,
        )
        return
    }

    if (!serviceOccurrenceReady) {
        ActivatingSelectedServiceScreen(
            selectedService = selectedScheduledService,
            modifier = modifier,
        )
        return
    }

    val baseContext = assemblyContext ?: assembleService(
        catalog = catalog,
        state = state,
        dateLabel = dateLabel,
        locationLabel = locationLabel,
    ).context
    val serviceInstanceKey = selectedScheduledServiceId
        ?: "${catalog.service.id}:${baseContext.date.civilDate}"
    var rehearsalOverrideEnabled by rememberSaveable(serviceInstanceKey) {
        mutableStateOf(false)
    }
    val effectiveContext = baseContext.copy(
        rehearsalOverride = if (rehearsalOverrideEnabled) {
            RehearsalReadinessOverride.Proceed(
                reason = "The user explicitly opened a development rehearsal preview while setup blockers remained visible.",
            )
        } else {
            RehearsalReadinessOverride.None
        },
    )
    val assembly = assembleService(catalog, state, effectiveContext)
    val assembledSegments = assembly.segments
    val segments = assembly.segments.map { it.segment }
    var focusedRun by remember(serviceInstanceKey) { mutableStateOf(false) }
    var pendingPacketExport by rememberSaveable(serviceInstanceKey) {
        mutableStateOf<String?>(null)
    }
    var currentSegmentIndex by remember(serviceInstanceKey) {
        mutableIntStateOf(
            initialSegmentIndex(
                segments = segments,
                completedSegmentIds = state.completedServiceSegmentIds,
            ),
        )
    }
    val boundedCurrentIndex = boundedSegmentIndex(
        currentIndex = currentSegmentIndex,
        change = 0,
        segmentCount = segments.size,
    )
    val currentSegment = assembledSegments.getOrNull(boundedCurrentIndex)
    val packetPayload = buildServicePacket(
        catalog = catalog,
        state = state,
        context = effectiveContext,
    )
    LaunchedEffect(serviceInstanceKey, assembly.readiness.status, rehearsalOverrideEnabled) {
        if (!canEnterFocusedConductor(assembly.readiness)) {
            focusedRun = false
        }
    }
    BackHandler(enabled = focusedRun) { focusedRun = false }

    if (focusedRun) {
        FocusedConductorScreen(
            catalog = catalog,
            state = state,
            currentSegment = currentSegment,
            currentSegmentIndex = boundedCurrentIndex,
            segmentCount = assembledSegments.size,
            onOpenOverview = { focusedRun = false },
            onPrevious = {
                currentSegmentIndex = boundedSegmentIndex(
                    currentIndex = boundedCurrentIndex,
                    change = -1,
                    segmentCount = segments.size,
                )
            },
            onNext = {
                currentSegmentIndex = boundedSegmentIndex(
                    currentIndex = boundedCurrentIndex,
                    change = 1,
                    segmentCount = segments.size,
                )
            },
            onSegmentCompleted = onSegmentCompleted,
            onOpenSource = onOpenSource,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = 24.dp,
                    top = 32.dp,
                    end = 24.dp,
                    bottom = 48.dp,
                ),
            )
            .testTag("prayer-screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_prayer_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .semantics { heading() }
                .testTag("prayer-heading"),
        )

        if (serviceSelection != null) {
            ServiceScheduleSection(
                selection = serviceSelection,
                selectedServiceId = selectedScheduledServiceId,
                onServiceSelected = onScheduledServiceSelected,
            )
            selectedScheduledService?.let { service ->
                SelectedServiceContextSection(
                    service = service,
                    contentSupported = true,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer-planner-boundary"),
            ) {
                Text(
                    text = stringResource(R.string.feature_prayer_planner_boundary),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        DevelopmentNotice(catalog = catalog)

        RehearsalEntrySection(
            assembly = assembly,
            state = state,
            onOpenNow = onOpenNow,
            onOpenPractice = onOpenPractice,
            onOpenBuild = onOpenBuild,
            onStart = {
                currentSegmentIndex = initialSegmentIndex(
                    segments = segments,
                    completedSegmentIds = state.completedServiceSegmentIds,
                )
                focusedRun = true
            },
            onOverrideStart = {
                rehearsalOverrideEnabled = true
                currentSegmentIndex = initialSegmentIndex(
                    segments = segments,
                    completedSegmentIds = state.completedServiceSegmentIds,
                )
                focusedRun = true
            },
        )

        OverviewSection(
            catalog = catalog,
            assembly = assembly,
            state = state,
            dateLabel = dateLabel,
            locationLabel = locationLabel,
            onOpenBuild = onOpenBuild,
        )

        AssemblyDecisionSection(assembly)

        PreflightSection(
            catalog = catalog,
            state = state,
            onPreflightCompleted = onPreflightCompleted,
        )

        RoleSection(
            selectedRole = state.selectedRole,
            onRoleSelected = onRoleSelected,
        )

        RoleViewSection(
            catalog = catalog,
            assembly = assembly,
            state = state,
            onOpenBuild = onOpenBuild,
        )

        TimelineSection(
            segments = segments,
            completedSegmentIds = state.completedServiceSegmentIds,
            currentSegmentIndex = boundedCurrentIndex,
            enabled = canEnterFocusedConductor(assembly.readiness),
            onSegmentSelected = {
                if (canEnterFocusedConductor(assembly.readiness)) {
                    currentSegmentIndex = it
                    focusedRun = true
                }
            },
        )

        PacketSection(
            onSharePacket = { pendingPacketExport = PacketExportAction.SHARE.name },
            onPrintPacket = { pendingPacketExport = PacketExportAction.PRINT.name },
        )
    }

    val exportAction = pendingPacketExport
        ?.let { name -> PacketExportAction.entries.firstOrNull { it.name == name } }
    if (exportAction != null) {
        PacketExportPreviewDialog(
            packet = packetPayload,
            title = stringResource(
                if (exportAction == PacketExportAction.SHARE) {
                    R.string.feature_prayer_review_share
                } else {
                    R.string.feature_prayer_review_print
                },
            ),
            accessibleEquivalentLabel = stringResource(
                R.string.feature_prayer_accessible_equivalent,
            ),
            disclosure = stringResource(R.string.feature_prayer_export_disclosure),
            confirmLabel = stringResource(
                if (exportAction == PacketExportAction.SHARE) {
                    R.string.feature_prayer_confirm_share
                } else {
                    R.string.feature_prayer_confirm_print
                },
            ),
            cancelLabel = stringResource(R.string.feature_prayer_cancel_export),
            testTagPrefix = "prayer-export-preview",
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
}

@Composable
private fun ActivatingSelectedServiceScreen(
    selectedService: DatedServiceInstance?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 48.dp))
            .testTag("prayer-screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_prayer_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() }.testTag("prayer-heading"),
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("prayer-selected-service-activating"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.feature_prayer_selected_activating_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.feature_prayer_selected_activating_detail),
                    style = MaterialTheme.typography.bodyLarge,
                )
                selectedService?.let { service ->
                    LabelValue(
                        label = stringResource(R.string.feature_prayer_selected_service_name),
                        value = service.definition.title.fallbackEnglish,
                        testTag = "prayer-activating-service-name",
                    )
                    LabelValue(
                        label = stringResource(R.string.feature_prayer_selected_service_date),
                        value = service.serviceDate.toString(),
                        testTag = "prayer-activating-service-date",
                    )
                }
            }
        }
    }
}

@Composable
private fun UnsupportedSelectedServiceScreen(
    catalog: DemonstratorCatalog,
    selection: ServiceSelectionResult?,
    selectedServiceId: String?,
    selectedService: DatedServiceInstance?,
    onServiceSelected: (String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = 24.dp,
                    top = 32.dp,
                    end = 24.dp,
                    bottom = 48.dp,
                ),
            )
            .testTag("prayer-screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_prayer_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .semantics { heading() }
                .testTag("prayer-heading"),
        )
        if (selection != null) {
            ServiceScheduleSection(
                selection = selection,
                selectedServiceId = selectedServiceId,
                onServiceSelected = onServiceSelected,
            )
        }
        if (selectedService == null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer-selected-service-unsupported"),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feature_prayer_no_selected_service_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.feature_prayer_no_selected_service_detail),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            SelectedServiceContextSection(
                service = selectedService,
                contentSupported = false,
            )
        }
        DevelopmentNotice(catalog = catalog)
    }
}

@Composable
private fun SelectedServiceContextSection(
    service: DatedServiceInstance,
    contentSupported: Boolean,
) {
    Surface(
        color = if (contentSupported) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (contentSupported) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(
                if (contentSupported) {
                    "prayer-selected-service-context"
                } else {
                    "prayer-selected-service-unsupported"
                },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    if (contentSupported) {
                        R.string.feature_prayer_selected_context_title
                    } else {
                        R.string.feature_prayer_selected_unsupported_title
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(
                    if (contentSupported) {
                        R.string.feature_prayer_selected_context_detail
                    } else {
                        R.string.feature_prayer_selected_unsupported_detail
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_selected_service_name),
                value = service.definition.title.fallbackEnglish,
                testTag = "prayer-selected-service-name",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_selected_service_date),
                value = service.serviceDate.toString(),
                testTag = "prayer-selected-service-civil-date",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_selected_service_day_kind),
                value = service.calendarDay.label.fallbackEnglish,
                testTag = "prayer-selected-service-day-kind",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_selected_service_time),
                value = scheduledServiceFormatter.format(service.startsAt),
                testTag = "prayer-selected-service-time",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_selected_service_profile),
                value = service.opinionProfile.title.fallbackEnglish,
                testTag = "prayer-selected-service-profile",
            )
            if (!contentSupported) {
                Text(
                    text = stringResource(R.string.feature_prayer_select_supported_service),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ServiceScheduleSection(
    selection: ServiceSelectionResult,
    selectedServiceId: String?,
    onServiceSelected: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-service-chooser"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_prayer_choose_service_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.feature_prayer_choose_service_detail),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (selection) {
                is ServiceSelectionResult.Blocked -> selection.reasons.forEach { reason ->
                    Text(
                        text = "${reason.title.fallbackEnglish}: ${reason.detail.fallbackEnglish}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    reason.suggestedAction?.let { action ->
                        Text(
                            text = action.fallbackEnglish,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is ServiceSelectionResult.Available -> ServiceScheduleContents(
                    agenda = selection.agenda,
                    selectedServiceId = selectedServiceId,
                    onServiceSelected = onServiceSelected,
                )
            }
        }
    }
}

@Composable
private fun ServiceScheduleContents(
    agenda: ServiceAgenda,
    selectedServiceId: String?,
    onServiceSelected: (String) -> Unit,
) {
    Text(
        text = agenda.catalogNotice.fallbackEnglish,
        style = MaterialTheme.typography.bodySmall,
    )
    val services = visibleScheduledServices(
        upcomingServices = agenda.upcomingServices,
        selectedServiceId = selectedServiceId,
    )
    if (services.isEmpty()) {
        Text(stringResource(R.string.feature_prayer_no_upcoming_services))
        return
    }
    services.forEach { service ->
        ServiceChoiceCard(
            service = service,
            selected = service.id == selectedServiceId,
            onSelected = { onServiceSelected(service.id) },
        )
    }
    if (agenda.upcomingServices.size > services.size) {
        Text(
            text = stringResource(
                R.string.feature_prayer_more_services,
                agenda.upcomingServices.size - services.size,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ServiceChoiceCard(
    service: DatedServiceInstance,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-service-${service.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = service.definition.title.fallbackEnglish,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(scheduledServiceFormatter.format(service.startsAt))
            Text(
                text = service.preparationHorizon.title.fallbackEnglish,
                style = MaterialTheme.typography.labelMedium,
            )
            service.calendarAdditions.forEach { addition ->
                Text(
                    text = "${addition.title.fallbackEnglish}: ${addition.summary.fallbackEnglish}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val availabilityLabel = when (service.availability.status) {
                ServiceAvailabilityStatus.AVAILABLE ->
                    stringResource(R.string.feature_prayer_service_available)
                ServiceAvailabilityStatus.PLANNING_ONLY ->
                    stringResource(R.string.feature_prayer_service_planning_only)
                ServiceAvailabilityStatus.BLOCKED ->
                    stringResource(R.string.feature_prayer_service_blocked)
            }
            Text(
                text = availabilityLabel,
                style = MaterialTheme.typography.labelLarge,
            )
            service.availability.reasons.forEach { reason ->
                Text(
                    text = reason.detail.fallbackEnglish,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                onClick = onSelected,
                enabled = !selected && service.availability.status != ServiceAvailabilityStatus.BLOCKED,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("prayer-service-action-${service.id}"),
            ) {
                Text(
                    stringResource(
                        if (selected) {
                            R.string.feature_prayer_service_selected
                        } else {
                            R.string.feature_prayer_review_service
                        },
                    ),
                )
            }
        }
    }
}

private enum class PacketExportAction {
    SHARE,
    PRINT,
}

@Composable
private fun AssemblyDecisionSection(assembly: ServiceAssembly) {
    val decision = assembly.orderingDecision
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-assembly-decision"),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_prayer_assembled_order),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            CatalogText(
                text = decision.explanation,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.feature_prayer_assembly_status,
                    decision.conclusionStatus.label,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(
                    R.string.feature_prayer_assembly_review_state,
                    decision.reviewState.label,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.feature_prayer_context_decisions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            assembly.contextFacts.forEach { fact ->
                CatalogText(
                    text = "${fact.label}: ${fact.value}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            assembly.compositionDecisions.forEach { composition ->
                val action = composition.action.name.lowercase().replaceFirstChar(Char::uppercase)
                CatalogText(
                    text = "$action · ${composition.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("prayer-composition-${composition.id}"),
                )
                composition.facts.forEach { fact ->
                    CatalogText(
                        text = "• $fact",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnavailablePrayerScreen(
    onOpenBuild: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("prayer-unavailable"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_prayer_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.feature_prayer_unavailable_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.feature_prayer_unavailable_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onOpenBuild,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("prayer-unavailable-open-build"),
        ) {
            Text(stringResource(R.string.feature_prayer_open_build))
        }
    }
}

@Composable
private fun DevelopmentNotice(catalog: DemonstratorCatalog) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-development-notice"),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_prayer_development_badge),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            CatalogText(
                text = catalog.noticeTitle,
                style = MaterialTheme.typography.labelLarge,
            )
            CatalogText(
                text = catalog.noticeBody,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RehearsalEntrySection(
    assembly: ServiceAssembly,
    state: ExperienceState,
    onOpenNow: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenBuild: () -> Unit,
    onStart: () -> Unit,
    onOverrideStart: () -> Unit,
) {
    val segmentIds = assembly.segments.map { it.segment.id }
    val completedCount = segmentIds.count(state.completedServiceSegmentIds::contains)
    val progress = completedCount to segmentIds.size
    val actionLabel = when (
        rehearsalEntryAction(
            segments = assembly.segments.map { it.segment },
            completedSegmentIds = state.completedServiceSegmentIds,
        )
    ) {
        RehearsalEntryAction.START -> R.string.feature_prayer_start_rehearsal
        RehearsalEntryAction.RESUME -> R.string.feature_prayer_resume_rehearsal
        RehearsalEntryAction.REVIEW -> R.string.feature_prayer_review_rehearsal
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-rehearsal-entry"),
        color = if (assembly.readiness.status == ServiceReadinessStatus.BLOCKED) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (assembly.readiness.status == ServiceReadinessStatus.BLOCKED) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = when (assembly.readiness.status) {
                    ServiceReadinessStatus.READY -> stringResource(R.string.feature_prayer_ready_title)
                    ServiceReadinessStatus.BLOCKED -> stringResource(R.string.feature_prayer_blocked_title)
                    ServiceReadinessStatus.OVERRIDDEN_FOR_REHEARSAL -> {
                        stringResource(R.string.feature_prayer_override_title)
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = pluralStringResource(
                    R.plurals.feature_prayer_progress,
                    progress.first,
                    progress.first,
                    progress.second,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            ReadinessIssues(readiness = assembly.readiness)
            if (assembly.context.deviceUseMode == DeviceUseMode.PRINT_ONLY) {
                Text(
                    text = stringResource(R.string.feature_prayer_print_only_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("prayer-print-only-notice"),
                )
            }
            Button(
                onClick = onStart,
                enabled = assembly.segments.isNotEmpty() && assembly.readiness.canBeginRehearsal,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("prayer-start-resume"),
            ) {
                Text(stringResource(actionLabel))
            }
            if (assembly.readiness.status == ServiceReadinessStatus.BLOCKED) {
                if (assembly.readiness.issues.any { issue ->
                        issue.id.startsWith("readiness.date") ||
                            issue.id.startsWith("readiness.place")
                    }
                ) {
                    OutlinedButton(
                        onClick = onOpenNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("prayer-readiness-open-now"),
                    ) {
                        Text(stringResource(R.string.feature_prayer_set_date_place))
                    }
                }
                if (assembly.readiness.issues.any { issue ->
                        issue.id.startsWith("readiness.preparation")
                    }
                ) {
                    OutlinedButton(
                        onClick = onOpenPractice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("prayer-readiness-open-practice"),
                    ) {
                        Text(stringResource(R.string.feature_prayer_complete_preparation))
                    }
                }
                if (assembly.readiness.issues.any { issue ->
                        issue.id.startsWith("readiness.calendar-region") ||
                            issue.id.startsWith("readiness.role") ||
                            issue.id.startsWith("readiness.readings")
                    }
                ) {
                    OutlinedButton(
                        onClick = onOpenBuild,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("prayer-readiness-open-build"),
                    ) {
                        Text(stringResource(R.string.feature_prayer_complete_build_setup))
                    }
                }
                OutlinedButton(
                    onClick = onOverrideStart,
                    enabled = assembly.segments.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("prayer-preview-with-blockers"),
                ) {
                    Text(stringResource(R.string.feature_prayer_preview_with_blockers))
                }
                Text(
                    text = stringResource(R.string.feature_prayer_preview_disclosure),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReadinessIssues(readiness: ServiceReadiness) {
    if (readiness.issues.isEmpty()) {
        Text(
            text = stringResource(R.string.feature_prayer_ready_detail),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("prayer-readiness-ready"),
        )
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.testTag("prayer-readiness-issues"),
    ) {
        readiness.issues.forEach { issue ->
            val severity = stringResource(
                if (issue.severity == ServiceReadinessSeverity.BLOCKER) {
                    R.string.feature_prayer_blocker
                } else {
                    R.string.feature_prayer_warning
                },
            )
            Text(
                text = "$severity · ${issue.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("prayer-readiness-${issue.id}"),
            )
            Text(
                text = issue.detail,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        readiness.overrideReason?.let { reason ->
            Text(
                text = stringResource(R.string.feature_prayer_override_reason, reason),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("prayer-override-reason"),
            )
        }
    }
}

@Composable
private fun FocusedConductorScreen(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    currentSegment: AssembledServiceSegment?,
    currentSegmentIndex: Int,
    segmentCount: Int,
    onOpenOverview: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSegmentCompleted: (String, Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            .testTag("prayer-focused-conductor"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onOpenOverview,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("prayer-open-overview"),
        ) {
            Text(stringResource(R.string.feature_prayer_overview_setup))
        }
        Text(
            text = stringResource(R.string.feature_prayer_development_badge),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (currentSegment == null) {
            Text(
                text = stringResource(R.string.feature_prayer_no_segments),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .testTag("prayer-no-segments"),
            )
        } else {
            val segment = currentSegment.segment
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
            ) {
                ConductorSection(
                    catalog = catalog,
                    state = state,
                    assembledSegment = currentSegment,
                    segmentIndex = currentSegmentIndex,
                    segmentCount = segmentCount,
                    onOpenSource = onOpenSource,
                )
            }
            ConductorControls(
                segment = segment,
                segmentIndex = currentSegmentIndex,
                segmentCount = segmentCount,
                completed = segment.id in state.completedServiceSegmentIds,
                onPrevious = onPrevious,
                onNext = onNext,
                onSegmentCompleted = onSegmentCompleted,
            )
        }
    }
}

@Composable
private fun OverviewSection(
    catalog: DemonstratorCatalog,
    assembly: ServiceAssembly,
    state: ExperienceState,
    dateLabel: String,
    locationLabel: String,
    onOpenBuild: () -> Unit,
) {
    val progress = assembledServiceProgress(
        assembledSegments = assembly.segments,
        completedSegmentIds = state.completedServiceSegmentIds,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("prayer-overview"),
    ) {
        SectionHeading(R.string.feature_prayer_overview)
        CatalogText(
            text = catalog.service.title,
            style = MaterialTheme.typography.headlineSmall,
        )
        CatalogText(
            text = catalog.service.subtitle,
            style = MaterialTheme.typography.bodyLarge,
        )
        LabelValue(
            label = stringResource(R.string.feature_prayer_date),
            value = dateLabel,
            testTag = "prayer-date",
        )
        LabelValue(
            label = stringResource(R.string.feature_prayer_place),
            value = locationLabel,
            testTag = "prayer-location",
        )
        LabelValue(
            label = stringResource(R.string.feature_prayer_view),
            value = state.selectedRole.label,
            testTag = "prayer-current-role",
        )
        Text(
            text = pluralStringResource(
                R.plurals.feature_prayer_progress,
                progress.first,
                progress.first,
                progress.second,
            ),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("prayer-progress"),
        )
        OutlinedButton(
            onClick = onOpenBuild,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("prayer-open-build"),
        ) {
            Text(stringResource(R.string.feature_prayer_open_build))
        }
    }
}

@Composable
private fun PreflightSection(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    onPreflightCompleted: (String, Boolean) -> Unit,
) {
    val completedCount = catalog.service.preflightSteps.count {
        it.id in state.completedPreflightStepIds
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("prayer-preflight"),
    ) {
        SectionHeading(R.string.feature_prayer_preflight)
        Text(
            text = pluralStringResource(
                R.plurals.feature_prayer_preflight_progress,
                completedCount,
                completedCount,
                catalog.service.preflightSteps.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        catalog.service.preflightSteps.forEach { step ->
            val completed = step.id in state.completedPreflightStepIds
            val completionDescription = stringResource(
                if (completed) {
                    R.string.feature_prayer_completed_state
                } else {
                    R.string.feature_prayer_not_completed_state
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = completed,
                        role = Role.Checkbox,
                        onValueChange = { onPreflightCompleted(step.id, it) },
                    )
                    .semantics { stateDescription = completionDescription }
                    .padding(vertical = 6.dp)
                    .testTag("prayer-preflight-${step.id}"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = completed,
                    onCheckedChange = null,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CatalogText(
                        text = step.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    CatalogText(
                        text = step.detail,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleSection(
    selectedRole: ParticipantRole,
    onRoleSelected: (ParticipantRole) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .selectableGroup()
            .testTag("prayer-role-selector"),
    ) {
        SectionHeading(R.string.feature_prayer_roles)
        Text(
            text = stringResource(R.string.feature_prayer_role_help),
            style = MaterialTheme.typography.bodyMedium,
        )
        ParticipantRole.entries.forEach { participantRole ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = participantRole == selectedRole,
                        role = Role.RadioButton,
                        onClick = { onRoleSelected(participantRole) },
                    )
                    .padding(vertical = 4.dp)
                    .testTag("prayer-role-${participantRole.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(
                    selected = participantRole == selectedRole,
                    onClick = null,
                )
                CatalogText(
                    text = participantRole.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun RoleViewSection(
    catalog: DemonstratorCatalog,
    assembly: ServiceAssembly,
    state: ExperienceState,
    onOpenBuild: () -> Unit,
) {
    val role = state.selectedRole
    val assignedParticipant = state.roleAssignments[role].orEmpty().trim()
    val handoffs = assembledRoleHandoffs(assembly.segments, role)
    val openReadingSlots = openReadingSlotCount(catalog.service.readingSlots, state)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-role-dashboard-${role.id}"),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_prayer_role_view, role.label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (role != ParticipantRole.CONGREGANT) {
                LabelValue(
                    label = stringResource(R.string.feature_prayer_assigned_person),
                    value = assignedParticipant.ifBlank {
                        stringResource(R.string.feature_prayer_assignment_open)
                    },
                    testTag = "prayer-role-dashboard-assignee",
                )
            }
            Text(
                text = stringResource(R.string.feature_prayer_role_responsibilities),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            roleResponsibilities(role).forEach { responsibility ->
                CatalogText(
                    text = "• $responsibility",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                text = stringResource(R.string.feature_prayer_role_handoffs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            handoffs.forEach { assembledSegment ->
                val segment = assembledSegment.segment
                val completion = if (segment.id in state.completedServiceSegmentIds) {
                    stringResource(R.string.feature_prayer_status_complete)
                } else {
                    stringResource(R.string.feature_prayer_status_incomplete)
                }
                CatalogText(
                    text = "• ${segment.title} · $completion",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (role == ParticipantRole.READER || role == ParticipantRole.GABBAI) {
                Text(
                    text = stringResource(
                        R.string.feature_prayer_open_reading_slots,
                        openReadingSlots,
                        catalog.service.readingSlots.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (openReadingSlots > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.testTag("prayer-role-dashboard-reading-status"),
                )
            }

            if (
                (role != ParticipantRole.CONGREGANT && assignedParticipant.isBlank()) ||
                ((role == ParticipantRole.READER || role == ParticipantRole.GABBAI) && openReadingSlots > 0)
            ) {
                OutlinedButton(
                    onClick = onOpenBuild,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("prayer-role-dashboard-open-build"),
                ) {
                    Text(stringResource(R.string.feature_prayer_fix_assignments))
                }
            }
        }
    }
}

private fun roleResponsibilities(role: ParticipantRole): List<String> = when (role) {
    ParticipantRole.CONGREGANT -> listOf(
        "Follow the visible response markers and current position.",
        "Use the equal seated or silent alternative whenever it works better.",
        "Signal when a cue, route, or printed instruction is unclear.",
    )

    ParticipantRole.LEADER -> listOf(
        "Name each transition and leave a full response window.",
        "Advance the conductor only after the handoff is clear.",
        "Pause the run when an assignment or access need is unresolved.",
    )

    ParticipantRole.READER -> listOf(
        "Confirm every reading slot assigned to you before the run.",
        "Watch for the gabbai handoff and confirm the start signal.",
        "Return control visibly when the reading handoff closes.",
    )

    ParticipantRole.GABBAI -> listOf(
        "Keep open roles and reading slots visible.",
        "Name the next reader and verify readiness before each handoff.",
        "Record assignment, timing, and access corrections for the next packet.",
    )

    ParticipantRole.HOST -> listOf(
        "Prepare the room, route, seating, light, and readable packet.",
        "Check audibility and sight lines from more than one position.",
        "Keep the chosen offline or print fallback ready.",
    )
}

private fun roleHandoffSegmentIds(role: ParticipantRole): Set<String> = when (role) {
    ParticipantRole.CONGREGANT -> setOf("segment.opening", "segment.response", "segment.reading")
    ParticipantRole.LEADER -> setOf(
        "segment.gather",
        "segment.opening",
        "segment.response",
        "segment.teaching",
        "segment.close",
    )
    ParticipantRole.READER -> setOf("segment.gather", "segment.reading")
    ParticipantRole.GABBAI -> setOf("segment.gather", "segment.reading", "segment.close")
    ParticipantRole.HOST -> setOf("segment.gather", "segment.reading", "segment.close")
}

@Composable
private fun TimelineSection(
    segments: List<ServiceSegment>,
    completedSegmentIds: Set<String>,
    currentSegmentIndex: Int,
    enabled: Boolean,
    onSegmentSelected: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("prayer-timeline"),
    ) {
        SectionHeading(R.string.feature_prayer_timeline)
        if (!enabled) {
            Text(
                text = stringResource(R.string.feature_prayer_timeline_locked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("prayer-timeline-locked"),
            )
        }
        segments.forEachIndexed { index, segment ->
            val completed = segment.id in completedSegmentIds
            val isCurrent = index == currentSegmentIndex
            val status = stringResource(
                if (completed) {
                    R.string.feature_prayer_status_complete
                } else {
                    R.string.feature_prayer_status_incomplete
                },
            )
            val description = stringResource(
                R.string.feature_prayer_timeline_item,
                index + 1,
                segments.size,
                segment.title.ltrIsolate(),
                status,
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = isCurrent,
                        enabled = enabled,
                        role = Role.Tab,
                        onClick = { onSegmentSelected(index) },
                    )
                    .semantics {
                        selected = isCurrent
                        stateDescription = description
                    }
                    .testTag("prayer-timeline-${segment.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CatalogText(
                        text = "${index + 1}. ${segment.phase}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    CatalogText(
                        text = segment.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConductorSection(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
    assembledSegment: AssembledServiceSegment,
    segmentIndex: Int,
    segmentCount: Int,
    onOpenSource: (String) -> Unit,
) {
    val segment = assembledSegment.segment
    val roleCue = segment.roleCues[state.selectedRole]
        .orEmpty()
        .ifBlank { segment.summary }
    val assignedParticipant = state.roleAssignments[state.selectedRole]
        .orEmpty()
        .trim()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .testTag("prayer-conductor")
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        SectionHeading(R.string.feature_prayer_conductor)
        Text(
            text = stringResource(
                R.string.feature_prayer_current_step,
                segmentIndex + 1,
                segmentCount,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        CatalogText(
            text = segment.phase,
            style = MaterialTheme.typography.labelLarge,
        )
        CatalogText(
            text = segment.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("prayer-current-segment-title"),
        )
        CatalogText(
            text = segment.summary,
            style = MaterialTheme.typography.bodyLarge,
        )

        SegmentContentSection(
            content = segment.content,
            selectedRole = state.selectedRole,
            visualVoiceCues = state.accessibilityProfile.useVisualVoiceCues,
        )

        LabelValue(
            label = stringResource(R.string.feature_prayer_selected_role),
            value = state.selectedRole.label,
            testTag = "prayer-focused-role",
        )
        if (assignedParticipant.isNotEmpty()) {
            LabelValue(
                label = stringResource(R.string.feature_prayer_assigned_person),
                value = assignedParticipant,
                testTag = "prayer-assigned-person",
            )
        }

        if (segment.id == "segment.reading" && catalog.service.readingSlots.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer-reading-assignments"),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feature_prayer_reading_assignments),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    catalog.service.readingSlots
                        .sortedBy { it.sequence }
                        .forEach { slot ->
                            val plan = state.readingPlanFor(slot)
                            val passageSourceId = slot.sourceId
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("prayer-reading-${slot.id}"),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    CatalogText(
                                        text = "${slot.sequence}. ${slot.label}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    LabelValue(
                                        label = stringResource(R.string.feature_prayer_primary_reader),
                                        value = plan.assignee.ifBlank {
                                            stringResource(R.string.feature_prayer_assignment_open)
                                        },
                                        testTag = "prayer-reading-primary-${slot.id}",
                                    )
                                    LabelValue(
                                        label = stringResource(R.string.feature_prayer_backup_reader),
                                        value = plan.backupAssignee.ifBlank {
                                            stringResource(R.string.feature_prayer_not_assigned)
                                        },
                                        testTag = "prayer-reading-backup-${slot.id}",
                                    )
                                    LabelValue(
                                        label = stringResource(R.string.feature_prayer_preparation_status),
                                        value = plan.preparationStatus.label,
                                        testTag = "prayer-reading-preparation-${slot.id}",
                                    )
                                    if (
                                        slot.passageAvailability == ReadingPassageAvailability.UNAVAILABLE &&
                                        !plan.manualOverride
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.feature_prayer_passage_unavailable,
                                                slot.passageUnavailableReason,
                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.testTag("prayer-reading-passage-unavailable-${slot.id}"),
                                        )
                                    } else {
                                        LabelValue(
                                            label = stringResource(R.string.feature_prayer_portion),
                                            value = plan.portionTitle.ifBlank {
                                                stringResource(R.string.feature_prayer_not_available)
                                            },
                                            testTag = "prayer-reading-portion-${slot.id}",
                                        )
                                        LabelValue(
                                            label = stringResource(R.string.feature_prayer_locator),
                                            value = plan.locator.ifBlank {
                                                stringResource(R.string.feature_prayer_not_available)
                                            },
                                            testTag = "prayer-reading-locator-${slot.id}",
                                        )
                                        LabelValue(
                                            label = stringResource(R.string.feature_prayer_range),
                                            value = plan.passageRange.ifBlank {
                                                stringResource(R.string.feature_prayer_not_available)
                                            },
                                            testTag = "prayer-reading-range-${slot.id}",
                                        )
                                        if (plan.manualOverride) {
                                            Text(
                                                text = stringResource(
                                                    R.string.feature_prayer_manual_passage,
                                                    plan.overrideReason.ifBlank {
                                                        stringResource(R.string.feature_prayer_not_recorded)
                                                    },
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        } else if (passageSourceId != null) {
                                            OutlinedButton(
                                                onClick = { onOpenSource(passageSourceId) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp),
                                            ) {
                                                Text(
                                                    stringResource(
                                                        R.string.feature_prayer_open_source,
                                                        passageSourceId.ltrIsolate(),
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }

        CueCard(
            heading = stringResource(R.string.feature_prayer_role_cue),
            body = roleCue,
            testTag = "prayer-role-cue",
        )
        CueCard(
            heading = stringResource(R.string.feature_prayer_movement_cue),
            body = "${assembledSegment.effectiveMovementCue}\n\n${assembledSegment.movementCueReason}",
            testTag = "prayer-movement-cue",
        )
        CueCard(
            heading = stringResource(R.string.feature_prayer_accessible_alternative),
            body = segment.accessibleAlternative,
            testTag = "prayer-accessible-alternative",
        )
        CueCard(
            heading = stringResource(
                if (state.accessibilityProfile.useVisualVoiceCues) {
                    R.string.feature_prayer_visual_voice_cue
                } else {
                    R.string.feature_prayer_voice_cue
                },
            ),
            body = segment.voiceCue,
            testTag = "prayer-voice-cue",
            emphasized = state.accessibilityProfile.useVisualVoiceCues,
            stateDescriptionText = if (state.accessibilityProfile.useVisualVoiceCues) {
                stringResource(R.string.feature_prayer_visual_cue_active_state)
            } else {
                null
            },
        )

        HorizontalDivider()
        WhyIncludedSection(
            catalog = catalog,
            assembledSegment = assembledSegment,
            onOpenSource = onOpenSource,
        )
    }
}

@Composable
private fun SegmentContentSection(
    content: ServiceSegmentContent,
    selectedRole: ParticipantRole,
    visualVoiceCues: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-liturgical-content"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_prayer_liturgical_content),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            ServiceTextValue(
                label = stringResource(R.string.feature_prayer_source_text),
                field = content.sourceText,
                testTag = "prayer-source-text",
            )
            ServiceTextValue(
                label = stringResource(R.string.feature_prayer_translation),
                field = content.translation,
                testTag = "prayer-translation",
            )
            ServiceTextValue(
                label = stringResource(R.string.feature_prayer_transliteration),
                field = content.transliteration,
                testTag = "prayer-transliteration",
            )

            val selectedRoleText = content.roleTexts[selectedRole]
            if (selectedRoleText == null) {
                Text(
                    text = stringResource(
                        R.string.feature_prayer_no_role_text,
                        selectedRole.label,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("prayer-role-text-status"),
                )
            } else {
                ServiceTextValue(
                    label = stringResource(
                        R.string.feature_prayer_role_text,
                        selectedRole.label,
                    ),
                    field = selectedRoleText,
                    testTag = "prayer-role-text-status",
                )
            }

            val relevantResponses = content.responses.filter { response ->
                response.speakerRole == selectedRole || response.responderRole == selectedRole
            }
            if (relevantResponses.isEmpty()) {
                Text(
                    text = stringResource(R.string.feature_prayer_no_responses),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("prayer-response-status"),
                )
            } else {
                val visualCueState = stringResource(R.string.feature_prayer_visual_cue_active_state)
                Surface(
                    color = if (visualVoiceCues) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (visualVoiceCues) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (visualVoiceCues) {
                                Modifier
                                    .semantics {
                                        stateDescription = visualCueState
                                        liveRegion = LiveRegionMode.Polite
                                    }
                                    .testTag("prayer-visual-response-cue")
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(if (visualVoiceCues) 12.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (visualVoiceCues) {
                                    R.string.feature_prayer_visual_role_responses
                                } else {
                                    R.string.feature_prayer_role_responses
                                },
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                        relevantResponses.forEachIndexed { index, response ->
                            Text(
                                text = stringResource(
                                    R.string.feature_prayer_response_roles,
                                    response.speakerRole.label,
                                    response.responderRole.label,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            ServiceTextValue(
                                label = stringResource(R.string.feature_prayer_prompt),
                                field = response.prompt,
                                testTag = "prayer-response-$index-prompt",
                            )
                            ServiceTextValue(
                                label = stringResource(R.string.feature_prayer_response),
                                field = response.response,
                                testTag = "prayer-response-$index-response",
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Text(
                text = stringResource(R.string.feature_prayer_text_provenance),
                fontWeight = FontWeight.Bold,
            )
            val provenance = content.provenance
            LabelValue(
                label = stringResource(R.string.feature_prayer_edition_id),
                value = provenance.editionId,
                testTag = "prayer-content-edition-id",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_edition),
                value = provenance.editionTitle,
                testTag = "prayer-content-edition",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_source_unit),
                value = provenance.sourceUnitId,
                testTag = "prayer-content-source-unit",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_locator),
                value = provenance.locator,
                testTag = "prayer-content-locator",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_provenance),
                value = provenance.provenance,
                testTag = "prayer-content-provenance",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_license),
                value = provenance.license,
                testTag = "prayer-content-license",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_conclusion_status),
                value = provenance.conclusionStatus.label,
                testTag = "prayer-content-conclusion-status",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_review_state),
                value = provenance.reviewState.label,
                testTag = "prayer-content-review-state",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_editorial_treatment),
                value = provenance.editorialTreatment,
                testTag = "prayer-content-editorial-treatment",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_punctuation_source),
                value = provenance.punctuationSource,
                testTag = "prayer-content-punctuation",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_vocalization_source),
                value = provenance.vocalizationSource,
                testTag = "prayer-content-vocalization",
            )
            LabelValue(
                label = stringResource(R.string.feature_prayer_variants),
                value = provenance.variantNotes,
                testTag = "prayer-content-variants",
            )
        }
    }
}

@Composable
private fun ServiceTextValue(
    label: String,
    field: ServiceTextField,
    testTag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        when (field.availability) {
            ServiceTextAvailability.AVAILABLE -> {
                CatalogText(
                    text = field.text,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        R.string.feature_prayer_language_tag,
                        field.languageTag.ifBlank { stringResource(R.string.feature_prayer_not_recorded) },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            ServiceTextAvailability.UNAVAILABLE -> Text(
                text = stringResource(R.string.feature_prayer_unavailable_detail, field.detail),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            ServiceTextAvailability.NOT_APPLICABLE -> Text(
                text = stringResource(R.string.feature_prayer_not_applicable_detail, field.detail),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ConductorControls(
    segment: ServiceSegment,
    segmentIndex: Int,
    segmentCount: Int,
    completed: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSegmentCompleted: (String, Boolean) -> Unit,
) {
    val completionDescription = stringResource(
        if (completed) {
            R.string.feature_prayer_completed_state
        } else {
            R.string.feature_prayer_not_completed_state
        },
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer-conductor-controls"),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = segmentIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("prayer-previous"),
                ) {
                    Text(stringResource(R.string.feature_prayer_previous))
                }
                OutlinedButton(
                    onClick = onNext,
                    enabled = segmentIndex < segmentCount - 1,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("prayer-next"),
                ) {
                    Text(stringResource(R.string.feature_prayer_next))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            if (completed) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            stateDescription = completionDescription
                            liveRegion = LiveRegionMode.Polite
                        }
                        .testTag("prayer-completion-confirmation"),
                ) {
                    Text(
                        text = stringResource(R.string.feature_prayer_completion_confirmed),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedButton(
                    onClick = { onSegmentCompleted(segment.id, false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics { stateDescription = completionDescription }
                        .testTag("prayer-undo-complete-current"),
                ) {
                    Text(stringResource(R.string.feature_prayer_undo_completion))
                }
            } else {
                Button(
                    onClick = { onSegmentCompleted(segment.id, true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics { stateDescription = completionDescription }
                        .testTag("prayer-complete-current"),
                ) {
                    Text(stringResource(R.string.feature_prayer_mark_complete))
                }
            }
        }
    }
}

@Composable
private fun CueCard(
    heading: String,
    body: String,
    testTag: String,
    emphasized: Boolean = false,
    stateDescriptionText: String? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (stateDescriptionText == null) {
                    Modifier
                } else {
                    Modifier.semantics {
                        stateDescription = stateDescriptionText
                        liveRegion = LiveRegionMode.Polite
                    }
                },
            )
            .testTag(testTag),
        color = if (emphasized) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            CatalogText(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun WhyIncludedSection(
    catalog: DemonstratorCatalog,
    assembledSegment: AssembledServiceSegment,
    onOpenSource: (String) -> Unit,
) {
    val segment = assembledSegment.segment
    val sourceById = remember(catalog.sources) {
        catalog.sources.associateBy(SourceUnit::id)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("prayer-why-included"),
    ) {
        SectionHeading(R.string.feature_prayer_why_included)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prayer-evaluated-inclusion"),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.feature_prayer_evaluated_inclusion),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                TraceValue(
                    label = stringResource(R.string.feature_prayer_inclusion_reason),
                    value = assembledSegment.inclusionReason,
                )
                Text(
                    text = stringResource(R.string.feature_prayer_evaluated_facts),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                assembledSegment.inclusionFacts.forEach { fact ->
                    CatalogText(
                        text = "• $fact",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.feature_prayer_evaluated_sources),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (assembledSegment.sourceReferences.isEmpty()) {
                    Text(
                        text = stringResource(R.string.feature_prayer_no_sources),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    assembledSegment.sourceReferences.distinctBy { reference -> reference.id }
                        .forEach { reference ->
                            val sourceId = reference.id
                            val source = sourceById[sourceId]
                            OutlinedButton(
                                onClick = { onOpenSource(sourceId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("prayer-source-$sourceId"),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.feature_prayer_open_source,
                                            sourceId.ltrIsolate(),
                                        ),
                                    )
                                    reference.locator?.takeIf(String::isNotBlank)?.let { locator ->
                                        CatalogText(
                                            text = stringResource(
                                                R.string.feature_prayer_evaluated_locator,
                                                locator,
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                    if (source != null) {
                                        CatalogText(
                                            text = stringResource(
                                                R.string.feature_prayer_source_metadata,
                                                source.title,
                                                source.locator,
                                                source.edition,
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                }
            }
        }

        HorizontalDivider()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("prayer-static-explanation"),
        ) {
            Text(
                text = stringResource(R.string.feature_prayer_installed_explanation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.feature_prayer_installed_explanation_detail),
                style = MaterialTheme.typography.bodySmall,
            )
            TraceValue(
                label = stringResource(R.string.feature_prayer_trace_result),
                value = segment.explanation.result,
            )
            Text(
                text = stringResource(R.string.feature_prayer_trace_facts),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            segment.explanation.facts.forEach { fact ->
                CatalogText(
                    text = "• $fact",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TraceValue(
                label = stringResource(R.string.feature_prayer_trace_rule),
                value = segment.explanation.rule,
            )
            TraceValue(
                label = stringResource(R.string.feature_prayer_definition_sources),
                value = segment.explanation.sourceIds.distinct().joinToString().ifBlank {
                    stringResource(R.string.feature_prayer_no_sources)
                },
            )
        }
    }
}

@Composable
private fun TraceValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        CatalogText(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PacketSection(
    onSharePacket: () -> Unit,
    onPrintPacket: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("prayer-packet"),
    ) {
        SectionHeading(R.string.feature_prayer_packet)
        Text(
            text = stringResource(R.string.feature_prayer_packet_explanation),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onSharePacket,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("prayer-share-packet"),
        ) {
            Text(stringResource(R.string.feature_prayer_share_packet))
        }
        OutlinedButton(
            onClick = onPrintPacket,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("prayer-print-packet"),
        ) {
            Text(stringResource(R.string.feature_prayer_print_packet))
        }
    }
}

@Composable
private fun SectionHeading(textResource: Int) {
    Text(
        text = stringResource(textResource),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun LabelValue(
    label: String,
    value: String,
    testTag: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        CatalogText(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CatalogText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style.merge(TextStyle(textDirection = TextDirection.ContentOrLtr)),
        modifier = modifier,
    )
}

internal fun initialSegmentIndex(
    segments: List<ServiceSegment>,
    completedSegmentIds: Set<String>,
): Int = segments.indexOfFirst { it.id !in completedSegmentIds }
    .takeIf { it >= 0 }
    ?: 0

internal fun assembledServiceProgress(
    assembledSegments: List<AssembledServiceSegment>,
    completedSegmentIds: Set<String>,
): Pair<Int, Int> {
    val assembledSegmentIds = assembledSegments.map { assembled -> assembled.segment.id }
    return assembledSegmentIds.count(completedSegmentIds::contains) to assembledSegmentIds.size
}

internal fun visibleScheduledServices(
    upcomingServices: List<DatedServiceInstance>,
    selectedServiceId: String?,
    chronologicalLimit: Int = 12,
): List<DatedServiceInstance> {
    require(chronologicalLimit > 0) { "The visible service limit must be positive." }
    val selectedService = upcomingServices.firstOrNull { service ->
        service.id == selectedServiceId
    }
    return (upcomingServices.take(chronologicalLimit) + listOfNotNull(selectedService))
        .distinctBy(DatedServiceInstance::id)
}

internal fun canEnterFocusedConductor(readiness: ServiceReadiness): Boolean =
    readiness.canBeginRehearsal

internal fun assembledRoleHandoffs(
    assembledSegments: List<AssembledServiceSegment>,
    role: ParticipantRole,
): List<AssembledServiceSegment> {
    val handoffIds = roleHandoffSegmentIds(role)
    return assembledSegments.filter { assembled -> assembled.segment.id in handoffIds }
}

internal fun openReadingSlotCount(
    readingSlots: List<ReadingSlot>,
    state: ExperienceState,
): Int = readingSlots.count { slot -> state.readingPlanFor(slot).assignee.isBlank() }

internal enum class RehearsalEntryAction {
    START,
    RESUME,
    REVIEW,
}

internal fun rehearsalEntryAction(
    segments: List<ServiceSegment>,
    completedSegmentIds: Set<String>,
): RehearsalEntryAction {
    val completedCount = segments.count { it.id in completedSegmentIds }
    return when {
        completedCount == 0 -> RehearsalEntryAction.START
        completedCount == segments.size -> RehearsalEntryAction.REVIEW
        else -> RehearsalEntryAction.RESUME
    }
}

internal fun boundedSegmentIndex(
    currentIndex: Int,
    change: Int,
    segmentCount: Int,
): Int {
    if (segmentCount <= 0) return 0
    return (currentIndex + change).coerceIn(0, segmentCount - 1)
}

private fun String.ltrIsolate(): String = "\u2066$this\u2069"
