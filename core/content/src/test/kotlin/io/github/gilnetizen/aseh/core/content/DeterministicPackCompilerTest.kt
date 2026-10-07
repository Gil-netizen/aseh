package io.github.gilnetizen.aseh.core.content

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicPackCompilerTest {
    @Test
    fun `development asset key is the public RFC test vector and deterministic`() {
        val first = DevelopmentPackAssetGenerator.syntheticKeyPair()
        val second = DevelopmentPackAssetGenerator.syntheticKeyPair()

        assertArrayEquals(first.public.encoded, second.public.encoded)
        assertArrayEquals(first.private.encoded, second.private.encoded)
        assertEquals(
            "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
            Ed25519Keys.rawPublicKey(first.public).joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            },
        )
    }

    @Test
    fun `same typed source and key produce byte-identical signed packs`() {
        val root = SyntheticPackFixture.tempDirectory("aseh-pack-repro-")
        try {
            val source = SyntheticPackFixture.source()
            val keyPair = SyntheticPackFixture.keyPair()
            val firstPath = root.resolve("first.asehpack")
            val secondPath = root.resolve("second.asehpack")

            val first = SyntheticPackFixture.compile(source, keyPair, firstPath)
            val second = SyntheticPackFixture.compile(source, keyPair, secondPath)

            assertArrayEquals(Files.readAllBytes(firstPath), Files.readAllBytes(secondPath))
            assertEquals(first.archiveSha256, second.archiveSha256)
            assertEquals(first.manifestSha256, second.manifestSha256)
            assertEquals(
                listOf("attribution.txt", "content.sqlite", "license.txt"),
                first.manifest.files.map { it.path },
            )

            ZipFile(firstPath.toFile(), StandardCharsets.UTF_8).use { zip ->
                val entries = zip.entries().toList()
                assertEquals(entries.map { it.name }.sorted(), entries.map { it.name })
                assertTrue(entries.all { it.method == ZipEntry.STORED })
                assertTrue(entries.all { it.timeLocal == LocalDateTime.of(1980, 1, 1, 0, 0) })
                assertTrue(entries.all { it.extra == null || it.extra.isEmpty() })

                val manifestBytes = zip.getInputStream(zip.getEntry("manifest.json")).use { it.readAllBytes() }
                val signatureBytes = zip.getInputStream(zip.getEntry("signature.json")).use { it.readAllBytes() }
                val manifest = PackManifestJson.decodeCanonical(manifestBytes)
                val signature = PackSignatureJson.decodeCanonical(signatureBytes)
                assertEquals(Ed25519Keys.keyId(keyPair.public), manifest.signingKeyId)
                assertTrue(
                    JdkEd25519SignatureVerifier.verify(
                        keyPair.public,
                        packSigningMessage(manifestBytes),
                        signature.signature,
                    ),
                )
                manifest.files.forEach { file ->
                    val bytes = zip.getInputStream(zip.getEntry(file.path)).use { it.readAllBytes() }
                    assertEquals(file.size, bytes.size.toLong())
                    assertEquals(file.sha256, ContentHashes.sha256(bytes))
                }
            }
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `generated SQLite FTS searches English Hebrew and locators offline`() {
        val root = SyntheticPackFixture.tempDirectory("aseh-pack-search-")
        try {
            val source = SyntheticPackFixture.source()
            val database = root.resolve("content.sqlite")
            JdbcDeterministicContentDatabaseCompiler().compile(source, database)
            JdbcContentDatabaseVerifier.verify(database, 1, source.identity.packId, source.identity.version)

            val search = JdbcInstalledContentSearch(database)
            assertTrue(search.search("rehear").any { it.contentId == "synthetic.guide.rehearsal" })
            assertTrue(search.search("תפקי").any { it.contentId == "synthetic.source.roles.he" })
            assertTrue(search.search("fixture:en:3").any { it.contentId == "synthetic.source.roles.en" })
            assertFalse(search.search("word-that-does-not-exist").isNotEmpty())
        } finally {
            deleteTree(root)
        }
    }
}
