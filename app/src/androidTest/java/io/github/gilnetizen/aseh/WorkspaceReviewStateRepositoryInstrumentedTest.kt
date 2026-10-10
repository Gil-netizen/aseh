package io.github.gilnetizen.aseh

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gilnetizen.aseh.core.database.StoredWorkspaceState
import io.github.gilnetizen.aseh.core.database.WorkspaceStateStore
import io.github.gilnetizen.aseh.domain.workspace.CalendarItemStatus
import io.github.gilnetizen.aseh.domain.workspace.HouseholdResponsibility
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.SyntheticWorkspaceFixtures
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceUpdateResult
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceReviewStateRepositoryInstrumentedTest {
  private val context: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

  private val preferences
    get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  private lateinit var scope: CoroutineScope
  private lateinit var store: InMemoryWorkspaceStateStore

  @Before
  fun clearBefore() {
    assertTrue(preferences.edit().clear().commit())
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    store = InMemoryWorkspaceStateStore()
  }

  @After
  fun clearAfter() {
    scope.cancel()
    assertTrue(preferences.edit().clear().commit())
  }

  @Test
  fun versionedSnapshotRoundTripsAndExplicitClearRemovesPrimaryAndBackup() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val first = createRepository(fixture)
    assertTrue(first.apply(WorkspaceCommand.RenameSelf(fixture.snapshot.selfWorkspaces.single().id, "My practice")) is WorkspaceUpdateResult.Applied)
    assertTrue(first.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Applied)

    val reopened = createRepository(fixture)
    assertEquals("My practice", reopened.state.value.selfWorkspaces.single().label)
    assertEquals(InventoryItemStatus.OUT_OF_SERVICE, reopened.inventoryStatus())
    assertNotNull(store.read()?.primaryPayload)
    assertNotNull(store.read()?.backupPayload)

    reopened.clear()
    val cleared = createRepository(fixture)
    assertEquals(fixture.snapshot, cleared.state.value)
    assertNull(store.read())
    assertNull(preferences.getString(KEY_COMMAND_LOG, null))
  }

  @Test
  fun corruptPrimaryRecoversLastGoodBackupWithoutDeletingStoredPayload() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val first = createRepository(fixture)
    assertTrue(first.apply(WorkspaceCommand.RenameHousehold(fixture.snapshot.households.single().id, "Our home")) is WorkspaceUpdateResult.Applied)
    assertTrue(first.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Applied)
    val storedBeforeCorruption = requireNotNull(store.read())
    val backupBeforeCorruption = requireNotNull(storedBeforeCorruption.backupPayload)
    store.replaceRaw(storedBeforeCorruption.copy(primaryPayload = "corrupt-primary"))

    val recovered = createRepository(fixture)

    assertEquals("Our home", recovered.state.value.households.single().label)
    assertEquals(InventoryItemStatus.ACTIVE, recovered.inventoryStatus())
    assertEquals("corrupt-primary", store.read()?.primaryPayload)
    assertEquals(backupBeforeCorruption, store.read()?.backupPayload)
    assertTrue(recovered.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Rejected)
    assertEquals("corrupt-primary", store.read()?.primaryPayload)
    assertEquals(backupBeforeCorruption, store.read()?.backupPayload)
  }

  @Test
  fun corruptPrimaryAndBackupArePreservedAndBlockWritesUntilExplicitClear() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    assertTrue(
      store.importIfAbsent(
        StoredWorkspaceState(
          persistenceSchemaVersion = 2,
          fixtureId = fixture.id,
          primaryPayload = "corrupt-primary",
          backupPayload = "corrupt-backup",
          legacyCommandPayload = null,
        ),
      ),
    )
    val recovered = createRepository(fixture)

    val result = recovered.apply(
      WorkspaceCommand.RenameSelf(fixture.snapshot.selfWorkspaces.single().id, "Must not overwrite"),
    )

    assertTrue(result is WorkspaceUpdateResult.Rejected)
    assertEquals(fixture.snapshot, recovered.state.value)
    assertEquals("corrupt-primary", store.read()?.primaryPayload)
    assertEquals("corrupt-backup", store.read()?.backupPayload)
    assertTrue(
      (result as WorkspaceUpdateResult.Rejected).issues.any {
        it.path == WORKSPACE_LOCAL_PERSISTENCE_PATH
      },
    )
  }

  @Test
  fun fixtureUpgradePreservesPersistedUserSnapshot() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val first = createRepository(fixture)
    val responsibility = HouseholdResponsibility(
      id = WorkspaceRecordId("local.responsibility.persisted"),
      label = "Arrange transportation",
      dueOn = LocalDate.parse("2026-10-12"),
      statusChangedOn = LocalDate.parse("2026-10-07"),
    )
    assertTrue(
      first.apply(
        WorkspaceCommand.PutHouseholdResponsibility(
          fixture.snapshot.households.single().id,
          responsibility,
        ),
      ) is WorkspaceUpdateResult.Applied,
    )
    val expected = first.state.value
    val upgradedFixture = fixture.copy(
      id = "${fixture.id}.upgraded",
      snapshot = fixture.snapshot.copy(
        households = fixture.snapshot.households.map { it.copy(label = "Changed fixture label") },
      ),
    )

    val reopened = createRepository(upgradedFixture)

    assertEquals(expected, reopened.state.value)
    assertTrue(reopened.state.value.households.single().responsibilities.any { it == responsibility })
  }

  @Test
  fun incompatibleOperationalWrapperIsQuarantinedAndBlocksWrites() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val source = createRepository(fixture)
    assertTrue(source.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Applied)
    val valid = requireNotNull(store.read())
    store.replaceRaw(
      valid.copy(
        persistenceSchemaVersion = 99,
        fixtureId = "mismatched.fixture.wrapper",
      ),
    )

    val recovered = createRepository(fixture)

    assertEquals(fixture.snapshot, recovered.state.value)
    assertTrue(recovered.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Rejected)
    assertEquals(99, store.read()?.persistenceSchemaVersion)
    assertEquals("mismatched.fixture.wrapper", store.read()?.fixtureId)
  }

  @Test
  fun corruptAndRejectedLegacyLogsMoveToOperationalQuarantineAndBlockWrites() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    assertTrue(
      preferences.edit()
        .putString(KEY_FIXTURE_ID, fixture.id)
        .putString(KEY_COMMAND_LOG, "not-json")
        .commit(),
    )
    val corruptRecovered = createRepository(fixture)
    assertEquals(fixture.snapshot, corruptRecovered.state.value)
    assertFalse(preferences.contains(KEY_COMMAND_LOG))
    assertEquals("not-json", store.read()?.legacyCommandPayload)
    assertTrue(
      corruptRecovered.apply(
        WorkspaceCommand.RenameSelf(fixture.snapshot.selfWorkspaces.single().id, "Blocked"),
      ) is WorkspaceUpdateResult.Rejected,
    )

    corruptRecovered.clear()
    val rejectedLog = """[{"type":"inventory","workspace_id":"missing","record_id":"missing","target":"RETIRED","changed_on":"2026-10-07"}]"""
    assertTrue(
      preferences.edit()
        .putString(KEY_FIXTURE_ID, fixture.id)
        .putString(KEY_COMMAND_LOG, rejectedLog)
        .commit(),
    )
    val rejectedRecovered = createRepository(fixture)
    assertEquals(fixture.snapshot, rejectedRecovered.state.value)
    assertFalse(preferences.contains(KEY_COMMAND_LOG))
    assertEquals(rejectedLog, store.read()?.legacyCommandPayload)
    assertTrue(rejectedRecovered.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Rejected)
  }

  @Test
  fun validLegacyLogMigratesToRoomBoundaryAndRemovesPreferenceKeys() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val legacyLog = """[{"type":"inventory","workspace_id":"dev.qahal.synthetic","record_id":"dev.qahal.inventory.chairs","target":"OUT_OF_SERVICE","changed_on":"2026-10-07"}]"""
    assertTrue(
      preferences.edit()
        .putString(KEY_FIXTURE_ID, fixture.id)
        .putString(KEY_COMMAND_LOG, legacyLog)
        .commit(),
    )

    val migrated = createRepository(fixture)

    assertEquals(InventoryItemStatus.OUT_OF_SERVICE, migrated.inventoryStatus())
    assertNotNull(store.read()?.primaryPayload)
    assertEquals(null, store.read()?.legacyCommandPayload)
    assertFalse(preferences.contains(KEY_COMMAND_LOG))
    assertFalse(preferences.contains(KEY_FIXTURE_ID))
  }

  @Test
  fun mismatchedLegacyLogIsQuarantinedWithoutReplay() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val legacyLog = """[{"type":"inventory","workspace_id":"dev.qahal.synthetic","record_id":"dev.qahal.inventory.chairs","target":"OUT_OF_SERVICE","changed_on":"2026-10-07"}]"""
    assertTrue(
      preferences.edit()
        .putString(KEY_FIXTURE_ID, "older-fixture-id")
        .putString(KEY_COMMAND_LOG, legacyLog)
        .commit(),
    )

    val recovered = createRepository(fixture)

    assertEquals(InventoryItemStatus.ACTIVE, recovered.inventoryStatus())
    assertEquals("older-fixture-id", store.read()?.fixtureId)
    assertEquals(legacyLog, store.read()?.legacyCommandPayload)
    assertFalse(preferences.contains(KEY_FIXTURE_ID))
    assertFalse(preferences.contains(KEY_COMMAND_LOG))
    assertTrue(recovered.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Rejected)
  }

  @Test
  fun unmarkedLegacyLogIsQuarantinedWithoutReplay() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val legacyLog = """[{"type":"inventory","workspace_id":"dev.qahal.synthetic","record_id":"dev.qahal.inventory.chairs","target":"OUT_OF_SERVICE","changed_on":"2026-10-07"}]"""
    assertTrue(preferences.edit().putString(KEY_COMMAND_LOG, legacyLog).commit())

    val recovered = createRepository(fixture)

    assertEquals(InventoryItemStatus.ACTIVE, recovered.inventoryStatus())
    assertEquals(legacyLog, store.read()?.legacyCommandPayload)
    assertTrue(store.read()?.fixtureId != fixture.id)
    assertFalse(preferences.contains(KEY_COMMAND_LOG))
    assertTrue(recovered.apply(outOfServiceCommand()) is WorkspaceUpdateResult.Rejected)
  }

  @Test
  fun markerOnlyLegacyStateIsRemovedWithoutCreatingOperationalState() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    assertTrue(preferences.edit().putString(KEY_FIXTURE_ID, fixture.id).commit())

    val recovered = createRepository(fixture)

    assertEquals(fixture.snapshot, recovered.state.value)
    assertNull(store.read())
    assertFalse(preferences.contains(KEY_FIXTURE_ID))
  }

  @Test
  fun granularEditorApplyUsesLatestRepositorySnapshot() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val repository = createRepository(fixture)
    val household = fixture.snapshot.households.single()
    val calendar = household.calendarItems.single()
    val staleEditorCommand = WorkspaceCommand.PutHouseholdResponsibility(
      household.id,
      HouseholdResponsibility(
        id = WorkspaceRecordId("local.responsibility.race"),
        label = "Arrange transportation",
        dueOn = LocalDate.parse("2026-10-12"),
        statusChangedOn = LocalDate.parse("2026-10-07"),
      ),
    )
    assertTrue(
      repository.apply(
        WorkspaceCommand.ChangeCalendarItemStatus(
          household.id,
          calendar.id,
          CalendarItemStatus.COMPLETED,
          LocalDate.parse("2026-10-10"),
        ),
      ) is WorkspaceUpdateResult.Applied,
    )

    assertTrue(repository.apply(staleEditorCommand) is WorkspaceUpdateResult.Applied)

    val reopened = createRepository(fixture)
    assertEquals(CalendarItemStatus.COMPLETED, reopened.state.value.households.single().calendarItems.single().status)
    assertTrue(
      reopened.state.value.households.single().responsibilities.any {
        it.id == WorkspaceRecordId("local.responsibility.race")
      },
    )
  }

  private fun outOfServiceCommand() = WorkspaceCommand.ChangeInventoryStatus(
    qahalId = WorkspaceId("dev.qahal.synthetic"),
    id = WorkspaceRecordId("dev.qahal.inventory.chairs"),
    target = InventoryItemStatus.OUT_OF_SERVICE,
    changedOn = LocalDate.parse("2026-10-07"),
  )

  private fun WorkspaceReviewStateRepository.inventoryStatus(): InventoryItemStatus =
    state.value.qahalWorkspaces.single().inventory.single().status

  private suspend fun createRepository(
    fixture: io.github.gilnetizen.aseh.domain.workspace.WorkspaceFixture,
  ): WorkspaceReviewStateRepository = WorkspaceReviewStateRepositoryFactory.create(
    context = context,
    fixture = fixture,
    workspaceStateStore = store,
    scope = scope,
  ).also { repository -> repository.awaitReady() }

  private class InMemoryWorkspaceStateStore : WorkspaceStateStore {
    private val mutex = Mutex()
    private var stored: StoredWorkspaceState? = null

    override suspend fun read(): StoredWorkspaceState? = mutex.withLock { stored }

    override suspend fun importIfAbsent(state: StoredWorkspaceState): Boolean = mutex.withLock {
      if (stored != null) return@withLock false
      stored = state
      true
    }

    override suspend fun save(
      persistenceSchemaVersion: Int,
      fixtureId: String,
      primaryPayload: String,
      backupPayload: String,
    ) = mutex.withLock {
      stored = StoredWorkspaceState(
        persistenceSchemaVersion = persistenceSchemaVersion,
        fixtureId = fixtureId,
        primaryPayload = primaryPayload,
        backupPayload = backupPayload,
        legacyCommandPayload = null,
      )
    }

    override suspend fun discardLegacyCommandPayload() = mutex.withLock {
      stored = stored?.copy(legacyCommandPayload = null)
    }

    override suspend fun clear() = mutex.withLock {
      stored = null
    }

    suspend fun replaceRaw(state: StoredWorkspaceState) = mutex.withLock {
      stored = state
    }
  }

  private companion object {
    const val PREFERENCES_NAME = "aseh_workspace_review_state"
    const val KEY_FIXTURE_ID = "fixture_id"
    const val KEY_COMMAND_LOG = "command_log"
  }
}
