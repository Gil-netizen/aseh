package io.github.gilnetizen.aseh.feature.now

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.gilnetizen.aseh.domain.zmanim.ElevationHandling
import io.github.gilnetizen.aseh.domain.servicecatalog.DatedServiceInstance
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceAgenda
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceAvailabilityStatus
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionResult
import io.github.gilnetizen.aseh.domain.zmanim.HebrewDateCalculation
import io.github.gilnetizen.aseh.domain.zmanim.KosherJavaZmanimEngine
import io.github.gilnetizen.aseh.domain.zmanim.ZmanCalculation
import io.github.gilnetizen.aseh.domain.zmanim.ZmanimEventType
import io.github.gilnetizen.aseh.domain.zmanim.ZmanimRequest
import io.github.gilnetizen.aseh.domain.zmanim.ZmanimSnapshot
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val civilDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)

private val weekdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

private val localTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)

private val solarTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

private val nextEventFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.ENGLISH)

private val serviceTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.ENGLISH)

private val availableTimeZoneIds: List<String> by lazy {
    ZoneId.getAvailableZoneIds().sorted()
}

internal data class NowUiState(
    val civilDate: String,
    val weekday: String,
    val localTime: String,
    val timeZone: String,
)

data class NowJourneySummary(
    val dateLabel: String,
    val locationLabel: String,
    val workspaceLabel: String,
    val practiceCompleted: Int,
    val practiceTotal: Int,
    val serviceCompleted: Int,
    val serviceTotal: Int,
    val nextActionTitle: String = "Continue preparation",
    val nextActionDetail: String = "Complete the next preparation item for this rehearsal.",
    val nextAction: NowJourneyAction = NowJourneyAction.PRACTICE,
)

data class NowWorkspaceDueItem(
    val label: String,
    val dueLabel: String,
    val timingLabel: String,
)

data class NowWorkspaceSummary(
    val contextLabel: String,
    val dueItems: List<NowWorkspaceDueItem>,
)

enum class NowJourneyAction {
    LOCATION,
    PRACTICE,
    PRAYER,
    BUILD,
}

/**
 * Creates the display state without consulting Android services or the network.
 *
 * Keeping the instant and zone explicit makes this formatter deterministic and
 * lets callers decide which clock represents "now".
 */
internal fun formatNowUiState(
    instant: Instant,
    zoneId: ZoneId,
): NowUiState {
    val zonedDateTime = instant.atZone(zoneId)
    return NowUiState(
        civilDate = civilDateFormatter.format(zonedDateTime),
        weekday = weekdayFormatter.format(zonedDateTime),
        localTime = localTimeFormatter.format(zonedDateTime),
        timeZone = zoneId.id,
    )
}

internal fun currentNowUiState(
    clock: Clock,
    timeZone: () -> ZoneId = { clock.zone },
): NowUiState =
    formatNowUiState(
        instant = clock.instant(),
        zoneId = timeZone(),
    )

internal class NowScreenState(
    private val clock: Clock,
    private val timeZone: () -> ZoneId = { clock.zone },
) {
    var snapshotInstant by mutableStateOf(clock.instant())
        private set

    var snapshot by mutableStateOf(formatNowUiState(snapshotInstant, timeZone()))
        private set

    fun refresh() {
        snapshotInstant = clock.instant()
        snapshot = formatNowUiState(snapshotInstant, timeZone())
    }
}

/**
 * Chooses the first clock boundary that can change content on the Now screen.
 *
 * A minute boundary keeps the visible clock current and also covers civil
 * midnight. A solar event may occur between minute boundaries, so it wins when
 * it is earlier; this lets the Hebrew date and next event turn over at sunset.
 */
internal fun nextNowRefreshInstant(
    currentInstant: Instant,
    nextSolarBoundary: Instant?,
): Instant {
    val nextMinute = currentInstant.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES)
    val solarRefresh = when {
        nextSolarBoundary == null -> null
        nextSolarBoundary == currentInstant -> nextSolarBoundary.plusMillis(1)
        nextSolarBoundary.isAfter(currentInstant) -> nextSolarBoundary
        else -> null
    }
    return solarRefresh?.takeIf { it.isBefore(nextMinute) } ?: nextMinute
}

internal fun calculateNowZmanimSnapshot(
    placeContext: NowPlaceContext,
    instant: Instant,
    engine: KosherJavaZmanimEngine,
): ZmanimSnapshot? = runCatching {
    val zoneId = ZoneId.of(placeContext.timeZoneId)
    engine.calculate(
        ZmanimRequest(
            date = instant.atZone(zoneId).toLocalDate(),
            referenceInstant = instant,
            latitude = placeContext.latitudeDegrees,
            longitude = placeContext.longitudeDegrees,
            elevationMeters = placeContext.elevationMeters,
            zoneId = zoneId,
        ),
    )
}.getOrNull()

@Composable
private fun KeepNowStateFresh(
    state: NowScreenState,
    clock: Clock,
    placeContext: NowPlaceContext?,
    nextSolarBoundary: Instant?,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, state, placeContext, nextSolarBoundary) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Resample immediately after returning from the background; time,
            // date, zone, and solar boundaries may all have changed.
            state.refresh()
            while (true) {
                val currentInstant = clock.instant()
                val nextRefresh = nextNowRefreshInstant(currentInstant, nextSolarBoundary)
                val waitMillis = Duration.between(currentInstant, nextRefresh)
                    .toMillis()
                    .coerceAtLeast(1L)
                delay(waitMillis)
                state.refresh()
            }
        }
    }
}

