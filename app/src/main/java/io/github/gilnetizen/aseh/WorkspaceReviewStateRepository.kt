package io.github.gilnetizen.aseh

import android.content.Context
import android.content.SharedPreferences
import io.github.gilnetizen.aseh.core.database.StoredWorkspaceState
import io.github.gilnetizen.aseh.core.database.WorkspaceStateStore
import io.github.gilnetizen.aseh.domain.workspace.CalendarItemStatus
import io.github.gilnetizen.aseh.domain.workspace.DecisionClassification
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlChecklist
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlItem
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlKind
import io.github.gilnetizen.aseh.domain.workspace.FinancialChecklistStatus
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlStatus
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarItem
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarKind
import io.github.gilnetizen.aseh.domain.workspace.HouseholdPreparationKit
import io.github.gilnetizen.aseh.domain.workspace.HouseholdResponsibility
import io.github.gilnetizen.aseh.domain.workspace.HouseholdWorkspace
import io.github.gilnetizen.aseh.domain.workspace.InventoryItem
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.LocalPersonId
import io.github.gilnetizen.aseh.domain.workspace.LocalWorkspaceApi
import io.github.gilnetizen.aseh.domain.workspace.PersonalPracticeAdoption
import io.github.gilnetizen.aseh.domain.workspace.PracticeCardReference
import io.github.gilnetizen.aseh.domain.workspace.PracticeAdoptionStatus
import io.github.gilnetizen.aseh.domain.workspace.PreparationKitTask
import io.github.gilnetizen.aseh.domain.workspace.PreparationKitStatus
import io.github.gilnetizen.aseh.domain.workspace.PublicRoleId
import io.github.gilnetizen.aseh.domain.workspace.QahalDecision
import io.github.gilnetizen.aseh.domain.workspace.QahalDecisionStatus
import io.github.gilnetizen.aseh.domain.workspace.QahalWorkspace
import io.github.gilnetizen.aseh.domain.workspace.SelfWorkspace
import io.github.gilnetizen.aseh.domain.workspace.VerifiedContentReference
import io.github.gilnetizen.aseh.domain.workspace.VolunteerRotation
import io.github.gilnetizen.aseh.domain.workspace.VolunteerRotationStatus
import io.github.gilnetizen.aseh.domain.workspace.VolunteerSlot
import io.github.gilnetizen.aseh.domain.workspace.VolunteerSlotStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkItemStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceFixture
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssue
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssueCode
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceUpdateResult
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Persists a validated, versioned local snapshot with a last-good recovery copy. */
interface WorkspaceReviewStateRepository {
  val state: StateFlow<WorkspaceSnapshot>
  val fixtureNotice: String

  suspend fun awaitReady()

  suspend fun apply(command: WorkspaceCommand): WorkspaceUpdateResult

  suspend fun clear()
}

internal const val WORKSPACE_LOCAL_PERSISTENCE_PATH = "localPersistence"
private const val WORKSPACE_SNAPSHOT_SCHEMA_VERSION = 2

internal object WorkspaceReviewStateRepositoryFactory {
  fun create(
    context: Context,
    fixture: WorkspaceFixture,
    workspaceStateStore: WorkspaceStateStore,
    scope: CoroutineScope,
  ): WorkspaceReviewStateRepository = RoomWorkspaceReviewStateRepository(
    legacyPreferences = context.applicationContext.getSharedPreferences(
      LEGACY_WORKSPACE_PREFERENCES_NAME,
      Context.MODE_PRIVATE,
    ),
    fixture = fixture,
    workspaceStateStore = workspaceStateStore,
    scope = scope,
  )

  internal const val LEGACY_WORKSPACE_PREFERENCES_NAME = "aseh_workspace_review_state"
}

