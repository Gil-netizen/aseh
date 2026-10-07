package io.github.gilnetizen.aseh.feature.study

import io.github.gilnetizen.aseh.core.model.SourceUnit

/** Why a visible answer claim may be shown when it is not a source quotation. */
internal enum class StudyClaimBasis(val label: String) {
    SOURCE_EXCERPT("Source excerpt"),
    RETRIEVAL_INFERENCE("Retrieval inference"),
    PRODUCT_GUIDANCE("Product guidance"),
    NOT_ESTABLISHED("Not established"),
    LIMITATION("Limitation"),
}

internal enum class ClaimVerificationStatus(val label: String) {
    EXACT_SOURCE_TEXT("Exact installed source text verified"),
    RETRIEVED_SOURCE_INFERENCE("Inference references verified retrieved sources"),
    EXPLICIT_NON_SOURCE_CLAIM("Explicit non-source claim"),
    REJECTED_UNSUPPORTED("Unsupported provider claim removed"),
}

/** An atomic visible claim after the verifier has checked it. */
internal data class AnswerClaim(
    val id: String,
    val text: String,
    val basis: StudyClaimBasis,
    val sourceIds: List<String> = emptyList(),
    val verificationStatus: ClaimVerificationStatus,
) {
    init {
        require(id.isNotBlank()) { "An answer claim requires a stable ID." }
        require(text.isNotBlank()) { "An answer claim requires visible text." }
        require(sourceIds.distinct().size == sourceIds.size) {
            "A claim must not repeat a source citation."
        }
        if (basis == StudyClaimBasis.SOURCE_EXCERPT) {
            require(sourceIds.size == 1) { "A verified source excerpt requires exactly one citation." }
            require(verificationStatus == ClaimVerificationStatus.EXACT_SOURCE_TEXT) {
                "A source excerpt must pass exact-source verification."
            }
        }
        if (verificationStatus == ClaimVerificationStatus.REJECTED_UNSUPPORTED) {
            require(basis == StudyClaimBasis.NOT_ESTABLISHED && sourceIds.isEmpty()) {
                "Rejected claims must be visibly unsupported and carry no citation."
            }
        }
    }
}

/** A claim emitted by a provider before any provider text is trusted. */
internal data class ProviderClaim(
    val id: String,
    val text: String,
    val basis: StudyClaimBasis,
    val sourceIds: List<String> = emptyList(),
)

internal data class ClaimCitation(
    val claimId: String,
    val sourceId: String,
)

internal data class RankedSourceMatch(
    val source: SourceUnit,
    val score: Int,
    val matchedTerms: List<String>,
)

internal data class RetrievalResult(
    val queryTerms: List<String>,
    val matches: List<RankedSourceMatch>,
)

internal data class StudyProviderIdentity(
    val id: String,
    val displayName: String,
    val disclosure: String,
    val isDevelopmentFake: Boolean,
)

internal enum class StudyResponseMode(val label: String) {
    ORDINARY_ANSWER("Ordinary source lookup"),
    CASE_PREPARATION("High-consequence case preparation"),
}

/**
 * A qualitative dimension from the METHOD confidence contract.
 *
 * Confidence deliberately has no numeric or overall score.
 */
internal data class ConfidenceDimension(
    val name: String,
    val assessment: String,
    val rationale: String,
    val evidenceThatCouldChangeIt: String,
)

internal data class AnswerConfidence(
    val dimensions: List<ConfidenceDimension>,
)

/** The provider's eight-section draft. It cannot reach the UI until verified. */
internal data class ProviderAnswerDraft(
    val mode: StudyResponseMode,
    val shortAnswer: ProviderClaim,
    val exactEvidence: List<ProviderClaim>,
    val authorityInterpretation: List<ProviderClaim>,
    val inference: List<ProviderClaim>,
    val disagreementAndUncertainty: List<ProviderClaim>,
    val nextSteps: List<ProviderClaim>,
    val confidence: AnswerConfidence,
    val limitations: List<ProviderClaim>,
)

internal data class ProviderRequest(
    val question: String,
    val mode: StudyResponseMode,
    val retrieval: RetrievalResult,
)

internal interface InstalledCorpusRetriever {
    fun retrieve(question: String, sources: List<SourceUnit>): RetrievalResult
}

internal interface AskAsehProvider {
    val identity: StudyProviderIdentity

    fun createDraft(request: ProviderRequest): ProviderAnswerDraft
}

internal interface StudyClaimVerifier {
    fun verify(claim: ProviderClaim, retrievedSources: Map<String, SourceUnit>): AnswerClaim
}