@Composable
fun NowScreen(
    clock: Clock,
    timeZone: () -> ZoneId = { clock.zone },
    placeContextReady: Boolean = true,
    placeContext: NowPlaceContext? = null,
    openEditorRequest: Int = 0,
    onOpenEditorRequestConsumed: () -> Unit = {},
    deviceLocationState: DeviceLocationUiState = DeviceLocationUiState.Idle,
    onRequestDeviceLocation: () -> Unit = {},
    onCancelDeviceLocation: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onSavePlaceContext: suspend (NowPlaceContext) -> Unit = {},
    onClearPlaceContext: suspend () -> Unit = {},
    journeySummary: NowJourneySummary? = null,
    workspaceSummary: NowWorkspaceSummary? = null,
    serviceSelection: ServiceSelectionResult? = null,
    showFunctionalReviewNotice: Boolean = false,
    onOpenService: (String) -> Unit = {},
    onOpenPractice: () -> Unit = {},
    onOpenPrayer: () -> Unit = {},
    onOpenBuild: () -> Unit = {},
    onOpenWorkspace: () -> Unit = onOpenBuild,
    modifier: Modifier = Modifier,
) {
    val selectedTimeZoneId = placeContext?.timeZoneId
    val selectedTimeZone = remember(timeZone, selectedTimeZoneId) {
        {
            selectedTimeZoneId?.let(ZoneId::of) ?: timeZone()
        }
    }
    val state = remember(clock, selectedTimeZone) {
        NowScreenState(clock, selectedTimeZone)
    }
    val zmanimEngine = remember { KosherJavaZmanimEngine() }
    val zmanimSnapshot = remember(
        placeContext,
        state.snapshotInstant,
        state.snapshot.timeZone,
    ) {
        placeContext?.let { context ->
            calculateNowZmanimSnapshot(
                placeContext = context,
                instant = state.snapshotInstant,
                engine = zmanimEngine,
            )
        }
    }
    KeepNowStateFresh(
        state = state,
        clock = clock,
        placeContext = placeContext,
        nextSolarBoundary = zmanimSnapshot?.nextEvent?.instant,
    )
    var editingPlace by remember { mutableStateOf(false) }

    LaunchedEffect(openEditorRequest, placeContextReady) {
        if (openEditorRequest > 0 && placeContextReady) {
            editingPlace = true
            onOpenEditorRequestConsumed()
        }
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
                    bottom = 32.dp,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_now_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .testTag("now-heading")
                .semantics { heading() },
        )
        Text(
            text = stringResource(
                if (!placeContextReady) {
                    R.string.feature_now_summary_loading
                } else if (placeContext == null) {
                    R.string.feature_now_summary
                } else {
                    R.string.feature_now_summary_place
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("now-summary"),
        )

        if (showFunctionalReviewNotice && !editingPlace) {
            FunctionalReviewCard(
                onOpenPractice = onOpenPractice,
                onOpenPrayer = onOpenPrayer,
                onOpenBuild = onOpenBuild,
            )
        }

        if (editingPlace) {
            PlaceContextEditor(
                existingContext = placeContext,
                defaultTimeZoneId = state.snapshot.timeZone,
                deviceLocationState = deviceLocationState,
                onRequestDeviceLocation = onRequestDeviceLocation,
                onCancelDeviceLocation = onCancelDeviceLocation,
                onOpenAppSettings = onOpenAppSettings,
                onOpenLocationSettings = onOpenLocationSettings,
                onSave = { savedContext ->
                    onSavePlaceContext(savedContext)
                    editingPlace = false
                },
                onCancel = { editingPlace = false },
                onClear = if (placeContext == null) {
                    null
                } else {
                    {
                        onClearPlaceContext()
                        editingPlace = false
                    }
                },
            )
        } else if (placeContext == null) {
            PlaceContextCard(
                placeContextReady = placeContextReady,
                placeContext = null,
                onChange = { editingPlace = true },
            )
        }

        if (!editingPlace && journeySummary != null) {
            JourneyCard(
                summary = journeySummary,
                onSetLocation = { editingPlace = true },
                onOpenPractice = onOpenPractice,
                onOpenPrayer = onOpenPrayer,
                onOpenBuild = onOpenBuild,
            )
        }

        if (!editingPlace && workspaceSummary != null) {
            WorkspaceDueCard(
                summary = workspaceSummary,
                onOpenWorkspace = onOpenWorkspace,
            )
        }

        if (!editingPlace && serviceSelection != null) {
            ServiceAgendaCard(
                selection = serviceSelection,
                onOpenService = onOpenService,
            )
        }

        NowValue(
            label = stringResource(R.string.feature_now_civil_date_label),
            value = state.snapshot.civilDate,
            testTag = "now-date",
        )
        NowValue(
            label = stringResource(R.string.feature_now_weekday_label),
            value = state.snapshot.weekday,
            testTag = "now-weekday",
        )
        NowValue(
            label = stringResource(R.string.feature_now_local_time_label),
            value = state.snapshot.localTime,
            testTag = "now-time",
        )
        NowValue(
            label = stringResource(R.string.feature_now_time_zone_label),
            value = state.snapshot.timeZone,
            testTag = "now-time-zone",
        )

        if (placeContext != null) {
            ZmanimCard(
                snapshot = zmanimSnapshot,
                zoneId = ZoneId.of(placeContext.timeZoneId),
            )
        }

        if (!editingPlace && placeContext != null) {
            PlaceContextCard(
                placeContextReady = placeContextReady,
                placeContext = placeContext,
                onChange = { editingPlace = true },
                onRefreshDeviceLocation = if (placeContext.source == NowPlaceSource.DEVICE) {
                    {
                        editingPlace = true
                        onRequestDeviceLocation()
                    }
                } else {
                    null
                },
            )
        }

        Button(
            onClick = state::refresh,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("now-refresh"),
        ) {
            Text(text = stringResource(R.string.feature_now_refresh))
        }
    }
}

@Composable
private fun WorkspaceDueCard(
    summary: NowWorkspaceSummary,
    onOpenWorkspace: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-workspace-summary"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_now_workspace_title, summary.contextLabel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (summary.dueItems.isEmpty()) {
                Text(stringResource(R.string.feature_now_workspace_none_due))
            } else {
                Text(
                    stringResource(
                        R.string.feature_now_workspace_due_count,
                        summary.dueItems.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                summary.dueItems.take(3).forEach { item ->
                    Text(
                        text = stringResource(
                            R.string.feature_now_workspace_due_item,
                            item.timingLabel,
                            item.dueLabel,
                            item.label,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Button(
                onClick = onOpenWorkspace,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-open-workspace"),
            ) {
                Text(stringResource(R.string.feature_now_workspace_open))
            }
        }
    }
}

@Composable
private fun FunctionalReviewCard(
    onOpenPractice: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenBuild: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-functional-review"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_now_functional_review_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.feature_now_functional_review_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onOpenPrayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-functional-open-prayer"),
            ) {
                Text(stringResource(R.string.feature_now_functional_open_prayer))
            }
            OutlinedButton(
                onClick = onOpenPractice,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-functional-open-practice"),
            ) {
                Text(stringResource(R.string.feature_now_functional_open_practice))
            }
            OutlinedButton(
                onClick = onOpenBuild,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-functional-open-build"),
            ) {
                Text(stringResource(R.string.feature_now_functional_open_build))
            }
        }
    }
}

