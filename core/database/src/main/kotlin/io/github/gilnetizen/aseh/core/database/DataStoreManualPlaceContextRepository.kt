package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.IOException
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

object ManualPlaceContextRepositoryFactory {
    /**
     * Creates the single app-scoped manual-place repository.
     *
     * The composition root owns [scope] and must not create another instance
     * for the same file while this one is active.
     */
    fun create(
        context: Context,
        scope: CoroutineScope,
    ): ManualPlaceContextRepository {
        val appContext = context.applicationContext
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = {
                appContext.preferencesDataStoreFile(MANUAL_PLACE_CONTEXT_FILE_NAME)
            },
        )
        return DataStoreManualPlaceContextRepository(dataStore)
    }
}

internal class DataStoreManualPlaceContextRepository(
    private val dataStore: DataStore<Preferences>,
) : ManualPlaceContextRepository {
    override val context: Flow<ManualPlaceContext?> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(Preferences::toManualPlaceContextOrNull)

    override suspend fun save(context: ManualPlaceContext) {
        val validated = context.validatedForStorage()

        dataStore.edit { values ->
            values[ManualPlacePreferenceKeys.label] = validated.label
            values[ManualPlacePreferenceKeys.latitudeDegrees] = validated.latitudeDegrees
            values[ManualPlacePreferenceKeys.longitudeDegrees] = validated.longitudeDegrees
            values[ManualPlacePreferenceKeys.timeZoneId] = validated.timeZoneId
            val elevationMeters = validated.elevationMeters
            if (elevationMeters == null) {
                values.remove(ManualPlacePreferenceKeys.elevationMeters)
            } else {
                values[ManualPlacePreferenceKeys.elevationMeters] = elevationMeters
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { values -> values.clear() }
    }
}

private fun Preferences.toManualPlaceContextOrNull(): ManualPlaceContext? {
    val values = asMap()
    val elevationValue = values[ManualPlacePreferenceKeys.elevationMeters]
    val candidate = ManualPlaceContext(
        label = values[ManualPlacePreferenceKeys.label] as? String ?: return null,
        latitudeDegrees = values[ManualPlacePreferenceKeys.latitudeDegrees] as? Double
            ?: return null,
        longitudeDegrees = values[ManualPlacePreferenceKeys.longitudeDegrees] as? Double
            ?: return null,
        elevationMeters = when (elevationValue) {
            null -> null
            is Double -> elevationValue
            else -> return null
        },
        timeZoneId = values[ManualPlacePreferenceKeys.timeZoneId] as? String ?: return null,
    )
    return candidate.takeIf(ManualPlaceContext::isValidStoredValue)
}

private fun ManualPlaceContext.validatedForStorage(): ManualPlaceContext {
    val normalizedLabel = label.trim()
    val normalized = copy(label = normalizedLabel)
    require(normalized.isValidStoredValue()) {
        "Manual place requires a non-blank label of at most " +
            "$MAX_MANUAL_PLACE_LABEL_LENGTH characters, finite latitude from -90 to 90, " +
            "finite longitude from -180 to 180, optional finite elevation, and an " +
            "available IANA time-zone ID"
    }
    return normalized
}

private fun ManualPlaceContext.isValidStoredValue(): Boolean =
    label.isNotBlank() &&
        label == label.trim() &&
        label.length <= MAX_MANUAL_PLACE_LABEL_LENGTH &&
        latitudeDegrees.isFinite() &&
        latitudeDegrees in MIN_LATITUDE_DEGREES..MAX_LATITUDE_DEGREES &&
        longitudeDegrees.isFinite() &&
        longitudeDegrees in MIN_LONGITUDE_DEGREES..MAX_LONGITUDE_DEGREES &&
        (elevationMeters == null || elevationMeters.isFinite()) &&
        timeZoneId in AVAILABLE_TIME_ZONE_IDS

private object ManualPlacePreferenceKeys {
    val label = stringPreferencesKey("label")
    val latitudeDegrees = doublePreferencesKey("latitude_degrees")
    val longitudeDegrees = doublePreferencesKey("longitude_degrees")
    val elevationMeters = doublePreferencesKey("elevation_meters")
    val timeZoneId = stringPreferencesKey("time_zone_id")
}

private val AVAILABLE_TIME_ZONE_IDS: Set<String> = ZoneId.getAvailableZoneIds()

private const val MANUAL_PLACE_CONTEXT_FILE_NAME = "manual_place_context"
private const val MIN_LATITUDE_DEGREES = -90.0
private const val MAX_LATITUDE_DEGREES = 90.0
private const val MIN_LONGITUDE_DEGREES = -180.0
private const val MAX_LONGITUDE_DEGREES = 180.0
