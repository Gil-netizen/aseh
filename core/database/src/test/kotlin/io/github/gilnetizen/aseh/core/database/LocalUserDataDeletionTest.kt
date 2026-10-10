package io.github.gilnetizen.aseh.core.database

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityAdoption
import io.github.gilnetizen.aseh.core.model.CommunityCharter
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ServiceAccessibilityProfile
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalUserDataDeletionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun deleteAllClearsSavedPlaceExperienceAndInterfacePreferences() = runTest {
        val experienceRepository = RecordingExperienceStateRepository(
            initialState = ExperienceState(
                workspaceName = "Private household",
                workspaceKind = WorkspaceKind.HOUSEHOLD,
                savedPracticeCardIds = setOf("practice.private"),
            ),
        )
        val placeRepository = DataStoreManualPlaceContextRepository(
            dataStore(backgroundScope, "place.preferences_pb"),
        )
        val preferencesRepository = DataStoreInterfacePreferencesRepository(
            dataStore(backgroundScope, "interface.preferences_pb"),
        )
        placeRepository.save(
            ManualPlaceContext(
                label = "",
                latitudeDegrees = 31.778,
                longitudeDegrees = 35.235,
                elevationMeters = null,
                timeZoneId = "Asia/Jerusalem",
                source = PlaceContextSource.DEVICE,
                horizontalAccuracyMeters = 12.0,
            ),
        )
        preferencesRepository.setSelectedDestinationId("build")
        preferencesRepository.setTextScale(1.8f)
        preferencesRepository.setDyslexiaFriendlyLatinEnabled(true)

        val result = LocalUserDataDeletion(
            experienceStateRepository = experienceRepository,
            manualPlaceContextRepository = placeRepository,
            interfacePreferencesRepository = preferencesRepository,
        ).deleteAll()

        assertTrue(result.isComplete)
        assertNull(placeRepository.context.first())
        assertEquals(ExperienceState(), experienceRepository.state.first())
        assertEquals(InterfacePreferences(), preferencesRepository.preferences.first())
    }

    @Test
    fun oneStoreFailureIsReportedWithoutSkippingTheOtherStores() = runTest {
        val failingPlaceRepository = object : ManualPlaceContextRepository {
            override val context: Flow<ManualPlaceContext?> = flowOf(null)

            override suspend fun save(context: ManualPlaceContext) = Unit

            override suspend fun clear() {
                error("Synthetic place deletion failure")
            }
        }
        val preferencesRepository = RecordingInterfacePreferencesRepository()

        val result = LocalUserDataDeletion(
            experienceStateRepository = null,
            manualPlaceContextRepository = failingPlaceRepository,
            interfacePreferencesRepository = preferencesRepository,
        ).deleteAll()

        assertFalse(result.isComplete)
        assertEquals(setOf(LocalUserDataCategory.SAVED_PLACE), result.failures.keys)
        assertTrue(preferencesRepository.resetCalled)
    }

    @Test
    fun deleteAllRunsEveryStoreOnTheConfiguredWorkDispatcher() = runTest {
        val workerThreadName = "local-user-data-deletion-test"
        val workDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, workerThreadName)
        }.asCoroutineDispatcher()
        val calls = mutableListOf<Pair<LocalUserDataCategory, String>>()
        val placeRepository = object : ManualPlaceContextRepository {
            override val context: Flow<ManualPlaceContext?> = flowOf(null)

            override suspend fun save(context: ManualPlaceContext) = Unit

            override suspend fun clear() {
                calls += LocalUserDataCategory.SAVED_PLACE to Thread.currentThread().name
            }
        }
        val experienceRepository = RecordingExperienceStateRepository(
            initialState = ExperienceState(workspaceName = "Local qahal"),
            onClear = {
                calls += LocalUserDataCategory.EXPERIENCE to Thread.currentThread().name
            },
        )
        val preferencesRepository = RecordingInterfacePreferencesRepository(
            onReset = {
                calls += LocalUserDataCategory.INTERFACE_PREFERENCES to Thread.currentThread().name
            },
        )

        try {
            val result = LocalUserDataDeletion(
                experienceStateRepository = experienceRepository,
                manualPlaceContextRepository = placeRepository,
                interfacePreferencesRepository = preferencesRepository,
                workDispatcher = workDispatcher,
            ).deleteAll()

            assertTrue(result.isComplete)
            assertEquals(LocalUserDataCategory.entries, calls.map { (category, _) -> category })
            assertTrue(calls.all { (_, threadName) -> threadName == workerThreadName })
        } finally {
            workDispatcher.close()
        }
    }

    @Test
    fun confirmedDeletionFinishesAllStoresWhenCallerIsCancelled() = runTest {
        val placeRepository = CoordinatedPlaceRepository()
        val experienceRepository = RecordingExperienceStateRepository(
            ExperienceState(workspaceName = "Local qahal"),
        )
        val preferencesRepository = RecordingInterfacePreferencesRepository()
        val deletion = LocalUserDataDeletion(
            experienceStateRepository = experienceRepository,
            manualPlaceContextRepository = placeRepository,
            interfacePreferencesRepository = preferencesRepository,
        )

        val deletionJob = launch { deletion.deleteAll() }
        placeRepository.clearStarted.await()
        deletionJob.cancel()
        placeRepository.allowClearToFinish.complete(Unit)
        deletionJob.join()

        assertTrue(placeRepository.clearFinished)
        assertEquals(ExperienceState(), experienceRepository.state.first())
        assertTrue(preferencesRepository.resetCalled)
    }

    private fun dataStore(
        scope: CoroutineScope,
        fileName: String,
    ) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(temporaryFolder.root, fileName) },
    )
}