private class RoomWorkspaceReviewStateRepository(
  private val legacyPreferences: SharedPreferences,
  private val fixture: WorkspaceFixture,
  private val workspaceStateStore: WorkspaceStateStore,
  scope: CoroutineScope,
) : WorkspaceReviewStateRepository {
  private val mutex = Mutex()
  private val mutableState = MutableStateFlow(fixture.snapshot)
  private var writesBlockedByCorruptState = false
  private val initialization = scope.async(Dispatchers.IO) {
    val loadedState = mutex.withLock { load() }
    mutableState.value = loadedState.snapshot
    writesBlockedByCorruptState = loadedState.writesBlocked
  }

  override val state: StateFlow<WorkspaceSnapshot> = mutableState.asStateFlow()
  override val fixtureNotice: String = fixture.notice

  override suspend fun awaitReady() {
    initialization.await()
  }

  override suspend fun apply(command: WorkspaceCommand): WorkspaceUpdateResult =
    withContext(Dispatchers.IO) {
      awaitReady()
      mutex.withLock {
        if (writesBlockedByCorruptState) {
          return@withLock WorkspaceUpdateResult.Rejected(
            unchangedSnapshot = mutableState.value,
            issues = listOf(persistenceIssue("Stored workspace recovery data is unreadable and was preserved.")),
          )
        }
        when (val result = LocalWorkspaceApi.apply(mutableState.value, command)) {
          is WorkspaceUpdateResult.Rejected -> result
          is WorkspaceUpdateResult.Applied -> {
            val persisted = persistSnapshot(result.snapshot)
            if (!persisted) {
              WorkspaceUpdateResult.Rejected(
                unchangedSnapshot = mutableState.value,
                issues = listOf(persistenceIssue("The local workspace change could not be persisted.")),
              )
            } else {
              mutableState.value = result.snapshot
              result
            }
          }
        }
      }
    }

  override suspend fun clear() = withContext(Dispatchers.IO) {
    awaitReady()
    mutex.withLock {
      clearLegacyPreferences()
      workspaceStateStore.clear()
      writesBlockedByCorruptState = false
      mutableState.value = fixture.snapshot
    }
  }

  private suspend fun load(): LoadedWorkspaceState = runCatching {
    var stored = workspaceStateStore.read()
    if (stored == null) {
      val legacy = legacyState()
      if (legacy != null) {
        workspaceStateStore.importIfAbsent(legacy)
        stored = checkNotNull(workspaceStateStore.read()) {
          "The imported workspace state could not be read back."
        }
        clearLegacyPreferences()
      } else if (hasLegacyPreferences()) {
        // A fixture marker without a payload is the residue of an already-completed legacy
        // cleanup. It is not operational state and must not keep the one-time importer alive.
        clearLegacyPreferences()
      }
    } else if (hasLegacyPreferences()) {
      // A previous import reached Room before the process stopped. Removing the source is the
      // idempotent second half of the cross-store migration.
      clearLegacyPreferences()
    }
    stored?.let { decodeStoredState(it) } ?: LoadedWorkspaceState(fixture.snapshot)
  }.getOrElse {
    LoadedWorkspaceState(snapshot = fixture.snapshot, writesBlocked = true)
  }

  private suspend fun decodeStoredState(stored: StoredWorkspaceState): LoadedWorkspaceState {
    if (stored.persistenceSchemaVersion != WORKSPACE_SNAPSHOT_SCHEMA_VERSION) {
      return LoadedWorkspaceState(snapshot = fixture.snapshot, writesBlocked = true)
    }
    val primary = decodeEnvelope(stored.primaryPayload, stored.fixtureId)
    val backup = decodeEnvelope(stored.backupPayload, stored.fixtureId)
    val hasUnreadableEnvelope =
      (stored.primaryPayload != null && primary == null) ||
        (stored.backupPayload != null && backup == null)
    if (hasUnreadableEnvelope) {
      return LoadedWorkspaceState(
        snapshot = primary ?: backup ?: fixture.snapshot,
        writesBlocked = true,
      )
    }
    primary?.let {
      if (stored.legacyCommandPayload != null) workspaceStateStore.discardLegacyCommandPayload()
      return LoadedWorkspaceState(it)
    }
    backup?.let {
      if (stored.legacyCommandPayload != null) workspaceStateStore.discardLegacyCommandPayload()
      return LoadedWorkspaceState(it)
    }
    stored.legacyCommandPayload?.let { encoded ->
      if (stored.fixtureId != fixture.id) {
        return LoadedWorkspaceState(snapshot = fixture.snapshot, writesBlocked = true)
      }
      migrateLegacyCommandLog(encoded)?.let { return LoadedWorkspaceState(it) }
      return LoadedWorkspaceState(snapshot = fixture.snapshot, writesBlocked = true)
    }
    return LoadedWorkspaceState(
      snapshot = fixture.snapshot,
      writesBlocked = false,
    )
  }

  private fun decodeEnvelope(encoded: String?, expectedFixtureId: String): WorkspaceSnapshot? {
    encoded ?: return null
    return runCatching { WorkspaceCommandJson.decodeSnapshot(encoded, expectedFixtureId) }
      .getOrNull()
      ?.takeIf { LocalWorkspaceApi.validate(it).isEmpty() }
  }

  private suspend fun migrateLegacyCommandLog(encoded: String): WorkspaceSnapshot? {
    val decoded = runCatching { WorkspaceCommandJson.decode(encoded) }.getOrNull() ?: return null
    var snapshot = fixture.snapshot
    var complete = true
    decoded.forEach { command ->
      snapshot = when (val result = LocalWorkspaceApi.apply(snapshot, command)) {
        is WorkspaceUpdateResult.Applied -> result.snapshot
        is WorkspaceUpdateResult.Rejected -> {
          complete = false
          snapshot
        }
      }
    }
    if (complete) {
      workspaceStateStore.save(
        persistenceSchemaVersion = WORKSPACE_SNAPSHOT_SCHEMA_VERSION,
        fixtureId = fixture.id,
        primaryPayload = WorkspaceCommandJson.encodeSnapshot(fixture.id, snapshot),
        backupPayload = WorkspaceCommandJson.encodeSnapshot(fixture.id, fixture.snapshot),
      )
      return snapshot
    }
    return null
  }

  private suspend fun persistSnapshot(snapshot: WorkspaceSnapshot): Boolean = runCatching {
    workspaceStateStore.save(
      persistenceSchemaVersion = WORKSPACE_SNAPSHOT_SCHEMA_VERSION,
      fixtureId = fixture.id,
      primaryPayload = WorkspaceCommandJson.encodeSnapshot(fixture.id, snapshot),
      backupPayload = WorkspaceCommandJson.encodeSnapshot(fixture.id, mutableState.value),
    )
  }.isSuccess

  private fun legacyState(): StoredWorkspaceState? {
    val primary = legacyPreferences.getString(KEY_STATE_ENVELOPE, null)
    val backup = legacyPreferences.getString(KEY_STATE_BACKUP, null)
    val commands = legacyPreferences.getString(KEY_COMMAND_LOG, null)
    if (primary == null && backup == null && commands == null) return null
    return StoredWorkspaceState(
      persistenceSchemaVersion = WORKSPACE_SNAPSHOT_SCHEMA_VERSION,
      fixtureId = listOfNotNull(primary, backup)
        .firstNotNullOfOrNull { payload -> WorkspaceCommandJson.fixtureIdOrNull(payload) }
        ?: legacyPreferences.getString(KEY_FIXTURE_ID, null)
        ?.takeIf(String::isNotBlank)
        ?: LEGACY_COMMAND_FIXTURE_ID_MISSING,
      primaryPayload = primary,
      backupPayload = backup,
      legacyCommandPayload = commands,
    )
  }

  private fun hasLegacyPreferences(): Boolean =
    legacyPreferences.contains(KEY_STATE_ENVELOPE) ||
      legacyPreferences.contains(KEY_STATE_BACKUP) ||
      legacyPreferences.contains(KEY_FIXTURE_ID) ||
      legacyPreferences.contains(KEY_COMMAND_LOG)

  private fun clearLegacyPreferences() {
    if (!hasLegacyPreferences()) return
    check(
      legacyPreferences.edit()
        .remove(KEY_STATE_ENVELOPE)
        .remove(KEY_STATE_BACKUP)
        .remove(KEY_FIXTURE_ID)
        .remove(KEY_COMMAND_LOG)
        .commit(),
    ) { "The migrated workspace preference state could not be removed." }
  }

  private fun persistenceIssue(detail: String) = WorkspaceIssue(
    code = WorkspaceIssueCode.STATUS_REQUIREMENT_NOT_MET,
    path = WORKSPACE_LOCAL_PERSISTENCE_PATH,
    detail = detail,
  )

  private data class LoadedWorkspaceState(
    val snapshot: WorkspaceSnapshot,
    val writesBlocked: Boolean = false,
  )

  private companion object {
    const val KEY_STATE_ENVELOPE = "workspace_state_v2"
    const val KEY_STATE_BACKUP = "workspace_state_v2_backup"
    const val KEY_FIXTURE_ID = "fixture_id"
    const val KEY_COMMAND_LOG = "command_log"
    const val LEGACY_COMMAND_FIXTURE_ID_MISSING = "legacy-command-fixture-id-missing"
  }
}

