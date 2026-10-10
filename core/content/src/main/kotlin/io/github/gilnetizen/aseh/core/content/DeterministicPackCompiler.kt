package io.github.gilnetizen.aseh.core.content

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

data class PackCompilationResult(
    val output: Path,
    val manifest: PackManifest,
    val manifestSha256: String,
    val archiveSha256: String,
)

class DeterministicPackCompiler(
    private val databaseCompiler: ContentDatabaseCompiler,
) {
    fun compile(
        source: EditorialPackSource,
        signer: PackSigner,
        output: Path,
        mode: PackBuildMode = PackBuildMode.DEVELOPMENT,
    ): PackCompilationResult {
        ContentPolicyValidator.validate(source, mode)
        require(!Files.exists(output)) { "Pack output already exists: $output" }
        val outputParent = output.toAbsolutePath().parent
        Files.createDirectories(outputParent)
        val work = Files.createTempDirectory(outputParent, ".aseh-pack-build-")
        val pendingOutput = work.resolve("pack.pending")
        return try {
            val database = work.resolve(CONTENT_DATABASE_FILE)
            databaseCompiler.compile(source, database)
            val payload = sortedMapOf<String, ByteArray>()
            payload["attribution.txt"] = normalizedTextFile(source.attributionText)
            payload[CONTENT_DATABASE_FILE] = Files.readAllBytes(database)
            payload["license.txt"] = normalizedTextFile(source.licenseText)

            val inventory = payload.map { (path, bytes) ->
                PackFileRecord(path = path, size = bytes.size.toLong(), sha256 = ContentHashes.sha256(bytes))
            }
            val manifest = PackManifest(
                formatVersion = ContentPolicyValidator.CURRENT_PACK_FORMAT,
                identity = source.identity,
                compatibility = source.compatibility,
                signingKeyId = signer.keyId,
                dependencies = source.dependencies.sortedBy { it.packId },
                provenance = source.provenance,
                review = source.review,
                rights = source.rights,
                editions = source.editions.sortedBy { it.editionId },
                files = inventory,
            )
            ContentPolicyValidator.validateManifest(manifest, production = mode == PackBuildMode.PRODUCTION)
            val manifestBytes = PackManifestJson.encode(manifest)
            val signature = PackSignature(
                formatVersion = 1,
                algorithm = "Ed25519",
                signingKeyId = signer.keyId,
                signature = signer.sign(packSigningMessage(manifestBytes)),
            )
            payload["manifest.json"] = manifestBytes
            payload["signature.json"] = PackSignatureJson.encode(signature)
            payload.keys.forEach(ContentPolicyValidator::validatePackPath)
            DeterministicStoredZipWriter.write(pendingOutput, payload)
            moveAtomically(pendingOutput, output.toAbsolutePath())
            PackCompilationResult(
                output = output.toAbsolutePath(),
                manifest = manifest,
                manifestSha256 = ContentHashes.sha256(manifestBytes),
                archiveSha256 = ContentHashes.sha256(Files.readAllBytes(output.toAbsolutePath())),
            )
        } finally {
            deleteTree(work)
        }
    }

    private fun normalizedTextFile(value: String): ByteArray {
        val normalized = value.replace("\r\n", "\n").replace('\r', '\n').trimEnd() + "\n"
        return normalized.toByteArray(StandardCharsets.UTF_8)
    }

    private fun moveAtomically(source: Path, destination: Path) {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE)
        } catch (error: AtomicMoveNotSupportedException) {
            throw IllegalStateException("Pack build output does not support atomic publication", error)
        }
    }
}

internal fun deleteTree(root: Path) {
    if (!Files.exists(root)) return
    Files.walk(root).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}
