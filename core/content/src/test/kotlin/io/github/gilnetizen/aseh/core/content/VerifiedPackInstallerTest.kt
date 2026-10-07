package io.github.gilnetizen.aseh.core.content

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedPackInstallerTest {
    @Test
    fun `valid signed pack installs activates searches and rolls back atomically`() {
        val root = SyntheticPackFixture.tempDirectory("aseh-install-rollback-")
        try {
            val keyPair = SyntheticPackFixture.keyPair()
            val sourceV1 = SyntheticPackFixture.source()
            val packV1 = root.resolve("v1.asehpack")
            SyntheticPackFixture.compile(sourceV1, keyPair, packV1)
            val installer = installer(root.resolve("installed"), keyPair)

            val first = installer.install(packV1)
            assertEquals(SemanticVersion(1, 0, 0), first.installed.version)
            val activeV1 = requireNotNull(installer.activeContentStore(sourceV1.identity.packId))
            assertTrue(
                JdbcInstalledContentSearch(activeV1.database).search("rehearsal")
                    .any { it.contentId == "synthetic.guide.rehearsal" },
            )

            val sourceV2 = sourceV1.copy(
                identity = sourceV1.identity.copy(version = SemanticVersion(2, 0, 0)),
                documents = sourceV1.documents.map { document ->
                    if (document.contentId == "synthetic.guide.rehearsal") {
                        document.copy(title = "Synthetic role rehearsal version two")
                    } else {
                        document
                    }
                },
            )
            val packV2 = root.resolve("v2.asehpack")
            SyntheticPackFixture.compile(sourceV2, keyPair, packV2)
            installer.install(packV2)
            assertEquals(SemanticVersion(2, 0, 0), installer.catalogState().pack(sourceV1.identity.packId)?.active?.version)

            installer.rollback(sourceV1.identity.packId, SemanticVersion(1, 0, 0))
            val state = installer.catalogState().pack(sourceV1.identity.packId)
            assertEquals(SemanticVersion(1, 0, 0), state?.active?.version)
            assertEquals(listOf(SemanticVersion(1, 0, 0), SemanticVersion(2, 0, 0)), state?.installed?.map { it.version })
            assertTrue(Files.exists(activeV1.database))
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `tampered payload is rejected and prior active pack remains untouched`() {
        val root = SyntheticPackFixture.tempDirectory("aseh-install-hash-")
        try {
            val keyPair = SyntheticPackFixture.keyPair()
            val source = SyntheticPackFixture.source()
            val valid = root.resolve("valid.asehpack")
            SyntheticPackFixture.compile(source, keyPair, valid)
            val installer = installer(root.resolve("installed"), keyPair)
            val installed = installer.install(valid).installed

            val tampered = root.resolve("tampered.asehpack")
            val entries = readArchive(valid).toMutableMap()
            entries["license.txt"] = entries.getValue("license.txt") + "tampered".encodeToByteArray()
            writeStoredArchive(tampered, entries)
            val error = assertThrows(PackInstallException::class.java) { installer.install(tampered) }

            assertEquals(PackInstallFailure.HASH_MISMATCH, error.reason)
            assertEquals(installed.manifestSha256, installer.catalogState().pack(source.identity.packId)?.activeManifestSha256)
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `wrong key and changed signature are rejected before staging`() {
        val root = SyntheticPackFixture.tempDirectory("aseh-install-signature-")
        try {
            val signingKey = SyntheticPackFixture.keyPair()
            val otherKey = SyntheticPackFixture.keyPair()
            val source = SyntheticPackFixture.source()
            val valid = root.resolve("valid.asehpack")
            SyntheticPackFixture.compile(source, signingKey, valid)

            val wrongTrust = installer(root.resolve("wrong-trust"), otherKey)
            val trustError = assertThrows(PackInstallException::class.java) { wrongTrust.install(valid) }
            assertEquals(PackInstallFailure.UNTRUSTED_KEY, trustError.reason)

            val entries = readArchive(valid).toMutableMap()
            val signature = PackSignatureJson.decodeCanonical(entries.getValue("signature.json"))
            val changedBytes = signature.signature.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
            entries["signature.json"] = PackSignatureJson.encode(signature.copy(signature = changedBytes))
            val changed = root.resolve("changed-signature.asehpack")
            writeStoredArchive(changed, entries)
            val signatureError = assertThrows(PackInstallException::class.java) {
                installer(root.resolve("changed-signature-install"), signingKey).install(changed)
            }
            assertEquals(PackInstallFailure.INVALID_SIGNATURE, signatureError.reason)
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `traversal and oversized archives are rejected without writing outside staging`() {
        val root = SyntheticPackFixture.tempDirectory("aseh-install-limits-")
        try {
            val keyPair = SyntheticPackFixture.keyPair()
            val source = SyntheticPackFixture.source()
            val valid = root.resolve("valid.asehpack")
            SyntheticPackFixture.compile(source, keyPair, valid)

            val traversalEntries = readArchive(valid).toMutableMap()
            traversalEntries["../escape.txt"] = "must never be extracted".encodeToByteArray()
            val traversal = root.resolve("traversal.asehpack")
            writeStoredArchive(traversal, traversalEntries)
            val traversalInstallerRoot = root.resolve("traversal-install")
            val traversalError = assertThrows(PackInstallException::class.java) {
                installer(traversalInstallerRoot, keyPair).install(traversal)
            }
            assertEquals(PackInstallFailure.UNSAFE_PATH, traversalError.reason)
            assertFalse(Files.exists(root.resolve("escape.txt")))

            val tinyLimits = PackInstallLimits(maxArchiveBytes = Files.size(valid) - 1)
            val sizeError = assertThrows(PackInstallException::class.java) {
                installer(root.resolve("size-install"), keyPair, tinyLimits).install(valid)
            }
            assertEquals(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, sizeError.reason)
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `incompatible schema and immutable version replacement are rejected`() {
        val root = SyntheticPackFixture.tempDirectory("aseh-install-version-")
        try {
            val keyPair = SyntheticPackFixture.keyPair()
            val source = SyntheticPackFixture.source()
            val installer = installer(root.resolve("installed"), keyPair)
            val valid = root.resolve("valid.asehpack")
            SyntheticPackFixture.compile(source, keyPair, valid)
            installer.install(valid)

            val incompatible = source.copy(
                identity = source.identity.copy(version = SemanticVersion(2, 0, 0)),
                compatibility = source.compatibility.copy(minAppSchema = 2, maxAppSchema = 2),
            )
            val incompatiblePack = root.resolve("incompatible.asehpack")
            SyntheticPackFixture.compile(incompatible, keyPair, incompatiblePack)
            val schemaError = assertThrows(PackInstallException::class.java) { installer.install(incompatiblePack) }
            assertEquals(PackInstallFailure.INCOMPATIBLE_SCHEMA, schemaError.reason)

            val replacement = source.copy(
                documents = source.documents.map { document ->
                    if (document.contentId == "synthetic.guide.rehearsal") {
                        document.copy(title = "Changed bytes under an immutable version")
                    } else {
                        document
                    }
                },
            )
            val replacementPack = root.resolve("replacement.asehpack")
            SyntheticPackFixture.compile(replacement, keyPair, replacementPack)
            val versionError = assertThrows(PackInstallException::class.java) { installer.install(replacementPack) }
            assertEquals(PackInstallFailure.VERSION_REJECTED, versionError.reason)
            assertEquals(SemanticVersion(1, 0, 0), installer.catalogState().pack(source.identity.packId)?.active?.version)
        } finally {
            deleteTree(root)
        }
    }

    private fun installer(
        root: Path,
        keyPair: KeyPair,
        limits: PackInstallLimits = PackInstallLimits(),
    ): VerifiedPackInstaller = VerifiedPackInstaller(
        root = root,
        trustStore = SyntheticPackFixture.trustStore(keyPair),
        trustMode = PackTrustMode.DEVELOPMENT,
        supportedAppSchema = 1,
        limits = limits,
        contentDatabaseVerifier = JdbcContentDatabaseVerifier,
    )

    private fun readArchive(path: Path): Map<String, ByteArray> =
        ZipFile(path.toFile(), StandardCharsets.UTF_8).use { zip ->
            zip.entries().toList().associate { entry ->
                entry.name to zip.getInputStream(entry).use { it.readAllBytes() }
            }
        }

    private fun writeStoredArchive(path: Path, entries: Map<String, ByteArray>) {
        DeterministicStoredZipWriter.write(path, entries)
    }
}
