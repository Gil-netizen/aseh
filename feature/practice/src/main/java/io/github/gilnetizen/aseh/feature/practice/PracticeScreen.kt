package io.github.gilnetizen.aseh.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.PracticeCard
import io.github.gilnetizen.aseh.core.model.PracticeStep
import io.github.gilnetizen.aseh.core.model.SourceUnit

@Composable
fun PracticeScreen(
    catalog: DemonstratorCatalog? = null,
    state: ExperienceState = ExperienceState(),
    occurrenceProgressEnabled: Boolean = true,
    occurrenceProgressActivating: Boolean = false,
    requestedCardId: String? = null,
    onRequestedCardConsumed: () -> Unit = {},
    onStepCompleted: (String, Boolean) -> Unit = { _, _ -> },
    onCardSaved: (String, Boolean) -> Unit = { _, _ -> },
    onOpenSource: (String) -> Unit = {},
    onOpenPrayer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedCardId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(requestedCardId, catalog) {
        val requested = requestedCardId ?: return@LaunchedEffect
        if (catalog?.practiceCards?.any { card -> card.id == requested } == true) {
            selectedCardId = requested
        }
        onRequestedCardConsumed()
    }
    val selectedCard = catalog?.practiceCards?.firstOrNull { it.id == selectedCardId }
    BackHandler(enabled = selectedCard != null) { selectedCardId = null }

    if (catalog != null && selectedCard != null) {
        PracticeCardDetail(
            catalog = catalog,
            card = selectedCard,
            state = state,
            occurrenceProgressEnabled = occurrenceProgressEnabled,
            occurrenceProgressActivating = occurrenceProgressActivating,
            onBack = { selectedCardId = null },
            onStepCompleted = onStepCompleted,
            onCardSaved = onCardSaved,
            onOpenSource = onOpenSource,
            onOpenPrayer = onOpenPrayer,
            modifier = modifier,
        )
    } else {
        PracticeCatalog(
            catalog = catalog,
            state = state,
            occurrenceProgressEnabled = occurrenceProgressEnabled,
            occurrenceProgressActivating = occurrenceProgressActivating,
            onCardSelected = { selectedCardId = it },
            onOpenPrayer = onOpenPrayer,
            modifier = modifier,
        )
    }
}

