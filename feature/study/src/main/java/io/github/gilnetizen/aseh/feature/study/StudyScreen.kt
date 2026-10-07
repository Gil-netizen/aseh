package io.github.gilnetizen.aseh.feature.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.SourceUnit

@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    catalog: DemonstratorCatalog? = null,
    state: ExperienceState = ExperienceState(),
    requestedSourceId: String? = null,
    onRequestedSourceConsumed: () -> Unit = {},
    onBookmarkChanged: (String, Boolean) -> Unit = { _, _ -> },
) {
    var selectedSourceId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(requestedSourceId, catalog) {
        if (requestedSourceId != null && catalog?.sources?.any { it.id == requestedSourceId } == true) {
            selectedSourceId = requestedSourceId
            onRequestedSourceConsumed()
        }
    }

    val selectedSource = catalog?.sources?.firstOrNull { it.id == selectedSourceId }
    BackHandler(enabled = selectedSource != null) { selectedSourceId = null }
    if (selectedSource != null) {
        SourceDetail(
            source = selectedSource,
            catalog = catalog,
            bookmarked = selectedSource.id in state.bookmarkedSourceIds,
            onBookmarkChanged = { bookmarked ->
                onBookmarkChanged(selectedSource.id, bookmarked)
            },
            onBack = { selectedSourceId = null },
            modifier = modifier,
        )
    } else {
        SourceLibrary(
            catalog = catalog,
            state = state,
            onOpenSource = { selectedSourceId = it },
            onBookmarkChanged = onBookmarkChanged,
            modifier = modifier,
        )
    }
}

@Composable
private fun SourceLibrary(
    catalog: DemonstratorCatalog?,
    state: ExperienceState,
    onOpenSource: (String) -> Unit,
    onBookmarkChanged: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var bookmarksOnly by rememberSaveable { mutableStateOf(false) }
    var question by rememberSaveable { mutableStateOf("") }
    var answeredQuestion by rememberSaveable { mutableStateOf<String?>(null) }
    val studyProvider = flavorStudyProvider()
    val applicationContext = LocalContext.current.applicationContext
    val developmentPackRuntime = remember(applicationContext) {
        flavorDevelopmentContentPackRuntime(applicationContext)
    }
    val normalizedQuery = query.trim().lowercase()
    val results = catalog?.sources.orEmpty().filter { source ->
        val matchesQuery = normalizedQuery.isEmpty() ||
            source.title.lowercase().contains(normalizedQuery) ||
            source.locator.lowercase().contains(normalizedQuery) ||
            source.body.lowercase().contains(normalizedQuery) ||
            source.id.lowercase().contains(normalizedQuery)
        val matchesBookmark = !bookmarksOnly || source.id in state.bookmarkedSourceIds
        matchesQuery && matchesBookmark
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Study",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .testTag("study-heading")
                .semantics { heading() },
        )
        EnglishFallbackNotice()
        Text(
            text = "Search the installed local source units and follow their links into practice and the service order.",
            style = MaterialTheme.typography.bodyLarge,
        )

        developmentPackRuntime?.let { runtime ->
            DevelopmentContentPackCard(runtime)
        }

        if (catalog == null) {
            NoticeCard(
                title = "No approved source pack installed",
                body = "Study will search local, versioned sources after a reviewed pack is installed. This build makes no network request and does not substitute an unverified edition.",
            )
            return@Column
        }

        NoticeCard(catalog.noticeTitle, catalog.noticeBody)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search local sources") },
            supportingText = { Text("Title, exact locator, source ID, or words in the text") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("study-search"),
        )
        FilterChip(
            selected = bookmarksOnly,
            onClick = { bookmarksOnly = !bookmarksOnly },
            label = { Text("Bookmarked only (${state.bookmarkedSourceIds.size})") },
            modifier = Modifier.heightIn(min = 48.dp),
        )

        studyProvider?.let { provider ->
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Ask ASEH",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(provider.identity.displayName, fontWeight = FontWeight.Bold)
                            Text(provider.identity.disclosure)
                        }
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = {
                            question = it
                            answeredQuestion = null
                        },
                        label = { Text("Question") },
                        supportingText = { Text("Use concrete words that may appear in a source") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("study-ask-question"),
                    )
                    OutlinedButton(
                        onClick = {
                            question = "Who owns each active role before rehearsal?"
                            answeredQuestion = null
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Try an exact-source question")
                    }
                    Button(
                        onClick = {
                            answeredQuestion = question.trim()
                        },
                        enabled = question.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("study-ask-submit"),
                    ) {
                        Text("Answer from installed sources")
                    }
                    answeredQuestion?.let { submittedQuestion ->
                        StructuredAnswer(
                            answer = answerFromInstalledSources(
                                question = submittedQuestion,
                                sources = catalog.sources,
                                provider = provider,
                            ),
                            sources = catalog.sources,
                            onOpenSource = onOpenSource,
                        )
                    }
                }
            }
        }

        Text(
            text = "${results.size} ${if (results.size == 1) "source" else "sources"}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (results.isEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("No local source matches", fontWeight = FontWeight.SemiBold)
                    Text("Try a title, locator, source ID, or clear the bookmark filter.")
                    OutlinedButton(
                        onClick = {
                            query = ""
                            bookmarksOnly = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Clear filters")
                    }
                }
            }
        } else {
            results.forEach { source ->
                SourceResult(
                    source = source,
                    bookmarked = source.id in state.bookmarkedSourceIds,
                    onOpen = { onOpenSource(source.id) },
                    onBookmarkChanged = { bookmarked ->
                        onBookmarkChanged(source.id, bookmarked)
                    },
                )
            }
        }
    }
}

