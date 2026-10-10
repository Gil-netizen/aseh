package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.room.Room
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope

/** A closeable owner for operational persistence created by the app root. */
interface OperationalStore : Closeable {
    val installedPackCatalog: InstalledPackCatalogRepository
    val experienceStateRepository: ExperienceStateRepository
    val workspaceStateStore: WorkspaceStateStore
}

object OperationalStoreFactory {
    fun create(
        context: Context,
        scope: CoroutineScope,
    ): OperationalStore {
        val appContext = context.applicationContext
        val database = Room.databaseBuilder(
            appContext,
            OperationalDatabase::class.java,
            OPERATIONAL_DATABASE_NAME,
        )
            .addMigrations(*OperationalDatabaseMigrations.all)
            .build()

        return DefaultOperationalStore(
            database = database,
            installedPackCatalog = RoomInstalledPackCatalogRepository(
                database.installedPackCatalogDao(),
            ),
            experienceStateRepository = RoomExperienceStateRepository(
                dao = database.experienceStateDao(),
                preferenceStore = createExperiencePreferencesDataStore(appContext, scope),
            ),
            workspaceStateStore = RoomWorkspaceStateStore(database.workspaceStateDao()),
        )
    }
}

private class DefaultOperationalStore(
    private val database: OperationalDatabase,
    override val installedPackCatalog: InstalledPackCatalogRepository,
    override val experienceStateRepository: ExperienceStateRepository,
    override val workspaceStateStore: WorkspaceStateStore,
) : OperationalStore {
    override fun close() {
        database.close()
    }
}