/** Strict, typed codec for the local commands emitted by WorkspaceDashboard. */
private object WorkspaceCommandJson {
  fun encodeSnapshot(fixtureId: String, snapshot: WorkspaceSnapshot): String = JSONObject().apply {
    put(SCHEMA_VERSION, WORKSPACE_SNAPSHOT_SCHEMA_VERSION)
    put(FIXTURE_ID, fixtureId)
    put(SNAPSHOT, snapshot.toJson())
  }.toString()

  fun decodeSnapshot(encoded: String, expectedFixtureId: String): WorkspaceSnapshot {
    val envelope = JSONObject(encoded)
    envelope.requireKeys(SCHEMA_VERSION, FIXTURE_ID, SNAPSHOT)
    require(envelope.requiredInt(SCHEMA_VERSION) == WORKSPACE_SNAPSHOT_SCHEMA_VERSION) {
      "Unsupported workspace-state schema version."
    }
    require(envelope.requiredString(FIXTURE_ID) == expectedFixtureId) {
      "Workspace-state fixture identity does not match its operational wrapper."
    }
    return envelope.getJSONObject(SNAPSHOT).toWorkspaceSnapshot()
  }

  fun fixtureIdOrNull(encoded: String): String? = runCatching {
    JSONObject(encoded).requiredString(FIXTURE_ID)
  }.getOrNull()

  fun encode(commands: List<WorkspaceCommand>): String = JSONArray().apply {
    commands.forEach { put(it.toJson()) }
  }.toString()

  fun decode(encoded: String): List<WorkspaceCommand> {
    val array = JSONArray(encoded)
    return List(array.length()) { index -> array.getJSONObject(index).toCommand() }
  }

