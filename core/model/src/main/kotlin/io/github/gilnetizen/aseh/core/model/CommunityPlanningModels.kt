package io.github.gilnetizen.aseh.core.model

/**
 * A local governance draft. Empty fields stay visibly incomplete; the app does
 * not infer authority or membership from a workspace name.
 */
data class CommunityCharter(
    val purpose: String = "",
    val participants: String = "",
    val authorityLimits: String = "",
    val decisionProcess: String = "",
    val roleTerms: String = "",
    val accessibilityCommitment: String = "",
    val effectiveDate: String = "",
    val reviewDate: String = "",
    val version: String = "Draft 1",
    val adopted: Boolean = false,
)

data class DossierArgument(
    val id: String,
    val title: String,
    val claim: String,
    val supports: List<String>,
    val challenges: List<String>,
    val sourceIds: List<String>,
)

data class DossierAdoptionOption(
    val id: String,
    val title: String,
    val practice: String,
    val scopeNote: String,
)

data class DisputedPracticeDossier(
    val id: String,
    val title: String,
    val question: String,
    val scope: String,
    val factualQuestions: List<String>,
    val historicalPosition: String,
    val arguments: List<DossierArgument>,
    val editorialConclusion: String,
    val conclusionStatus: ConclusionStatus,
    val reviewState: EditorialReviewState,
    val sourceIds: List<String>,
    val reviewRequirement: String,
    val adoptionOptions: List<DossierAdoptionOption>,
)

/** A community's local operating record, kept separate from dossier evidence. */
data class CommunityAdoption(
    val optionId: String? = null,
    val scope: String = "",
    val effectiveDate: String = "",
    val reviewDate: String = "",
    val recordedBy: String = "",
)

fun CommunityCharter.missingFields(): List<String> = buildList {
    if (purpose.isBlank()) add("purpose")
    if (participants.isBlank()) add("participants")
    if (authorityLimits.isBlank()) add("authority limits")
    if (decisionProcess.isBlank()) add("decision process")
    if (roleTerms.isBlank()) add("role terms")
    if (accessibilityCommitment.isBlank()) add("accessibility commitment")
    if (effectiveDate.isBlank()) add("effective date")
    if (reviewDate.isBlank()) add("review date")
}

fun CommunityAdoption.missingFields(): List<String> = buildList {
    if (optionId.isNullOrBlank()) add("adopted option")
    if (scope.isBlank()) add("scope")
    if (effectiveDate.isBlank()) add("effective date")
    if (reviewDate.isBlank()) add("review date")
    if (recordedBy.isBlank()) add("recorded by")
}
