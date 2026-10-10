package io.github.gilnetizen.aseh.feature.prayer

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.IndividualPrayerService
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ServiceSegment
import io.github.gilnetizen.aseh.core.model.ServiceTextAvailability
import io.github.gilnetizen.aseh.core.model.ServiceTextField
import io.github.gilnetizen.aseh.core.model.SourceUnit
import io.github.gilnetizen.aseh.core.model.buildIndividualPrayerPacket

/** A Hebrew-first, sequential reader for an individual's offline prayer service. */
@Composable
fun IndividualPrayerScreen(
    services: List<IndividualPrayerService>,
    selectedServiceId: String?,
    suggestedServiceId: String?,
    onServiceSelected: (String) -> Unit,
    state: ExperienceState = ExperienceState(),
    dateLabel: String = "",
    locationLabel: String = "",
    onUseDeviceLocation: (() -> Unit)? = null,
    onSegmentCompleted: (String, Boolean) -> Unit = { _, _ -> },
    onSharePacket: (String) -> Unit = {},
    onPrintPacket: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val service = services.firstOrNull { it.id == selectedServiceId }
        ?: services.firstOrNull { it.id == suggestedServiceId }
        ?: services.firstOrNull()
    if (service == null) {
        IndividualPrayerUnavailable(modifier)
        return
    }

    val completedIds = state.completedServiceSegmentIds
    val packet = remember(service, completedIds, dateLabel, locationLabel) {
        buildIndividualPrayerPacket(
            service = service,
            completedSegmentIds = completedIds,
            dateLabel = dateLabel,
            locationLabel = locationLabel,
        )
    }
    var readerOpen by rememberSaveable(service.id) { mutableStateOf(false) }
    var currentIndex by rememberSaveable(service.id) {
        mutableIntStateOf(firstIncompleteSegmentIndex(service.segments, completedIds))
    }

    BackHandler(enabled = readerOpen) { readerOpen = false }

    if (readerOpen) {
        IndividualPrayerReader(
            service = service,
            completedIds = completedIds,
            currentIndex = currentIndex.coerceIn(0, service.segments.lastIndex),
            onOverview = { readerOpen = false },
            onPrevious = { currentIndex = (currentIndex - 1).coerceAtLeast(0) },
            onNext = { currentIndex = (currentIndex + 1).coerceAtMost(service.segments.lastIndex) },
            onSegmentCompleted = onSegmentCompleted,
            modifier = modifier,
        )
        return
    }

    IndividualPrayerOverview(
        services = services,
        service = service,
        suggestedServiceId = suggestedServiceId,
        completedIds = completedIds,
        dateLabel = dateLabel,
        locationLabel = locationLabel,
        onServiceSelected = onServiceSelected,
        onUseDeviceLocation = onUseDeviceLocation,
        onBegin = {
            currentIndex = firstIncompleteSegmentIndex(service.segments, completedIds)
            readerOpen = true
        },
        onSharePacket = { onSharePacket(packet) },
        onPrintPacket = { onPrintPacket(packet) },
        modifier = modifier,
    )
}

