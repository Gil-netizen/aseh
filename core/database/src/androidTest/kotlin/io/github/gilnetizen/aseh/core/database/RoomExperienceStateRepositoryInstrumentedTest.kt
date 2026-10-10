package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityAdoption
import io.github.gilnetizen.aseh.core.model.CommunityCharter
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ReadingPreparationStatus
import io.github.gilnetizen.aseh.core.model.ServiceInstanceKey
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomExperienceStateRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var preferenceFile: File
    private lateinit var database: OperationalDatabase
    private lateinit var preferenceScope: CoroutineScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val identity = UUID.randomUUID().toString()
        databaseName = "experience-$identity.db"
        preferenceFile = File(context.cacheDir, "experience-$identity.preferences_pb")
        database = openDatabase()
        preferenceScope = repositoryScope()
    }

    @After
    fun tearDown() {
        database.close()
        preferenceScope.cancel()
        context.deleteDatabase(databaseName)
        preferenceFile.delete()
    }

    @Test
    fun completeStateSurvivesDatabaseAndRepositoryRestart() = runTest {
        val firstStore = preferenceStore(preferenceScope)
        val first = RoomExperienceStateRepository(database.experienceStateDao(), firstStore)
        first.activateServiceInstance(SERVICE_INSTANCE)
        first.setWorkspace("  Kehillah rehearsal  ", WorkspaceKind.QAHAL)
        first.setSelectedRole(ParticipantRole.GABBAI)
        first.setSelectedServicePlan("dev.service.shabbat.morning@2026-10-10")
        first.setRoleAssignment(ParticipantRole.LEADER, "  Ari  ")
        first.setRoleAssignment(ParticipantRole.READER, "Miriam")
        first.setReadingPlan(
            "reading.aliyah.1",
            ReadingPlanEntry(
                portionTitle = "  Synthetic portion label  ",
                locator = "  Local plan 4  ",
                passageRange = "  Section 1-8  ",
                assignee = "  Shira  ",
                backupAssignee = "  Yael  ",
                preparationStatus = ReadingPreparationStatus.READY,
                manualOverride = true,
                overrideReason = "  Verified against the group's own prepared copy.  ",
            ),
        )
        first.setReadingAssignment("reading.maftir", "David")
        first.setDeviceUseMode(DeviceUseMode.PRINT_ONLY)
        first.setCalendarRegion(CalendarRegion.ISRAEL)
        first.updateAccessibilityProfile { current ->
            current.copy(
                participantNeedsReviewed = true,
                useMovementAlternatives = true,
                useLargeText = true,
                keepScreenAwake = true,
            )
        }
        first.updateAccessibilityProfile { current ->
            current.copy(
                useVisualVoiceCues = true,
                useHighContrast = true,
                reduceMotion = true,
                lowLightMode = true,
            )
        }
        first.setPracticeStepCompleted("practice.team.leader", true)
        first.setPracticeCardSaved("practice.rehearsal.team", true)
        first.setPreflightStepCompleted("preflight.roles", true)
        first.setServiceSegmentCompleted("segment.gather", true)
        first.setSourceBookmarked("source.demo.role-readiness", true)
        first.setCommunityOption("choice.teaching.after-reading")
        first.setCommunityCharter(completeCharter())
        first.setDisputedPracticeAdoption(completeAdoption())
        first.setDossierFactReviewed("dossier.shabbat-electric-lighting.fact.1", true)

        preferenceScope.cancelAndJoin()
        database.close()
        preferenceScope = repositoryScope()
        database = openDatabase()

        val restored = RoomExperienceStateRepository(
            database.experienceStateDao(),
            preferenceStore(preferenceScope),
        ).state.first()

        assertEquals("Kehillah rehearsal", restored.workspaceName)
        assertEquals(SERVICE_INSTANCE.occurrenceId, restored.serviceInstanceId)
        assertEquals(SERVICE_DATE, restored.serviceInstanceDate)
        assertEquals(ParticipantRole.GABBAI, restored.selectedRole)
        assertEquals(
            "dev.service.shabbat.morning@2026-10-10",
            restored.selectedServicePlanId,
        )
        assertEquals("Ari", restored.roleAssignments[ParticipantRole.LEADER])
        assertEquals("Miriam", restored.roleAssignments[ParticipantRole.READER])
        assertEquals("Shira", restored.readingAssignments["reading.aliyah.1"])
        assertEquals("David", restored.readingAssignments["reading.maftir"])
        assertEquals(
            ReadingPlanEntry(
                portionTitle = "Synthetic portion label",
                locator = "Local plan 4",
                passageRange = "Section 1-8",
                assignee = "Shira",
                backupAssignee = "Yael",
                preparationStatus = ReadingPreparationStatus.READY,
                manualOverride = true,
                overrideReason = "Verified against the group's own prepared copy.",
            ),
            restored.readingPlans["reading.aliyah.1"],
        )
        assertEquals(ReadingPlanEntry(assignee = "David"), restored.readingPlans["reading.maftir"])
        assertEquals(DeviceUseMode.PRINT_ONLY, restored.deviceUseMode)
        assertEquals(CalendarRegion.ISRAEL, restored.calendarRegion)
        assertTrue(restored.accessibilityProfile.keepScreenAwake)
        assertTrue(restored.accessibilityProfile.lowLightMode)
        assertEquals(setOf("practice.team.leader"), restored.completedPracticeStepIds)
        assertEquals(setOf("practice.rehearsal.team"), restored.savedPracticeCardIds)
        assertEquals(setOf("preflight.roles"), restored.completedPreflightStepIds)
        assertEquals(setOf("segment.gather"), restored.completedServiceSegmentIds)
        assertEquals(setOf("source.demo.role-readiness"), restored.bookmarkedSourceIds)
        assertEquals("choice.teaching.after-reading", restored.selectedCommunityOptionId)
        assertEquals(completeCharter(), restored.communityCharter)
        assertEquals(completeAdoption(), restored.disputedPracticeAdoption)
        assertEquals(
            setOf("dossier.shabbat-electric-lighting.fact.1"),
            restored.reviewedDossierFactIds,
        )
    }

    @Test
    fun legacyDataStoreImportsOnceRetainsSmallPreferencesAndCleansStaleKeys() = runTest {
        val store = preferenceStore(preferenceScope)
        store.edit { values ->
            values[ExperiencePreferenceKeys.workspaceName] = "Legacy qahal"
            values[ExperiencePreferenceKeys.workspaceKind] = WorkspaceKind.QAHAL.id
            values[ExperiencePreferenceKeys.roleAssignment(ParticipantRole.LEADER)] = "Ari"
            values[ExperiencePreferenceKeys.readingAssignment("reading.aliyah.1")] = "Shira"
            values[ExperiencePreferenceKeys.completedServiceSegments] = setOf("segment.gather")
            values[ExperiencePreferenceKeys.charterReviewDate] = "2027-01-07"
            values[ExperiencePreferenceKeys.selectedRole] = ParticipantRole.READER.id
            values[ExperiencePreferenceKeys.deviceUseMode] = DeviceUseMode.PRINT_ONLY.id
            values[ExperiencePreferenceKeys.useLargeText] = true
        }

        val first = RoomExperienceStateRepository(database.experienceStateDao(), store)
        val imported = first.state.first()

        assertEquals("Legacy qahal", imported.workspaceName)
        assertTrue(imported.roleAssignments.isEmpty())
        assertTrue(imported.readingAssignments.isEmpty())
        assertTrue(imported.readingPlans.isEmpty())
        assertTrue(imported.completedServiceSegmentIds.isEmpty())
        assertNull(imported.serviceInstanceDate)
        assertEquals("2027-01-07", imported.communityCharter.reviewDate)
        assertEquals(ParticipantRole.READER, imported.selectedRole)
        assertEquals(DeviceUseMode.PRINT_ONLY, imported.deviceUseMode)
        assertTrue(imported.accessibilityProfile.useLargeText)
        assertTrue(database.experienceStateDao().profile()?.legacyDataStoreMigrated == true)

        val cleaned = store.data.first()
        assertNull(cleaned[ExperiencePreferenceKeys.workspaceName])
        assertNull(cleaned[ExperiencePreferenceKeys.readingAssignment("reading.aliyah.1")])
        assertEquals(ParticipantRole.READER.id, cleaned[ExperiencePreferenceKeys.selectedRole])
        assertEquals(DeviceUseMode.PRINT_ONLY.id, cleaned[ExperiencePreferenceKeys.deviceUseMode])

        // Simulate a process stopping after Room commit but before legacy-key cleanup.
        store.edit { values -> values[ExperiencePreferenceKeys.workspaceName] = "Stale copy" }
        val restarted = RoomExperienceStateRepository(database.experienceStateDao(), store)
        assertEquals("Legacy qahal", restarted.state.first().workspaceName)
        assertNull(store.data.first()[ExperiencePreferenceKeys.workspaceName])
    }

    @Test
    fun transientLegacyReadFailureDoesNotCommitDefaultsAndCanRetry() = runTest {
        val store = preferenceStore(preferenceScope)
        store.edit { values ->
            values[ExperiencePreferenceKeys.workspaceName] = "Recovered qahal"
        }
        val failFirstStore = FailFirstReadDataStore(store)
        val repository = RoomExperienceStateRepository(
            database.experienceStateDao(),
            failFirstStore,
        )

        val firstFailure = runCatching { repository.state.first() }.exceptionOrNull()

        assertTrue(firstFailure is IOException)
        assertNull(database.experienceStateDao().profile())
        assertEquals("Recovered qahal", store.data.first()[ExperiencePreferenceKeys.workspaceName])

        assertEquals("Recovered qahal", repository.state.first().workspaceName)
        assertTrue(database.experienceStateDao().profile()?.legacyDataStoreMigrated == true)
        assertNull(store.data.first()[ExperiencePreferenceKeys.workspaceName])
    }

    @Test
    fun resetAndDeleteAllAffectTheIntendedRoomAndPreferenceRecords() = runTest {
        val store = preferenceStore(preferenceScope)
        val repository = RoomExperienceStateRepository(database.experienceStateDao(), store)
        repository.activateServiceInstance(SERVICE_INSTANCE)
        repository.setWorkspace("Household", WorkspaceKind.HOUSEHOLD)
        repository.setSelectedRole(ParticipantRole.READER)
        repository.setPracticeStepCompleted("practice.access.path", true)
        repository.setPracticeCardSaved("practice.rehearsal.access", true)
        repository.setPreflightStepCompleted("preflight.access", true)
        repository.setServiceSegmentCompleted("segment.reading", true)
        repository.setSourceBookmarked("source.demo.access-path", true)
        repository.setCommunityCharter(completeCharter())

        repository.resetRehearsalProgress()
        val reset = repository.state.first()
        assertTrue(reset.completedPracticeStepIds.isEmpty())
        assertTrue(reset.completedPreflightStepIds.isEmpty())
        assertTrue(reset.completedServiceSegmentIds.isEmpty())
        assertEquals(setOf("practice.rehearsal.access"), reset.savedPracticeCardIds)
        assertEquals(setOf("source.demo.access-path"), reset.bookmarkedSourceIds)
        assertEquals("Household", reset.workspaceName)
        assertEquals(ParticipantRole.READER, reset.selectedRole)

        repository.clearAllExperienceData()

        assertEquals(ExperienceState(), repository.state.first())
        val snapshot = database.experienceStateDao().snapshot()
        assertTrue(snapshot?.profile?.legacyDataStoreMigrated == true)
        assertTrue(snapshot?.roleAssignments.orEmpty().isEmpty())
        assertTrue(snapshot?.readingAssignments.orEmpty().isEmpty())
        assertTrue(snapshot?.readingPlans.orEmpty().isEmpty())
        assertTrue(snapshot?.recordMarkers.orEmpty().isEmpty())
        assertTrue(store.data.first().asMap().isEmpty())
    }

    @Test
    fun sameDateDifferentProfileOccurrencesKeepProgressIsolated() = runTest {
        val repository = RoomExperienceStateRepository(
            database.experienceStateDao(),
            preferenceStore(preferenceScope),
        )
        val israel = SERVICE_INSTANCE
        val diaspora = ServiceInstanceKey(
            "dev.service.shabbat.morning@2026-10-10#dev.opinion.diaspora",
            SERVICE_DATE,
        )

        repository.activateServiceInstance(israel)
        repository.setWorkspace("Harimon", WorkspaceKind.QAHAL)
        repository.setPracticeCardSaved("practice.saved", true)
        repository.setSourceBookmarked("source.saved", true)
        repository.setCommunityCharter(completeCharter())
        repository.setDossierFactReviewed("dossier.fact.saved", true)
        repository.setPracticeStepCompleted("practice.weekly", true)
        repository.setPreflightStepCompleted("preflight.weekly", true)
        repository.setServiceSegmentCompleted("segment.weekly", true)
        repository.setRoleAssignment(ParticipantRole.LEADER, "Ari")
        repository.setReadingPlan(
            "reading.aliyah.1",
            ReadingPlanEntry(
                assignee = "Miriam",
                preparationStatus = ReadingPreparationStatus.READY,
            ),
        )

        repository.activateServiceInstance(diaspora)
        val diasporaInitially = repository.state.first()

        assertEquals(diaspora, diasporaInitially.serviceInstanceKey)
        assertTrue(diasporaInitially.completedPracticeStepIds.isEmpty())
        assertTrue(diasporaInitially.completedPreflightStepIds.isEmpty())
        assertTrue(diasporaInitially.completedServiceSegmentIds.isEmpty())
        assertTrue(diasporaInitially.roleAssignments.isEmpty())
        assertTrue(diasporaInitially.readingPlans.isEmpty())
        repository.setPracticeStepCompleted("practice.diaspora", true)
        repository.setRoleAssignment(ParticipantRole.LEADER, "Leah")

        repository.activateServiceInstance(israel)
        val restoredIsrael = repository.state.first()

        assertEquals(israel, restoredIsrael.serviceInstanceKey)
        assertEquals(setOf("practice.weekly"), restoredIsrael.completedPracticeStepIds)
        assertEquals(setOf("preflight.weekly"), restoredIsrael.completedPreflightStepIds)
        assertEquals(setOf("segment.weekly"), restoredIsrael.completedServiceSegmentIds)
        assertEquals("Ari", restoredIsrael.roleAssignments[ParticipantRole.LEADER])
        assertEquals("Miriam", restoredIsrael.readingPlans["reading.aliyah.1"]?.assignee)
        assertEquals("Harimon", restoredIsrael.workspaceName)
        assertEquals(setOf("practice.saved"), restoredIsrael.savedPracticeCardIds)
        assertEquals(setOf("source.saved"), restoredIsrael.bookmarkedSourceIds)

        repository.activateServiceInstance(diaspora)
        val restoredDiaspora = repository.state.first()
        assertEquals(setOf("practice.diaspora"), restoredDiaspora.completedPracticeStepIds)
        assertEquals("Leah", restoredDiaspora.roleAssignments[ParticipantRole.LEADER])
        assertTrue(restoredDiaspora.completedServiceSegmentIds.isEmpty())
    }

    @Test
    fun switchingDatesPreservesEachOccurrenceAndRejectsDelayedWrites() = runTest {
        val repository = RoomExperienceStateRepository(
            database.experienceStateDao(),
            preferenceStore(preferenceScope),
        )
        val first = SERVICE_INSTANCE
        val nextDate = SERVICE_DATE.plusWeeks(1)
        val second = ServiceInstanceKey(
            "dev.service.shabbat.morning@2026-10-17#dev.opinion.israel",
            nextDate,
        )

        repository.activateServiceInstance(first)
        repository.setServiceSegmentCompleted("segment.first", true)
        repository.setRoleAssignment(ParticipantRole.LEADER, "Ari")
        repository.activateServiceInstance(second)
        val secondInitially = repository.state.first()

        assertEquals(second, secondInitially.serviceInstanceKey)
        assertTrue(secondInitially.completedServiceSegmentIds.isEmpty())
        assertTrue(secondInitially.roleAssignments.isEmpty())
        repository.setServiceSegmentCompleted("segment.second", true)

        repository.setServiceSegmentCompleted(first, "segment.delayed-old-write", true)
        val afterDelayedWrite = repository.state.first()
        assertEquals(second, afterDelayedWrite.serviceInstanceKey)
        assertEquals(setOf("segment.second"), afterDelayedWrite.completedServiceSegmentIds)

        repository.activateServiceInstance(first)
        val restoredFirst = repository.state.first()
        assertEquals(setOf("segment.first"), restoredFirst.completedServiceSegmentIds)
        assertEquals("Ari", restoredFirst.roleAssignments[ParticipantRole.LEADER])

        repository.activateServiceInstance(second)
        assertEquals(
            setOf("segment.second"),
            repository.state.first().completedServiceSegmentIds,
        )
    }

    @Test
    fun unsupportedSelectionDeactivatesOccurrenceAndRejectsQueuedOldWrite() = runTest {
        val repository = RoomExperienceStateRepository(
            database.experienceStateDao(),
            preferenceStore(preferenceScope),
        )

        repository.activateServiceInstance(SERVICE_INSTANCE)
        repository.setServiceSegmentCompleted("segment.before-selection-change", true)
        repository.deactivateServiceInstance()

        assertEquals(null, repository.state.first().serviceInstanceKey)
        repository.setServiceSegmentCompleted(
            SERVICE_INSTANCE,
            "segment.queued-after-unsupported-selection",
            true,
        )
        assertEquals(null, repository.state.first().serviceInstanceKey)

        repository.activateServiceInstance(SERVICE_INSTANCE)
        assertEquals(
            setOf("segment.before-selection-change"),
            repository.state.first().completedServiceSegmentIds,
        )
    }

    private fun openDatabase(): OperationalDatabase = Room.databaseBuilder(
        context,
        OperationalDatabase::class.java,
        databaseName,
    )
        .allowMainThreadQueries()
        .addMigrations(*OperationalDatabaseMigrations.all)
        .build()

    private fun preferenceStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { preferenceFile },
    )

    private fun repositoryScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun CoroutineScope.cancelAndJoin() {
        val scopeJob: Job = coroutineContext.job
        cancel()
        scopeJob.join()
    }

    private fun completeCharter() = CommunityCharter(
        purpose = "Practice participation",
        participants = "Members and guests",
        authorityLimits = "Local rehearsal operations",
        decisionProcess = "Consensus recorded by the coordinator",
        roleTerms = "Review roles each quarter",
        accessibilityCommitment = "Ask participants and prepare equal alternatives",
        effectiveDate = "2026-10-07",
        reviewDate = "2027-01-07",
        version = "Trial 1",
        adopted = true,
    )

    private fun completeAdoption() = CommunityAdoption(
        optionId = "adoption.lighting.prepared-precaution",
        scope = "This rehearsal workspace",
        effectiveDate = "2026-10-07",
        reviewDate = "2026-11-07",
        recordedBy = "Community coordinator",
    )

    private companion object {
        val SERVICE_DATE: LocalDate = LocalDate.of(2026, 10, 10)
        val SERVICE_INSTANCE = ServiceInstanceKey(
            "dev.service.shabbat.morning@2026-10-10#dev.opinion.israel",
            SERVICE_DATE,
        )
    }
}

private class FailFirstReadDataStore(
    private val delegate: DataStore<Preferences>,
) : DataStore<Preferences> {
    private var shouldFail = true

    override val data: Flow<Preferences>
        get() = flow {
            if (shouldFail) {
                shouldFail = false
                throw IOException("Synthetic transient read failure")
            }
            emitAll(delegate.data)
        }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = delegate.updateData(transform)
}
