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
    fun operationalSchemaContainsOnlyPackCatalogMetadata() {
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
        assertEquals(setOf("installed_pack_catalog"), applicationTables)

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
    fun exportedVersionOneSchemaOpensThroughMigrationRegistry() {
        migrationHelper.createDatabase(MIGRATION_DATABASE_NAME, OPERATIONAL_DATABASE_VERSION).close()

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
    }
}
