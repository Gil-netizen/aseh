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
import androidx.sqlite.execSQL

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
    entities = [
        InstalledPackCatalogEntity::class,
        ExperienceProfileEntity::class,
        ExperienceRoleAssignmentEntity::class,
        ExperienceReadingAssignmentEntity::class,
        ExperienceReadingPlanEntity::class,
        ExperienceRecordMarkerEntity::class,
    ],
    version = OPERATIONAL_DATABASE_VERSION,
    exportSchema = true,
)
internal abstract class OperationalDatabase : RoomDatabase() {
    abstract fun installedPackCatalogDao(): InstalledPackCatalogDao
    abstract fun experienceStateDao(): ExperienceStateDao
}

/**
 * Migration registry used by every operational-database builder and its tests.
 * Version 1 has no inbound migrations; future versions append migrations here.
 */
internal object OperationalDatabaseMigrations {
    val all: Array<Migration>
        get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `experience_profile` (
                    `profile_id` INTEGER NOT NULL,
                    `workspace_name` TEXT NOT NULL,
                    `workspace_kind` TEXT NOT NULL,
                    `selected_community_option_id` TEXT,
                    `charter_purpose` TEXT NOT NULL,
                    `charter_participants` TEXT NOT NULL,
                    `charter_authority_limits` TEXT NOT NULL,
                    `charter_decision_process` TEXT NOT NULL,
                    `charter_role_terms` TEXT NOT NULL,
                    `charter_accessibility_commitment` TEXT NOT NULL,
                    `charter_effective_date` TEXT NOT NULL,
                    `charter_review_date` TEXT NOT NULL,
                    `charter_version` TEXT NOT NULL,
                    `charter_adopted` INTEGER NOT NULL,
                    `practice_adoption_option_id` TEXT,
                    `practice_adoption_scope` TEXT NOT NULL,
                    `practice_adoption_effective_date` TEXT NOT NULL,
                    `practice_adoption_review_date` TEXT NOT NULL,
                    `practice_adoption_recorded_by` TEXT NOT NULL,
                    `legacy_datastore_migrated` INTEGER NOT NULL,
                    PRIMARY KEY(`profile_id`)
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `experience_role_assignment` (
                    `profile_id` INTEGER NOT NULL,
                    `role_id` TEXT NOT NULL,
                    `assignee_name` TEXT NOT NULL,
                    PRIMARY KEY(`profile_id`, `role_id`),
                    FOREIGN KEY(`profile_id`) REFERENCES `experience_profile`(`profile_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `experience_reading_assignment` (
                    `profile_id` INTEGER NOT NULL,
                    `slot_id` TEXT NOT NULL,
                    `assignee_name` TEXT NOT NULL,
                    PRIMARY KEY(`profile_id`, `slot_id`),
                    FOREIGN KEY(`profile_id`) REFERENCES `experience_profile`(`profile_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `experience_reading_plan` (
                    `profile_id` INTEGER NOT NULL,
                    `slot_id` TEXT NOT NULL,
                    `portion_title` TEXT NOT NULL,
                    `locator` TEXT NOT NULL,
                    `passage_range` TEXT NOT NULL,
                    `assignee_name` TEXT NOT NULL,
                    `backup_assignee_name` TEXT NOT NULL,
                    `preparation_status` TEXT NOT NULL,
                    `manual_override` INTEGER NOT NULL,
                    `override_reason` TEXT NOT NULL,
                    PRIMARY KEY(`profile_id`, `slot_id`),
                    FOREIGN KEY(`profile_id`) REFERENCES `experience_profile`(`profile_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `experience_record_marker` (
                    `profile_id` INTEGER NOT NULL,
                    `record_type` TEXT NOT NULL,
                    `record_id` TEXT NOT NULL,
                    PRIMARY KEY(`profile_id`, `record_type`, `record_id`),
                    FOREIGN KEY(`profile_id`) REFERENCES `experience_profile`(`profile_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `experience_profile` ADD COLUMN `service_instance_date` TEXT",
            )

            // Version 2 had no service-instance key, so attaching any of these records to the
            // device's newly calculated Shabbat would silently relabel unknown old state. Fail
            // closed while retaining workspace, governance, saved cards, bookmarks, and dossier
            // review markers.
            connection.execSQL("DELETE FROM `experience_role_assignment`")
            connection.execSQL("DELETE FROM `experience_reading_assignment`")
            connection.execSQL("DELETE FROM `experience_reading_plan`")
            connection.execSQL(
                """
                DELETE FROM `experience_record_marker`
                WHERE `record_type` IN (
                    'completed_practice_step',
                    'completed_preflight_step',
                    'completed_service_segment'
                )
                """.trimIndent(),
            )
        }
    }
}

internal const val OPERATIONAL_DATABASE_VERSION = 3
internal const val OPERATIONAL_DATABASE_NAME = "aseh-operational.db"
