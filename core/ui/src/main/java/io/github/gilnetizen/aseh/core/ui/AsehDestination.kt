package io.github.gilnetizen.aseh.core.ui

import androidx.annotation.StringRes

/**
 * The stable top-level information architecture, in visual and reading order.
 *
 * [persistedId] is storage-safe and must not be changed when a visible label is
 * edited. Unknown values restore to [NOW] so a removed or corrupt preference
 * cannot make the shell unreachable.
 */
enum class AsehDestination(
    val persistedId: String,
    @get:StringRes val labelRes: Int,
    @get:StringRes val symbolRes: Int,
) {
    NOW(
        persistedId = "now",
        labelRes = R.string.aseh_destination_now,
        symbolRes = R.string.aseh_destination_symbol_now,
    ),
    PRACTICE(
        persistedId = "practice",
        labelRes = R.string.aseh_destination_practice,
        symbolRes = R.string.aseh_destination_symbol_practice,
    ),
    PRAYER(
        persistedId = "prayer",
        labelRes = R.string.aseh_destination_prayer,
        symbolRes = R.string.aseh_destination_symbol_prayer,
    ),
    STUDY(
        persistedId = "study",
        labelRes = R.string.aseh_destination_study,
        symbolRes = R.string.aseh_destination_symbol_study,
    ),
    BUILD(
        persistedId = "build",
        labelRes = R.string.aseh_destination_build,
        symbolRes = R.string.aseh_destination_symbol_build,
    ),
    ;

    companion object {
        fun fromPersistedId(persistedId: String?): AsehDestination =
            entries.firstOrNull { it.persistedId == persistedId } ?: NOW
    }
}
