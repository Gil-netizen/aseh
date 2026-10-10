package io.github.gilnetizen.aseh.feature.study

internal fun flavorStudyProvider(): AskAsehProvider? = DeterministicInstalledCorpusFakeProvider()

/**
 * A deterministic development provider over already-retrieved installed records.
 *
 * It has no network, key, tool, instruction-following, or generative model capability. Source
 * bodies remain untrusted data and can only be emitted as exact, verifier-checked excerpts.
 */
internal class DeterministicInstalledCorpusFakeProvider : AskAsehProvider {
    override val identity = StudyProviderIdentity(
        id = "dev.fake.installed-corpus.v1",
        displayName = "Development fake provider",
        disclosure = "Deterministic installed-corpus fixture; no model, network, or provider key.",
        isDevelopmentFake = true,
    )

    override fun createDraft(request: ProviderRequest): ProviderAnswerDraft = when (request.mode) {
        StudyResponseMode.ORDINARY_ANSWER -> ordinaryDraft(request.retrieval)
        StudyResponseMode.CASE_PREPARATION -> casePreparationDraft(request.retrieval)
    }

    private fun ordinaryDraft(retrieval: RetrievalResult): ProviderAnswerDraft {
        if (retrieval.matches.isEmpty()) return notEstablishedDraft(retrieval.queryTerms)

        val sourceIds = retrieval.matches.map { match -> match.source.id }
        val strongest = retrieval.matches.first().source
        val overlap = retrieval.matches.flatMap { match -> match.matchedTerms }.distinct()
        return ProviderAnswerDraft(
            mode = StudyResponseMode.ORDINARY_ANSWER,
            shortAnswer = ProviderClaim(
                id = "short-answer",
                text = strongest.body,
                basis = StudyClaimBasis.SOURCE_EXCERPT,
                sourceIds = listOf(strongest.id),
            ),
            exactEvidence = exactEvidenceClaims(retrieval.matches),
            authorityInterpretation = listOf(
                ProviderClaim(
                    id = "authority-1",
                    text = "No separately structured Rambam, Geonic, or other named-authority " +
                        "position is established by these installed records.",
                    basis = StudyClaimBasis.NOT_ESTABLISHED,
                ),
            ),
            inference = listOf(
                ProviderClaim(
                    id = "inference-1",
                    text = "These records were ranked by exact overlap with the question terms " +
                        overlap.joinToString(
                            prefix = "\u201c",
                            postfix = "\u201d",
                            separator = "\u201d, \u201c",
                        ) + ". Relevance and any connection between records are retrieval inferences.",
                    basis = StudyClaimBasis.RETRIEVAL_INFERENCE,
                    sourceIds = sourceIds,
                ),
            ),
            disagreementAndUncertainty = listOf(
                ProviderClaim(
                    id = "uncertainty-1",
                    text = "No structured disagreement record was retrieved. That does not establish " +
                        "consensus, and exact-term retrieval may miss records that use different words.",
                    basis = StudyClaimBasis.LIMITATION,
                ),
            ),
            nextSteps = listOf(
                ProviderClaim(
                    id = "next-step-1",
                    text = "Open every cited record and inspect its exact locator, edition, provenance, " +
                        "license, and review state before relying on it.",
                    basis = StudyClaimBasis.PRODUCT_GUIDANCE,
                ),
                ProviderClaim(
                    id = "next-step-2",
                    text = "If the evidence does not answer the question, ask a narrower question using " +
                        "terms from the installed records.",
                    basis = StudyClaimBasis.PRODUCT_GUIDANCE,
                ),
            ),
            confidence = confidenceFor(retrieval.queryTerms, retrieval.matches),
            limitations = commonLimitations(retrieval.matches),
        )
    }

