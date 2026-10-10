package io.github.gilnetizen.aseh.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * One explicitly versioned operational payload for the local workspace repository.
 *
 * The payload is produced and consumed only by the strict workspace codec in the app module. It
 * is intentionally not a general object-serialization facility and must contain only records the
 * product classifies as non-sensitive operational state.
 */
data class StoredWorkspaceState(
    val persistenceSchemaVersion: Int,
    val fixtureId: String,
    val primaryPayload: String?,
    val backupPayload: String?,
    val legacyCommandPayload: String?,
) {
    init {
        require(persistenceSchemaVersion > 0) { "Workspace persistence schema must be positive" }
        require(fixtureId.isNotBlank()) { "Workspace fixture ID must not be blank" }
        require(primaryPayload != null || backupPayload != null || legacyCommandPayload != null) {
            "Stored workspace state must contain a payload"
        }
    }
}

/** Narrow operational-store boundary used by the application workspace repository. */
interface WorkspaceStateStore {
    suspend fun read(): StoredWorkspaceState?

    /** Imports legacy state only when no operational workspace row exists. */
    suspend fun importIfAbsent(state: StoredWorkspaceState): Boolean

    /** Replaces the current and recovery snapshots after a successful legacy replay, if any. */
    suspend fun save(
        persistenceSchemaVersion: Int,
        fixtureId: String,
        primaryPayload: String,
        backupPayload: String,
    )

    /** Removes a successfully superseded legacy command source without touching recovery snapshots. */
    suspend fun discardLegacyCommandPayload()

    suspend fun clear()
}

@Entity(tableName = "workspace_snapshot_state")
internal data class WorkspaceSnapshotStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "singleton_id")
    val singletonId: Int = WORKSPACE_STATE_SINGLETON_ID,
    @ColumnInfo(name = "persistence_schema_version")
    val persistenceSchemaVersion: Int,
    @ColumnInfo(name = "fixture_id")
    val fixtureId: String,
    @ColumnInfo(name = "primary_payload")
    val primaryPayload: String?,
    @ColumnInfo(name = "backup_payload")
    val backupPayload: String?,
    @ColumnInfo(name = "legacy_command_payload")
    val legacyCommandPayload: String?,
)

@Dao
internal abstract class WorkspaceStateDao {
    @Query(
        "SELECT * FROM workspace_snapshot_state WHERE singleton_id = :singletonId LIMIT 1",
    )
    abstract suspend fun read(
        singletonId: Int = WORKSPACE_STATE_SINGLETON_ID,
    ): WorkspaceSnapshotStateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIfAbsent(entity: WorkspaceSnapshotStateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun replace(entity: WorkspaceSnapshotStateEntity)

    @Query("DELETE FROM workspace_snapshot_state WHERE singleton_id = :singletonId")
    abstract suspend fun clear(singletonId: Int = WORKSPACE_STATE_SINGLETON_ID)

    @Query(
        "UPDATE workspace_snapshot_state SET legacy_command_payload = NULL " +
            "WHERE singleton_id = :singletonId",
    )
    abstract suspend fun discardLegacyCommandPayload(
        singletonId: Int = WORKSPACE_STATE_SINGLETON_ID,
    )

    @Transaction
    open suspend fun importIfAbsent(entity: WorkspaceSnapshotStateEntity): Boolean =
        insertIfAbsent(entity) != -1L

    @Transaction
    open suspend fun save(
        persistenceSchemaVersion: Int,
        fixtureId: String,
        primaryPayload: String,
        backupPayload: String,
    ) {
        replace(
            WorkspaceSnapshotStateEntity(
                persistenceSchemaVersion = persistenceSchemaVersion,
                fixtureId = fixtureId,
                primaryPayload = primaryPayload,
                backupPayload = backupPayload,
                legacyCommandPayload = null,
            ),
        )
    }
}

internal class RoomWorkspaceStateStore(
    private val dao: WorkspaceStateDao,
) : WorkspaceStateStore {
    override suspend fun read(): StoredWorkspaceState? = dao.read()?.toStoredState()

    override suspend fun importIfAbsent(state: StoredWorkspaceState): Boolean =
        dao.importIfAbsent(state.toEntity())

    override suspend fun save(
        persistenceSchemaVersion: Int,
        fixtureId: String,
        primaryPayload: String,
        backupPayload: String,
    ) {
        require(persistenceSchemaVersion > 0) { "Workspace persistence schema must be positive" }
        require(fixtureId.isNotBlank()) { "Workspace fixture ID must not be blank" }
        dao.save(
            persistenceSchemaVersion = persistenceSchemaVersion,
            fixtureId = fixtureId,
            primaryPayload = primaryPayload,
            backupPayload = backupPayload,
        )
    }

    override suspend fun discardLegacyCommandPayload() = dao.discardLegacyCommandPayload()

    override suspend fun clear() = dao.clear()
}

private fun WorkspaceSnapshotStateEntity.toStoredState(): StoredWorkspaceState =
    StoredWorkspaceState(
        persistenceSchemaVersion = persistenceSchemaVersion,
        fixtureId = fixtureId,
        primaryPayload = primaryPayload,
        backupPayload = backupPayload,
        legacyCommandPayload = legacyCommandPayload,
    )

private fun StoredWorkspaceState.toEntity(): WorkspaceSnapshotStateEntity =
    WorkspaceSnapshotStateEntity(
        persistenceSchemaVersion = persistenceSchemaVersion,
        fixtureId = fixtureId,
        primaryPayload = primaryPayload,
        backupPayload = backupPayload,
        legacyCommandPayload = legacyCommandPayload,
    )

private const val WORKSPACE_STATE_SINGLETON_ID = 1
