package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterfacePreferencesRepositoryInstrumentedTest {
    @Test
    fun preferencesSurviveRepositoryRestartAndResetTogether() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(
            context.cacheDir,
            "interface-preferences-${UUID.randomUUID()}.preferences_pb",
        )
        val firstScope = repositoryScope()
        try {
            val first = repository(file, firstScope)
            first.setSelectedDestinationId("study")
            first.setTextScale(1.75f)
            first.setDyslexiaFriendlyLatinEnabled(true)

            firstScope.cancelAndJoin()

            val secondScope = repositoryScope()
            try {
                val restarted = repository(file, secondScope)
                val restored = restarted.preferences.first()
                assertEquals("study", restored.selectedDestinationId)
                assertEquals(1.75f, restored.textScale, 0.0f)
                assertTrue(restored.dyslexiaFriendlyLatinEnabled)

                restarted.reset()
                val reset = restarted.preferences.first()
                assertEquals(DEFAULT_DESTINATION_ID, reset.selectedDestinationId)
                assertEquals(DEFAULT_TEXT_SCALE, reset.textScale, 0.0f)
                assertFalse(reset.dyslexiaFriendlyLatinEnabled)
            } finally {
                secondScope.cancelAndJoin()
            }
        } finally {
            firstScope.cancelAndJoin()
            file.delete()
            File(file.absolutePath + ".tmp").delete()
        }
    }

    private fun repository(
        file: File,
        scope: CoroutineScope,
    ): InterfacePreferencesRepository = DataStoreInterfacePreferencesRepository(
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        ),
    )

    private fun repositoryScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun CoroutineScope.cancelAndJoin() {
        val scopeJob: Job = coroutineContext.job
        cancel()
        scopeJob.join()
    }
}
