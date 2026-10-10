package io.github.gilnetizen.aseh.core.content.runtime

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gilnetizen.aseh.core.content.ActiveContentStore
import io.github.gilnetizen.aseh.core.content.ContentDatabaseContract
import io.github.gilnetizen.aseh.core.content.ContentHashes
import io.github.gilnetizen.aseh.core.content.InstalledPackVersion
import io.github.gilnetizen.aseh.core.content.SemanticVersion
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidContentStoreInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val createdFiles = mutableListOf<Path>()

    @After
    fun cleanUp() {
        createdFiles.forEach { path ->
            Files.deleteIfExists(path)
            Files.deleteIfExists(path.resolveSibling("${path.fileName}-journal"))
            Files.deleteIfExists(path.resolveSibling("${path.fileName}-wal"))
            Files.deleteIfExists(path.resolveSibling("${path.fileName}-shm"))
        }
    }

    @Test
    fun platformVerifierAndFtsSearchUseTheImmutableDatabaseReadOnly() {
        val store = createValidStore()
        val before = ContentHashes.sha256(Files.readAllBytes(store.database))

        AndroidInstalledContentDatabaseVerifier.verify(
            store.database,
            expectedContentSchema = 1,
            expectedPackId = PACK_ID,
            expectedVersion = VERSION,
        )
        val search = AndroidInstalledContentSearch(store)
        val english = search.search("rehear")
        val hebrew = search.search("תפקי")

        assertTrue(english.any { it.contentId == "synthetic.guide.rehearsal" })
        assertTrue(hebrew.any { it.contentId == "synthetic.source.roles.he" })
        assertEquals(before, ContentHashes.sha256(Files.readAllBytes(store.database)))
    }

    @Test
    fun malformedDatabaseIdentityAndUnexpectedSchemaFailClosed() {
        val store = createValidStore()
        assertThrows(ContentDatabaseVerificationException::class.java) {
            AndroidInstalledContentDatabaseVerifier.verify(store.database, 1, "wrong.pack", VERSION)
        }

        SQLiteDatabase.openDatabase(store.database.toString(), null, SQLiteDatabase.OPEN_READWRITE).use { database ->
            database.execSQL("CREATE TRIGGER forbidden_trigger AFTER INSERT ON claims BEGIN DELETE FROM claims; END")
        }
        assertThrows(ContentDatabaseVerificationException::class.java) {
            AndroidInstalledContentDatabaseVerifier.verify(store.database, 1, PACK_ID, VERSION)
        }

        val malformed = newDatabasePath("malformed")
        Files.write(malformed, "not a sqlite database".encodeToByteArray())
        assertThrows(ContentDatabaseVerificationException::class.java) {
            AndroidInstalledContentDatabaseVerifier.verify(malformed, 1, PACK_ID, VERSION)
        }
    }

    @Test
    fun queryInjectionAndOversizedInputsCannotChangeTheDatabase() {
        val store = createValidStore()
        val before = ContentHashes.sha256(Files.readAllBytes(store.database))
        val search = AndroidInstalledContentSearch(store)

        assertTrue(search.search("x' OR 1=1 -- \" NEAR(test)").isEmpty())
        assertThrows(ContentSearchInputException::class.java) { search.search("x".repeat(1025)) }
        assertThrows(ContentSearchInputException::class.java) { search.search("rehearsal", limit = 0) }
        assertEquals(before, ContentHashes.sha256(Files.readAllBytes(store.database)))
    }

    @Test
    fun ed25519ReadinessIsExplicitAcrossTheSupportedDeviceMatrix() {
        val available = AndroidPackCryptoReadiness.isEd25519Available()
        if (Build.VERSION.SDK_INT == 26) {
            assertTrue("API 26 unexpectedly exposed Ed25519; re-review the release blocker", !available)
            assertThrows(PackCryptoUnavailableException::class.java) {
                AndroidPackCryptoReadiness.requireEd25519()
            }
            return
        }

        assertTrue("Default JCA Ed25519 is unavailable on API ${Build.VERSION.SDK_INT}", available)
        AndroidPackCryptoReadiness.requireEd25519()
        val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val result = runCatching {
            val message = "ASEH Android API ${Build.VERSION.SDK_INT} Ed25519 probe".encodeToByteArray()
            val signature = Signature.getInstance("Ed25519").run {
                initSign(keyPair.private)
                update(message)
                sign()
            }
            check(AndroidJcaEd25519SignatureVerifier.verify(keyPair.public, message, signature))
        }

        assertTrue(
            "Default JCA Ed25519 verification failed on Android API ${Build.VERSION.SDK_INT}: " +
                (result.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" }
                    ?: "verification returned false") +
                "; publicKey=${keyPair.public.algorithm}/${keyPair.public.format}/" +
                "${keyPair.public.encoded?.size ?: -1} bytes/" +
                keyPair.public.encoded?.joinToString("") { "%02x".format(it) }?.take(32),
            result.isSuccess,
        )
    }

    private fun createValidStore(): ActiveContentStore {
        val path = newDatabasePath("valid")
        SQLiteDatabase.openDatabase(
            path.toString(),
            null,
            SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
        ).use { database ->
            database.execSQL("PRAGMA foreign_keys=ON")
            database.execSQL("PRAGMA application_id=${ContentDatabaseContract.APPLICATION_ID}")
            database.execSQL("PRAGMA user_version=1")
            database.execSQL("CREATE TABLE pack_metadata (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL) WITHOUT ROWID")
            database.execSQL(
                """
                CREATE TABLE editions (
                  edition_id TEXT PRIMARY KEY NOT NULL, work_id TEXT NOT NULL,
                  language TEXT NOT NULL, script TEXT NOT NULL, version_title TEXT NOT NULL,
                  edition_title TEXT NOT NULL, contributor TEXT NOT NULL, source_url TEXT NOT NULL,
                  raw_sha256 TEXT NOT NULL, license_label TEXT NOT NULL, review_state TEXT NOT NULL
                ) WITHOUT ROWID
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE content_documents (
                  content_id TEXT PRIMARY KEY NOT NULL, kind TEXT NOT NULL, language TEXT NOT NULL,
                  script TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL,
                  normalized_body TEXT NOT NULL, edition_id TEXT, locator TEXT, review_state TEXT NOT NULL,
                  FOREIGN KEY (edition_id) REFERENCES editions(edition_id)
                ) WITHOUT ROWID
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE content_prerequisites (
                  content_id TEXT NOT NULL, prerequisite_content_id TEXT NOT NULL,
                  PRIMARY KEY (content_id, prerequisite_content_id),
                  FOREIGN KEY (content_id) REFERENCES content_documents(content_id),
                  FOREIGN KEY (prerequisite_content_id) REFERENCES content_documents(content_id)
                ) WITHOUT ROWID
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE claims (
                  claim_id TEXT PRIMARY KEY NOT NULL, content_id TEXT NOT NULL, text TEXT NOT NULL,
                  conclusion_status TEXT NOT NULL, review_state TEXT NOT NULL,
                  FOREIGN KEY (content_id) REFERENCES content_documents(content_id)
                ) WITHOUT ROWID
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE citations (
                  claim_id TEXT NOT NULL, source_unit_id TEXT NOT NULL, relation TEXT NOT NULL,
                  PRIMARY KEY (claim_id, source_unit_id, relation),
                  FOREIGN KEY (claim_id) REFERENCES claims(claim_id),
                  FOREIGN KEY (source_unit_id) REFERENCES content_documents(content_id)
                ) WITHOUT ROWID
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE VIRTUAL TABLE content_fts USING fts4(
                  content_id, title, body, normalized_body, locator,
                  tokenize=unicode61 "remove_diacritics=0",
                  notindexed=content_id
                )
                """.trimIndent(),
            )
            database.execSQL("INSERT INTO pack_metadata(key, value) VALUES('pack_id', ?)", arrayOf(PACK_ID))
            database.execSQL("INSERT INTO pack_metadata(key, value) VALUES('pack_version', ?)", arrayOf(VERSION.toString()))
            database.execSQL("INSERT INTO pack_metadata(key, value) VALUES('content_schema', '1')")
            database.execSQL("INSERT INTO pack_metadata(key, value) VALUES('pack_type', 'org.aseh/dev-fixture')")
            database.execSQL("INSERT INTO pack_metadata(key, value) VALUES('review_state', 'Drafted')")
            database.execSQL("INSERT INTO pack_metadata(key, value) VALUES('search_engine', 'fts4')")
            database.execSQL(
                "INSERT INTO pack_metadata(key, value) VALUES('source_tree_sha256', ?)",
                arrayOf("0".repeat(64)),
            )
            insertDocument(
                database,
                id = "synthetic.guide.rehearsal",
                language = "en",
                title = "Synthetic role rehearsal",
                body = "The invented rehearsal team records roles for this software test.",
                normalized = "the invented rehearsal team records roles for this software test",
            )
            insertDocument(
                database,
                id = "synthetic.source.roles.he",
                language = "he",
                title = "מקור בדיקה מלאכותי",
                body = "צוות הבדיקה רושם תפקידים.",
                normalized = "צוות הבדיקה רושם תפקידים",
            )
        }
        return ActiveContentStore(
            identity = InstalledPackVersion(
                packId = PACK_ID,
                version = VERSION,
                manifestSha256 = "0".repeat(64),
                signingKeyId = "1".repeat(64),
                relativeDirectory = "packs/$PACK_ID/$VERSION-${"0".repeat(64)}",
            ),
            database = path,
        )
    }

    private fun insertDocument(
        database: SQLiteDatabase,
        id: String,
        language: String,
        title: String,
        body: String,
        normalized: String,
    ) {
        database.execSQL(
            """
            INSERT INTO content_documents(
              content_id, kind, language, script, title, body, normalized_body,
              edition_id, locator, review_state
            ) VALUES (?, 'development-fixture', ?, ?, ?, ?, ?, NULL, NULL, 'Drafted')
            """.trimIndent(),
            arrayOf(id, language, if (language == "he") "Hebr" else "Latn", title, body, normalized),
        )
        database.execSQL(
            "INSERT INTO content_fts(content_id, title, body, normalized_body, locator) VALUES (?, ?, ?, ?, '')",
            arrayOf(id, title, body, normalized),
        )
    }

    private fun newDatabasePath(label: String): Path {
        val path = context.cacheDir.toPath().resolve("aseh-content-$label-${UUID.randomUUID()}.sqlite")
        createdFiles.add(path)
        return path
    }

    companion object {
        private const val PACK_ID = "aseh.synthetic.rehearsal"
        private val VERSION = SemanticVersion(1, 0, 0)
    }
}
