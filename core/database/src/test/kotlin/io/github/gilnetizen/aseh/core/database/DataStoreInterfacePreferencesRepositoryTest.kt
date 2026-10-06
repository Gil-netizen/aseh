package io.github.gilnetizen.aseh.core.database

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreInterfacePreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyStoreUsesNonSensitiveDefaults() = runTest {
        val repository = repository(backgroundScope, "defaults.preferences_pb")

        assertEquals(
            InterfacePreferences(
                selectedDestinationId = "now",
                textScale = 1.0f,
                dyslexiaFriendlyLatinEnabled = false,
            ),
            repository.preferences.first(),
        )
    }

    @Test
    fun selectedDestinationWritesAndReads() = runTest {
        val repository = repository(backgroundScope, "destination-write.preferences_pb")

        repository.setSelectedDestinationId("practice")

        val stored = repository.preferences.first()
        assertEquals("practice", stored.selectedDestinationId)
    }

    @Test
    fun accessibilityPreferenceWritesAndReads() = runTest {
        val repository = repository(backgroundScope, "accessibility-write.preferences_pb")

        repository.setDyslexiaFriendlyLatinEnabled(true)

        assertTrue(repository.preferences.first().dyslexiaFriendlyLatinEnabled)
    }

    @Test
    fun textScaleIsBoundedAndNonFiniteInputReturnsToDefault() = runTest {
        val maximumRepository = repository(backgroundScope, "maximum.preferences_pb")
        val minimumRepository = repository(backgroundScope, "minimum.preferences_pb")
        val nonFiniteRepository = repository(backgroundScope, "non-finite.preferences_pb")

        maximumRepository.setTextScale(10.0f)
        assertEquals(MAX_TEXT_SCALE, maximumRepository.preferences.first().textScale, 0.0f)

        minimumRepository.setTextScale(-10.0f)
        assertEquals(MIN_TEXT_SCALE, minimumRepository.preferences.first().textScale, 0.0f)

        nonFiniteRepository.setTextScale(Float.NaN)
        assertEquals(DEFAULT_TEXT_SCALE, nonFiniteRepository.preferences.first().textScale, 0.0f)
    }

    @Test
    fun destinationIdRejectsFreeText() = runTest {
        val repository = repository(backgroundScope, "destination.preferences_pb")

        var rejected = false
        try {
            repository.setSelectedDestinationId("a destination with free text")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(DEFAULT_DESTINATION_ID, repository.preferences.first().selectedDestinationId)
    }

    private fun repository(
        scope: CoroutineScope,
        fileName: String,
    ): InterfacePreferencesRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, fileName) },
        )
        return DataStoreInterfacePreferencesRepository(dataStore)
    }
}