    private fun casePreparationDraft(retrieval: RetrievalResult): ProviderAnswerDraft {
        val exactEvidence = exactEvidenceClaims(retrieval.matches).ifEmpty {
            listOf(
                ProviderClaim(
                    id = "evidence-1",
                    text = "No installed source matched exact searchable terms from this question.",
                    basis = StudyClaimBasis.NOT_ESTABLISHED,
                ),
            )
        }
        return ProviderAnswerDraft(
            mode = StudyResponseMode.CASE_PREPARATION,
            shortAnswer = ProviderClaim(
                id = "short-answer",
                text = "This is a high-consequence question. ASEH can prepare a private source " +
                    "packet, but this development provider does not give an operative answer.",
                basis = StudyClaimBasis.PRODUCT_GUIDANCE,
            ),
            exactEvidence = exactEvidence,
            authorityInterpretation = listOf(
                ProviderClaim(
                    id = "authority-1",
                    text = "No case-specific position of Rambam, the Geonim, a court, clinician, or " +
                        "other qualified reviewer is established by this lookup.",
                    basis = StudyClaimBasis.NOT_ESTABLISHED,
                ),
            ),
            inference = listOf(
                ProviderClaim(
                    id = "inference-1",
                    text = "The question was routed here because its wording names a " +
                        "high-consequence domain. No live case facts were evaluated.",
                    basis = StudyClaimBasis.LIMITATION,
                ),
            ),
            disagreementAndUncertainty = listOf(
                ProviderClaim(
                    id = "uncertainty-1",
                    text = "Material facts, disagreement, recognition requirements, and required " +
                        "human roles remain unestablished.",
                    basis = StudyClaimBasis.NOT_ESTABLISHED,
                ),
            ),
            nextSteps = listOf(
                ProviderClaim(
                    id = "next-step-1",
                    text = "Create a private case-preparation packet containing only the question, " +
                        "facts, documents, and source records you deliberately choose to include.",
                    basis = StudyClaimBasis.PRODUCT_GUIDANCE,
                ),
                ProviderClaim(
                    id = "next-step-2",
                    text = "Use that packet with the qualified human participants required for the " +
                        "specific matter before taking an operative step.",
                    basis = StudyClaimBasis.PRODUCT_GUIDANCE,
                ),
            ),
            confidence = confidenceFor(retrieval.queryTerms, retrieval.matches),
            limitations = listOf(
                ProviderClaim(
                    id = "limitation-1",
                    text = "Case-preparation mode does not determine facts, issue a ruling, create an " +
                        "operative document, or claim outside recognition.",
                    basis = StudyClaimBasis.LIMITATION,
                ),
            ) + commonLimitations(retrieval.matches),
        )
    }
}

private fun exactEvidenceClaims(matches: List<RankedSourceMatch>): List<ProviderClaim> =
    matches.mapIndexed { index, match ->
        ProviderClaim(
            id = "evidence-${index + 1}",
            text = match.source.body,
            basis = StudyClaimBasis.SOURCE_EXCERPT,
            sourceIds = listOf(match.source.id),
        )
    }

private fun commonLimitations(matches: List<RankedSourceMatch>): List<ProviderClaim> = buildList {
    add(
        ProviderClaim(
            id = "limitation-exact-retrieval",
            text = "This answer uses exact-token retrieval only; it does not resolve synonyms, " +
                "Hebrew morphology, textual variants, or unstated implications.",
            basis = StudyClaimBasis.LIMITATION,
        ),
    )
    if (matches.isNotEmpty()) {
        add(
            ProviderClaim(
                id = "limitation-editorial-states",
                text = editorialStateLimitation(matches),
                basis = StudyClaimBasis.RETRIEVAL_INFERENCE,
                sourceIds = matches.map { match -> match.source.id },
            ),
        )
    }
}

private fun notEstablishedDraft(queryTerms: List<String>) = ProviderAnswerDraft(
    mode = StudyResponseMode.ORDINARY_ANSWER,
    shortAnswer = ProviderClaim(
        id = "short-answer",
        text = "Not established in the installed sources.",
        basis = StudyClaimBasis.NOT_ESTABLISHED,
    ),
    exactEvidence = listOf(
        ProviderClaim(
            id = "evidence-1",
            text = "No installed source contains an exact searchable term from this question.",
            basis = StudyClaimBasis.NOT_ESTABLISHED,
        ),
    ),
    authorityInterpretation = listOf(
        ProviderClaim(
            id = "authority-1",
            text = "No named-authority interpretation is established without retrieved evidence.",
            basis = StudyClaimBasis.NOT_ESTABLISHED,
        ),
    ),
    inference = listOf(
        ProviderClaim(
            id = "inference-1",
            text = "No interpretive inference is offered without a matching exact source.",
            basis = StudyClaimBasis.NOT_ESTABLISHED,
        ),
    ),
    disagreementAndUncertainty = listOf(
        ProviderClaim(
            id = "uncertainty-1",
            text = "The absence of a keyword match does not establish agreement or prove that " +
                "relevant evidence is absent.",
            basis = StudyClaimBasis.LIMITATION,
        ),
    ),
    nextSteps = listOf(
        ProviderClaim(
            id = "next-step-1",
            text = "Try a narrower question using a title, locator, source ID, or words expected " +
                "in the source text.",
            basis = StudyClaimBasis.PRODUCT_GUIDANCE,
        ),
    ),
    confidence = notEstablishedConfidence(queryTerms),
    limitations = listOf(
        ProviderClaim(
            id = "limitation-1",
            text = "This lookup uses exact tokens only and can miss synonyms, inflected forms, " +
                "other languages, textual variants, and sources not installed on this device.",
            basis = StudyClaimBasis.LIMITATION,
        ),
    ),
)

