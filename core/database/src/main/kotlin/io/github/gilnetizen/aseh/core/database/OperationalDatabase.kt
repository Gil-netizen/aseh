package io.github.gilnetizen.aseh.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration

@Entity(
    tableName = "installed_pack_catalog",
    primaryKeys = ["pack_id", "immutable_version"],
    indices = [Index(value = ["pack_id", "is_active"])],
)
internal data class InstalledPackCatalogEntity(
    @ColumnInfo(name = "pack_id") val packId: String,
    @ColumnInfo(name = "immutable_version") val immutableVersion: String,
    @ColumnInfo(name = "manifest_sha256") val manifestSha256: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "signing_key_id") val signingKeyId: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
)

@Dao
internal abstract class InstalledPackCatalogDao {
    @Query(
        """
        SELECT * FROM installed_pack_catalog
        ORDER BY pack_id ASC, immutable_version ASC
        """,
    )
    abstract suspend fun entries(): List<InstalledPackCatalogEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insert(entity: InstalledPackCatalogEntity)

    @Query(
        """
        SELECT * FROM installed_pack_catalog
        WHERE pack_id = :packId AND immutable_version = :immutableVersion
        LIMIT 1
        """,
    )
    protected abstract suspend fun find(
        packId: String,
        immutableVersion: String,
    ): InstalledPackCatalogEntity?

    @Query(
        """
        SELECT COUNT(*) FROM installed_pack_catalog
        WHERE pack_id = :packId AND immutable_version = :immutableVersion
        """,
    )
    protected abstract suspend fun contains(packId: String, immutableVersion: String): Int

    @Query("UPDATE installed_pack_catalog SET is_active = 0 WHERE pack_id = :packId")
    protected abstract suspend fun clearActive(packId: String)

    @Query(
        """
        UPDATE installed_pack_catalog SET is_active = 1
        WHERE pack_id = :packId AND immutable_version = :immutableVersion
        """,
    )
    protected abstract suspend fun markActive(packId: String, immutableVersion: String): Int

    @Query(
        """
        DELETE FROM installed_pack_catalog
        WHERE pack_id = :packId AND immutable_version = :immutableVersion
        """,
    )
    abstract suspend fun remove(packId: String, immutableVersion: String): Int

    @Transaction
    open suspend fun recordInstalled(entity: InstalledPackCatalogEntity) {
        val existing = find(entity.packId, entity.immutableVersion)
        if (existing == null) {
            insert(entity)
            return
        }

        require(existing.copy(isActive = false) == entity) {
            "An immutable pack version cannot be replaced with different catalog metadata"
        }
    }

    @Transaction
    open suspend fun activate(packId: String, immutableVersion: String): Boolean {
        if (contains(packId, immutableVersion) != 1) return false

        clearActive(packId)
        return markActive(packId, immutableVersion) == 1
    }
}

@Database(
    entities = [InstalledPackCatalogEntity::class],
    version = OPERATIONAL_DATABASE_VERSION,
    exportSchema = true,
)
internal abstract class OperationalDatabase : RoomDatabase() {
    abstract fun installedPackCatalogDao(): InstalledPackCatalogDao
}

/**
 * Migration registry used by every operational-database builder and its tests.
 * Version 1 has no inbound migrations; future versions append migrations here.
 */
internal object OperationalDatabaseMigrations {
    val all: Array<Migration>
        get() = emptyArray()
}

internal const val OPERATIONAL_DATABASE_VERSION = 1
internal const val OPERATIONAL_DATABASE_NAME = "aseh-operational.db"
