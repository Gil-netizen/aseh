package io.github.gilnetizen.aseh.core.content.runtime

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import io.github.gilnetizen.aseh.core.content.ActiveContentStore
import io.github.gilnetizen.aseh.core.content.ContentDatabaseContract
import io.github.gilnetizen.aseh.core.content.ContentKind
import io.github.gilnetizen.aseh.core.content.ContentSearchHit
import io.github.gilnetizen.aseh.core.content.ContentPolicyValidator
import io.github.gilnetizen.aseh.core.content.InstalledContentDatabaseVerifier
import io.github.gilnetizen.aseh.core.content.SemanticVersion
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

class ContentDatabaseVerificationException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

class ContentSearchException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/** Android platform-SQLite verifier for an already hash- and signature-verified pack database. */
object AndroidInstalledContentDatabaseVerifier : InstalledContentDatabaseVerifier {
    override fun verify(
        database: Path,
        expectedContentSchema: Int,
        expectedPackId: String,
        expectedVersion: SemanticVersion,
    ) {
        if (!Files.isRegularFile(database, LinkOption.NOFOLLOW_LINKS)) {
            throw ContentDatabaseVerificationException("Content database is missing or is not a regular file")
        }
        val sqlite = try {
            openReadOnly(database)
        } catch (error: RuntimeException) {
            throw ContentDatabaseVerificationException("Content database could not be opened read-only", error)
        }
        sqlite.use { connection ->
            try {
                connection.execSQL("PRAGMA query_only=ON")
                requireSingleLong(connection, "PRAGMA query_only", 1L, "Content database is not query-only")
                requireSingleText(connection, "PRAGMA quick_check", "ok", "SQLite quick_check failed")
                requireSingleLong(
                    connection,
                    "PRAGMA application_id",
                    ContentDatabaseContract.APPLICATION_ID.toLong(),
                    "Unexpected content database application ID",
                )
                requireSingleLong(
                    connection,
                    "PRAGMA user_version",
                    expectedContentSchema.toLong(),
                    "Unexpected content database schema",
                )
                val prohibitedObjects = queryStrings(
                    connection,
                    "SELECT type || ':' || name FROM sqlite_master " +
                        "WHERE type IN ('trigger', 'view') ORDER BY type, name",
                )
                if (prohibitedObjects.isNotEmpty()) {
                    throw ContentDatabaseVerificationException("Content database contains executable views or triggers")
                }
                val tables = queryStrings(
                    connection,
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
                ).toSet()
                if (tables != ContentDatabaseContract.requiredTableNames) {
                    throw ContentDatabaseVerificationException(
                        "Content database table allowlist mismatch: found=${tables.sorted()}",
                    )
                }
                if (queryRows(connection, "PRAGMA foreign_key_check") != 0) {
                    throw ContentDatabaseVerificationException("Content database contains broken references")
                }
                val metadata = mutableMapOf<String, String>()
                connection.rawQuery("SELECT key, value FROM pack_metadata ORDER BY key", emptyArray()).use { cursor ->
                    while (cursor.moveToNext()) metadata[cursor.getString(0)] = cursor.getString(1)
                }
                if (metadata["pack_id"] != expectedPackId) {
                    throw ContentDatabaseVerificationException("Content database pack identity mismatch")
                }
                if (metadata["pack_version"] != expectedVersion.toString()) {
                    throw ContentDatabaseVerificationException("Content database version mismatch")
                }
                if (metadata.keys != ContentDatabaseContract.requiredMetadataKeys) {
                    throw ContentDatabaseVerificationException("Content database metadata allowlist mismatch")
                }
                if (metadata["search_engine"] != ContentDatabaseContract.SEARCH_ENGINE) {
                    throw ContentDatabaseVerificationException("Unsupported content search engine")
                }
                val searchRows = querySingleLong(
                    connection,
                    "SELECT count(*) FROM ${ContentDatabaseContract.SEARCH_TABLE}",
                )
                if (searchRows <= 0L) {
                    throw ContentDatabaseVerificationException("Content search index is empty")
                }
            } catch (error: ContentDatabaseVerificationException) {
                throw error
            } catch (error: SQLiteException) {
                throw ContentDatabaseVerificationException("Content database verification query failed", error)
            } catch (error: RuntimeException) {
                throw ContentDatabaseVerificationException("Content database metadata is invalid", error)
            }
        }
    }