@Composable
private fun ServiceAgendaCard(
    selection: ServiceSelectionResult,
    onOpenService: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-service-agenda"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_now_services_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            when (selection) {
                is ServiceSelectionResult.Blocked -> {
                    Text(
                        text = stringResource(R.string.feature_now_services_context_needed),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    selection.reasons.forEach { reason ->
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
                }
                is ServiceSelectionResult.Available -> ServiceAgendaContents(
                    agenda = selection.agenda,
                    onOpenService = onOpenService,
                )
            }
        }
    }
}

@Composable
private fun ServiceAgendaContents(
    agenda: ServiceAgenda,
    onOpenService: (String) -> Unit,
) {
    Text(
        text = agenda.catalogNotice.fallbackEnglish,
        style = MaterialTheme.typography.bodySmall,
    )
    val visible = agenda.upcomingServices.take(5)
    if (visible.isEmpty()) {
        Text(stringResource(R.string.feature_now_services_none))
        agenda.nextServiceUnavailableReason?.let { reason ->
            Text(reason.detail.fallbackEnglish)
        }
        return
    }
    visible.forEach { service ->
        ScheduledServiceRow(
            service = service,
            onOpen = { onOpenService(service.id) },
        )
    }
    if (agenda.upcomingServices.size > visible.size) {
        Text(
            text = stringResource(
                R.string.feature_now_services_more,
                agenda.upcomingServices.size - visible.size,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ScheduledServiceRow(
    service: DatedServiceInstance,
    onOpen: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-service-${service.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = service.definition.title.fallbackEnglish,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(serviceTimeFormatter.format(service.startsAt))
            Text(
                text = service.calendarDay.label.fallbackEnglish,
                style = MaterialTheme.typography.labelMedium,
            )
            service.calendarAdditions.forEach { addition ->
                Text(
                    text = addition.title.fallbackEnglish,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val statusText = when (service.availability.status) {
                ServiceAvailabilityStatus.AVAILABLE ->
                    stringResource(R.string.feature_now_service_available)
                ServiceAvailabilityStatus.PLANNING_ONLY ->
                    stringResource(R.string.feature_now_service_planning_only)
                ServiceAvailabilityStatus.BLOCKED ->
                    stringResource(R.string.feature_now_service_blocked)
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
            )
            service.availability.reasons.firstOrNull()?.let { reason ->
                Text(
                    text = reason.detail.fallbackEnglish,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                onClick = onOpen,
                enabled = service.availability.status != ServiceAvailabilityStatus.BLOCKED,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-service-action-${service.id}"),
            ) {
                Text(stringResource(R.string.feature_now_choose_service))
            }
        }
    }
}

@Composable
private fun JourneyCard(
    summary: NowJourneySummary,
    onSetLocation: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenBuild: () -> Unit,
) {
    val total = summary.practiceTotal + summary.serviceTotal
    val completed = summary.practiceCompleted + summary.serviceCompleted
    val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
    val onNextAction = when (summary.nextAction) {
        NowJourneyAction.LOCATION -> onSetLocation
        NowJourneyAction.PRACTICE -> onOpenPractice
        NowJourneyAction.PRAYER -> onOpenPrayer
        NowJourneyAction.BUILD -> onOpenBuild
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-journey"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_now_journey_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.feature_now_journey_scope),
                style = MaterialTheme.typography.labelLarge,
            )
            Text("${summary.dateLabel} · ${summary.locationLabel}")
            Text(stringResource(R.string.feature_now_journey_workspace, summary.workspaceLabel))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    R.string.feature_now_journey_progress,
                    summary.practiceCompleted,
                    summary.practiceTotal,
                    summary.serviceCompleted,
                    summary.serviceTotal,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now-next-step"),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feature_now_journey_next_step),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = summary.nextActionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = summary.nextActionDetail,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Button(
                onClick = onNextAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-next-action"),
            ) {
                Text(summary.nextActionTitle)
            }
            Button(
                onClick = onOpenPrayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.feature_now_journey_open_rehearsal))
            }
            OutlinedButton(
                onClick = onOpenBuild,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.feature_now_journey_open_build))
            }
        }
    }
}

