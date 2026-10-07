package io.github.gilnetizen.aseh.feature.study

import io.github.gilnetizen.aseh.core.model.SourceUnit

internal class ExactTokenInstalledCorpusRetriever : InstalledCorpusRetriever {
    override fun retrieve(question: String, sources: List<SourceUnit>): RetrievalResult {
        val queryTerms = searchableTerms(question)
        val matches = if (queryTerms.isEmpty()) {
            emptyList()
        } else {
            sources
                .mapNotNull { source -> rankSource(source, queryTerms) }
                .sortedWith(
                    compareByDescending<RankedSourceMatch> { match -> match.score }
                        .thenByDescending { match -> match.matchedTerms.size }
                        .thenBy { match -> match.source.id },
                )
                .take(MAX_ANSWER_MATCHES)
        }
        return RetrievalResult(
            queryTerms = queryTerms,
            matches = matches,
        )
    }
}

internal class ExactInstalledSourceClaimVerifier : StudyClaimVerifier {
    override fun verify(
        claim: ProviderClaim,
        retrievedSources: Map<String, SourceUnit>,
    ): AnswerClaim {
        val hasUniqueSourceIds = claim.sourceIds.distinct().size == claim.sourceIds.size
        val citedSources = claim.sourceIds.mapNotNull(retrievedSources::get)
        val allCitationsRetrieved = hasUniqueSourceIds && citedSources.size == claim.sourceIds.size

        val verificationStatus = when (claim.basis) {
            StudyClaimBasis.SOURCE_EXCERPT -> {
                if (
                    claim.sourceIds.size == 1 &&
                    allCitationsRetrieved &&
                    citedSources.single().body == claim.text
                ) {
                    ClaimVerificationStatus.EXACT_SOURCE_TEXT
                } else {
                    null
                }
            }
            StudyClaimBasis.RETRIEVAL_INFERENCE -> {
                if (claim.sourceIds.isNotEmpty() && allCitationsRetrieved) {
                    ClaimVerificationStatus.RETRIEVED_SOURCE_INFERENCE
                } else {
                    null
                }
            }
            StudyClaimBasis.PRODUCT_GUIDANCE,
            StudyClaimBasis.NOT_ESTABLISHED,
            StudyClaimBasis.LIMITATION,
            -> {
                if (claim.sourceIds.isEmpty()) {
                    ClaimVerificationStatus.EXPLICIT_NON_SOURCE_CLAIM
                } else {
                    null
                }
            }
        }

        return if (
            claim.id.isNotBlank() &&
            claim.text.isNotBlank() &&
            verificationStatus != null
        ) {
            AnswerClaim(
                id = claim.id,
                text = claim.text,
                basis = claim.basis,
                sourceIds = claim.sourceIds,
                verificationStatus = verificationStatus,
            )
        } else {
            unsupportedClaim(claim.id)
        }
    }

    private fun unsupportedClaim(providerClaimId: String): AnswerClaim = AnswerClaim(
        id = providerClaimId.ifBlank { "rejected-provider-claim" },
        text = "Not established. The provider claim was removed because its text and citations " +
            "did not pass exact installed-source verification.",
        basis = StudyClaimBasis.NOT_ESTABLISHED,
        verificationStatus = ClaimVerificationStatus.REJECTED_UNSUPPORTED,
    )
}

private fun rankSource(
    source: SourceUnit,
    queryTerms: List<String>,
): RankedSourceMatch? {
    val idTerms = allTerms(source.id)
    val titleTerms = allTerms(source.title)
    val locatorTerms = allTerms(source.locator)
    val bodyTerms = allTerms(source.body)
    val matchedTerms = queryTerms.filter { term ->
        term in idTerms || term in titleTerms || term in locatorTerms || term in bodyTerms
    }
    if (matchedTerms.isEmpty()) return null

    val score = matchedTerms.sumOf { term ->
        when {
            term in idTerms -> ID_MATCH_SCORE
            term in titleTerms -> TITLE_MATCH_SCORE
            term in locatorTerms -> LOCATOR_MATCH_SCORE
            else -> BODY_MATCH_SCORE
        }
    }
    return RankedSourceMatch(
        source = source,
        score = score,
        matchedTerms = matchedTerms,
    )
}

private fun searchableTerms(value: String): List<String> =
    allTerms(value)
        .asSequence()
        .filter { term -> term.length >= MIN_TERM_LENGTH }
        .filterNot(SEARCH_STOP_WORDS::contains)
        .distinct()
        .toList()

private fun allTerms(value: String): Set<String> = TOKEN_PATTERN
    .findAll(value.lowercase())
    .map { match -> match.value }
    .toSet()

private val TOKEN_PATTERN = Regex("[\\p{L}\\p{N}]+")

private val SEARCH_STOP_WORDS = setOf(
    "about", "all", "and", "answer", "are", "before", "does", "each", "for", "from", "how",
    "ignore", "instructions", "please", "previous", "rules", "source", "sources", "tell", "the",
    "this", "use", "used", "what", "when", "where", "who", "why",
)

private const val MAX_ANSWER_MATCHES = 5
private const val MIN_TERM_LENGTH = 3
private const val ID_MATCH_SCORE = 8
private const val TITLE_MATCH_SCORE = 6
private const val LOCATOR_MATCH_SCORE = 5
private const val BODY_MATCH_SCORE = 3
