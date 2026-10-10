package io.github.gilnetizen.aseh.feature.study

import io.github.gilnetizen.aseh.core.model.ConclusionStatus
import io.github.gilnetizen.aseh.core.model.EditorialReviewState
import io.github.gilnetizen.aseh.core.model.SourceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySearchTest {
    private val roleSource = source(
        id = "roles",
        body = "The coordinator records who owns every active role.",
    )
    private val accessSource = source(
        id = "access",
        body = "Every movement cue has a seated alternative.",
    )

    @Test
    fun `exact source lookup selects the matching local record`() {
        assertEquals(
            roleSource,
            findBestSource("Who owns each active role?", listOf(accessSource, roleSource)),
        )
    }

    @Test
    fun `structured answer preserves all eight sections and claim level citations`() {
        val secondRoleSource = source(
            id = "second-role",
            body = "The host marks an active role as open until a participant accepts it.",
        )

        val answer = answerFromInstalledSources(
            question = "How is an active role recorded?",
            sources = listOf(accessSource, secondRoleSource, roleSource),
        )

        assertTrue(answer.isEstablished)
        assertEquals(setOf("roles", "second-role"), answer.rankedMatches.map { it.source.id }.toSet())
        assertEquals(answer.rankedMatches.first().source.body, answer.shortAnswer.text)
        assertEquals(StudyClaimBasis.SOURCE_EXCERPT, answer.shortAnswer.basis)
        assertEquals(2, answer.exactEvidence.size)
        assertTrue(answer.authorityInterpretation.isNotEmpty())
        assertTrue(answer.inference.isNotEmpty())
        assertTrue(answer.disagreementAndUncertainty.isNotEmpty())
        assertTrue(answer.nextSteps.isNotEmpty())
        assertTrue(answer.limitations.isNotEmpty())

        val sourceClaims = answer.allClaims.filter { it.sourceIds.isNotEmpty() }
        assertEquals(
            sourceClaims.flatMap { claim ->
                claim.sourceIds.map { sourceId -> ClaimCitation(claim.id, sourceId) }
            }.toSet(),
            answer.claimCitations.toSet(),
        )
        assertTrue(answer.exactEvidence.all { it.sourceIds.size == 1 })
        assertTrue(
            answer.allClaims.all { claim ->
                claim.sourceIds.isNotEmpty() || claim.basis != StudyClaimBasis.SOURCE_EXCERPT
            },
        )
    }

    @Test
    fun `ranking prefers exact title terms over the same terms found only in body`() {
        val titleMatch = source(
            id = "z-title",
            title = "Active role rehearsal",
            body = "A short fixture.",
        )
        val bodyMatch = source(
            id = "a-body",
            title = "General fixture",
            body = "This body discusses an active role rehearsal.",
        )

        val answer = answerFromInstalledSources(
            question = "active role rehearsal",
            sources = listOf(bodyMatch, titleMatch),
        )

        assertEquals(listOf("z-title", "a-body"), answer.rankedMatches.map { it.source.id })
        assertTrue(answer.rankedMatches.first().score > answer.rankedMatches.last().score)
    }

    @Test
    fun `unrelated and substring-only queries are not established`() {
        val unrelated = answerFromInstalledSources(
            question = "astronomy calculation",
            sources = listOf(accessSource, roleSource),
        )
        val substringOnly = answerFromInstalledSources(
            question = "station",
            sources = listOf(accessSource),
        )

        listOf(unrelated, substringOnly).forEach { answer ->
            assertFalse(answer.isEstablished)
            assertTrue(answer.rankedMatches.isEmpty())
            assertEquals(StudyClaimBasis.NOT_ESTABLISHED, answer.shortAnswer.basis)
            assertTrue(answer.exactEvidence.isNotEmpty())
            assertTrue(answer.authorityInterpretation.isNotEmpty())
            assertTrue(answer.inference.isNotEmpty())
            assertTrue(answer.disagreementAndUncertainty.isNotEmpty())
            assertTrue(answer.nextSteps.isNotEmpty())
            assertTrue(answer.limitations.isNotEmpty())
            assertTrue(answer.claimCitations.isEmpty())
            assertEquals(5, answer.confidence.dimensions.size)
        }
        assertEquals(null, findBestSource("astronomy calculation", listOf(accessSource, roleSource)))
    }

    @Test
    fun `authority and disagreement are never invented from plain source records`() {
        val answer = answerFromInstalledSources(
            question = "Who owns each active role?",
            sources = listOf(roleSource),
        )

        assertEquals(
            listOf(StudyClaimBasis.NOT_ESTABLISHED),
            answer.authorityInterpretation.map { it.basis },
        )
        assertTrue(answer.authorityInterpretation.all { it.sourceIds.isEmpty() })
        assertEquals(
            listOf(StudyClaimBasis.LIMITATION),
            answer.disagreementAndUncertainty.map { it.basis },
        )
        assertTrue(answer.disagreementAndUncertainty.all { it.sourceIds.isEmpty() })
    }

    @Test
    fun `confidence is qualitative and covers every required dimension`() {
        val answer = answerFromInstalledSources(
            question = "active role",
            sources = listOf(roleSource),
        )

        assertEquals(
            listOf(
                "Textual certainty",
                "Attribution certainty",
                "Interpretive certainty",
                "Factual fit",
                "Breadth of editorial review",
            ),
            answer.confidence.dimensions.map { it.name },
        )
        assertTrue(answer.confidence.dimensions.all { it.assessment.isNotBlank() })
        assertTrue(answer.confidence.dimensions.all { it.rationale.isNotBlank() })
        assertTrue(answer.confidence.dimensions.all { it.evidenceThatCouldChangeIt.isNotBlank() })
        val editorialLimitation = answer.limitations.single { it.id == "limitation-editorial-states" }
        assertTrue("conclusion evidence statuses" in editorialLimitation.text)
        assertTrue("Editorial proposal" in editorialLimitation.text)
        assertTrue("editorial review states" in editorialLimitation.text)
        assertTrue("Drafted" in editorialLimitation.text)
    }

    @Test
    fun `answer is deterministic across source input order`() {
        val secondRoleSource = source(
            id = "second-role",
            body = "An active role stays open until a participant accepts it.",
        )

        val first = answerFromInstalledSources(
            question = "active role",
            sources = listOf(roleSource, secondRoleSource, accessSource),
        )
        val reordered = answerFromInstalledSources(
            question = "active role",
            sources = listOf(accessSource, secondRoleSource, roleSource),
        )

        assertEquals(first, reordered)
    }

    private fun source(
        id: String,
        body: String,
        title: String = "Fixture $id",
    ) = SourceUnit(
        id = id,
        title = title,
        locator = "Fixture 1:$id",
        language = "English",
        body = body,
        edition = "Synthetic",
        provenance = "Synthetic",
        license = "CC-BY-SA-4.0",
        conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
        reviewState = EditorialReviewState.DRAFTED,
        relatedPracticeIds = emptyList(),
        relatedSegmentIds = emptyList(),
    )
}

private fun answerFromInstalledSources(
    question: String,
    sources: List<SourceUnit>,
): OfflineStudyAnswer = answerFromInstalledSources(
    question = question,
    sources = sources,
    provider = DeterministicInstalledCorpusFakeProvider(),
)