    private fun requireSingleLong(
        database: SQLiteDatabase,
        sql: String,
        expected: Long,
        message: String,
    ) {
        if (querySingleLong(database, sql) != expected) throw ContentDatabaseVerificationException(message)
    }

    private fun requireSingleText(
        database: SQLiteDatabase,
        sql: String,
        expected: String,
        message: String,
    ) {
        database.rawQuery(sql, emptyArray()).use { cursor ->
            if (!cursor.moveToFirst() || cursor.getString(0) != expected || cursor.moveToNext()) {
                throw ContentDatabaseVerificationException(message)
            }
        }
    }

    private fun querySingleLong(database: SQLiteDatabase, sql: String): Long =
        database.rawQuery(sql, emptyArray()).use { cursor ->
            if (!cursor.moveToFirst() || cursor.columnCount != 1) {
                throw ContentDatabaseVerificationException("Expected one SQLite scalar result")
            }
            val result = cursor.getLong(0)
            if (cursor.moveToNext()) {
                throw ContentDatabaseVerificationException("Expected one SQLite scalar row")
            }
            result
        }

    private fun queryStrings(database: SQLiteDatabase, sql: String): List<String> =
        database.rawQuery(sql, emptyArray()).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun queryRows(database: SQLiteDatabase, sql: String): Int =
        database.rawQuery(sql, emptyArray()).use { cursor -> cursor.count }
}

/** Read-only lexical search over a verified active pack. */
class AndroidInstalledContentSearch(
    private val store: ActiveContentStore,
    verifier: InstalledContentDatabaseVerifier = AndroidInstalledContentDatabaseVerifier,
    expectedContentSchema: Int = ContentPolicyValidator.CURRENT_CONTENT_SCHEMA,
) {
    init {
        verifier.verify(
            database = store.database,
            expectedContentSchema = expectedContentSchema,
            expectedPackId = store.identity.packId,
            expectedVersion = store.identity.version,
        )
    }

    fun search(query: String, limit: Int = 20): List<ContentSearchHit> {
        if (limit !in 1..100) throw ContentSearchInputException("Search limit must be between 1 and 100")
        val expression = SafeFtsQuery.compile(query) ?: return emptyList()
        val database = try {
            openReadOnly(store.database)
        } catch (error: RuntimeException) {
            throw ContentSearchException("Verified content database is unavailable", error)
        }
        database.use { connection ->
            try {
                connection.execSQL("PRAGMA query_only=ON")
                connection.rawQuery(
                    """
                    SELECT d.content_id, d.kind, d.language, d.title,
                           snippet(content_fts, '', '', ' … ', 2, 18),
                           d.edition_id, d.locator
                    FROM content_fts
                    JOIN content_documents d ON d.content_id = content_fts.content_id
                    WHERE content_fts MATCH ?
                    ORDER BY d.content_id
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(expression, limit.toString()),
                ).use { cursor ->
                    return buildList {
                        while (cursor.moveToNext()) {
                            add(
                                ContentSearchHit(
                                    contentId = cursor.getString(0),
                                    kind = ContentKind.parse(cursor.getString(1)),
                                    language = cursor.getString(2),
                                    title = cursor.getString(3),
                                    snippet = cursor.getString(4),
                                    editionId = if (cursor.isNull(5)) null else cursor.getString(5),
                                    locator = if (cursor.isNull(6)) null else cursor.getString(6),
                                ),
                            )
                        }
                    }
                }
            } catch (error: SQLiteException) {
                throw ContentSearchException("Offline content search failed closed", error)
            } catch (error: IllegalArgumentException) {
                throw ContentSearchException("Offline content search returned invalid content metadata", error)
            }
        }
    }
}

private fun openReadOnly(path: Path): SQLiteDatabase = SQLiteDatabase.openDatabase(
    path.toAbsolutePath().normalize().toString(),
    null,
    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
)
