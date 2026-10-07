package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OperationalDatabaseTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OperationalDatabase::class.java,
    )

    private lateinit var database: OperationalDatabase
    private lateinit var repository: InstalledPackCatalogRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OperationalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomInstalledPackCatalogRepository(database.installedPackCatalogDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun activationIsAtomicWithinOnePackIdentity() = runTest {
        val versionOne = identity(version = "1.0.0", digestCharacter = 'a')
        val versionTwo = identity(version = "2.0.0", digestCharacter = 'b')
        repository.recordInstalled(versionOne)
        repository.recordInstalled(versionTwo)

        assertTrue(repository.activate(versionOne.packId, versionOne.immutableVersion))
        assertTrue(repository.activate(versionTwo.packId, versionTwo.immutableVersion))

        val entries = repository.entries()
        assertEquals(2, entries.size)
        assertFalse(entries.single { it.identity == versionOne }.isActive)
        assertTrue(entries.single { it.identity == versionTwo }.isActive)
    }

    @Test
    fun missingVersionCannotClearCurrentActivation() = runTest {
        val installed = identity(version = "1.0.0", digestCharacter = 'c')
        repository.recordInstalled(installed)
        repository.activate(installed.packId, installed.immutableVersion)

        assertFalse(repository.activate(installed.packId, "missing"))
        assertTrue(repository.entries().single().isActive)
    }

    @Test
    fun immutableVersionCannotBeReplacedByAnotherManifest() = runTest {
        val installed = identity(version = "1.0.0", digestCharacter = 'c')
        repository.recordInstalled(installed)

        var rejected = false
        try {
            repository.recordInstalled(installed.copy(manifestSha256 = "e".repeat(64)))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(installed, repository.entries().single().identity)
    }

    @Test
    fun operationalSchemaContainsOnlyAllowlistedPackAndExperienceTables() {
        val sqlite = database.openHelper.writableDatabase
        val applicationTables = buildSet {
            sqlite.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    if (name != "android_metadata" && name != "room_master_table") add(name)
                }
            }
        }
        assertEquals(
            setOf(
                "installed_pack_catalog",
                "experience_profile",
                "experience_role_assignment",
                "experience_reading_assignment",
                "experience_reading_plan",
                "experience_record_marker",
            ),
            applicationTables,
        )

        val columns = buildSet {
            sqlite.query("PRAGMA table_info(`installed_pack_catalog`)").use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameColumn))
            }
        }
        assertEquals(
            setOf(
                "pack_id",
                "immutable_version",
                "manifest_sha256",
                "schema_version",
                "signing_key_id",
                "is_active",
            ),
            columns,
        )
    }

    @Test
    fun exportedVersionOneSchemaMigratesWithoutDestroyingPackCatalog() = runTest {
        migrationHelper.createDatabase(MIGRATION_DATABASE_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO installed_pack_catalog (
                    pack_id, immutable_version, manifest_sha256,
                    schema_version, signing_key_id, is_active
                ) VALUES ('aseh.test', '1.0.0', '${"a".repeat(64)}', 1, '${"b".repeat(64)}', 1)
                """.trimIndent(),
            )
            close()
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val migrated = Room.databaseBuilder(
            context,
            OperationalDatabase::class.java,
            MIGRATION_DATABASE_NAME,
        )
            .addMigrations(*OperationalDatabaseMigrations.all)
            .build()
        try {
            migrated.openHelper.writableDatabase
            assertEquals("aseh.test", migrated.installedPackCatalogDao().entries().single().packId)
            assertEquals(null, migrated.experienceStateDao().profile())
        } finally {
            migrated.close()
        }
    }

    @Test
    fun versionTwoMigrationDropsUndatedServiceStateAndPreservesDurableRecords() = runTest {
        migrationHelper.createDatabase(SERVICE_MIGRATION_DATABASE_NAME, 2).apply {
            execSQL(
                """
                INSERT INTO experience_profile (
                    profile_id, workspace_name, workspace_kind, selected_community_option_id,
                    charter_purpose, charter_participants, charter_authority_limits,
                    charter_decision_process, charter_role_terms, charter_accessibility_commitment,
                    charter_effective_date, charter_review_date, charter_version, charter_adopted,
                    practice_adoption_option_id, practice_adoption_scope,
                    practice_adoption_effective_date, practice_adoption_review_date,
                    practice_adoption_recorded_by, legacy_datastore_migrated
                ) VALUES (
                    1, 'Harimon', 'qahal', 'choice.keep',
                    'Purpose', 'Participants', 'Limits', 'Consensus', 'Quarterly', 'Access',
                    '2026-10-01', '2027-01-01', 'Trial 1', 1,
                    'adoption.keep', 'Local scope', '2026-10-01', '2026-11-01',
                    'Coordinator', 1
                )
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO experience_role_assignment VALUES (1, 'leader', 'Old leader')",
            )
            execSQL(
                "INSERT INTO experience_reading_assignment VALUES (1, 'reading.1', 'Old reader')",
            )
            execSQL(
                """
                INSERT INTO experience_reading_plan VALUES (
                    1, 'reading.1', '', '', '', 'Old reader', '', 'READY', 0, ''
                )
                """.trimIndent(),
            )
            listOf(
                "completed_practice_step" to "practice.old",
                "completed_preflight_step" to "preflight.old",
                "completed_service_segment" to "segment.old",
                "saved_practice_card" to "practice.saved",
                "bookmarked_source" to "source.saved",
                "reviewed_dossier_fact" to "dossier.fact.saved",
            ).forEach { (type, id) ->
                execSQL(
                    "INSERT INTO experience_record_marker VALUES (1, '$type', '$id')",
                )
            }
            close()
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val migrated = Room.databaseBuilder(
            context,
            OperationalDatabase::class.java,
            SERVICE_MIGRATION_DATABASE_NAME,
        )
            .addMigrations(*OperationalDatabaseMigrations.all)
            .build()
        try {
            val snapshot = checkNotNull(migrated.experienceStateDao().snapshot())
            assertEquals("Harimon", snapshot.profile.workspaceName)
            assertEquals("choice.keep", snapshot.profile.selectedCommunityOptionId)
            assertEquals("Purpose", snapshot.profile.charterPurpose)
            assertEquals("adoption.keep", snapshot.profile.practiceAdoptionOptionId)
            assertEquals(null, snapshot.profile.serviceInstanceDate)
            assertTrue(snapshot.roleAssignments.isEmpty())
            assertTrue(snapshot.readingAssignments.isEmpty())
            assertTrue(snapshot.readingPlans.isEmpty())
            assertEquals(
                setOf(
                    "saved_practice_card" to "practice.saved",
                    "bookmarked_source" to "source.saved",
                    "reviewed_dossier_fact" to "dossier.fact.saved",
                ),
                snapshot.recordMarkers.map { marker ->
                    marker.recordType to marker.recordId
                }.toSet(),
            )
        } finally {
            migrated.close()
        }
    }

    private fun identity(version: String, digestCharacter: Char) = InstalledPackIdentity(
        packId = "aseh.core",
        immutableVersion = version,
        manifestSha256 = digestCharacter.toString().repeat(64),
        schemaVersion = 1,
        signingKeyId = "d".repeat(64),
    )

    private companion object {
        const val MIGRATION_DATABASE_NAME = "operational-migration-test.db"
        const val SERVICE_MIGRATION_DATABASE_NAME = "operational-service-migration-test.db"
    }
}