@Composable
private fun PracticeCatalog(
    catalog: DemonstratorCatalog?,
    state: ExperienceState,
    occurrenceProgressEnabled: Boolean,
    occurrenceProgressActivating: Boolean,
    onCardSelected: (String) -> Unit,
    onOpenPrayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("practice-catalog"),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 32.dp,
            end = 24.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "practice-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.feature_practice_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .testTag("practice-heading")
                        .semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.feature_practice_catalog_intro),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item(key = "practice-notice") {
            DevelopmentContentNotice(catalog)
        }

        if (catalog != null && !occurrenceProgressEnabled) {
            item(key = "practice-progress-unavailable") {
                OccurrenceProgressUnavailableNotice(occurrenceProgressActivating)
            }
        }

        if (catalog == null) {
            item(key = "practice-empty") {
                EmptyCatalog()
            }
        } else {
            val progress = practiceCatalogProgress(catalog, state)
            item(key = "practice-progress") {
                ProgressSummary(
                    title = stringResource(R.string.feature_practice_overall_progress),
                    progress = progress,
                    modifier = Modifier.testTag("practice-progress"),
                )
            }

            item(key = "practice-open-prayer") {
                FilledTonalButton(
                    onClick = onOpenPrayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("practice-open-prayer"),
                ) {
                    Text(stringResource(R.string.feature_practice_open_prayer))
                }
            }

            practiceTopics(catalog, state).forEachIndexed { index, topic ->
                item(key = "practice-topic-$index-${topic.name}") {
                    ProgressSummary(
                        title = topic.name,
                        progress = topic.progress,
                        modifier = Modifier.testTag("practice-topic-$index"),
                    )
                }

                items(
                    items = topic.cards,
                    key = PracticeCard::id,
                ) { card ->
                    PracticeCardListItem(
                        card = card,
                        state = state,
                        onClick = { onCardSelected(card.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DevelopmentContentNotice(catalog: DemonstratorCatalog?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("practice-notice"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = catalog?.noticeTitle
                    ?: stringResource(R.string.feature_practice_no_catalog_notice_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            ContentText(
                text = catalog?.noticeBody
                    ?: stringResource(R.string.feature_practice_no_catalog_notice_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EmptyCatalog() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("practice-empty"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_practice_no_catalog_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.feature_practice_no_catalog_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ProgressSummary(
    title: String,
    progress: PracticeProgress,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContentText(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(
                R.string.feature_practice_step_progress,
                progress.completed,
                progress.total,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PracticeCardListItem(
    card: PracticeCard,
    state: ExperienceState,
    onClick: () -> Unit,
) {
    val progress = practiceCardProgress(card, state)
    val isSaved = card.id in state.savedPracticeCardIds

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("practice-card-${card.id}")
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContentText(
                text = card.title,
                style = MaterialTheme.typography.titleLarge,
            )
            ContentText(
                text = card.summary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.feature_practice_step_progress,
                    progress.completed,
                    progress.total,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(
                    R.string.feature_practice_conclusion_status_value,
                    card.conclusionStatus.label,
                    isolateLtr(card.conclusionStatus.name),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.feature_practice_review_state_value,
                    card.reviewState.label,
                    isolateLtr(card.reviewState.name),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (isSaved) {
                Text(
                    text = stringResource(R.string.feature_practice_saved_indicator),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(R.string.feature_practice_open_guide),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PracticeCardDetail(
    catalog: DemonstratorCatalog,
    card: PracticeCard,
    state: ExperienceState,
    occurrenceProgressEnabled: Boolean,
    occurrenceProgressActivating: Boolean,
    onBack: () -> Unit,
    onStepCompleted: (String, Boolean) -> Unit,
    onCardSaved: (String, Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    onOpenPrayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = practiceCardProgress(card, state)
    val isSaved = card.id in state.savedPracticeCardIds
    val sourcesById = catalog.sources.associateBy(SourceUnit::id)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("practice-detail"),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 20.dp,
            end = 24.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "practice-back") {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("practice-back"),
            ) {
                Text(stringResource(R.string.feature_practice_back_to_guides))
            }
        }

        item(key = "practice-detail-notice") {
            DevelopmentContentNotice(catalog)
        }

        if (!occurrenceProgressEnabled) {
            item(key = "practice-detail-progress-unavailable") {
                OccurrenceProgressUnavailableNotice(occurrenceProgressActivating)
            }
        }

        item(key = "practice-detail-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentText(
                    text = card.topic,
                    style = MaterialTheme.typography.labelLarge,
                )
                ContentText(
                    text = card.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                ProgressSummary(
                    title = stringResource(R.string.feature_practice_card_progress),
                    progress = progress,
                    modifier = Modifier.testTag("practice-card-progress"),
                )
            }
        }

        item(key = "practice-summary") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_summary),
                value = card.summary,
            )
        }
        item(key = "practice-action") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_action),
                value = card.action,
            )
        }
        item(key = "practice-context") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_context),
                value = card.context,
            )
        }
        item(key = "practice-supplies") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_supplies),
                value = card.supplies,
            )
        }

        item(key = "practice-steps-heading") {
            SectionHeading(stringResource(R.string.feature_practice_field_steps))
        }
        if (card.steps.isEmpty()) {
            item(key = "practice-no-steps") {
                Text(
                    text = stringResource(R.string.feature_practice_no_steps),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            items(
                items = card.steps,
                key = PracticeStep::id,
            ) { step ->
                PracticeStepToggle(
                    step = step,
                    completed = step.id in state.completedPracticeStepIds,
                    enabled = occurrenceProgressEnabled,
                    onCompletedChange = { onStepCompleted(step.id, it) },
                )
            }
        }

        item(key = "practice-different") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_different),
                value = card.circumstancesDiffer,
            )
        }
        item(key = "practice-purpose") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_purpose),
                value = card.purpose,
            )
        }
        item(key = "practice-conclusion-status") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_status),
                value = stringResource(
                    R.string.feature_practice_status_value,
                    card.conclusionStatus.label,
                    isolateLtr(card.conclusionStatus.name),
                ),
            )
        }
        item(key = "practice-review-state") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_review_state),
                value = stringResource(
                    R.string.feature_practice_status_value,
                    card.reviewState.label,
                    isolateLtr(card.reviewState.name),
                ),
            )
        }
        item(key = "practice-authorities") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_authorities),
                value = stringResource(R.string.feature_practice_authorities_not_recorded),
            )
        }
        item(key = "practice-reasoning") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_reasoning),
                value = card.reasoning,
            )
        }
        item(key = "practice-other-readings") {
            DetailField(
                title = stringResource(R.string.feature_practice_field_other_readings),
                value = card.otherReadings,
            )
        }
        item(key = "practice-confidence") {
            DetailSection(
                title = stringResource(R.string.feature_practice_field_confidence_review),
            ) {
                ContentText(
                    text = card.confidence,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        R.string.feature_practice_review_due,
                        card.reviewDue,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item(key = "practice-sources") {
            DetailSection(
                title = stringResource(R.string.feature_practice_field_sources),
            ) {
                if (card.primarySourceIds.isEmpty()) {
                    Text(
                        text = stringResource(R.string.feature_practice_no_sources),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    card.primarySourceIds.forEach { sourceId ->
                        ExactSourceButton(
                            sourceId = sourceId,
                            source = sourcesById[sourceId],
                            onOpenSource = onOpenSource,
                        )
                    }
                }
            }
        }

        item(key = "practice-save") {
            DetailSection(
                title = stringResource(R.string.feature_practice_field_save),
            ) {
                Text(
                    text = stringResource(R.string.feature_practice_save_scope),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val savedState = stringResource(R.string.feature_practice_saved_state)
                val notSavedState = stringResource(R.string.feature_practice_not_saved_state)
                FilledTonalButton(
                    onClick = { onCardSaved(card.id, !isSaved) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("practice-save-${card.id}")
                        .semantics {
                            selected = isSaved
                            stateDescription = if (isSaved) savedState else notSavedState
                        },
                ) {
                    Text(
                        if (isSaved) {
                            stringResource(R.string.feature_practice_remove_saved)
                        } else {
                            stringResource(R.string.feature_practice_save)
                        },
                    )
                }
            }
        }

        item(key = "practice-export") {
            DetailSection(
                title = stringResource(R.string.feature_practice_field_export),
            ) {
                Text(
                    text = stringResource(R.string.feature_practice_export_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(
                    onClick = onOpenPrayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("practice-export-via-prayer"),
                ) {
                    Text(stringResource(R.string.feature_practice_open_prayer))
                }
            }
        }
    }
}

@Composable
private fun PracticeStepToggle(
    step: PracticeStep,
    completed: Boolean,
    enabled: Boolean,
    onCompletedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("practice-step-${step.id}")
                .semantics(mergeDescendants = true) {}
                .toggleable(
                    value = completed,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCompletedChange,
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ContentText(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                ContentText(
                    text = step.detail,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun OccurrenceProgressUnavailableNotice(activating: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("practice-progress-unavailable"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    if (activating) {
                        R.string.feature_practice_progress_activating_title
                    } else {
                        R.string.feature_practice_progress_unavailable_title
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    if (activating) {
                        R.string.feature_practice_progress_activating_body
                    } else {
                        R.string.feature_practice_progress_unavailable_body
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ExactSourceButton(
    sourceId: String,
    source: SourceUnit?,
    onOpenSource: (String) -> Unit,
) {
    OutlinedButton(
        onClick = { onOpenSource(sourceId) },
        enabled = source != null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("practice-source-$sourceId"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ContentText(
                text = source?.title
                    ?: stringResource(R.string.feature_practice_source_not_installed),
                style = MaterialTheme.typography.labelLarge,
            )
            if (source != null) {
                ContentText(
                    text = source.locator,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(
                    R.string.feature_practice_source_id,
                    isolateLtr(sourceId),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DetailField(
    title: String,
    value: String,
) {
    DetailSection(title = title) {
        ContentText(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeading(title)
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ContentText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val textDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Rtl -> TextDirection.ContentOrRtl
        LayoutDirection.Ltr -> TextDirection.ContentOrLtr
    }
    Text(
        text = text,
        style = style.merge(TextStyle(textDirection = textDirection)),
        modifier = modifier,
    )
}

private fun isolateLtr(value: String): String = "\u2066$value\u2069"

@Preview(
    name = "English phone, 200 percent",
    locale = "en",
    fontScale = 2f,
    widthDp = 360,
    heightDp = 640,
    showBackground = true,
)
@Composable
private fun PracticeEnglishPreview() {
    MaterialTheme {
        PracticeScreen()
    }
}

@Preview(
    name = "Hebrew RTL tablet fallback",
    locale = "he",
    widthDp = 800,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun PracticeHebrewPreview() {
    MaterialTheme {
        PracticeScreen()
    }
}
