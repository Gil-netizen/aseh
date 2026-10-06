package io.github.gilnetizen.aseh.core.database

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreManualPlaceContextRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun validContextRoundTripsWithNormalizedLabel() = runTest {
        val repository = repository(backgroundScope, "round-trip.preferences_pb")

        repository.save(
            ManualPlaceContext(
                label = "  Synthetic test place  ",
                latitudeDegrees = 12.25,
                longitudeDegrees = -34.5,
                elevationMeters = 123.75,
                timeZoneId = "UTC",
            ),
        )

        assertEquals(
            ManualPlaceContext(
                label = "Synthetic test place",
                latitudeDegrees = 12.25,
                longitudeDegrees = -34.5,
                elevationMeters = 123.75,
                timeZoneId = "UTC",
            ),
            repository.context.first(),
        )
    }

    @Test
    fun inclusiveCoordinateBoundariesAreAccepted() = runTest {
        val repository = DataStoreManualPlaceContextRepository(InMemoryPreferencesDataStore())
        val northEast = validContext.copy(
            label = "x".repeat(MAX_MANUAL_PLACE_LABEL_LENGTH),
            latitudeDegrees = 90.0,
            longitudeDegrees = 180.0,
            elevationMeters = Double.MAX_VALUE,
        )
        val southWest = validContext.copy(
            latitudeDegrees = -90.0,
            longitudeDegrees = -180.0,
            elevationMeters = -430.0,
        )

        repository.save(northEast)
        assertEquals(northEast, repository.context.first())

        repository.save(southWest)
        assertEquals(southWest, repository.context.first())
    }

    @Test
    fun invalidCandidateIsRejectedWithoutChangingStoredContext() = runTest {
        val repository = repository(backgroundScope, "atomic-rejection.preferences_pb")
        repository.save(validContext)
        val invalidCandidates = listOf(
            validContext.copy(label = "   "),
            validContext.copy(label = "x".repeat(MAX_MANUAL_PLACE_LABEL_LENGTH + 1)),
            validContext.copy(latitudeDegrees = 90.000_001),
            validContext.copy(latitudeDegrees = Double.NaN),
            validContext.copy(longitudeDegrees = -180.000_001),
            validContext.copy(longitudeDegrees = Double.POSITIVE_INFINITY),
            validContext.copy(elevationMeters = Double.NEGATIVE_INFINITY),
            validContext.copy(timeZoneId = "GMT+02:00"),
            validContext.copy(timeZoneId = "Not/A_Zone"),
        )

        invalidCandidates.forEach { invalid ->
            try {
                repository.save(invalid)
                fail("Expected invalid manual place to be rejected: $invalid")
            } catch (_: IllegalArgumentException) {
                // Expected: validation runs before DataStore.edit.
            }
            assertEquals(validContext, repository.context.first())
        }
    }

    @Test
    fun clearRestoresNoContext() = runTest {
        val repository = DataStoreManualPlaceContextRepository(InMemoryPreferencesDataStore())
        repository.save(validContext)

        repository.clear()

        assertNull(repository.context.first())
    }

    @Test
    fun incompleteOrInvalidStoredInputFailsClosed() = runTest {
        val incompleteStore = dataStore(backgroundScope, "incomplete.preferences_pb")
        incompleteStore.edit { values ->
            values[stringPreferencesKey("label")] = "Synthetic place"
            values[doublePreferencesKey("latitude_degrees")] = 10.0
        }
        assertNull(DataStoreManualPlaceContextRepository(incompleteStore).context.first())

        val invalidStore = dataStore(backgroundScope, "invalid.preferences_pb")
        invalidStore.edit { values ->
            values[stringPreferencesKey("label")] = "Synthetic place"
            values[doublePreferencesKey("latitude_degrees")] = 91.0
            values[doublePreferencesKey("longitude_degrees")] = 20.0
            values[doublePreferencesKey("elevation_meters")] = Double.NaN
            values[stringPreferencesKey("time_zone_id")] = "Not/A_Zone"
        }
        assertNull(DataStoreManualPlaceContextRepository(invalidStore).context.first())
    }

    @Test
    fun wrongPrimitiveTypesInStoredInputFailClosed() = runTest {
        val wrongRequiredTypeStore = dataStore(
            backgroundScope,
            "wrong-required-type.preferences_pb",
        )
        wrongRequiredTypeStore.edit { values ->
            values[intPreferencesKey("label")] = 7
            values[doublePreferencesKey("latitude_degrees")] = 10.0
            values[doublePreferencesKey("longitude_degrees")] = 20.0
            values[stringPreferencesKey("time_zone_id")] = "Etc/UTC"
        }
        assertNull(
            DataStoreManualPlaceContextRepository(wrongRequiredTypeStore).context.first(),
        )

        val wrongOptionalTypeStore = dataStore(
            backgroundScope,
            "wrong-optional-type.preferences_pb",
        )
        wrongOptionalTypeStore.edit { values ->
            values[stringPreferencesKey("label")] = "Synthetic place"
            values[doublePreferencesKey("latitude_degrees")] = 10.0
            values[doublePreferencesKey("longitude_degrees")] = 20.0
            values[stringPreferencesKey("elevation_meters")] = "high"
            values[stringPreferencesKey("time_zone_id")] = "Etc/UTC"
        }
        assertNull(
            DataStoreManualPlaceContextRepository(wrongOptionalTypeStore).context.first(),
        )
    }

    @Test
    fun malformedPreferenceFileIsReplacedWithNoContext() = runTest {
        val file = File(temporaryFolder.root, "malformed.preferences_pb")
        file.writeBytes(byteArrayOf(0x7f, 0x01, 0x02, 0x03))
        val repository = DataStoreManualPlaceContextRepository(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = backgroundScope,
                produceFile = { file },
            ),
        )

        assertNull(repository.context.first())
    }

    private fun repository(
        scope: CoroutineScope,
        fileName: String,
    ): ManualPlaceContextRepository =
        DataStoreManualPlaceContextRepository(dataStore(scope, fileName))

    private fun dataStore(
        scope: CoroutineScope,
        fileName: String,
    ) = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = { File(temporaryFolder.root, fileName) },
    )

    private companion object {
        val validContext = ManualPlaceContext(
            label = "Synthetic place",
            latitudeDegrees = 10.0,
            longitudeDegrees = 20.0,
            elevationMeters = 30.0,
            timeZoneId = "Etc/UTC",
        )
    }
}

/**
 * Keeps repository behavior tests independent of the host file-system implementation.
 *
 * Persistence and restart behavior remain covered by the Android instrumented test. This avoids
 * the upstream DataStore JVM atomic-replace failure on Windows after a second write to one file.
 */
private class InMemoryPreferencesDataStore(
    initialValue: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initialValue)
    private val updateMutex = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = updateMutex.withLock {
        transform(state.value).also { state.value = it }
    }
}
