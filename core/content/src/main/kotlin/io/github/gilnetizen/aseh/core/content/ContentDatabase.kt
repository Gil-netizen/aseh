package io.github.gilnetizen.aseh.core.content

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

internal const val CONTENT_DATABASE_FILE = ContentDatabaseContract.FILE_NAME
internal const val CONTENT_DATABASE_APPLICATION_ID = ContentDatabaseContract.APPLICATION_ID

fun interface ContentDatabaseCompiler {
    fun compile(source: EditorialPackSource, output: Path)
}

/** JVM build-tool implementation. The sqlite-jdbc driver is supplied only by the tool/test runtime. */
class JdbcDeterministicContentDatabaseCompiler : ContentDatabaseCompiler {
    override fun compile(source: EditorialPackSource, output: Path) {
        require(!Files.exists(output)) { "Content database output already exists: $output" }
        Files.createDirectories(output.toAbsolutePath().parent)
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${output.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=OFF")
                statement.execute("PRAGMA synchronous=OFF")
                statement.execute("PRAGMA locking_mode=EXCLUSIVE")
                statement.execute("PRAGMA page_size=4096")
                statement.execute("PRAGMA auto_vacuum=NONE")
                statement.execute("PRAGMA encoding='UTF-8'")
                statement.execute("PRAGMA foreign_keys=ON")
                statement.execute("PRAGMA application_id=$CONTENT_DATABASE_APPLICATION_ID")
                statement.execute("PRAGMA user_version=${source.compatibility.contentSchema}")
            }
            connection.autoCommit = false
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE pack_metadata (
                      key TEXT PRIMARY KEY NOT NULL,
                      value TEXT NOT NULL
                    ) WITHOUT ROWID
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE editions (
                      edition_id TEXT PRIMARY KEY NOT NULL,
                      work_id TEXT NOT NULL,
                      language TEXT NOT NULL,
                      script TEXT NOT NULL,
                      version_title TEXT NOT NULL,
                      edition_title TEXT NOT NULL,
                      contributor TEXT NOT NULL,
                      source_url TEXT NOT NULL,
                      raw_sha256 TEXT NOT NULL,
                      license_label TEXT NOT NULL,
                      review_state TEXT NOT NULL
                    ) WITHOUT ROWID
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE content_documents (
                      content_id TEXT PRIMARY KEY NOT NULL,
                      kind TEXT NOT NULL,
                      language TEXT NOT NULL,
                      script TEXT NOT NULL,
                      title TEXT NOT NULL,
                      body TEXT NOT NULL,
                      normalized_body TEXT NOT NULL,
                      edition_id TEXT,
                      locator TEXT,
                      review_state TEXT NOT NULL,
                      FOREIGN KEY (edition_id) REFERENCES editions(edition_id)
                    ) WITHOUT ROWID
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE content_prerequisites (
                      content_id TEXT NOT NULL,
                      prerequisite_content_id TEXT NOT NULL,
                      PRIMARY KEY (content_id, prerequisite_content_id),
                      FOREIGN KEY (content_id) REFERENCES content_documents(content_id),
                      FOREIGN KEY (prerequisite_content_id) REFERENCES content_documents(content_id)
                    ) WITHOUT ROWID
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE claims (
                      claim_id TEXT PRIMARY KEY NOT NULL,
                      content_id TEXT NOT NULL,
                      text TEXT NOT NULL,
                      conclusion_status TEXT NOT NULL,
                      review_state TEXT NOT NULL,
                      FOREIGN KEY (content_id) REFERENCES content_documents(content_id)
                    ) WITHOUT ROWID
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE citations (
                      claim_id TEXT NOT NULL,
                      source_unit_id TEXT NOT NULL,
                      relation TEXT NOT NULL,
                      PRIMARY KEY (claim_id, source_unit_id, relation),
                      FOREIGN KEY (claim_id) REFERENCES claims(claim_id),
                      FOREIGN KEY (source_unit_id) REFERENCES content_documents(content_id)
                    ) WITHOUT ROWID
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE VIRTUAL TABLE content_fts USING fts4(
                      content_id,
                      title,
                      body,
                      normalized_body,
                      locator,
                      tokenize=unicode61 "remove_diacritics=0",
                      notindexed=content_id
                    )
                    """.trimIndent(),
                )
            }
            insertMetadata(connection, source)
            insertEditions(connection, source.editions.sortedBy { it.editionId })
            insertDocuments(connection, source.documents.sortedBy { it.contentId })
            insertPrerequisites(connection, source.documents.sortedBy { it.contentId })
            insertClaimsAndCitations(connection, source.documents.sortedBy { it.contentId })
            connection.commit()
            connection.createStatement().use { statement ->
                statement.execute("INSERT INTO content_fts(content_fts) VALUES('optimize')")
            }
            connection.commit()
            connection.autoCommit = true
            connection.createStatement().use { statement ->
                statement.execute("VACUUM")
            }
        }
    }

    private fun insertMetadata(connection: Connection, source: EditorialPackSource) {
        val values = sortedMapOf(
            "content_schema" to source.compatibility.contentSchema.toString(),
            "pack_id" to source.identity.packId,
            "pack_type" to source.identity.packType.value,
            "pack_version" to source.identity.version.toString(),
            "review_state" to source.review.state.wireName,
            "search_engine" to ContentDatabaseContract.SEARCH_ENGINE,
            "source_tree_sha256" to source.provenance.sourceTreeSha256,
        )
        connection.prepareStatement("INSERT INTO pack_metadata(key, value) VALUES(?, ?)").use { statement ->
            values.forEach { (key, value) ->
                statement.setString(1, key)
                statement.setString(2, value)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertEditions(connection: Connection, editions: List<EditionMetadata>) {
        connection.prepareStatement(
            """
            INSERT INTO editions(
              edition_id, work_id, language, script, version_title, edition_title,
              contributor, source_url, raw_sha256, license_label, review_state
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            editions.forEach { edition ->
                statement.setString(1, edition.editionId)
                statement.setString(2, edition.workId)
                statement.setString(3, edition.language)
                statement.setString(4, edition.script)
                statement.setString(5, edition.versionTitle)
                statement.setString(6, edition.editionTitle)
                statement.setString(7, edition.contributor)
                statement.setString(8, edition.sourceUrl)
                statement.setString(9, edition.rawSha256)
                statement.setString(10, edition.rights.licenseLabel)
                statement.setString(11, edition.review.state.wireName)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertDocuments(connection: Connection, documents: List<ContentDocument>) {
        connection.prepareStatement(
            """
            INSERT INTO content_documents(
              content_id, kind, language, script, title, body, normalized_body,
              edition_id, locator, review_state
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            documents.forEach { document ->
                statement.setString(1, document.contentId)
                statement.setString(2, document.kind.wireName)
                statement.setString(3, document.language)
                statement.setString(4, document.script)
                statement.setString(5, document.title)
                statement.setString(6, document.body)
                statement.setString(7, document.normalizedBody)
                statement.setString(8, document.editionId)
                statement.setString(9, document.locator)
                statement.setString(10, document.reviewState.wireName)
                statement.addBatch()
            }
            statement.executeBatch()
        }
        connection.prepareStatement(
            "INSERT INTO content_fts(content_id, title, body, normalized_body, locator) VALUES (?, ?, ?, ?, ?)",
        ).use { statement ->
            documents.forEach { document ->
                statement.setString(1, document.contentId)
                statement.setString(2, document.title)
                statement.setString(3, document.body)
                statement.setString(4, document.normalizedBody)
                statement.setString(5, document.locator.orEmpty())
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun insertPrerequisites(connection: Connection, documents: List<ContentDocument>) {
        connection.prepareStatement(
            "INSERT INTO content_prerequisites(content_id, prerequisite_content_id) VALUES (?, ?)",
        ).use { statement ->
            documents.forEach { document ->
                document.prerequisiteContentIds.sorted().forEach { prerequisite ->
                    statement.setString(1, document.contentId)
                    statement.setString(2, prerequisite)
                    statement.addBatch()
                }
            }
            statement.executeBatch()
        }
    }

    private fun insertClaimsAndCitations(connection: Connection, documents: List<ContentDocument>) {
        connection.prepareStatement(
            "INSERT INTO claims(claim_id, content_id, text, conclusion_status, review_state) VALUES (?, ?, ?, ?, ?)",
        ).use { claimStatement ->
            connection.prepareStatement(
                "INSERT INTO citations(claim_id, source_unit_id, relation) VALUES (?, ?, ?)",
            ).use { citationStatement ->
                documents.forEach { document ->
                    document.claims.sortedBy { it.claimId }.forEach { claim ->
                        claimStatement.setString(1, claim.claimId)
                        claimStatement.setString(2, document.contentId)
                        claimStatement.setString(3, claim.text)
                        claimStatement.setString(4, claim.conclusionStatus.wireName)
                        claimStatement.setString(5, claim.reviewState.wireName)
                        claimStatement.addBatch()
                        claim.citations.sortedWith(
                            compareBy<CitationRef> { it.sourceUnitId }.thenBy { it.relation.wireName },
                        )
                            .forEach { citation ->
                                citationStatement.setString(1, claim.claimId)
                                citationStatement.setString(2, citation.sourceUnitId)
                                citationStatement.setString(3, citation.relation.wireName)
                                citationStatement.addBatch()
                            }
                    }
                }
                claimStatement.executeBatch()
                citationStatement.executeBatch()
            }
        }
    }
}

data class ContentSearchHit(
    val contentId: String,
    val kind: ContentKind,
    val language: String,
    val title: String,
    val snippet: String,
    val editionId: String?,
    val locator: String?,
)

/** JVM build-tool/test search implementation; Android supplies its own query-only SQLite adapter. */
class JdbcInstalledContentSearch(private val database: Path) {
    fun search(query: String, limit: Int = 20): List<ContentSearchHit> {
        require(limit in 1..100) { "Search limit must be between 1 and 100" }
        val expression = toFtsExpression(query)
        if (expression.isEmpty()) return emptyList()
        openReadOnlyContentDatabase(database).use { connection ->
            connection.prepareStatement(
                """
                SELECT d.content_id, d.kind, d.language, d.title,
                       snippet(content_fts, '', '', ' … ', 2, 18) AS result_snippet,
                       d.edition_id, d.locator
                FROM content_fts
                JOIN content_documents d ON d.content_id = content_fts.content_id
                WHERE content_fts MATCH ?
                ORDER BY d.content_id
                LIMIT ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, expression)
                statement.setInt(2, limit)
                statement.executeQuery().use { results ->
                    return buildList {
                        while (results.next()) {
                            add(
                                ContentSearchHit(
                                    contentId = results.getString("content_id"),
                                    kind = ContentKind.parse(results.getString("kind")),
                                    language = results.getString("language"),
                                    title = results.getString("title"),
                                    snippet = results.getString("result_snippet"),
                                    editionId = results.getString("edition_id"),
                                    locator = results.getString("locator"),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toFtsExpression(query: String): String {
        val terms = Regex("[\\p{L}\\p{M}\\p{N}._:-]+")
            .findAll(query)
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .take(12)
            .toList()
        return terms.joinToString(" AND ") { term -> "\"${term.replace("\"", "\"\"")}*\"" }
    }
}

fun interface InstalledContentDatabaseVerifier {
    fun verify(database: Path, expectedContentSchema: Int, expectedPackId: String, expectedVersion: SemanticVersion)
}

/** JVM build-tool/test verifier; Android must implement [InstalledContentDatabaseVerifier] with platform SQLite. */
object JdbcContentDatabaseVerifier : InstalledContentDatabaseVerifier {
    override fun verify(
        database: Path,
        expectedContentSchema: Int,
        expectedPackId: String,
        expectedVersion: SemanticVersion,
    ) {
        require(Files.isRegularFile(database)) { "Missing content database" }
        openReadOnlyContentDatabase(database).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA query_only").use { result ->
                    require(result.next() && result.getInt(1) == 1) { "Content database is not query-only" }
                }
                statement.executeQuery("PRAGMA quick_check").use { result ->
                    require(result.next() && result.getString(1) == "ok" && !result.next()) { "SQLite quick_check failed" }
                }
                statement.executeQuery("PRAGMA application_id").use { result ->
                    require(result.next() && result.getInt(1) == CONTENT_DATABASE_APPLICATION_ID) {
                        "Unexpected content database application_id"
                    }
                }
                statement.executeQuery("PRAGMA user_version").use { result ->
                    require(result.next() && result.getInt(1) == expectedContentSchema) {
                        "Unexpected content database schema"
                    }
                }
                statement.executeQuery(
                    "SELECT type, name FROM sqlite_master WHERE type IN ('trigger', 'view') ORDER BY type, name",
                ).use { result ->
                    require(!result.next()) { "Content database contains executable views or triggers" }
                }
                val actualTables = buildSet {
                    statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").use { result ->
                        while (result.next()) add(result.getString(1))
                    }
                }
                require(actualTables == ContentDatabaseContract.requiredTableNames) {
                    "Content database table allowlist mismatch"
                }
                statement.executeQuery("PRAGMA foreign_key_check").use { result ->
                    require(!result.next()) { "Content database contains broken references" }
                }
            }
            val metadata = mutableMapOf<String, String>()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT key, value FROM pack_metadata ORDER BY key").use { result ->
                    while (result.next()) metadata[result.getString(1)] = result.getString(2)
                }
            }
            require(metadata["pack_id"] == expectedPackId) { "Content database pack identity mismatch" }
            require(metadata["pack_version"] == expectedVersion.toString()) { "Content database version mismatch" }
            require(metadata.keys == ContentDatabaseContract.requiredMetadataKeys) {
                "Content database metadata allowlist mismatch"
            }
            require(metadata["search_engine"] == ContentDatabaseContract.SEARCH_ENGINE) {
                "Unsupported content search engine"
            }
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM content_fts").use { result ->
                    require(result.next() && result.getInt(1) > 0) { "Content search index is empty" }
                }
            }
        }
    }
}

private fun openReadOnlyContentDatabase(path: Path): Connection {
    Class.forName("org.sqlite.JDBC")
    val normalized = path.toAbsolutePath().normalize()
    val uri = normalized.toUri().toASCIIString()
    val connection = DriverManager.getConnection("jdbc:sqlite:$uri?mode=ro&immutable=1")
    connection.createStatement().use { it.execute("PRAGMA query_only=ON") }
    return connection
}
