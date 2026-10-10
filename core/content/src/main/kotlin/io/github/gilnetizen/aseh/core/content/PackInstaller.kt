package io.github.gilnetizen.aseh.core.content

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.io.path.invariantSeparatorsPathString

enum class PackInstallFailure {
    INVALID_ARCHIVE,
    ARCHIVE_LIMIT_EXCEEDED,
    UNSAFE_PATH,
    INVALID_METADATA,
    UNTRUSTED_KEY,
    INVALID_SIGNATURE,
    HASH_MISMATCH,
    INCOMPATIBLE_SCHEMA,
    MISSING_DEPENDENCY,
    VERSION_REJECTED,
    INVALID_CONTENT_DATABASE,
    ATOMIC_ACTIVATION_UNAVAILABLE,
    IO_FAILURE,
}

class PackInstallException(
    val reason: PackInstallFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

data class PackInstallLimits(
    val maxEntries: Int = 256,
    val maxIndividualBytes: Long = 64L * 1024 * 1024,
    val maxTotalPayloadBytes: Long = 256L * 1024 * 1024,
    val maxArchiveBytes: Long = 280L * 1024 * 1024,
    val maxMetadataBytes: Int = 1024 * 1024,
)

data class PackInstallResult(
    val installed: InstalledPackVersion,
    val previousActive: InstalledPackVersion?,
    val activated: Boolean,
)

class VerifiedPackInstaller(
    root: Path,
    private val trustStore: PackTrustStore,
    private val trustMode: PackTrustMode,
    private val supportedAppSchema: Int,
    private val supportedPackSchema: Int = ContentPolicyValidator.CURRENT_PACK_FORMAT,
    private val supportedContentSchema: Int = ContentPolicyValidator.CURRENT_CONTENT_SCHEMA,
    private val limits: PackInstallLimits = PackInstallLimits(),
    private val signatureVerifier: PackSignatureVerifier = JdkEd25519SignatureVerifier,
    private val contentDatabaseVerifier: InstalledContentDatabaseVerifier,
) {
    private val root = root.toAbsolutePath().normalize()
    private val packsRoot = this.root.resolve("packs")
    private val stagingRoot = this.root.resolve("staging")
    private val catalog = InstalledPackCatalog(this.root)

    fun install(archive: Path): PackInstallResult = withCatalogLock {
        val previousCatalog = catalog.read()
        val verified = inspectArchive(archive, previousCatalog)
        val previousActive = previousCatalog.pack(verified.manifest.identity.packId)?.active
        val alreadyInstalled = previousCatalog.pack(verified.manifest.identity.packId)?.installed
            ?.singleOrNull { it.manifestSha256 == verified.manifestSha256 }
        if (alreadyInstalled != null) {
            verifyInstalledDirectory(alreadyInstalled)
            catalog.activate(alreadyInstalled)
            return@withCatalogLock PackInstallResult(alreadyInstalled, previousActive, activated = true)
        }

        val stage = Files.createDirectories(stagingRoot).resolve(
            "${verified.manifest.identity.packId}-${UUID.randomUUID()}",
        )
        Files.createDirectory(stage)
        var movedDestination: Path? = null
        try {
            extractVerifiedPayload(verified, stage)
            val database = stage.resolve(CONTENT_DATABASE_FILE)
            verifyContentDatabase(database, verified.manifest)
            Files.write(stage.resolve("manifest.json"), verified.manifestBytes, StandardOpenOption.CREATE_NEW)
            Files.write(stage.resolve("signature.json"), verified.signatureBytes, StandardOpenOption.CREATE_NEW)

            val destination = destinationFor(verified.manifest, verified.manifestSha256)
            Files.createDirectories(destination.parent)
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                reject(PackInstallFailure.VERSION_REJECTED, "Verified pack destination already exists outside the catalog")
            }
            try {
                Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE)
            } catch (error: AtomicMoveNotSupportedException) {
                reject(
                    PackInstallFailure.ATOMIC_ACTIVATION_UNAVAILABLE,
                    "Pack filesystem does not support atomic staged installation",
                    error,
                )
            }
            movedDestination = destination
            val record = InstalledPackVersion(
                packId = verified.manifest.identity.packId,
                version = verified.manifest.identity.version,
                manifestSha256 = verified.manifestSha256,
                signingKeyId = verified.manifest.signingKeyId,
                relativeDirectory = root.relativize(destination).invariantSeparatorsPathString,
            )
            try {
                catalog.activate(record)
            } catch (error: AtomicMoveNotSupportedException) {
                reject(
                    PackInstallFailure.ATOMIC_ACTIVATION_UNAVAILABLE,
                    "Pack catalog does not support atomic activation",
                    error,
                )
            } catch (error: Exception) {
                reject(PackInstallFailure.IO_FAILURE, "Could not activate verified pack", error)
            }
            movedDestination = null
            PackInstallResult(record, previousActive, activated = true)
        } finally {
            runCatching { deleteTree(stage) }
            movedDestination?.let { destination -> runCatching { deleteTree(destination) } }
        }
    }

    fun rollback(packId: String, version: SemanticVersion): PackInstallResult = withCatalogLock {
        val current = catalog.read()
        val pack = current.pack(packId)
            ?: reject(PackInstallFailure.VERSION_REJECTED, "Pack '$packId' has no installed versions")
        val target = pack.installed.singleOrNull { it.version == version }
            ?: reject(PackInstallFailure.VERSION_REJECTED, "Version $version is not installed for '$packId'")
        val previous = pack.active
        val manifest = verifyInstalledDirectory(target)
        validateDependencies(manifest, current)
        try {
            catalog.activateExisting(packId, target.manifestSha256)
        } catch (error: AtomicMoveNotSupportedException) {
            reject(
                PackInstallFailure.ATOMIC_ACTIVATION_UNAVAILABLE,
                "Pack catalog does not support atomic rollback",
                error,
            )
        }
        PackInstallResult(target, previous, activated = true)
    }

    fun catalogState(): InstalledPackCatalogState = withCatalogLock { catalog.read() }

    fun activeContentStore(packId: String): ActiveContentStore? = withCatalogLock {
        val current = catalog.read()
        val active = current.pack(packId)?.active ?: return@withCatalogLock null
        val manifest = verifyInstalledDirectory(active)
        validateDependencies(manifest, current)
        val directory = resolveInstalledDirectory(active)
        require(Files.isRegularFile(directory.resolve(CONTENT_DATABASE_FILE), LinkOption.NOFOLLOW_LINKS)) {
            "Active content database is unavailable"
        }
        ActiveContentStore(active, directory.resolve(CONTENT_DATABASE_FILE))
    }

    private fun inspectArchive(archive: Path, currentCatalog: InstalledPackCatalogState): InspectedArchive {
        if (!Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS)) {
            reject(PackInstallFailure.INVALID_ARCHIVE, "Pack archive is not a regular file")
        }
        val archiveSize = Files.size(archive)
        if (archiveSize > limits.maxArchiveBytes) {
            reject(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, "Pack archive exceeds byte limit")
        }
        try {
            ZipFile(archive.toFile(), StandardCharsets.UTF_8).use { zip ->
                if (zip.comment != null) reject(PackInstallFailure.INVALID_ARCHIVE, "ZIP comments are not allowed")
                val entries = zip.entries().toList()
                if (entries.size > limits.maxEntries) {
                    reject(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, "Pack contains too many entries")
                }
                val byName = linkedMapOf<String, ZipEntry>()
                val foldedPaths = mutableSetOf<String>()
                var totalPayload = 0L
                entries.forEach { entry ->
                    validateArchiveEntry(entry)
                    if (byName.put(entry.name, entry) != null) {
                        reject(PackInstallFailure.UNSAFE_PATH, "Duplicate archive path '${entry.name}'")
                    }
                    val folded = Normalizer.normalize(entry.name, Normalizer.Form.NFC).lowercase(Locale.ROOT)
                    if (!foldedPaths.add(folded)) {
                        reject(PackInstallFailure.UNSAFE_PATH, "Archive path normalization collision '${entry.name}'")
                    }
                    totalPayload = try {
                        Math.addExact(totalPayload, entry.size)
                    } catch (error: ArithmeticException) {
                        reject(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, "Pack payload size overflow", error)
                    }
                    if (totalPayload > limits.maxTotalPayloadBytes) {
                        reject(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, "Pack payload exceeds total byte limit")
                    }
                }
                val manifestEntry = byName["manifest.json"]
                    ?: reject(PackInstallFailure.INVALID_METADATA, "Pack has no manifest.json")
                val signatureEntry = byName["signature.json"]
                    ?: reject(PackInstallFailure.INVALID_METADATA, "Pack has no signature.json")
                val manifestBytes = readBounded(zip.getInputStream(manifestEntry), limits.maxMetadataBytes)
                val signatureBytes = readBounded(zip.getInputStream(signatureEntry), limits.maxMetadataBytes)
                val manifest = decodeManifest(manifestBytes)
                val signature = decodeSignature(signatureBytes)
                if (signature.formatVersion != 1 || signature.algorithm != "Ed25519") {
                    reject(PackInstallFailure.INVALID_SIGNATURE, "Unsupported pack signature format")
                }
                if (signature.signingKeyId != manifest.signingKeyId) {
                    reject(PackInstallFailure.INVALID_SIGNATURE, "Manifest and signature key IDs differ")
                }
                val publicKey = trustStore.resolve(manifest.signingKeyId, manifest.identity, trustMode)
                    ?: reject(PackInstallFailure.UNTRUSTED_KEY, "Pack signing key is unknown or out of scope")
                if (!signatureVerifier.verify(publicKey, packSigningMessage(manifestBytes), signature.signature)) {
                    reject(PackInstallFailure.INVALID_SIGNATURE, "Pack manifest signature is invalid")
                }
                validateTrustedManifest(manifest)
                val expectedNames = (manifest.files.map(PackFileRecord::path) + "manifest.json" + "signature.json").toSet()
                if (byName.keys != expectedNames) {
                    val missing = expectedNames - byName.keys
                    val extra = byName.keys - expectedNames
                    reject(
                        PackInstallFailure.INVALID_ARCHIVE,
                        "Archive inventory mismatch; missing=${missing.sorted()} extra=${extra.sorted()}",
                    )
                }
                manifest.files.forEach { file ->
                    val entry = byName.getValue(file.path)
                    if (entry.size != file.size) {
                        reject(PackInstallFailure.HASH_MISMATCH, "Declared size differs for ${file.path}")
                    }
                }
                validateDependencies(manifest, currentCatalog)
                validateVersion(manifest, ContentHashes.sha256(manifestBytes), currentCatalog)
                return InspectedArchive(
                    archive = archive.toAbsolutePath(),
                    manifest = manifest,
                    manifestBytes = manifestBytes,
                    signatureBytes = signatureBytes,
                    manifestSha256 = ContentHashes.sha256(manifestBytes),
                )
            }
        } catch (error: PackInstallException) {
            throw error
        } catch (error: ZipException) {
            reject(PackInstallFailure.INVALID_ARCHIVE, "Pack is not a valid ZIP archive", error)
        } catch (error: Exception) {
            reject(PackInstallFailure.IO_FAILURE, "Could not inspect pack archive", error)
        }
    }

    private fun validateArchiveEntry(entry: ZipEntry) {
        try {
            ContentPolicyValidator.validatePackPath(entry.name)
        } catch (error: ContentValidationException) {
            reject(PackInstallFailure.UNSAFE_PATH, error.message ?: "Unsafe archive path", error)
        }
        if (entry.isDirectory) reject(PackInstallFailure.UNSAFE_PATH, "Directory ZIP entries are not allowed")
        if (entry.method != ZipEntry.STORED) {
            reject(PackInstallFailure.INVALID_ARCHIVE, "Compressed ZIP entries are not allowed")
        }
        if (entry.size < 0 || entry.compressedSize != entry.size || entry.size > limits.maxIndividualBytes) {
            reject(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, "Invalid or excessive size for ${entry.name}")
        }
        if (entry.comment != null) reject(PackInstallFailure.INVALID_ARCHIVE, "Entry comments are not allowed")
        if (entry.extra != null && entry.extra.isNotEmpty()) {
            reject(PackInstallFailure.INVALID_ARCHIVE, "ZIP extra fields are not allowed")
        }
    }

    private fun decodeManifest(bytes: ByteArray): PackManifest = try {
        PackManifestJson.decodeCanonical(bytes)
    } catch (error: Exception) {
        reject(PackInstallFailure.INVALID_METADATA, "Pack manifest is invalid or non-canonical", error)
    }

    private fun decodeSignature(bytes: ByteArray): PackSignature = try {
        PackSignatureJson.decodeCanonical(bytes)
    } catch (error: Exception) {
        reject(PackInstallFailure.INVALID_METADATA, "Pack signature metadata is invalid or non-canonical", error)
    }

    private fun validateTrustedManifest(manifest: PackManifest) {
        try {
            ContentPolicyValidator.validateManifest(manifest, production = trustMode == PackTrustMode.PRODUCTION)
        } catch (error: ContentValidationException) {
            reject(PackInstallFailure.INVALID_METADATA, error.message ?: "Invalid manifest policy", error)
        }
        if (manifest.compatibility.packSchema != supportedPackSchema ||
            manifest.compatibility.contentSchema != supportedContentSchema ||
            supportedAppSchema !in manifest.compatibility.minAppSchema..manifest.compatibility.maxAppSchema
        ) {
            reject(PackInstallFailure.INCOMPATIBLE_SCHEMA, "Pack schema is incompatible with this application")
        }
        if (manifest.files.none { it.path == CONTENT_DATABASE_FILE }) {
            reject(PackInstallFailure.INVALID_METADATA, "Pack has no searchable content database")
        }
    }

    private fun validateDependencies(manifest: PackManifest, catalog: InstalledPackCatalogState) {
        manifest.dependencies.forEach { dependency ->
            val active = catalog.pack(dependency.packId)?.active
            if (active == null || active.version !in dependency.minVersion..dependency.maxVersion) {
                reject(
                    PackInstallFailure.MISSING_DEPENDENCY,
                    "Dependency ${dependency.packId} ${dependency.minVersion}..${dependency.maxVersion} is unavailable",
                )
            }
        }
    }

    private fun validateVersion(
        manifest: PackManifest,
        manifestSha256: String,
        catalog: InstalledPackCatalogState,
    ) {
        val pack = catalog.pack(manifest.identity.packId) ?: return
        val sameVersion = pack.installed.filter { it.version == manifest.identity.version }
        if (sameVersion.any { it.manifestSha256 != manifestSha256 }) {
            reject(PackInstallFailure.VERSION_REJECTED, "An immutable pack version cannot be replaced")
        }
        val active = pack.active
        if (active != null && manifest.identity.version < active.version && sameVersion.isEmpty()) {
            reject(PackInstallFailure.VERSION_REJECTED, "Pack downgrade must use rollback to a verified installed version")
        }
    }

    private fun extractVerifiedPayload(verified: InspectedArchive, stage: Path) {
        ZipFile(verified.archive.toFile(), StandardCharsets.UTF_8).use { zip ->
            verified.manifest.files.sortedBy { it.path }.forEach { file ->
                val entry = zip.getEntry(file.path)
                    ?: reject(PackInstallFailure.INVALID_ARCHIVE, "Payload disappeared during staged install")
                val destination = stage.resolve(file.path).normalize()
                if (!destination.startsWith(stage)) {
                    reject(PackInstallFailure.UNSAFE_PATH, "Payload path escapes staging")
                }
                Files.createDirectories(destination.parent)
                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                zip.getInputStream(entry).use { input ->
                    Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            if (copied > file.size || copied > limits.maxIndividualBytes) {
                                reject(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, "Payload expanded past declared size")
                            }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                val actualHash = digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                if (copied != file.size || actualHash != file.sha256) {
                    reject(PackInstallFailure.HASH_MISMATCH, "Payload hash or size mismatch for ${file.path}")
                }
            }
        }
    }

    private fun verifyInstalledDirectory(record: InstalledPackVersion): PackManifest {
        val directory = resolveInstalledDirectory(record)
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            reject(PackInstallFailure.INVALID_ARCHIVE, "Installed pack directory is missing")
        }
        val manifestPath = directory.resolve("manifest.json")
        val signaturePath = directory.resolve("signature.json")
        val manifestBytes = readFileBounded(manifestPath, limits.maxMetadataBytes)
        val signatureBytes = readFileBounded(signaturePath, limits.maxMetadataBytes)
        val manifest = decodeManifest(manifestBytes)
        val signature = decodeSignature(signatureBytes)
        if (signature.formatVersion != 1 || signature.algorithm != "Ed25519") {
            reject(PackInstallFailure.INVALID_SIGNATURE, "Installed pack signature format is invalid")
        }
        if (ContentHashes.sha256(manifestBytes) != record.manifestSha256 ||
            manifest.identity.packId != record.packId || manifest.identity.version != record.version ||
            manifest.signingKeyId != record.signingKeyId || signature.signingKeyId != record.signingKeyId
        ) {
            reject(PackInstallFailure.HASH_MISMATCH, "Installed pack identity no longer matches catalog")
        }
        val publicKey = trustStore.resolve(manifest.signingKeyId, manifest.identity, trustMode)
            ?: reject(PackInstallFailure.UNTRUSTED_KEY, "Installed pack key is no longer trusted")
        if (!signatureVerifier.verify(publicKey, packSigningMessage(manifestBytes), signature.signature)) {
            reject(PackInstallFailure.INVALID_SIGNATURE, "Installed pack signature is invalid")
        }
        validateTrustedManifest(manifest)
        val expected = (manifest.files.map { it.path } + "manifest.json" + "signature.json").toSet()
        val actual = mutableSetOf<String>()
        Files.walk(directory).use { paths ->
            paths.forEach { path ->
                if (path != directory) {
                    when {
                        Files.isSymbolicLink(path) ->
                            reject(PackInstallFailure.UNSAFE_PATH, "Installed pack contains a symbolic link")
                        Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) -> Unit
                        Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) ->
                            actual += directory.relativize(path).invariantSeparatorsPathString
                        else -> reject(PackInstallFailure.UNSAFE_PATH, "Installed pack contains a non-file entry")
                    }
                }
            }
        }
        if (actual != expected) reject(PackInstallFailure.INVALID_ARCHIVE, "Installed pack file inventory changed")
        manifest.files.forEach { file ->
            val path = directory.resolve(file.path).normalize()
            if (!path.startsWith(directory) || Files.size(path) != file.size || hashFile(path) != file.sha256) {
                reject(PackInstallFailure.HASH_MISMATCH, "Installed payload changed: ${file.path}")
            }
        }
        verifyContentDatabase(directory.resolve(CONTENT_DATABASE_FILE), manifest)
        return manifest
    }

    private fun verifyContentDatabase(database: Path, manifest: PackManifest) {
        try {
            contentDatabaseVerifier.verify(
                database = database,
                expectedContentSchema = manifest.compatibility.contentSchema,
                expectedPackId = manifest.identity.packId,
                expectedVersion = manifest.identity.version,
            )
        } catch (error: Exception) {
            reject(PackInstallFailure.INVALID_CONTENT_DATABASE, "Content SQLite/FTS database failed verification", error)
        }
    }

    private fun destinationFor(manifest: PackManifest, digest: String): Path =
        packsRoot.resolve(manifest.identity.packId).resolve("${manifest.identity.version}-$digest").normalize()

    private fun resolveInstalledDirectory(record: InstalledPackVersion): Path {
        try {
            ContentPolicyValidator.validatePackPath(record.relativeDirectory)
        } catch (error: ContentValidationException) {
            reject(PackInstallFailure.UNSAFE_PATH, "Unsafe installed pack catalog path", error)
        }
        val resolved = root.resolve(record.relativeDirectory).normalize()
        if (!resolved.startsWith(packsRoot) || resolved != destinationForRecord(record)) {
            reject(PackInstallFailure.UNSAFE_PATH, "Installed pack catalog path escapes its immutable directory")
        }
        return resolved
    }

    private fun destinationForRecord(record: InstalledPackVersion): Path =
        packsRoot.resolve(record.packId).resolve("${record.version}-${record.manifestSha256}").normalize()

    private fun readFileBounded(path: Path, maxBytes: Int): ByteArray {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > maxBytes) {
            reject(PackInstallFailure.INVALID_METADATA, "Installed metadata is missing or oversized")
        }
        return Files.newInputStream(path).use { readBounded(it, maxBytes) }
    }

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        input.use {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) reject(PackInstallFailure.ARCHIVE_LIMIT_EXCEEDED, "Metadata exceeds byte limit")
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    private fun hashFile(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun <T> withCatalogLock(action: () -> T): T {
        Files.createDirectories(root)
        val lockPath = root.resolve(".catalog.lock")
        try {
            java.nio.channels.FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            ).use { channel ->
                channel.lock().use { return action() }
            }
        } catch (error: PackInstallException) {
            throw error
        } catch (error: Exception) {
            reject(PackInstallFailure.IO_FAILURE, "Pack catalog operation failed", error)
        }
    }

    private fun reject(reason: PackInstallFailure, message: String, cause: Throwable? = null): Nothing =
        throw PackInstallException(reason, message, cause)

    private data class InspectedArchive(
        val archive: Path,
        val manifest: PackManifest,
        val manifestBytes: ByteArray,
        val signatureBytes: ByteArray,
        val manifestSha256: String,
    )
}

/** Repository-layer handoff. Feature modules should receive search results, never this filesystem handle. */
data class ActiveContentStore(
    val identity: InstalledPackVersion,
    val database: Path,
)