/** The complete eight-part answer contract used by the offline demonstrator. */
internal data class OfflineStudyAnswer(
    val responseMode: StudyResponseMode,
    val providerIdentity: StudyProviderIdentity,
    val shortAnswer: AnswerClaim,
    val exactEvidence: List<AnswerClaim>,
    val authorityInterpretation: List<AnswerClaim>,
    val inference: List<AnswerClaim>,
    val disagreementAndUncertainty: List<AnswerClaim>,
    val nextSteps: List<AnswerClaim>,
    val claimCitations: List<ClaimCitation>,
    val confidence: AnswerConfidence,
    val limitations: List<AnswerClaim>,
    val rankedMatches: List<RankedSourceMatch>,
) {
    val isEstablished: Boolean
        get() = responseMode == StudyResponseMode.ORDINARY_ANSWER &&
            shortAnswer.basis == StudyClaimBasis.SOURCE_EXCERPT &&
            shortAnswer.verificationStatus == ClaimVerificationStatus.EXACT_SOURCE_TEXT

    val allClaims: List<AnswerClaim>
        get() = listOf(shortAnswer) +
            exactEvidence +
            authorityInterpretation +
            inference +
            disagreementAndUncertainty +
            nextSteps +
            limitations
}

internal class OfflineAskAsehBoundary(
    private val retriever: InstalledCorpusRetriever = ExactTokenInstalledCorpusRetriever(),
    private val provider: AskAsehProvider,
    private val verifier: StudyClaimVerifier = ExactInstalledSourceClaimVerifier(),
) {
    fun answer(question: String, sources: List<SourceUnit>): OfflineStudyAnswer {
        val retrieval = retriever.retrieve(question, sources)
        val mode = classifyResponseMode(question)
        val draft = provider.createDraft(
            ProviderRequest(
                question = question,
                mode = mode,
                retrieval = retrieval,
            ),
        )
        val retrievedSources = retrieval.matches
            .map(RankedSourceMatch::source)
            .associateBy(SourceUnit::id)

        fun verifyAll(claims: List<ProviderClaim>): List<AnswerClaim> =
            claims.map { claim -> verifier.verify(claim, retrievedSources) }

        val answerWithoutCitations = OfflineStudyAnswer(
            responseMode = mode,
            providerIdentity = provider.identity,
            shortAnswer = verifier.verify(draft.shortAnswer, retrievedSources),
            exactEvidence = verifyAll(draft.exactEvidence),
            authorityInterpretation = verifyAll(draft.authorityInterpretation),
            inference = verifyAll(draft.inference),
            disagreementAndUncertainty = verifyAll(draft.disagreementAndUncertainty),
            nextSteps = verifyAll(draft.nextSteps),
            claimCitations = emptyList(),
            confidence = draft.confidence,
            limitations = verifyAll(draft.limitations),
            rankedMatches = retrieval.matches,
        )
        return answerWithoutCitations.copy(
            claimCitations = citationsFor(answerWithoutCitations.allClaims),
        )
    }
}

/** Compatibility entry point retained for the current Study UI and tests. */
internal fun answerFromInstalledSources(
    question: String,
    sources: List<SourceUnit>,
    provider: AskAsehProvider,
): OfflineStudyAnswer = OfflineAskAsehBoundary(provider = provider).answer(question, sources)

/** Retained for callers that only need the strongest local match. */
internal fun findBestSource(question: String, sources: List<SourceUnit>): SourceUnit? =
    ExactTokenInstalledCorpusRetriever()
        .retrieve(question, sources)
        .matches
        .firstOrNull()
        ?.source

private fun citationsFor(claims: List<AnswerClaim>): List<ClaimCitation> = claims
    .flatMap { claim ->
        claim.sourceIds.map { sourceId ->
            ClaimCitation(claimId = claim.id, sourceId = sourceId)
        }
    }
    .distinct()

private fun classifyResponseMode(question: String): StudyResponseMode {
    val normalized = question.lowercase()
    return if (HIGH_CONSEQUENCE_PATTERNS.any { pattern -> pattern.containsMatchIn(normalized) }) {
        StudyResponseMode.CASE_PREPARATION
    } else {
        StudyResponseMode.ORDINARY_ANSWER
    }
}

private val HIGH_CONSEQUENCE_PATTERNS = listOf(
    Regex("\\b(?:marriage|marry|wedding|divorce|annulment)\\b"),
    Regex("\\b(?:conversion|convert|converted)\\b"),
    Regex("\\b(?:vow|vows|neder|nedarim)\\b"),
    Regex("\\b(?:medical|medicine|diagnosis|treatment|fertility|pregnancy|pregnant)\\b"),
    Regex("\\b(?:abuse|abusive|coercion|coerced|immediate danger|suicide|self-harm)\\b"),
    Regex("\\b(?:death|dying|burial|funeral)\\b"),
    Regex("\\b(?:public accusation|defamation|accuse|accusation)\\b"),
    Regex("\\b(?:complex monetary dispute|lawsuit|litigation)\\b"),
)
