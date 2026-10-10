package io.github.gilnetizen.aseh.feature.practice

import io.github.gilnetizen.aseh.core.model.ChoiceOption
import io.github.gilnetizen.aseh.core.model.CommunityChoice
import io.github.gilnetizen.aseh.core.model.ConclusionStatus
import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.EditorialReviewState
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.PracticeCard
import io.github.gilnetizen.aseh.core.model.PracticeStep
import io.github.gilnetizen.aseh.core.model.ServiceDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeUiModelsTest {
    @Test
    fun `card progress counts only steps belonging to that card`() {
        val card = card(
            id = "card.one",
            topic = "Preparation",
            stepIds = listOf("step.one", "step.two"),
        )
        val state = ExperienceState(
            completedPracticeStepIds = setOf("step.one", "step.from.another.card"),
        )

        assertEquals(PracticeProgress(completed = 1, total = 2), practiceCardProgress(card, state))
    }

    @Test
    fun `topics preserve catalog order and aggregate their own progress`() {
        val catalog = catalog(
            listOf(
                card("card.one", "Preparation", listOf("step.one", "step.two")),
                card("card.two", "Access", listOf("step.three")),
                card("card.three", "Preparation", listOf("step.four")),
            ),
        )
        val state = ExperienceState(
            completedPracticeStepIds = setOf("step.two", "step.three", "unknown"),
        )

        val topics = practiceTopics(catalog, state)

        assertEquals(listOf("Preparation", "Access"), topics.map(PracticeTopic::name))
        assertEquals(PracticeProgress(completed = 1, total = 3), topics[0].progress)
        assertEquals(PracticeProgress(completed = 1, total = 1), topics[1].progress)
        assertEquals(
            PracticeProgress(completed = 2, total = 4),
            practiceCatalogProgress(catalog, state),
        )
    }

    @Test
    fun `empty progress has a stable zero fraction`() {
        assertEquals(0f, PracticeProgress(completed = 0, total = 0).fraction)
    }
}

private fun card(
    id: String,
    topic: String,
    stepIds: List<String>,
) = PracticeCard(
    id = id,
    topic = topic,
    title = "Synthetic title",
    summary = "Synthetic summary",
    action = "Synthetic action",
    context = "Synthetic context",
    supplies = "Synthetic supplies",
    steps = stepIds.map { PracticeStep(it, "Synthetic step", "Synthetic detail") },
    circumstancesDiffer = "Synthetic alternative",
    purpose = "Synthetic purpose",
    conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
    reviewState = EditorialReviewState.DRAFTED,
    primarySourceIds = listOf("source.synthetic"),
    reasoning = "Synthetic reasoning",
    otherReadings = "Synthetic other reading",
    confidence = "Synthetic confidence",
    reviewDue = "2026-11-06",
)

private fun catalog(cards: List<PracticeCard>) = DemonstratorCatalog(
    label = "Development rehearsal",
    noticeTitle = "Synthetic development content",
    noticeBody = "Not reviewed guidance.",
    practiceCards = cards,
    service = ServiceDefinition(
        id = "service.synthetic",
        title = "Synthetic service",
        subtitle = "Synthetic subtitle",
        segments = emptyList(),
        preflightSteps = emptyList(),
    ),
    sources = emptyList(),
    communityChoice = CommunityChoice(
        id = "choice.synthetic",
        title = "Synthetic choice",
        summary = "Synthetic summary",
        options = listOf(ChoiceOption("option.synthetic", "Synthetic option", "Synthetic argument")),
        conclusionStatus = ConclusionStatus.UNRESOLVED,
        reviewState = EditorialReviewState.DRAFTED,
    ),
)
