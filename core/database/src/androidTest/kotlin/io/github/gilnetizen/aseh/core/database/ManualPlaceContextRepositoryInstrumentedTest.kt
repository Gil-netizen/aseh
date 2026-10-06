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
import org.junit.Assert.assertNull
import org.junit.Test

class ManualPlaceContextRepositoryInstrumentedTest {
    @Test
    fun contextSurvivesRepositoryRestartAndCanBeCleared() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(
            context.cacheDir,
            "manual-place-context-${UUID.randomUUID()}.preferences_pb",
        )
        val expected = ManualPlaceContext(
            label = "Synthetic place",
            latitudeDegrees = 12.345,
            longitudeDegrees = 67.89,
            elevationMeters = 123.0,
            timeZoneId = "Etc/UTC",
        )
        val firstScope = repositoryScope()
        try {
            val first = repository(file, firstScope)
            first.save(expected)

            firstScope.cancelAndJoin()

            val secondScope = repositoryScope()
            try {
                val restarted = repository(file, secondScope)
                assertEquals(expected, restarted.context.first())

                restarted.clear()
                assertNull(restarted.context.first())
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
    ): ManualPlaceContextRepository = DataStoreManualPlaceContextRepository(
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
