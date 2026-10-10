package io.github.gilnetizen.aseh.feature.study

internal interface DevelopmentContentPackRuntime {
    suspend fun load(query: String): DevelopmentContentPackUiState
}

internal sealed interface DevelopmentContentPackUiState {
    data object Loading : DevelopmentContentPackUiState

    data class Unsupported(val reason: String) : DevelopmentContentPackUiState

    data class Failed(val reason: String) : DevelopmentContentPackUiState

    data class Ready(
        val packId: String,
        val version: String,
        val manifestSha256: String,
        val signingKeyId: String,
        val query: String,
        val hits: List<DevelopmentContentSearchHit>,
    ) : DevelopmentContentPackUiState
}

internal data class DevelopmentContentSearchHit(
    val contentId: String,
    val kind: String,
    val language: String,
    val title: String,
    val snippet: String,
    val locator: String?,
)