  private fun WorkspaceCommand.toJson(): JSONObject = JSONObject().apply {
    when (this@toJson) {
      is WorkspaceCommand.PutSelf -> {
        put(TYPE, "put_self")
        put(WORKSPACE, self.toJson())
      }
      is WorkspaceCommand.PutHousehold -> {
        put(TYPE, "put_household")
        put(WORKSPACE, household.toJson())
      }
      is WorkspaceCommand.PutQahal -> {
        put(TYPE, "put_qahal")
        put(WORKSPACE, qahal.toJson())
      }
      is WorkspaceCommand.ChangePracticeStatus -> {
        put(TYPE, "practice")
        put(WORKSPACE_ID, selfId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
        putOptional(NEXT_REVIEW_ON, nextReviewOn)
      }
      is WorkspaceCommand.ChangeResponsibilityStatus -> {
        put(TYPE, "responsibility")
        put(WORKSPACE_ID, householdId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
      }
      is WorkspaceCommand.ChangeCalendarItemStatus -> {
        put(TYPE, "calendar")
        put(WORKSPACE_ID, householdId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
      }
      is WorkspaceCommand.ChangePreparationTaskStatus -> {
        put(TYPE, "preparation_task")
        put(WORKSPACE_ID, householdId.value)
        put(PARENT_ID, kitId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
      }
      is WorkspaceCommand.ChangePreparationKitStatus -> {
        put(TYPE, "preparation_kit")
        put(WORKSPACE_ID, householdId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
      }
      is WorkspaceCommand.ChangeDecisionStatus -> {
        put(TYPE, "decision")
        put(WORKSPACE_ID, qahalId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
        putOptional(NEXT_REVIEW_ON, nextReviewOn)
      }
      is WorkspaceCommand.ChangeVolunteerSlotStatus -> {
        put(TYPE, "volunteer_slot")
        put(WORKSPACE_ID, qahalId.value)
        put(PARENT_ID, rotationId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
        assignedTo?.let { put(ASSIGNED_TO, it.value) }
      }
      is WorkspaceCommand.ChangeVolunteerRotationStatus -> {
        put(TYPE, "volunteer_rotation")
        put(WORKSPACE_ID, qahalId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
      }
      is WorkspaceCommand.ChangeInventoryStatus -> {
        put(TYPE, "inventory")
        put(WORKSPACE_ID, qahalId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
      }
      is WorkspaceCommand.ChangeFinancialControlStatus -> {
        put(TYPE, "financial_control")
        put(WORKSPACE_ID, qahalId.value)
        put(PARENT_ID, checklistId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
      }
      is WorkspaceCommand.ChangeFinancialChecklistStatus -> {
        put(TYPE, "financial_checklist")
        put(WORKSPACE_ID, qahalId.value)
        put(RECORD_ID, id.value)
        put(TARGET, target.name)
        put(CHANGED_ON, changedOn.toString())
        putOptional(NEXT_REVIEW_ON, nextReviewOn)
      }
      is WorkspaceCommand.PutPersonalPractice,
      is WorkspaceCommand.RenameSelf,
      is WorkspaceCommand.RenameHousehold,
      is WorkspaceCommand.RenameQahal,
      is WorkspaceCommand.PutHouseholdResponsibility,
      is WorkspaceCommand.DeleteHouseholdResponsibility,
      is WorkspaceCommand.PutHouseholdCalendarItem,
      is WorkspaceCommand.DeleteHouseholdCalendarItem,
      is WorkspaceCommand.PutHouseholdPreparationKit,
      is WorkspaceCommand.DeleteHouseholdPreparationKit,
      is WorkspaceCommand.PutPreparationKitTask,
      is WorkspaceCommand.DeletePreparationKitTask,
      is WorkspaceCommand.PutQahalDecision,
      is WorkspaceCommand.DeleteQahalDecision,
      is WorkspaceCommand.PutInventoryItem,
      is WorkspaceCommand.DeleteInventoryItem,
      is WorkspaceCommand.PutVolunteerRotation,
      is WorkspaceCommand.DeleteVolunteerRotation,
      is WorkspaceCommand.PutVolunteerSlot,
      is WorkspaceCommand.DeleteVolunteerSlot,
      is WorkspaceCommand.PutFinancialChecklist,
      is WorkspaceCommand.DeleteFinancialChecklist,
      is WorkspaceCommand.PutFinancialControl,
      is WorkspaceCommand.DeleteFinancialControl,
      -> error("Workspace creation commands are not emitted by the review dashboard.")
    }
  }

  private fun JSONObject.toCommand(): WorkspaceCommand {
    return when (requiredString(TYPE)) {
      "put_self" -> {
        requireKeys(TYPE, WORKSPACE)
        WorkspaceCommand.PutSelf(getJSONObject(WORKSPACE).toSelfWorkspace())
      }
      "put_household" -> {
        requireKeys(TYPE, WORKSPACE)
        WorkspaceCommand.PutHousehold(getJSONObject(WORKSPACE).toHouseholdWorkspace())
      }
      "put_qahal" -> {
        requireKeys(TYPE, WORKSPACE)
        WorkspaceCommand.PutQahal(getJSONObject(WORKSPACE).toQahalWorkspace())
      }
      "practice" -> WorkspaceCommand.ChangePracticeStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
        optionalDate(NEXT_REVIEW_ON),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON, optional = setOf(NEXT_REVIEW_ON)) }
      "responsibility" -> WorkspaceCommand.ChangeResponsibilityStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<WorkItemStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON) }
      "calendar" -> WorkspaceCommand.ChangeCalendarItemStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<CalendarItemStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON) }
      "preparation_task" -> WorkspaceCommand.ChangePreparationTaskStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(PARENT_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<WorkItemStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
      ).also { requireKeys(TYPE, WORKSPACE_ID, PARENT_ID, RECORD_ID, TARGET, CHANGED_ON) }
      "preparation_kit" -> WorkspaceCommand.ChangePreparationKitStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<PreparationKitStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON) }
      "decision" -> WorkspaceCommand.ChangeDecisionStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<QahalDecisionStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
        optionalDate(NEXT_REVIEW_ON),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON, optional = setOf(NEXT_REVIEW_ON)) }
      "volunteer_slot" -> WorkspaceCommand.ChangeVolunteerSlotStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(PARENT_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<VolunteerSlotStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
        optionalString(ASSIGNED_TO)?.let(::LocalPersonId),
      ).also {
        requireKeys(TYPE, WORKSPACE_ID, PARENT_ID, RECORD_ID, TARGET, CHANGED_ON, optional = setOf(ASSIGNED_TO))
      }
      "volunteer_rotation" -> WorkspaceCommand.ChangeVolunteerRotationStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<VolunteerRotationStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON) }
      "inventory" -> WorkspaceCommand.ChangeInventoryStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<InventoryItemStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON) }
      "financial_control" -> WorkspaceCommand.ChangeFinancialControlStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(PARENT_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<FinancialControlStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
      ).also { requireKeys(TYPE, WORKSPACE_ID, PARENT_ID, RECORD_ID, TARGET, CHANGED_ON) }
      "financial_checklist" -> WorkspaceCommand.ChangeFinancialChecklistStatus(
        WorkspaceId(requiredString(WORKSPACE_ID)),
        WorkspaceRecordId(requiredString(RECORD_ID)),
        enumValueOf<FinancialChecklistStatus>(requiredString(TARGET)),
        LocalDate.parse(requiredString(CHANGED_ON)),
        optionalDate(NEXT_REVIEW_ON),
      ).also { requireKeys(TYPE, WORKSPACE_ID, RECORD_ID, TARGET, CHANGED_ON, optional = setOf(NEXT_REVIEW_ON)) }
      else -> error("Unsupported workspace command type.")
    }
  }

  private fun WorkspaceSnapshot.toJson(): JSONObject = JSONObject().apply {
    put(SELF_WORKSPACES, selfWorkspaces.toJsonArray { it.toJson() })
    put(HOUSEHOLDS, households.toJsonArray { it.toJson() })
    put(QAHAL_WORKSPACES, qahalWorkspaces.toJsonArray { it.toJson() })
  }

  private fun JSONObject.toWorkspaceSnapshot(): WorkspaceSnapshot {
    requireKeys(SELF_WORKSPACES, HOUSEHOLDS, QAHAL_WORKSPACES)
    return WorkspaceSnapshot(
      selfWorkspaces = getJSONArray(SELF_WORKSPACES).mapObjects { it.toSelfWorkspace() },
      households = getJSONArray(HOUSEHOLDS).mapObjects { it.toHouseholdWorkspace() },
      qahalWorkspaces = getJSONArray(QAHAL_WORKSPACES).mapObjects { it.toQahalWorkspace() },
    )
  }

  private fun SelfWorkspace.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(PRACTICE_ADOPTIONS, practiceAdoptions.toJsonArray { it.toJson() })
  }

  private fun PersonalPracticeAdoption.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(PRACTICE, practice.toJson())
    put(STATUS, status.name)
    putOptional(ADOPTED_ON, adoptedOn)
    putOptional(REVIEW_ON, reviewOn)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
  }

  private fun PracticeCardReference.toJson(): JSONObject = JSONObject().apply {
    put(PRACTICE_CARD_ID, practiceCardId)
    put(CONTENT, content.toJson())
  }

  private fun VerifiedContentReference.toJson(): JSONObject = JSONObject().apply {
    put(PACK_ID, packId)
    put(PACK_VERSION, packVersion)
    put(EDITION_ID, editionId)
    put(SOURCE_UNIT_ID, sourceUnitId)
  }

  private fun HouseholdWorkspace.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(RESPONSIBILITIES, responsibilities.toJsonArray { it.toJson() })
    put(CALENDAR_ITEMS, calendarItems.toJsonArray { it.toJson() })
    put(PREPARATION_KITS, preparationKits.toJsonArray { it.toJson() })
  }

  private fun HouseholdResponsibility.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(DUE_ON, dueOn.toString())
    putOptionalString(ASSIGNED_TO, assignedTo?.value)
    put(STATUS, status.name)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
  }

  private fun HouseholdCalendarItem.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(KIND, kind.name)
    put(STARTS_ON, startsOn.toString())
    put(ENDS_ON, endsOn.toString())
    put(STATUS, status.name)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
  }

  private fun HouseholdPreparationKit.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(TARGET_ON, targetOn.toString())
    put(STATUS, status.name)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
    put(TASKS, tasks.toJsonArray { it.toJson() })
  }

  private fun PreparationKitTask.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(DUE_ON, dueOn.toString())
    putOptionalString(ASSIGNED_TO, assignedTo?.value)
    put(STATUS, status.name)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
  }

  private fun QahalWorkspace.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(DECISIONS, decisions.toJsonArray { it.toJson() })
    put(VOLUNTEER_ROTATIONS, volunteerRotations.toJsonArray { it.toJson() })
    put(INVENTORY_ITEMS, inventory.toJsonArray { it.toJson() })
    put(FINANCIAL_CONTROLS, financialControls.toJsonArray { it.toJson() })
  }

  private fun QahalDecision.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(CLASSIFICATION, classification.name)
    put(PUBLIC_SUMMARY, publicSummary)
    put(AUTHORITY_SCOPE, authorityScope)
    put(STATUS, status.name)
    putOptional(EFFECTIVE_ON, effectiveOn)
    putOptional(REVIEW_ON, reviewOn)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
    put(SOURCE_REFERENCES, sourceReferences.toJsonArray { it.toJson() })
    putOptionalString(DISSENT_SUMMARY, dissentSummary)
  }

  private fun VolunteerRotation.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(PUBLIC_ROLE, publicRole.value)
    put(STATUS, status.name)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
    put(SLOTS, slots.toJsonArray { it.toJson() })
  }

  private fun VolunteerSlot.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(SERVICE_ON, serviceOn.toString())
    putOptionalString(ASSIGNED_TO, assignedTo?.value)
    put(STATUS, status.name)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
  }

  private fun InventoryItem.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(QUANTITY_ON_HAND, quantityOnHand)
    put(MINIMUM_DESIRED, minimumDesired)
    putOptional(NEXT_CHECK_ON, nextCheckOn)
    put(STATUS, status.name)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
  }

  private fun FinancialControlChecklist.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(LABEL, label)
    put(STATUS, status.name)
    putOptional(REVIEW_ON, reviewOn)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
    put(CONTROLS, controls.toJsonArray { it.toJson() })
  }

  private fun FinancialControlItem.toJson(): JSONObject = JSONObject().apply {
    put(ID, id.value)
    put(KIND, kind.name)
    put(STATUS, status.name)
    putOptional(DUE_ON, dueOn)
    putOptionalString(RESPONSIBLE_ROLE, responsibleRole?.value)
    put(STATUS_CHANGED_ON, statusChangedOn.toString())
  }

  private fun JSONObject.toSelfWorkspace(): SelfWorkspace {
    requireKeys(ID, LABEL, PRACTICE_ADOPTIONS)
    return SelfWorkspace(
      id = WorkspaceId(requiredString(ID)),
      label = requiredString(LABEL),
      practiceAdoptions = getJSONArray(PRACTICE_ADOPTIONS).mapObjects { it.toPersonalPracticeAdoption() },
    )
  }

  private fun JSONObject.toPersonalPracticeAdoption(): PersonalPracticeAdoption {
    requireKeys(
      ID,
      LABEL,
      PRACTICE,
      STATUS,
      STATUS_CHANGED_ON,
      optional = setOf(ADOPTED_ON, REVIEW_ON),
    )
    return PersonalPracticeAdoption(
      id = WorkspaceRecordId(requiredString(ID)),
      practice = getJSONObject(PRACTICE).toPracticeCardReference(),
      label = requiredString(LABEL),
      status = enumValueOf(requiredString(STATUS)),
      adoptedOn = optionalDate(ADOPTED_ON),
      reviewOn = optionalDate(REVIEW_ON),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
    )
  }

  private fun JSONObject.toPracticeCardReference(): PracticeCardReference {
    requireKeys(PRACTICE_CARD_ID, CONTENT)
    return PracticeCardReference(
      practiceCardId = requiredString(PRACTICE_CARD_ID),
      content = getJSONObject(CONTENT).toVerifiedContentReference(),
    )
  }

  private fun JSONObject.toVerifiedContentReference(): VerifiedContentReference {
    requireKeys(PACK_ID, PACK_VERSION, EDITION_ID, SOURCE_UNIT_ID)
    return VerifiedContentReference(
      packId = requiredString(PACK_ID),
      packVersion = requiredString(PACK_VERSION),
      editionId = requiredString(EDITION_ID),
      sourceUnitId = requiredString(SOURCE_UNIT_ID),
    )
  }

  private fun JSONObject.toHouseholdWorkspace(): HouseholdWorkspace {
    requireKeys(ID, LABEL, RESPONSIBILITIES, CALENDAR_ITEMS, PREPARATION_KITS)
    return HouseholdWorkspace(
      id = WorkspaceId(requiredString(ID)),
      label = requiredString(LABEL),
      responsibilities = getJSONArray(RESPONSIBILITIES).mapObjects { it.toHouseholdResponsibility() },
      calendarItems = getJSONArray(CALENDAR_ITEMS).mapObjects { it.toHouseholdCalendarItem() },
      preparationKits = getJSONArray(PREPARATION_KITS).mapObjects { it.toHouseholdPreparationKit() },
    )
  }

  private fun JSONObject.toHouseholdResponsibility(): HouseholdResponsibility {
    requireKeys(ID, LABEL, DUE_ON, STATUS, STATUS_CHANGED_ON, optional = setOf(ASSIGNED_TO))
    return HouseholdResponsibility(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      dueOn = LocalDate.parse(requiredString(DUE_ON)),
      assignedTo = optionalString(ASSIGNED_TO)?.let(::LocalPersonId),
      status = enumValueOf(requiredString(STATUS)),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
    )
  }

  private fun JSONObject.toHouseholdCalendarItem(): HouseholdCalendarItem {
    requireKeys(ID, LABEL, KIND, STARTS_ON, ENDS_ON, STATUS, STATUS_CHANGED_ON)
    return HouseholdCalendarItem(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      kind = enumValueOf<HouseholdCalendarKind>(requiredString(KIND)),
      startsOn = LocalDate.parse(requiredString(STARTS_ON)),
      endsOn = LocalDate.parse(requiredString(ENDS_ON)),
      status = enumValueOf(requiredString(STATUS)),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
    )
  }

  private fun JSONObject.toHouseholdPreparationKit(): HouseholdPreparationKit {
    requireKeys(ID, LABEL, TARGET_ON, STATUS, STATUS_CHANGED_ON, TASKS)
    return HouseholdPreparationKit(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      targetOn = LocalDate.parse(requiredString(TARGET_ON)),
      status = enumValueOf(requiredString(STATUS)),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
      tasks = getJSONArray(TASKS).mapObjects { it.toPreparationKitTask() },
    )
  }

  private fun JSONObject.toPreparationKitTask(): PreparationKitTask {
    requireKeys(ID, LABEL, DUE_ON, STATUS, STATUS_CHANGED_ON, optional = setOf(ASSIGNED_TO))
    return PreparationKitTask(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      dueOn = LocalDate.parse(requiredString(DUE_ON)),
      assignedTo = optionalString(ASSIGNED_TO)?.let(::LocalPersonId),
      status = enumValueOf(requiredString(STATUS)),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
    )
  }

  private fun JSONObject.toQahalWorkspace(): QahalWorkspace {
    requireKeys(ID, LABEL, DECISIONS, VOLUNTEER_ROTATIONS, INVENTORY_ITEMS, FINANCIAL_CONTROLS)
    return QahalWorkspace(
      id = WorkspaceId(requiredString(ID)),
      label = requiredString(LABEL),
      decisions = getJSONArray(DECISIONS).mapObjects { it.toQahalDecision() },
      volunteerRotations = getJSONArray(VOLUNTEER_ROTATIONS).mapObjects { it.toVolunteerRotation() },
      inventory = getJSONArray(INVENTORY_ITEMS).mapObjects { it.toInventoryItem() },
      financialControls = getJSONArray(FINANCIAL_CONTROLS).mapObjects { it.toFinancialControlChecklist() },
    )
  }

  private fun JSONObject.toQahalDecision(): QahalDecision {
    requireKeys(
      ID,
      LABEL,
      CLASSIFICATION,
      PUBLIC_SUMMARY,
      AUTHORITY_SCOPE,
      STATUS,
      STATUS_CHANGED_ON,
      SOURCE_REFERENCES,
      optional = setOf(EFFECTIVE_ON, REVIEW_ON, DISSENT_SUMMARY),
    )
    return QahalDecision(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      classification = enumValueOf<DecisionClassification>(requiredString(CLASSIFICATION)),
      publicSummary = stringValue(PUBLIC_SUMMARY),
      authorityScope = stringValue(AUTHORITY_SCOPE),
      status = enumValueOf(requiredString(STATUS)),
      effectiveOn = optionalDate(EFFECTIVE_ON),
      reviewOn = optionalDate(REVIEW_ON),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
      sourceReferences = getJSONArray(SOURCE_REFERENCES).mapObjects { it.toVerifiedContentReference() },
      dissentSummary = optionalString(DISSENT_SUMMARY),
    )
  }

  private fun JSONObject.toVolunteerRotation(): VolunteerRotation {
    requireKeys(ID, LABEL, PUBLIC_ROLE, STATUS, STATUS_CHANGED_ON, SLOTS)
    return VolunteerRotation(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      publicRole = PublicRoleId(requiredString(PUBLIC_ROLE)),
      status = enumValueOf(requiredString(STATUS)),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
      slots = getJSONArray(SLOTS).mapObjects { it.toVolunteerSlot() },
    )
  }

  private fun JSONObject.toVolunteerSlot(): VolunteerSlot {
    requireKeys(ID, SERVICE_ON, STATUS, STATUS_CHANGED_ON, optional = setOf(ASSIGNED_TO))
    return VolunteerSlot(
      id = WorkspaceRecordId(requiredString(ID)),
      serviceOn = LocalDate.parse(requiredString(SERVICE_ON)),
      assignedTo = optionalString(ASSIGNED_TO)?.let(::LocalPersonId),
      status = enumValueOf(requiredString(STATUS)),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
    )
  }

  private fun JSONObject.toInventoryItem(): InventoryItem {
    requireKeys(
      ID,
      LABEL,
      QUANTITY_ON_HAND,
      MINIMUM_DESIRED,
      STATUS,
      STATUS_CHANGED_ON,
      optional = setOf(NEXT_CHECK_ON),
    )
    return InventoryItem(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      quantityOnHand = requiredInt(QUANTITY_ON_HAND),
      minimumDesired = requiredInt(MINIMUM_DESIRED),
      nextCheckOn = optionalDate(NEXT_CHECK_ON),
      status = enumValueOf(requiredString(STATUS)),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
    )
  }

  private fun JSONObject.toFinancialControlChecklist(): FinancialControlChecklist {
    requireKeys(ID, LABEL, STATUS, STATUS_CHANGED_ON, CONTROLS, optional = setOf(REVIEW_ON))
    return FinancialControlChecklist(
      id = WorkspaceRecordId(requiredString(ID)),
      label = requiredString(LABEL),
      status = enumValueOf(requiredString(STATUS)),
      reviewOn = optionalDate(REVIEW_ON),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
      controls = getJSONArray(CONTROLS).mapObjects { it.toFinancialControlItem() },
    )
  }

  private fun JSONObject.toFinancialControlItem(): FinancialControlItem {
    requireKeys(ID, KIND, STATUS, STATUS_CHANGED_ON, optional = setOf(DUE_ON, RESPONSIBLE_ROLE))
    return FinancialControlItem(
      id = WorkspaceRecordId(requiredString(ID)),
      kind = enumValueOf<FinancialControlKind>(requiredString(KIND)),
      status = enumValueOf(requiredString(STATUS)),
      dueOn = optionalDate(DUE_ON),
      responsibleRole = optionalString(RESPONSIBLE_ROLE)?.let(::PublicRoleId),
      statusChangedOn = LocalDate.parse(requiredString(STATUS_CHANGED_ON)),
    )
  }

  private fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray = JSONArray().apply {
    this@toJsonArray.forEach { put(transform(it)) }
  }

  private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }

  private fun JSONObject.requireKeys(
    vararg required: String,
    optional: Set<String> = emptySet(),
  ) {
    val actual = buildSet {
      val iterator = keys()
      while (iterator.hasNext()) add(iterator.next())
    }
    val expected = required.toSet() + optional
    require(actual.all(expected::contains) && required.all(actual::contains)) {
      "Unexpected or missing JSON fields."
    }
  }

  private fun JSONObject.stringValue(key: String): String {
    require(has(key) && !isNull(key) && get(key) is String) { "$key must be a string." }
    return getString(key)
  }

  private fun JSONObject.requiredString(key: String): String = stringValue(key).also {
    require(it.isNotBlank()) { "$key must not be blank." }
  }

  private fun JSONObject.requiredInt(key: String): Int {
    require(has(key) && !isNull(key) && get(key) is Number) { "$key must be an integer." }
    val number = get(key) as Number
    val value = number.toLong()
    require(number.toDouble() == value.toDouble()) { "$key must be an integer." }
    require(value in Int.MIN_VALUE..Int.MAX_VALUE) { "$key is outside the supported integer range." }
    return value.toInt()
  }

  private fun JSONObject.optionalString(key: String): String? {
    if (!has(key)) return null
    return requiredString(key)
  }

  private fun JSONObject.optionalDate(key: String): LocalDate? =
    if (has(key)) LocalDate.parse(requiredString(key)) else null

  private fun JSONObject.putOptional(key: String, value: LocalDate?) {
    value?.let { put(key, it.toString()) }
  }

  private fun JSONObject.putOptionalString(key: String, value: String?) {
    value?.let { put(key, it) }
  }

  private const val TYPE = "type"
  private const val SCHEMA_VERSION = "schema_version"
  private const val FIXTURE_ID = "fixture_id"
  private const val SNAPSHOT = "snapshot"
  private const val SELF_WORKSPACES = "self_workspaces"
  private const val HOUSEHOLDS = "households"
  private const val QAHAL_WORKSPACES = "qahal_workspaces"
  private const val WORKSPACE_ID = "workspace_id"
  private const val PARENT_ID = "parent_id"
  private const val RECORD_ID = "record_id"
  private const val TARGET = "target"
  private const val CHANGED_ON = "changed_on"
  private const val NEXT_REVIEW_ON = "next_review_on"
  private const val ASSIGNED_TO = "assigned_to"
  private const val WORKSPACE = "workspace"
  private const val ID = "id"
  private const val LABEL = "label"
  private const val STATUS = "status"
  private const val STATUS_CHANGED_ON = "status_changed_on"
  private const val PRACTICE_ADOPTIONS = "practice_adoptions"
  private const val PRACTICE = "practice"
  private const val ADOPTED_ON = "adopted_on"
  private const val REVIEW_ON = "review_on"
  private const val PRACTICE_CARD_ID = "practice_card_id"
  private const val CONTENT = "content"
  private const val PACK_ID = "pack_id"
  private const val PACK_VERSION = "pack_version"
  private const val EDITION_ID = "edition_id"
  private const val SOURCE_UNIT_ID = "source_unit_id"
  private const val RESPONSIBILITIES = "responsibilities"
  private const val CALENDAR_ITEMS = "calendar_items"
  private const val PREPARATION_KITS = "preparation_kits"
  private const val DUE_ON = "due_on"
  private const val KIND = "kind"
  private const val STARTS_ON = "starts_on"
  private const val ENDS_ON = "ends_on"
  private const val TARGET_ON = "target_on"
  private const val TASKS = "tasks"
  private const val DECISIONS = "decisions"
  private const val VOLUNTEER_ROTATIONS = "volunteer_rotations"
  private const val INVENTORY_ITEMS = "inventory_items"
  private const val FINANCIAL_CONTROLS = "financial_controls"
  private const val CLASSIFICATION = "classification"
  private const val PUBLIC_SUMMARY = "public_summary"
  private const val AUTHORITY_SCOPE = "authority_scope"
  private const val EFFECTIVE_ON = "effective_on"
  private const val SOURCE_REFERENCES = "source_references"
  private const val DISSENT_SUMMARY = "dissent_summary"
  private const val PUBLIC_ROLE = "public_role"
  private const val SLOTS = "slots"
  private const val SERVICE_ON = "service_on"
  private const val QUANTITY_ON_HAND = "quantity_on_hand"
  private const val MINIMUM_DESIRED = "minimum_desired"
  private const val NEXT_CHECK_ON = "next_check_on"
  private const val CONTROLS = "controls"
  private const val RESPONSIBLE_ROLE = "responsible_role"
}