@Composable
private fun DevelopmentContentPackCard(runtime: DevelopmentContentPackRuntime) {
    var query by rememberSaveable { mutableStateOf("rehearsal") }
    var requestId by rememberSaveable { mutableStateOf(0) }
    var submittedQuery by rememberSaveable { mutableStateOf("rehearsal") }
    var selectedHitId by rememberSaveable { mutableStateOf<String?>(null) }
    var state by remember(runtime) {
        mutableStateOf<DevelopmentContentPackUiState>(DevelopmentContentPackUiState.Loading)
    }

    LaunchedEffect(runtime, requestId) {
        if (state !is DevelopmentContentPackUiState.Ready) {
            state = DevelopmentContentPackUiState.Loading
        }
        state = runtime.load(submittedQuery)
    }

    val selectedHit = (state as? DevelopmentContentPackUiState.Ready)
        ?.hits
        ?.firstOrNull { hit -> hit.contentId == selectedHitId }
    BackHandler(enabled = selectedHit != null) { selectedHitId = null }

    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study-development-pack"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Signed offline content pack",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Development fixture only. Every record is invented software-test content. " +
                        "It contains no sacred, liturgical, or Torah text and is not religious instruction. " +
                        "Editorial state: Drafted. Rights review: not completed.",
                    modifier = Modifier.padding(12.dp),
                )
            }

            when (val current = state) {
                DevelopmentContentPackUiState.Loading -> Text(
                    text = "Verifying signature, activating the pack, and opening its local search index…",
                    modifier = Modifier.testTag("study-development-pack-loading"),
                )

                is DevelopmentContentPackUiState.Unsupported -> {
                    Text(
                        text = "Unsupported on this Android version",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("study-development-pack-unsupported")
                            .semantics { heading() },
                    )
                    Text(current.reason)
                    Text("Verification fails closed; ASEH does not bypass Ed25519 or expose unverified content.")
                }

                is DevelopmentContentPackUiState.Failed -> {
                    Text(
                        text = "Pack verification failed closed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("study-development-pack-failed")
                            .semantics { heading() },
                    )
                    Text(current.reason)
                }

                is DevelopmentContentPackUiState.Ready -> {
                    Text(
                        text = "Verified and active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .testTag("study-development-pack-ready")
                            .semantics { heading() },
                    )
                    Text("Pack ${current.packId} · version ${current.version}")
                    Text(
                        text = "Manifest ${current.manifestSha256.take(16)}… · signing key ${current.signingKeyId.take(16)}…",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Search runs against the verified pack's local SQLite FTS index. No network is used.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (selectedHit != null) {
                        DevelopmentPackSearchResultDetail(
                            hit = selectedHit,
                            packId = current.packId,
                            version = current.version,
                            manifestSha256 = current.manifestSha256,
                            signingKeyId = current.signingKeyId,
                            onBack = { selectedHitId = null },
                        )
                    } else {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search verified pack") },
                            supportingText = { Text("Try an English word, Hebrew word, or exact locator") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("study-development-pack-query"),
                        )
                        Button(
                            onClick = {
                                selectedHitId = null
                                submittedQuery = query.trim()
                                requestId += 1
                            },
                            enabled = query.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("study-development-pack-search"),
                        ) {
                            Text("Search offline pack")
                        }
                        listOf(
                            "rehearsal" to "Search sample: rehearsal",
                            "תפקידים" to "Search sample: \u2068תפקידים\u2069",
                            "fixture:en:3" to "Search sample: fixture:en:3",
                        ).forEach { (sample, label) ->
                            OutlinedButton(
                                onClick = {
                                    selectedHitId = null
                                    query = sample
                                    submittedQuery = sample
                                    requestId += 1
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(label)
                            }
                        }

                        Text(
                            text = if (current.hits.size == 1) "1 verified result" else "${current.hits.size} verified results",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        if (current.hits.isEmpty()) {
                            Text(
                                text = if (current.query.isBlank()) {
                                    "Enter a search term."
                                } else {
                                    "No record in this verified fixture matches “${current.query}”."
                                },
                                modifier = Modifier.testTag("study-development-pack-no-results"),
                            )
                        } else {
                            current.hits.forEach { hit ->
                                DevelopmentPackSearchResult(
                                    hit = hit,
                                    onOpen = { selectedHitId = hit.contentId },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevelopmentPackSearchResult(
    hit: DevelopmentContentSearchHit,
    onOpen: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                onClickLabel = "Open verified pack result",
                role = Role.Button,
                onClick = onOpen,
            )
            .testTag("study-development-pack-hit-${hit.contentId}"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(hit.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${hit.kind} · ${hit.language}", style = MaterialTheme.typography.labelLarge)
            Text(hit.snippet)
            Text(
                text = listOfNotNull(hit.contentId, hit.locator).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Open verified record",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DevelopmentPackSearchResultDetail(
    hit: DevelopmentContentSearchHit,
    packId: String,
    version: String,
    manifestSha256: String,
    signingKeyId: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study-development-pack-detail-${hit.contentId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("study-development-pack-detail-back"),
        ) {
            Text("Back to verified results")
        }
        Text(
            text = "Verified pack record",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = hit.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .testTag("study-development-pack-detail-heading")
                .semantics { heading() },
        )
        Text(
            text = "Content ID: ${hit.contentId}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag("study-development-pack-detail-id"),
        )
        Text("${hit.kind} · ${hit.language}")
        hit.locator?.let { locator -> Text("Locator: $locator") }
        Text(
            text = "Matching excerpt",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        SelectionContainer {
            Text(
                text = hit.snippet,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("study-development-pack-detail-text"),
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "This record was read from the signature-verified development fixture. " +
                    "It remains invented software-test content with Drafted editorial status, " +
                    "not sacred text or religious instruction.",
                modifier = Modifier.padding(12.dp),
            )
        }
        Text(
            text = "Pack $packId · version $version",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Manifest $manifestSha256 · signing key $signingKeyId",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("study-development-pack-detail-provenance"),
        )
    }
}

@Composable
private fun StructuredAnswer(
    answer: OfflineStudyAnswer,
    sources: List<SourceUnit>,
    onOpenSource: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study-structured-answer"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider()
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("study-provider-identity"),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = answer.providerIdentity.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(answer.providerIdentity.disclosure)
                Text(
                    text = "Mode: ${answer.responseMode.label}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("study-answer-mode"),
                )
            }
        }
        AnswerSection(
            heading = "1. Short answer",
            claims = listOf(answer.shortAnswer),
            sources = sources,
            onOpenSource = onOpenSource,
            testTag = if (answer.isEstablished) {
                "study-answer-finding"
            } else {
                "study-answer-not-established"
            },
        )

        AnswerHeading("2. What the sources explicitly establish")
        answer.exactEvidence.forEachIndexed { index, claim ->
            val match = answer.rankedMatches.getOrNull(index)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study-answer-match-${index + 1}"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (match != null) {
                    Text(
                        text = "Match ${index + 1} · ${match.source.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Exact terms: ${match.matchedTerms.joinToString()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                AnswerClaimCard(
                    claim = claim,
                    sources = sources,
                    onOpenSource = onOpenSource,
                )
            }
        }

        AnswerSection(
            heading = "3. How Rambam or relevant Geonim understand them",
            claims = answer.authorityInterpretation,
            sources = sources,
            onOpenSource = onOpenSource,
        )
        AnswerSection(
            heading = "4. What must be inferred",
            claims = answer.inference,
            sources = sources,
            onOpenSource = onOpenSource,
            testTag = "study-answer-inference",
        )
        AnswerSection(
            heading = "5. Material disagreements and uncertainty",
            claims = answer.disagreementAndUncertainty,
            sources = sources,
            onOpenSource = onOpenSource,
        )
        AnswerSection(
            heading = "6. Practical next steps",
            claims = answer.nextSteps,
            sources = sources,
            onOpenSource = onOpenSource,
        )

        AnswerHeading("7. Exact claim citations")
        if (answer.claimCitations.isEmpty()) {
            Text(
                text = "No claim citation is available because no exact source matched.",
                modifier = Modifier.testTag("study-answer-no-citations"),
            )
        } else {
            answer.claimCitations.forEach { citation ->
                ClaimCitationCard(
                    citation = citation,
                    source = sources.firstOrNull { it.id == citation.sourceId },
                    onOpenSource = onOpenSource,
                )
            }
        }

        AnswerHeading("8. Confidence and limitations")
        Text(
            text = "Confidence is reported by dimension; ASEH does not combine it into a single score.",
            style = MaterialTheme.typography.bodyMedium,
        )
        answer.confidence.dimensions.forEach { dimension ->
            ConfidenceDimensionCard(dimension)
        }
        Text(
            text = "Limitations",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        answer.limitations.forEach { limitation ->
            AnswerClaimCard(
                claim = limitation,
                sources = sources,
                onOpenSource = onOpenSource,
            )
        }

        Text(
            text = "No AI model or network was used for this answer.",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("study-answer-local-only"),
        )
    }
}

@Composable
private fun AnswerSection(
    heading: String,
    claims: List<AnswerClaim>,
    sources: List<SourceUnit>,
    onOpenSource: (String) -> Unit,
    testTag: String? = null,
) {
    AnswerHeading(heading)
    claims.forEachIndexed { index, claim ->
        AnswerClaimCard(
            claim = claim,
            sources = sources,
            onOpenSource = onOpenSource,
            testTag = testTag?.takeIf { index == 0 },
        )
    }
}

@Composable
private fun AnswerHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun AnswerClaimCard(
    claim: AnswerClaim,
    sources: List<SourceUnit>,
    onOpenSource: (String) -> Unit,
    testTag: String? = null,
) {
    val statementModifier = if (testTag == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .testTag(testTag)
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (claim.basis == StudyClaimBasis.NOT_ESTABLISHED) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (claim.basis == StudyClaimBasis.NOT_ESTABLISHED) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = statementModifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Claim ${claim.id} · ${claim.basis.label}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Verification: ${claim.verificationStatus.label}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(claim.text, style = MaterialTheme.typography.bodyLarge)
            claim.sourceIds.forEach { sourceId ->
                val source = sources.firstOrNull { it.id == sourceId }
                OutlinedButton(
                    onClick = { onOpenSource(sourceId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("study-claim-${claim.id}-open-$sourceId"),
                ) {
                    Text(
                        text = if (source == null) {
                            "Open source $sourceId"
                        } else {
                            "Open ${source.id} · ${source.locator}"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaimCitationCard(
    citation: ClaimCitation,
    source: SourceUnit?,
    onOpenSource: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study-citation-${citation.claimId}-${citation.sourceId}"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Claim ${citation.claimId}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            if (source == null) {
                Text("Missing installed source ${citation.sourceId}")
            } else {
                Text("${source.id} · ${source.locator}", color = MaterialTheme.colorScheme.primary)
                Text(source.edition, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = { onOpenSource(citation.sourceId) },
                enabled = source != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("study-citation-open-${citation.claimId}-${citation.sourceId}"),
            ) {
                Text("Open exact source")
            }
        }
    }
}

@Composable
private fun ConfidenceDimensionCard(dimension: ConfidenceDimension) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study-confidence-${dimension.name.lowercase().replace(' ', '-')}"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(dimension.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(dimension.assessment, color = MaterialTheme.colorScheme.primary)
            Text(dimension.rationale)
            Text(
                text = "Could change with: ${dimension.evidenceThatCouldChangeIt}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SourceResult(
    source: SourceUnit,
    bookmarked: Boolean,
    onOpen: () -> Unit,
    onBookmarkChanged: (Boolean) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .testTag("study-source-${source.id}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(source.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(source.locator, color = MaterialTheme.colorScheme.primary)
            Text(source.body, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Conclusion: ${source.conclusionStatus.label} · Review: ${source.reviewState.label} · ${source.language}",
                style = MaterialTheme.typography.labelLarge,
            )
            OutlinedButton(
                onClick = { onBookmarkChanged(!bookmarked) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (bookmarked) "Remove bookmark" else "Bookmark")
            }
        }
    }
}

@Composable
private fun SourceDetail(
    source: SourceUnit,
    catalog: DemonstratorCatalog,
    bookmarked: Boolean,
    onBookmarkChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val relatedPractices = catalog.practiceCards.filter { it.id in source.relatedPracticeIds }
    val relatedSegments = catalog.service.segments.filter { it.id in source.relatedSegmentIds }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Back to source library")
        }
        Text(
            text = source.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .testTag("study-source-heading")
                .semantics { heading() },
        )
        EnglishFallbackNotice()
        NoticeCard(catalog.noticeTitle, catalog.noticeBody)

        SourceField("Exact locator", source.locator)
        SourceField("Stable source ID", source.id)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    text = source.body,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp),
                )
            }
        }
        Button(
            onClick = { onBookmarkChanged(!bookmarked) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(if (bookmarked) "Remove bookmark" else "Bookmark source")
        }

        HorizontalDivider()
        Text("Edition and provenance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SourceField("Edition", source.edition)
        SourceField("Language", source.language)
        SourceField("Provenance", source.provenance)
        SourceField("License", source.license)
        SourceField("Conclusion evidence status", source.conclusionStatus.label)
        SourceField("Review state", source.reviewState.label)

        HorizontalDivider()
        Text("Source-to-practice chain", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (relatedPractices.isEmpty() && relatedSegments.isEmpty()) {
            Text("No linked records in this pack.")
        } else {
            relatedPractices.forEach { card ->
                RelationshipRow(kind = "Practice", title = card.title, id = card.id)
            }
            relatedSegments.forEach { segment ->
                RelationshipRow(kind = "Service", title = segment.title, id = segment.id)
            }
        }
    }
}

@Composable
private fun EnglishFallbackNotice() {
    val deviceLanguage = LocalConfiguration.current.locales[0].language
    if (deviceLanguage != "he" && deviceLanguage != "iw") return

    val notice = stringResource(R.string.feature_study_english_fallback_notice)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study-english-fallback-notice")
            .semantics(mergeDescendants = true) {
                contentDescription = notice
            },
    ) {
        Text(
            text = notice,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun RelationshipRow(kind: String, title: String, id: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(kind, style = MaterialTheme.typography.labelLarge)
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(id, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SourceField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun StudyPreview() {
    StudyScreen()
}