@Composable
private fun ZmanimCard(
    snapshot: ZmanimSnapshot?,
    zoneId: ZoneId,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-zmanim-card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_now_hebrew_date_heading),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            when (val hebrewDate = snapshot?.hebrewDate) {
                is HebrewDateCalculation.Available -> {
                    HebrewDateValue(
                        label = stringResource(R.string.feature_now_hebrew_date_label),
                        transliterated = hebrewDate.date.transliterated,
                        hebrew = hebrewDate.date.hebrew,
                    )
                    Text(
                        text = stringResource(R.string.feature_now_hebrew_date_boundary),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("now-hebrew-date-boundary"),
                    )
                }
                is HebrewDateCalculation.Unavailable -> {
                    Text(
                        text = stringResource(R.string.feature_now_hebrew_date_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("now-hebrew-date-unavailable"),
                    )
                }
                null -> {
                    Text(
                        text = stringResource(R.string.feature_now_calculation_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("now-hebrew-date-unavailable"),
                    )
                }
            }

            Text(
                text = stringResource(R.string.feature_now_solar_times_heading),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            SolarTimeValue(
                label = stringResource(R.string.feature_now_sunrise_label),
                calculation = snapshot?.sunrise,
                zoneId = zoneId,
                testTag = "now-sunrise",
            )
            SolarTimeValue(
                label = stringResource(R.string.feature_now_solar_noon_label),
                calculation = snapshot?.solarNoonChatzot,
                zoneId = zoneId,
                testTag = "now-solar-noon",
            )
            SolarTimeValue(
                label = stringResource(R.string.feature_now_sunset_label),
                calculation = snapshot?.sunset,
                zoneId = zoneId,
                testTag = "now-sunset",
            )

            NowValue(
                label = stringResource(R.string.feature_now_next_event_label),
                value = snapshot?.nextEvent?.let { event ->
                    val eventName = stringResource(event.type.labelResource())
                    stringResource(
                        R.string.feature_now_next_event_value,
                        eventName,
                        nextEventFormatter.format(event.instant.atZone(zoneId)),
                    )
                } ?: stringResource(R.string.feature_now_solar_time_unavailable),
                testTag = "now-next-event",
                forceLtrValue = false,
            )

            if (snapshot != null) {
                Text(
                    text = stringResource(R.string.feature_now_calculation_method_heading),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(
                        R.string.feature_now_calculation_method,
                        snapshot.metadata.engineVersion,
                        snapshot.metadata.algorithmName,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("now-calculation-method"),
                )
                Text(
                    text = stringResource(snapshot.metadata.elevationHandling.explanationResource()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("now-calculation-elevation"),
                )
            }
        }
    }
}

@Composable
private fun SolarTimeValue(
    label: String,
    calculation: ZmanCalculation?,
    zoneId: ZoneId,
    testTag: String,
) {
    NowValue(
        label = label,
        value = if (calculation is ZmanCalculation.Available) {
            solarTimeFormatter.format(calculation.instant.atZone(zoneId))
        } else {
            stringResource(R.string.feature_now_solar_time_unavailable)
        },
        testTag = testTag,
    )
}

@Composable
private fun HebrewDateValue(
    label: String,
    transliterated: String,
    hebrew: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-hebrew-date")
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = transliterated,
            style = MaterialTheme.typography.titleLarge.merge(
                TextStyle(textDirection = TextDirection.Ltr),
            ),
            modifier = Modifier.testTag("now-hebrew-date-transliterated"),
        )
        Text(
            text = hebrew,
            style = MaterialTheme.typography.titleLarge.merge(
                TextStyle(textDirection = TextDirection.Rtl),
            ),
            modifier = Modifier.testTag("now-hebrew-date-hebrew"),
        )
    }
}

@StringRes
private fun ZmanimEventType.labelResource(): Int = when (this) {
    ZmanimEventType.SUNRISE -> R.string.feature_now_sunrise_label
    ZmanimEventType.SOLAR_NOON_CHATZOT -> R.string.feature_now_solar_noon_label
    ZmanimEventType.SUNSET -> R.string.feature_now_sunset_label
}

@StringRes
private fun ElevationHandling.explanationResource(): Int = when (this) {
    ElevationHandling.REPORTED_ELEVATION -> R.string.feature_now_elevation_reported_method
    ElevationHandling.SEA_LEVEL_WHEN_NOT_REPORTED -> R.string.feature_now_elevation_missing_method
    ElevationHandling.SEA_LEVEL_FOR_BELOW_SEA_LEVEL_LOCATION ->
        R.string.feature_now_elevation_below_sea_level_method
}

