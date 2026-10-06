package io.github.gilnetizen.aseh.feature.now

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val civilDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)

private val weekdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

private val localTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)

private val availableTimeZoneIds: List<String> by lazy {
    ZoneId.getAvailableZoneIds().sorted()
}

internal data class NowUiState(
    val civilDate: String,
    val weekday: String,
    val localTime: String,
    val timeZone: String,
)

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
    var snapshot by mutableStateOf(currentNowUiState(clock, timeZone))
        private set

    fun refresh() {
        snapshot = currentNowUiState(clock, timeZone)
    }
}

@Composable
fun NowScreen(
    clock: Clock,
    timeZone: () -> ZoneId = { clock.zone },
    placeContextReady: Boolean = true,
    placeContext: NowPlaceContext? = null,
    onSavePlaceContext: suspend (NowPlaceContext) -> Unit = {},
    onClearPlaceContext: suspend () -> Unit = {},
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
    var editingPlace by remember { mutableStateOf(false) }

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

        if (editingPlace) {
            ManualPlaceEditor(
                existingContext = placeContext,
                defaultTimeZoneId = state.snapshot.timeZone,
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
        } else {
            PlaceContextCard(
                placeContextReady = placeContextReady,
                placeContext = placeContext,
                onChange = { editingPlace = true },
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
private fun PlaceContextCard(
    placeContextReady: Boolean,
    placeContext: NowPlaceContext?,
    onChange: () -> Unit,
) {
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
                        } else {
                            R.string.feature_now_location_manual
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (placeContextReady && placeContext != null) {
                NowValue(
                    label = stringResource(R.string.feature_now_place_name_label),
                    value = placeContext.label,
                    testTag = "now-place-name",
                    forceLtrValue = false,
                )
                NowValue(
                    label = stringResource(R.string.feature_now_coordinates_label),
                    value = "${placeContext.latitudeDegrees.toDisplayNumber()}, " +
                        placeContext.longitudeDegrees.toDisplayNumber(),
                    testTag = "now-place-coordinates",
                )
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
            }

            if (placeContextReady) {
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
                text = stringResource(R.string.feature_now_editor_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.feature_now_editor_explanation),
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
