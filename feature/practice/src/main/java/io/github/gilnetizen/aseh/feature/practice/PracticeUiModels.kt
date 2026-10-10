package io.github.gilnetizen.aseh.feature.practice

import io.github.gilnetizen.aseh.core.model.DemonstratorCatalog
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.PracticeCard

internal data class PracticeProgress(
    val completed: Int,
    val total: Int,
) {
    val fraction: Float
        get() = if (total == 0) 0f else completed.toFloat() / total
}

internal data class PracticeTopic(
    val name: String,
    val cards: List<PracticeCard>,
    val progress: PracticeProgress,
)

internal fun practiceCardProgress(
    card: PracticeCard,
    state: ExperienceState,
): PracticeProgress = PracticeProgress(
    completed = card.steps.count { it.id in state.completedPracticeStepIds },
    total = card.steps.size,
)

internal fun practiceCatalogProgress(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
): PracticeProgress {
    val steps = catalog.practiceCards.flatMap(PracticeCard::steps)
    return PracticeProgress(
        completed = steps.count { it.id in state.completedPracticeStepIds },
        total = steps.size,
    )
}

internal fun practiceTopics(
    catalog: DemonstratorCatalog,
    state: ExperienceState,
): List<PracticeTopic> = catalog.practiceCards
    .groupBy(PracticeCard::topic)
    .map { (topic, cards) ->
        val cardProgress = cards.map { practiceCardProgress(it, state) }
        PracticeTopic(
            name = topic,
            cards = cards,
            progress = PracticeProgress(
                completed = cardProgress.sumOf(PracticeProgress::completed),
                total = cardProgress.sumOf(PracticeProgress::total),
            ),
        )
    }
