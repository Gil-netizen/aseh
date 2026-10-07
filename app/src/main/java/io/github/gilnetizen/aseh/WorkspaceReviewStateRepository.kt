package io.github.gilnetizen.aseh

import android.content.Context
import android.content.SharedPreferences
import io.github.gilnetizen.aseh.domain.workspace.CalendarItemStatus
import io.github.gilnetizen.aseh.domain.workspace.FinancialChecklistStatus
import io.github.gilnetizen.aseh.domain.workspace.FinancialControlStatus
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.LocalPersonId
import io.github.gilnetizen.aseh.domain.workspace.LocalWorkspaceApi
import io.github.gilnetizen.aseh.domain.workspace.PracticeAdoptionStatus
import io.github.gilnetizen.aseh.domain.workspace.PreparationKitStatus
import io.github.gilnetizen.aseh.domain.workspace.QahalDecisionStatus
import io.github.gilnetizen.aseh.domain.workspace.VolunteerRotationStatus
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the development workspace's explicit lifecycle commands, then replays them over the
 * versioned fixture. Persisting commands keeps the fixture immutable and makes fixture upgrades
 * deterministic: a changed fixture id starts a fresh review workspace instead of merging records.
 */
interface WorkspaceReviewStateRepository {
  val state: StateFlow<WorkspaceSnapshot>
  val fixtureNotice: String

  suspend fun apply(command: WorkspaceCommand): WorkspaceUpdateResult

  suspend fun clear()
}

internal const val WORKSPACE_LOCAL_PERSISTENCE_PATH = "localPersistence"

internal object WorkspaceReviewStateRepositoryFactory {
  fun create(
    context: Context,
    fixture: WorkspaceFixture,
  ): WorkspaceReviewStateRepository = SharedPreferencesWorkspaceReviewStateRepository(
    preferences = context.applicationContext.getSharedPreferences(
      "aseh_workspace_review_state",
      Context.MODE_PRIVATE,
    ),
    fixture = fixture,
  )
}

private class SharedPreferencesWorkspaceReviewStateRepository(
  private val preferences: SharedPreferences,
  private val fixture: WorkspaceFixture,
) : WorkspaceReviewStateRepository {
  private val mutex = Mutex()
  private val commands = mutableListOf<WorkspaceCommand>()
  private val mutableState = MutableStateFlow(load())

  override val state: StateFlow<WorkspaceSnapshot> = mutableState.asStateFlow()
  override val fixtureNotice: String = fixture.notice

  override suspend fun apply(command: WorkspaceCommand): WorkspaceUpdateResult =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        when (val result = LocalWorkspaceApi.apply(mutableState.value, command)) {
          is WorkspaceUpdateResult.Rejected -> result
          is WorkspaceUpdateResult.Applied -> {
            val nextCommands = commands + command
            val persisted = preferences.edit()
            .putString(KEY_FIXTURE_ID, fixture.id)
            .putString(KEY_COMMAND_LOG, WorkspaceCommandJson.encode(nextCommands))
              .commit()
            if (!persisted) {
              WorkspaceUpdateResult.Rejected(
                unchangedSnapshot = mutableState.value,
                issues = listOf(
                    WorkspaceIssue(
                      code = WorkspaceIssueCode.STATUS_REQUIREMENT_NOT_MET,
                      path = WORKSPACE_LOCAL_PERSISTENCE_PATH,
                      detail = "The local workspace change could not be persisted.",
                  ),
                ),
              )
            } else {
              commands += command
              mutableState.value = result.snapshot
              result
            }
          }
        }
      }
    }

  override suspend fun clear() = withContext(Dispatchers.IO) {
    mutex.withLock {
      check(
        preferences.edit()
          .putString(KEY_FIXTURE_ID, fixture.id)
          .remove(KEY_COMMAND_LOG)
          .commit(),
      ) { "The local workspace review state could not be cleared." }
      commands.clear()
      mutableState.value = fixture.snapshot
    }
  }

  private fun load(): WorkspaceSnapshot {
    if (preferences.getString(KEY_FIXTURE_ID, null) != fixture.id) {
      preferences.edit()
        .putString(KEY_FIXTURE_ID, fixture.id)
        .remove(KEY_COMMAND_LOG)
        .apply()
      return fixture.snapshot
    }
    val encoded = preferences.getString(KEY_COMMAND_LOG, null) ?: return fixture.snapshot
    val decoded = runCatching { WorkspaceCommandJson.decode(encoded) }.getOrElse {
      preferences.edit().remove(KEY_COMMAND_LOG).apply()
      return fixture.snapshot
    }
    var snapshot = fixture.snapshot
    decoded.forEach { command ->
      snapshot = when (val result = LocalWorkspaceApi.apply(snapshot, command)) {
        is WorkspaceUpdateResult.Applied -> result.snapshot
        is WorkspaceUpdateResult.Rejected -> {
          preferences.edit().remove(KEY_COMMAND_LOG).apply()
          commands.clear()
          return fixture.snapshot
        }
      }
    }
    commands += decoded
    return snapshot
  }

  private companion object {
    const val KEY_FIXTURE_ID = "fixture_id"
    const val KEY_COMMAND_LOG = "command_log"
  }
}