@Composable
private fun PlaceContextCard(
    placeContextReady: Boolean,
    placeContext: NowPlaceContext?,
    onChange: () -> Unit,
    onRefreshDeviceLocation: (() -> Unit)? = null,
) {
    var showTechnicalDetails by remember(placeContext) { mutableStateOf(false) }
    val isDeviceLocation = placeContext?.source == NowPlaceSource.DEVICE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .testTag("now-location-status")
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.feature_now_location_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        if (!placeContextReady) {
                            R.string.feature_now_location_loading
                        } else if (placeContext == null) {
                            R.string.feature_now_location_unavailable
                        } else if (placeContext.source == NowPlaceSource.DEVICE) {
                            R.string.feature_now_location_device
                        } else {
                            R.string.feature_now_location_manual
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (placeContextReady && placeContext != null) {
                NowValue(
                    label = stringResource(
                        if (isDeviceLocation) {
                            R.string.feature_now_location_source_label
                        } else {
                            R.string.feature_now_place_name_label
                        },
                    ),
                    value = if (isDeviceLocation) {
                        stringResource(R.string.feature_now_current_location)
                    } else {
                        placeContext.label
                    },
                    testTag = "now-place-name",
                    forceLtrValue = false,
                )
                if (isDeviceLocation) {
                    TextButton(
                        onClick = { showTechnicalDetails = !showTechnicalDetails },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-place-technical-toggle"),
                    ) {
                        Text(
                            text = stringResource(
                                if (showTechnicalDetails) {
                                    R.string.feature_now_hide_technical_details
                                } else {
                                    R.string.feature_now_show_technical_details
                                },
                            ),
                        )
                    }
                }
                if (!isDeviceLocation || showTechnicalDetails) {
                    NowValue(
                        label = stringResource(R.string.feature_now_coordinates_label),
                        value = "${placeContext.latitudeDegrees.toDisplayNumber()}, " +
                            placeContext.longitudeDegrees.toDisplayNumber(),
                        testTag = "now-place-coordinates",
                    )
                }
                NowValue(
                    label = stringResource(R.string.feature_now_elevation_label),
                    value = placeContext.elevationMeters?.let {
                        stringResource(
                            R.string.feature_now_elevation_meters,
                            it.toDisplayNumber(),
                        )
                    } ?: stringResource(R.string.feature_now_not_set),
                    testTag = "now-place-elevation",
                )
                if (placeContext.horizontalAccuracyMeters != null) {
                    NowValue(
                        label = stringResource(R.string.feature_now_accuracy_label),
                        value = stringResource(
                            R.string.feature_now_accuracy_meters,
                            placeContext.horizontalAccuracyMeters.toDisplayNumber(),
                        ),
                        testTag = "now-place-accuracy",
                    )
                }
            }

            if (placeContextReady) {
                if (placeContext == null) {
                    Button(
                        onClick = onChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-set-up-place"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_set_up_place))
                    }
                } else if (onRefreshDeviceLocation != null) {
                    Button(
                        onClick = onRefreshDeviceLocation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-reacquire-device-location"),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.feature_now_refresh_device_location,
                            ),
                        )
                    }
                    OutlinedButton(
                        onClick = onChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-change-context"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_location_options))
                    }
                } else {
                    OutlinedButton(
                        onClick = onChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-change-context"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_change_context))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceContextEditor(
    existingContext: NowPlaceContext?,
    defaultTimeZoneId: String,
    deviceLocationState: DeviceLocationUiState,
    onRequestDeviceLocation: () -> Unit,
    onCancelDeviceLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onSave: suspend (NowPlaceContext) -> Unit,
    onCancel: () -> Unit,
    onClear: (suspend () -> Unit)?,
) {
    var showManualEditor by remember { mutableStateOf(false) }

    if (showManualEditor) {
        ManualPlaceEditor(
            existingContext = existingContext,
            defaultTimeZoneId = defaultTimeZoneId,
            onSave = onSave,
            onCancel = onCancel,
            onClear = onClear,
        )
    } else {
        DevicePlaceEditor(
            state = deviceLocationState,
            onRequest = onRequestDeviceLocation,
            onCancelRequest = onCancelDeviceLocation,
            onOpenAppSettings = onOpenAppSettings,
            onOpenLocationSettings = onOpenLocationSettings,
            onUseManual = {
                onCancelDeviceLocation()
                showManualEditor = true
            },
            onSave = onSave,
            onCancel = {
                onCancelDeviceLocation()
                onCancel()
            },
            onClear = onClear,
        )
    }
}

@Composable
private fun DevicePlaceEditor(
    state: DeviceLocationUiState,
    onRequest: () -> Unit,
    onCancelRequest: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onUseManual: () -> Unit,
    onSave: suspend (NowPlaceContext) -> Unit,
    onCancel: () -> Unit,
    onClear: (suspend () -> Unit)?,
) {
    val editorScope = rememberCoroutineScope()
    var operationInProgress by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var clearFailed by remember { mutableStateOf(false) }
    var showTechnicalDetails by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-place-editor"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_now_editor_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.feature_now_editor_explanation),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.feature_now_device_location_help),
                style = MaterialTheme.typography.bodyMedium,
            )

            when (state) {
                DeviceLocationUiState.Idle -> {
                    Button(
                        onClick = onRequest,
                        enabled = !operationInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-use-device-location"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_use_device_location))
                    }
                }

                DeviceLocationUiState.Locating -> {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("now-location-progress"),
                    )
                    StatusMessage(
                        text = stringResource(R.string.feature_now_locating),
                        testTag = "now-location-status-message",
                    )
                    OutlinedButton(
                        onClick = onCancelRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-cancel-location-request"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_cancel))
                    }
                }

                is DeviceLocationUiState.PermissionDenied -> {
                    StatusMessage(
                        text = stringResource(
                            if (state.openSettingsRequired) {
                                R.string.feature_now_location_permission_settings
                            } else {
                                R.string.feature_now_location_permission_denied
                            },
                        ),
                        testTag = "now-location-permission-error",
                        isError = true,
                    )
                    Button(
                        onClick = if (state.openSettingsRequired) {
                            onOpenAppSettings
                        } else {
                            onRequest
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-location-permission-action"),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.openSettingsRequired) {
                                    R.string.feature_now_open_app_settings
                                } else {
                                    R.string.feature_now_retry_location
                                },
                            ),
                        )
                    }
                }

                DeviceLocationUiState.LocationDisabled -> {
                    StatusMessage(
                        text = stringResource(R.string.feature_now_location_disabled),
                        testTag = "now-location-disabled-error",
                        isError = true,
                    )
                    Button(
                        onClick = onOpenLocationSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-open-location-settings"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_open_location_settings))
                    }
                    OutlinedButton(
                        onClick = onRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-retry-location"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_retry_location))
                    }
                }

                DeviceLocationUiState.TimedOut,
                DeviceLocationUiState.Unavailable,
                -> {
                    StatusMessage(
                        text = stringResource(
                            if (state == DeviceLocationUiState.TimedOut) {
                                R.string.feature_now_location_timeout
                            } else {
                                R.string.feature_now_location_unavailable_error
                            },
                        ),
                        testTag = "now-location-unavailable-error",
                        isError = true,
                    )
                    Button(
                        onClick = onRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-retry-location"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_retry_location))
                    }
                }

                is DeviceLocationUiState.Preview -> {
                    Text(
                        text = stringResource(R.string.feature_now_location_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.feature_now_location_preview_explanation),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    NowValue(
                        label = stringResource(R.string.feature_now_location_label),
                        value = stringResource(
                            if (state.isApproximate) {
                                R.string.feature_now_location_approximate
                            } else {
                                R.string.feature_now_location_precise
                            },
                        ),
                        testTag = "now-location-preview-kind",
                        forceLtrValue = false,
                    )
                    state.context.horizontalAccuracyMeters?.let { accuracy ->
                        NowValue(
                            label = stringResource(R.string.feature_now_accuracy_label),
                            value = stringResource(
                                R.string.feature_now_accuracy_meters,
                                accuracy.toDisplayNumber(),
                            ),
                            testTag = "now-location-preview-accuracy",
                        )
                    }
                    NowValue(
                        label = stringResource(R.string.feature_now_time_zone_label),
                        value = state.context.timeZoneId,
                        testTag = "now-location-preview-time-zone",
                    )
                    TextButton(
                        onClick = { showTechnicalDetails = !showTechnicalDetails },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-location-technical-toggle"),
                    ) {
                        Text(
                            text = stringResource(
                                if (showTechnicalDetails) {
                                    R.string.feature_now_hide_technical_details
                                } else {
                                    R.string.feature_now_show_technical_details
                                },
                            ),
                        )
                    }
                    if (showTechnicalDetails) {
                        Text(
                            text = stringResource(
                                R.string.feature_now_technical_coordinates,
                                state.context.latitudeDegrees.toDisplayNumber(),
                                state.context.longitudeDegrees.toDisplayNumber(),
                            ),
                            style = MaterialTheme.typography.bodyMedium.merge(
                                TextStyle(textDirection = TextDirection.Ltr),
                            ),
                            modifier = Modifier.testTag("now-location-technical-coordinates"),
                        )
                    }
                    if (saveFailed) {
                        StatusMessage(
                            text = stringResource(R.string.feature_now_save_persistence_error),
                            testTag = "now-place-persistence-error",
                            isError = true,
                        )
                    }
                    Button(
                        onClick = {
                            operationInProgress = true
                            saveFailed = false
                            editorScope.launch {
                                try {
                                    withContext(NonCancellable) { onSave(state.context) }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveFailed = true
                                } finally {
                                    operationInProgress = false
                                }
                            }
                        },
                        enabled = !operationInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-use-this-location"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_use_this_location))
                    }
                    OutlinedButton(
                        onClick = onRequest,
                        enabled = !operationInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("now-refresh-device-location"),
                    ) {
                        Text(text = stringResource(R.string.feature_now_retry_location))
                    }
                }
            }

            OutlinedButton(
                onClick = onUseManual,
                enabled = !operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-enter-location-manually"),
            ) {
                Text(text = stringResource(R.string.feature_now_enter_manually))
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-cancel-context"),
            ) {
                Text(text = stringResource(R.string.feature_now_cancel))
            }
            if (onClear != null) {
                if (clearFailed) {
                    StatusMessage(
                        text = stringResource(R.string.feature_now_clear_persistence_error),
                        testTag = "now-place-persistence-error",
                        isError = true,
                    )
                }
                TextButton(
                    onClick = {
                        clearFailed = false
                        operationInProgress = true
                        editorScope.launch {
                            try {
                                withContext(NonCancellable) { onClear() }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                clearFailed = true
                            } finally {
                                operationInProgress = false
                            }
                        }
                    },
                    enabled = !operationInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("now-clear-context"),
                ) {
                    Text(text = stringResource(R.string.feature_now_clear_context))
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    text: String,
    testTag: String,
    isError: Boolean = false,
) {
    Text(
        text = text,
        color = if (isError) MaterialTheme.colorScheme.error else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .testTag(testTag)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ManualPlaceEditor(
    existingContext: NowPlaceContext?,
    defaultTimeZoneId: String,
    onSave: suspend (NowPlaceContext) -> Unit,
    onCancel: () -> Unit,
    onClear: (suspend () -> Unit)?,
) {
    val initialDraft = remember(existingContext, defaultTimeZoneId) {
        ManualPlaceDraft.initial(existingContext, defaultTimeZoneId)
    }
    var label by remember { mutableStateOf(initialDraft.label) }
    var latitude by remember { mutableStateOf(initialDraft.latitude) }
    var longitude by remember { mutableStateOf(initialDraft.longitude) }
    var elevation by remember { mutableStateOf(initialDraft.elevation) }
    var selectedTimeZoneId by remember { mutableStateOf(initialDraft.timeZoneId) }
    var showErrors by remember { mutableStateOf(false) }
    var showTimeZonePicker by remember { mutableStateOf(false) }
    var operationInProgress by remember { mutableStateOf(false) }
    var failedPersistenceOperation by remember { mutableStateOf<PersistenceOperation?>(null) }
    var focusFirstErrorRequest by remember { mutableIntStateOf(0) }
    val editorScope = rememberCoroutineScope()
    val labelFocusRequester = remember { FocusRequester() }
    val latitudeFocusRequester = remember { FocusRequester() }
    val longitudeFocusRequester = remember { FocusRequester() }
    val elevationFocusRequester = remember { FocusRequester() }
    val timeZoneFocusRequester = remember { FocusRequester() }

    val draft = ManualPlaceDraft(
        label = label,
        latitude = latitude,
        longitude = longitude,
        elevation = elevation,
        timeZoneId = selectedTimeZoneId,
    )
    val validation = validateManualPlaceDraft(draft)

    LaunchedEffect(focusFirstErrorRequest) {
        if (focusFirstErrorRequest == 0) return@LaunchedEffect
        when {
            validation.labelError != null -> labelFocusRequester
            validation.latitudeError != null -> latitudeFocusRequester
            validation.longitudeError != null -> longitudeFocusRequester
            validation.elevationError != null -> elevationFocusRequester
            validation.timeZoneError != null -> timeZoneFocusRequester
            else -> null
        }?.requestFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now-place-editor"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_now_manual_editor_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.feature_now_manual_editor_explanation),
                style = MaterialTheme.typography.bodyLarge,
            )

            ManualPlaceTextField(
                value = label,
                onValueChange = { label = it },
                label = stringResource(R.string.feature_now_place_name_label),
                validationMessage = if (showErrors) {
                    manualPlaceErrorText(validation.labelError)
                } else {
                    null
                },
                testTag = "now-place-label-input",
                enabled = !operationInProgress,
                focusRequester = labelFocusRequester,
            )
            ManualPlaceTextField(
                value = latitude,
                onValueChange = { latitude = it },
                label = stringResource(R.string.feature_now_latitude_label),
                supportingText = stringResource(R.string.feature_now_latitude_help),
                validationMessage = if (showErrors) {
                    manualPlaceErrorText(
                        validation.latitudeError,
                        R.string.feature_now_error_latitude_range,
                    )
                } else {
                    null
                },
                testTag = "now-place-latitude-input",
                numeric = true,
                enabled = !operationInProgress,
                focusRequester = latitudeFocusRequester,
            )
            ManualPlaceTextField(
                value = longitude,
                onValueChange = { longitude = it },
                label = stringResource(R.string.feature_now_longitude_label),
                supportingText = stringResource(R.string.feature_now_longitude_help),
                validationMessage = if (showErrors) {
                    manualPlaceErrorText(
                        validation.longitudeError,
                        R.string.feature_now_error_longitude_range,
                    )
                } else {
                    null
                },
                testTag = "now-place-longitude-input",
                numeric = true,
                enabled = !operationInProgress,
                focusRequester = longitudeFocusRequester,
            )
            ManualPlaceTextField(
                value = elevation,
                onValueChange = { elevation = it },
                label = stringResource(R.string.feature_now_elevation_optional_label),
                validationMessage = if (showErrors) {
                    manualPlaceErrorText(validation.elevationError)
                } else {
                    null
                },
                testTag = "now-place-elevation-input",
                numeric = true,
                enabled = !operationInProgress,
                focusRequester = elevationFocusRequester,
            )
            ManualPlaceTextField(
                value = selectedTimeZoneId,
                onValueChange = {},
                label = stringResource(R.string.feature_now_time_zone_label),
                supportingText = stringResource(R.string.feature_now_time_zone_help),
                validationMessage = if (showErrors) {
                    manualPlaceErrorText(validation.timeZoneError)
                } else {
                    null
                },
                testTag = "now-place-time-zone-input",
                readOnly = true,
                forceLtrText = true,
                enabled = !operationInProgress,
                focusRequester = timeZoneFocusRequester,
            )
            OutlinedButton(
                onClick = { showTimeZonePicker = true },
                enabled = !operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-select-time-zone"),
            ) {
                Text(text = stringResource(R.string.feature_now_select_time_zone))
            }

            if (showErrors && !validation.isValid) {
                Text(
                    text = stringResource(R.string.feature_now_error_summary),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .testTag("now-place-error-summary")
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            if (failedPersistenceOperation != null) {
                Text(
                    text = stringResource(
                        if (failedPersistenceOperation == PersistenceOperation.CLEAR) {
                            R.string.feature_now_clear_persistence_error
                        } else {
                            R.string.feature_now_save_persistence_error
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .testTag("now-place-persistence-error")
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }

            Button(
                onClick = {
                    showErrors = true
                    failedPersistenceOperation = null
                    val validatedContext = validation.context
                    if (validatedContext == null) {
                        focusFirstErrorRequest += 1
                    } else {
                        operationInProgress = true
                        editorScope.launch {
                            try {
                                withContext(NonCancellable) {
                                    onSave(validatedContext)
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failedPersistenceOperation = PersistenceOperation.SAVE
                            } finally {
                                operationInProgress = false
                            }
                        }
                    }
                },
                enabled = !operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-save-context"),
            ) {
                Text(text = stringResource(R.string.feature_now_save_context))
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("now-cancel-context"),
            ) {
                Text(text = stringResource(R.string.feature_now_cancel))
            }
            if (onClear != null) {
                TextButton(
                    onClick = {
                        failedPersistenceOperation = null
                        operationInProgress = true
                        editorScope.launch {
                            try {
                                withContext(NonCancellable) {
                                    onClear()
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failedPersistenceOperation = PersistenceOperation.CLEAR
                            } finally {
                                operationInProgress = false
                            }
                        }
                    },
                    enabled = !operationInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("now-clear-context"),
                ) {
                    Text(text = stringResource(R.string.feature_now_clear_context))
                }
            }
        }
    }

    if (showTimeZonePicker) {
        TimeZonePickerDialog(
            selectedTimeZoneId = selectedTimeZoneId,
            onSelect = {
                selectedTimeZoneId = it
                showTimeZonePicker = false
            },
            onDismiss = { showTimeZonePicker = false },
        )
    }
}

@Composable
private fun ManualPlaceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validationMessage: String?,
    testTag: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    numeric: Boolean = false,
    readOnly: Boolean = false,
    forceLtrText: Boolean = numeric,
    enabled: Boolean = true,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = (validationMessage ?: supportingText)?.let { message ->
            { Text(message) }
        },
        isError = validationMessage != null,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.DecimalSigned)
        } else {
            KeyboardOptions.Default
        },
        textStyle = if (forceLtrText) {
            MaterialTheme.typography.bodyLarge.merge(
                TextStyle(textDirection = TextDirection.Ltr),
            )
        } else {
            MaterialTheme.typography.bodyLarge
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .focusRequester(focusRequester)
            .testTag(testTag)
            .semantics {
                validationMessage?.let { error(it) }
            },
    )
}

@Composable
private fun TimeZonePickerDialog(
    selectedTimeZoneId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredTimeZones = remember(query) {
        if (query.isBlank()) {
            availableTimeZoneIds
        } else {
            availableTimeZoneIds.filter {
                it.contains(query.trim(), ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.feature_now_time_zone_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.testTag("now-time-zone-dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = {
                        Text(text = stringResource(R.string.feature_now_time_zone_search_label))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now-time-zone-search"),
                )
                if (filteredTimeZones.isEmpty()) {
                    Text(
                        text = stringResource(R.string.feature_now_time_zone_no_results),
                        modifier = Modifier.testTag("now-time-zone-no-results"),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .testTag("now-time-zone-list"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            items = filteredTimeZones,
                            key = { it },
                        ) { timeZoneId ->
                            TextButton(
                                onClick = { onSelect(timeZoneId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("now-time-zone-option-$timeZoneId")
                                    .semantics {
                                        selected = timeZoneId == selectedTimeZoneId
                                    },
                            ) {
                                Text(
                                    text = timeZoneId,
                                    style = MaterialTheme.typography.bodyLarge.merge(
                                        TextStyle(textDirection = TextDirection.Ltr),
                                    ),
                                )
                                if (timeZoneId == selectedTimeZoneId) {
                                    Text(text = " •")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.feature_now_cancel))
            }
        },
    )
}

@Composable
private fun manualPlaceErrorText(
    error: ManualPlaceInputError?,
    @StringRes outOfRangeMessage: Int? = null,
): String? = when (error) {
    null -> null
    ManualPlaceInputError.REQUIRED -> stringResource(R.string.feature_now_error_required)
    ManualPlaceInputError.TOO_LONG -> stringResource(
        R.string.feature_now_error_label_too_long,
        MAX_PLACE_LABEL_LENGTH,
    )
    ManualPlaceInputError.INVALID_NUMBER -> stringResource(
        R.string.feature_now_error_invalid_number,
    )
    ManualPlaceInputError.OUT_OF_RANGE -> stringResource(requireNotNull(outOfRangeMessage))
    ManualPlaceInputError.UNKNOWN_TIME_ZONE -> stringResource(
        R.string.feature_now_error_unknown_time_zone,
    )
}

@Composable
private fun NowValue(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
    forceLtrValue: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (forceLtrValue) {
                MaterialTheme.typography.titleLarge.merge(
                    TextStyle(textDirection = TextDirection.Ltr),
                )
            } else {
                MaterialTheme.typography.titleLarge
            },
        )
    }
}

private fun Double.toDisplayNumber(): String =
    BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

private enum class PersistenceOperation {
    SAVE,
    CLEAR,
}

@Preview(
    name = "English phone, 200 percent",
    locale = "en",
    fontScale = 2f,
    widthDp = 360,
    heightDp = 640,
    showBackground = true,
)
@Composable
private fun NowEnglishPreview() {
    NowScreen(clock = Clock.systemDefaultZone())
}

@Preview(
    name = "Hebrew RTL tablet, English fallback",
    locale = "he",
    widthDp = 800,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun NowHebrewPreview() {
    NowScreen(
        clock = Clock.systemDefaultZone(),
        placeContext = NowPlaceContext(
            label = "מקום בדיקה",
            latitudeDegrees = 12.25,
            longitudeDegrees = -34.5,
            elevationMeters = 123.75,
            timeZoneId = "UTC",
        ),
    )
}
