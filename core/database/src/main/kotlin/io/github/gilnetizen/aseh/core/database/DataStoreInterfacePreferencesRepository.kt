package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

object InterfacePreferencesRepositoryFactory {
    /**
     * Creates the single app-scoped preferences repository.
     *
     * The composition root owns [scope] and must not create another instance
     * for the same file while this one is active.
     */
    fun create(
        context: Context,
        scope: CoroutineScope,
    ): InterfacePreferencesRepository {
        val appContext = context.applicationContext
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { appContext.preferencesDataStoreFile(INTERFACE_PREFERENCES_FILE_NAME) },
        )
        return DataStoreInterfacePreferencesRepository(dataStore)
    }
}

internal class DataStoreInterfacePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : InterfacePreferencesRepository {
    override val preferences: Flow<InterfacePreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(Preferences::toExternalModel)

    override suspend fun setSelectedDestinationId(destinationId: String) {
        require(destinationId.isValidDestinationId()) {
            "destinationId must use lowercase ASCII letters, digits, or underscores"
        }
        dataStore.edit { values ->
            values[PreferenceKeys.selectedDestinationId] = destinationId
        }
    }

    override suspend fun setTextScale(textScale: Float) {
        dataStore.edit { values ->
            values[PreferenceKeys.textScale] = textScale.normalizedTextScale()
        }
    }

    override suspend fun setDyslexiaFriendlyLatinEnabled(enabled: Boolean) {
        dataStore.edit { values ->
            values[PreferenceKeys.dyslexiaFriendlyLatinEnabled] = enabled
        }
    }

    override suspend fun reset() {
        dataStore.edit { values -> values.clear() }
    }
}

private fun Preferences.toExternalModel(): InterfacePreferences = InterfacePreferences(
    selectedDestinationId = this[PreferenceKeys.selectedDestinationId]
        ?.takeIf(String::isValidDestinationId)
        ?: DEFAULT_DESTINATION_ID,
    textScale = this[PreferenceKeys.textScale]
        ?.normalizedTextScale()
        ?: DEFAULT_TEXT_SCALE,
    dyslexiaFriendlyLatinEnabled =
        this[PreferenceKeys.dyslexiaFriendlyLatinEnabled]
            ?: false,
)

private object PreferenceKeys {
    val selectedDestinationId = stringPreferencesKey("selected_destination_id")
    val textScale = floatPreferencesKey("text_scale")
    val dyslexiaFriendlyLatinEnabled = booleanPreferencesKey("dyslexia_friendly_latin_enabled")
}

private object DestinationIdPattern {
    val regex = Regex("[a-z][a-z0-9_]{0,63}")
}

private fun String.isValidDestinationId(): Boolean = DestinationIdPattern.regex.matches(this)

private fun Float.normalizedTextScale(): Float = when {
    !isFinite() -> DEFAULT_TEXT_SCALE
    else -> coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE)
}

private const val INTERFACE_PREFERENCES_FILE_NAME = "interface_preferences"