/** Strict, typed codec for the status commands emitted by WorkspaceDashboard. */
private object WorkspaceCommandJson {
  fun encode(commands: List<WorkspaceCommand>): String = JSONArray().apply {
    commands.forEach { put(it.toJson()) }
  }.toString()

  fun decode(encoded: String): List<WorkspaceCommand> {
    val array = JSONArray(encoded)
    return List(array.length()) { index -> array.getJSONObject(index).toCommand() }
  }

  private fun WorkspaceCommand.toJson(): JSONObject = JSONObject().apply {
    when (this@toJson) {
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
      is WorkspaceCommand.PutHousehold,
      is WorkspaceCommand.PutPersonalPractice,
      is WorkspaceCommand.PutQahal,
      is WorkspaceCommand.PutSelf,
      -> error("Workspace creation commands are not emitted by the review dashboard.")
    }
  }

  private fun JSONObject.toCommand(): WorkspaceCommand {
    val workspaceId = WorkspaceId(requiredString(WORKSPACE_ID))
    val recordId = WorkspaceRecordId(requiredString(RECORD_ID))
    val changedOn = LocalDate.parse(requiredString(CHANGED_ON))
    return when (requiredString(TYPE)) {
      "practice" -> WorkspaceCommand.ChangePracticeStatus(
        workspaceId,
        recordId,
        enumValueOf(requiredString(TARGET)),
        changedOn,
        optionalDate(NEXT_REVIEW_ON),
      )
      "responsibility" -> WorkspaceCommand.ChangeResponsibilityStatus(
        workspaceId,
        recordId,
        enumValueOf<WorkItemStatus>(requiredString(TARGET)),
        changedOn,
      )
      "calendar" -> WorkspaceCommand.ChangeCalendarItemStatus(
        workspaceId,
        recordId,
        enumValueOf<CalendarItemStatus>(requiredString(TARGET)),
        changedOn,
      )
      "preparation_task" -> WorkspaceCommand.ChangePreparationTaskStatus(
        workspaceId,
        WorkspaceRecordId(requiredString(PARENT_ID)),
        recordId,
        enumValueOf<WorkItemStatus>(requiredString(TARGET)),
        changedOn,
      )
      "preparation_kit" -> WorkspaceCommand.ChangePreparationKitStatus(
        workspaceId,
        recordId,
        enumValueOf<PreparationKitStatus>(requiredString(TARGET)),
        changedOn,
      )
      "decision" -> WorkspaceCommand.ChangeDecisionStatus(
        workspaceId,
        recordId,
        enumValueOf<QahalDecisionStatus>(requiredString(TARGET)),
        changedOn,
        optionalDate(NEXT_REVIEW_ON),
      )
      "volunteer_slot" -> WorkspaceCommand.ChangeVolunteerSlotStatus(
        workspaceId,
        WorkspaceRecordId(requiredString(PARENT_ID)),
        recordId,
        enumValueOf<VolunteerSlotStatus>(requiredString(TARGET)),
        changedOn,
        optString(ASSIGNED_TO).takeIf(String::isNotBlank)?.let(::LocalPersonId),
      )
      "volunteer_rotation" -> WorkspaceCommand.ChangeVolunteerRotationStatus(
        workspaceId,
        recordId,
        enumValueOf<VolunteerRotationStatus>(requiredString(TARGET)),
        changedOn,
      )
      "inventory" -> WorkspaceCommand.ChangeInventoryStatus(
        workspaceId,
        recordId,
        enumValueOf<InventoryItemStatus>(requiredString(TARGET)),
        changedOn,
      )
      "financial_control" -> WorkspaceCommand.ChangeFinancialControlStatus(
        workspaceId,
        WorkspaceRecordId(requiredString(PARENT_ID)),
        recordId,
        enumValueOf<FinancialControlStatus>(requiredString(TARGET)),
        changedOn,
      )
      "financial_checklist" -> WorkspaceCommand.ChangeFinancialChecklistStatus(
        workspaceId,
        recordId,
        enumValueOf<FinancialChecklistStatus>(requiredString(TARGET)),
        changedOn,
        optionalDate(NEXT_REVIEW_ON),
      )
      else -> error("Unsupported workspace command type.")
    }
  }

  private fun JSONObject.requiredString(key: String): String = getString(key).also {
    require(it.isNotBlank()) { "$key must not be blank." }
  }

  private fun JSONObject.optionalDate(key: String): LocalDate? =
    optString(key).takeIf(String::isNotBlank)?.let(LocalDate::parse)

  private fun JSONObject.putOptional(key: String, value: LocalDate?) {
    value?.let { put(key, it.toString()) }
  }

  private const val TYPE = "type"
  private const val WORKSPACE_ID = "workspace_id"
  private const val PARENT_ID = "parent_id"
  private const val RECORD_ID = "record_id"
  private const val TARGET = "target"
  private const val CHANGED_ON = "changed_on"
  private const val NEXT_REVIEW_ON = "next_review_on"
  private const val ASSIGNED_TO = "assigned_to"
}
