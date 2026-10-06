package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.room.Room
import java.io.Closeable

/** A closeable owner for operational persistence created by the app root. */
interface OperationalStore : Closeable {
    val installedPackCatalog: InstalledPackCatalogRepository
}

object OperationalStoreFactory {
    fun create(context: Context): OperationalStore {
        val database = Room.databaseBuilder(
            context.applicationContext,
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
        )
    }
}

private class DefaultOperationalStore(
    private val database: OperationalDatabase,
    override val installedPackCatalog: InstalledPackCatalogRepository,
) : OperationalStore {
    override fun close() {
        database.close()
    }
}