private fun confidenceFor(
    queryTerms: List<String>,
    matches: List<RankedSourceMatch>,
): AnswerConfidence {
    if (matches.isEmpty()) return notEstablishedConfidence(queryTerms)

    val matchedTerms = matches.flatMap { match -> match.matchedTerms }.distinct()
    val conclusionStatuses = matches.map { match -> match.source.conclusionStatus.label }.distinct().sorted()
    val reviewStates = matches.map { match -> match.source.reviewState.label }.distinct().sorted()
    return AnswerConfidence(
        dimensions = listOf(
            ConfidenceDimension(
                name = "Textual certainty",
                assessment = "Exact excerpts available",
                rationale = "The answer displays verifier-checked text from ${matches.size} installed " +
                    if (matches.size == 1) "record." else "records.",
                evidenceThatCouldChangeIt = "A corrected edition or changed installed source record.",
            ),
            ConfidenceDimension(
                name = "Attribution certainty",
                assessment = "Not assessed",
                rationale = "The installed record shape does not identify a separate named-authority position.",
                evidenceThatCouldChangeIt = "Structured authority, work, passage, and attribution metadata.",
            ),
            ConfidenceDimension(
                name = "Interpretive certainty",
                assessment = "Limited",
                rationale = "Retrieval matched ${matchedTerms.size} of ${queryTerms.size} searchable " +
                    "question terms and performs no interpretive synthesis.",
                evidenceThatCouldChangeIt = "Reviewed interpretation and disagreement records tied to these excerpts.",
            ),
            ConfidenceDimension(
                name = "Factual fit",
                assessment = "Not assessed",
                rationale = "No present-case facts were evaluated by this offline source lookup.",
                evidenceThatCouldChangeIt = "A bounded question with dated, relevant case facts.",
            ),
            ConfidenceDimension(
                name = "Breadth of editorial review",
                assessment = "Limited to installed record states",
                rationale = "Matched conclusion evidence statuses: ${conclusionStatuses.joinToString()}; " +
                    "review states: ${reviewStates.joinToString()}.",
                evidenceThatCouldChangeIt = "A newly reviewed or approved installed content pack.",
            ),
        ),
    )
}

private fun notEstablishedConfidence(queryTerms: List<String>) = AnswerConfidence(
    dimensions = listOf(
        ConfidenceDimension(
            name = "Textual certainty",
            assessment = "Not established",
            rationale = "No exact source excerpt was retrieved.",
            evidenceThatCouldChangeIt = "An installed source matching the bounded question.",
        ),
        ConfidenceDimension(
            name = "Attribution certainty",
            assessment = "Not assessed",
            rationale = "No authority record was retrieved.",
            evidenceThatCouldChangeIt = "A cited, structured authority position.",
        ),
        ConfidenceDimension(
            name = "Interpretive certainty",
            assessment = "Not established",
            rationale = "${queryTerms.size} searchable question terms produced no exact match.",
            evidenceThatCouldChangeIt = "Matching evidence and a reviewed interpretation record.",
        ),
        ConfidenceDimension(
            name = "Factual fit",
            assessment = "Not assessed",
            rationale = "No present-case facts were evaluated.",
            evidenceThatCouldChangeIt = "A bounded question with dated, relevant case facts.",
        ),
        ConfidenceDimension(
            name = "Breadth of editorial review",
            assessment = "Not assessed",
            rationale = "There are no matched review records to assess.",
            evidenceThatCouldChangeIt = "A matching reviewed or approved installed content record.",
        ),
    ),
)

private fun editorialStateLimitation(matches: List<RankedSourceMatch>): String {
    val conclusionStatuses = matches.map { match -> match.source.conclusionStatus.label }.distinct().sorted()
    val reviewStates = matches.map { match -> match.source.reviewState.label }.distinct().sorted()
    return "Matched records carry these conclusion evidence statuses: ${conclusionStatuses.joinToString()}; " +
        "editorial review states: ${reviewStates.joinToString()}. A match does not upgrade either state " +
        "or make a record an operative ruling."
}