@Composable
private fun IndividualPrayerUnavailable(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("individual-prayer-unavailable"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.individual_prayer_unavailable_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.individual_prayer_unavailable_body),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun IndividualPrayerOverview(
    services: List<IndividualPrayerService>,
    service: IndividualPrayerService,
    suggestedServiceId: String?,
    completedIds: Set<String>,
    dateLabel: String,
    locationLabel: String,
    onServiceSelected: (String) -> Unit,
    onUseDeviceLocation: (() -> Unit)?,
    onBegin: () -> Unit,
    onSharePacket: () -> Unit,
    onPrintPacket: () -> Unit,
    modifier: Modifier,
) {
    val completedCount = service.segments.count { segment -> segment.id in completedIds }
    val beginLabel = when {
        completedCount == 0 -> stringResource(R.string.individual_prayer_start)
        completedCount == service.segments.size -> stringResource(R.string.individual_prayer_review)
        else -> stringResource(R.string.individual_prayer_resume)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("individual-prayer-screen"),
        contentPadding = PaddingValues(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ServiceChooser(
                services = services,
                selectedServiceId = service.id,
                suggestedServiceId = suggestedServiceId,
                onServiceSelected = onServiceSelected,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag("individual-prayer-heading"),
                )
                Text(service.subtitle, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = service.notice,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (dateLabel.isNotBlank()) {
                    ContextValue(stringResource(R.string.individual_prayer_date), dateLabel)
                }
                if (locationLabel.isNotBlank()) {
                    ContextValue(stringResource(R.string.individual_prayer_place), locationLabel)
                }
                Text(
                    text = stringResource(
                        R.string.individual_prayer_progress,
                        completedCount,
                        service.segments.size,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("individual-prayer-progress"),
                )
            }
        }
        if (locationLabel.isBlank()) {
            item {
                MissingLocationCard(onUseDeviceLocation = onUseDeviceLocation)
            }
        }
        item {
            Button(
                onClick = onBegin,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("individual-prayer-begin"),
            ) {
                Text(beginLabel)
            }
        }
        item {
            Text(
                text = stringResource(R.string.individual_prayer_order),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
        }
        items(
            count = service.segments.size,
            key = { index -> service.segments[index].id },
        ) { index ->
            val segment = service.segments[index]
            val complete = segment.id in completedIds
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.individual_prayer_step,
                            index + 1,
                            service.segments.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(segment.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            if (complete) {
                                R.string.individual_prayer_complete
                            } else {
                                R.string.individual_prayer_not_complete
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            HorizontalDivider()
        }
        item {
            Text(
                text = stringResource(R.string.individual_prayer_offline_packet),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onSharePacket,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("individual-prayer-share"),
                ) {
                    Text(stringResource(R.string.individual_prayer_share))
                }
                OutlinedButton(
                    onClick = onPrintPacket,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("individual-prayer-print"),
                ) {
                    Text(stringResource(R.string.individual_prayer_print))
                }
            }
        }
    }
}

@Composable
private fun ServiceChooser(
    services: List<IndividualPrayerService>,
    selectedServiceId: String,
    suggestedServiceId: String?,
    onServiceSelected: (String) -> Unit,
) {
    val selectedDescription = stringResource(R.string.individual_prayer_service_selected)
    val notSelectedDescription = stringResource(R.string.individual_prayer_service_not_selected)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .testTag("individual-prayer-service-chooser"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.individual_prayer_choose_service),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.individual_prayer_choose_service_help),
            style = MaterialTheme.typography.bodyMedium,
        )
        services.forEach { option ->
            val selected = option.id == selectedServiceId
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(
                        selected = selected,
                        onClick = { onServiceSelected(option.id) },
                        role = Role.RadioButton,
                    )
                    .semantics {
                        stateDescription = if (selected) {
                            selectedDescription
                        } else {
                            notSelectedDescription
                        }
                    }
                    .testTag("individual-prayer-service-${option.id}"),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (option.id == suggestedServiceId) {
                        Text(
                            text = stringResource(
                                R.string.individual_prayer_service_suggested_local_time,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Text(option.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MissingLocationCard(onUseDeviceLocation: (() -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("individual-prayer-no-location"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.individual_prayer_location_optional_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.individual_prayer_location_optional_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (onUseDeviceLocation != null) {
                Button(
                    onClick = onUseDeviceLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("individual-prayer-use-device-location"),
                ) {
                    Text(stringResource(R.string.individual_prayer_use_device_location))
                }
            }
        }
    }
}

@Composable
private fun IndividualPrayerReader(
    service: IndividualPrayerService,
    completedIds: Set<String>,
    currentIndex: Int,
    onOverview: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSegmentCompleted: (String, Boolean) -> Unit,
    modifier: Modifier,
) {
    val segment = service.segments[currentIndex]
    val isComplete = segment.id in completedIds
    val listState = rememberLazyListState()
    var translationExpanded by rememberSaveable(service.id, segment.id) { mutableStateOf(false) }
    var cuesExpanded by rememberSaveable(service.id, segment.id) { mutableStateOf(false) }
    var sourcesExpanded by rememberSaveable(service.id, segment.id) { mutableStateOf(false) }

    LaunchedEffect(currentIndex) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("individual-prayer-reader"),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(
                onClick = onOverview,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("individual-prayer-overview"),
            ) {
                Text(stringResource(R.string.individual_prayer_back_to_overview))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        R.string.individual_prayer_step,
                        currentIndex + 1,
                        service.segments.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(segment.phase, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = segment.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag("individual-prayer-segment-title"),
                )
                if (segment.summary.isNotBlank()) {
                    Text(segment.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            PrimaryHebrewText(segment.content.sourceText)
        }
        item {
            ExpandButton(
                label = stringResource(R.string.individual_prayer_translation_and_transliteration),
                expanded = translationExpanded,
                testTag = "individual-prayer-toggle-translation",
                onClick = { translationExpanded = !translationExpanded },
            )
        }
        if (translationExpanded) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.testTag("individual-prayer-translation-panel"),
                ) {
                    SecondaryPrayerText(
                        label = stringResource(R.string.individual_prayer_translation),
                        field = segment.content.translation,
                    )
                    SecondaryPrayerText(
                        label = stringResource(R.string.individual_prayer_transliteration),
                        field = segment.content.transliteration,
                    )
                }
            }
        }
        item {
            ExpandButton(
                label = stringResource(R.string.individual_prayer_cues),
                expanded = cuesExpanded,
                testTag = "individual-prayer-toggle-cues",
                onClick = { cuesExpanded = !cuesExpanded },
            )
        }
        if (cuesExpanded) {
            item {
                PrayerCues(segment)
            }
        }
        item {
            ExpandButton(
                label = stringResource(R.string.individual_prayer_sources),
                expanded = sourcesExpanded,
                testTag = "individual-prayer-toggle-sources",
                onClick = { sourcesExpanded = !sourcesExpanded },
            )
        }
        if (sourcesExpanded) {
            item {
                PrayerSources(
                    service = service,
                    segment = segment,
                )
            }
        }
        item {
            Button(
                onClick = { onSegmentCompleted(segment.id, !isComplete) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics {
                        stateDescription = if (isComplete) {
                            "complete"
                        } else {
                            "not complete"
                        }
                    }
                    .testTag("individual-prayer-completion"),
            ) {
                Text(
                    stringResource(
                        if (isComplete) {
                            R.string.individual_prayer_mark_not_complete
                        } else {
                            R.string.individual_prayer_mark_complete
                        },
                    ),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag("individual-prayer-previous"),
                ) {
                    Text(stringResource(R.string.individual_prayer_previous))
                }
                Button(
                    onClick = if (currentIndex < service.segments.lastIndex) onNext else onOverview,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag("individual-prayer-next"),
                ) {
                    Text(
                        stringResource(
                            if (currentIndex < service.segments.lastIndex) {
                                R.string.individual_prayer_next
                            } else {
                                R.string.individual_prayer_finish
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryHebrewText(field: ServiceTextField) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("individual-prayer-hebrew-text"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.individual_prayer_hebrew_text),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            when (field.availability) {
                ServiceTextAvailability.AVAILABLE -> Text(
                    text = field.text,
                    style = MaterialTheme.typography.headlineSmall.merge(
                        TextStyle(
                            textDirection = TextDirection.ContentOrRtl,
                            lineHeight = 40.sp,
                        ),
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )

                ServiceTextAvailability.UNAVAILABLE -> Text(
                    text = stringResource(R.string.individual_prayer_text_unavailable, field.detail),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )

                ServiceTextAvailability.NOT_APPLICABLE -> Text(
                    text = field.detail,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SecondaryPrayerText(label: String, field: ServiceTextField) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        when (field.availability) {
            ServiceTextAvailability.AVAILABLE -> Text(
                text = field.text,
                style = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(textDirection = TextDirection.ContentOrLtr),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            ServiceTextAvailability.UNAVAILABLE -> Text(
                text = stringResource(R.string.individual_prayer_text_unavailable, field.detail),
                color = MaterialTheme.colorScheme.error,
            )

            ServiceTextAvailability.NOT_APPLICABLE -> Text(
                text = stringResource(R.string.individual_prayer_not_included),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PrayerCues(segment: ServiceSegment) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("individual-prayer-cues-panel"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CueValue(stringResource(R.string.individual_prayer_movement), segment.movementCue)
            CueValue(
                stringResource(R.string.individual_prayer_accessible_alternative),
                segment.accessibleAlternative,
            )
            CueValue(stringResource(R.string.individual_prayer_voice), segment.voiceCue)
            segment.roleCues[ParticipantRole.CONGREGANT]
                ?.takeIf(String::isNotBlank)
                ?.let { cue -> CueValue(stringResource(R.string.individual_prayer_personal_cue), cue) }
        }
    }
}

@Composable
private fun PrayerSources(
    service: IndividualPrayerService,
    segment: ServiceSegment,
) {
    val provenance = segment.content.provenance
    val sourcesById = remember(service.sources) { service.sources.associateBy(SourceUnit::id) }
    val sourceIds = remember(segment) {
        (
            segment.explanation.sourceIds +
                provenance.sourceUnitId.takeIf { id ->
                    id.isNotBlank() && id != "Not established"
                }.orEmpty()
            ).distinct()
    }

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("individual-prayer-sources-panel"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SourceValue(stringResource(R.string.individual_prayer_edition), provenance.editionTitle)
            SourceValue(stringResource(R.string.individual_prayer_edition_id), provenance.editionId)
            SourceValue(stringResource(R.string.individual_prayer_locator), provenance.locator)
            SourceValue(
                stringResource(R.string.individual_prayer_editorial_treatment),
                provenance.editorialTreatment,
            )
            SourceValue(
                stringResource(R.string.individual_prayer_review_state),
                "${provenance.reviewState.label} · ${provenance.conclusionStatus.label}",
            )
            SourceValue(stringResource(R.string.individual_prayer_license), provenance.license)

            if (sourceIds.isEmpty()) {
                Text(
                    text = stringResource(R.string.individual_prayer_no_sources),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                sourceIds.forEach { sourceId ->
                    val source = sourcesById[sourceId]
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("individual-prayer-source-$sourceId"),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = source?.title ?: sourceId,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            source?.let {
                                Text(
                                    text = "${it.locator} · ${it.edition}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(it.body, style = MaterialTheme.typography.bodyMedium)
                                Text(it.provenance, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandButton(
    label: String,
    expanded: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val state = stringResource(
        if (expanded) R.string.individual_prayer_expanded else R.string.individual_prayer_collapsed,
    )
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                stateDescription = state
            }
            .testTag(testTag),
    ) {
        Text(
            text = stringResource(
                if (expanded) R.string.individual_prayer_hide_section else R.string.individual_prayer_show_section,
                label,
            ),
        )
    }
}

@Composable
private fun ContextValue(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium.merge(
            TextStyle(textDirection = TextDirection.ContentOrLtr),
        ),
    )
}

@Composable
private fun CueValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SourceValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(textDirection = TextDirection.ContentOrLtr),
            ),
        )
    }
}

private fun firstIncompleteSegmentIndex(
    segments: List<ServiceSegment>,
    completedIds: Set<String>,
): Int = segments.indexOfFirst { segment -> segment.id !in completedIds }
    .takeIf { index -> index >= 0 }
    ?: 0
