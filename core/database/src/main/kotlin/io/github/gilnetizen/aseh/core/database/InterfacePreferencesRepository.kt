package io.github.gilnetizen.aseh.core.database

import kotlinx.coroutines.flow.Flow

data class InterfacePreferences(
    val selectedDestinationId: String = DEFAULT_DESTINATION_ID,
    val textScale: Float = DEFAULT_TEXT_SCALE,
    val dyslexiaFriendlyLatinEnabled: Boolean = false,
)

interface InterfacePreferencesRepository {
    val preferences: Flow<InterfacePreferences>

    suspend fun setSelectedDestinationId(destinationId: String)
    suspend fun setTextScale(textScale: Float)
    suspend fun setDyslexiaFriendlyLatinEnabled(enabled: Boolean)
    suspend fun reset()
}

const val DEFAULT_DESTINATION_ID = "now"
const val MIN_TEXT_SCALE = 0.8f
const val DEFAULT_TEXT_SCALE = 1.0f
const val MAX_TEXT_SCALE = 2.0f