private class CoordinatedPlaceRepository : ManualPlaceContextRepository {
    override val context: Flow<ManualPlaceContext?> = flowOf(null)
    val clearStarted = CompletableDeferred<Unit>()
    val allowClearToFinish = CompletableDeferred<Unit>()
    var clearFinished = false

    override suspend fun save(context: ManualPlaceContext) = Unit

    override suspend fun clear() {
        clearStarted.complete(Unit)
        allowClearToFinish.await()
        clearFinished = true
    }
}

private class RecordingExperienceStateRepository(
    initialState: ExperienceState,
    private val onClear: () -> Unit = {},
) : ExperienceStateRepository {
    private val mutableState = MutableStateFlow(initialState)
    override val state: Flow<ExperienceState> = mutableState

    override suspend fun setPracticeStepCompleted(stepId: String, completed: Boolean) = Unit
    override suspend fun setPracticeCardSaved(cardId: String, saved: Boolean) = Unit
    override suspend fun setPreflightStepCompleted(stepId: String, completed: Boolean) = Unit
    override suspend fun setServiceSegmentCompleted(segmentId: String, completed: Boolean) = Unit
    override suspend fun setSelectedRole(role: ParticipantRole) = Unit
    override suspend fun setSourceBookmarked(sourceId: String, bookmarked: Boolean) = Unit
    override suspend fun setWorkspace(name: String, kind: WorkspaceKind) = Unit
    override suspend fun setRoleAssignment(role: ParticipantRole, name: String) = Unit
    override suspend fun setReadingAssignment(slotId: String, name: String) = Unit
    override suspend fun setReadingPlan(slotId: String, plan: ReadingPlanEntry) = Unit
    override suspend fun setDeviceUseMode(mode: DeviceUseMode) = Unit
    override suspend fun setCalendarRegion(region: CalendarRegion) = Unit
    override suspend fun updateAccessibilityProfile(
        transform: (ServiceAccessibilityProfile) -> ServiceAccessibilityProfile,
    ) = Unit
    override suspend fun setCommunityOption(optionId: String?) = Unit
    override suspend fun setProvisionalCharterAdopted(adopted: Boolean) = Unit
    override suspend fun setCommunityCharter(charter: CommunityCharter) = Unit
    override suspend fun setDisputedPracticeAdoption(adoption: CommunityAdoption) = Unit
    override suspend fun setDossierFactReviewed(factId: String, reviewed: Boolean) = Unit
    override suspend fun resetRehearsalProgress() = Unit

    override suspend fun clearAllExperienceData() {
        onClear()
        mutableState.value = ExperienceState()
    }
}

private class RecordingInterfacePreferencesRepository(
    private val onReset: () -> Unit = {},
) : InterfacePreferencesRepository {
    override val preferences: Flow<InterfacePreferences> = flowOf(InterfacePreferences())
    var resetCalled: Boolean = false

    override suspend fun setSelectedDestinationId(destinationId: String) = Unit

    override suspend fun setTextScale(textScale: Float) = Unit

    override suspend fun setDyslexiaFriendlyLatinEnabled(enabled: Boolean) = Unit

    override suspend fun reset() {
        onReset()
        resetCalled = true
    }
}
